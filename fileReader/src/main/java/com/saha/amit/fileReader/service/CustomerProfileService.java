package com.saha.amit.fileReader.service;

import com.saha.amit.fileReader.entity.CustomerProfile;
import com.saha.amit.fileReader.mapper.CustomerMapper;
import com.saha.amit.fileReader.reopsitory.CustomerChurnEntityRepository;
import com.saha.amit.fileReader.reopsitory.CustomerEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CustomerProfileService {
    private final CustomerEntityRepository customerRepo;
    private final CustomerChurnEntityRepository churnRepo;
    private final CustomerMapper mapper;

    public Mono<CustomerProfile> getFullProfile(Long id) {
        return Mono.zip(
                customerRepo.findById(id),
                churnRepo.findById(id)
        ).map(tuple -> mapper.toProfile(tuple.getT1(), tuple.getT2()));
    }
}
