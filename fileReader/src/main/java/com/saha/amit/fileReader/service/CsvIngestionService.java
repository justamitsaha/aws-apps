package com.saha.amit.fileReader.service;

import com.saha.amit.fileReader.components.CustomerChurnCsvProcessor;
import com.saha.amit.fileReader.components.CustomerCsvProcessor;
import com.saha.amit.fileReader.entity.CustomerChurnEntity;
import com.saha.amit.fileReader.entity.CustomerEntity;
import com.saha.amit.fileReader.reopsitory.CustomerChurnEntityRepository;
import com.saha.amit.fileReader.reopsitory.CustomerEntityRepository;
import com.saha.amit.fileReader.reopsitory.custom.CustomerInsertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
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

    @Value("${app.upload.dir}")
    private String uploadDir;

    public Mono<Void> saveToDisk(FilePart file) {
        // 1. Create the directory path safely for any OS
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            // 2. Ensure the directory exists (mkdir -p)
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }
        } catch (IOException e) {
            return Mono.error(new RuntimeException("Could not initialize storage directory", e));
        }

        // 3. Resolve the file name and save
        Path targetFile = root.resolve(file.filename());

        log.info("Saving file to: {}", targetFile);

        // 4. transferTo is the reactive way to write to disk
        return file.transferTo(targetFile);
    }




}

