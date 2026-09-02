package com.enterprise.ai.knowledge.assistant.knowledge.repository;

import com.enterprise.ai.knowledge.assistant.knowledge.entity.Collection;
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
public class CollectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public CollectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureTable() {

        String knowledge_bases_sql = "CREATE TABLE IF NOT EXISTS knowledge_bases (" +
                "id UUID PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "description TEXT, " +
                "created_at TIMESTAMP, " +
                "updated_at TIMESTAMP" +
                ")";
        jdbcTemplate.execute(knowledge_bases_sql);

        String collections_sql = "CREATE TABLE IF NOT EXISTS collections (" +
                "id UUID PRIMARY KEY, " +
                "knowledge_base_id UUID NOT NULL, " +
                "name TEXT NOT NULL, " +
                "description TEXT, " +
                "owner_id UUID, " +
                "created_at TIMESTAMP, " +
                "updated_at TIMESTAMP, " +
                "FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases(id) ON DELETE CASCADE" +
                ")";
        jdbcTemplate.execute(collections_sql);
    }

    public Collection save(Collection collection) {
        UUID id = collection.getId() == null ? UUID.randomUUID() : collection.getId();
        Instant now = collection.getCreatedAt() == null ? Instant.now() : collection.getCreatedAt();
        Instant updatedAt = Instant.now();

        String sql = "INSERT INTO collections (id, knowledge_base_id, name, description, owner_id, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, owner_id = EXCLUDED.owner_id, updated_at = EXCLUDED.updated_at";

        jdbcTemplate.update(sql, id, collection.getKnowledgeBaseId(), collection.getName(),
                          collection.getDescription(), collection.getOwnerId(), Timestamp.from(now), Timestamp.from(updatedAt));

        collection.setId(id);
        collection.setCreatedAt(now);
        collection.setUpdatedAt(updatedAt);
        return collection;
    }

    public Optional<Collection> findById(UUID id) {
        String sql = "SELECT id, knowledge_base_id, name, description, owner_id, created_at, updated_at FROM collections WHERE id = ?";
        try {
            Collection coll = jdbcTemplate.queryForObject(sql, collectionRowMapper(), id);
            return Optional.ofNullable(coll);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<Collection> findByKnowledgeBaseId(UUID knowledgeBaseId) {
        String sql = "SELECT id, knowledge_base_id, name, description, owner_id, created_at, updated_at " +
                     "FROM collections WHERE knowledge_base_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, collectionRowMapper(), knowledgeBaseId);
    }

    public List<Collection> findAll() {
        String sql = "SELECT id, knowledge_base_id, name, description, owner_id, created_at, updated_at FROM collections ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, collectionRowMapper());
    }

    public void deleteById(UUID id) {
        String sql = "DELETE FROM collections WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    private RowMapper<Collection> collectionRowMapper() {
        return (rs, rowNum) -> new Collection(
            (UUID) rs.getObject("id"),
            (UUID) rs.getObject("knowledge_base_id"),
            rs.getString("name"),
            rs.getString("description"),
            (UUID) rs.getObject("owner_id"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
        );
    }
}
