package com.it342.projectmanagementsystem.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final Firestore firestore;
    
    private static final String CHANNEL_ID = "appointments";

    public NotificationService(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Send a notification for an appointment request
     */
    public void sendAppointmentRequest(String userId, String appointmentId, String title, String requesterName) {
        try {
            String message = requesterName + " has requested an appointment with you.";
            Map<String, String> data = new HashMap<>();
            data.put("type", "APPOINTMENT_REQUEST");
            data.put("appointmentId", appointmentId);
            
            sendNotification(userId, "Appointment Request: " + title, message, data);
            logger.info("Sent appointment request notification to userId: {}", userId);
        } catch (Exception e) {
            logger.error("Failed to send appointment request notification to {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Send a notification for an appointment status update (approved/denied)
     */
    public void sendAppointmentUpdate(String userId, String appointmentId, String title, String status) {
        try {
            String message = "Your appointment request '" + title + "' has been " + status.toLowerCase() + ".";
            Map<String, String> data = new HashMap<>();
            data.put("type", "APPOINTMENT_UPDATE");
            data.put("appointmentId", appointmentId);
            data.put("status", status);
            
            sendNotification(userId, "Appointment Update", message, data);
            logger.info("Sent appointment update notification ({}) to userId: {}", status, userId);
        } catch (Exception e) {
            logger.error("Failed to send appointment update notification to {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Send a general appointment notification
     */
    public void sendAppointmentNotification(String userId, String title, String message, Map<String, String> data) {
        try {
            sendNotification(userId, title, message, data);
            logger.info("Sent general appointment notification to userId: {}", userId);
        } catch (Exception e) {
            logger.error("Failed to send appointment notification to {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Send an appointment reminder notification
     */
    public void sendAppointmentReminder(String userId, String appointmentId, String title, String startTime) {
        try {
            String message = "Reminder: Your appointment '" + title + "' is scheduled for " + startTime;
            Map<String, String> data = new HashMap<>();
            data.put("type", "APPOINTMENT_REMINDER");
            data.put("appointmentId", appointmentId);
            data.put("startTime", startTime);
            
            sendNotification(userId, "Appointment Reminder", message, data);
            logger.info("Sent appointment reminder notification to userId: {}", userId);
        } catch (Exception e) {
            logger.error("Failed to send appointment reminder notification to {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Core method to send FCM notifications
     */
    private void sendNotification(String userId, String title, String body, Map<String, String> data) throws ExecutionException, InterruptedException {
        try {
            // Get user's FCM token from Firestore
            var userDoc = firestore.collection("users").document(userId).get().get();
            if (!userDoc.exists()) {
                logger.error("User document not found for userId: {}", userId);
                return;
            }
            
            String token = userDoc.getString("fcmToken");
            if (token == null || token.isEmpty()) {
                // If token not found in user document, check devices subcollection
                var deviceDocs = firestore.collection("users")
                        .document(userId)
                        .collection("devices")
                        .get()
                        .get()
                        .getDocuments();
                
                if (!deviceDocs.isEmpty()) {
                    token = deviceDocs.iterator().next().getString("fcmToken");
                }
            }
            
            if (token == null || token.isEmpty()) {
                logger.error("No FCM token found for userId: {}", userId);
                return;
            }
            
            // Build notification
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();
            
            // Add default channel ID for Android
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setNotification(AndroidNotification.builder()
                            .setChannelId(CHANNEL_ID)
                            .build())
                    .build();
            
            // Create the message
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(notification)
                    .setAndroidConfig(androidConfig)
                    .putAllData(data)
                    .build();
            
            // Send the message
            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("Successfully sent notification to userId: {} with FCM response: {}", userId, response);
            
            // Record notification sending in Firestore for tracking
            Map<String, Object> notificationHistory = new HashMap<>();
            notificationHistory.put("userId", userId);
            notificationHistory.put("title", title);
            notificationHistory.put("body", body);
            notificationHistory.put("data", data);
            notificationHistory.put("sentAt", Timestamp.now());
            notificationHistory.put("successful", true);
            
            firestore.collection("notification_history").document().set(notificationHistory);
            
        } catch (FirebaseMessagingException e) {
            logger.error("FirebaseMessagingException while sending notification to {}: {}", userId, e.getMessage());
            
            // Record failed notification attempt
            Map<String, Object> notificationHistory = new HashMap<>();
            notificationHistory.put("userId", userId);
            notificationHistory.put("title", title);
            notificationHistory.put("body", body);
            notificationHistory.put("data", data);
            notificationHistory.put("sentAt", Timestamp.now());
            notificationHistory.put("successful", false);
            notificationHistory.put("errorMessage", e.getMessage());
            
            firestore.collection("notification_history").document().set(notificationHistory);
            
            throw new RuntimeException("Failed to send FCM notification: " + e.getMessage());
        }
    }

    /**
     * Send notifications to multiple users
     */
    public void sendBulkNotifications(List<String> userIds, String title, String body, Map<String, String> data) {
        List<String> successfulSends = new ArrayList<>();
        List<String> failedSends = new ArrayList<>();
        
        for (String userId : userIds) {
            try {
                sendNotification(userId, title, body, data);
                successfulSends.add(userId);
            } catch (Exception e) {
                failedSends.add(userId);
                logger.error("Failed to send notification to userId {}: {}", userId, e.getMessage());
            }
        }
        
        logger.info("Bulk notification summary - Successful: {}, Failed: {}", 
                successfulSends.size(), failedSends.size());
    }
} 