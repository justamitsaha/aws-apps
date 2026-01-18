package com.saha.amit.fileReader.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;

@Slf4j

public class R2dbcSchemaInitializer {


    ApplicationRunner initSchema(DatabaseClient client) {
        log.info("Initializing database schema...");
        return args -> client
                .sql("""
                CREATE TABLE IF NOT EXISTS "customers" (
                    customer_id BIGINT PRIMARY KEY,
                    row_numbers INT,
                    surname VARCHAR(100),
                    credit_score INT,
                    geography VARCHAR(50),
                    gender VARCHAR(10),
                    age INT,
                    tenure INT,
                    balance DECIMAL(15,2),
                    num_of_products INT,
                    has_cr_card BOOLEAN,
                    is_active_member BOOLEAN,
                    estimated_salary DECIMAL(15,2),
                    exited BOOLEAN
                );

                CREATE TABLE IF NOT EXISTS "customer_churn" (
                    customer_id BIGINT PRIMARY KEY,
                    unique_id VARCHAR(50),

                    gender VARCHAR(10),
                    senior_citizen BOOLEAN,
                    partner BOOLEAN,
                    dependents BOOLEAN,
                    tenure INT,

                    phone_service BOOLEAN,
                    multiple_lines VARCHAR(20),
                    internet_service VARCHAR(30),
                    online_security VARCHAR(30),
                    online_backup VARCHAR(30),
                    device_protection VARCHAR(30),
                    tech_support VARCHAR(30),
                    streaming_tv VARCHAR(30),
                    streaming_movies VARCHAR(30),

                    contract VARCHAR(30),
                    paperless_billing BOOLEAN,
                    payment_method VARCHAR(50),

                    monthly_charges DECIMAL(10,2),
                    total_charges DECIMAL(12,2),
                    churn BOOLEAN,

                    CONSTRAINT fk_customer
                        FOREIGN KEY (customer_id)
                        REFERENCES customers(customer_id)
                );
                CREATE TABLE IF NOT EXISTS ai_interactions (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                customer_id VARCHAR(50),
                                raw_prompt TEXT,               -- Prompts can be long, TEXT is fine here
                                ai_response JSON,             -- Use JSON for the structured output
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                            );
            """)
                .then()
                .doOnSuccess(v -> log.info("R2DBC schema initialization completed successfully"))
                .doOnError(e -> log.error("R2DBC schema initialization failed", e))
                .subscribe();
    }
}
