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
@Table("customer_churn")
public class CustomerChurnEntity {

    @Id
    @Column("customer_id")
    private Long customerId;

    @Column("unique_id")
    private String uniqueId;

    private String gender;

    @Column("senior_citizen")
    private Boolean seniorCitizen;

    private Boolean partner;
    private Boolean dependents;
    private Integer tenure;

    @Column("phone_service")
    private Boolean phoneService;

    @Column("multiple_lines")
    private String multipleLines;

    @Column("internet_service")
    private String internetService;

    @Column("online_security")
    private String onlineSecurity;

    @Column("online_backup")
    private String onlineBackup;

    @Column("device_protection")
    private String deviceProtection;

    @Column("tech_support")
    private String techSupport;

    @Column("streaming_tv")
    private String streamingTv;

    @Column("streaming_movies")
    private String streamingMovies;

    private String contract;

    @Column("paperless_billing")
    private Boolean paperlessBilling;

    @Column("payment_method")
    private String paymentMethod;

    @Column("monthly_charges")
    private BigDecimal monthlyCharges;

    @Column("total_charges")
    private BigDecimal totalCharges;

    private Boolean churn;
}

