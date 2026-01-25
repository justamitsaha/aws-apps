package com.saha.amit.reporting.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("documents")
public record DocumentEntity(
        @Id Long id,
        String fileName,
        LocalDateTime createdAt
) {}
