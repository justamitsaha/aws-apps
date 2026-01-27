# User Manual - Customer Churn + AI Reporting + RAG

## Overview

This application is a two-service system:

- `fileReader` (port 8080) ingests CSV data, stores customers + churn data, and serves the UI.
- `reporting` (port 8081) generates AI retention plans and provides RAG-based analysis using policy documents.

The system is designed to explore reactive data ingestion, AI reasoning, and RAG retrieval with pgvector.

## Key features

- CSV upload with streaming results (SSE).
- Customer and churn dataset browsing with pagination.
- Joined customer profile lookup by ID.
- AI retention plan generation and caching.
- RAG document ingestion with chunking + embeddings.
- Semantic search across policy documents.
- RAG-based retention analysis endpoint (`/reporting/{id}/analyze/rag`).

## Technology used

- Spring Boot WebFlux (reactive APIs + SSE).
- R2DBC with PostgreSQL.
- pgvector for embeddings storage and similarity search.
- Spring AI (OpenAI chat + embeddings).
- Bootstrap + jQuery UI pages.

## UI usage guide

Open the UI at `http://localhost:8080/`.

### 1) CSV Uploads (`index.html`)

Purpose: upload the two datasets and stream inserted records.

- Upload `Churn_Modelling.csv` using **Upload Customer**.
- Upload `CustomerChurn.csv` using **Upload Churn**.
- Output streams live in the log panel.

### 2) Customers & Churn (`customers.html`)

Purpose: browse stored data with pagination and clean up.

- Choose dataset (Customers or CustomersChurn).
- Enter page + size, then click **Load Page**.
- Use **Prev/Next** for pagination.
- Use **Cleanup** to delete all customer + churn + cached AI data.

### 3) Customer Profile (`customer-profile.html`)

Purpose: fetch the joined profile for a single customer ID.

- Enter a customer ID.
- View a formatted summary and raw JSON.

### 4) AI Report (`ai-report.html`)

Purpose: generate an AI retention plan (cached on subsequent calls).

- Enter a customer ID.
- The app calls `GET /reporting/{id}/analyze`.
- Results show risk level, reasoning, actions, and offer.

### 5) RAG Upload (`rag-upload.html`)

Purpose: upload policy documents, chunk them, embed them, and store in pgvector.

- Upload a text/markdown policy file (examples in `_rag-docs/`).
- View generated chunks.
- Use the built-in search to call `GET /rag/search`.

### 6) RAG Upload (Stream) (`rag-upload-stream.html`)

Purpose: view streaming chunk generation (SSE).

- Upload a policy file.
- Chunks stream live as the file is processed.

### 7) AI RAG Report (`ai-rag-report.html`)

Purpose: generate a retention plan grounded in policy documents.

- Upload policy docs first (see RAG Upload).
- Enter a customer ID.
- The app calls `GET /reporting/{id}/analyze/rag`.
- Results are generated using both customer data and retrieved policy chunks.

## Tips

- If you change ports, update the base URLs in the JS files under `fileReader/src/main/resources/static/js`.
- The RAG-based analysis works best after you upload several policy documents.
