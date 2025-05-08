package com.it342.projectmanagementsystem.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.activities.AppointmentDetailsActivity;
import com.it342.projectmanagementsystem.activities.FacultyDashboardActivity;
import com.it342.projectmanagementsystem.activities.HomePage;
import com.it342.projectmanagementsystem.activities.ManageAppointmentParticipantsActivity;
import com.it342.projectmanagementsystem.activities.ManageAppointmentRequestsActivity;
import com.it342.projectmanagementsystem.activities.NotificationsActivity;
import com.it342.projectmanagementsystem.api.ApiService;
import com.it342.projectmanagementsystem.api.RetrofitClient;
import com.it342.projectmanagementsystem.utils.Constants;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PMSFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "PMSFirebaseMsgService";
    private static final String CHANNEL_ID = "appointments";
    private static final String CHANNEL_NAME = "Appointments";

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed FCM token: " + token);
        
        // Save FCM token locally
        saveTokenToPrefs(token);
        
        // Send token to server if user is logged in
        if (isUserLoggedIn()) {
            sendRegistrationTokenToServer(token);
        }
    }
    
    private void saveTokenToPrefs(String token) {
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.KEY_FCM_TOKEN, token);
        editor.apply();
        Log.d(TAG, "FCM token saved to SharedPreferences");
    }
    
    private boolean isUserLoggedIn() {
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String token = sharedPreferences.getString(Constants.KEY_TOKEN, "");
        String userId = sharedPreferences.getString(Constants.KEY_USER_ID, "");
        return !token.isEmpty() && !userId.isEmpty();
    }
    
    public static void sendRegistrationTokenToServer(Context context, String token) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        String authToken = sharedPreferences.getString(Constants.KEY_TOKEN, "");
        
        if (authToken.isEmpty()) {
            Log.e(TAG, "Cannot send FCM token to server: user not authenticated");
            return;
        }
        
        sendTokenToServer(context, token, authToken);
    }

    private void sendRegistrationTokenToServer(String token) {
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String authToken = sharedPreferences.getString(Constants.KEY_TOKEN, "");
        
        if (authToken.isEmpty()) {
            Log.e(TAG, "Cannot send FCM token to server: user not authenticated");
            return;
        }
        
        sendTokenToServer(this, token, authToken);
    }
    
    private static void sendTokenToServer(Context context, String fcmToken, String authToken) {
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("fcmToken", fcmToken);
        
        apiService.updateFcmToken(tokenData, "Bearer " + authToken).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "FCM token sent to server successfully");
                } else {
                    try {
                        Log.e(TAG, "Failed to send FCM token to server: " + 
                            (response.errorBody() != null ? response.errorBody().string() : "Unknown error"));
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error response", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Log.e(TAG, "Network error when sending FCM token to server", t);
            }
        });
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        
        Map<String, String> data = remoteMessage.getData();
        Log.d(TAG, "Message data payload: " + data);

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            
            handleNotification(title, body, data);
        } else if (!data.isEmpty()) {
            // Handle data payload
            String title = data.get("title");
            String body = data.get("body");
            
            if (title != null && body != null) {
                handleNotification(title, body, data);
            }
        }
    }
    
    private void handleNotification(String title, String messageBody, Map<String, String> data) {
        String type = data.get("type");
        String appointmentId = data.get("appointmentId");
        
        Intent intent;
        int notificationPriority = NotificationCompat.PRIORITY_DEFAULT;
        
        if (type != null) {
            switch (type) {
                case "APPOINTMENT_REQUEST":
                    intent = new Intent(this, ManageAppointmentRequestsActivity.class);
                    notificationPriority = NotificationCompat.PRIORITY_HIGH;
                    break;
                case "FACULTY_APPOINTMENT_REQUEST":
                    intent = new Intent(this, ManageAppointmentRequestsActivity.class);
                    notificationPriority = NotificationCompat.PRIORITY_HIGH;
                    break;
                case "APPOINTMENT_UPDATED":
                    intent = new Intent(this, AppointmentDetailsActivity.class);
                    if (appointmentId != null) {
                        intent.putExtra("APPOINTMENT_ID", appointmentId);
                    }
                    break;
                case "APPOINTMENT_CANCELLED":
                    intent = new Intent(this, NotificationsActivity.class);
                    break;
                case "APPOINTMENT_REMINDER":
                    intent = new Intent(this, AppointmentDetailsActivity.class);
                    if (appointmentId != null) {
                        intent.putExtra("APPOINTMENT_ID", appointmentId);
                    }
                    notificationPriority = NotificationCompat.PRIORITY_HIGH;
                    break;
                default:
                    // Default to showing the notifications screen
                    intent = new Intent(this, NotificationsActivity.class);
                    break;
            }
        } else {
            // If no type specified, default to notifications screen
            intent = new Intent(this, NotificationsActivity.class);
        }
        
        // Add any additional data to the intent
        if (appointmentId != null) {
            intent.putExtra("APPOINTMENT_ID", appointmentId);
        }
        
        // Add all data values as extras
        for (Map.Entry<String, String> entry : data.entrySet()) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent,
            PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(notificationPriority)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Use appointmentId or current time as notification ID
        int notificationId;
        if (appointmentId != null) {
            // Use hashCode of appointmentId to create a consistent ID
            notificationId = appointmentId.hashCode();
        } else {
            // If no appointment ID, use current time
            notificationId = (int) System.currentTimeMillis();
        }
        
        notificationManager.notify(notificationId, notificationBuilder.build());
        
        // Also update the Firestore database to reflect new notifications
        // This will be picked up by the UI when the user opens the app
        storeNotificationInFirestore(title, messageBody, data);
    }
    
    private void storeNotificationInFirestore(String title, String body, Map<String, String> data) {
        // Get user ID
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String userId = prefs.getString(Constants.KEY_USER_ID, "");
        
        if (userId.isEmpty()) {
            Log.e(TAG, "Cannot store notification: user ID not found");
            return;
        }
        
        // Create a notification object to store in Firestore
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", title);
        notificationData.put("message", body);
        notificationData.put("recipientId", userId);
        notificationData.put("read", false);
        notificationData.put("createdAt", com.google.firebase.Timestamp.now());
        
        // Add appointment-related data if available
        String appointmentId = data.get("appointmentId");
        if (appointmentId != null) {
            notificationData.put("appointmentId", appointmentId);
        }
        
        String appointmentTitle = data.get("appointmentTitle");
        if (appointmentTitle != null) {
            notificationData.put("appointmentTitle", appointmentTitle);
        }
        
        String appointmentDate = data.get("startTime");
        if (appointmentDate != null) {
            notificationData.put("appointmentDate", appointmentDate);
        }
        
        // Store in Firestore
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("notifications")
            .add(notificationData)
            .addOnSuccessListener(documentReference -> 
                Log.d(TAG, "Notification stored in Firestore with ID: " + documentReference.getId()))
            .addOnFailureListener(e -> 
                Log.e(TAG, "Error storing notification in Firestore", e));
    }
} 