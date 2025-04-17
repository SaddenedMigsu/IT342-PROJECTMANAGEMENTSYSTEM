package com.it342.projectmanagementsystem.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.api.RetrofitClient;
import com.it342.projectmanagementsystem.api.ApiService;
import com.it342.projectmanagementsystem.models.RegisterRequest;
import com.it342.projectmanagementsystem.models.RegisterResponse;
import com.it342.projectmanagementsystem.LoginActivity;
import com.it342.projectmanagementsystem.activities.HomePage;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";
    private TextInputLayout tilRole, tilFirstName, tilLastName, tilEmail, tilPassword, tilConfirmPassword, tilCourse;
    private TextInputEditText etFirstName, etLastName, etEmail, etPassword, etConfirmPassword, etCourse;
    private AutoCompleteTextView actvRole;
    private MaterialButton btnRegister;
    private View tvLoginLink;
    private ProgressBar progressBar;
    private ApiService apiService;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();
        setupRoleSpinner();
        setupClickListeners();
        initializeApiService();
        mAuth = FirebaseAuth.getInstance();
    }

    private void initializeViews() {
        tilRole = findViewById(R.id.tilRole);
        tilFirstName = findViewById(R.id.tilFirstName);
        tilLastName = findViewById(R.id.tilLastName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        tilCourse = findViewById(R.id.tilCourse);

        actvRole = findViewById(R.id.actvRole);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etCourse = findViewById(R.id.etCourse);

        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRoleSpinner() {
        String[] roles = new String[]{"Student", "Faculty"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                roles
        );
        actvRole.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> handleRegister());
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void initializeApiService() {
        apiService = RetrofitClient.getInstance().getApiService();
    }

    private void handleRegister() {
        // Get values from fields
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String role = actvRole.getText().toString().trim().toUpperCase(); // Get selected role
        String course = etCourse.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(firstName, lastName, email, password, confirmPassword, role)) {
            return;
        }

        // Show loading
        progressBar.setVisibility(View.VISIBLE);

        // 1. Create user in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Firebase user created successfully.");
                    // 2. If Firebase user created, call backend API
                    registerUserInBackend(firstName, lastName, email, password, role, course);
                } else {
                    // Firebase user creation failed
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Firebase user creation failed", task.getException());
                    String errorMessage = "Registration failed.";
                    if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                        errorMessage = "This email address is already registered.";
                    } else if (task.getException() != null) {
                        errorMessage += " Error: " + task.getException().getMessage();
                    }
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
    }

    private void registerUserInBackend(String firstName, String lastName, String email, 
                                     String password, String role, String course) {
        // Create register request for backend
        RegisterRequest registerRequest = new RegisterRequest(
            firstName,
            lastName,
            email,
            password,
            role,
            course
        );

        // Make backend API call
        Call<RegisterResponse> call = apiService.register(registerRequest);
        call.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    // Backend registration successful
                    Log.d(TAG, "Backend registration successful.");
                    Toast.makeText(RegisterActivity.this, 
                        "Registration successful! Please log in.", Toast.LENGTH_LONG).show();
                    
                    // Navigate to LoginFormActivity with success message
                    Intent intent = new Intent(RegisterActivity.this, LoginFormActivity.class);
                    intent.putExtra("REGISTRATION_SUCCESS", "Account created successfully!");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
                    startActivity(intent);
                    finish(); // Finish RegisterActivity
                } else {
                    // Backend registration failed
                    try {
                        String errorBody = response.errorBody() != null ? 
                            response.errorBody().string() : "Unknown backend error";
                        Log.e(TAG, "Backend registration failed: " + errorBody);
                        Toast.makeText(RegisterActivity.this,
                            "Registration failed (backend): " + errorBody, Toast.LENGTH_LONG).show();
                        // Optional: Consider deleting the Firebase user here if backend fails
                        // mAuth.getCurrentUser().delete(); 
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading backend error body", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                // Network or other backend call failure
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Backend network error: " + t.getMessage(), t);
                Toast.makeText(RegisterActivity.this,
                    "Network error during registration: " + t.getMessage(), Toast.LENGTH_LONG).show();
                 // Optional: Consider deleting the Firebase user here
            }
        });
    }

    private boolean validateInputs(String firstName, String lastName, String email, 
                                 String password, String confirmPassword, String role) {
        if (firstName.isEmpty()) {
            etFirstName.setError("First name is required");
            return false;
        }
        if (lastName.isEmpty()) {
            etLastName.setError("Last name is required");
            return false;
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email is required");
            return false;
        }
        if (password.isEmpty() || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return false;
        }
        if (role.isEmpty()) {
            actvRole.setError("Please select a role");
            return false;
        }
        if (!role.equals("STUDENT") && !role.equals("FACULTY")) {
            actvRole.setError("Invalid role selected");
            return false;
        }
        return true;
    }
} 