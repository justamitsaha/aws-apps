package com.saha.amit.fileReader.service;

import com.saha.amit.fileReader.entity.AiInteraction;
import com.saha.amit.fileReader.entity.CustomerChurnEntity;
import com.saha.amit.fileReader.entity.CustomerEntity;
import com.saha.amit.fileReader.entity.CustomerProfile;
import com.saha.amit.fileReader.mapper.CustomerMapper;
import com.saha.amit.fileReader.reopsitory.AiInteractionRepository;
import com.saha.amit.fileReader.reopsitory.CustomerChurnEntityRepository;
import com.saha.amit.fileReader.reopsitory.CustomerEntityRepository;
import com.saha.amit.fileReader.reopsitory.custom.CustomerInsertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CustomerProfileService {

    private final CustomerEntityRepository customerEntityRepository;
    private final CustomerChurnEntityRepository customerChurnEntityRepository;
    private final CustomerInsertRepository customerInsertRepository;
    private final CustomerMapper mapper;
    private final AiInteractionRepository repository;

    public Mono<Void> clearAllData() {
        return customerInsertRepository.clearAll();
    }

    public Flux<CustomerEntity> getAllCustomers(Pageable pageable) {
        return customerEntityRepository.findAllBy(pageable);
    }


    public Flux<CustomerChurnEntity> getAllCustomersChurn(Pageable pageable) {
        return customerChurnEntityRepository.findAllBy(pageable);
    }

    public Mono<CustomerProfile> getFullProfile(Long id) {
        return Mono.zip(
                customerEntityRepository.findById(id),
                customerChurnEntityRepository.findById(id)
        ).map(tuple -> mapper.toProfile(tuple.getT1(), tuple.getT2()));
    }


    public Mono<AiInteraction> getRecommendation(String customerId) {
        return repository.findFirstByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public Mono<AiInteraction> generateAndSave(AiInteraction aiInteraction) {
        return repository.save(aiInteraction);
    }

}
