package com.saha.amit.fileReader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FileReaderApplication {

	public static void main(String[] args) {
        System.out.println("http://localhost:8080/");
        System.out.println("http://localhost:8080/customers.html");
        System.out.println("SPRING_PROFILES_ACTIVE=h2/mysql");
        System.out.println("http://localhost:8080/upload/churnFailures");
        System.out.println("http://localhost:8080/upload/customers");
        System.out.println("http://localhost:8080/upload/customersChurn");
        System.out.println("http://localhost:8080/upload/health");
		SpringApplication.run(FileReaderApplication.class, args);
	}

}
