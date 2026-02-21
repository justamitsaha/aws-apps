package com.saha.amit.reporting.controller;


import com.saha.amit.reporting.model.*;
import com.saha.amit.reporting.service.ExternalApiService;
import com.saha.amit.reporting.service.DocumentChunkingService;
import com.saha.amit.reporting.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final ExternalApiService externalApiService;
    private final DocumentChunkingService documentChunkingService;
    private final RagService ragService;

    @GetMapping("/health")
    public Mono<ResponseEntity<String>> healthCheck() {
        return externalApiService.fetchExternalData("/upload/health")
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<Chunk> chunkUploadedFileRag(@RequestPart("file") FilePart filePart) {
        log.info("Received file: {}", filePart.filename());
        return ragService.saveDocument(filePart.filename())
                .flatMapMany(documentId ->
                        documentChunkingService.chunkUploadedFileStreaming(filePart)
                                .flatMap(chunk ->
                                        ragService.saveChunk(documentId, chunk)
                                                .thenReturn(chunk)
                                )
                );
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @GetMapping("/search")
    public Flux<ChunkMatch> ragSearch(
            @RequestParam("q") String q,
            @RequestParam(name = "topK", defaultValue = "5") int topK
    ) {
        return ragService.search(q, topK, false);
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @GetMapping("/documents")
    public Flux<DocumentSummary> getAllDocuments() {
        return ragService.getAllDocuments();
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @PostMapping("/{documentId}/ask")
    public Mono<ResponseEntity<DocumentAnswer>> askDocument(
            @PathVariable Long documentId,
            @RequestBody DocumentQuestionRequest request
    ) {
        if (request.question() == null || request.question().isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return ragService.askDocument(documentId, request.question())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
