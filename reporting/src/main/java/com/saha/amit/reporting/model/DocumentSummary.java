package com.saha.amit.reporting.model;

import java.time.Instant;

public record DocumentSummary(
        Long id,
        String fileName,
        String documentType,
        String contentType,
        Instant createdAt
) {}
