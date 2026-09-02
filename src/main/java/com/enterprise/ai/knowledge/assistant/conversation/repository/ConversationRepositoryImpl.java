package com.enterprise.ai.knowledge.assistant.conversation.repository;

import com.enterprise.ai.knowledge.assistant.chat.dto.ChatResponse;
import com.enterprise.ai.knowledge.assistant.conversation.entity.Conversation;
import com.enterprise.ai.knowledge.assistant.conversation.entity.ConversationMessage;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service
public class ConversationRepositoryImpl implements ConversationRepository {

    private final JdbcTemplate jdbcTemplate;

    public ConversationRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureTable() {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        } catch (Exception ignored) {}

        String sql = "CREATE TABLE IF NOT EXISTS conversations (" +
                "id UUID PRIMARY KEY, " +
                "title TEXT, " +
                "owner_id UUID, " +
                "created_at TIMESTAMP, " +
                "updated_at TIMESTAMP, " +
                "metadata JSONB" +
                ")";

        jdbcTemplate.execute(sql);
    }

    @PostConstruct
    public void ensureConversationMessagesTable(){
        String sql = "CREATE TABLE IF NOT EXISTS conversation_messages (" +
                "id UUID PRIMARY KEY, " +
                "conversation_id UUID, " +
                "message_order INT, " +
                "role TEXT, " +
                "message TEXT, " +
                "created_at TIMESTAMP, " +
                "metadata JSONB" +
                ")";

        jdbcTemplate.execute(sql);

    }
    @Override
    public UUID createConversation(String title, UUID ownerId) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String sql = "INSERT INTO conversations (id, title, owner_id, created_at, updated_at, metadata) VALUES (?, ?, ?, ?, ?, ?::jsonb)";
        jdbcTemplate.update(sql, id, title, ownerId, Timestamp.from(now), Timestamp.from(now), "{}");
        return id;
    }

    @Override
    public Optional<Conversation> getConversation(UUID conversationId) {
        String sql = "SELECT id, title, owner_id, created_at, updated_at, metadata FROM conversations WHERE id = ?";
        try {
            Conversation conv = jdbcTemplate.queryForObject(sql, conversationRowMapper(), conversationId);
            return Optional.ofNullable(conv);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<ConversationMessage> getRecentMessages(UUID conversationId, int limit) {
        String sql = "SELECT id, conversation_id, message_order, role, message, created_at, metadata " +
                     "FROM conversation_messages WHERE conversation_id = ? ORDER BY message_order DESC LIMIT ?";
        return jdbcTemplate.query(sql, messageRowMapper(), conversationId, limit);
    }

    @Override
    public void saveMessage(ConversationMessage message) {
        UUID id = message.getId() == null ? UUID.randomUUID() : message.getId();
        Instant now = message.getCreatedAt() == null ? Instant.now() : message.getCreatedAt();

        String sql = "INSERT INTO conversation_messages (id, conversation_id, message_order, role, message, created_at, metadata) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)";

        jdbcTemplate.update(sql,
            id,
            message.getConversationId(),
            message.getMessageOrder(),
            message.getRole(),
            message.getMessage(),
            Timestamp.from(now),
            "{}"
        );

        message.setId(id);
        message.setCreatedAt(now);
    }

    @Override
    public int getMessageCount(UUID conversationId) {
        String sql = "SELECT COUNT(*) FROM conversation_messages WHERE conversation_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, conversationId);
        return count == null ? 0 : count;
    }

    @Override
    public void updateConversationTitle(UUID conversationId, String title) {
        String sql = "UPDATE conversations SET title = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, title, Timestamp.from(Instant.now()), conversationId);
    }

    @Override
    public List<ChatResponse> getConversationHistory(UUID conversationId) {
        String sql = "SELECT id, conversation_id, message_order, role, message, created_at FROM conversation_messages " +
                     "WHERE conversation_id = ? ORDER BY message_order ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
            ChatResponse.builder()
                .answer(rs.getString("message"))
                .isFromContext(false)
                .retrievalCount(0)
                .sourceDocuments(List.of())
                .build(), conversationId);
    }

    @Override
    public List<Map<String, Object>> getAllConversations(UUID userId) {
        String sql = "SELECT id, title, created_at, " +
                     "(SELECT MAX(created_at) FROM conversation_messages WHERE conversation_id = c.id) as last_activity, " +
                     "(SELECT COUNT(*) FROM conversation_messages WHERE conversation_id = c.id) as message_count " +
                     "FROM conversations c WHERE owner_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", rs.getObject("id"));
            map.put("title", rs.getString("title"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                map.put("createdAt", createdAt.toInstant());
            }
            Timestamp lastActivity = rs.getTimestamp("last_activity");
            if (lastActivity != null) {
                map.put("lastActivity", lastActivity.toInstant());
            }
            map.put("messageCount", rs.getInt("message_count"));
            return map;
        }, userId);
    }

    @Override
    public void deleteConversation(UUID conversationId) {
        String deleteMsgSql = "DELETE FROM conversation_messages WHERE conversation_id = ?";
        jdbcTemplate.update(deleteMsgSql, conversationId);

        String deleteConvSql = "DELETE FROM conversations WHERE id = ?";
        jdbcTemplate.update(deleteConvSql, conversationId);
    }

    @Override
    public boolean isOwner(UUID conversationId, UUID userId) {
        String sql = "SELECT COUNT(*) FROM conversations WHERE id = ? AND owner_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, conversationId, userId);
        return count != null && count > 0;
    }

    @Override
    public Map<String, Object> getCitationDetails(String chunkHash) {
        String sql = "SELECT id, content, document_name, page_number, chunk_index FROM embeddings WHERE id = ?";
        try {
            return jdbcTemplate.queryForMap(sql, chunkHash);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    @Override
    public List<Map<String, Object>> searchConversations(String query) {
        String sql = "SELECT c.id, c.title, c.created_at, " +
                     "(SELECT MAX(created_at) FROM conversation_messages WHERE conversation_id = c.id) as last_activity, " +
                     "(SELECT COUNT(*) FROM conversation_messages WHERE conversation_id = c.id) as message_count " +
                     "FROM conversations c " +
                     "WHERE c.title ILIKE ? OR EXISTS (" +
                     "  SELECT 1 FROM conversation_messages cm " +
                     "  WHERE cm.conversation_id = c.id AND cm.message ILIKE ?" +
                     ") ORDER BY created_at DESC";
        
        String searchPattern = "%" + query + "%";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", rs.getObject("id"));
            map.put("title", rs.getString("title"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                map.put("createdAt", createdAt.toInstant());
            }
            Timestamp lastActivity = rs.getTimestamp("last_activity");
            if (lastActivity != null) {
                map.put("lastActivity", lastActivity.toInstant());
            }
            map.put("messageCount", rs.getInt("message_count"));
            return map;
        }, searchPattern, searchPattern);
    }

    // ...existing code...

    private RowMapper<Conversation> conversationRowMapper() {
        return (rs, rowNum) -> new Conversation(
            (UUID) rs.getObject("id"),
            rs.getString("title"),
            (UUID) rs.getObject("owner_id"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant(),
            null
        );
    }

    private RowMapper<ConversationMessage> messageRowMapper() {
        return (rs, rowNum) -> new ConversationMessage(
            (UUID) rs.getObject("id"),
            (UUID) rs.getObject("conversation_id"),
            rs.getInt("message_order"),
            rs.getString("role"),
            rs.getString("message"),
            rs.getTimestamp("created_at").toInstant(),
            null
        );
    }
}

