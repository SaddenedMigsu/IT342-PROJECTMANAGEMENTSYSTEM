package com.it342.projectmanagementsystem.dto;

public class RegisterResponse {
    private String userId;
    private String email;
    private String role;

    public RegisterResponse() {
    }

    public RegisterResponse(String userId, String email, String role) {
        this.userId = userId;
        this.email = email;
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
} 