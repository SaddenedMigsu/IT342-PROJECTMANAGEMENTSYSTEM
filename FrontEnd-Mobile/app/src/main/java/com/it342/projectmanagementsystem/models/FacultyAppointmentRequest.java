package com.it342.projectmanagementsystem.models;

import com.google.gson.annotations.SerializedName;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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
} 