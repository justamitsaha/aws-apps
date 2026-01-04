package com.saha.amit.fileReader.mapper;

import com.saha.amit.fileReader.entity.CustomerChurnEntity;

import java.math.BigDecimal;

public final class CustomerChurnCsvMapper {

    private CustomerChurnCsvMapper() {}

    public static CustomerChurnEntity toEntity(String[] r) {

        CustomerChurnEntity e = new CustomerChurnEntity();

        e.setUniqueId(r[0]);
        e.setCustomerId(Long.valueOf(r[1]));
        e.setGender(r[2]);
        e.setSeniorCitizen(parseBoolean(r[3]));
        e.setPartner(parseBoolean(r[4]));
        e.setDependents(parseBoolean(r[5]));
        e.setTenure(Integer.valueOf(r[6]));
        e.setPhoneService(parseBoolean(r[7]));
        e.setMultipleLines(r[8]);
        e.setInternetService(r[9]);
        e.setOnlineSecurity(r[10]);
        e.setOnlineBackup(r[11]);
        e.setDeviceProtection(r[12]);
        e.setTechSupport(r[13]);
        e.setStreamingTv(r[14]);
        e.setStreamingMovies(r[15]);
        e.setContract(r[16]);
        e.setPaperlessBilling(parseBoolean(r[17]));
        e.setPaymentMethod(r[18]);
        e.setMonthlyCharges(new BigDecimal(r[19]));
        e.setTotalCharges(new BigDecimal(r[20]));
        e.setChurn(parseBoolean(r[21]));

        return e;
    }

    private static Boolean parseBoolean(String v) {
        return v.equals("1") || v.equalsIgnoreCase("yes") || v.equalsIgnoreCase("true");
    }
}

