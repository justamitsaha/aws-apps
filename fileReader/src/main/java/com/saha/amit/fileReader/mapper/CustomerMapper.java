package com.saha.amit.fileReader.mapper;

import com.saha.amit.fileReader.entity.CustomerChurnEntity;
import com.saha.amit.fileReader.entity.CustomerEntity;
import com.saha.amit.fileReader.entity.CustomerProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring") // Use "spring" if using Spring Boot
public interface CustomerMapper {

    CustomerMapper INSTANCE = Mappers.getMapper(CustomerMapper.class);

    @Mapping(source = "customer.customerId", target = "customerId")
    @Mapping(source = "customer.age", target = "age")
    @Mapping(source = "customer.geography", target = "geography")
    @Mapping(source = "customer.gender", target = "gender")
    @Mapping(source = "customer.tenure", target = "tenure")
    @Mapping(source = "customer.isActiveMember", target = "isActiveMember")
    @Mapping(source = "customer.balance", target = "balance")
    @Mapping(source = "churn.seniorCitizen", target = "seniorCitizen")
    @Mapping(source = "churn.internetService", target = "internetService")
    @Mapping(source = "churn.techSupport", target = "techSupport")
    @Mapping(source = "churn.streamingTv", target = "streamingTv")
    @Mapping(source = "churn.monthlyCharges", target = "monthlyCharges")
    @Mapping(source = "churn.totalCharges", target = "totalCharges")
    @Mapping(source = "churn.contract", target = "contract")
    @Mapping(source = "churn.paymentMethod", target = "paymentMethod")
    CustomerProfile toProfile(CustomerEntity customer, CustomerChurnEntity churn);
}
