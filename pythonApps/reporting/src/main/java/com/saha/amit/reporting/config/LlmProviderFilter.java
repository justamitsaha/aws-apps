package com.saha.amit.reporting.config;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Filter to extract the 'x-llm-provider' header and put it into the Reactor context.
 */
@Component
public class LlmProviderFilter implements WebFilter {

    public static final String CONTEXT_KEY = "llm-provider";
    public static final String HEADER_NAME = "x-llm-provider";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String provider = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        if (provider == null || provider.isBlank()) {
            provider = "openai";
        }
        
        final String finalProvider = provider;
        return chain.filter(exchange)
                .contextWrite(Context.of(CONTEXT_KEY, finalProvider));
    }
}
