package com.saha.amit.fileReader.mapper;

import com.saha.amit.fileReader.entity.CustomerChurnEntity;

import java.math.BigDecimal;

public final class CustomerChurnCsvMapper {

    private CustomerChurnCsvMapper() {}

    public static CustomerChurnEntity toEntity(String[] r) {
        CustomerChurnEntity e = new CustomerChurnEntity();
        // Use .trim() on everything, especially numbers and booleans
        e.setUniqueId(r[0].trim());
        e.setCustomerId(Long.valueOf(r[1].trim()));
        e.setGender(r[2].trim());
        e.setSeniorCitizen(parseBoolean(r[3].trim()));
        e.setPartner(parseBoolean(r[4].trim()));
        e.setDependents(parseBoolean(r[5].trim()));
        e.setTenure(Integer.valueOf(r[6].trim()));
        e.setPhoneService(parseBoolean(r[7].trim()));
        e.setMultipleLines(r[8].trim());
        e.setInternetService(r[9].trim());
        e.setOnlineSecurity(r[10].trim());
        e.setOnlineBackup(r[11].trim());
        e.setDeviceProtection(r[12].trim());
        e.setTechSupport(r[13].trim());
        e.setStreamingTv(r[14].trim());
        e.setStreamingMovies(r[15].trim());
        e.setContract(r[16].trim());
        e.setPaperlessBilling(parseBoolean(r[17].trim()));
        e.setPaymentMethod(r[18].trim());

        // The culprits for your specific error:
        e.setMonthlyCharges(parseBigDecimal(r[19]));
        e.setTotalCharges(parseBigDecimal(r[20]));
        e.setChurn(parseBoolean(r[21].trim()));

        return e;
    }

    private static BigDecimal parseBigDecimal(String v) {
        if (v == null || v.trim().isEmpty()) return BigDecimal.ZERO;
        // Clean string of spaces or currency symbols
        String clean = v.trim().replaceAll("[^\\d.]", "");
        return new BigDecimal(clean);
    }

    private static Boolean parseBoolean(String v) {
        return v.equals("1") || v.equalsIgnoreCase("yes") || v.equalsIgnoreCase("true");
    }
}

