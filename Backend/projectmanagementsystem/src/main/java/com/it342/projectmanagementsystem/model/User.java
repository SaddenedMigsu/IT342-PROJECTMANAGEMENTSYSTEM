package com.it342.projectmanagementsystem.model;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.PropertyName;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class User implements UserDetails {
    private String userId;
    private String studId;
    private String firstName;
    private String lastName;
    private String course;
    private String email;
    private String password;
    private String role;
    private String phoneNumber;
    private String profilePicture;
    private Timestamp createdAt;
    private boolean enabled;

    // Default constructor
    public User() {}

    // Constructor with all fields
    public User(String userId, String studId, String firstName, String lastName, String course, 
                String email, String password, String role, String phoneNumber, String profilePicture, Timestamp createdAt, boolean enabled) {
        this.userId = userId;
        this.studId = studId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.course = course;
        this.email = email;
        this.password = password;
        this.role = role;
        this.phoneNumber = phoneNumber;
        this.profilePicture = profilePicture;
        this.createdAt = createdAt;
        this.enabled = enabled;
    }

    @PropertyName("userId")
    public String getUserId() {
        return userId;
    }

    @PropertyName("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @PropertyName("studId")
    public String getStudId() {
        return studId;
    }

    @PropertyName("studId")
    public void setStudId(String studId) {
        this.studId = studId;
    }

    @PropertyName("firstName")
    public String getFirstName() {
        return firstName;
    }

    @PropertyName("firstName")
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @PropertyName("lastName")
    public String getLastName() {
        return lastName;
    }

    @PropertyName("lastName")
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @PropertyName("course")
    public String getCourse() {
        return course;
    }

    @PropertyName("course")
    public void setCourse(String course) {
        this.course = course;
    }

    @PropertyName("email")
    public String getEmail() {
        return email;
    }

    @PropertyName("email")
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @PropertyName("password")
    public String getPassword() {
        return password;
    }

    @PropertyName("password")
    public void setPassword(String password) {
        this.password = password;
    }

    @PropertyName("role")
    public String getRole() {
        return role;
    }

    @PropertyName("role")
    public void setRole(String role) {
        this.role = role;
    }

    @PropertyName("createdAt")
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @PropertyName("createdAt")
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @PropertyName("phoneNumber")
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @PropertyName("phoneNumber")
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @PropertyName("profilePicture")
    public String getProfilePicture() {
        return profilePicture;
    }

    @PropertyName("profilePicture")
    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", studId='" + studId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", course='" + course + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", createdAt=" + createdAt +
                ", enabled=" + enabled +
                '}';
    }
}