package com.it342.projectmanagementsystem;

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
import com.it342.projectmanagementsystem.api.ApiService;
import com.it342.projectmanagementsystem.api.RetrofitClient;
import com.it342.projectmanagementsystem.models.AuthResponse;
import com.it342.projectmanagementsystem.models.LoginRequest;
import com.google.firebase.auth.FirebaseAuth;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.IOException;

public class LoginFormActivity extends AppCompatActivity {

    private static final String TAG = "LoginFormActivity";
    private EditText etEmail, etPassword;
    private Button btnSignIn;
    private TextView tvForgotPassword;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_form);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if there's a success message from registration
        if (getIntent().hasExtra("REGISTRATION_SUCCESS")) {
            String message = getIntent().getStringExtra("REGISTRATION_SUCCESS");
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }

        // Initialize views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);

        // Set click listeners
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInputs()) {
                    attemptLogin();
                }
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    mAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(LoginFormActivity.this, 
                                        "Password reset email sent", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(LoginFormActivity.this, 
                                        "Failed to send reset email", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    Toast.makeText(LoginFormActivity.this, 
                        "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        Log.d(TAG, "Activity created. Will connect to Firebase Auth");
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInputs()) {
            return;
        }

        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);

        // Attempt Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success");
                        
                        // Create login request for backend sync
                        LoginRequest loginRequest = new LoginRequest(email, password);
                        syncWithBackend(loginRequest);
                    } else {
                        // If sign in fails, display a message to the user.
                        progressBar.setVisibility(View.GONE);
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginFormActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void syncWithBackend(LoginRequest loginRequest) {
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        Call<AuthResponse> call = apiService.login(loginRequest);

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    Log.d(TAG, "Backend sync successful!");

                    // Save auth token and user info
                    SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("token", authResponse.getToken());
                    editor.putString("userId", authResponse.getUserId());
                    editor.putString("studId", authResponse.getStudId());
                    editor.putString("email", authResponse.getEmail());
                    editor.apply();

                    // Navigate to HomePage with clear task flag
                    Intent intent = new Intent(LoginFormActivity.this, HomePage.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Backend sync error: " + errorBody);
                            // Don't show backend sync errors to user since Firebase auth was successful
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    // Still proceed to HomePage even if backend sync fails
                    Intent intent = new Intent(LoginFormActivity.this, HomePage.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Network error during backend sync", t);
                // Still proceed to HomePage even if backend sync fails
                Intent intent = new Intent(LoginFormActivity.this, HomePage.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
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