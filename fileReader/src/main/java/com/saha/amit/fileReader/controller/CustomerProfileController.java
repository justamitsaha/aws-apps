package com.saha.amit.fileReader.controller;

import com.saha.amit.fileReader.entity.CustomerProfile;
import com.saha.amit.fileReader.service.CustomerProfileService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

}
