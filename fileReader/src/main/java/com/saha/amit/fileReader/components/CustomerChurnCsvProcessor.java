package com.saha.amit.fileReader.components;

import com.saha.amit.fileReader.entity.CustomerChurnEntity;
import com.saha.amit.fileReader.mapper.CustomerChurnCsvMapper;
import com.saha.amit.fileReader.reopsitory.CustomerChurnInsertRepository;
import com.saha.amit.fileReader.util.CsvUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class CustomerChurnCsvProcessor {

    private final CustomerChurnInsertRepository repository;

    public Flux<CustomerChurnEntity> process(FilePart file) {
        return CsvUtils.read(file)
                .map(CustomerChurnCsvMapper::toEntity)
                .flatMap(repository::insert, 50); // concurrency
    }
}

