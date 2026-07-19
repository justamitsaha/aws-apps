-- Create schema (similar to "use aws;" in MySQL)
CREATE SCHEMA IF NOT EXISTS aws;

-- Optional: set default schema for this session
SET search_path TO aws;

-- ----------------------------
-- customers
-- ----------------------------
CREATE TABLE aws.customers (
    customer_id BIGINT PRIMARY KEY,
    row_numbers INT,
    surname VARCHAR(100),
    credit_score INT,
    geography VARCHAR(50),
    gender VARCHAR(10),
    age INT,
    tenure INT,
    balance NUMERIC(15,2),
    num_of_products INT,
    has_cr_card BOOLEAN,
    is_active_member BOOLEAN,
    estimated_salary NUMERIC(15,2),
    exited BOOLEAN
);

-- ----------------------------
-- customer_churn
-- ----------------------------
CREATE TABLE aws.customer_churn (
    customer_id BIGINT PRIMARY KEY,
    unique_id VARCHAR(50),

    gender VARCHAR(10),
    senior_citizen BOOLEAN,
    partner BOOLEAN,
    dependents BOOLEAN,
    tenure INT,

    phone_service BOOLEAN,
    multiple_lines VARCHAR(20),
    internet_service VARCHAR(30),
    online_security VARCHAR(30),
    online_backup VARCHAR(30),
    device_protection VARCHAR(30),
    tech_support VARCHAR(30),
    streaming_tv VARCHAR(30),
    streaming_movies VARCHAR(30),

    contract VARCHAR(30),
    paperless_billing BOOLEAN,
    payment_method VARCHAR(50),

    monthly_charges NUMERIC(10,2),
    total_charges NUMERIC(12,2),
    churn BOOLEAN,

    CONSTRAINT fk_customer
        FOREIGN KEY (customer_id)
        REFERENCES aws.customers(customer_id)
);

-- ----------------------------
-- ai_interactions
-- ----------------------------


CREATE TABLE aws.ai_interactions (
    id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(50),
    raw_prompt TEXT,
    ai_response TEXT,   -- store JSON string
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- ----------------------------
-- Queries
-- ----------------------------
SELECT * FROM aws.customers;
SELECT * FROM aws.customer_churn;
SELECT * FROM aws.ai_interactions;
SELECT COUNT (*) FROM aws.customers;
SELECT COUNT (*) FROM aws.customer_churn;
SELECT COUNT (*) FROM aws.ai_interactions;

-- ----------------------------
-- Cleanup (Postgres doesn't need SQL_SAFE_UPDATES)
-- ----------------------------
DELETE FROM aws.customer_churn;
DELETE FROM aws.customers;
DELETE FROM aws.ai_interactions;
