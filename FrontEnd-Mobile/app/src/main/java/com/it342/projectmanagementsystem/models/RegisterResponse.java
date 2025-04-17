package com.it342.projectmanagementsystem.models;

import com.google.gson.annotations.SerializedName;

public class RegisterResponse {
    @SerializedName("token")
    private String token;

    @SerializedName("userId")
    private String userId;

    public String getToken() {
        return token;
    }

    public String getUserId() {
        return userId;
    }
} 