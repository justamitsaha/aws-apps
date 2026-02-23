package com.saha.amit.reporting.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * OpenAI-specific LLM execution wrapper.
 */
@Slf4j
@Component
@Primary
public class OpenAiExecutionWrapper implements LlmExecutionWrapper {

    private final ChatClient chatClient;

    public OpenAiExecutionWrapper(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Mono<String> execute(String userPrompt) {
        return execute(null, userPrompt);
    }

    @Override
    public Mono<String> execute(String systemPrompt, String userPrompt) {
        return Mono.defer(() -> {
            String promptId = UUID.randomUUID().toString();
            long startTime = System.currentTimeMillis();
            log.info("OPENAI START promptId={} hasSystemPrompt={}", promptId, systemPrompt != null);

            return Mono.fromCallable(() -> {
                        var spec = chatClient.prompt();
                        if (systemPrompt != null && !systemPrompt.isBlank()) {
                            spec.system(systemPrompt);
                        }
                        return spec.user(userPrompt).call().chatResponse();
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .timeout(Duration.ofSeconds(30))
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(2)).filter(this::isRetryableError))
                    .map(response -> {
                        long latency = System.currentTimeMillis() - startTime;
                        log.info("OPENAI SUCCESS promptId={} latency={}ms", promptId, latency);
                        logTokenUsage(response, promptId);
                        
                        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                            return "";
                        }
                        return response.getResult().getOutput().getText();
                    })
                    .doOnError(ex -> log.error("OPENAI FAILURE promptId={}: {}", promptId, ex.getMessage()));
        });
    }

    @Override
    public String getProvider() {
        return "openai";
    }

    private void logTokenUsage(ChatResponse response, String promptId) {
        if (response == null || response.getMetadata() == null) return;
        var metadata = response.getMetadata();
        var usage = metadata.getUsage();
        if (usage != null) {
            log.info("OPENAI TOKENS promptId={} prompt={} completion={} total={}",
                    promptId, usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
        }
    }

    private boolean isRetryableError(Throwable t) {
        return t instanceof IOException || t instanceof TimeoutException ||
                (t.getMessage() != null && (t.getMessage().contains("503") || t.getMessage().contains("rate limit")));
    }


    public Flux<String> stream(String prompt) {

        String promptId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();

        log.info("LLM STREAM START promptId={}", promptId);

        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content()          // Flux<String>

                .doOnNext(token ->
                        log.debug("LLM TOKEN promptId={} token={}", promptId, token)
                )

                .doOnComplete(() -> {
                    long latency = System.currentTimeMillis() - start;
                    log.info(
                            "LLM STREAM COMPLETE promptId={} latency={}ms",
                            promptId,
                            latency
                    );
                })

                .doOnError(e ->
                        log.error("LLM STREAM FAILURE promptId={}", promptId, e)
                );
    }

}
