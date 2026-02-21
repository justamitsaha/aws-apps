package com.saha.amit.fileReader.controller;

import com.saha.amit.fileReader.entity.AiInteraction;
import com.saha.amit.fileReader.entity.CustomerChurnEntity;
import com.saha.amit.fileReader.entity.CustomerEntity;
import com.saha.amit.fileReader.entity.CustomerProfile;
import com.saha.amit.fileReader.service.AiCacheService;
import com.saha.amit.fileReader.service.CustomerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/customerProfile")
@AllArgsConstructor
public class CustomerProfileController {

    private final CustomerService customerService;
    private final AiCacheService aiCacheService;

    @GetMapping(value = "/customers", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<CustomerEntity> customerEndpoint(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return customerService.getAllCustomers(PageRequest.of(page, size));
    }

    @GetMapping(value = "/customersChurn", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<CustomerChurnEntity> customerChurnEndpoint(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return customerService.getAllCustomersChurn(PageRequest.of(page, size));
    }

    @DeleteMapping("/cleanup")
    public Mono<ResponseEntity<Void>> cleanup() {
        return customerService.clearAllData()
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<CustomerProfile>> getCustomerProfile(@PathVariable Long id) {
        log.info("Received request for customer profile with ID: {}", id);
        return customerService.getFullProfile(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnNext(customerProfileResponseEntity -> log.info("Fetched profile for customer ID: {}", id));
    }

    @PostMapping("/recommendation")
    public Mono<ResponseEntity<AiInteraction>> saveRecommendation(@RequestBody AiInteraction aiInteraction) {
        log.info("Received AI interaction for customer ID: {}", aiInteraction.customerId());
        return aiCacheService.saveRecommendation(aiInteraction)
                .map(savedInteraction -> {
                    log.info("Saved AI interaction for customer ID: {}", aiInteraction.customerId());
                    return ResponseEntity.ok(savedInteraction);
                })
                .doOnError(error -> log.error("Error saving AI interaction for customer ID {}: {}", aiInteraction.customerId(), error.getMessage()));
    }

    @GetMapping("/{customerId}/recommendation")
    public Mono<ResponseEntity<AiInteraction>> getRecommendation(@PathVariable String customerId) {
        log.info("Received request for recommendation for customer ID: {}", customerId);
        return aiCacheService.getRecommendation(customerId)
                .doOnNext(interaction -> log.info("Fetched interaction for {}", interaction.id()))
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("No recommendation found for customer: {}", customerId);
                    return Mono.just(ResponseEntity.notFound().build());
                }))
                .doOnError(e -> log.error("Failed to fetch for {}: {}", customerId, e.getMessage()));
    }
}
