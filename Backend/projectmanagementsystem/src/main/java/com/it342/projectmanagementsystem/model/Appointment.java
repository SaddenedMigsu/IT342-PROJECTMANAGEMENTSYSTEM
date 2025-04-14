package com.it342.projectmanagementsystem.model;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.PropertyName;

import java.util.List;

public class Appointment {
    private String appointmentId;
    private String title;
    private String description;
    private Timestamp startTime;
    private Timestamp endTime;
    private String createdBy;
    private List<String> participants;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // User-specific appointment data
    private String userRole;    // CREATOR or PARTICIPANT
    private String userStatus;  // PENDING, CONFIRMED, etc.
    private Boolean hasApproved; // For faculty members to track their approval status

    public Appointment() {
    }

    @PropertyName("appointmentId")
    public String getAppointmentId() {
        return appointmentId;
    }

    @PropertyName("appointmentId")
    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    @PropertyName("title")
    public String getTitle() {
        return title;
    }

    @PropertyName("title")
    public void setTitle(String title) {
        this.title = title;
    }

    @PropertyName("description")
    public String getDescription() {
        return description;
    }

    @PropertyName("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @PropertyName("startTime")
    public Timestamp getStartTime() {
        return startTime;
    }

    @PropertyName("startTime")
    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    @PropertyName("endTime")
    public Timestamp getEndTime() {
        return endTime;
    }

    @PropertyName("endTime")
    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    @PropertyName("createdBy")
    public String getCreatedBy() {
        return createdBy;
    }

    @PropertyName("createdBy")
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @PropertyName("participants")
    public List<String> getParticipants() {
        return participants;
    }

    @PropertyName("participants")
    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    @PropertyName("status")
    public String getStatus() {
        return status;
    }

    @PropertyName("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @PropertyName("createdAt")
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @PropertyName("createdAt")
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @PropertyName("updatedAt")
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    @PropertyName("updatedAt")
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    public Boolean getHasApproved() {
        return hasApproved;
    }

    public void setHasApproved(Boolean hasApproved) {
        this.hasApproved = hasApproved;
    }
} 