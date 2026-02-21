package com.saha.amit.reporting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saha.amit.reporting.model.CustomerProfile;
import com.saha.amit.reporting.model.DocumentAnswer;
import com.saha.amit.reporting.model.RetentionPlan;
import com.saha.amit.reporting.model.SourceChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Service dedicated to Large Language Model (LLM) interactions.
 * Handles prompt construction, model calls, and JSON parsing of AI responses.
 */
@Slf4j
@Service
public class LlmService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public LlmService(ChatClient.Builder builder, ObjectMapper objectMapper) {
        this.chatClient = builder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Generates a retention plan for a customer without additional context.
     */
    public Mono<RetentionPlan> generateRetentionPlan(CustomerProfile profile) {
        String prompt = buildRetentionPrompt(profile, null);
        return callLlmAndParsePlan(prompt);
    }

    /**
     * Generates a retention plan for a customer grounded in provided policy context.
     */
    public Mono<RetentionPlan> generateRetentionPlanWithContext(CustomerProfile profile, String policyContext) {
        String prompt = buildRetentionPrompt(profile, policyContext);
        return callLlmAndParsePlan(prompt);
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

        return Mono.fromCallable(() ->
                        chatClient.prompt()
                                .system("""
                                        You answer ONLY using the provided document context.
                                        If the answer is not present, say "Not found in the document."
                                        """)
                                .user(prompt)
                                .call()
                                .content()
                )
                .subscribeOn(Schedulers.boundedElastic());
    }

    private String buildRetentionPrompt(CustomerProfile profile, String policyContext) {
        log.info("Building LLM prompt for customerId={} with policy context={}", profile.customerId(), policyContext);
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
        log.info("Constructed LLM prompt for customerId={}: {}", profile.customerId(), prompt);
        return prompt;
    }

    private Mono<RetentionPlan> callLlmAndParsePlan(String prompt) {
        return Mono.fromCallable(() -> chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content()
                )
                .subscribeOn(Schedulers.boundedElastic())
                .map(this::extractJson)
                .flatMap(this::parseRetentionPlan);
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
}
