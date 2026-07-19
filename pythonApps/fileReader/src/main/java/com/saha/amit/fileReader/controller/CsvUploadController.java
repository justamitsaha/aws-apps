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

    @PostMapping(value = "/churn", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<CustomerChurnEntity>> upload(@RequestPart("file") FilePart file) {
        log.info("Received churn file upload request");
        return ResponseEntity.ok(ingestionService.processChurn(file));
    }

    @PostMapping(value = "/save-only", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<String>> saveFile(@RequestPart("file") FilePart file) {
        return ingestionService.saveToDisk(file)
                .thenReturn(ResponseEntity.ok("File saved successfully on " + System.getProperty("os.name")));
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("CSV Upload Service is healthy");
    }
}

