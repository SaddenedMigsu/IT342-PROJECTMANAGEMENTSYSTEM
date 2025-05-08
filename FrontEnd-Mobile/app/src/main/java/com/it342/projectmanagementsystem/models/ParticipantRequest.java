package com.it342.projectmanagementsystem.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import java.io.Serializable;

public class ParticipantRequest implements Serializable {
    @DocumentId
    private String id;
    private String appointmentId;
    private String studentId;
    private String studentName;
    private Timestamp requestTime;
    private String status; // PENDING, APPROVED, REJECTED
    
    // Empty constructor for Firestore
    public ParticipantRequest() {
    }
    
    public ParticipantRequest(String appointmentId, String studentId, String studentName, Timestamp requestTime, String status) {
        this.appointmentId = appointmentId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.requestTime = requestTime;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
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

    public Timestamp getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(Timestamp requestTime) {
        this.requestTime = requestTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
} 