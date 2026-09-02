package com.enterprise.ai.knowledge.assistant.auth.repository;

import com.enterprise.ai.knowledge.assistant.auth.entity.UserConversation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserConversationRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<UserConversation> rowMapper;

    public UserConversationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = (rs, rowNum) -> new UserConversation(
            UUID.fromString(rs.getString("id")),
            UUID.fromString(rs.getString("user_id")),
            UUID.fromString(rs.getString("conversation_id")),
            rs.getTimestamp("created_at").toInstant()
        );
    }

    public UserConversation save(UserConversation userConversation) {
        if (userConversation.getId() == null) {
            userConversation.setId(UUID.randomUUID());
            jdbcTemplate.update(
                "INSERT INTO user_conversations (id, user_id, conversation_id, created_at) VALUES (?, ?, ?, ?)",
                userConversation.getId(), userConversation.getUserId(), userConversation.getConversationId(),
                userConversation.getCreatedAt()
            );
        }
        return userConversation;
    }

    public List<UserConversation> findByUserId(UUID userId) {
        return jdbcTemplate.query(
            "SELECT * FROM user_conversations WHERE user_id = ?",
            rowMapper,
            userId
        );
    }

    public List<UserConversation> findByConversationId(UUID conversationId) {
        return jdbcTemplate.query(
            "SELECT * FROM user_conversations WHERE conversation_id = ?",
            rowMapper,
            conversationId
        );
    }

    public Optional<UserConversation> findByUserIdAndConversationId(UUID userId, UUID conversationId) {
        return jdbcTemplate.query(
            "SELECT * FROM user_conversations WHERE user_id = ? AND conversation_id = ?",
            rowMapper,
            userId, conversationId
        ).stream().findFirst();
    }

    public void deleteByUserIdAndConversationId(UUID userId, UUID conversationId) {
        jdbcTemplate.update(
            "DELETE FROM user_conversations WHERE user_id = ? AND conversation_id = ?",
            userId, conversationId
        );
    }

    public void deleteByUserId(UUID userId) {
        jdbcTemplate.update("DELETE FROM user_conversations WHERE user_id = ?", userId);
    }
}
