package com.saha.amit.reporting.controller;


import com.saha.amit.reporting.model.RetentionPlan;
import com.saha.amit.reporting.service.ExternalApiService;
import com.saha.amit.reporting.service.RetentionAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/reporting")
@RequiredArgsConstructor
public class ReportingController {

    private final ExternalApiService externalApiService;
    private final RetentionAiService retentionAiService;

    @GetMapping("/health")
    public Mono<ResponseEntity<String>> healthCheck() {
        return externalApiService.fetchExternalData("/upload/health")
                .map(ResponseEntity::ok) // Wrap data in 200 OK
                .defaultIfEmpty(ResponseEntity.notFound().build()); // Handle empty case
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @GetMapping("/{id}/analyze")
    public Mono<ResponseEntity<RetentionPlan>> analyze(@PathVariable Long id) {
        return retentionAiService.analyzeCustomer(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @GetMapping("/{id}/analyze/nocache")
    public Mono<ResponseEntity<RetentionPlan>> analyzeNoCache(@PathVariable Long id) {
        return retentionAiService.analyzeCustomerWithoutCache(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @GetMapping("/{id}/analyze/rag")
    public Mono<ResponseEntity<RetentionPlan>> analyzeWithRag(@PathVariable Long id) {
        return externalApiService.fetchCustomerProfile(id)
                .flatMap(retentionAiService::analyzeCustomerWithRAg)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
