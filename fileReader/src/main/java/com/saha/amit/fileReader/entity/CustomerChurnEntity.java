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
@Table("customer_churn")
public class CustomerChurnEntity {

    @Id
    private Long customerId; // PK + FK

    private String uniqueId;
    private String gender;
    private Boolean seniorCitizen;
    private Boolean partner;
    private Boolean dependents;
    private Integer tenure;

    private Boolean phoneService;
    private String multipleLines;
    private String internetService;
    private String onlineSecurity;
    private String onlineBackup;
    private String deviceProtection;
    private String techSupport;
    private String streamingTv;
    private String streamingMovies;

    private String contract;
    private Boolean paperlessBilling;
    private String paymentMethod;

    private BigDecimal monthlyCharges;
    private BigDecimal totalCharges;
    private Boolean churn;
}
