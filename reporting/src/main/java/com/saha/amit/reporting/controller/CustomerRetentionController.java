package com.saha.amit.reporting.controller;

import com.saha.amit.reporting.model.Chunk;
import com.saha.amit.reporting.model.ChunkMatch;
import com.saha.amit.reporting.model.RetentionPlan;
import com.saha.amit.reporting.service.ExternalApiService;
import com.saha.amit.reporting.service.RagChunkService;
import com.saha.amit.reporting.service.RagIngestService;
import com.saha.amit.reporting.service.RetentionAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/retention")
public class CustomerRetentionController {

    private final RagChunkService ragChunkService;
    private final RagIngestService ragIngestService;
    private final RetentionAiService retentionAiService;
    private final ExternalApiService externalApiService;

    public CustomerRetentionController(RagChunkService ragChunkService, RagIngestService ragIngestService, RetentionAiService retentionAiService, ExternalApiService externalApiService) {
        this.ragChunkService = ragChunkService;
        this.ragIngestService = ragIngestService;
        this.retentionAiService = retentionAiService;
        this.externalApiService = externalApiService;
    }

    /**
     * Analyze customer retention plan using AI service. Will check for cached result first.
     * if not found in cache, will call AI service.
     * Example: curl -X GET "http://localhost:8081/retention/123/analyze"
     */
    @CrossOrigin(origins = "http://localhost:8080")
    @GetMapping("/{id}/analyze")
    public Mono<ResponseEntity<RetentionPlan>> analyze(@PathVariable Long id) {
        return retentionAiService.analyzeCustomer(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Analyze customer retention plan using AI service without cache.
     * Example: curl -X GET "http://localhost:8081/retention/123/analyze/nocache"
     */
    @CrossOrigin(origins = "http://localhost:8080")
    @GetMapping("/{id}/analyze/nocache")
    public Mono<ResponseEntity<RetentionPlan>> analyzeNoCache(@PathVariable Long id) {
        return retentionAiService.analyzeCustomerWithoutCache(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Upload a policy related file and return the chunks of that file.
     * Request must be multipart/form-data with key "file"
     * Example: curl -X POST http://localhost:8081/retention/policyUpload -F "file=@offers_catalog.md"
     */
    @CrossOrigin(origins = "http://localhost:8080")
    @PostMapping(value = "/policyUpload", consumes = "multipart/form-data")
    public Mono<ResponseEntity<List<Chunk>>> upload(@RequestPart("file") FilePart filePart) {
        return ragChunkService.chunkUploadedFile(filePart)
                .flatMap(chunks ->
                        // save to DB using the same filePart OR using chunks (recommended)
                        ragIngestService.ingestChunks(filePart.filename(), chunks)
                                .thenReturn(ResponseEntity.ok(chunks))
                );
    }

    /**
     * Search the ingested policy documents for relevant chunks.
     * Example: curl -X GET "http://localhost:8081/retention/policySearch?q=discount+offers&topK=3"
     */
    @CrossOrigin(origins = "http://localhost:8080")
    @GetMapping("/policySearch")
    public Flux<ChunkMatch> search(
            @RequestParam("q") String q,
            @RequestParam(name = "topK", defaultValue = "5") int topK
    ) {
        return ragIngestService.search(q, topK, true);
    }


    /**
     * Analyze customer retention plan using RAG approach. Uses ingested policy documents to
     * provide context to AI service.
     * Example: curl -X GET "http://localhost:8081/retention/123/analyze/rag"
     */
    @CrossOrigin(origins = "http://localhost:8080")
    @GetMapping("/{id}/analyze/rag")
    public Mono<ResponseEntity<RetentionPlan>> analyzeWithRag(@PathVariable Long id) {
        return externalApiService.fetchCustomerProfile(id)
                .flatMap(retentionAiService::analyzeCustomerWithRAg)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }


}


