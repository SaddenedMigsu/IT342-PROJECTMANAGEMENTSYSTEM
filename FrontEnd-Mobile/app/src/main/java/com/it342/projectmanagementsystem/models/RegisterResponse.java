package com.it342.projectmanagementsystem.models;

public class RegisterResponse {
    private String message;
    private String token;
    private User user;

    // Default constructor
    public RegisterResponse() {
    }

    // Parameterized constructor
    public RegisterResponse(String message, String token, User user) {
        this.message = message;
        this.token = token;
        this.user = user;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
} 