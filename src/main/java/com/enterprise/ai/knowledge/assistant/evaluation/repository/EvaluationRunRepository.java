package com.enterprise.ai.knowledge.assistant.evaluation.repository;

import com.enterprise.ai.knowledge.assistant.evaluation.entity.EvaluationRun;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EvaluationRunRepository {

    private final JdbcTemplate jdbcTemplate;

    public EvaluationRunRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS evaluation_runs (" +
                "id UUID PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "description TEXT, " +
                "started_at TIMESTAMP, " +
                "completed_at TIMESTAMP, " +
                "status TEXT" +
                ")";
        jdbcTemplate.execute(sql);
    }

    public EvaluationRun save(EvaluationRun run) {
        UUID id = run.getId() == null ? UUID.randomUUID() : run.getId();

        String sql = "INSERT INTO evaluation_runs (id, name, description, started_at, completed_at, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, completed_at = EXCLUDED.completed_at, status = EXCLUDED.status";

        jdbcTemplate.update(sql, id, run.getName(), run.getDescription(),
                          run.getStartedAt() != null ? Timestamp.from(run.getStartedAt()) : null,
                          run.getCompletedAt() != null ? Timestamp.from(run.getCompletedAt()) : null,
                          run.getStatus());

        run.setId(id);
        return run;
    }

    public Optional<EvaluationRun> findById(UUID id) {
        String sql = "SELECT id, name, description, started_at, completed_at, status FROM evaluation_runs WHERE id = ?";
        try {
            EvaluationRun run = jdbcTemplate.queryForObject(sql, evaluationRunRowMapper(), id);
            return Optional.ofNullable(run);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<EvaluationRun> findAll() {
        String sql = "SELECT id, name, description, started_at, completed_at, status FROM evaluation_runs ORDER BY started_at DESC";
        return jdbcTemplate.query(sql, evaluationRunRowMapper());
    }

    public void deleteById(UUID id) {
        String sql = "DELETE FROM evaluation_runs WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    private RowMapper<EvaluationRun> evaluationRunRowMapper() {
        return (rs, rowNum) -> new EvaluationRun(
            (UUID) rs.getObject("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getTimestamp("started_at") != null ? rs.getTimestamp("started_at").toInstant() : null,
            rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toInstant() : null,
            rs.getString("status")
        );
    }
}
