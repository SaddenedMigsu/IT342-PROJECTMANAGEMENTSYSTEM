package com.it342.projectmanagementsystem.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.activities.HomePage;
import com.it342.projectmanagementsystem.api.ApiService;
import com.it342.projectmanagementsystem.api.RetrofitClient;
import com.it342.projectmanagementsystem.utils.NotificationUtils;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "appointments";
    
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        
        // Handle notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            
            Log.d(TAG, "Message Notification Title: " + title);
            Log.d(TAG, "Message Notification Body: " + body);
            
            // Handle data payload
            Map<String, String> data = remoteMessage.getData();
            String appointmentId = data.get("appointmentId");
            String type = data.get("type");
            
            Log.d(TAG, "Message Data appointmentId: " + appointmentId);
            Log.d(TAG, "Message Data type: " + type);
            
            // Show notification
            showNotification(title, body, appointmentId, type);
        }
    }

    private void showNotification(String title, String body, String appointmentId, String type) {
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Appointments",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Appointment notifications");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        // Create intent based on notification type
        Intent intent = new Intent(this, HomePage.class);
        intent.putExtra("appointmentId", appointmentId);
        intent.putExtra("type", type);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Make sure you have this icon
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        // Show the notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationId = (int) System.currentTimeMillis(); // Unique ID for each notification
        manager.notify(notificationId, builder.build());
        
        // Save notification to local storage
        NotificationUtils.saveNotification(this, title, body, appointmentId, type);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        
        // Save token locally
        NotificationUtils.saveFCMToken(this, token);
        
        // Send token to server
        sendTokenToServer(token);
    }

    private void sendTokenToServer(String token) {
        // Get authentication token
        String authToken = NotificationUtils.getAuthToken(this);
        if (authToken == null || authToken.isEmpty()) {
            Log.e(TAG, "Cannot update FCM token: Not authenticated");
            return;
        }
        
        // Send token to server using Retrofit
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        
        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("fcmToken", token);
        
        apiService.updateFcmToken(tokenData, "Bearer " + authToken)
            .enqueue(new retrofit2.Callback<Map<String, String>>() {
                @Override
                public void onResponse(retrofit2.Call<Map<String, String>> call, retrofit2.Response<Map<String, String>> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "FCM token updated successfully on server");
                    } else {
                        Log.e(TAG, "Failed to update FCM token on server: " + response.code());
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<Map<String, String>> call, Throwable t) {
                    Log.e(TAG, "Error updating FCM token on server", t);
                }
            });
    }
} 