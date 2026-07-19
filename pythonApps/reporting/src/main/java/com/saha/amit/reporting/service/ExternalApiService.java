package com.saha.amit.reporting.service;

import com.saha.amit.reporting.model.AiInteraction;
import com.saha.amit.reporting.model.CustomerProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
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


    public Mono<CustomerProfile> fetchCustomerProfile(Long id) {
        return this.webClient.get()
                .uri("/customerProfile/{id}", id)
                .retrieve()
                .bodyToMono(CustomerProfile.class)
                .doOnSuccess(customerProfile -> log.debug("Fetched customer profile: {}", customerProfile))
                .doOnError(error -> log.error("Error fetching customer profile for id {}: {}", id, error.getMessage()));
    }


    public Mono<AiInteraction> getSavedRecommendation(String customerId) {
        return this.webClient.get()
                .uri("/customerProfile/{customerId}/recommendation", customerId)
                .retrieve()
                // Handle 404 gracefully so it returns Mono.empty() instead of throwing an error
                .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.empty())
                .bodyToMono(AiInteraction.class)
                .doOnSuccess(res -> {
                    if (res != null) log.debug("Cache HIT for customer: {}", res);
                })
                .doOnError(error -> log.error("Error fetching saved recommendation for customerId {}: {}", customerId, error.getMessage()));
    }

    public Mono<AiInteraction> saveToApi1(AiInteraction interaction) {
        return this.webClient.post()
                .uri("/customerProfile/recommendation")
                .bodyValue(interaction)
                .retrieve()
                .bodyToMono(AiInteraction.class)
                .doOnSuccess(s -> log.debug("Successfully persisted AI response to API 1{}", s))
                .doOnError(error -> log.error("Error saving AI interaction for customerId {}: {}", interaction.customerId(), error.getMessage()));
    }
}


