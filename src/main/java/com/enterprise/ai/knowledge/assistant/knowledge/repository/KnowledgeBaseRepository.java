package com.enterprise.ai.knowledge.assistant.knowledge.repository;

import com.enterprise.ai.knowledge.assistant.knowledge.entity.KnowledgeBase;
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
public class KnowledgeBaseRepository {

    private final JdbcTemplate jdbcTemplate;

    public KnowledgeBaseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS knowledge_bases (" +
                "id UUID PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "description TEXT, " +
                "created_at TIMESTAMP, " +
                "updated_at TIMESTAMP" +
                ")";
        jdbcTemplate.execute(sql);
    }

    public KnowledgeBase save(KnowledgeBase knowledgeBase) {
        UUID id = knowledgeBase.getId() == null ? UUID.randomUUID() : knowledgeBase.getId();
        Instant now = knowledgeBase.getCreatedAt() == null ? Instant.now() : knowledgeBase.getCreatedAt();
        Instant updatedAt = Instant.now();

        String sql = "INSERT INTO knowledge_bases (id, name, description, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?) " +
                     "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, updated_at = EXCLUDED.updated_at";

        jdbcTemplate.update(sql, id, knowledgeBase.getName(), knowledgeBase.getDescription(), 
                          Timestamp.from(now), Timestamp.from(updatedAt));

        knowledgeBase.setId(id);
        knowledgeBase.setCreatedAt(now);
        knowledgeBase.setUpdatedAt(updatedAt);
        return knowledgeBase;
    }

    public Optional<KnowledgeBase> findById(UUID id) {
        String sql = "SELECT id, name, description, created_at, updated_at FROM knowledge_bases WHERE id = ?";
        try {
            KnowledgeBase kb = jdbcTemplate.queryForObject(sql, knowledgeBaseRowMapper(), id);
            return Optional.ofNullable(kb);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<KnowledgeBase> findAll() {
        String sql = "SELECT id, name, description, created_at, updated_at FROM knowledge_bases ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, knowledgeBaseRowMapper());
    }

    public void deleteById(UUID id) {
        String sql = "DELETE FROM knowledge_bases WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    private RowMapper<KnowledgeBase> knowledgeBaseRowMapper() {
        return (rs, rowNum) -> new KnowledgeBase(
            (UUID) rs.getObject("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
        );
    }
}
