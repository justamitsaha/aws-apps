package com.saha.amit.fileReader.reopsitory;

import com.saha.amit.fileReader.entity.CustomerChurnEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerChurnEntityRepository extends ReactiveCrudRepository<CustomerChurnEntity,Long> {
}
