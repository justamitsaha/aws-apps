package com.saha.amit.reporting.model;


/*
Why this matters
    You control what the AI sees
    You avoid leaking unnecessary data
    You can evolve prompts safely
 */
import java.math.BigDecimal;

public record CustomerProfile(
        Long customerId,

        // Demographics
        Integer age,
        String gender,
        String geography,
        Boolean seniorCitizen,

        // Banking profile (from CustomerEntity)
        Integer creditScore,
        Integer numOfProducts,
        Boolean hasCrCard,
        Boolean isActiveMember,
        BigDecimal estimatedSalary,

        // Relationship
        Integer tenure,

        // Services (from CustomerChurnEntity)
        String internetService,
        String techSupport,
        String onlineSecurity,
        String deviceProtection,
        String streamingTv,
        String streamingMovies,

        // Contract + billing
        String contract,
        Boolean paperlessBilling,
        String paymentMethod,

        // Financial
        BigDecimal monthlyCharges,
        BigDecimal totalCharges,
        BigDecimal balance,

        // Optional label (only if you want training/testing)
        Boolean churn
) {}

