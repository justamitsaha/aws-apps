package com.saha.amit.reporting.model;


/*
Why this matters
    You control what the AI sees
    You avoid leaking unnecessary data
    You can evolve prompts safely
 */

public record CustomerProfile(
        Long customerId,

        // Demographics
        int age,
        String gender,
        String geography,
        boolean seniorCitizen,

        // Relationship
        int tenure,
        boolean isActiveMember,

        // Services
        String internetService,
        String techSupport,
        String streamingTv,

        // Financial
        double monthlyCharges,
        double totalCharges,
        double balance,

        // Contract
        String contract,
        String paymentMethod
) {}

