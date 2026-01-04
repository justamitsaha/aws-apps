package com.saha.amit.reporting.controller;


import com.saha.amit.reporting.service.ExternalApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/reporting")
@RequiredArgsConstructor
public class ReportingController {

    private final ExternalApiService externalApiService;

    @GetMapping("/health")
    public Mono<ResponseEntity<String>> healthCheck() {
        return externalApiService.fetchExternalData("/upload/health")
                .map(data -> ResponseEntity.ok(data)) // Wrap data in 200 OK
                .defaultIfEmpty(ResponseEntity.notFound().build()); // Handle empty case
    }
}
