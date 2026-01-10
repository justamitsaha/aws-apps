package com.saha.amit.fileReader.controller;

import com.saha.amit.fileReader.components.CustomerChurnCsvProcessor;
import com.saha.amit.fileReader.entity.CustomerChurnEntity;
import com.saha.amit.fileReader.entity.CustomerEntity;
import com.saha.amit.fileReader.entity.FailedRecord;
import com.saha.amit.fileReader.service.CsvIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class CsvUploadController {

    private final CsvIngestionService ingestionService;

    @PostMapping(value = "/customer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<CustomerEntity>> uploadCustomer(@RequestPart("file") FilePart file) {
        log.info("Received customer file upload request");
        return ResponseEntity.ok(ingestionService.processCustomer(file));
    }

    @GetMapping(value= "/customers", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<CustomerEntity> customerEndpoint() {
        return ingestionService.getAllCustomers();
    }

    @PostMapping(value = "/churn", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<CustomerChurnEntity>> upload(@RequestPart("file") FilePart file) {
        log.info("Received churn file upload request");
        return ResponseEntity.ok(ingestionService.processChurn(file));
    }

    @GetMapping(value= "/customersChurn", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<CustomerChurnEntity> customerChurnEndpoint() {
        return ingestionService.getAllCustomersChurn();
    }

    @GetMapping("/churnFailures")
    public List<FailedRecord> getFailures() {
        return CustomerChurnCsvProcessor.FAILED_RECORDS;
    }

    // IMPORTANT: An endpoint to clear the list after you've downloaded/fixed it
    @DeleteMapping("/churnFailures")
    public Mono<Void> clearFailures() {
        CustomerChurnCsvProcessor.FAILED_RECORDS.clear();
        return Mono.empty();
    }


    @DeleteMapping("/cleanup")
    public Mono<ResponseEntity<Void>> cleanup() {
        return ingestionService.clearAllData()
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("CSV Upload Service is healthy");
    }
}

