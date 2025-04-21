package com.it342.projectmanagementsystem.models;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("firstName")
    private String firstName;

    @SerializedName("lastName")
    private String lastName;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("role")
    private String role;

    @SerializedName("course")
    private String course;

    // Default constructor
    public RegisterRequest() {
    }

    // Parameterized constructor
    public RegisterRequest(String firstName, String lastName, String email, String password, String role, String course) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.role = role.toUpperCase(); // Ensure role is uppercase
        this.course = course;
    }

    // Getters
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getCourse() { return course; }

    // Setters
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(String role) { this.role = role.toUpperCase(); } // Ensure role is uppercase
    public void setCourse(String course) { this.course = course; }
} 