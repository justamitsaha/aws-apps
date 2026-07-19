package com.saha.amit.reporting.controller;

import com.saha.amit.reporting.model.Chunk;
import com.saha.amit.reporting.model.ChunkMatch;
import com.saha.amit.reporting.model.RetentionPlan;
import com.saha.amit.reporting.service.ExternalApiService;
import com.saha.amit.reporting.service.DocumentChunkingService;
import com.saha.amit.reporting.service.RagService;
import com.saha.amit.reporting.service.RetentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/retention")
@RequiredArgsConstructor
public class CustomerRetentionController {

    private final RetentionService retentionService;
    private final DocumentChunkingService documentChunkingService;
    private final RagService ragService;
    private final ExternalApiService externalApiService;

    @GetMapping("/{id}/analyze")
    public Mono<ResponseEntity<RetentionPlan>> analyze(@PathVariable Long id) {
        return retentionService.analyzeCustomer(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/analyze/nocache")
    public Mono<ResponseEntity<RetentionPlan>> analyzeNoCache(@PathVariable Long id) {
        return retentionService.analyzeCustomerWithoutCache(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/policyUpload", consumes = "multipart/form-data")
    public Mono<ResponseEntity<List<Chunk>>> upload(@RequestPart("file") FilePart filePart) {
        return documentChunkingService.chunkUploadedFile(filePart)
                .flatMap(chunks ->
                        ragService.ingestChunks(filePart.filename(), chunks)
                                .thenReturn(ResponseEntity.ok(chunks))
                );
    }

    @GetMapping("/policySearch")
    public Flux<ChunkMatch> search(
            @RequestParam("q") String q,
            @RequestParam(name = "topK", defaultValue = "5") int topK
    ) {
        return ragService.search(q, topK, true);
    }

    @GetMapping("/{id}/analyze/rag")
    public Mono<ResponseEntity<RetentionPlan>> analyzeWithRag(@PathVariable Long id) {
        return externalApiService.fetchCustomerProfile(id)
                .flatMap(retentionService::analyzeCustomerWithRag)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}


