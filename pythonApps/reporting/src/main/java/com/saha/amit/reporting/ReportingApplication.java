package com.saha.amit.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReportingApplication {

    public static void main(String[] args) {
        System.out.println("http://localhost:8081/reporting/health");
        System.out.println("http://localhost:8081/reporting/15565701/analyze");
        SpringApplication.run(ReportingApplication.class, args);
    }
}
