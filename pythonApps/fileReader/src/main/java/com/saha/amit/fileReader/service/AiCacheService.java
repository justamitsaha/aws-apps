package com.saha.amit.fileReader.service;

import com.saha.amit.fileReader.entity.AiInteraction;
import com.saha.amit.fileReader.reopsitory.AiInteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service for managing the AI recommendation cache (AI Interactions).
 * Handles retrieval and persistence of AI-generated plans for customers.
 */
@Service
@RequiredArgsConstructor
public class AiCacheService {

    private final AiInteractionRepository repository;

    public Mono<AiInteraction> getRecommendation(String customerId) {
        return repository.findFirstByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public Mono<AiInteraction> saveRecommendation(AiInteraction aiInteraction) {
        return repository.save(aiInteraction);
    }
}
