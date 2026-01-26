CREATE SCHEMA IF NOT EXISTS aws;

DROP TABLE IF EXISTS aws.document_chunks;

DROP TABLE IF EXISTS aws.documents;


CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS aws.documents (
  id BIGSERIAL PRIMARY KEY,
  file_name TEXT NOT NULL,
  content TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS aws.document_chunks (
  id BIGSERIAL PRIMARY KEY,
  document_id BIGINT REFERENCES aws.documents(id) ON DELETE CASCADE,
  chunk_index INT NOT NULL,
  chunk_text TEXT NOT NULL,
  embedding vector(1536) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id
ON aws.document_chunks(document_id);

-- Vector index (choose one)
CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding
ON aws.document_chunks USING ivfflat (embedding vector_cosine_ops);

-- verification 

SELECT n.nspname AS schema, t.typname
FROM pg_type t
JOIN pg_namespace n ON n.oid = t.typnamespace
WHERE t.typname = 'vector';


SELECT extname FROM pg_extension WHERE extname='vector';


SELECT * FROM aws.documents;
SELECT * FROM aws.document_chunks;
TRUNCATE TABLE aws.documents CASCADE;
TRUNCATE TABLE aws.document_chunks;

