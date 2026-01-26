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
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class RagChunkService {

    /**
     * chunkSize = max characters per chunk.
     * If not configured in properties, default = 1000.
     */
    @Value("${rag.chunk.size:1000}")
    private int chunkSize;

    /**
     * overlap = how many characters from the end of previous chunk
     * should be repeated at the beginning of next chunk.
     * Default = 200.
     */
    @Value("${rag.chunk.overlap:200}")
    private int overlap;

    /**
     * Reads the uploaded file into a String and chunks it.
     */
    public Mono<List<Chunk>> chunkUploadedFile(FilePart filePart) {

        // filePart.content() gives Flux<DataBuffer> (stream of bytes)
        // DataBufferUtils.join(...) joins the stream into one DataBuffer
        return DataBufferUtils.join(filePart.content())
                .map(dataBuffer -> {

                    // Convert DataBuffer -> byte[]
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);

                    // Important: release buffer to avoid memory leak
                    DataBufferUtils.release(dataBuffer);

                    // Convert bytes -> String (UTF-8 text)
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                // Convert the file text into chunks
                .map(this::chunkText);
    }

    /**
     * Splits large text into smaller overlapping chunks.
     * This is useful for embeddings + RAG retrieval later.
     * Assume: chunkSize = 1000,  overlap = 200
     * Then:
     * Chunk 0 = chars 0 → 1000
     * Chunk 1 starts at 800 (1000 - 200)
     * Chunk 1 = chars 800 → 1800
     * Chunk 2 starts at 1600
     */
    private List<Chunk> chunkText(String text) {

        // Remove leading/trailing spaces
        text = text.trim();

        // If file is empty, return no chunks
        if (text.isEmpty()) return List.of();

        var chunks = new java.util.ArrayList<Chunk>();

        // start = index where the next chunk begins
        int start = 0;

        // chunk index counter
        int index = 0;

        while (start < text.length()) {

            // end = start + chunkSize (but not beyond text length)
            int end = Math.min(start + chunkSize, text.length());

            // Try to end the chunk at a newline for better readability.
            // This avoids cutting in the middle of a line/sentence.
            int lastNewLine = text.lastIndexOf('\n', end);

            // Only use the newline if it doesn't create a very small chunk
            // (example: don't create chunk of 50 chars)
            if (lastNewLine > start + 200) {
                end = lastNewLine;
            }

            // Extract chunk text
            String chunk = text.substring(start, end).trim();
            log.info("<--->");
            log.info("Created chunk index={} (len={})", index, chunk.length());
            log.info(chunk);

            // Add chunk if not empty
            if (!chunk.isEmpty()) {
                chunks.add(new Chunk(index++, chunk));
            }

            // If we reached the end, stop loop
            if (end == text.length()) break;

            // Move forward, but keep overlap characters from previous chunk
            start = Math.max(end - overlap, 0);
        }

        return chunks;
    }



    public Flux<Chunk> chunkUploadedFile2(FilePart filePart) {
        StringDecoder decoder = StringDecoder.allMimeTypes();

        return decoder.decode(filePart.content(), ResolvableType.forClass(String.class), null, Collections.emptyMap())
                // 1. Accumulate text and find chunks
                .scan(new ChunkState(), (state, segment) -> {
                    state.readyToEmit = new ArrayList<>(); // Reset emission list for this step
                    state.buffer += segment;

                    // Process chunks as long as we have enough text
                    // We use a smaller buffer (overlap + 10) to ensure we have enough context
                    while (state.buffer.length() >= chunkSize) {
                        int end = calculateEnd(state.buffer);
                        String content = state.buffer.substring(0, end).trim();

                        if (!content.isEmpty()) {
                            state.readyToEmit.add(new Chunk(state.lastIndex++, content));
                        }

                        // Move the window: Keep only the overlap for the next round
                        state.buffer = state.buffer.substring(Math.max(0, end - overlap));
                    }
                    return state;
                })
                // 2. Emit the chunks found in the current step
                .flatMapIterable(state -> state.readyToEmit)
                // 3. IMPORTANT: Handle the very last piece when the file stream ends
                .concatWith(Flux.defer(() -> Flux.empty())) // Placeholder for logic below
                .doOnNext(chunk -> log.info("Emitting chunk {}", chunk));
    }

    private static class ChunkState {
        String buffer = "";
        int lastIndex = 0;
        List<Chunk> readyToEmit = new ArrayList<>();
    }

    private int calculateEnd(String text) {
        int end = Math.min(chunkSize, text.length());
        int lastNewLine = text.lastIndexOf('\n', end);
        if (lastNewLine > (chunkSize - 200)) {
            return lastNewLine;
        }
        return end;
    }
}
