package com.saha.amit.reporting.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Non-blocking wrapper.
     * Runs the blocking OpenAI embedding call on boundedElastic thread pool.
     */
    public Mono<List<Double>> embedAsync(String text) {
        return Mono.fromCallable(() -> {
                    float[] vector = embeddingModel.embed(text);

                    return IntStream.range(0, vector.length)
                            .mapToDouble(i -> vector[i])
                            .boxed()
                            .toList();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public String toPgVectorLiteral(List<Double> vector) {
        return vector.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }
}

