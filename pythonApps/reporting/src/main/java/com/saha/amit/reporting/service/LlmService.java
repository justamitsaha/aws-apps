package com.saha.amit.reporting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saha.amit.reporting.config.LlmExecutionWrapper;
import com.saha.amit.reporting.config.LlmProviderFactory;
import com.saha.amit.reporting.config.LlmProviderFilter;
import com.saha.amit.reporting.model.CustomerProfile;
import com.saha.amit.reporting.model.RetentionPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Service dedicated to Large Language Model (LLM) interactions.
 * Handles prompt construction, model calls via a dynamic provider factory, and JSON parsing of AI responses.
 */
@Slf4j
@Service
public class LlmService {

    private final ChatClient chatClient; // Kept for the reference method
    private final LlmProviderFactory llmProviderFactory;
    private final ObjectMapper objectMapper;

    public LlmService(ChatClient chatClient, LlmProviderFactory llmProviderFactory, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.llmProviderFactory = llmProviderFactory;
        this.objectMapper = objectMapper;
    }

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
     * Answers a question based on the provided document context with system prompt grounding.
     */
    public Mono<String> askQuestion(String question, String context) {
        String systemPrompt = """
                You answer ONLY using the provided document context.
                If the answer is not present, say "Not found in the document."
                """;
        
        String userPrompt = """
                Document context:
                %s
                
                Question:
                %s
                """.formatted(context, question);

        return selectWrapper()
                .flatMap(wrapper -> wrapper.execute(systemPrompt, userPrompt));
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
        return selectWrapper()
                .flatMap(wrapper -> wrapper.execute(prompt))
                .map(this::extractJson)
                .flatMap(this::parseRetentionPlan)
                .doOnSuccess(plan -> log.debug("Retention plan generated successfully"))
                .onErrorResume(ex -> {
                    log.error("LLM retention plan generation failed, falling back to default. Error: {}", ex.getMessage());
                    return Mono.just(defaultRetentionPlan());
                });
    }

    private Mono<LlmExecutionWrapper> selectWrapper() {
        return Mono.deferContextual(ctx -> {
            String provider = ctx.getOrDefault(LlmProviderFilter.CONTEXT_KEY, "openai");
            log.info("Runtime selected LLM provider: {}", provider);
            return Mono.just(llmProviderFactory.getWrapper(provider));
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

    // --- OLD IMPLEMENTATION KEPT FOR REFERENCE ONLY ---

    /**
     * Manual Reactor-based implementation for reference.
     * This logic is now encapsulated within the LlmExecutionWrapper for cross-cutting concerns.
     */
    @Deprecated(forRemoval = false)
    private Mono<RetentionPlan> callLlmAndParsePlan_Reference(String prompt) {
        return Mono.fromCallable(() -> chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content()
                )
                /*
                 * Runs the blocking LLM HTTP call on Reactor's boundedElastic pool.
                 * Prevents blocking the WebFlux event-loop threads.
                 */
                .subscribeOn(Schedulers.boundedElastic())
                /*
                 * Prevents requests from hanging indefinitely if the LLM
                 * provider is slow or unresponsive.
                 */
                .timeout(Duration.ofSeconds(30))
                /*
                 * Retries transient failures such as network glitches
                 * or temporary provider errors using exponential backoff.
                 */
                .retryWhen(
                        Retry.backoff(2, Duration.ofSeconds(2))
                                .filter(this::isRetryableError_Reference)
                )
                /*
                 * LLM responses may contain explanations or markdown.
                 * This step extracts only the valid JSON section.
                 */
                .map(this::extractJson)
                /*
                 * Converts JSON into the domain object using Jackson.
                 * flatMap is required because parsing returns Mono<RetentionPlan>.
                 */
                .flatMap(this::parseRetentionPlan)
                /*
                 * Observability hooks for tracing execution lifecycle.
                 */
                .doOnSubscribe(s -> log.debug("Calling LLM for retention plan"))
                .doOnSuccess(plan -> log.debug("Retention plan generated successfully"))
                .doOnError(e -> log.error("LLM retention plan generation failed", e))
                /*
                 * Fallback behavior if the entire pipeline fails.
                 * Ensures downstream services still receive a deterministic response.
                 */
                .onErrorResume(ex -> {
                    log.warn("Falling back to default retention plan due to error: {}", ex.getMessage());
                    return Mono.just(defaultRetentionPlan());
                });
    }

    private boolean isRetryableError_Reference(Throwable t) {
        return t instanceof IOException
                || t instanceof TimeoutException
                || (t.getMessage() != null &&
                (t.getMessage().contains("503") || t.getMessage().contains("rate limit")));
    }
}
