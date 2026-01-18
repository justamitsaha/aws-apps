package com.saha.amit.fileReader.components;

import com.saha.amit.fileReader.entity.CustomerChurnEntity;
import com.saha.amit.fileReader.entity.FailedRecord;
import com.saha.amit.fileReader.mapper.CustomerChurnCsvMapper;
import com.saha.amit.fileReader.reopsitory.custom.CustomerChurnInsertRepository;
import com.saha.amit.fileReader.util.CsvUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerChurnCsvProcessor {

    private final CustomerChurnInsertRepository repository;

    // Use a thread-safe list to handle concurrent updates

    public Flux<CustomerChurnEntity> process(FilePart file) {
        return CsvUtils.read(file)
                .map(CustomerChurnCsvMapper::toEntity)
                .flatMap(entity ->
                                repository.insert(entity)
                                        //.doOnSuccess(success -> log.info("Inserted: {}", entity.getUniqueId()))
                                        // Handle errors here so the stream doesn't stop
                                        .onErrorResume(error -> {
                                            //FAILED_RECORDS.add(new FailedRecord(entity, error.getMessage()));
                                            // Return Mono.empty() so the "Success" Flux just ignores this row
                                            log.error("Failed to insert record {}: {}", entity.getUniqueId(), error.getMessage());
                                            return Mono.empty();
                                        })
                        , 50)
                .doOnError(e -> log.error("THE STREAM CRASHED HERE: {}", e.getMessage())); // ADD THIS// Concurrency of 50
                //.doOnTerminate(() -> log.info("Processing finished. Total failures: {}", FAILED_RECORDS.size()));
    }
}

