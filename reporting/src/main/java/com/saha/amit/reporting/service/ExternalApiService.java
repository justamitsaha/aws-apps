package com.saha.amit.reporting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ExternalApiService {

    private final WebClient webClient;

    // Inject the host and builder directly into the constructor
    public ExternalApiService(
            WebClient.Builder webClientBuilder,
            @Value("${app.external.api.host}") String host) {

        this.webClient = webClientBuilder
                .baseUrl(host) // host is now guaranteed to be present
                .build();
    }

    public Mono<String> fetchExternalData(String endpoint) {
        return this.webClient.get()
                .uri(endpoint) // Automatically uses the baseUrl set above
                .retrieve()
                .bodyToMono(String.class);
    }
}


