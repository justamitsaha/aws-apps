package com.saha.amit.reporting.model;



import java.time.LocalDateTime;

public record AiInteraction(
        Long id,
        String customerId,
        String rawPrompt,
        String aiResponse, // Stored as String (JSON) in DB
        LocalDateTime createdAt
) {}