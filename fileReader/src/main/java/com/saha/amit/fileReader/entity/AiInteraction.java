package com.saha.amit.fileReader.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table("ai_interactions")
public record AiInteraction(
        @Id Long id,
        String customerId,
        String rawPrompt,
        String aiResponse, // Stored as String (JSON) in DB
        LocalDateTime createdAt
) {}