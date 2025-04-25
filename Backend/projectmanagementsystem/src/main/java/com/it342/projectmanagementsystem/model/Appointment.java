package com.it342.projectmanagementsystem.model;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Appointment {
    @JsonProperty("appointmentId")
    private String appointmentId;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("startTime")
    private Timestamp startTime;
    
    @JsonProperty("endTime")
    private Timestamp endTime;
    
    @JsonProperty("createdBy")
    private String createdBy;
    
    @JsonProperty("creatorName")
    private String creatorName;
    
    @JsonProperty("participants")
    private List<String> participants;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("createdAt")
    private Timestamp createdAt;
    
    @JsonProperty("updatedAt")
    private Timestamp updatedAt;
    
    // User-specific appointment data
    @JsonProperty("userRole")
    private String userRole;    // CREATOR or PARTICIPANT
    
    @JsonProperty("userStatus")
    private String userStatus;  // PENDING, CONFIRMED, etc.
    
    @JsonProperty("hasApproved")
    private Boolean hasApproved; // For faculty members to track their approval status

    @JsonProperty("facultyName")
    private String facultyName; // To store faculty name for appointments

    public Appointment() {
    }

    @PropertyName("appointmentId")
    @JsonProperty("appointmentId")
    public String getAppointmentId() {
        return appointmentId;
    }

    @PropertyName("appointmentId")
    @JsonProperty("appointmentId")
    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    @PropertyName("title")
    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @PropertyName("title")
    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    @PropertyName("description")
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @PropertyName("description")
    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @PropertyName("startTime")
    @JsonProperty("startTime")
    public Timestamp getStartTime() {
        return startTime;
    }

    @PropertyName("startTime")
    @JsonProperty("startTime")
    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    @PropertyName("endTime")
    @JsonProperty("endTime")
    public Timestamp getEndTime() {
        return endTime;
    }

    @PropertyName("endTime")
    @JsonProperty("endTime")
    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    @PropertyName("createdBy")
    @JsonProperty("createdBy")
    public String getCreatedBy() {
        return createdBy;
    }

    @PropertyName("createdBy")
    @JsonProperty("createdBy")
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @PropertyName("creatorName")
    @JsonProperty("creatorName")
    public String getCreatorName() {
        return creatorName;
    }

    @PropertyName("creatorName")
    @JsonProperty("creatorName")
    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    @PropertyName("participants")
    @JsonProperty("participants")
    public List<String> getParticipants() {
        return participants;
    }

    @PropertyName("participants")
    @JsonProperty("participants")
    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    @PropertyName("status")
    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    @PropertyName("status")
    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @PropertyName("createdAt")
    @JsonProperty("createdAt")
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @PropertyName("createdAt")
    @JsonProperty("createdAt")
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @PropertyName("updatedAt")
    @JsonProperty("updatedAt")
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    @PropertyName("updatedAt")
    @JsonProperty("updatedAt")
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @JsonProperty("userRole")
    public String getUserRole() {
        return userRole;
    }

    @JsonProperty("userRole")
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    @JsonProperty("userStatus")
    public String getUserStatus() {
        return userStatus;
    }

    @JsonProperty("userStatus")
    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    @JsonProperty("hasApproved")
    public Boolean getHasApproved() {
        return hasApproved;
    }

    @JsonProperty("hasApproved")
    public void setHasApproved(Boolean hasApproved) {
        this.hasApproved = hasApproved;
    }

    @JsonProperty("facultyName")
    public String getFacultyName() {
        return facultyName;
    }

    @JsonProperty("facultyName")
    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }
} 