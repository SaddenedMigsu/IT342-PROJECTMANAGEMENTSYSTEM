package com.it342.projectmanagementsystem.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final FirebaseMessaging firebaseMessaging;
    private final Firestore firestore;

    public NotificationService(FirebaseMessaging firebaseMessaging, Firestore firestore) {
        this.firebaseMessaging = firebaseMessaging;
        this.firestore = firestore;
    }

    public void sendAppointmentNotification(String userId, String title, String body, Map<String, String> data) {
        try {
            // Get user's FCM token from Firestore
            var userDoc = firestore.collection("users").document(userId).get().get();
            if (!userDoc.exists()) {
                logger.error("User not found: {}", userId);
                return;
            }

            String fcmToken = userDoc.getString("fcmToken");
            if (fcmToken == null || fcmToken.isEmpty()) {
                logger.warn("No FCM token found for user: {}", userId);
                return;
            }

            // Create notification
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            // Create message
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification)
                    .putAllData(data)
                    .build();

            // Send message
            String response = firebaseMessaging.send(message);
            logger.info("Successfully sent notification: {}", response);
        } catch (Exception e) {
            logger.error("Error sending notification: {}", e.getMessage());
        }
    }

    public void sendAppointmentReminder(String userId, String appointmentId, String title, String startTime) {
        Map<String, String> data = Map.of(
            "type", "APPOINTMENT_REMINDER",
            "appointmentId", appointmentId,
            "startTime", startTime
        );
        sendAppointmentNotification(userId, "Appointment Reminder", 
            "You have an appointment: " + title + " starting at " + startTime, data);
    }

    public void sendAppointmentUpdate(String userId, String appointmentId, String title, String status) {
        Map<String, String> data = Map.of(
            "type", "APPOINTMENT_UPDATE",
            "appointmentId", appointmentId,
            "status", status
        );
        sendAppointmentNotification(userId, "Appointment Update", 
            "Your appointment '" + title + "' has been " + status.toLowerCase(), data);
    }

    public void sendAppointmentRequest(String userId, String appointmentId, String title, String requesterName) {
        Map<String, String> data = Map.of(
            "type", "APPOINTMENT_REQUEST",
            "appointmentId", appointmentId,
            "requesterName", requesterName
        );
        sendAppointmentNotification(userId, "New Appointment Request", 
            requesterName + " has requested an appointment: " + title, data);
    }
} 