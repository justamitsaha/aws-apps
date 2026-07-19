# GEMINI.md - Python FastAPI Apps

## Project Overview
This project is a Python-based implementation of a two-service system originally written in Java (Spring Boot). It focuses on **customer churn analysis**, **AI-driven retention planning**, and **RAG (Retrieval-Augmented Generation)** capabilities using **FastAPI** and **PostgreSQL** with the `pgvector` extension.

**Note:** The UI has been moved to a separate Angular application and is no longer served by these backend services.

### Core Functionalities
- **CSV Ingestion**: Upload and process customer and churn data from CSV files using Pandas.
- **Customer Profiles**: Manage and retrieve consolidated customer profiles.
- **AI Retention Analysis**: Generate personalized retention plans using LLMs (GPT-4o), with caching of results.
- **RAG System**: Ingest documents, chunk them, generate embeddings, and perform similarity searches for grounded AI responses.

### Architecture
The application follows a modular structure:
1.  **`app/file_reader`**: Mirroring the Java `fileReader` service.
    - Handles CSV uploads for `customers` and `customer_churn`.
    - Manages `ai_interactions` cache.
    - Provides customer profile data.
2.  **`app/reporting`**: Mirroring the Java `reporting` service.
    - Orchestrates AI analysis and RAG logic.
    - Manages document ingestion and similarity search using `pgvector`.
3.  **`app/shared`**: Centralized logic for:
    - **Database**: SQLAlchemy models and async session management.
    - **LLM**: OpenAI client for chat completions and embeddings.
    - **Config**: Pydantic-based settings and environment variable management.

## Building and Running

### 1. Prerequisites
- Python 3.10+
- PostgreSQL instance with `pgvector` extension installed.
- OpenAI API Key.

### 2. Setup
Install dependencies:
```bash
pip install -r requirements.txt
```

### 3. Configuration
Create a `.env` file in the root directory or set the following environment variables:
- `SPRING_R2DBC_URL`: PostgreSQL connection string (e.g., `postgresql+asyncpg://user:pass@localhost:5432/db`)
- `OPENAI_API_KEY_PRACTICE`: Your OpenAI API key.
- `APP_API_URL`: Base URL for the application (default: `http://localhost:8000`).

### 4. Database Initialization
Ensure the `aws` schema exists in your PostgreSQL database. The models in `app/shared/models.py` define the tables.
*Note: You may need to run a migration or use a script to create the schema and tables initially if not handled automatically.*

### 5. Running the Application
Start the FastAPI server:
```bash
python -m app.main
```
or
```bash
python app/main.py
```
- Default port is 8000.
- Access the health check at `http://localhost:8000/upload/health`.
- Interactive API docs (Swagger) are available at `http://localhost:8000/docs`.

## Development Conventions

### Coding Style & Patterns
- **Asynchronous Programming**: Uses `async`/`await` throughout for non-blocking I/O (FastAPI, SQLAlchemy, httpx).
- **Data Validation**: Rigorous use of **Pydantic** (v2) for request/response schemas and application settings.
- **ORM**: **SQLAlchemy 2.0** with `asyncio` support for database interactions.
- **Vector Search**: Uses `pgvector` with SQLAlchemy to perform similarity searches directly in the database.
- **Separation of Concerns**: Logic is divided into `routers.py` (endpoints), `service.py` (business logic), and `schemas.py` (data models).

### Key Files
- `app/main.py`: Application entry point and router inclusion.
- `app/shared/models.py`: Unified SQLAlchemy models for all tables.
- `app/shared/llm.py`: Wrapper for OpenAI service calls.
- `app/reporting/service.py`: Implementation of RAG and retention logic.
- `app/file_reader/service.py`: Implementation of CSV ingestion and customer profile logic.
