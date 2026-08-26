package com.enterprise.ai.knowledge.assistant.evaluation.repository;

import com.enterprise.ai.knowledge.assistant.evaluation.entity.EvaluationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@Slf4j
public class EvaluationResultRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public EvaluationResultRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS evaluation_results (" +
                "id UUID PRIMARY KEY, " +
                "test_id UUID NOT NULL, " +
                "run_id UUID NOT NULL, " +
                "retrieved_chunk_ids TEXT[], " +
                "metrics JSONB, " +
                "latency_ms BIGINT, " +
                "created_at TIMESTAMP, " +
                "FOREIGN KEY (test_id) REFERENCES evaluation_tests(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (run_id) REFERENCES evaluation_runs(id) ON DELETE CASCADE" +
                ")";
        jdbcTemplate.execute(sql);
    }

    public EvaluationResult save(EvaluationResult result) {
        UUID id = result.getId() == null ? UUID.randomUUID() : result.getId();
        Instant now = result.getCreatedAt() == null ? Instant.now() : result.getCreatedAt();

        String sql = "INSERT INTO evaluation_results (id, test_id, run_id, retrieved_chunk_ids, metrics, latency_ms, created_at) " +
                     "VALUES (?, ?, ?, ?::text[], ?::jsonb, ?, ?)";

        try {
            String metricsJson = objectMapper.writeValueAsString(result.getMetrics());
            jdbcTemplate.update(sql, id, result.getTestId(), result.getRunId(),
                              result.getRetrievedChunkIds().toArray(new String[0]),
                              metricsJson, result.getLatencyMs(), Timestamp.from(now));
        } catch (Exception e) {
            log.error("Failed to serialize metrics to JSON", e);
            throw new RuntimeException("Failed to save evaluation result", e);
        }

        result.setId(id);
        result.setCreatedAt(now);
        return result;
    }

    public Optional<EvaluationResult> findById(UUID id) {
        String sql = "SELECT id, test_id, run_id, retrieved_chunk_ids, metrics, latency_ms, created_at FROM evaluation_results WHERE id = ?";
        try {
            EvaluationResult result = jdbcTemplate.queryForObject(sql, evaluationResultRowMapper(), id);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<EvaluationResult> findByRunId(UUID runId) {
        String sql = "SELECT id, test_id, run_id, retrieved_chunk_ids, metrics, latency_ms, created_at " +
                     "FROM evaluation_results WHERE run_id = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, evaluationResultRowMapper(), runId);
    }

    public List<EvaluationResult> findByTestId(UUID testId) {
        String sql = "SELECT id, test_id, run_id, retrieved_chunk_ids, metrics, latency_ms, created_at " +
                     "FROM evaluation_results WHERE test_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, evaluationResultRowMapper(), testId);
    }

    public void deleteById(UUID id) {
        String sql = "DELETE FROM evaluation_results WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    private RowMapper<EvaluationResult> evaluationResultRowMapper() {
        return (rs, rowNum) -> {
            try {
                String metricsJson = rs.getString("metrics");
                Map<String, Object> metrics = metricsJson != null ? 
                    objectMapper.readValue(metricsJson, Map.class) : Map.of();
                
                return new EvaluationResult(
                    (UUID) rs.getObject("id"),
                    (UUID) rs.getObject("test_id"),
                    (UUID) rs.getObject("run_id"),
                    rs.getArray("retrieved_chunk_ids") != null ? 
                        List.of((String[]) rs.getArray("retrieved_chunk_ids").getArray()) : List.of(),
                    metrics,
                    rs.getLong("latency_ms"),
                    rs.getTimestamp("created_at").toInstant()
                );
            } catch (Exception e) {
                log.error("Failed to deserialize metrics from JSON", e);
                throw new RuntimeException("Failed to map evaluation result", e);
            }
        };
    }
}
