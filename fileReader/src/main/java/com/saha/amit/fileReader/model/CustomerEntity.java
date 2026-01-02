package com.saha.amit.fileReader.model;

@Table("customers")
public class CustomerEntity {

    @Id
    private Long customerId;

    private Integer rowNumber;
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
