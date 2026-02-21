# GEMINI.md - Project Context

## Project Overview
**AWS Apps - Customer Churn + AI Reporting + RAG** is a two-service system built with **Java 21** and **Spring Boot WebFlux**. It focuses on customer churn analysis, AI-driven retention planning, and RAG (Retrieval-Augmented Generation) capabilities using **pgvector**.

### Architecture
Detailed flow diagrams for the APIs can be found in [API_FLOWS.md](API_FLOWS.md).

1. **`fileReader` (Port 8080)**:
    - Responsible for CSV ingestion (via SSE streaming) using `CsvIngestionService`.
    - Manages customer profiles and churn data via `CustomerService`.
    - Manages AI recommendation cache via `AiCacheService`.
    - Serves the static UI (HTML/Bootstrap/jQuery).
    - Database: PostgreSQL (schema: `aws`, tables: `customers`, `customer_churn`, `ai_interactions`).

2. **`reporting` (Port 8081)**:
    - Provides AI retention analysis via `RetentionService`.
    - Implements RAG ingestion and search via `RagService`.
    - Centralized AI/LLM logic in `LlmService`.
    - Document processing and chunking in `DocumentChunkingService`.
    - Integrates with OpenAI via **Spring AI**.
    - Database: PostgreSQL (schema: `aws`, tables: `documents`, `document_chunks` using `vector(1536)`).

### Core Technologies
- **Backend**: Java 21, Spring Boot (WebFlux, R2DBC), Spring AI (OpenAI).
- **Database**: PostgreSQL with `pgvector` for similarity search.
- **Frontend**: Plain HTML, Bootstrap, jQuery (served as static resources from `fileReader`).
- **Infrastructure**: Docker Compose for local PostgreSQL.

## Building and Running

### 1. Prerequisites
- Docker & Docker Compose.
- Java 21 JDK.
- Maven (or use included `mvnw`).
- OpenAI API Key (configured via environment variable `OPENAI_API_KEY_PRACTICE`).

### 2. Infrastructure Setup
Start the local PostgreSQL (pgvector) container:
```bash
docker compose -f _setup/docker/docker-compose.yaml up -d
```

### 3. Database Initialization
Execute the following SQL scripts in your PostgreSQL instance:
- `fileReader/src/main/resources/postgress.sql` (Schema, Customers, Churn, AI Interaction cache)
- `reporting/src/main/resources/pgvector.sql` (Vector extension, Documents, Chunks)

### 4. Running the Services
**Start `fileReader`:**
```bash
cd fileReader
./mvnw spring-boot:run
```

**Start `reporting`:**
```bash
cd reporting
./mvnw spring-boot:run
```

### 5. Accessing the Application
- **UI Portal**: `http://localhost:8080/`
- **Health Check**: `http://localhost:8080/upload/health`

## Development Conventions

### Coding Style & Patterns
- **Reactive Programming**: The system is fully reactive using **Project Reactor** (`Mono`, `Flux`).
- **Streaming APIs**: Uses **Server-Sent Events (SSE)** for long-running processes like CSV ingestion and RAG document chunking.
- **R2DBC**: Reactive database access via Spring Data R2DBC.
- **Service Integration**: `reporting` calls `fileReader` to fetch consolidated customer profiles.
- **AI Integration**: Uses `Spring AI ChatClient` for LLM calls and `EmbeddingModel` for vector generation.

### Directory Structure
- `fileReader/`: Spring Boot app for data ingestion and UI.
- `reporting/`: Spring Boot app for AI and RAG logic.
- `_files/`: Sample CSV datasets (`Churn_Modelling.csv`, `CustomerChurn.csv`).
- `_rag-docs/`: Sample Markdown/Text files for RAG ingestion.
- `_setup/`: Docker configuration for the database.
- `_documentation/`: Deployment notes for EC2 and S3 environments.

### Environment Variables
- `SPRING_R2DBC_URL`: R2DBC connection string.
- `SPRING_R2DBC_USERNAME` / `SPRING_R2DBC_PASSWORD`: DB credentials.
- `OPENAI_API_KEY_PRACTICE`: Required for AI features in `reporting`.
- `APP_API_URL`: Base URL for `fileReader` (used by `reporting`).
