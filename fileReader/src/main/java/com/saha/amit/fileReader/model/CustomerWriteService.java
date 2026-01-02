package com.saha.amit.fileReader.model;

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
                .as(tx::transactional);
    }
}
