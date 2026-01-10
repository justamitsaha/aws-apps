package com.saha.amit.fileReader.service;

import com.saha.amit.fileReader.components.CustomerChurnCsvProcessor;
import com.saha.amit.fileReader.components.CustomerCsvProcessor;
import com.saha.amit.fileReader.entity.CustomerChurnEntity;
import com.saha.amit.fileReader.entity.CustomerEntity;
import com.saha.amit.fileReader.reopsitory.CustomerChurnEntityRepository;
import com.saha.amit.fileReader.reopsitory.CustomerEntityRepository;
import com.saha.amit.fileReader.reopsitory.CustomerInsertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CsvIngestionService {

    private final CustomerCsvProcessor customerProcessor;
    private final CustomerChurnCsvProcessor churnProcessor;
    private final CustomerInsertRepository repository;
    private final CustomerEntityRepository customerEntityRepository;
    private final CustomerChurnEntityRepository customerChurnEntityRepository;


    public Flux<CustomerEntity> processCustomer(FilePart file) {

        String filename = file.filename();

        if (filename.equalsIgnoreCase("Churn_Modelling.csv")) {
            return customerProcessor.process(file);
        } else {
            throw new IllegalArgumentException("Unsupported file: " + filename);
        }
    }

    public Flux<CustomerChurnEntity> processChurn(FilePart file) {

        String filename = file.filename();

        if (filename.equalsIgnoreCase("CustomerChurn.csv")) {
            return churnProcessor.process(file);
        } else {
            throw new IllegalArgumentException("Unsupported file: " + filename);
        }
    }


    public Mono<Void> clearAllData() {
        return repository.clearAll();
    }

    public Flux<CustomerEntity> getAllCustomers() {
        return customerEntityRepository
                .findAll()
                .take(10);
    }


    public Flux<CustomerChurnEntity> getAllCustomersChurn() {
        return customerChurnEntityRepository
                .findAll()
                .take(100);
    }




}

