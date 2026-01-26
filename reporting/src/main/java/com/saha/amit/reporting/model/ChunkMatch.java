package com.saha.amit.reporting.model;

public record ChunkMatch(
        Long chunkId,
        Long documentId,
        String fileName,
        Integer chunkIndex,
        String chunkText,
        Double score
) {}

