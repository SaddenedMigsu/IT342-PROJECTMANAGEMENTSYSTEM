package com.it342.projectmanagementsystem.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationUtils {
    private static final String TAG = "NotificationUtils";
    private static final String PREF_NAME = "NotificationPrefs";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final String AUTH_PREFS = "AuthPrefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_EMAIL = "email";

    /**
     * Save FCM token to SharedPreferences
     */
    public static void saveFCMToken(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
        Log.d(TAG, "FCM token saved locally: " + token);
    }

    /**
     * Get FCM token from SharedPreferences
     */
    public static String getFCMToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_FCM_TOKEN, null);
    }
    
    /**
     * Get authentication token from SharedPreferences
     */
    public static String getAuthToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }

    /**
     * Get user email from SharedPreferences
     */
    public static String getUserEmail(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_EMAIL, null);
    }

    /**
     * Send FCM token to server
     */
    public static void sendFCMTokenToServer(Context context) {
        String token = getFCMToken(context);
        if (token == null || token.isEmpty()) {
            // If no token is saved, request a new one
            requestAndUpdateFCMToken(context);
            return;
        }
        
        String authToken = getAuthToken(context);
        if (authToken == null || authToken.isEmpty()) {
            Log.w(TAG, "Cannot send FCM token to server: Not authenticated");
            return;
        }
        
        // Log user email for debugging
        String userEmail = getUserEmail(context);
        Log.d(TAG, "Sending FCM token for user: " + (userEmail != null ? userEmail : "unknown"));
        
        // Send token to server
        com.it342.projectmanagementsystem.api.ApiService apiService = 
            com.it342.projectmanagementsystem.api.RetrofitClient.getInstance().getApiService();
        
        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("fcmToken", token);
        
        // Add user email to request for debugging
        if (userEmail != null) {
            tokenData.put("email", userEmail);
        }
        
        Log.d(TAG, "Sending FCM token update request: " + tokenData);
        
        apiService.updateFcmToken(tokenData, "Bearer " + authToken)
            .enqueue(new retrofit2.Callback<Map<String, String>>() {
                @Override
                public void onResponse(retrofit2.Call<Map<String, String>> call, 
                                    retrofit2.Response<Map<String, String>> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "FCM token updated successfully on server");
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                            Log.e(TAG, "Failed to update FCM token on server: " + response.code() + 
                                   " - " + errorBody);
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to update FCM token on server: " + response.code());
                        }
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<Map<String, String>> call, Throwable t) {
                    Log.e(TAG, "Error updating FCM token on server", t);
                }
            });
    }

    /**
     * Save notification to SharedPreferences
     */
    public static void saveNotification(Context context, String title, String body, 
                                        String appointmentId, String type) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String notificationsJson = prefs.getString(KEY_NOTIFICATIONS, "[]");
            JSONArray notifications = new JSONArray(notificationsJson);
            
            // Create new notification object
            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("body", body);
            notification.put("appointmentId", appointmentId);
            notification.put("type", type);
            notification.put("timestamp", new Date().getTime());
            notification.put("read", false);
            
            // Add to array
            notifications.put(notification);
            
            // Save back to SharedPreferences
            prefs.edit().putString(KEY_NOTIFICATIONS, notifications.toString()).apply();
            Log.d(TAG, "Notification saved locally: " + title);
        } catch (JSONException e) {
            Log.e(TAG, "Error saving notification", e);
        }
    }

    /**
     * Get all notifications from SharedPreferences
     */
    public static List<NotificationItem> getNotifications(Context context) {
        List<NotificationItem> notificationList = new ArrayList<>();
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String notificationsJson = prefs.getString(KEY_NOTIFICATIONS, "[]");
            JSONArray notifications = new JSONArray(notificationsJson);
            
            for (int i = 0; i < notifications.length(); i++) {
                JSONObject notification = notifications.getJSONObject(i);
                NotificationItem item = new NotificationItem(
                    notification.getString("title"),
                    notification.getString("body"),
                    notification.optString("appointmentId", ""),
                    notification.optString("type", ""),
                    notification.getLong("timestamp"),
                    notification.getBoolean("read")
                );
                notificationList.add(item);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error getting notifications", e);
        }
        return notificationList;
    }
    
    /**
     * Mark notification as read
     */
    public static void markNotificationAsRead(Context context, int position) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String notificationsJson = prefs.getString(KEY_NOTIFICATIONS, "[]");
            JSONArray notifications = new JSONArray(notificationsJson);
            
            if (position >= 0 && position < notifications.length()) {
                JSONObject notification = notifications.getJSONObject(position);
                notification.put("read", true);
                notifications.put(position, notification);
                
                // Save back to SharedPreferences
                prefs.edit().putString(KEY_NOTIFICATIONS, notifications.toString()).apply();
                Log.d(TAG, "Notification marked as read at position: " + position);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error marking notification as read", e);
        }
    }
    
    /**
     * Get unread notification count
     */
    public static int getUnreadNotificationCount(Context context) {
        int count = 0;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String notificationsJson = prefs.getString(KEY_NOTIFICATIONS, "[]");
            JSONArray notifications = new JSONArray(notificationsJson);
            
            for (int i = 0; i < notifications.length(); i++) {
                JSONObject notification = notifications.getJSONObject(i);
                if (!notification.getBoolean("read")) {
                    count++;
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error getting unread notification count", e);
        }
        return count;
    }
    
    /**
     * Request FCM token and send to server
     */
    public static void requestAndUpdateFCMToken(Context context) {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    return;
                }
                
                // Get new FCM registration token
                String token = task.getResult();
                
                // Save token locally
                saveFCMToken(context, token);
                
                // Get auth token
                String authToken = getAuthToken(context);
                if (authToken != null && !authToken.isEmpty()) {
                    // Send token to server using Retrofit
                    com.it342.projectmanagementsystem.api.ApiService apiService = 
                        com.it342.projectmanagementsystem.api.RetrofitClient.getInstance().getApiService();
                    
                    Map<String, String> tokenData = new HashMap<>();
                    tokenData.put("fcmToken", token);
                    
                    // Add user email to request for debugging
                    String userEmail = getUserEmail(context);
                    if (userEmail != null) {
                        tokenData.put("email", userEmail);
                        Log.d(TAG, "Sending FCM token for user: " + userEmail);
                    }
                    
                    Log.d(TAG, "Sending FCM token update request: " + tokenData);
                    
                    apiService.updateFcmToken(tokenData, "Bearer " + authToken)
                        .enqueue(new retrofit2.Callback<Map<String, String>>() {
                            @Override
                            public void onResponse(retrofit2.Call<Map<String, String>> call, 
                                                retrofit2.Response<Map<String, String>> response) {
                                if (response.isSuccessful()) {
                                    Log.d(TAG, "FCM token updated successfully on server");
                                } else {
                                    try {
                                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                                        Log.e(TAG, "Failed to update FCM token on server: " + response.code() + 
                                               " - " + errorBody);
                                    } catch (IOException e) {
                                        Log.e(TAG, "Failed to update FCM token on server: " + response.code());
                                    }
                                }
                            }

                            @Override
                            public void onFailure(retrofit2.Call<Map<String, String>> call, Throwable t) {
                                Log.e(TAG, "Error updating FCM token on server", t);
                            }
                        });
                } else {
                    Log.w(TAG, "Cannot send FCM token to server: Not authenticated");
                }
            });
    }
    
    /**
     * Clear all notifications
     */
    public static void clearNotifications(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_NOTIFICATIONS, "[]").apply();
        Log.d(TAG, "All notifications cleared");
    }
    
    /**
     * Notification item class for easier handling
     */
    public static class NotificationItem {
        private String title;
        private String body;
        private String appointmentId;
        private String type;
        private long timestamp;
        private boolean read;
        
        public NotificationItem(String title, String body, String appointmentId, 
                               String type, long timestamp, boolean read) {
            this.title = title;
            this.body = body;
            this.appointmentId = appointmentId;
            this.type = type;
            this.timestamp = timestamp;
            this.read = read;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getBody() {
            return body;
        }
        
        public String getAppointmentId() {
            return appointmentId;
        }
        
        public String getType() {
            return type;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public boolean isRead() {
            return read;
        }
        
        public void setRead(boolean read) {
            this.read = read;
        }
    }
} 