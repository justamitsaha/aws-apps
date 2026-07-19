package com.saha.amit.reporting.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/ping")
public class TestController {

    /*
    Endpoint Style,             Content-Type,       Transfer-Encoding,  Header Timing
    Mono<T>,                    application/json,   Content-Length,     Delayed (waits for value)
    ResponseEntity<Mono<T>>,    application/json,   Content-Length,     Immediate
    Flux<T>,                    application/json,   chunked,            Delayed (starts with first chunk)
    Flux (SSE Stream),          text/event-stream,  chunked,            Immediate (connection opens)
     */

    @GetMapping("/mono/1/{value}")
    public Mono<Integer> ping(@PathVariable Integer value) {
        return Mono.just(value * value);
    }

    @GetMapping("/mono/2/{value}")
    public ResponseEntity<Mono<Integer>> ping2(@PathVariable Integer value) {
        return ResponseEntity.ok(Mono.just(value * value));
    }

    @GetMapping("mono/3/{value}")
    public Mono<ResponseEntity<Integer>> ping3(@PathVariable Integer value) {
        return Mono.just(ResponseEntity.ok(value * value));
    }

    @GetMapping(value = "/flux/1/{value}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Integer> pingFlux(@PathVariable Integer value) {
        return Flux.range(1, 10)
                .delayElements(Duration.ofMillis(500))
                .map(i -> i * value);
    }

    @GetMapping(value = "/flux/2/{value}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<Integer>> pingFlux2(@PathVariable Integer value) {
        return ResponseEntity.ok(
                Flux.range(1, 10)
                        .delayElements(Duration.ofMillis(500))
                        .map(i -> i * value)
        );
    }

    @GetMapping(value = "/flux/3/{value}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseEntity<Integer>> pingFlux3(@PathVariable Integer value) {
        return Flux.range(1, 10)
                .delayElements(Duration.ofMillis(500))
                .map(i -> ResponseEntity.ok(i * value));
    }
}
