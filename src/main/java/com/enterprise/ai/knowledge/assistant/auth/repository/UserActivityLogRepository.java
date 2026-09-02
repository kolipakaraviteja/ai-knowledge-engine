package com.enterprise.ai.knowledge.assistant.auth.repository;

import com.enterprise.ai.knowledge.assistant.auth.entity.UserActivityLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class UserActivityLogRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<UserActivityLog> rowMapper;

    public UserActivityLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = (rs, rowNum) -> new UserActivityLog(
            UUID.fromString(rs.getString("id")),
            UUID.fromString(rs.getString("user_id")),
            rs.getString("action"),
            rs.getString("resource_type"),
            rs.getString("resource_id") != null ? UUID.fromString(rs.getString("resource_id")) : null,
            null, // details as JSONB - would need additional mapping
            rs.getTimestamp("created_at").toInstant()
        );
    }

    public UserActivityLog save(UserActivityLog activityLog) {
        if (activityLog.getId() == null) {
            activityLog.setId(UUID.randomUUID());
            jdbcTemplate.update(
                "INSERT INTO user_activity_log (id, user_id, action, resource_type, resource_id, details, created_at) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)",
                activityLog.getId(), activityLog.getUserId(), activityLog.getAction(),
                activityLog.getResourceType(), activityLog.getResourceId(),
                activityLog.getDetails() != null ? activityLog.getDetails().toString() : "{}",
                activityLog.getCreatedAt()
            );
        }
        return activityLog;
    }

    public List<UserActivityLog> findByUserId(UUID userId) {
        return jdbcTemplate.query(
            "SELECT * FROM user_activity_log WHERE user_id = ? ORDER BY created_at DESC",
            rowMapper,
            userId
        );
    }

    public List<UserActivityLog> findByUserIdAndAction(UUID userId, String action) {
        return jdbcTemplate.query(
            "SELECT * FROM user_activity_log WHERE user_id = ? AND action = ? ORDER BY created_at DESC",
            rowMapper,
            userId, action
        );
    }

    public List<UserActivityLog> findByUserIdAndResourceType(UUID userId, String resourceType) {
        return jdbcTemplate.query(
            "SELECT * FROM user_activity_log WHERE user_id = ? AND resource_type = ? ORDER BY created_at DESC",
            rowMapper,
            userId, resourceType
        );
    }

    public void deleteById(UUID id) {
        jdbcTemplate.update("DELETE FROM user_activity_log WHERE id = ?", id);
    }
}
