package com.saha.amit.fileReader.entity;

import com.saha.amit.fileReader.reopsitory.CustomerChurnInsertRepository;
import com.saha.amit.fileReader.reopsitory.CustomerInsertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CustomerWriteService {

    private final CustomerInsertRepository customerRepo;
    private final CustomerChurnInsertRepository churnRepo;
    private final TransactionalOperator tx;

    public Mono<Void> create(CustomerEntity customer,
                             CustomerChurnEntity churn) {

        return customerRepo.insert(customer)
                .then(churnRepo.insert(churn))
                .as(tx::transactional)
                .then();
    }
}
