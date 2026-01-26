
### 1) Backend: CSV Ingestion + Customer APIs (Spring Boot WebFlux)

You created multiple reactive APIs to support CSV upload, ingestion, listing, cleanup, and profile lookup.

**CSV Upload APIs**

-   `POST /upload/customer`  
    Upload customer CSV and stream inserted customers as **SSE** (`TEXT_EVENT_STREAM`)

-   `POST /upload/churn`  
    Upload churn CSV and stream inserted churn records as **SSE**

-   `POST /upload/save-only`  
    Upload a file and save it to disk only (no DB), returns **plain text**


**Customer Data APIs**

-   `GET /customerProfile/{customerId}`  
    Fetch full customer profile JSON for a single customer ID

-   `GET /customerProfile/customers?page=&size=`  
    Stream customers as **SSE** with pagination

-   `GET /customerProfile/customersChurn?page=&size=`  
    Stream churn dataset as **SSE** with pagination

-   `DELETE /customerProfile/cleanup`  
    Delete all stored customer/churn data


----------

### 2) Backend: AI Reporting Service (separate service on port 8081)

You created an API that provides AI-based churn/retention analysis.

-   `GET http://localhost:8081/reporting/{customerId}/analyze`  
    Returns JSON with:

    -   `riskLevel`

    -   `reasoning[]`

    -   `actions[]`

    -   `offer{...}`


This demonstrates **inter-application communication** and AI-style decision responses.

----------

### 3) Backend: RAG (Document Chunking + Search)

You added a RAG module to upload documents and generate chunks, and later search over them.

**Chunking APIs**

-   `POST /rag/upload` (batch mode)  
    Upload file → returns `List<Chunk>`

-   Optional streaming version:  
    `POST /rag/upload` returning `Flux<Chunk>` with `TEXT_EVENT_STREAM_VALUE` (SSE)


Chunk model:

`public  record  Chunk(int index, String text) {}`

**Search API**

-   `GET /rag/search?q=...&topK=...`  
    Returns `Flux<ChunkMatch>`


ChunkMatch model:

`public  record  ChunkMatch(
  Long chunkId,
  Long documentId,
  String fileName,
  Integer chunkIndex,
  String chunkText,
  Double score
) {}`

----------

## Summary of UI you built (Bootstrap + jQuery + separate JS/CSS)
The UI code is in the `src/main/resources/static` folder of the main Spring Boot app. `fileReader`

### Shared UI structure

-   Separate files:

    -   HTML pages

    -   `css/styles.css`

    -   page-specific JS in `js/*.js`

-   Bootstrap styling for professional look

-   A shared **navbar** for navigation across pages


----------

### UI Pages created

#### 1) `index.html` (CSV Upload Portal)

Buttons + file input to call:

-   `/upload/customer` (SSE stream handling using fetch stream reader)

-   `/upload/churn` (SSE stream handling)

-   `/upload/save-only` (normal fetch + text response)


Shows streamed output in a log-style UI.

----------

#### 2) `customers.html` (Customer Management)

Supports:

-   paginated SSE listing:

    -   `/customerProfile/customers?page=&size=`

    -   `/customerProfile/customersChurn?page=&size=`

-   dataset selector dropdown (Customers vs CustomersChurn)

-   Prev / Next pagination buttons

-   Cleanup button:

    -   `DELETE /customerProfile/cleanup`


----------

#### 3) `customer-profile.html` (Customer Profile Lookup)

Input customerId → calls:

-   `GET /customerProfile/{id}`  
    Shows:

-   formatted summary card

-   raw JSON response


----------

#### 4) `ai-report.html` (AI Prediction Report)

Input customerId → calls:

-   `GET http://localhost:8081/reporting/{id}/analyze`  
    Shows:

-   risk level badge

-   reasoning list

-   actions cards with priority

-   offer block

-   raw JSON


----------

#### 5) `rag-upload.html` (RAG Chunk Generator – batch response)

Upload file → calls:

-   `POST /rag/upload` returning `List<Chunk>`  
    Features:

-   chunk preview

-   show all / show less toggle

-   local filter search inside returned chunks

-   raw JSON


----------

#### 6) `rag-upload-stream.html` (RAG Chunk Generator – streaming SSE)

Upload file → calls:

-   `POST /rag/upload` returning `Flux<Chunk>` as SSE  
    Features:

-   live chunk streaming preview

-   show all / show less toggle

-   search inside streamed chunks

-   raw JSON collected incrementally


----------

#### 7) RAG Search UI (planned/added into rag-upload page)

Search form to call:

-   `GET /rag/search?q=...&topK=...`  
    Displays ranked results using score + chunk text.


----------

## Key technical concepts you covered

-   Multipart upload in WebFlux (`FilePart`)

-   Reactive streaming responses (`Flux<T>`)

-   SSE streaming (`TEXT_EVENT_STREAM_VALUE`)

-   UI streaming consumption using:

    -   `fetch() + response.body.getReader()`

-   Pagination in SSE endpoints

-   Two backend apps on different ports (8080 + 8081)

-   Basic UI navigation with Bootstrap navbar

-   Chunking + retrieval patterns (RAG foundation)