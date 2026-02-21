package com.saha.amit.reporting.service;

import com.saha.amit.reporting.model.Chunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service dedicated to chunking documents into smaller, overlapping segments.
 * This is a crucial step for generating vector embeddings and performing RAG retrieval.
 */
@Slf4j
@Service
public class DocumentChunkingService {

    @Value("${rag.chunk.size:1000}")
    private int chunkSize;

    @Value("${rag.chunk.overlap:200}")
    private int overlap;

    /**
     * Reads the entire content of an uploaded file and splits it into chunks.
     */
    public Mono<List<Chunk>> chunkUploadedFile(FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .map(this::chunkText);
    }

    /**
     * Chunks text incrementally from a file stream (SSE-friendly).
     */
    public Flux<Chunk> chunkUploadedFileStreaming(FilePart filePart) {
        StringDecoder decoder = StringDecoder.allMimeTypes();

        return decoder
                .decode(filePart.content(), ResolvableType.forClass(String.class), null, Collections.emptyMap())
                .scan(new ChunkState(), (state, segment) -> {
                    state.readyToEmit.clear();
                    state.buffer.append(segment);

                    while (state.buffer.length() >= chunkSize) {
                        int end = calculateEnd(state.buffer.toString());
                        String chunkText = state.buffer.substring(0, end).trim();
                        if (!chunkText.isEmpty()) {
                            state.readyToEmit.add(new Chunk(state.lastIndex++, chunkText));
                        }
                        int keepFrom = Math.max(0, end - overlap);
                        state.buffer.delete(0, keepFrom);
                    }
                    return state;
                })
                .flatMapIterable(state -> state.readyToEmit)
                .concatWith(Flux.defer(() -> {
                    ChunkState state = ChunkState.last();
                    if (state != null && !state.buffer.isEmpty()) {
                        String finalChunk = state.buffer.toString().trim();
                        if (!finalChunk.isEmpty()) {
                            return Flux.just(new Chunk(state.lastIndex, finalChunk));
                        }
                    }
                    return Flux.empty();
                }))
                .doOnNext(chunk -> log.info("Emitting chunk index={} size={}", chunk.index(), chunk.text().length()));
    }

    private List<Chunk> chunkText(String text) {
        text = text.trim();
        if (text.isEmpty()) return List.of();

        List<Chunk> chunks = new ArrayList<>();
        int start = 0;
        int index = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            int lastNewLine = text.lastIndexOf('\n', end);

            if (lastNewLine > start + 200) {
                end = lastNewLine;
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(new Chunk(index++, chunk));
            }

            if (end == text.length()) break;
            start = Math.max(end - overlap, 0);
        }
        return chunks;
    }

    private int calculateEnd(String text) {
        int end = Math.min(chunkSize, text.length());
        int lastNewLine = text.lastIndexOf('\n', end);
        if (lastNewLine > (chunkSize - 200)) {
            return lastNewLine;
        }
        return end;
    }

    private static class ChunkState {
        static ChunkState LAST;
        StringBuilder buffer = new StringBuilder();
        int lastIndex = 0;
        List<Chunk> readyToEmit = new ArrayList<>();

        ChunkState() {
            LAST = this;
        }

        static ChunkState last() {
            return LAST;
        }
    }
}
