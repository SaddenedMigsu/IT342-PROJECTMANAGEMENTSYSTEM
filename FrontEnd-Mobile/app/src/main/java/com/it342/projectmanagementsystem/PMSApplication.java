package com.it342.projectmanagementsystem;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.it342.projectmanagementsystem.api.RetrofitClient;
import com.it342.projectmanagementsystem.services.PMSFirebaseMessagingService;
import com.it342.projectmanagementsystem.utils.Constants;

public class PMSApplication extends Application {
    private static final String TAG = "PMSApplication";
    private static Context appContext;
    
    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        
        // Initialize RetrofitClient
        RetrofitClient.init(this);
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        
        // Get FCM token
        retrieveAndStoreFCMToken();
    }
    
    public static Context getAppContext() {
        return appContext;
    }
    
    private void retrieveAndStoreFCMToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();
                Log.d(TAG, "FCM Token: " + token);
                
                // Save token locally
                SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(Constants.KEY_FCM_TOKEN, token);
                editor.apply();
                
                // Check if user is logged in, then send token to server
                String authToken = prefs.getString(Constants.KEY_TOKEN, "");
                if (!authToken.isEmpty()) {
                    // User is logged in, send token to server
                    PMSFirebaseMessagingService.sendRegistrationTokenToServer(this, token);
                }
            });
    }
    
    /**
     * Request notification permission for Android 13+
     * This should be called from the active activity
     */
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity, 
                    Manifest.permission.POST_NOTIFICATIONS) != 
                    PackageManager.PERMISSION_GRANTED) {
                
                // Request permission
                ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    100);
            }
        }
    }
} 