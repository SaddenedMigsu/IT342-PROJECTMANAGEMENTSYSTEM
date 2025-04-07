package com.it342.projectmanagementsystem.dto;

public class RegisterRequest {
    private String studId;
    private String firstName;
    private String lastName;
    private String course;
    private String email;
    private String password;
    private String role;

    public RegisterRequest() {
    }

    public RegisterRequest(String studId, String firstName, String lastName, String course, 
                         String email, String password, String role) {
        this.studId = studId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.course = course;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public String getStudId() {
        return studId;
    }

    public void setStudId(String studId) {
        this.studId = studId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}