package com.it342.projectmanagementsystem.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;

// Import the Kotlin Tag class specifically
import com.it342.projectmanagementsystem.models.Tag;

public class AppointmentWithTags {
    private String id;
    private String title;
    private String description;
    private Timestamp startTime;
    private Timestamp endTime;
    private String status;
    private String facultyId;
    private String studentId;
    private String createdBy;
    private String creatorName;
    private List<String> participants;
    private Map<String, Tag> tags;

    // Default constructor required for Firestore
    public AppointmentWithTags() {
        this.tags = new HashMap<>();
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(String facultyId) {
        this.facultyId = facultyId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public Map<String, Tag> getTags() {
        return tags;
    }

    public void setTags(Map<String, Tag> tags) {
        this.tags = tags;
    }

    // Custom method suffix to avoid conflicts with Kotlin implementation
    public void addTagJava(String name, Tag tag) {
        if (this.tags == null) {
            this.tags = new HashMap<>();
        }
        this.tags.put(name, tag);
    }

    public void removeTagJava(String name) {
        if (this.tags != null) {
            this.tags.remove(name);
        }
    }

    public Tag getTagJava(String name) {
        if (this.tags != null) {
            return this.tags.get(name);
        }
        return null;
    }

    public boolean hasTagJava(String name) {
        return this.tags != null && this.tags.containsKey(name);
    }

    public List<Tag> getTagList() {
        if (this.tags == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(this.tags.values());
    }
} 