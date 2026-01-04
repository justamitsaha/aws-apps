package com.saha.amit.fileReader.reopsitory;

import com.saha.amit.fileReader.entity.CustomerEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerEntityRepository extends ReactiveCrudRepository<CustomerEntity, Long> {

}

