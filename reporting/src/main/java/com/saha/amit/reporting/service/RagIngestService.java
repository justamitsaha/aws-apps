package com.saha.amit.reporting.service;

import com.saha.amit.reporting.model.*;
import com.saha.amit.reporting.repository.RagIngestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RagIngestService {

    private final RagIngestRepository repository;
    private final EmbeddingService embeddingService;
    private final ChatClient chatClient;

    public RagIngestService(RagIngestRepository repository,
                            EmbeddingService embeddingService, ChatClient.Builder builder) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.chatClient = builder.build();
    }

    public Mono<Void> ingestChunks(String fileName, List<Chunk> chunks) {
        return repository.insertDocument(fileName, "RETENTION_POLICY", "text/markdown")
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

    public Flux<ChunkMatch> search(String query, int topK, boolean docTypeFlag) {
        return embeddingService.embedAsync(query)
                .map(embeddingService::toPgVectorLiteral)
                .flatMapMany(literal -> repository.searchSimilarChunks(literal, topK, docTypeFlag ? "RETENTION_POLICY" : "RAG_DOCUMENT"));
    }

//    public Flux<ChunkMatch> ragSearch(String query, int topK, boolean docTypeFlag) {
//        return embeddingService.embedAsync(query)
//                .map(embeddingService::toPgVectorLiteral)
//                .flatMapMany(literal -> repository.searchSimilarChunks(literal, topK, docTypeFlag ? "RETENTION_POLICY" : "RAG_DOCUMENT"));
//    }

    public Flux<DocumentSummary> getAllDocuments() {
        return repository.findAllDocuments();
    }

    public Mono<DocumentAnswer> askDocument(Long documentId, String question) {

        return embeddingService.embedAsync(question)
                .map(embeddingService::toPgVectorLiteral)
                .flatMapMany(vector ->
                        repository.searchChunksByDocument(documentId, vector, 5)
                )
                .collectList()
                .flatMap(chunks -> {
                    // 1. No relevant chunks → deterministic response
                    if (chunks.isEmpty()) {
                        return Mono.just(
                                new DocumentAnswer("Not found in the document", List.of())
                        );
                    }

                    // 2. Build context for the LLM
                    String context = chunks.stream()
                            .map(ChunkMatch::chunkText)
                            .collect(Collectors.joining("\n\n"));

                    String prompt = """
                            Document context:
                            %s
                            
                            Question:
                            %s
                            """.formatted(context, question);

                    // 3. Call LLM
                    return Mono.fromCallable(() ->
                                    chatClient.prompt()
                                            .system("""
                                                    You answer ONLY using the provided document context.
                                                    If the answer is not present, say "Not found in the document."
                                                    """)
                                            .user(prompt)
                                            .call()
                                            .content()
                            )
                            .subscribeOn(Schedulers.boundedElastic())
                            // 4. Attach provenance
                            .map(answer -> new DocumentAnswer(
                                    answer,
                                    chunks.stream()
                                            .map(c ->
                                                 new SourceChunk(
                                                        c.chunkIndex(),
                                                        c.score()
                                                )
                                            )
                                            .collect(Collectors.toList())
                            ));
                });
    }

}

