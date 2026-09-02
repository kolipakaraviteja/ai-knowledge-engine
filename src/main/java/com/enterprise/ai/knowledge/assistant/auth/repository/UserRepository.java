package com.enterprise.ai.knowledge.assistant.auth.repository;

import com.enterprise.ai.knowledge.assistant.auth.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<User> userRowMapper;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRowMapper = (rs, rowNum) -> new User(
            UUID.fromString(rs.getString("id")),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("username"),
            rs.getString("role"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant(),
            rs.getTimestamp("last_login_at") != null ? rs.getTimestamp("last_login_at").toInstant() : null
        );
    }

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
            jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, username, role, created_at, updated_at, last_login_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                user.getId(), user.getEmail(), user.getPasswordHash(), user.getUsername(), user.getRole(),
                Timestamp.from(user.getCreatedAt()), Timestamp.from(user.getUpdatedAt()),
                user.getLastLoginAt() != null ? Timestamp.from(user.getLastLoginAt()) : null
            );
        } else {
            jdbcTemplate.update(
                "UPDATE users SET email = ?, password_hash = ?, username = ?, role = ?, updated_at = ?, last_login_at = ? WHERE id = ?",
                user.getEmail(), user.getPasswordHash(), user.getUsername(), user.getRole(),
                Timestamp.from(user.getUpdatedAt()),
                user.getLastLoginAt() != null ? Timestamp.from(user.getLastLoginAt()) : null,
                user.getId()
            );
        }
        return user;
    }

    public Optional<User> findById(UUID id) {
        return jdbcTemplate.query(
            "SELECT * FROM users WHERE id = ?",
            userRowMapper,
            id
        ).stream().findFirst();
    }

    public Optional<User> findByEmail(String email) {
        return jdbcTemplate.query(
            "SELECT * FROM users WHERE email = ?",
            userRowMapper,
            email
        ).stream().findFirst();
    }

    public Optional<User> findByUsername(String username) {
        return jdbcTemplate.query(
            "SELECT * FROM users WHERE username = ?",
            userRowMapper,
            username
        ).stream().findFirst();
    }

    public void deleteById(UUID id) {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
    }

    public void updateLastLogin(UUID userId) {
        jdbcTemplate.update("UPDATE users SET last_login_at = CURRENT_TIMESTAMP WHERE id = ?", userId);
    }

    public List<User> findAll() {
        return jdbcTemplate.query("SELECT * FROM users ORDER BY created_at DESC", userRowMapper);
    }
}
