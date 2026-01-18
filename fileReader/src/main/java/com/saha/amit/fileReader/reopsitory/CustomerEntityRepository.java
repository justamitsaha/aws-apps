package com.saha.amit.fileReader.reopsitory;

import com.saha.amit.fileReader.entity.CustomerEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;


@Repository
public interface CustomerEntityRepository extends ReactiveCrudRepository<CustomerEntity, Long> {

    Flux<CustomerEntity> findAllBy(Pageable pageable);

}

