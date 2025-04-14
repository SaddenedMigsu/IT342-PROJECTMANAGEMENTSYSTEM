package com.it342.projectmanagementsystem.models;

public class AuthResponse {
    private String message;
    private String token;
    private String userId;
    private String studId;
    private String email;

    // Default constructor
    public AuthResponse() {
    }

    // Parameterized constructor
    public AuthResponse(String message, String token, String userId, String studId, String email) {
        this.message = message;
        this.token = token;
        this.userId = userId;
        this.studId = studId;
        this.email = email;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStudId() {
        return studId;
    }

    public void setStudId(String studId) {
        this.studId = studId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
} 