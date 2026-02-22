package com.saha.amit.reporting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saha.amit.reporting.config.LlmExecutionWrapper;
import com.saha.amit.reporting.model.CustomerProfile;
import com.saha.amit.reporting.model.RetentionPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Service dedicated to Large Language Model (LLM) interactions.
 * Handles prompt construction, model calls via a wrapper, and JSON parsing of AI responses.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final LlmExecutionWrapper llmExecutionWrapper;
    private final ObjectMapper objectMapper;

    /**
     * Generates a retention plan for a customer without additional context.
     */
    public Mono<RetentionPlan> generateRetentionPlan(CustomerProfile profile) {
        String prompt = buildRetentionPrompt(profile, null);
        return executeAndParse(prompt);
    }

    /**
     * Generates a retention plan for a customer grounded in provided policy context.
     */
    public Mono<RetentionPlan> generateRetentionPlanWithContext(CustomerProfile profile, String policyContext) {
        String prompt = buildRetentionPrompt(profile, policyContext);
        return executeAndParse(prompt);
    }

    /**
     * Answers a question based on the provided document context.
     */
    public Mono<String> askQuestion(String question, String context) {
        String prompt = """
                Document context:
                %s
                
                Question:
                %s
                """.formatted(context, question);

        return llmExecutionWrapper.execute(prompt);
    }

    private String buildRetentionPrompt(CustomerProfile profile, String policyContext) {
        String basePrompt = """
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
                - Use ONLY the input fields below%s.
                - Do NOT invent discount/coupon codes.
                - Enum values must be UPPERCASE exactly as shown.
                %s
                
                INPUT:
                age=%s
                tenure=%s
                monthlyCharges=%s
                contract=%s
                techSupport=%s
                internetService=%s
                paymentMethod=%s
                """;

        String contextHeader = (policyContext != null) ? " + POLICY CONTEXT" : "";
        String contextRule = (policyContext != null) ? "- If POLICY CONTEXT does not support an offer, set offer.type = \"NONE\"." : "";
        
        String prompt = basePrompt.formatted(
                contextHeader,
                contextRule,
                profile.age(),
                profile.tenure(),
                profile.monthlyCharges(),
                profile.contract(),
                profile.techSupport(),
                profile.internetService(),
                profile.paymentMethod()
        );

        if (policyContext != null) {
            prompt += "\nPOLICY CONTEXT (retrieved from company docs):\n" + policyContext;
        }

        return prompt;
    }

    /**
     * Executes the LLM call using the wrapper and handles parsing + business-level fallbacks.
     */
    private Mono<RetentionPlan> executeAndParse(String prompt) {
        return llmExecutionWrapper.execute(prompt)
                .map(this::extractJson)
                .flatMap(this::parseRetentionPlan)
                .doOnSuccess(plan -> log.debug("Retention plan generated successfully"))
                .onErrorResume(ex -> {
                    log.error("LLM retention plan generation failed, falling back to default. Error: {}", ex.getMessage());
                    return Mono.just(defaultRetentionPlan());
                });
    }

    private String extractJson(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
    }

    private Mono<RetentionPlan> parseRetentionPlan(String json) {
        try {
            return Mono.just(objectMapper.readValue(json, RetentionPlan.class));
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("Invalid JSON returned by model: " + json, e));
        }
    }

    private RetentionPlan defaultRetentionPlan() {
        return new RetentionPlan(
                RetentionPlan.RiskLevel.MEDIUM,
                List.of("Unable to generate plan due to technical issues. Please try again later."),
                List.of(),
                null,
                null);
    }
}
