package com.saha.amit.fileReader.reopsitory;

import com.saha.amit.fileReader.entity.CustomerChurnEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CustomerChurnEntityRepository extends ReactiveCrudRepository<CustomerChurnEntity,Long> {

    Flux<CustomerChurnEntity> findAllBy(Pageable pageable);
}
