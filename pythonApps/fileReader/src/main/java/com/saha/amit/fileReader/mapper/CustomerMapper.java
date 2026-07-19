package com.saha.amit.fileReader.mapper;

import com.saha.amit.fileReader.entity.CustomerChurnEntity;
import com.saha.amit.fileReader.entity.CustomerEntity;
import com.saha.amit.fileReader.entity.CustomerProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mappings({
            @Mapping(source = "customer.customerId", target = "customerId"),

            // Demographics
            @Mapping(source = "customer.age", target = "age"),
            @Mapping(source = "customer.geography", target = "geography"),
            @Mapping(source = "churn.gender", target = "gender"), // prefer churn gender
            @Mapping(source = "churn.seniorCitizen", target = "seniorCitizen"),

            // Banking profile
            @Mapping(source = "customer.creditScore", target = "creditScore"),
            @Mapping(source = "customer.numOfProducts", target = "numOfProducts"),
            @Mapping(source = "customer.hasCrCard", target = "hasCrCard"),
            @Mapping(source = "customer.isActiveMember", target = "isActiveMember"),
            @Mapping(source = "customer.estimatedSalary", target = "estimatedSalary"),

            // Relationship
            @Mapping(source = "churn.tenure", target = "tenure"),

            // Services
            @Mapping(source = "churn.internetService", target = "internetService"),
            @Mapping(source = "churn.techSupport", target = "techSupport"),
            @Mapping(source = "churn.onlineSecurity", target = "onlineSecurity"),
            @Mapping(source = "churn.deviceProtection", target = "deviceProtection"),
            @Mapping(source = "churn.streamingTv", target = "streamingTv"),
            @Mapping(source = "churn.streamingMovies", target = "streamingMovies"),

            // Contract + billing
            @Mapping(source = "churn.contract", target = "contract"),
            @Mapping(source = "churn.paperlessBilling", target = "paperlessBilling"),
            @Mapping(source = "churn.paymentMethod", target = "paymentMethod"),

            // Financial
            @Mapping(source = "churn.monthlyCharges", target = "monthlyCharges"),
            @Mapping(source = "churn.totalCharges", target = "totalCharges"),
            @Mapping(source = "customer.balance", target = "balance"),

            // Label
            @Mapping(source = "churn.churn", target = "churn")
    })
    CustomerProfile toProfile(CustomerEntity customer, CustomerChurnEntity churn);
}

