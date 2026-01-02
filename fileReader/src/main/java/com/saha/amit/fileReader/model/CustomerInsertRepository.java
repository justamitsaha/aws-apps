package com.saha.amit.fileReader.model;

@Repository
@RequiredArgsConstructor
public class CustomerInsertRepository {

    private final DatabaseClient client;

    public Mono<Void> insert(CustomerEntity c) {
        return client.sql("""
                    INSERT INTO customers (
                        customer_id, row_number, surname, credit_score, geography,
                        gender, age, tenure, balance, num_of_products,
                        has_cr_card, is_active_member, estimated_salary, exited
                    ) VALUES (
                        :customerId, :rowNumber, :surname, :creditScore, :geography,
                        :gender, :age, :tenure, :balance, :numOfProducts,
                        :hasCrCard, :isActiveMember, :estimatedSalary, :exited
                    )
                """)
                .bind("customerId", c.getCustomerId())
                .bind("rowNumber", c.getRowNumber())
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
                .then();
    }
}
