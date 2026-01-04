package com.saha.amit.fileReader.reopsitory;

import com.saha.amit.fileReader.entity.CustomerChurnEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class CustomerChurnInsertRepository {

    private final DatabaseClient client;

    public Mono<CustomerChurnEntity> insert(CustomerChurnEntity c) {
        return client.sql("""
            INSERT INTO customer_churn (
                customer_id, unique_id, gender, senior_citizen,
                partner, dependents, tenure, phone_service,
                multiple_lines, internet_service, online_security,
                online_backup, device_protection, tech_support,
                streaming_tv, streaming_movies, contract,
                paperless_billing, payment_method,
                monthly_charges, total_charges, churn
            ) VALUES (
                :customerId, :uniqueId, :gender, :seniorCitizen,
                :partner, :dependents, :tenure, :phoneService,
                :multipleLines, :internetService, :onlineSecurity,
                :onlineBackup, :deviceProtection, :techSupport,
                :streamingTv, :streamingMovies, :contract,
                :paperlessBilling, :paymentMethod,
                :monthlyCharges, :totalCharges, :churn
            )
        """)
                .bind("customerId", c.getCustomerId())
                .bind("uniqueId", c.getUniqueId())
                .bind("gender", c.getGender())
                .bind("seniorCitizen", c.getSeniorCitizen())
                .bind("partner", c.getPartner())
                .bind("dependents", c.getDependents())
                .bind("tenure", c.getTenure())
                .bind("phoneService", c.getPhoneService())
                .bind("multipleLines", c.getMultipleLines())
                .bind("internetService", c.getInternetService())
                .bind("onlineSecurity", c.getOnlineSecurity())
                .bind("onlineBackup", c.getOnlineBackup())
                .bind("deviceProtection", c.getDeviceProtection())
                .bind("techSupport", c.getTechSupport())
                .bind("streamingTv", c.getStreamingTv())
                .bind("streamingMovies", c.getStreamingMovies())
                .bind("contract", c.getContract())
                .bind("paperlessBilling", c.getPaperlessBilling())
                .bind("paymentMethod", c.getPaymentMethod())
                .bind("monthlyCharges", c.getMonthlyCharges())
                .bind("totalCharges", c.getTotalCharges())
                .bind("churn", c.getChurn())
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1
                        ? Mono.just(c)
                        : Mono.error(new IllegalStateException("Churn insert failed"))
                );
    }
}

