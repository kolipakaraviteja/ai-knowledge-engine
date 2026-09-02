package com.enterprise.ai.knowledge.assistant.auth.repository;

import com.enterprise.ai.knowledge.assistant.auth.entity.UserKnowledgeBase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserKnowledgeBaseRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<UserKnowledgeBase> rowMapper;

    public UserKnowledgeBaseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = (rs, rowNum) -> new UserKnowledgeBase(
            UUID.fromString(rs.getString("id")),
            UUID.fromString(rs.getString("user_id")),
            UUID.fromString(rs.getString("knowledge_base_id")),
            rs.getTimestamp("created_at").toInstant()
        );
    }

    public UserKnowledgeBase save(UserKnowledgeBase userKnowledgeBase) {
        if (userKnowledgeBase.getId() == null) {
            userKnowledgeBase.setId(UUID.randomUUID());
            jdbcTemplate.update(
                "INSERT INTO user_knowledge_bases (id, user_id, knowledge_base_id, created_at) VALUES (?, ?, ?, ?)",
                userKnowledgeBase.getId(), userKnowledgeBase.getUserId(), userKnowledgeBase.getKnowledgeBaseId(),
                userKnowledgeBase.getCreatedAt()
            );
        }
        return userKnowledgeBase;
    }

    public List<UserKnowledgeBase> findByUserId(UUID userId) {
        return jdbcTemplate.query(
            "SELECT * FROM user_knowledge_bases WHERE user_id = ?",
            rowMapper,
            userId
        );
    }

    public List<UserKnowledgeBase> findByKnowledgeBaseId(UUID knowledgeBaseId) {
        return jdbcTemplate.query(
            "SELECT * FROM user_knowledge_bases WHERE knowledge_base_id = ?",
            rowMapper,
            knowledgeBaseId
        );
    }

    public Optional<UserKnowledgeBase> findByUserIdAndKnowledgeBaseId(UUID userId, UUID knowledgeBaseId) {
        return jdbcTemplate.query(
            "SELECT * FROM user_knowledge_bases WHERE user_id = ? AND knowledge_base_id = ?",
            rowMapper,
            userId, knowledgeBaseId
        ).stream().findFirst();
    }

    public void deleteByUserIdAndKnowledgeBaseId(UUID userId, UUID knowledgeBaseId) {
        jdbcTemplate.update(
            "DELETE FROM user_knowledge_bases WHERE user_id = ? AND knowledge_base_id = ?",
            userId, knowledgeBaseId
        );
    }

    public void deleteByUserId(UUID userId) {
        jdbcTemplate.update("DELETE FROM user_knowledge_bases WHERE user_id = ?", userId);
    }
}
