package com.it342.projectmanagementsystem.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.messaging.FirebaseMessaging;
import com.it342.projectmanagementsystem.PMSApplication;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.api.ApiService;
import com.it342.projectmanagementsystem.api.RetrofitClient;
import com.it342.projectmanagementsystem.models.AuthResponse;
import com.it342.projectmanagementsystem.models.LoginRequest;
import com.it342.projectmanagementsystem.utils.Constants;
import com.it342.projectmanagementsystem.utils.FcmTokenDebugger;
import com.it342.projectmanagementsystem.utils.NotificationUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.IOException;

public class LoginFormActivity extends AppCompatActivity {

    private static final String TAG = "LoginFormActivity";
    private EditText etEmail, etPassword;
    private Button btnSignIn;
    private ProgressBar progressBar;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_form);

        // Check if there's a success message from registration
        if (getIntent().hasExtra("REGISTRATION_SUCCESS")) {
            String message = getIntent().getStringExtra("REGISTRATION_SUCCESS");
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }

        // Initialize views
        initializeViews();
        // Initialize ApiService
        apiService = RetrofitClient.getInstance().getApiService();

        // Set click listeners
        btnSignIn.setOnClickListener(v -> attemptLoginWithApi());
        
        // Request notification permission for Android 13+
        PMSApplication.requestNotificationPermission(this);
        
        Log.d(TAG, "Activity created. Using Custom API for login.");
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        progressBar = findViewById(R.id.progressBar);
    }
    
    private void attemptLoginWithApi() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInputs()) {
            Log.d(TAG, "Input validation failed");
            return;
        }

        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "Progress bar shown, attempting API authentication");

        Log.d(TAG, "Attempting to sign in via API with email: " + email);
        
        // Create request for backend
        LoginRequest loginRequest = new LoginRequest(email, password);
        Log.d(TAG, "Created login request - identifier: " + email);
        
        // Call the backend API directly
        Call<AuthResponse> call = apiService.login(loginRequest);
        Log.d(TAG, "API call created, attempting to execute with URL: " + call.request().url());

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                progressBar.setVisibility(View.GONE);
                Log.d(TAG, "Received response from backend. Code: " + response.code());
                Log.d(TAG, "Response headers: " + response.headers());
                
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    // Check for role and token directly within AuthResponse
                    if (authResponse.getRole() != null && authResponse.getToken() != null) { 
                        Log.d(TAG, "Backend login successful!");
                        Log.d(TAG, "Role: " + authResponse.getRole());
                        // Save auth token and user info
                        saveUserData(authResponse);
                        // Get and send FCM token to server
                        retrieveAndSendFCMToken();
                        // Navigate based on role
                        navigateToHomePage(authResponse.getRole());
                    } else {
                        Log.e(TAG, "Backend login failed: Invalid data in response");
                        Toast.makeText(LoginFormActivity.this, "Login failed: Invalid response data", Toast.LENGTH_LONG).show();
                    }

                } else {
                    String errorMessage = "Login failed.";
                     try {
                        if (response.errorBody() != null) {
                            errorMessage = response.errorBody().string(); 
                            Log.e(TAG, "Backend login error: " + errorMessage);
                        } else {
                            errorMessage += " (Code: " + response.code() + ")";
                            Log.e(TAG, "Backend login failed with code: " + response.code());
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Toast.makeText(LoginFormActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Network error during API login", t);
                Toast.makeText(LoginFormActivity.this, "Login failed: Network error - " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveUserData(AuthResponse authResponse) { 
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.KEY_TOKEN, authResponse.getToken());
        editor.putString(Constants.KEY_USER_ID, authResponse.getUserId()); 
        editor.putString(Constants.KEY_STUD_ID, authResponse.getStudId()); // Assuming studId might be needed
        editor.putString(Constants.KEY_EMAIL, authResponse.getEmail());
        editor.putString(Constants.KEY_FIRST_NAME, authResponse.getFirstName());
        editor.putString(Constants.KEY_LAST_NAME, authResponse.getLastName());
        editor.putString(Constants.KEY_ROLE, authResponse.getRole());
        editor.apply();
        Log.i(TAG, "User data saved successfully. Role: " + authResponse.getRole());
    }
    
    private void retrieveAndSendFCMToken() {
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
                NotificationUtils.saveFCMToken(this, token);
                
                // Test FCM token update with various approaches
                FcmTokenDebugger.testFcmTokenUpdate(this);
                
                // Send token to server using the normal method
                NotificationUtils.sendFCMTokenToServer(this);
            });
    }

    private void navigateToHomePage(String userRole) {
        Log.d(TAG, "Navigating based on role: " + userRole);
        Intent intent;
        if ("FACULTY".equalsIgnoreCase(userRole)) {
            intent = new Intent(LoginFormActivity.this, FacultyDashboardActivity.class); 
            Log.i(TAG, "Navigating to Faculty Dashboard");
        } else {
            intent = new Intent(LoginFormActivity.this, HomePage.class);
            Log.i(TAG, "Navigating to Student/Default Home Page");
        }
        // Use clear task flags to prevent returning to login
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); 
        startActivity(intent);
        finish(); // Close LoginFormActivity
    }

    private boolean validateInputs() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        boolean isValid = true;

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            isValid = false;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            isValid = false;
        }

        return isValid;
    }
} 