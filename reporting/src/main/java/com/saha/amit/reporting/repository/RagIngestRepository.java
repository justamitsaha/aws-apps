package com.saha.amit.reporting.repository;

import com.saha.amit.reporting.model.ChunkMatch;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class RagIngestRepository {

    private final DatabaseClient client;

    public RagIngestRepository(DatabaseClient client) {
        this.client = client;
    }

    public Mono<Long> insertDocument(String fileName) {
        return client.sql("""
                        INSERT INTO aws.documents(file_name)
                        VALUES (:fileName)
                        RETURNING id
                        """)
                .bind("fileName", fileName)
                .map(row -> row.get("id", Long.class))
                .one();
    }

    public Mono<Void> insertChunk(Long documentId, int chunkIndex, String chunkText, String embeddingVectorLiteral) {
        return client.sql("""
                        INSERT INTO aws.document_chunks(document_id, chunk_index, chunk_text, embedding)
                        VALUES (:documentId, :chunkIndex, :chunkText, CAST(:embedding AS public.vector(1536)))
                        """)
                .bind("documentId", documentId)
                .bind("chunkIndex", chunkIndex)
                .bind("chunkText", chunkText)
                .bind("embedding", embeddingVectorLiteral)
                .then();
    }


    public Flux<ChunkMatch> searchSimilarChunks(String queryEmbeddingLiteral, int topK) {
        return client.sql("""
                        SELECT
                            c.id AS chunk_id,
                            c.document_id,
                            d.file_name,
                            c.chunk_index,
                            c.chunk_text,
                            1 - (c.embedding OPERATOR(public.<=>) CAST(:q AS public.vector(1536))) AS score
                        FROM aws.document_chunks c
                        JOIN aws.documents d ON d.id = c.document_id
                        ORDER BY c.embedding OPERATOR(public.<=>) CAST(:q AS public.vector(1536))
                        LIMIT :topK
                        """)
                .bind("q", queryEmbeddingLiteral)
                .bind("topK", topK)
                .map((row, meta) -> new ChunkMatch(
                        row.get("chunk_id", Long.class),
                        row.get("document_id", Long.class),
                        row.get("file_name", String.class),
                        row.get("chunk_index", Integer.class),
                        row.get("chunk_text", String.class),
                        row.get("score", Double.class)
                ))
                .all();
    }


}
