package com.saha.amit.reporting.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory to select the appropriate LLM execution wrapper at runtime.
 */
@Component
@RequiredArgsConstructor
public class LlmProviderFactory {

    private final List<LlmExecutionWrapper> wrappers;

    /**
     * Returns the wrapper for the given provider name.
     * Defaults to OpenAI if not found or if provider is null.
     */
    public LlmExecutionWrapper getWrapper(String provider) {
        if (provider == null || provider.isBlank()) {
            provider = "openai";
        }
        
        String finalProvider = provider.toLowerCase();
        return wrappers.stream()
                .filter(w -> w.getProvider().equals(finalProvider))
                .findFirst()
                .orElseGet(() -> wrappers.stream()
                        .filter(w -> w.getProvider().equals("openai"))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No LLM wrappers found")));
    }
}
