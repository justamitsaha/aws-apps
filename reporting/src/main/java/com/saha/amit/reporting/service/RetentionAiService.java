package com.saha.amit.reporting.service;

import com.saha.amit.reporting.model.CustomerProfile;
import com.saha.amit.reporting.model.RetentionPlan;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class RetentionAiService {

    private final ChatClient chatClient;

    private final ExternalApiService externalApiService;

    public RetentionAiService(ChatClient.Builder builder, ExternalApiService externalApiService) {
        this.chatClient = builder.build();
        this.externalApiService = externalApiService;
    }

    public Mono<RetentionPlan> analyze(Long id) {
        return externalApiService.fetchCustomerProfile(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Customer profile not found for id: " + id)))
                .flatMap(this::analyzeCustomer)
                .doOnError(error -> {
                    System.err.println("Error during analysis: " + error.getMessage());
                });
    }


    public Mono<RetentionPlan> analyzeCustomer(CustomerProfile profile) {
        // 1. Define the Prompt Template
        String userPrompt = """
                Analyze the following customer profile for churn risk:
                - Age: {age}
                - Tenure: {tenure} months
                - Monthly Charges: {charges}
                - Contract: {contract}
                - Tech Support: {techSupport}
                
                Based on this data, provide a retention strategy.
                """;

        return Mono.fromCallable(() -> chatClient.prompt()
                .user(u -> u.text(userPrompt)
                        .param("age", String.valueOf(profile.age()))
                        .param("tenure", String.valueOf(profile.tenure()))
                        .param("charges", String.valueOf(profile.monthlyCharges()))
                        .param("contract", profile.contract())
                        .param("techSupport", profile.techSupport()))
                .call()
                .entity(RetentionPlan.class)
        ).subscribeOn(Schedulers.boundedElastic());
    }
}

        /*var retentionPlan = chatClient.prompt()
                .user(u -> u.text(userPrompt)
                        .param("age", String.valueOf(profile.age()))
                        .param("tenure", String.valueOf(profile.tenure()))
                        .param("charges", String.valueOf(profile.monthlyCharges()))
                        .param("contract", profile.contract())
                        .param("techSupport", profile.techSupport()))
                .call()
                .entity(RetentionPlan.class);

        return Mono.just(retentionPlan);*/