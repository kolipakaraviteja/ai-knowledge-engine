package com.enterprise.ai.knowledge.assistant.auth.dto;

public class CreateUserRequest {
    private String email;
    private String password;
    private String username;
    private String role;

    public CreateUserRequest() {}

    public CreateUserRequest(String email, String password, String username, String role) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.role = role;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
