package com.it342.projectmanagementsystem.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

/**
 * Model class for notification data
 */
public class Notification implements Serializable {
    @DocumentId
    private String id;
    private String title;
    private String message;
    private String recipientId;
    private String appointmentId;
    private String appointmentTitle;
    private String appointmentDate;
    private String appointmentTime;
    private boolean read;
    @ServerTimestamp
    private Timestamp createdAt;
    private String type;
    private String status;
    
    // Fields for faculty notifications format
    private String studentId;
    private String studentName;
    private String reason;
    
    // Fields for student notifications format
    private String requesterId;
    private String requesterName;
    
    // Missing fields causing mapping issues
    private String userId;
    private Object details; // Using Object type to accommodate different structure types

    // Required empty constructor for Firestore
    public Notification() {
    }

    // Constructor
    public Notification(String title, String message, String recipientId, 
                        String appointmentId, String appointmentTitle,
                        String appointmentDate, String appointmentTime) {
        this.title = title;
        this.message = message;
        this.recipientId = recipientId;
        this.appointmentId = appointmentId;
        this.appointmentTitle = appointmentTitle;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.read = false;
        this.createdAt = Timestamp.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getAppointmentId() {
        if (appointmentId != null && !appointmentId.isEmpty()) {
            return appointmentId;
        }
        // Try to get from details field for faculty notifications
        return null;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getAppointmentTitle() {
        return appointmentTitle;
    }

    public void setAppointmentTitle(String appointmentTitle) {
        this.appointmentTitle = appointmentTitle;
    }

    public String getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(String appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(String appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getDisplayName() {
        if (studentName != null && !studentName.isEmpty()) {
            return studentName;
        } else if (requesterName != null && !requesterName.isEmpty()) {
            return requesterName;
        } else {
            return "Unknown User";
        }
    }

    public String getDisplayReason() {
        if (reason != null && !reason.isEmpty()) {
            return reason;
        } else if (message != null && !message.isEmpty()) {
            return message;
        } else {
            return "No reason provided";
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Object getDetails() {
        return details;
    }

    public void setDetails(Object details) {
        this.details = details;
    }

    /**
     * For debugging purposes
     */
    @Override
    public String toString() {
        return "Notification{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", recipientId='" + recipientId + '\'' +
                ", userId='" + userId + '\'' +
                ", read=" + read +
                ", type='" + type + '\'' +
                '}';
    }
    
    /**
     * Checks if notification has valid data
     */
    public boolean isValid() {
        // Title is required
        if (title == null || title.isEmpty()) {
            return false;
        }
        
        // Either message or reason should be present
        if ((message == null || message.isEmpty()) && 
            (reason == null || reason.isEmpty())) {
            return false;
        }
        
        // Need at least one ID
        if ((recipientId == null || recipientId.isEmpty()) && 
            (userId == null || userId.isEmpty())) {
            return false;
        }
        
        return true;
    }
} 