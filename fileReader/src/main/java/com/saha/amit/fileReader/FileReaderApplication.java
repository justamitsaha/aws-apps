package com.saha.amit.fileReader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FileReaderApplication {

	public static void main(String[] args) {
        System.out.println("http://localhost:8080/");
        System.out.println("http://localhost:8080/customers.html");
		SpringApplication.run(FileReaderApplication.class, args);
	}

}
