package com.saha.amit.reporting.service;

import com.saha.amit.reporting.model.Chunk;
import com.saha.amit.reporting.repository.RagIngestRepository;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class RagIngestService {

    private final RagChunkService ragChunkService;
    private final RagIngestRepository repository;
    private final EmbeddingService embeddingService;

    public RagIngestService(RagChunkService ragChunkService,
                            RagIngestRepository repository,
                            EmbeddingService embeddingService) {
        this.ragChunkService = ragChunkService;
        this.repository = repository;
        this.embeddingService = embeddingService;
    }

    public Mono<Void> ingestChunks(String fileName, List<Chunk> chunks) {
        return repository.insertDocument(fileName)
                .flatMapMany(documentId ->
                        Flux.fromIterable(chunks)
                                .concatMap(chunk -> saveChunk(documentId, chunk))
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


//    public Mono<Long> ingest(FilePart filePart) {
//        String fileName = filePart.filename();
//
//        return repository.insertDocument(fileName)
//                .flatMap(documentId ->
//                        ragChunkService.chunkUploadedFile(filePart)
//                                .flatMapMany(Flux::fromIterable)
//                                .concatMap(chunk -> saveChunk(documentId, chunk))
//                                .then(Mono.just(documentId))
//                );
//    }
}

