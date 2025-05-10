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
import com.it342.projectmanagementsystem.utils.NotificationHelper;
import com.it342.projectmanagementsystem.utils.NotificationUtils;

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
        
        // Also save in NotificationUtils for consistency
        NotificationUtils.saveFCMToken(this, token);
        
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
        try {
            ApiService apiService = RetrofitClient.getInstance().getApiService();
            Map<String, String> tokenData = new HashMap<>();
            tokenData.put("fcmToken", fcmToken);
            
            // Add user email for debugging
            SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
            String email = sharedPreferences.getString(Constants.KEY_EMAIL, "");
            if (!email.isEmpty()) {
                tokenData.put("email", email);
            }
            
            Log.d(TAG, "Sending FCM token to server: " + fcmToken);
            Log.d(TAG, "User email: " + (email.isEmpty() ? "unknown" : email));
            
            apiService.updateFcmToken(tokenData, "Bearer " + authToken).enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "FCM token sent to server successfully");
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                            Log.e(TAG, "Failed to send FCM token to server: " + errorBody +
                                " (Status code: " + response.code() + ")");
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing error response", e);
                        }
                    }
                }

                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    Log.e(TAG, "Network error when sending FCM token to server: " + t.getMessage(), t);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception when sending FCM token to server: " + e.getMessage(), e);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        
        Map<String, String> data = remoteMessage.getData();
        Log.d(TAG, "Message data payload: " + data);

        // Extract notification data
        String title = null;
        String body = null;
        
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }
        
        if ((title == null || body == null) && data != null) {
            // Try to get title and body from data payload
            title = data.get("title");
            body = data.get("body");
        }
        
        if (title == null || body == null) {
            Log.e(TAG, "Cannot show notification: missing title or body");
            return;
        }
        
        // Get other data
        String appointmentId = data != null ? data.get("appointmentId") : null;
        String type = data != null ? data.get("type") : null;
        
        // Show notification
        showNotification(title, body, appointmentId, type);
        
        // Store notification in Firestore for persistence
        storeNotificationInFirestore(title, body, data);
    }
    
    private void showNotification(String title, String body, String appointmentId, String type) {
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Appointment notifications");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        // Create intent based on notification type
        Intent intent;
        if (type != null && type.contains("APPOINTMENT_REQUEST")) {
            intent = new Intent(this, ManageAppointmentRequestsActivity.class);
        } else if (appointmentId != null) {
            intent = new Intent(this, AppointmentDetailsActivity.class);
            intent.putExtra("appointmentId", appointmentId);
        } else {
            intent = new Intent(this, NotificationsActivity.class);
        }
        
        // Add notification type if available
        if (type != null) {
            intent.putExtra("type", type);
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        // Show the notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationId = appointmentId != null ? appointmentId.hashCode() : (int) System.currentTimeMillis();
        manager.notify(notificationId, builder.build());
        
        // Save notification to local storage
        NotificationUtils.saveNotification(this, title, body, appointmentId, type);
    }
    
    private void storeNotificationInFirestore(String title, String body, Map<String, String> data) {
        // Get user ID
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String userId = prefs.getString(Constants.KEY_USER_ID, "");
        
        if (userId.isEmpty()) {
            Log.e(TAG, "Cannot store notification: user ID not found");
            return;
        }
        
        // Create notification data
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", title);
        notificationData.put("message", body);
        notificationData.put("recipientId", userId);
        notificationData.put("read", false);
        notificationData.put("createdAt", com.google.firebase.Timestamp.now());
        
        // Add appointment ID if available
        String appointmentId = data.get("appointmentId");
        if (appointmentId != null) {
            notificationData.put("appointmentId", appointmentId);
        }
        
        // Add notification type if available
        String type = data.get("type");
        if (type != null) {
            notificationData.put("type", type);
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