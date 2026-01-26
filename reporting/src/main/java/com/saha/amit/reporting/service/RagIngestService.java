package com.saha.amit.reporting.service;

import com.saha.amit.reporting.model.Chunk;
import com.saha.amit.reporting.model.ChunkMatch;
import com.saha.amit.reporting.repository.RagIngestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
public class RagIngestService {

    private final RagIngestRepository repository;
    private final EmbeddingService embeddingService;

    public RagIngestService(RagIngestRepository repository,
                            EmbeddingService embeddingService) {
        this.repository = repository;
        this.embeddingService = embeddingService;
    }

    public Mono<Void> ingestChunks(String fileName, List<Chunk> chunks) {
        return repository.insertDocument(fileName)
                .flatMapMany(documentId ->
                        Flux.fromIterable(chunks)
                                .concatMap(chunk -> saveChunk(documentId, chunk)
                                        .onErrorResume(ex -> {
                                            log.error("Failed chunk {}", chunk.index(), ex);
                                            return Mono.empty();
                                        }))
                )
                .then();
    }

    private Mono<Void> saveChunk(Long documentId, Chunk chunk) {
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

    public Flux<ChunkMatch> search(String query, int topK) {
        return embeddingService.embedAsync(query)
                .map(embeddingService::toPgVectorLiteral)
                .flatMapMany(literal -> repository.searchSimilarChunks(literal, topK));
    }
}

