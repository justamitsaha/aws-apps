package com.saha.amit.reporting.service;

import com.saha.amit.reporting.model.*;
import com.saha.amit.reporting.repository.RagIngestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Retrieval-Augmented Generation (RAG) operations.
 * Handles document and chunk persistence, semantic search, and document-specific Q&A coordination.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final RagIngestRepository repository;
    private final EmbeddingService embeddingService;
    private final LlmService llmService;

    public Mono<Void> ingestChunks(String fileName, List<Chunk> chunks) {
        return repository.insertDocument(fileName, "RETENTION_POLICY", "text/markdown")
                .flatMapMany(documentId ->
                        Flux.fromIterable(chunks)
                                .concatMap(chunk -> saveChunk(documentId, chunk)
                                        .onErrorResume(ex -> {
                                            log.error("Failed to save chunk {} for file {}: {}", chunk.index(), fileName, ex.getMessage());
                                            return Mono.empty();
                                        }))
                )
                .then();
    }

    public Mono<Long> saveDocument(String fileName) {
        return repository.insertDocument(fileName, "RAG_DOCUMENT", "text/markdown");
    }

    public Mono<Void> saveChunk(Long documentId, Chunk chunk) {
        return embeddingService.embedAsync(chunk.text())
                .map(embeddingService::toPgVectorLiteral)
                .flatMap(literal ->
                        repository.insertChunk(
                                documentId,
                                chunk.index(),
                                chunk.text(),
                                literal
                        )
                )
                .then();
    }

    public Flux<ChunkMatch> search(String query, int topK, boolean isRetentionPolicy) {
        String documentType = isRetentionPolicy ? "RETENTION_POLICY" : "RAG_DOCUMENT";
        return embeddingService.embedAsync(query)
                .map(embeddingService::toPgVectorLiteral)
                .flatMapMany(literal -> repository.searchSimilarChunks(literal, topK, documentType));
    }

    public Flux<DocumentSummary> getAllDocuments() {
        return repository.findAllDocuments();
    }

    /**
     * Answers a question based on a specific document by performing semantic search
     * to find relevant context and then calling the LLM.
     */
    public Mono<DocumentAnswer> askDocument(Long documentId, String question) {
        return embeddingService.embedAsync(question)
                .map(embeddingService::toPgVectorLiteral)
                .flatMapMany(vector -> repository.searchChunksByDocument(documentId, vector, 5))
                .collectList()
                .flatMap(chunks -> {
                    if (chunks.isEmpty()) {
                        return Mono.just(new DocumentAnswer("Not found in the document", List.of()));
                    }

                    String context = chunks.stream()
                            .map(ChunkMatch::chunkText)
                            .collect(Collectors.joining("\n\n"));

                    return llmService.askQuestion(question, context)
                            .map(answer -> new DocumentAnswer(
                                    answer,
                                    chunks.stream()
                                            .map(c -> new SourceChunk(c.chunkIndex(), c.score()))
                                            .collect(Collectors.toList())
                            ));
                });
    }
}
