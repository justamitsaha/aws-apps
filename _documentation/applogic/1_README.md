**2-service GenAI backend system** where:

### **Application 1 = Data Ingestion + Profile Aggregation Service**

**Responsibilities**

- Upload **Customer** CSV + **Customer Churn** CSV
- Persist both datasets into DB tables (`customers`, `customer_churn`)
- Expose a **joined "customer profile" API** that returns a single combined view

**Key output**  
`GET /customerProfile/{customerId}` returns a normalized profile DTO like:

`{ "customerId":15565701, "age":39, "gender":"Female", "geography":"Spain", "seniorCitizen":false, "tenure":9, "isActiveMember":false, "internetService":"Fiber optic", "techSupport":"Yes", "streamingTv":"Yes", "monthlyCharges":107.5, "totalCharges":3242.5, "balance":161993.89, "contract":"Month-to-month", "paymentMethod":"Electronic check" }`

This is the correct "AI-ready input object" layer.

----------

### **Application 2 = AI Reasoning / Reporting Service**

**Responsibilities**

- Calls Application 1 to fetch the joined customer profile
- Sends selected fields to an LLM using Spring AI `ChatClient`
- Returns a **structured output** (`RetentionPlan`) as JSON

**Key retention endpoints**

- `GET /retention/{customerId}/analyze` (cached)
- `GET /retention/{customerId}/analyze/nocache`
- `GET /retention/{customerId}/analyze/rag` (policy-grounded)
- `POST /retention/policyUpload` (ingest policy docs)
- `GET /retention/policySearch?q=...&topK=...`

**General RAG endpoints (non-retention)**

- `POST /rag/upload` (stream chunks + persist)
- `GET /rag/search?q=...&topK=...`

Example output:

`{ "riskLevel":"Medium", "reasoning":"...", "actions":[ "...", "...", "..." ], "discountCode":"SAVE10" }`
