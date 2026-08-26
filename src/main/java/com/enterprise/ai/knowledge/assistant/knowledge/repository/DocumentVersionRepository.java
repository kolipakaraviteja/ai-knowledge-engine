package com.enterprise.ai.knowledge.assistant.knowledge.repository;

import com.enterprise.ai.knowledge.assistant.knowledge.entity.DocumentVersion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DocumentVersionRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentVersionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS document_versions (" +
                "id UUID PRIMARY KEY, " +
                "document_id VARCHAR(255) NOT NULL, " +
                "version_number INT NOT NULL, " +
                "chunk_count INT, " +
                "embedding_model VARCHAR(100), " +
                "created_at TIMESTAMP, " +
                "is_active BOOLEAN DEFAULT true" +
                ")";
        jdbcTemplate.execute(sql);
    }

    public DocumentVersion save(DocumentVersion documentVersion) {
        UUID id = documentVersion.getId() == null ? UUID.randomUUID() : documentVersion.getId();
        Instant now = documentVersion.getCreatedAt() == null ? Instant.now() : documentVersion.getCreatedAt();

        String sql = "INSERT INTO document_versions (id, document_id, document_name, version_number, chunk_count, embedding_model, created_at, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, id, documentVersion.getDocumentId(), documentVersion.getDocumentName(), documentVersion.getVersionNumber(),
                          documentVersion.getChunkCount(), documentVersion.getEmbeddingModel(),
                          Timestamp.from(now), documentVersion.getIsActive());

        documentVersion.setId(id);
        documentVersion.setCreatedAt(now);
        return documentVersion;
    }

    public Optional<DocumentVersion> findById(UUID id) {
        String sql = "SELECT id, document_id, document_name, version_number, chunk_count, embedding_model, created_at, is_active " +
                     "FROM document_versions WHERE id = ?";
        try {
            DocumentVersion dv = jdbcTemplate.queryForObject(sql, documentVersionRowMapper(), id);
            return Optional.ofNullable(dv);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<DocumentVersion> findByDocumentId(String documentId) {
        String sql = "SELECT id, document_id, document_name, version_number, chunk_count, embedding_model, created_at, is_active " +
                     "FROM document_versions WHERE document_id = ? ORDER BY version_number DESC";
        return jdbcTemplate.query(sql, documentVersionRowMapper(), documentId);
    }

    public Optional<DocumentVersion> findActiveVersion(String documentId) {
        String sql = "SELECT id, document_id, document_name, version_number, chunk_count, embedding_model, created_at, is_active " +
                     "FROM document_versions WHERE document_id = ? AND is_active = true ORDER BY version_number DESC LIMIT 1";
        try {
            DocumentVersion dv = jdbcTemplate.queryForObject(sql, documentVersionRowMapper(), documentId);
            return Optional.ofNullable(dv);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void setActiveVersion(String documentId, Integer versionNumber) {
        // Deactivate all versions for this document
        String deactivateSql = "UPDATE document_versions SET is_active = false WHERE document_id = ?";
        jdbcTemplate.update(deactivateSql, documentId);

        // Activate the specified version
        String activateSql = "UPDATE document_versions SET is_active = true WHERE document_id = ? AND version_number = ?";
        jdbcTemplate.update(activateSql, documentId, versionNumber);
    }

    public void deleteById(UUID id) {
        String sql = "DELETE FROM document_versions WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    private RowMapper<DocumentVersion> documentVersionRowMapper() {
        return (rs, rowNum) -> new DocumentVersion(
            (UUID) rs.getObject("id"),
            rs.getString("document_id"),
            rs.getString("document_name"),
            rs.getInt("version_number"),
            rs.getObject("chunk_count") == null ? null : rs.getInt("chunk_count"),
            rs.getString("embedding_model"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getBoolean("is_active")
        );
    }
}
