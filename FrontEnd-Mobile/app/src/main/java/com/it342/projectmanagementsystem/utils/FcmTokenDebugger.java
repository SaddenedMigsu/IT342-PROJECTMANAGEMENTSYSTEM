package com.it342.projectmanagementsystem.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.it342.projectmanagementsystem.api.ApiService;
import com.it342.projectmanagementsystem.api.RetrofitClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Utility class to help debug FCM token update issues
 */
public class FcmTokenDebugger {
    private static final String TAG = "FcmTokenDebugger";
    private static final String AUTH_PREFS = "AuthPrefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USER_ID = "userId";

    /**
     * Test FCM token update with various approaches
     */
    public static void testFcmTokenUpdate(Context context) {
        // Get authentication data
        SharedPreferences prefs = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE);
        String authToken = prefs.getString(KEY_TOKEN, "");
        String email = prefs.getString(KEY_EMAIL, "");
        String userId = prefs.getString(KEY_USER_ID, "");
        String fcmToken = NotificationUtils.getFCMToken(context);
        
        if (authToken.isEmpty()) {
            Log.e(TAG, "Cannot test FCM token update: Not authenticated");
            return;
        }
        
        if (fcmToken == null || fcmToken.isEmpty()) {
            Log.e(TAG, "Cannot test FCM token update: FCM token not available");
            return;
        }
        
        Log.d(TAG, "Testing FCM token update with:");
        Log.d(TAG, "Auth Token: " + authToken.substring(0, 10) + "...");
        Log.d(TAG, "Email: " + email);
        Log.d(TAG, "User ID: " + userId);
        Log.d(TAG, "FCM Token: " + fcmToken.substring(0, 10) + "...");
        
        // Get API service
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        
        // Test with userId in payload
        Map<String, String> tokenData1 = new HashMap<>();
        tokenData1.put("fcmToken", fcmToken);
        tokenData1.put("userId", userId);
        
        Log.d(TAG, "Test 1: With userId in payload");
        apiService.updateFcmToken(tokenData1, "Bearer " + authToken)
            .enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Test 1: Success");
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                            Log.e(TAG, "Test 1: Failed - " + response.code() + " - " + errorBody);
                        } catch (IOException e) {
                            Log.e(TAG, "Test 1: Failed - " + response.code());
                        }
                    }
                    
                    // Test with email in payload
                    testWithEmail(apiService, fcmToken, email, authToken);
                }

                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    Log.e(TAG, "Test 1: Network error", t);
                    // Test with email in payload
                    testWithEmail(apiService, fcmToken, email, authToken);
                }
            });
    }
    
    private static void testWithEmail(ApiService apiService, String fcmToken, String email, String authToken) {
        Map<String, String> tokenData2 = new HashMap<>();
        tokenData2.put("fcmToken", fcmToken);
        tokenData2.put("email", email);
        
        Log.d(TAG, "Test 2: With email in payload");
        apiService.updateFcmToken(tokenData2, "Bearer " + authToken)
            .enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Test 2: Success");
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                            Log.e(TAG, "Test 2: Failed - " + response.code() + " - " + errorBody);
                        } catch (IOException e) {
                            Log.e(TAG, "Test 2: Failed - " + response.code());
                        }
                    }
                    
                    // Test with just fcmToken
                    testWithJustToken(apiService, fcmToken, authToken);
                }

                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    Log.e(TAG, "Test 2: Network error", t);
                    // Test with just fcmToken
                    testWithJustToken(apiService, fcmToken, authToken);
                }
            });
    }
    
    private static void testWithJustToken(ApiService apiService, String fcmToken, String authToken) {
        Map<String, String> tokenData3 = new HashMap<>();
        tokenData3.put("fcmToken", fcmToken);
        
        Log.d(TAG, "Test 3: With just fcmToken");
        apiService.updateFcmToken(tokenData3, "Bearer " + authToken)
            .enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Test 3: Success");
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                            Log.e(TAG, "Test 3: Failed - " + response.code() + " - " + errorBody);
                        } catch (IOException e) {
                            Log.e(TAG, "Test 3: Failed - " + response.code());
                        }
                    }
                }

                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    Log.e(TAG, "Test 3: Network error", t);
                }
            });
    }
} 