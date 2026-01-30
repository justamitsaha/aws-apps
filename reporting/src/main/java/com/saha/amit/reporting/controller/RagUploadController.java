package com.saha.amit.reporting.controller;

import com.saha.amit.reporting.model.Chunk;
import com.saha.amit.reporting.model.ChunkMatch;
import com.saha.amit.reporting.service.RagChunkService;
import com.saha.amit.reporting.service.RagIngestService;
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
@RequestMapping("/rag")
public class RagUploadController {

    private final RagChunkService ragChunkService;
    private final RagIngestService ragIngestService;

    public RagUploadController(RagChunkService ragChunkService, RagIngestService ragIngestService) {
        this.ragChunkService = ragChunkService;
        this.ragIngestService = ragIngestService;
    }


    /**
     * Upload a file and return the chunks of that file.
     * Request must be multipart/form-data with key "file"
     * Example: curl -X POST http://localhost:8081/rag/upload -F "file=@offers_catalog.md"
     */
    @CrossOrigin(origins = "http://localhost:8080")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Mono<ResponseEntity<List<Chunk>>> upload(@RequestPart("file") FilePart filePart) {
        return ragChunkService.chunkUploadedFile(filePart)
                .flatMap(chunks ->
                        // save to DB using the same filePart OR using chunks (recommended)
                        ragIngestService.ingestChunks(filePart.filename(), chunks)
                                .thenReturn(ResponseEntity.ok(chunks))
                );
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @GetMapping("/search")
    public Flux<ChunkMatch> search(
            @RequestParam("q") String q,
            @RequestParam(name = "topK", defaultValue = "5") int topK
    ) {
        return ragIngestService.search(q, topK);
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @PostMapping(
            value = "/upload/rag",
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

}


