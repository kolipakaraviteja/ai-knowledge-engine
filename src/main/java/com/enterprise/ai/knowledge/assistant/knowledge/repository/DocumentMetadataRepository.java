package com.enterprise.ai.knowledge.assistant.knowledge.repository;

import com.enterprise.ai.knowledge.assistant.document.dto.DocumentMetadata;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Slf4j
public class DocumentMetadataRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentMetadataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS document_metadata (" +
                "id UUID PRIMARY KEY, " +
                "document_name TEXT NOT NULL, " +
                "document_id VARCHAR(255) UNIQUE NOT NULL, " +
                "document_hash VARCHAR(64), " +
                "chunk_count INT DEFAULT 0, " +
                "file_size BIGINT, " +
                "pages INT DEFAULT 0, " +
                "characters INT DEFAULT 0, " +
                "uploaded_at TIMESTAMP, " +
                "indexed_at TIMESTAMP, " +
                "knowledge_base_id UUID, " +
                "collection_id UUID" +
                ")";
        jdbcTemplate.execute(sql);
    }

    public DocumentMetadata save(DocumentMetadata metadata) {
        String sql = "INSERT INTO document_metadata (id, document_name, document_id, document_hash, chunk_count, file_size, pages, characters, uploaded_at, indexed_at, knowledge_base_id, collection_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (document_id) DO UPDATE SET " +
                "document_name = EXCLUDED.document_name, " +
                "document_hash = EXCLUDED.document_hash, " +
                "chunk_count = EXCLUDED.chunk_count, " +
                "file_size = EXCLUDED.file_size, " +
                "pages = EXCLUDED.pages, " +
                "characters = EXCLUDED.characters, " +
                "uploaded_at = EXCLUDED.uploaded_at, " +
                "indexed_at = EXCLUDED.indexed_at, " +
                "knowledge_base_id = EXCLUDED.knowledge_base_id, " +
                "collection_id = EXCLUDED.collection_id";
        
        UUID id = metadata.documentId() != null ? UUID.fromString(metadata.documentId()) : UUID.randomUUID();
        jdbcTemplate.update(sql,
                id,
                metadata.documentName(),
                metadata.documentId() != null ? metadata.documentId() : UUID.randomUUID().toString(),
                metadata.documentHash(),
                metadata.chunkCount(),
                metadata.fileSize(),
                metadata.pages(),
                metadata.characters(),
                metadata.uploadedAt() != null ? Timestamp.from(metadata.uploadedAt()) : null,
                metadata.indexedAt() != null ? Timestamp.from(metadata.indexedAt()) : null,
                metadata.knowledgeBaseId() != null ? UUID.fromString(metadata.knowledgeBaseId()) : null,
                metadata.collectionId() != null ? UUID.fromString(metadata.collectionId()) : null
        );
        
        return metadata;
    }

    public Optional<DocumentMetadata> findByDocumentId(String documentId) {
        String sql = "SELECT * FROM document_metadata WHERE document_id = ?";
        List<DocumentMetadata> results = jdbcTemplate.query(sql, new DocumentMetadataRowMapper(), documentId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<DocumentMetadata> findByDocumentHash(String documentHash) {
        String sql = "SELECT * FROM document_metadata WHERE document_hash = ?";
        List<DocumentMetadata> results = jdbcTemplate.query(sql, new DocumentMetadataRowMapper(), documentHash);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<DocumentMetadata> findAll() {
        String sql = "SELECT * FROM document_metadata ORDER BY uploaded_at DESC";
        return jdbcTemplate.query(sql, new DocumentMetadataRowMapper());
    }

    public void updateChunkCount(String documentId, int chunkCount) {
        String sql = "UPDATE document_metadata SET chunk_count = ? WHERE document_id = ?";
        jdbcTemplate.update(sql, chunkCount, documentId);
    }

    public void updateIndexedAt(String documentId, Instant indexedAt) {
        String sql = "UPDATE document_metadata SET indexed_at = ? WHERE document_id = ?";
        jdbcTemplate.update(sql, indexedAt != null ? Timestamp.from(indexedAt) : null, documentId);
    }

    public void deleteByDocumentId(String documentId) {
        String sql = "DELETE FROM document_metadata WHERE document_id = ?";
        jdbcTemplate.update(sql, documentId);
    }

    public boolean existsByDocumentId(String documentId) {
        String sql = "SELECT COUNT(*) FROM document_metadata WHERE document_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, documentId);
        return count != null && count > 0;
    }

    public boolean existsByDocumentHash(String documentHash) {
        String sql = "SELECT COUNT(*) FROM document_metadata WHERE document_hash = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, documentHash);
        return count != null && count > 0;
    }

    public List<DocumentMetadata> findByCollectionId(String collectionId) {
        String sql = "SELECT * FROM document_metadata WHERE collection_id = ? ORDER BY uploaded_at DESC";
        return jdbcTemplate.query(sql, new DocumentMetadataRowMapper(), UUID.fromString(collectionId));
    }

    public List<DocumentMetadata> findByKnowledgeBaseId(String knowledgeBaseId) {
        String sql = "SELECT * FROM document_metadata WHERE knowledge_base_id = ? ORDER BY uploaded_at DESC";
        return jdbcTemplate.query(sql, new DocumentMetadataRowMapper(), UUID.fromString(knowledgeBaseId));
    }

    private static class DocumentMetadataRowMapper implements RowMapper<DocumentMetadata> {
        @Override
        public DocumentMetadata mapRow(ResultSet rs, int rowNum) throws SQLException {
            UUID knowledgeBaseId = (UUID) rs.getObject("knowledge_base_id");
            UUID collectionId = (UUID) rs.getObject("collection_id");
            return new DocumentMetadata(
                    rs.getString("document_id"),
                    rs.getString("document_name"),
                    rs.getString("document_hash"),
                    rs.getInt("chunk_count"),
                    rs.getLong("file_size"),
                    rs.getInt("pages"),
                    rs.getInt("characters"),
                    rs.getTimestamp("uploaded_at") != null ? rs.getTimestamp("uploaded_at").toInstant() : null,
                    rs.getTimestamp("indexed_at") != null ? rs.getTimestamp("indexed_at").toInstant() : null,
                    knowledgeBaseId != null ? knowledgeBaseId.toString() : null,
                    collectionId != null ? collectionId.toString() : null
            );
        }
    }
}
