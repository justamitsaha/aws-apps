package com.saha.amit.reporting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/*
 * This configuration class is needed to fix the issue when using Spring WebFlux.
 * WebFlux don’t provide RestClient.Builder. So we need to define our own Bean for it.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}

