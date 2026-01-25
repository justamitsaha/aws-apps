# Retention Policy (Internal)

## Purpose
This document defines the rules for retention offers for customers who are at risk of churn.

## Eligibility Rules
A customer is eligible for retention offers if at least ONE condition is true:
- Contract is "Month-to-month"
- Tenure is less than 12 months
- MonthlyCharges is greater than 80
- TechSupport is "No"

## Offer Limits
- Maximum discount allowed: 15% of MonthlyCharges
- Discount duration must be between 1 and 6 months
- Only ONE discount offer can be active at a time
- Do not offer discounts if the customer already has an active discount

## Special Rules
- If customer has "Fiber optic" internet AND TechSupport is "No":
    - Prefer offering a TechSupport trial instead of a large discount
- If PaymentMethod is "Electronic check":
    - Prefer recommending switching to "Credit card (automatic)" or "Bank transfer (automatic)"

## Communication Guidelines
- Always provide clear reasoning for the recommended actions
- Keep offers realistic and within policy limits
