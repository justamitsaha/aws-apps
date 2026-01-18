package com.saha.amit.fileReader.controller;

import com.saha.amit.fileReader.entity.AiInteraction;
import com.saha.amit.fileReader.entity.CustomerProfile;
import com.saha.amit.fileReader.service.CustomerProfileService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/customerProfile")
@AllArgsConstructor
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping("/{id}")
    public Mono<ResponseEntity<CustomerProfile>> getCustomerProfile(@PathVariable Long id) {
        log.info("Received request for customer profile with ID: {}", id);
        return customerProfileService.getFullProfile(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnNext(customerProfileResponseEntity -> log.info("Fetched profile for customer ID {}: {}", id, customerProfileResponseEntity));
    }

    @PostMapping("/recommendation")
    public Mono<ResponseEntity<AiInteraction>> saveRecommendation(@RequestBody AiInteraction aiInteraction) {
        log.info("Received AI interaction for customer ID: {}", aiInteraction.customerId());
        return customerProfileService.generateAndSave(aiInteraction)
                .map(savedInteraction -> {
                    log.info("Saved AI interaction for customer ID {}: {}", aiInteraction.customerId(), savedInteraction.toString());
                    return ResponseEntity.ok(savedInteraction);
                })
                .doOnError(error -> log.error("Error saving AI interaction for customer ID {}: {}", aiInteraction.customerId(), error.getMessage()));
    }

    @GetMapping("/{customerId}/recommendation")
    public Mono<ResponseEntity<AiInteraction>> getRecommendation(@PathVariable String customerId) {
        log.info("Received request for recommendation for customer ID: {}", customerId);
        return customerProfileService.getRecommendation(customerId)
                // Use log() or tap() to see the signals without manually mapping just for logs
                .doOnNext(interaction -> log.info("Fetched interaction for {}", interaction))
                .map(ResponseEntity::ok)
                // If the Mono is empty, return a 404
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("No recommendation found for customer: {}", customerId);
                    return Mono.just(ResponseEntity.notFound().build());
                }))
                .doOnError(e -> log.error("Failed to fetch for {}: {}", customerId, e.getMessage()));
    }

}
