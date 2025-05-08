package com.it342.projectmanagementsystem.utils;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.it342.projectmanagementsystem.models.Appointment;
import com.it342.projectmanagementsystem.models.Notification;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Helper class for creating and managing notifications in the app
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    
    /**
     * Create a notification for an appointment request
     * 
     * @param appointment The appointment object
     * @param studentName The name of the student who requested the appointment
     * @param facultyId The ID of the faculty member
     */
    public static void createAppointmentRequestNotification(
            Appointment appointment, String studentName, String facultyId) {
        
        if (appointment == null || facultyId == null || facultyId.isEmpty()) {
            Log.e(TAG, "Cannot create notification with null appointment or faculty ID");
            return;
        }
        
        Log.d(TAG, "Creating appointment request notification for faculty: " + facultyId);
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
        timeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
        
        String appointmentDate = "Unknown date";
        String appointmentTime = "Unknown time";
        
        try {
            if (appointment.getStartTime() != null) {
                appointmentDate = dateFormat.format(appointment.getStartTime().toDate());
                appointmentTime = timeFormat.format(appointment.getStartTime().toDate());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting appointment date/time", e);
        }
        
        // Create a notification object
        Notification notification = new Notification(
                "Appointment Request",
                studentName + " has requested an appointment with you",
                facultyId,
                appointment.getId(),
                appointment.getTitle(),
                appointmentDate,
                appointmentTime
        );
        
        // Save to Firestore
        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Notification created with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating notification", e);
                });
    }
    
    /**
     * Mark a notification as read
     * 
     * @param notificationId The ID of the notification to mark as read
     */
    public static void markNotificationAsRead(String notificationId) {
        if (notificationId == null || notificationId.isEmpty()) {
            return;
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("read", true);
        
        db.collection("notifications")
                .document(notificationId)
                .update(updates)
                .addOnSuccessListener(aVoid -> 
                    Log.d(TAG, "Notification " + notificationId + " marked as read"))
                .addOnFailureListener(e -> 
                    Log.e(TAG, "Error marking notification as read", e));
    }
} 