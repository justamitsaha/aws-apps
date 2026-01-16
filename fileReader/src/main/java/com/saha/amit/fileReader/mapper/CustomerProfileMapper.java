package com.saha.amit.fileReader.mapper;

import com.saha.amit.fileReader.entity.CustomerChurnEntity;
import com.saha.amit.fileReader.entity.CustomerEntity;
import com.saha.amit.fileReader.entity.CustomerProfile;

import java.util.Optional;

public class CustomerProfileMapper {

    public static CustomerProfile mapToProfile(CustomerEntity customer, CustomerChurnEntity churn) {
        if (customer == null || churn == null) {
            return null; // Or throw an IllegalArgumentException
        }

        return new CustomerProfile(
                customer.getCustomerId(),

                // Demographics
                Optional.ofNullable(customer.getAge()).orElse(0),
                customer.getGender(),
                customer.getGeography(),
                Boolean.TRUE.equals(churn.getSeniorCitizen()),

                // Relationship
                Optional.ofNullable(customer.getTenure()).orElse(0),
                Boolean.TRUE.equals(customer.getIsActiveMember()),

                // Services
                churn.getInternetService(),
                churn.getTechSupport(),
                churn.getStreamingTv(),

                // Financial (BigDecimal to double conversion)
                customer.getBalance() != null ? customer.getBalance().doubleValue() : 0.0,
                churn.getMonthlyCharges() != null ? churn.getMonthlyCharges().doubleValue() : 0.0,
                churn.getTotalCharges() != null ? churn.getTotalCharges().doubleValue() : 0.0,

                // Contract
                churn.getContract(),
                churn.getPaymentMethod()
        );
    }
}