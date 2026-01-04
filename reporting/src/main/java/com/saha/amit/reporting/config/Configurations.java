package com.saha.amit.reporting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class Configurations {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
