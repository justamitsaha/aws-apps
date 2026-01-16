package com.saha.amit.reporting.model;

import java.util.List;

public record RetentionPlan(
        String riskLevel,        // High, Medium, Low
        String reasoning,       // Why the AI thinks they will leave
        List<String> actions,    // Actionable steps for the agent
        String discountCode      // A generated offer code
) {}
