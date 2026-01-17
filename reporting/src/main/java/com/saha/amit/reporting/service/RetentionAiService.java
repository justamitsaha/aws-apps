package com.saha.amit.reporting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saha.amit.reporting.model.CustomerProfile;
import com.saha.amit.reporting.model.RetentionPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

    public Mono<RetentionPlan> analyze(Long id) {
        return externalApiService.fetchCustomerProfile(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Customer profile not found for id: " + id)))
                .flatMap(this::analyzeCustomer)
                .doOnError(error -> {
                    System.err.println("Error during analysis: " + error.getMessage());
                });
    }


    public Mono<RetentionPlan> analyzeCustomer(CustomerProfile profile) {

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

        log.info("Generated prompt for AI: {}", prompt);

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