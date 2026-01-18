package com.saha.amit.reporting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saha.amit.reporting.model.AiInteraction;
import com.saha.amit.reporting.model.CustomerProfile;
import com.saha.amit.reporting.model.RetentionPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@Slf4j
@Service
public class RetentionAiService {

    private final ChatClient chatClient;

    private final ExternalApiService externalApiService;

    private final ObjectMapper objectMapper;

    public RetentionAiService(ChatClient.Builder builder, ExternalApiService externalApiService, ObjectMapper objectMapper) {
        this.chatClient = builder.build();
        this.externalApiService = externalApiService;
        this.objectMapper = objectMapper;
    }

    public Mono<RetentionPlan> analyzeCustomer(Long id) {
        String customerId = String.valueOf(id);

        return externalApiService.fetchCustomerProfile(id)
                .doOnSuccess(profile -> log.info("Fetched profile for customerId={}", customerId))
                .doOnError(error -> log.error("Error fetching profile for customerId={}: {}", customerId, error.getMessage()))
                .switchIfEmpty(Mono.error(new RuntimeException("Customer profile not found for id: " + id)))
                .flatMap(profile ->
                        // 1. ATTEMPT CACHE FETCH
                        externalApiService.getSavedRecommendation(customerId)
                                .doOnNext(aiInteraction -> log.info("Fetched cached recommendation for customerId={}:", aiInteraction))
                                .flatMap(interaction -> Mono.fromCallable(() ->
                                                objectMapper.readValue(interaction.aiResponse(), RetentionPlan.class)
                                        )
                                        // Can be resource intensive, so offload to boundedElastic
                                        .subscribeOn(Schedulers.boundedElastic()))
                                .doOnNext(plan -> log.info("Cache HIT for customer: {}", plan))
                                // RESILIENCE: If API 1 is down or DB error, log it and treat as a Cache Miss
                                .onErrorResume(JsonProcessingException.class, e -> {
                                    log.error("Cache entry invalid JSON for customer {}. Recomputing.", customerId);
                                    return Mono.empty();
                                })
                                .onErrorResume(e -> {
                                    log.error("API 1 unavailable. Falling back to AI. {}", e.getMessage());
                                    return Mono.empty();
                                })
                                // 2. CACHE MISS (OR API 1 ERROR) -> CALL AI
                                .switchIfEmpty(Mono.defer(() ->
                                        analyzeCustomerWithAI(profile)
                                                .flatMap(plan -> {
                                                    // 3. ATTEMPT PERSIST
                                                    return persistAiInteractionToDb(customerId, plan)
                                                            .thenReturn(plan);
                                                })
                                ))
                );
    }

    /**
     * Helper to handle the "Fire and Forget" save.
     * If this fails, we don't want to stop the user from getting their plan.
     */
    private Mono<AiInteraction> persistAiInteractionToDb(String customerId, RetentionPlan plan) {
        try {
            String jsonStr = objectMapper.writeValueAsString(plan);
            AiInteraction interaction = new AiInteraction(
                    null, customerId, "AI Generated Plan", jsonStr, LocalDateTime.now()
            );

            return externalApiService.saveToApi1(interaction)
                    .doOnSuccess(s -> log.info("Persisted AI recommendation for customerId={}", customerId))
                    .doOnError(e -> log.error("Failed to save to API 1: {}", e.getMessage()))
                    // Return empty instead of error so the stream continues
                    .onErrorResume(e -> Mono.empty());
        } catch (Exception e) {
            log.error("Serialization error: {}", e.getMessage());
            return Mono.empty();
        }
    }


    public Mono<RetentionPlan> analyzeCustomerWithAI(CustomerProfile profile) {

        String prompt = """
                You are an API that returns ONLY valid JSON. No markdown. No extra text.
                
                Return JSON strictly matching this structure:
                {
                  "riskLevel": "LOW|MEDIUM|HIGH",
                  "reasoning": ["string"],
                  "actions": [
                    { "title": "string", "details": "string", "priority": "HIGH|MEDIUM|LOW" }
                  ],
                  "offer": null or {
                    "type": "DISCOUNT|UPGRADE|SUPPORT|NONE",
                    "description": "string",
                    "discountPercent": number or null,
                    "durationMonths": number or null
                  }
                }
                
                Rules:
                - Use ONLY the input fields below.
                - Do NOT invent discount/coupon codes.
                - Enum values must be UPPERCASE exactly as shown.
                
                INPUT:
                age=%s
                tenure=%s
                monthlyCharges=%s
                contract=%s
                techSupport=%s
                internetService=%s
                paymentMethod=%s
                """.formatted(
                profile.age(),
                profile.tenure(),
                profile.monthlyCharges(),
                profile.contract(),
                profile.techSupport(),
                profile.internetService(),
                profile.paymentMethod()
        );

        log.info("Generated prompt for customerId={} (len={})", profile.customerId(), prompt.length());

        return Mono.fromCallable(() -> chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content()
                )
                .subscribeOn(Schedulers.boundedElastic())
                .map(this::extractJson)
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, RetentionPlan.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Invalid JSON returned by model: " + json, e);
                    }
                });
    }


    private String extractJson(String text) {
        if (text == null) return null;

        String trimmed = text.trim();

        // Remove ```json ... ``` or ``` ... ```
        if (trimmed.startsWith("```")) {
            // remove starting ```
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
            // remove ending ```
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
    }
}

        /*var retentionPlan = chatClient.prompt()
                .user(u -> u.text(userPrompt)
                        .param("age", String.valueOf(profile.age()))
                        .param("tenure", String.valueOf(profile.tenure()))
                        .param("charges", String.valueOf(profile.monthlyCharges()))
                        .param("contract", profile.contract())
                        .param("techSupport", profile.techSupport()))
                .call()
                .entity(RetentionPlan.class);

        return Mono.just(retentionPlan);*/