package com.enterprise.ai.knowledge.assistant.evaluation.repository;

import com.enterprise.ai.knowledge.assistant.evaluation.entity.EvaluationTest;
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
public class EvaluationTestRepository {

    private final JdbcTemplate jdbcTemplate;

    public EvaluationTestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS evaluation_tests (" +
                "id UUID PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "query TEXT NOT NULL, " +
                "expected_chunk_ids TEXT[], " +
                "created_at TIMESTAMP" +
                ")";
        jdbcTemplate.execute(sql);
    }

    public EvaluationTest save(EvaluationTest test) {
        UUID id = test.getId() == null ? UUID.randomUUID() : test.getId();
        Instant now = test.getCreatedAt() == null ? Instant.now() : test.getCreatedAt();

        String sql = "INSERT INTO evaluation_tests (id, name, query, expected_chunk_ids, created_at) " +
                     "VALUES (?, ?, ?, ?::text[], ?) " +
                     "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, query = EXCLUDED.query, expected_chunk_ids = EXCLUDED.expected_chunk_ids";

        jdbcTemplate.update(sql, id, test.getName(), test.getQuery(), 
                          test.getExpectedChunkIds().toArray(new String[0]), Timestamp.from(now));

        test.setId(id);
        test.setCreatedAt(now);
        return test;
    }

    public Optional<EvaluationTest> findById(UUID id) {
        String sql = "SELECT id, name, query, expected_chunk_ids, created_at FROM evaluation_tests WHERE id = ?";
        try {
            EvaluationTest test = jdbcTemplate.queryForObject(sql, evaluationTestRowMapper(), id);
            return Optional.ofNullable(test);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<EvaluationTest> findAll() {
        String sql = "SELECT id, name, query, expected_chunk_ids, created_at FROM evaluation_tests ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, evaluationTestRowMapper());
    }

    public void deleteById(UUID id) {
        String sql = "DELETE FROM evaluation_tests WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    private RowMapper<EvaluationTest> evaluationTestRowMapper() {
        return (rs, rowNum) -> new EvaluationTest(
            (UUID) rs.getObject("id"),
            rs.getString("name"),
            rs.getString("query"),
            List.of((String[]) rs.getArray("expected_chunk_ids").getArray()),
            rs.getTimestamp("created_at").toInstant()
        );
    }
}
