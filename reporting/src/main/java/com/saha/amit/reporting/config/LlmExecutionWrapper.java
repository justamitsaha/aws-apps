package com.saha.amit.reporting.config;

import reactor.core.publisher.Mono;

/**
 * Interface for LLM execution wrappers.
 * Defines the contract for executing prompts with cross-cutting concerns.
 */
public interface LlmExecutionWrapper {

    /**
     * Executes the provided user prompt.
     */
    Mono<String> execute(String userPrompt);

    /**
     * Executes the provided prompt with an optional system prompt.
     */
    Mono<String> execute(String systemPrompt, String userPrompt);

    /**
     * Returns the provider name for this wrapper.
     */
    String getProvider();
}
