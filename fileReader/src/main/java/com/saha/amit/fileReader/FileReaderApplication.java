package com.saha.amit.fileReader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FileReaderApplication {

	public static void main(String[] args) {
        System.out.println("http://localhost:8080/");
        System.out.println("http://localhost:8080/customers.html");
        System.out.println("SPRING_PROFILES_ACTIVE=h2/mysql");
        System.out.println("URI to fetch AI recommendation from AI http://localhost:8080/customerProfile/15565701/recommendation");
		SpringApplication.run(FileReaderApplication.class, args);
	}

}
