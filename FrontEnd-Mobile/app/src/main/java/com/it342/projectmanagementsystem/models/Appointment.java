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
    public Map<String, Object> tags;
    
    @SerializedName("creatorName")
    private String creatorName;

    @SerializedName("reason")
    private String reason;

    @SerializedName("createdAt")
    private Timestamp createdAt;

    @SerializedName("createdBy")
    private String createdBy;

    @SerializedName("facultyApprovals")
    private Map<String, Boolean> facultyApprovals;

    @SerializedName("requiresApproval")
    private Boolean requiresApproval;

    @SerializedName("updatedAt")
    private Timestamp updatedAt;
    
    @SerializedName("appointmentType")
    private String appointmentType;
    
    @SerializedName("timezone")
    private String timezone;

    @SerializedName("userRole")
    private String userRole;
    
    @SerializedName("userStatus")
    private String userStatus;
    
    @SerializedName("hasApproved")
    private Boolean hasApproved;
    
    @SerializedName("facultyName")
    private String facultyName;

    // Constructor
    public Appointment() {
        this.tags = new HashMap<>();
        this.facultyApprovals = new HashMap<>();
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
    
    @SuppressWarnings("unchecked")
    public Map<String, Tag> getTags() {
        if (tags == null) {
            return new HashMap<>();
        }
        
        // Convert tags to Tag objects if needed
        Map<String, Tag> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : tags.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                try {
                    Map<String, Object> tagMap = (Map<String, Object>) value;
                    String name = (String) tagMap.get("name");
                    String color = (String) tagMap.get("color");
                    if (name != null && color != null) {
                        result.put(key, new Tag(name, color));
                    }
                } catch (ClassCastException e) {
                    // Skip this entry if there's a casting issue
                    continue;
                }
            } else if (value instanceof Tag) {
                result.put(key, (Tag) value);
            }
        }
        
        return result;
    }

    public void setTags(Map<String, Object> tags) {
        this.tags = tags;
    }
    
    // Add method to set Map<String, Tag>
    public void setTagsFromTagMap(Map<String, Tag> tagMap) {
        if (tagMap == null) {
            this.tags = null;
            return;
        }
        
        Map<String, Object> newTags = new HashMap<>();
        for (Map.Entry<String, Tag> entry : tagMap.entrySet()) {
            String key = entry.getKey();
            Tag tag = entry.getValue();
            
            // Create a map with name and color instead of storing the Tag directly
            Map<String, Object> tagData = new HashMap<>();
            tagData.put("name", tag.getName());
            tagData.put("color", tag.getColorForJava());
            
            newTags.put(key, tagData);
        }
        this.tags = newTags;
    }
    
    public List<Tag> getTagList() {
        // Convert the map to a list of Tag objects
        List<Tag> tagList = new ArrayList<>();
        
        if (this.tags == null) {
            return tagList;
        }
        
        // Extract each tag from the map
        for (Map.Entry<String, Object> entry : this.tags.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Tag) {
                tagList.add((Tag) value);
            } else if (value instanceof Map) {
                try {
                    // Convert map to Tag
                    @SuppressWarnings("unchecked")
                    Map<String, Object> tagMap = (Map<String, Object>) value;
                    String name = (String) tagMap.get("name");
                    String color = (String) tagMap.get("color");
                    if (name != null && color != null) {
                        tagList.add(new Tag(name, color));
                    }
                } catch (ClassCastException e) {
                    // Handle potential cast issue
                    continue;
                }
            }
        }
        
        return tagList;
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
        
        // Store tag as a Map<String, Object> to match Firebase structure
        Map<String, Object> tagData = new HashMap<>();
        tagData.put("name", tag.getName());
        tagData.put("color", tag.getColorForJava());
        
        this.tags.put(name, tagData);
    }

    public void removeTag(String name) {
        if (this.tags != null) {
            this.tags.remove(name);
        }
    }

    public Tag getTagByName(String name) {
        if (this.tags == null) {
            return null;
        }
        
        // First check if it's directly in the map
        Object tagObj = this.tags.get(name);
        if (tagObj instanceof Tag) {
            return (Tag) tagObj;
        } else if (tagObj instanceof Map) {
            // It could be a map with name and color
            @SuppressWarnings("unchecked")
            Map<String, Object> tagMap = (Map<String, Object>) tagObj;
            String tagName = (String) tagMap.get("name");
            String tagColor = (String) tagMap.get("color");
            if (tagName != null && tagColor != null) {
                return new Tag(tagName, tagColor);
            }
        }
        
        // Check for nested tag fields like "tags.Very Important"
        String prefixToCheck = "tags.";
        for (Map.Entry<String, Object> entry : this.tags.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(prefixToCheck) && key.substring(prefixToCheck.length()).equals(name)) {
                Object value = entry.getValue();
                if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> tagMap = (Map<String, Object>) value;
                    String tagName = (String) tagMap.get("name");
                    String tagColor = (String) tagMap.get("color");
                    if (tagName != null && tagColor != null) {
                        return new Tag(tagName, tagColor);
                    }
                }
            }
        }
        
        return null;
    }

    public boolean hasTag(String name) {
        return this.tags != null && this.tags.containsKey(name);
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
        createdBy = in.readString();
        requiresApproval = in.readByte() != 0;
        appointmentType = in.readString();
        timezone = in.readString();
        
        // Read timestamps
        long startSeconds = in.readLong();
        int startNanos = in.readInt();
        if (startSeconds != -1) {
            startTime = new Timestamp(startSeconds, startNanos);
        }
        
        long endSeconds = in.readLong();
        int endNanos = in.readInt();
        if (endSeconds != -1) {
            endTime = new Timestamp(endSeconds, endNanos);
        }
        
        long createdAtSeconds = in.readLong();
        int createdAtNanos = in.readInt();
        if (createdAtSeconds != -1) {
            createdAt = new Timestamp(createdAtSeconds, createdAtNanos);
        }
        
        long updatedAtSeconds = in.readLong();
        int updatedAtNanos = in.readInt();
        if (updatedAtSeconds != -1) {
            updatedAt = new Timestamp(updatedAtSeconds, updatedAtNanos);
        }
        
        // Reconstructing the participants list
        if (in.readByte() == 1) {
            int participantsSize = in.readInt();
            participants = new ArrayList<>(participantsSize);
            for (int i = 0; i < participantsSize; i++) {
                participants.add(in.readString());
            }
        }
        
        // Reconstructing the facultyApprovals map
        if (in.readByte() == 1) {
            int approvalsSize = in.readInt();
            facultyApprovals = new HashMap<>(approvalsSize);
            for (int i = 0; i < approvalsSize; i++) {
                String key = in.readString();
                boolean value = in.readByte() == 1;
                facultyApprovals.put(key, value);
            }
        } else {
            facultyApprovals = new HashMap<>();
        }
        
        // Reconstructing the tags map - update to handle Map<String, Object>
        int tagsSize = in.readInt();
        if (tagsSize > 0) {
            tags = new HashMap<>(tagsSize);
            for (int i = 0; i < tagsSize; i++) {
                String key = in.readString();
                String color = in.readString();
                
                // Create a map with name and color
                Map<String, String> tagMap = new HashMap<>();
                tagMap.put("name", key);
                tagMap.put("color", color);
                
                // Store in the tags map
                tags.put(key, tagMap);
            }
        } else {
            tags = new HashMap<>();
        }
        
        // Read creator name if available
        if (in.readByte() == 1) {
            creatorName = in.readString();
        }
        
        // Read user role and status if available
        if (in.readByte() == 1) {
            userRole = in.readString();
            userStatus = in.readString();
            hasApproved = in.readByte() == 1;
        }
        
        // Read faculty name if available
        if (in.readByte() == 1) {
            facultyName = in.readString();
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
        dest.writeString(createdBy);
        dest.writeByte((byte) (requiresApproval != null && requiresApproval ? 1 : 0));
        dest.writeString(appointmentType);
        dest.writeString(timezone);
        
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
        
        if (createdAt != null) {
            dest.writeLong(createdAt.getSeconds());
            dest.writeInt(createdAt.getNanoseconds());
        } else {
            dest.writeLong(-1);
            dest.writeInt(0);
        }
        
        if (updatedAt != null) {
            dest.writeLong(updatedAt.getSeconds());
            dest.writeInt(updatedAt.getNanoseconds());
        } else {
            dest.writeLong(-1);
            dest.writeInt(0);
        }
        
        // Writing participants list
        if (participants != null && !participants.isEmpty()) {
            dest.writeByte((byte) 1);
            dest.writeInt(participants.size());
            for (String participant : participants) {
                dest.writeString(participant);
            }
        } else {
            dest.writeByte((byte) 0);
        }
        
        // Writing faculty approvals map
        if (facultyApprovals != null && !facultyApprovals.isEmpty()) {
            dest.writeByte((byte) 1);
            dest.writeInt(facultyApprovals.size());
            for (Map.Entry<String, Boolean> entry : facultyApprovals.entrySet()) {
                dest.writeString(entry.getKey());
                dest.writeByte((byte) (entry.getValue() ? 1 : 0));
            }
        } else {
            dest.writeByte((byte) 0);
        }
        
        // Writing tags map - simplify to only write Tag objects, not all Object types
        if (tags != null) {
            // First convert our custom structure to a simple map of tag name to tag color
            Map<String, String> simpleTags = new HashMap<>();
            for (Map.Entry<String, Object> entry : tags.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                if (value instanceof Tag) {
                    Tag tag = (Tag) value;
                    String color = null;
                    try {
                        color = tag.getColorForJava();
                    } catch (Exception e) {
                        try {
                            color = tag.getColor();
                        } catch (Exception ignored) {}
                    }
                    if (color != null) {
                        simpleTags.put(key, color);
                    }
                } else if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> tagMap = (Map<String, Object>) value;
                    String color = (String) tagMap.get("color");
                    if (color != null) {
                        simpleTags.put(key, color);
                    }
                }
            }
            
            // Write the simple map
            dest.writeInt(simpleTags.size());
            for (Map.Entry<String, String> entry : simpleTags.entrySet()) {
                dest.writeString(entry.getKey());
                dest.writeString(entry.getValue());
            }
        } else {
            dest.writeInt(0);
        }
        
        // Write creator name if available
        if (creatorName != null) {
            dest.writeByte((byte) 1);
            dest.writeString(creatorName);
        } else {
            dest.writeByte((byte) 0);
        }
        
        // Write user role and status if available
        if (userRole != null) {
            dest.writeByte((byte) 1);
            dest.writeString(userRole);
            dest.writeString(userStatus != null ? userStatus : "");
            dest.writeByte((byte) (hasApproved != null && hasApproved ? 1 : 0));
        } else {
            dest.writeByte((byte) 0);
        }
        
        // Write faculty name if available
        if (facultyName != null) {
            dest.writeByte((byte) 1);
            dest.writeString(facultyName);
        } else {
            dest.writeByte((byte) 0);
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

    // Add getters and setters for new fields
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Map<String, Boolean> getFacultyApprovals() {
        return facultyApprovals;
    }

    public void setFacultyApprovals(Map<String, Boolean> facultyApprovals) {
        this.facultyApprovals = facultyApprovals;
    }

    public Boolean getRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(Boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getAppointmentType() {
        return appointmentType;
    }
    
    public void setAppointmentType(String appointmentType) {
        this.appointmentType = appointmentType;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
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
    
    public String getFacultyName() {
        return facultyName;
    }
    
    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }
} 