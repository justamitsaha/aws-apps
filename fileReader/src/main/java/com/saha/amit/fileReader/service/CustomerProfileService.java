package com.saha.amit.fileReader.service;

import com.saha.amit.fileReader.entity.AiInteraction;
import com.saha.amit.fileReader.entity.CustomerProfile;
import com.saha.amit.fileReader.mapper.CustomerMapper;
import com.saha.amit.fileReader.reopsitory.AiInteractionRepository;
import com.saha.amit.fileReader.reopsitory.CustomerChurnEntityRepository;
import com.saha.amit.fileReader.reopsitory.CustomerEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class CustomerProfileService {
    private final CustomerEntityRepository customerRepo;
    private final CustomerChurnEntityRepository churnRepo;
    private final CustomerMapper mapper;
    private final AiInteractionRepository repository;
    private final ObjectMapper objectMapper;

    public Mono<CustomerProfile> getFullProfile(Long id) {
        return Mono.zip(
                customerRepo.findById(id),
                churnRepo.findById(id)
        ).map(tuple -> mapper.toProfile(tuple.getT1(), tuple.getT2()));
    }


    public Mono<AiInteraction> getRecommendation(String customerId) {
        return repository.findFirstByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public Mono<AiInteraction> generateAndSave(AiInteraction aiInteraction) {
        return repository.save(aiInteraction);
    }


}
