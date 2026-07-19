package com.saha.amit.reporting.model;

public record Citation(
        String fileName,
        Integer chunkIndex,
        Double score
) {}
