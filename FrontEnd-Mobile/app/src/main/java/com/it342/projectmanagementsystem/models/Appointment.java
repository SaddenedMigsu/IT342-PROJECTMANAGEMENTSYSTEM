package com.it342.projectmanagementsystem.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.Timestamp;
import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Exclude;

// Import the Kotlin Tag class specifically
import com.it342.projectmanagementsystem.models.Tag;

public class Appointment implements Parcelable {
    @SerializedName("_id")
    private String id;
    
    @SerializedName("appointmentId")
    private String appointmentId;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("location")
    private String location;
    
    @SerializedName("startTime")
    private Timestamp startTime;
    
    @SerializedName("endTime")
    private Timestamp endTime;
    
    @SerializedName("status")
    private String status;
    
    @SerializedName("facultyId")
    private String facultyId;
    
    @SerializedName("studentId")
    private String studentId;
    
    @SerializedName("participants")
    private List<String> participants;
    
    @SerializedName("tags")
    private Map<String, Tag> tags;
    
    @SerializedName("creatorName")
    private String creatorName;

    @SerializedName("reason")
    private String reason;

    // Constructor
    public Appointment() {
        this.tags = new HashMap<>();
    }

    // Getters and Setters with compatible method names
    public String getId() {
        // Return appointmentId if id is null
        if (id == null && appointmentId != null) {
            return appointmentId;
        }
        return id;
    }

    public void setId(String id) {
        this.id = id;
        // Also set appointmentId for compatibility
        if (this.appointmentId == null) {
            this.appointmentId = id;
        }
    }
    
    // Add getter and setter for appointmentId
    public String getAppointmentId() {
        return appointmentId;
    }
    
    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
        // Also set id for compatibility
        if (this.id == null) {
            this.id = appointmentId;
        }
    }

    public String getTitle() {
        if (title == null) {
            return "No title";
        }
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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
    
    public String getCreatorName() {
        return creatorName;
    }
    
    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }
    
    // Additional compatibility methods matching those in the Kotlin version
    public String getIdValue() {
        return id;
    }
    
    public String getTitleValue() {
        return title;
    }
    
    public String getDescriptionValue() {
        return description;
    }
    
    public Timestamp getStartTimeValue() {
        return startTime;
    }
    
    public Timestamp getEndTimeValue() {
        return endTime;
    }
    
    public String getStatusValue() {
        return status;
    }
    
    public String getFacultyIdValue() {
        return facultyId;
    }
    
    public String getStudentIdValue() {
        return studentId;
    }
    
    public void setTitleValue(String value) {
        this.title = value;
    }
    
    public void setDescriptionValue(String value) {
        this.description = value;
    }
    
    public void setStartTimeValue(Timestamp value) {
        this.startTime = value;
    }
    
    public void setEndTimeValue(Timestamp value) {
        this.endTime = value;
    }
    
    public void setStatusValue(String value) {
        this.status = value;
    }
    
    public void setParticipantsValue(List<String> value) {
        this.participants = value;
    }
    
    // Helper for date formatting
    public String getFormattedDateTime() {
        if (startTime != null) {
            Date date = startTime.toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila")); // Use Manila timezone
            return sdf.format(date);
        }
        return "Date not set";
    }
    
    // Update tag methods with simplified names
    public void addTag(String name, Tag tag) {
        if (this.tags == null) {
            this.tags = new HashMap<>();
        }
        this.tags.put(name, tag);
    }

    public void removeTag(String name) {
        if (this.tags != null) {
            this.tags.remove(name);
        }
    }

    public Tag getTagByName(String name) {
        if (this.tags != null) {
            return this.tags.get(name);
        }
        return null;
    }

    public boolean hasTag(String name) {
        return this.tags != null && this.tags.containsKey(name);
    }

    public List<Tag> getTagList() {
        if (this.tags == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(this.tags.values());
    }

    // Parcelable implementation
    protected Appointment(Parcel in) {
        id = in.readString();
        appointmentId = in.readString();
        title = in.readString();
        description = in.readString();
        location = in.readString();
        status = in.readString();
        facultyId = in.readString();
        studentId = in.readString();
        reason = in.readString();
        
        // Handle Timestamp serialization
        long startSeconds = in.readLong();
        int startNanos = in.readInt();
        startTime = startSeconds == -1 ? null : new Timestamp(startSeconds, startNanos);
        
        long endSeconds = in.readLong();
        int endNanos = in.readInt();
        endTime = endSeconds == -1 ? null : new Timestamp(endSeconds, endNanos);
        
        participants = in.createStringArrayList();
        
        // Read tags map with Kotlin Tag class
        int tagsSize = in.readInt();
        this.tags = new HashMap<>();
        for (int i = 0; i < tagsSize; i++) {
            String key = in.readString();
            Tag tag = in.readParcelable(Tag.class.getClassLoader());
            this.tags.put(key, tag);
        }
    }

    public static final Creator<Appointment> CREATOR = new Creator<Appointment>() {
        @Override
        public Appointment createFromParcel(Parcel in) {
            return new Appointment(in);
        }

        @Override
        public Appointment[] newArray(int size) {
            return new Appointment[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(appointmentId);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(location);
        dest.writeString(status);
        dest.writeString(facultyId);
        dest.writeString(studentId);
        dest.writeString(reason);
        
        // Handle Timestamp serialization
        if (startTime != null) {
            dest.writeLong(startTime.getSeconds());
            dest.writeInt(startTime.getNanoseconds());
        } else {
            dest.writeLong(-1);
            dest.writeInt(0);
        }
        
        if (endTime != null) {
            dest.writeLong(endTime.getSeconds());
            dest.writeInt(endTime.getNanoseconds());
        } else {
            dest.writeLong(-1);
            dest.writeInt(0);
        }
        
        dest.writeStringList(participants);
        
        // Write tags map
        if (tags != null) {
            dest.writeInt(tags.size());
            for (Map.Entry<String, Tag> entry : tags.entrySet()) {
                dest.writeString(entry.getKey());
                dest.writeParcelable(entry.getValue(), flags);
            }
        } else {
            dest.writeInt(0);
        }
    }

    // Add a toString method to help with debugging
    @Override
    public String toString() {
        return "Appointment{" +
                "id='" + id + '\'' +
                ", appointmentId='" + appointmentId + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", startTime=" + (startTime != null ? startTime.toDate() : "null") +
                ", status='" + status + '\'' +
                ", tags=" + (tags != null ? tags.size() : "null") +
                '}';
    }

    // Format the date for display in a consistent timezone
    public String getFormattedStartTime() {
        if (startTime == null) return "N/A";
        return formatTimestamp(startTime);
    }

    public String getFormattedEndTime() {
        if (endTime == null) return "N/A";
        return formatTimestamp(endTime);
    }

    // Helper method to format timestamp in the correct timezone
    private String formatTimestamp(Timestamp timestamp) {
        Date date = timestamp.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila")); // Use Manila timezone
        return sdf.format(date);
    }

    // Get formatted date for display on cards
    public String getDisplayDate() {
        if (startTime == null) return "No date set";
        Date date = startTime.toDate();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila")); // Use Manila timezone
        return dateFormat.format(date);
    }

    // Get formatted time for display on cards
    public String getDisplayTime() {
        if (startTime == null || endTime == null) return "No time set";
        
        Date startDate = startTime.toDate();
        Date endDate = endTime.toDate();
        
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        timeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila")); // Use Manila timezone
        
        return timeFormat.format(startDate) + " - " + timeFormat.format(endDate);
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
} 