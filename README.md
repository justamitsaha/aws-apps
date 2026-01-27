# AWS Apps - Customer Churn + AI Reporting + RAG

This repo contains two Spring Boot WebFlux services that work together:

- `fileReader` (port 8080): CSV ingestion, customer profile APIs, and static UI.
- `reporting` (port 8081): AI retention analysis + RAG ingestion/search.

Both services use PostgreSQL (pgvector) via R2DBC and share the same schema (`aws`).

## Repo layout

- `fileReader/` - CSV ingestion + customer profile service (serves the UI).
- `reporting/` - AI analysis + RAG service.
- `_setup/docker/docker-compose.yaml` - local Postgres (pgvector) container.
- `_files/` - sample CSVs (`Churn_Modelling.csv`, `CustomerChurn.csv`).
- `_rag-docs/` - sample policy documents for RAG uploads.
- `_documentation/` - AWS setup notes/scripts.

## Architecture

1. Upload CSVs to `fileReader` (SSE streaming endpoints).
2. `fileReader` stores customers + churn data in Postgres (`aws.customers`, `aws.customer_churn`).
3. `reporting` calls `fileReader` to fetch a combined `CustomerProfile`.
4. `reporting` calls OpenAI (Spring AI ChatClient) to generate a `RetentionPlan`.
5. `reporting` caches the plan by saving it back to `fileReader` (`aws.ai_interactions`).
6. For RAG, `reporting` chunks documents, embeds them, and stores chunks in pgvector.
7. `/reporting/{id}/analyze/rag` retrieves top policy chunks and generates a RAG-based plan.

## Quickstart (local)

1) Start Postgres:

```bash
docker compose -f _setup/docker/docker-compose.yaml up -d
```

2) Initialize schema/tables:

- Customers + churn + cache:
  - `fileReader/src/main/resources/postgress.sql`
- RAG documents + chunks + vector extension:
  - `reporting/src/main/resources/pgvector.sql`

3) Start `fileReader` (port 8080):

```bash
cd fileReader
./mvnw spring-boot:run
```

4) Start `reporting` (port 8081):

```bash
cd reporting
mvn spring-boot:run
```

5) Open UI:

- `http://localhost:8080/`

## Configuration

### Common

- `SPRING_R2DBC_URL` (default `r2dbc:postgresql://localhost:5432/aidb`)
- `SPRING_R2DBC_USERNAME` / `SPRING_R2DBC_PASSWORD` (default `aiuser` / `aipass`)
- `SERVER_PORT` to change ports

### fileReader

- `app.upload.dir` (default `/tmp/uploads`)
- Optional profiles in `fileReader/src/main/resources` for H2/MySQL/EC2

### reporting

- `APP_API_URL` (default `http://localhost:8080`)
- `OPENAI_API_KEY_PRACTICE` (required for chat + embeddings)
- `rag.chunk.size` (default `1000`)
- `rag.chunk.overlap` (default `200`)

## Service: fileReader (port 8080)

### CSV ingestion

- `POST /upload/customer` (multipart `file`) -> SSE stream of `CustomerEntity`
- `POST /upload/churn` (multipart `file`) -> SSE stream of `CustomerChurnEntity`
- `POST /upload/save-only` (multipart `file`) -> saves to disk
- `GET /upload/health`

Notes:

- Expected filenames: `Churn_Modelling.csv` and `CustomerChurn.csv`.
- Parsing is line-based and skips malformed rows.

### Customer profile + cache

- `GET /customerProfile/{id}` -> joined `CustomerProfile`
- `GET /customerProfile/customers?page=&size=` -> SSE stream of customers
- `GET /customerProfile/customersChurn?page=&size=` -> SSE stream of churn rows
- `DELETE /customerProfile/cleanup` -> deletes customers, churn, and AI cache
- `POST /customerProfile/recommendation` -> save AI plan (cache)
- `GET /customerProfile/{customerId}/recommendation` -> fetch latest cache entry

## Service: reporting (port 8081)

### AI retention analysis

- `GET /reporting/{id}/analyze` -> returns `RetentionPlan` (uses cache when available)
- `GET /reporting/{id}/analyze/nocache` -> always calls the model
- `GET /reporting/{id}/analyze/rag` -> uses policy documents (RAG) to generate plan
- `GET /reporting/health` -> proxy call to `fileReader` `/upload/health`

`RetentionPlan` fields: `riskLevel`, `reasoning[]`, `actions[]`, `offer`.

### RAG ingestion + search

- `POST /rag/upload` (multipart `file`) -> chunk + embed + store in pgvector
- `GET /rag/search?q=...&topK=...` -> semantic search results
- `POST /rag/upload2` -> streaming chunk preview (does not persist)

Chunking uses overlap; embeddings are stored as `vector(1536)`.

### Test endpoints

- `GET /ping/...` (demo endpoints for Mono/Flux behavior)

## UI (served by fileReader)

Pages in `fileReader/src/main/resources/static`:

- `index.html` - CSV upload portal (SSE logs)
- `customers.html` - paginated SSE viewers + cleanup
- `customer-profile.html` - single profile lookup
- `ai-report.html` - calls `reporting` and shows the plan
- `ai-rag-report.html` - calls `reporting/{id}/analyze/rag` (RAG-based plan)
- `rag-upload.html` - batch chunk upload + preview + search
- `rag-upload-stream.html` - streaming chunk preview (SSE)

More UI notes: `README_UI.md`.

## Sample data

- CSVs: `_files/Churn_Modelling.csv`, `_files/CustomerChurn.csv`
- RAG docs: `_rag-docs/*.md`

## Typical flow

1) Upload both CSVs in `index.html`.
2) Browse customers in `customers.html` or fetch a profile by ID.
3) Use `ai-report.html` to generate and cache a retention plan.
4) Upload policy docs in `rag-upload.html` and query with `/rag/search`.
5) Use `ai-rag-report.html` for the RAG-based retention plan.
