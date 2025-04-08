package com.it342.projectmanagementsystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginRequest {
    @JsonProperty("identifier")
    private String identifier; // This can be either email or studentId

    @JsonProperty("password")
    private String password;

    
    public LoginRequest() {}

    
    public LoginRequest(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }

    
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "identifier='" + identifier + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}