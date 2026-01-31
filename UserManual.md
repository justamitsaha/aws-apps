# User Manual - Customer Churn + AI Reporting + RAG

## Overview

This application is a two-service system:

- `fileReader` (port 8080) ingests CSV data, stores customers + churn data, and serves the UI.
- `reporting` (port 8081) generates AI retention plans and provides two RAG flows:
  - General document Q&A (non-retention).
  - Retention policy RAG used to ground customer retention recommendations.

The system is designed to explore reactive data ingestion, AI reasoning, and RAG retrieval with pgvector.

## Key features

- CSV upload with streaming results (SSE).
- Customer and churn dataset browsing with pagination.
- Joined customer profile lookup by ID.
- AI retention plan generation and caching.
- RAG document ingestion with chunking + embeddings (general and retention-specific).
- Semantic search across ingested documents.
- RAG-based retention analysis endpoint (`/retention/{id}/analyze/rag`).

## Technology used

- Spring Boot WebFlux (reactive APIs + SSE).
- R2DBC with PostgreSQL.
- pgvector for embeddings storage and similarity search.
- Spring AI (OpenAI chat + embeddings).
- Bootstrap + jQuery UI pages.

## UI usage guide

Open the UI at `http://localhost:8080/`.

### 1) General RAG Upload + Search (`index.html`)

Purpose: upload any document (non-retention), stream chunks, and do vector search.

- Upload a document and click **Upload & Stream**.
- Live chunks appear in the preview.
- Use the **RAG Search** box to run `/rag/search`.

### 2) CSV Uploads (`customer_csv_upload.html`)

Purpose: upload the two customer datasets and stream inserted records.

- Upload `Churn_Modelling.csv` using **Upload Customer File**.
- Upload `CustomerChurn.csv` using **Upload Churn File**.
- Output streams live in the log panel.

### 3) Customers & Churn (`customers.html`)

Purpose: browse stored data with pagination and clean up.

- Choose dataset (Customers or CustomersChurn).
- Enter page + size, then click **Load Page**.
- Use **Prev/Next** for pagination.
- Use **Cleanup** to delete all customer + churn + cached AI data.

### 4) Customer Profile (`customer-profile.html`)

Purpose: fetch the joined profile for a single customer ID.

- Enter a customer ID.
- View a formatted summary and raw JSON.

### 5) Customer Retention (`customer-Retention.html`)

Purpose: generate an AI retention plan (cached on subsequent calls).

- Enter a customer ID.
- The app calls `GET /retention/{id}/analyze`.
- Results show risk level, reasoning, actions, and offer.

### 6) Policy Upload (Retention RAG) (`policy-upload.html`)

Purpose: upload retention policy documents, chunk them, embed them, and store in pgvector.

- Upload a text/markdown policy file (examples in `_rag-docs/`).
- View generated chunks.
- Use the built-in search to call `GET /retention/policySearch`.

### 7) Policy-based Retention (`policy-based-Retention.html`)

Purpose: generate a retention plan grounded in policy documents.

- Upload policy docs first (see Policy Upload).
- Enter a customer ID.
- The app calls `GET /retention/{id}/analyze/rag`.
- Results are generated using both customer data and retrieved policy chunks.

## Tips

- If you change ports, update the base URLs in the JS files under `fileReader/src/main/resources/static/js`.
- The RAG-based retention analysis works best after you upload several policy documents.
