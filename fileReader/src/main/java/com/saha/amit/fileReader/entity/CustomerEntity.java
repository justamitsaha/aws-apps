package com.saha.amit.fileReader.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
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
    private Long customerId;
    private Integer rowNumbers;
    private String surname;
    private Integer creditScore;
    private String geography;
    private String gender;
    private Integer age;
    private Integer tenure;
    private BigDecimal balance;
    private Integer numOfProducts;
    private Boolean hasCrCard;
    private Boolean isActiveMember;
    private BigDecimal estimatedSalary;
    private Boolean exited;
}
