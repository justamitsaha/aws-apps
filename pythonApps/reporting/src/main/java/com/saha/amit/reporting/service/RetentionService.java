package com.saha.amit.reporting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saha.amit.reporting.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for customer retention analysis.
 * Orchestrates data fetching, AI analysis, caching, and RAG-based grounded analysis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetentionService {

    private final ExternalApiService externalApiService;
    private final LlmService llmService;
    private final RagService ragService;
    private final ObjectMapper objectMapper;

    public Mono<RetentionPlan> analyzeCustomer(Long id) {
        String customerId = String.valueOf(id);

        return externalApiService.fetchCustomerProfile(id)
                .doOnSuccess(profile -> log.info("Fetched profile for customerId={}", customerId))
                .doOnError(error -> log.error("Error fetching profile for customerId={}: {}", customerId, error.getMessage()))
                .switchIfEmpty(Mono.error(new RuntimeException("Customer profile not found for id: " + id)))
                .flatMap(profile ->
                        // 1. ATTEMPT CACHE FETCH
                        externalApiService.getSavedRecommendation(customerId)
                                .doOnNext(aiInteraction -> log.info("Fetched cached recommendation for customerId={}:", aiInteraction.customerId()))
                                .flatMap(this::parseCachedPlan)
                                .doOnNext(plan -> log.info("Cache HIT for customer: {}", plan))
                                .onErrorResume(e -> {
                                    log.error("Cache fetch/parse failed for customer {}. Falling back to AI. Error: {}", customerId, e.getMessage());
                                    return Mono.empty();
                                })
                                // 2. CACHE MISS -> CALL AI
                                .switchIfEmpty(Mono.defer(() ->
                                        llmService.generateRetentionPlan(profile)
                                                .flatMap(plan -> persistAiInteractionToDb(customerId, plan).thenReturn(plan))
                                ))
                );
    }

    public Mono<RetentionPlan> analyzeCustomerWithoutCache(Long id) {
        String customerId = String.valueOf(id);
        return externalApiService.fetchCustomerProfile(id)
                .doOnError(error -> log.error("Error fetching profile customerId={}: {}", customerId, error.getMessage()))
                .switchIfEmpty(Mono.error(new RuntimeException("Customer profile not found for id: " + id)))
                .flatMap(llmService::generateRetentionPlan);
    }

    public Mono<RetentionPlan> analyzeCustomerWithRag(CustomerProfile profile) {
        String ragQuery = buildRagQuery(profile);

        return ragService.search(ragQuery, 5, true)
                .collectList()
                .flatMap(matches -> {
                    String policyContext = formatPolicyContext(matches);
                    log.info("RAG query for customerId={}: {}", profile.customerId(), ragQuery);
                    log.info("Policy chunks retrieved={} for customerId={}", matches.size(), profile.customerId());

                    return llmService.generateRetentionPlanWithContext(profile, policyContext)
                            .map(plan -> attachCitations(plan, matches));
                });
    }

    private Mono<RetentionPlan> parseCachedPlan(AiInteraction interaction) {
        return Mono.fromCallable(() -> objectMapper.readValue(interaction.aiResponse(), RetentionPlan.class))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<AiInteraction> persistAiInteractionToDb(String customerId, RetentionPlan plan) {
        try {
            String jsonStr = objectMapper.writeValueAsString(plan);
            AiInteraction interaction = new AiInteraction(
                    null, customerId, "AI Generated Plan", jsonStr, LocalDateTime.now()
            );

            return externalApiService.saveToApi1(interaction)
                    .doOnSuccess(s -> log.info("Persisted AI recommendation for customerId={}", customerId))
                    .doOnError(e -> log.error("Failed to save to API 1: {}", e.getMessage()))
                    .onErrorResume(e -> Mono.empty());
        } catch (Exception e) {
            log.error("Serialization error: {}", e.getMessage());
            return Mono.empty();
        }
    }

    private String buildRagQuery(CustomerProfile profile) {
        return """
                retention policy and offers for:
                contract=%s,
                monthlyCharges=%s,
                tenure=%s,
                techSupport=%s,
                internetService=%s,
                paymentMethod=%s
                """.formatted(
                profile.contract(),
                profile.monthlyCharges(),
                profile.tenure(),
                profile.techSupport(),
                profile.internetService(),
                profile.paymentMethod()
        );
    }

    private String formatPolicyContext(List<ChunkMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return "NO_POLICY_FOUND";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(matches.size(), 5); i++) {
            ChunkMatch m = matches.get(i);
            sb.append("SOURCE: ").append(m.fileName())
                    .append(" | chunk=").append(m.chunkIndex())
                    .append(" | score=").append(String.format("%.3f", m.score()))
                    .append("\n")
                    .append("[CITATION ").append(i + 1).append("] ")
                    .append(m.fileName()).append(" | chunk=").append(m.chunkIndex())
                    .append("\n")
                    .append(m.chunkText())
                    .append("\n\n");
        }
        return sb.toString();
    }

    private RetentionPlan attachCitations(RetentionPlan basePlan, List<ChunkMatch> matches) {
        List<Citation> citations = matches.stream()
                .map(m -> new Citation(m.fileName(), m.chunkIndex(), m.score()))
                .toList();

        return new RetentionPlan(
                basePlan.riskLevel(),
                basePlan.reasoning(),
                basePlan.actions(),
                basePlan.offer(),
                citations
        );
    }
}
