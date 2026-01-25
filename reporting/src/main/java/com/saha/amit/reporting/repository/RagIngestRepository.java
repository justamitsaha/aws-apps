package com.saha.amit.reporting.repository;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
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

}
