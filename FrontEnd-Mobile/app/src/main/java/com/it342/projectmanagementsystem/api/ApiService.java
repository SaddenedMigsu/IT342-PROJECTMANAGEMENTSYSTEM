package com.it342.projectmanagementsystem.api;

import com.it342.projectmanagementsystem.models.AuthResponse;
import com.it342.projectmanagementsystem.models.LoginRequest;
import com.it342.projectmanagementsystem.models.RegisterRequest;
import com.it342.projectmanagementsystem.models.RegisterResponse;
import com.it342.projectmanagementsystem.models.User;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @POST("/api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest loginRequest);

    @POST("/api/auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest registerRequest);

    @GET("/api/auth/user/{id}")
    Call<User> getUserProfile(
        @Path("id") String userId,
        @Header("Authorization") String token
    );

    @POST("/api/auth/logout")
    Call<Void> logout(@Header("Authorization") String token);
} 