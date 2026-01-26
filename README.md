
## 1) Customer Data + Churn Data Platform (Application 1)

### What it does

-   Accepts uploads of **two CSV datasets**

    -   Customer profile dataset (`customers`)

    -   Customer churn dataset (`customer_churn`)

-   Persists both into a relational database (H2/MySQL)

-   Exposes a **joined “CustomerProfile” endpoint** that returns a combined view


### Key output

`GET /customerProfile/{customerId}` returns one clean object with fields like:

-   age, gender, geography

-   tenure, contract

-   monthlyCharges, totalCharges

-   techSupport, internetService

-   paymentMethod, etc.


This service acts as your **structured data provider**.

----------

## 2) AI Reporting / Reasoning Service (Application 2)

### What it does

-   Exposes: `GET /reporting/{customerId}/analyze`

-   Calls Application 1 to fetch the combined customer profile

-   Sends selected fields to an LLM using **Spring AI ChatClient**

-   Returns a **structured response** (`RetentionPlan`) with:

    -   riskLevel

    -   reasoning

    -   actions

    -   discountCode


This service acts as your **GenAI reasoning microservice**.

----------

## 3) RAG Document Ingestion (New Phase Work)

### What it does

You built a **document upload + chunking + embedding pipeline** that stores data in **Postgres + pgvector** using **R2DBC**.

### Data model

-   `aws.documents` → stores uploaded file metadata + full content

-   `aws.document_chunks` → stores:

    -   chunk_index

    -   chunk_text

    -   embedding vector(1536)


### Upload endpoint behavior

`POST /upload (multipart)`

-   reads file content

-   splits into overlapping chunks (chunkSize=1000, overlap=200)

-   generates embeddings using an embedding model (OpenAI)

-   stores embeddings in pgvector


This is your **RAG knowledge base ingestion layer**.

----------

## Architecture summary (one line)

You have built a **two-microservice GenAI system** where:

-   one service manages **structured customer data**

-   another service generates **AI retention recommendations**  
    and you’ve started adding **RAG capability** by storing document embeddings in **pgvector** for semantic retrieval.


----------

## What’s left to complete RAG

The next missing piece is:

-   `GET /rag/search?q=...&topK=...`

    -   embed query

    -   retrieve top chunks from pgvector

    -   feed those chunks into the LLM prompt during `/analyze`