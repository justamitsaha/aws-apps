package com.saha.amit.reporting.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("document_chunks")
public record DocumentChunkEntity(
        @Id Long id,
        Long documentId,
        Integer chunkIndex,
        String chunkText,
        String embedding, // store as text like: [0.1,0.2,...]
        LocalDateTime createdAt
) {}
