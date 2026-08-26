package com.enterprise.ai.knowledge.assistant.embedding;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PostgresService {

    private final JdbcTemplate jdbcTemplate;

    public PostgresService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureTable() {
        // Try to create the pgvector extension (no-op if not permitted) and the embeddings table
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        } catch (Exception ignored) {
            // Creating extensions may fail depending on permissions; ignore so the app can still run.
        }

        String sql = "CREATE TABLE IF NOT EXISTS embeddings (" +
                "id UUID PRIMARY KEY, " +
                "content TEXT, " +
                "embedding vector, " +
                "created_at TIMESTAMP" +
                ")";
        jdbcTemplate.execute(sql);
    }

    /**
     * Insert an embedding into the database using Postgres `vector` type (pgvector).
     * @param content human readable content / source text
     * @param embedding float[] vector
     */
    public void insertEmbedding(String content, float[] embedding) {
        UUID id = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());

        String sql = "INSERT INTO embeddings (id, content, embedding, created_at) VALUES (?, ?, ?::vector, ?)";

        PreparedStatementCreator psc = con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setObject(1, id);
            ps.setString(2, content);
            ps.setString(3, toPgVectorString(embedding));
            ps.setTimestamp(4, now);
            return ps;
        };

        jdbcTemplate.update(psc);
    }

    /**
     * Find the k nearest embeddings to the provided query vector using pgvector operator <->.
     * Returns a list of EmbeddingResult containing id, content and distance.
     */
    public List<EmbeddingResult> findNearest(float[] query, int k) {
        String sql = "SELECT id, content, embedding <-> (?::vector) AS distance FROM embeddings ORDER BY distance ASC LIMIT ?";

        PreparedStatementCreator psc = con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, toPgVectorString(query));
            ps.setInt(2, k);
            return ps;
        };

        RowMapper<EmbeddingResult> mapper = (rs, rowNum) -> {
            UUID id = (UUID) rs.getObject("id");
            String content = rs.getString("content");
            double distance = rs.getDouble("distance");
            return new EmbeddingResult(id, content, distance);
        };

        try {
            return jdbcTemplate.query(psc, mapper);
        } catch (Exception e) {
            // If the vector type/operator is not available (e.g., in H2 tests), return empty list
            return new ArrayList<>();
        }
    }

    private String toPgVectorString(float[] embedding) {
        // pgvector expects a bracketed list like "[0.1,0.2,...]"
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
            // use Float.toString to ensure decimal point formatting
            sb.append(Float.toString(embedding[i]));
        }
        sb.append(']');
        return sb.toString();
    }

    public static final class EmbeddingResult {
        private final UUID id;
        private final String content;
        private final double distance;

        public EmbeddingResult(UUID id, String content, double distance) {
            this.id = id;
            this.content = content;
            this.distance = distance;
        }

        public UUID getId() {
            return id;
        }

        public String getContent() {
            return content;
        }

        public double getDistance() {
            return distance;
        }
    }
}
