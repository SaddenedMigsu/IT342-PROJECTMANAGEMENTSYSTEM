package com.it342.projectmanagementsystem.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.messaging.FirebaseMessaging;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.api.ApiService;
import com.it342.projectmanagementsystem.api.RetrofitClient;
import com.it342.projectmanagementsystem.models.LoginRequest;
import com.it342.projectmanagementsystem.models.AuthResponse;
import com.it342.projectmanagementsystem.utils.Constants;
import java.io.IOException;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private EditText etEmail, etPassword;
    private Button btnLogin, btnCreateAccount;
    private ProgressBar progressBar;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        apiService = RetrofitClient.getInstance().getApiService();
        setupClickListeners();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        // Set up Login button click listener
        btnLogin.setOnClickListener(v -> loginUser());

        // Set up Create Account button click listener
        btnCreateAccount.setOnClickListener(v -> {
            Log.d(TAG, "Create Account button clicked");
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        LoginRequest loginRequest = new LoginRequest(email, password);
        Call<AuthResponse> call = apiService.login(loginRequest);

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getRole() != null && response.body().getToken() != null) {
                        // Login successful, save user data and navigate
                        saveUserData(response.body());
                        
                        // Navigate based on role
                        String userRole = response.body().getRole();
                        navigateToHomePage(userRole);
                        
                    } else {
                        Toast.makeText(LoginActivity.this, "Invalid login data in response", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMessage = "Login failed";
                    if (response.errorBody() != null) {
                        try {
                            errorMessage = response.errorBody().string();
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            } 

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Login API call failed", t);
                Toast.makeText(LoginActivity.this, "Login failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserData(AuthResponse authResponse) {
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("token", authResponse.getToken());
        editor.putString("userId", authResponse.getUserId());
        editor.putString("email", authResponse.getEmail());
        editor.putString("firstName", authResponse.getFirstName());
        editor.putString("lastName", authResponse.getLastName());
        editor.putString("role", authResponse.getRole());
        editor.apply();
        Log.i(TAG, "User data saved successfully. Role: " + authResponse.getRole());
        
        // Send FCM token to server after successful login
        sendFCMTokenToServer();
    }
    
    private void sendFCMTokenToServer() {
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String fcmToken = prefs.getString(Constants.KEY_FCM_TOKEN, "");
        
        if (!fcmToken.isEmpty()) {
            // We have a token, send it to server
            com.it342.projectmanagementsystem.services.PMSFirebaseMessagingService.sendRegistrationTokenToServer(this, fcmToken);
        } else {
            // No token available, try to get one
            FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        
                        // Save token locally
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(Constants.KEY_FCM_TOKEN, token);
                        editor.apply();
                        
                        // Send to server
                        com.it342.projectmanagementsystem.services.PMSFirebaseMessagingService.sendRegistrationTokenToServer(this, token);
                    } else {
                        Log.e(TAG, "Failed to get FCM token", task.getException());
                    }
                });
        }
    }

    private void navigateToHomePage(String userRole) {
        Log.d(TAG, "Navigating based on role: " + userRole);
        Intent intent;
        if ("FACULTY".equalsIgnoreCase(userRole)) {
            // Navigate to Faculty Dashboard
            intent = new Intent(LoginActivity.this, FacultyDashboardActivity.class); 
            Log.i(TAG, "Navigating to Faculty Dashboard");
        } else {
            // Navigate to Student Home Page (or default)
            intent = new Intent(LoginActivity.this, HomePage.class);
            Log.i(TAG, "Navigating to Student/Default Home Page");
        }
        startActivity(intent);
        finish(); // Close LoginActivity
    }
} 