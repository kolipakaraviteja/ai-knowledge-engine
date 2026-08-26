package com.enterprise.ai.knowledge.assistant.knowledge.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DocumentCollectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentCollectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS document_collections (" +
                "id UUID PRIMARY KEY, " +
                "document_id VARCHAR(255) NOT NULL, " +
                "collection_id UUID NOT NULL, " +
                "created_at TIMESTAMP, " +
                "CONSTRAINT fk_doc_collections_collection FOREIGN KEY (collection_id) REFERENCES collections(id) ON DELETE CASCADE, " +
                "CONSTRAINT uk_document_collection UNIQUE(document_id, collection_id)" +
                ")";
        jdbcTemplate.execute(sql);

        // Create indexes
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_document_collections_doc ON document_collections(document_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_document_collections_coll ON document_collections(collection_id)");
    }

    public void associateDocumentWithCollection(String documentId, UUID collectionId) {
        String sql = "INSERT INTO document_collections (id, document_id, collection_id, created_at) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT (document_id, collection_id) DO NOTHING";
        jdbcTemplate.update(sql, UUID.randomUUID(), documentId, collectionId, Timestamp.from(Instant.now()));
    }

    public void associateDocumentWithCollections(String documentId, List<UUID> collectionIds) {
        for (UUID collectionId : collectionIds) {
            associateDocumentWithCollection(documentId, collectionId);
        }
    }

    public List<UUID> getCollectionsByDocumentId(String documentId) {
        String sql = "SELECT collection_id FROM document_collections WHERE document_id = ?";
        return jdbcTemplate.queryForList(sql, UUID.class, documentId);
    }

    public List<String> getDocumentsByCollectionId(UUID collectionId) {
        String sql = "SELECT DISTINCT document_id FROM document_collections WHERE collection_id = ?";
        return jdbcTemplate.queryForList(sql, String.class, collectionId);
    }

    public void removeDocumentFromCollection(String documentId, UUID collectionId) {
        String sql = "DELETE FROM document_collections WHERE document_id = ? AND collection_id = ?";
        jdbcTemplate.update(sql, documentId, collectionId);
    }

    public void removeAllCollectionsFromDocument(String documentId) {
        String sql = "DELETE FROM document_collections WHERE document_id = ?";
        jdbcTemplate.update(sql, documentId);
    }

    public void removeAllDocumentsFromCollection(UUID collectionId) {
        String sql = "DELETE FROM document_collections WHERE collection_id = ?";
        jdbcTemplate.update(sql, collectionId);
    }

    public boolean isDocumentInCollection(String documentId, UUID collectionId) {
        String sql = "SELECT COUNT(*) FROM document_collections WHERE document_id = ? AND collection_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, documentId, collectionId);
        return count != null && count > 0;
    }
}
