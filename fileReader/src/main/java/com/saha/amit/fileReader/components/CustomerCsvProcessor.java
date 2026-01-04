package com.saha.amit.fileReader.components;

import com.saha.amit.fileReader.entity.CustomerEntity;
import com.saha.amit.fileReader.mapper.CustomerCsvMapper;
import com.saha.amit.fileReader.reopsitory.CustomerInsertRepository;
import com.saha.amit.fileReader.util.CsvUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerCsvProcessor {

    private final CustomerInsertRepository repository;

    public Flux<CustomerEntity> process(FilePart file) {
        return CsvUtils.read(file)
                .map(CustomerCsvMapper::toEntity)
                .flatMap(customer ->
                                repository.insert(customer)
                                        .doOnError(e -> log.error("Failed item: {}", customer))
                                        .onErrorResume(e -> Mono.empty()), // Swallow error and continue with next row
                        50
                );
    }
}
