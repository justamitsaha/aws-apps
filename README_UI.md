# UI Guide (fileReader static pages)

The UI is served by `fileReader` from `fileReader/src/main/resources/static` at:

- `http://localhost:8080/`

It is plain HTML + Bootstrap + jQuery with page-specific JavaScript in `fileReader/src/main/resources/static/js`.

## Pages

- `index.html` - CSV upload portal for `Churn_Modelling.csv` and `CustomerChurn.csv`
- `customers.html` - paginated SSE list for customers or churn dataset + cleanup
- `customer-profile.html` - single customer profile lookup
- `ai-report.html` - calls `reporting` service for `/reporting/{id}/analyze`
- `rag-upload.html` - batch RAG upload + preview + search
- `rag-upload-stream.html` - streaming RAG chunk preview (SSE)

## How the UI streams data

- Upload and list pages use `fetch()` + `response.body.getReader()` to consume SSE (`text/event-stream`).
- The `reporting` service allows CORS from `http://localhost:8080` so the UI can call port 8081.
