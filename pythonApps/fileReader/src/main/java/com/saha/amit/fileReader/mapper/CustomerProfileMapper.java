package com.saha.amit.fileReader.mapper;

import com.saha.amit.fileReader.entity.CustomerChurnEntity;
import com.saha.amit.fileReader.entity.CustomerEntity;
import com.saha.amit.fileReader.entity.CustomerProfile;

import java.util.Optional;

import java.util.Objects;

public class CustomerProfileMapper {

    public static CustomerProfile mapToProfile(CustomerEntity customer, CustomerChurnEntity churn) {
        if (customer == null || churn == null) {
            return null; // or throw new IllegalArgumentException("customer/churn cannot be null");
        }

        return new CustomerProfile(
                customer.getCustomerId(),

                // Demographics
                customer.getAge(),
                churn.getGender() != null ? churn.getGender() : customer.getGender(), // prefer churn if present
                customer.getGeography(),
                churn.getSeniorCitizen(),

                // Banking profile (CustomerEntity)
                customer.getCreditScore(),
                customer.getNumOfProducts(),
                customer.getHasCrCard(),
                customer.getIsActiveMember(),
                customer.getEstimatedSalary(),

                // Relationship
                churn.getTenure() != null ? churn.getTenure() : customer.getTenure(),

                // Services (CustomerChurnEntity)
                churn.getInternetService(),
                churn.getTechSupport(),
                churn.getOnlineSecurity(),
                churn.getDeviceProtection(),
                churn.getStreamingTv(),
                churn.getStreamingMovies(),

                // Contract + billing
                churn.getContract(),
                churn.getPaperlessBilling(),
                churn.getPaymentMethod(),

                // Financial
                churn.getMonthlyCharges(),
                churn.getTotalCharges(),
                customer.getBalance(),

                // Label
                churn.getChurn()
        );
    }
}
