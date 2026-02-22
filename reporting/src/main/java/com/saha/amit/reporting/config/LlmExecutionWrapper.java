package com.saha.amit.reporting.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Executes an LLM prompt with cross-cutting concerns:
 * - blocking isolation (boundedElastic)
 * - latency tracking and request tracing (UUID)
 * - token usage logging
 * - resilience (retries + timeouts)
 */
@Slf4j
@Component
public class LlmExecutionWrapper {

    private final ChatClient chatClient;

    public LlmExecutionWrapper(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * Executes the provided user prompt and returns the result as a String.
     */
    public Mono<String> execute(String userPrompt) {
        return execute(null, userPrompt);
    }

    /**
     * Executes the provided prompt with an optional system prompt and returns the result as a String.
     */
    public Mono<String> execute(String systemPrompt, String userPrompt) {
        return Mono.defer(() -> {
            String promptId = UUID.randomUUID().toString();
            long startTime = System.currentTimeMillis();
            
            log.info("LLM START promptId={} | hasSystemPrompt={} | userPrompt={}", promptId, systemPrompt != null, userPrompt);
            
            return Mono.fromCallable(() -> {
                            var spec = chatClient.prompt();
                            if (systemPrompt != null && !systemPrompt.isBlank()) {
                                spec.system(systemPrompt);
                                log.info("LLM SYSTEM PROMPT promptId={} systemPrompt={}", promptId, systemPrompt);
                            }
                            return spec.user(userPrompt)
                                    .call()
                                    .chatResponse();
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .timeout(Duration.ofSeconds(30))
                    .retryWhen(
                            Retry.backoff(2, Duration.ofSeconds(2))
                                    .filter(this::isRetryableError)
                    )
                    .map(response -> {
                        long latency = System.currentTimeMillis() - startTime;
                        log.info("LLM SUCCESS promptId={} latency={}ms", promptId, latency);
                        logTokenUsage(response, promptId);
                        
                        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                            return "";
                        }
                        return response.getResult().getOutput().getText();
                    })
                    .doOnError(ex -> log.error("LLM FAILURE promptId={}: {}", promptId, ex.getMessage()));
        });
    }

    private void logTokenUsage(ChatResponse response, String promptId) {
        if (response == null || response.getMetadata() == null) return;
        
        var metadata = response.getMetadata();
        var usage = metadata.getUsage();
        
        if (usage != null) {
            log.info(
                    "LLM TOKENS promptId={} prompt={} completion={} total={}",
                    promptId,
                    usage.getPromptTokens(),
                    usage.getCompletionTokens(),
                    usage.getTotalTokens()
            );
        }
    }

    private boolean isRetryableError(Throwable t) {
        return t instanceof IOException
                || t instanceof TimeoutException
                || (t.getMessage() != null &&
                (t.getMessage().contains("503") || t.getMessage().contains("rate limit")));
    }
}
