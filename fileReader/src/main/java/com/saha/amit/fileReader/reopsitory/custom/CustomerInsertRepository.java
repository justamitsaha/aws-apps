package com.saha.amit.fileReader.reopsitory.custom;

import com.saha.amit.fileReader.entity.CustomerEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomerInsertRepository {

    private final DatabaseClient client;

    public Mono<CustomerEntity> insert(CustomerEntity c) {
        return client.sql("""
                            INSERT INTO aws.customers (
                                customer_id, row_numbers, surname, credit_score, geography,
                                gender, age, tenure, balance, num_of_products,
                                has_cr_card, is_active_member, estimated_salary, exited
                            ) VALUES (
                                :customerId, :rowNumbers, :surname, :creditScore, :geography,
                                :gender, :age, :tenure, :balance, :numOfProducts,
                                :hasCrCard, :isActiveMember, :estimatedSalary, :exited
                            )
                        """)
                .bind("customerId", c.getCustomerId())
                .bind("rowNumbers", c.getRowNumbers())
                .bind("surname", c.getSurname())
                .bind("creditScore", c.getCreditScore())
                .bind("geography", c.getGeography())
                .bind("gender", c.getGender())
                .bind("age", c.getAge())
                .bind("tenure", c.getTenure())
                .bind("balance", c.getBalance())
                .bind("numOfProducts", c.getNumOfProducts())
                .bind("hasCrCard", c.getHasCrCard())
                .bind("isActiveMember", c.getIsActiveMember())
                .bind("estimatedSalary", c.getEstimatedSalary())
                .bind("exited", c.getExited())
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1
                        ? Mono.just(c)
                        : Mono.error(new IllegalStateException("Customer insert failed"))
                )
                .doOnError(e -> log.error("Insert failed for customerId={}, data={}", c.getCustomerId(), c, e));
    }

    public Mono<Void> clearCustomerChurn() {
        return client.sql("DELETE FROM customer_churn")
                .then();
    }

    public Mono<Void> clearCustomers() {
        return client.sql("DELETE FROM customers")
                .then();
    }

    public Mono<Void> clearAll() {
        return client.sql("DELETE FROM customer_churn")
                .fetch()
                .rowsUpdated()
                .then(
                        client.sql("DELETE FROM customers")
                                .fetch()
                                .rowsUpdated()
                )
                .then(
                        client.sql("DELETE FROM ai_interactions")
                                .fetch()
                                .rowsUpdated())
                .then();
    }

}

