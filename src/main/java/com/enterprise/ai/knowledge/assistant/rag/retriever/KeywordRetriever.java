package com.enterprise.ai.knowledge.assistant.rag.retriever;

import com.enterprise.ai.knowledge.assistant.repository.SearchResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Component
public class KeywordRetriever {

    private final JdbcTemplate jdbcTemplate;

    public KeywordRetriever(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SearchResult> retrieve(String query, int topK) {
        try {
            String sql = """
                SELECT content, page_number, document_name, chunk_index, 
                       document_id, document_hash, chunk_hash, embedding_model,
                       embedding_dimension, language, version, updated_at,
                       ts_rank(search_vector, plainto_tsquery('english', ?)) as score
                FROM embeddings
                WHERE search_vector @@ plainto_tsquery('english', ?)
                ORDER BY score DESC
                LIMIT ?
                """;

            return jdbcTemplate.query(sql, keywordRowMapper(), query, query, topK);
        } catch (Exception e) {
            return List.of();
        }
    }

    private RowMapper<SearchResult> keywordRowMapper() {
        return (rs, rowNum) -> {
            String content = rs.getString("content");
            Integer pageNumber = rs.getObject("page_number") == null ? null : rs.getInt("page_number");
            String documentName = rs.getString("document_name");
            Integer chunkIndex = rs.getObject("chunk_index") == null ? null : rs.getInt("chunk_index");
            double score = rs.getDouble("score");
            String documentId = rs.getString("document_id");
            String documentHash = rs.getString("document_hash");
            String chunkHash = rs.getString("chunk_hash");
            String embeddingModel = rs.getString("embedding_model");
            Integer embeddingDimension = rs.getObject("embedding_dimension") == null ? null : rs.getInt("embedding_dimension");
            String language = rs.getString("language");
            Integer version = rs.getObject("version") == null ? null : rs.getInt("version");
            Timestamp updatedAtTs = rs.getTimestamp("updated_at");
            Instant updatedAt = updatedAtTs == null ? null : updatedAtTs.toInstant();

            return new SearchResult(content, score, pageNumber, documentName, chunkIndex,
                    documentId, documentHash, chunkHash, embeddingModel, embeddingDimension,
                    language, version, updatedAt);
        };
    }
}

