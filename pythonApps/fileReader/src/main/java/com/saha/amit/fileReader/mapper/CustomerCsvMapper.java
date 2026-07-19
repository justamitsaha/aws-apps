package com.saha.amit.fileReader.mapper;

import com.saha.amit.fileReader.entity.CustomerEntity;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Arrays;

@Slf4j
public final class CustomerCsvMapper {

    private CustomerCsvMapper() {}

    public static CustomerEntity toEntity(String[] r) {
        return new CustomerEntity(
                Long.valueOf(r[1]),   // CustomerId
                Integer.valueOf(r[0]),// RowNumber
                r[2],                 // Surname
                Integer.valueOf(r[3]),
                r[4],
                r[5],
                Integer.valueOf(r[6]),
                Integer.valueOf(r[7]),
                new BigDecimal(r[8]),
                Integer.valueOf(r[9]),
                parseBoolean(r[10]),
                parseBoolean(r[11]),
                new BigDecimal(r[12]),
                parseBoolean(r[13])
        );
    }

    private static Boolean parseBoolean(String v) {
        return v.equals("1") || v.equalsIgnoreCase("yes") || v.equalsIgnoreCase("true");
    }
}

