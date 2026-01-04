package com.saha.amit.fileReader.util;

import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

public final class CsvUtils {

    private CsvUtils() {
    }

    public static Flux<String[]> read(FilePart file) {

        /*
        Your current code processes each DataBuffer as an independent chunk of text. In reactive programming, a file is read in small pieces (usually 4KB or 8KB).

        If a row in your CSV happens to sit exactly on the boundary where one buffer ends and the next begins, the line gets cut in half. 1. Buffer A ends with ...0,0,19 2. Buffer B starts with 0857.79,0
        This means that when you split Buffer A by newlines, you get an incomplete line "0,0,19" and when you split Buffer B, you get "0857.79,0". Neither of these lines is valid on its own.
         */

        /* Your code calls .split("\n") on Buffer A, so it thinks the line ends at 19. Because that line is now incomplete, it only has 13 columns instead of 14, causing your CustomerCsvMapper to crash when it tries to read the 14th column.
        return file.content()
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .flatMap(content ->
                        Flux.fromArray(content.split("\n"))
                )
                .skip(1) // skip header
                .map(line -> line.split(","));*/


        /*
        The Solution: Use StringDecoder To fix this, you need to tell Spring to "aggregate" the buffers until it finds a newline character, rather than processing chunks manually.
         */
        StringDecoder decoder = StringDecoder.textPlainOnly();

        return decoder.decode(
                        file.content(),
                        null, null, null // Use default settings for decoding
                )
                .skip(1) // Skip header
                .map(line -> line.split(","))
                // Safety check: ensure the row has exactly 14 columns
                .filter(items -> {
                    if (items.length < 14) {
                        System.err.println("Skipping malformed row (length " + items.length + "): " + String.join(",", items));
                        return false;
                    }
                    return true;
                });
    }
}

