package com.enterprise.ai.knowledge.assistant.repository;

import com.enterprise.ai.knowledge.assistant.config.CorrelationIdUtil;
import com.enterprise.ai.knowledge.assistant.document.dto.DocumentMetadata;
import com.enterprise.ai.knowledge.assistant.vector.entity.ChunkEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PostgresVectorRepository implements VectorRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresVectorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureTable() {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        } catch (Exception e) {
            String correlationId = CorrelationIdUtil.getCorrelationId();
            log.warn("[{}] Failed to create vector extension", correlationId, e);
        }

        String sql = "CREATE TABLE IF NOT EXISTS embeddings (" +
                "id UUID PRIMARY KEY, " +
                "document_name TEXT, " +
                "document_id VARCHAR(255), " +
                "document_hash VARCHAR(64), " +
                "chunk_hash VARCHAR(64), " +
                "page_number INT, " +
                "chunk_index INT, " +
                "content TEXT, " +
                "embedding vector, " +
                "embedding_model VARCHAR(100), " +
                "embedding_dimension INT, " +
                "language VARCHAR(10), " +
                "version INT DEFAULT 1, " +
                "hash TEXT, " +
                "created_at TIMESTAMP, " +
                "updated_at TIMESTAMP, " +
                "collection_id UUID, " +
                "knowledge_base_id UUID" +
                ")";
        jdbcTemplate.execute(sql);
    }

    @Override
    public void insertChunk(ChunkEntity chunk) {
        UUID id = chunk.getId() == null ? UUID.randomUUID() : chunk.getId();
        Timestamp now = chunk.getCreatedAt() == null ? Timestamp.from(Instant.now()) : Timestamp.from(chunk.getCreatedAt());
        Timestamp updatedAt = chunk.getUpdatedAt() == null ? now : Timestamp.from(chunk.getUpdatedAt());

        String sql = "INSERT INTO embeddings (id, document_name, document_id, document_hash, chunk_hash, page_number, chunk_index, content, embedding, embedding_model, embedding_dimension, language, version, hash, created_at, updated_at, collection_id, knowledge_base_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::vector, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatementCreator psc = con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setObject(1, id);
            ps.setString(2, chunk.getDocumentName());
            ps.setString(3, chunk.getDocumentId());
            ps.setString(4, chunk.getDocumentHash());
            ps.setString(5, chunk.getChunkHash());
            if (chunk.getPageNumber() == null) ps.setNull(6, java.sql.Types.INTEGER); else ps.setInt(6, chunk.getPageNumber());
            if (chunk.getChunkIndex() == null) ps.setNull(7, java.sql.Types.INTEGER); else ps.setInt(7, chunk.getChunkIndex());
            ps.setString(8, chunk.getContent());
            ps.setString(9, toPgVectorString(chunk.getEmbedding()));
            ps.setString(10, chunk.getEmbeddingModel());
            if (chunk.getEmbeddingDimension() == null) ps.setNull(11, java.sql.Types.INTEGER); else ps.setInt(11, chunk.getEmbeddingDimension());
            ps.setString(12, chunk.getLanguage());
            if (chunk.getVersion() == null) ps.setNull(13, java.sql.Types.INTEGER); else ps.setInt(13, chunk.getVersion());
            ps.setString(14, chunk.getHash());
            ps.setTimestamp(15, now);
            ps.setTimestamp(16, updatedAt);
            ps.setObject(17, chunk.getCollectionId());
            ps.setObject(18, chunk.getKnowledgeBaseId());
            return ps;
        };

        jdbcTemplate.update(psc);
    }

    @Override
    public List<SearchResult> findNearest(float[] query, int k) {
        return findNearest(query, k, null);
    }

    public List<SearchResult> findNearest(float[] query, int k, UUID collectionId) {
        return findNearest(query, k, null, collectionId);
    }

    public List<SearchResult> findNearest(float[] query, int k, UUID knowledgeBaseId, UUID collectionId) {
        String sql;
        if (knowledgeBaseId != null && collectionId != null) {
            sql = "SELECT content, page_number, document_name, chunk_index, document_id, document_hash, chunk_hash, embedding_model, embedding_dimension, language, version, updated_at, embedding <-> (?::vector) AS distance FROM embeddings WHERE knowledge_base_id = ? AND collection_id = ? ORDER BY distance ASC LIMIT ?";
        } else if (knowledgeBaseId != null) {
            sql = "SELECT content, page_number, document_name, chunk_index, document_id, document_hash, chunk_hash, embedding_model, embedding_dimension, language, version, updated_at, embedding <-> (?::vector) AS distance FROM embeddings WHERE knowledge_base_id = ? ORDER BY distance ASC LIMIT ?";
        } else if (collectionId != null) {
            sql = "SELECT content, page_number, document_name, chunk_index, document_id, document_hash, chunk_hash, embedding_model, embedding_dimension, language, version, updated_at, embedding <-> (?::vector) AS distance FROM embeddings WHERE collection_id = ? ORDER BY distance ASC LIMIT ?";
        } else {
            sql = "SELECT content, page_number, document_name, chunk_index, document_id, document_hash, chunk_hash, embedding_model, embedding_dimension, language, version, updated_at, embedding <-> (?::vector) AS distance FROM embeddings ORDER BY distance ASC LIMIT ?";
        }

        PreparedStatementCreator psc = con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, toPgVectorString(query));
            int paramIndex = 2;
            if (knowledgeBaseId != null && collectionId != null) {
                ps.setObject(paramIndex++, knowledgeBaseId);
                ps.setObject(paramIndex++, collectionId);
                ps.setInt(paramIndex, k);
            } else if (knowledgeBaseId != null) {
                ps.setObject(paramIndex++, knowledgeBaseId);
                ps.setInt(paramIndex, k);
            } else if (collectionId != null) {
                ps.setObject(paramIndex++, collectionId);
                ps.setInt(paramIndex, k);
            } else {
                ps.setInt(paramIndex, k);
            }
            return ps;
        };

        RowMapper<SearchResult> mapper = (rs, rowNum) -> {
            String content = rs.getString("content");
            Integer pageNumber = rs.getObject("page_number") == null ? null : rs.getInt("page_number");
            String documentName = rs.getString("document_name");
            Integer chunkIndex = rs.getObject("chunk_index") == null ? null : rs.getInt("chunk_index");
            double distance = rs.getDouble("distance");
            String documentId = rs.getString("document_id");
            String documentHash = rs.getString("document_hash");
            String chunkHash = rs.getString("chunk_hash");
            String embeddingModel = rs.getString("embedding_model");
            Integer embeddingDimension = rs.getObject("embedding_dimension") == null ? null : rs.getInt("embedding_dimension");
            String language = rs.getString("language");
            Integer version = rs.getObject("version") == null ? null : rs.getInt("version");
            Timestamp updatedAtTs = rs.getTimestamp("updated_at");
            Instant updatedAt = updatedAtTs == null ? null : updatedAtTs.toInstant();

            return new SearchResult(content, distance, pageNumber, documentName, chunkIndex, documentId, documentHash, chunkHash, embeddingModel, embeddingDimension, language, version, updatedAt);
        };

        try {
            return jdbcTemplate.query(psc, mapper);
        } catch (Exception e) {
            String correlationId = CorrelationIdUtil.getCorrelationId();
            log.error("[{}] Error performing vector similarity search", correlationId, e);
            return new ArrayList<>();
        }
    }

    private String toPgVectorString(float[] embedding) {
        if (embedding == null) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(Float.toString(embedding[i]));
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public boolean existsByHash(String hash) {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM embeddings WHERE hash = ?", Integer.class, hash);
            return count != null && count > 0;
        } catch (Exception e) {
            String correlationId = CorrelationIdUtil.getCorrelationId();
            log.debug("[{}] Error checking chunk existence by hash: {}", correlationId, hash, e);
            return false;
        }
    }

    @Override
    public void deleteByDocumentId(String documentId) {
        String sql = "DELETE FROM embeddings WHERE document_id = ?";
        jdbcTemplate.update(sql, documentId);
    }

    @Override
    public List<DocumentMetadata> listDocuments() {
        String sql = "SELECT document_id, document_name, document_hash, chunk_count, file_size, pages, characters, uploaded_at, indexed_at " +
                     "FROM document_metadata ORDER BY uploaded_at DESC";

        RowMapper<DocumentMetadata> mapper = (rs, rowNum) -> {
            String documentId = rs.getString("document_id");
            String documentName = rs.getString("document_name");
            String documentHash = rs.getString("document_hash");
            int chunkCount = rs.getInt("chunk_count");
            long fileSize = rs.getLong("file_size");
            int pages = rs.getInt("pages");
            int characters = rs.getInt("characters");
            Timestamp uploadedAtTs = rs.getTimestamp("uploaded_at");
            Instant uploadedAt = uploadedAtTs == null ? null : uploadedAtTs.toInstant();
            Timestamp indexedAtTs = rs.getTimestamp("indexed_at");
            Instant indexedAt = indexedAtTs == null ? null : indexedAtTs.toInstant();

            return new DocumentMetadata(
                    documentId,
                    documentName,
                    documentHash,
                    chunkCount,
                    fileSize,
                    pages,
                    characters,
                    uploadedAt,
                    indexedAt,
                    null, // knowledgeBaseId
                    null // collectionId
            );
        };

        try {
            return jdbcTemplate.query(sql, mapper);
        } catch (Exception e) {
            String correlationId = CorrelationIdUtil.getCorrelationId();
            log.error("[{}] Error listing documents from document_metadata table", correlationId, e);
            return new ArrayList<>();
        }
    }
}

