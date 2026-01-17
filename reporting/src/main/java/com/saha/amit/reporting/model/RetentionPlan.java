package com.saha.amit.reporting.model;

import java.util.List;

import java.util.List;

public record RetentionPlan(
        RiskLevel riskLevel,
        List<String> reasoning,   // multiple bullet reasons (more useful)
        List<ActionItem> actions,
        Offer offer               // can be null
) {
    public enum RiskLevel { HIGH, MEDIUM, LOW }

    public record ActionItem(
            String title,
            String details,
            String priority        // HIGH/MEDIUM/LOW
    ) {}

    public record Offer(
            OfferType type,           // DISCOUNT | UPGRADE | SUPPORT | NONE
            String description,
            Integer discountPercent,  // null if not applicable
            Integer durationMonths    // null if not applicable
    ) {}

    public enum OfferType { DISCOUNT, UPGRADE, SUPPORT, NONE }

}

