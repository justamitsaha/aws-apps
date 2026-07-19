package com.saha.amit.fileReader.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table("customers")
public class CustomerEntity {

    @Id
    @Column("customer_id")
    private Long customerId;

    //@Column("row_num")
    @Column("row_numbers")
    private Integer rowNumbers;

    private String surname;

    @Column("credit_score")
    private Integer creditScore;

    private String geography;
    private String gender;
    private Integer age;
    private Integer tenure;

    private BigDecimal balance;

    @Column("num_of_products")
    private Integer numOfProducts;

    @Column("has_cr_card")
    private Boolean hasCrCard;

    @Column("is_active_member")
    private Boolean isActiveMember;

    @Column("estimated_salary")
    private BigDecimal estimatedSalary;

    private Boolean exited;
}
