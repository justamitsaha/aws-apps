# API Flows - Mermaid Diagrams

This document illustrates the logical flow of key APIs in the system, showing the interactions between the API Controllers, Services, Databases, and LLM (OpenAI).

## 1. Customer Retention Analysis (`/retention/{id}/analyze`)

This flow shows how the system attempts to fetch a cached retention plan before falling back to AI generation.

```mermaid
graph TD
    Client[Client] --> Controller[CustomerRetentionController]
    Controller --> Service[RetentionService]
    
    subgraph "External Data"
        Service --> |Fetch Profile| FileReader[fileReader Service]
        FileReader --> |Query DB| DB_Cust[(aws.customers / churn)]
    end
    
    Service --> |Check Cache| AiCache[AiCacheService in fileReader]
    AiCache --> |Query DB| DB_AI[(aws.ai_interactions)]
    
    AiCache -.-> |Cache Hit| Service
    AiCache -.-> |Cache Miss| Service
    
    subgraph "AI Generation"
        Service --> |Request Plan| LlmService[LlmService]
        LlmService --> |Prompt| OpenAI[OpenAI API]
        OpenAI --> |JSON Response| LlmService
    end
    
    Service --> |Save Result| AiCache
    Service --> |Return Plan| Client
```
![API diagram](./_files/Customer_retention_API.png)

---

## 2. RAG-Based Retention Analysis (`/retention/{id}/analyze/rag`)

This flow demonstrates how policy documents are retrieved from the vector database to ground the AI's retention recommendation.

```mermaid
graph TD
    Client[Client] --> Controller[CustomerRetentionController]
    Controller --> Service[RetentionService]
    
    Service --> |Fetch Profile| FileReader[fileReader Service]
    
    subgraph "Vector Search"
        Service --> |Search Policy| RagService[RagService]
        RagService --> |Embed Query| Embedding[EmbeddingModel]
        RagService --> |Vector Search| DB_Vector[(aws.document_chunks)]
    end
    
    subgraph "Grounded AI"
        Service --> |Prompt + Context| LlmService[LlmService]
        LlmService --> |Prompt| OpenAI[OpenAI API]
    end
    
    Service --> |Return Grounded Plan| Client
```
![API diagram](./_files/Customer_RAG_retention_API.png)

---

## 3. General RAG Ingestion (`/rag/upload`)

This flow shows the streaming ingestion process where documents are chunked, embedded, and stored in the vector database.

```mermaid
graph TD
    Client[Client] --> Controller[RagController]
    Controller --> RagService[RagService]
    RagService --> |Create Doc Record| DB_Docs[(aws.documents)]
    
    subgraph "Streaming Process (SSE)"
        Controller --> |Stream Segments| ChunkService[DocumentChunkingService]
        ChunkService --> |New Chunk| RagService
        RagService --> |Embed Text| Embedding[EmbeddingModel]
        RagService --> |Store Vector| DB_Chunks[(aws.document_chunks)]
        RagService --> |Emit Progress| Client
    end
```
![API diagram](./_files/RAG_upload_API.png)

---

## 4. Document Q&A (`/rag/{id}/ask`)

This flow illustrates the semantic search and context-based answering for a specific document.

```mermaid
graph TD
    Client[Client] --> Controller[RagController]
    Controller --> RagService[RagService]
    
    subgraph "Context Retrieval"
        RagService --> |Embed Question| Embedding[EmbeddingModel]
        RagService --> |Similarity Search| DB_Chunks[(aws.document_chunks)]
    end
    
    subgraph "Answering"
        RagService --> |Question + Chunks| LlmService[LlmService]
        LlmService --> |Prompt| OpenAI[OpenAI API]
    end
    
    RagService --> |Answer + Sources| Client
```
![API diagram](./_files/RAG_ask_API.png)
