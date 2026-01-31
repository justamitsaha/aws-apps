package com.saha.amit.reporting.controller;


import com.saha.amit.reporting.model.Chunk;
import com.saha.amit.reporting.model.ChunkMatch;
import com.saha.amit.reporting.model.RetentionPlan;
import com.saha.amit.reporting.service.ExternalApiService;
import com.saha.amit.reporting.service.RagChunkService;
import com.saha.amit.reporting.service.RagIngestService;
import com.saha.amit.reporting.service.RetentionAiService;
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
    private final RagChunkService ragChunkService;
    private final RagIngestService ragIngestService;

    @GetMapping("/health")
    public Mono<ResponseEntity<String>> healthCheck() {
        return externalApiService.fetchExternalData("/upload/health")
                .map(ResponseEntity::ok) // Wrap data in 200 OK
                .defaultIfEmpty(ResponseEntity.notFound().build()); // Handle empty case
    }


    /**
     * Upload a RAG document and return the chunks as they are processed. This is not related to customer retention.
     * But general RAG document ingestion endpoint.
     * Request must be multipart/form-data with key "file"
     * Example: curl -X POST http://localhost:8081/rag/upload/rag -F "file=@sample_rag_doc.md"
     */
    @CrossOrigin(origins = "http://localhost:8080")
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<Chunk> chunkUploadedFileRag(@RequestPart("file") FilePart filePart) {
        log.info("Received file: {}", filePart.filename());
        return ragIngestService.saveDocument(filePart.filename())
                .flatMapMany(documentId ->
                        ragChunkService.chunkUploadedFileRag(filePart)
                                .flatMap(chunk ->
                                        ragIngestService
                                                .saveChunk(documentId, chunk)
                                                .thenReturn(chunk) // emit only after persistence
                                )
                );
    }


    /**
     * Search the ingested policy documents for relevant chunks.
     * Example: curl -X GET "http://localhost:8081/retention/policySearch?q=discount+offers&topK=3"
     */
    @CrossOrigin(origins = "http://localhost:8080")
    @GetMapping("/search")
    public Flux<ChunkMatch> ragSearch(
            @RequestParam("q") String q,
            @RequestParam(name = "topK", defaultValue = "5") int topK
    ) {
        return ragIngestService.search(q, topK, false);
    }
}
