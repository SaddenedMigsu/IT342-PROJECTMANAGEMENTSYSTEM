package com.it342.projectmanagementsystem.models;

import com.google.gson.annotations.SerializedName;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.List;

public class FacultyAppointmentRequest {
    @SerializedName("title")
    private String title;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("startTime")
    private String startTime;
    
    @SerializedName("endTime")
    private String endTime;
    
    @SerializedName("userId")
    private String userId;
    
    @SerializedName("type")
    private String type = "FACULTY";
    
    @SerializedName("location")
    private String location = "Virtual Meeting";

    @SerializedName("participants")
    private List<String> participants;

    private static final SimpleDateFormat ISO_FORMAT;
    
    static {
        ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStartTime(TimestampObject timestampObj) {
        Date date = new Date(timestampObj.toMillis());
        this.startTime = ISO_FORMAT.format(date);
    }

    public void setEndTime(TimestampObject timestampObj) {
        Date date = new Date(timestampObj.toMillis());
        this.endTime = ISO_FORMAT.format(date);
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public void setType(String type) {
        this.type = type;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getUserId() {
        return userId;
    }
    
    public String getLocation() {
        return location;
    }
    
    public String getType() {
        return type;
    }

    public List<String> getParticipants() {
        return participants;
    }
} 