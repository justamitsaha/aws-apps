package com.saha.amit.fileReader.reopsitory;

import com.saha.amit.fileReader.entity.AiInteraction;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface AiInteractionRepository extends ReactiveCrudRepository<AiInteraction, Long> {
    Mono<AiInteraction> findFirstByCustomerIdOrderByCreatedAtDesc(String customerId);
}
