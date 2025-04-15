package com.it342.projectmanagementsystem.api;

import com.it342.projectmanagementsystem.models.AuthResponse;
import com.it342.projectmanagementsystem.models.LoginRequest;
import com.it342.projectmanagementsystem.models.RegisterRequest;
import com.it342.projectmanagementsystem.models.RegisterResponse;
import com.it342.projectmanagementsystem.models.User;
import com.it342.projectmanagementsystem.models.Appointment;
import com.it342.projectmanagementsystem.models.FacultyAppointmentRequest;
import com.it342.projectmanagementsystem.models.Faculty;
import com.it342.projectmanagementsystem.models.AppointmentRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

import java.util.List;

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

    @POST("/api/appointments/create")
    Call<Appointment> createAppointment(@Body AppointmentRequest request, @Header("Authorization") String token);

    @POST("/api/appointments/request-faculty")
    Call<Appointment> requestFacultyAppointment(@Body FacultyAppointmentRequest request);

    @GET("/api/appointments/user/{userId}")
    Call<List<Appointment>> getUserAppointments(
        @Path("userId") String userId,
        @Header("Authorization") String token
    );

    @GET("/api/appointments/{appointmentId}")
    Call<Appointment> getAppointment(
        @Path("appointmentId") String appointmentId,
        @Header("Authorization") String token
    );

    @PUT("/api/appointments/{appointmentId}")
    Call<Appointment> updateAppointment(
        @Path("appointmentId") String appointmentId,
        @Body Appointment appointment,
        @Header("Authorization") String token
    );

    @DELETE("/api/appointments/{appointmentId}")
    Call<Void> deleteAppointment(
        @Path("appointmentId") String appointmentId,
        @Header("Authorization") String token
    );

    @GET("/api/users/faculty")
    Call<List<Faculty>> getFacultyMembers(@Header("Authorization") String token);

    @GET("/api/appointments")
    Call<List<Appointment>> getAppointments(@Header("Authorization") String token);
} 