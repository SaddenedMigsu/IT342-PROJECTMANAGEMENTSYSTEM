package com.it342.projectmanagementsystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.api.RetrofitClient;
import com.it342.projectmanagementsystem.api.ApiService;
import com.it342.projectmanagementsystem.models.RegisterRequest;
import com.it342.projectmanagementsystem.models.RegisterResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private TextInputLayout tilRole, tilFirstName, tilLastName, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etFirstName, etLastName, etEmail, etPassword, etConfirmPassword;
    private AutoCompleteTextView actvRole;
    private MaterialButton btnRegister;
    private View tvLoginLink;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();
        setupRoleSpinner();
        setupClickListeners();
        initializeApiService();
    }

    private void initializeViews() {
        tilRole = findViewById(R.id.tilRole);
        tilFirstName = findViewById(R.id.tilFirstName);
        tilLastName = findViewById(R.id.tilLastName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        actvRole = findViewById(R.id.actvRole);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);
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
        btnRegister.setOnClickListener(v -> handleRegistration());
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginFormActivity.class));
            finish();
        });
    }

    private void initializeApiService() {
        apiService = RetrofitClient.getInstance().getApiService();
    }

    private void handleRegistration() {
        // Reset all error states
        tilRole.setError(null);
        tilFirstName.setError(null);
        tilLastName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        // Get input values
        String role = actvRole.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        // Validate inputs
        if (!validateInputs(role, firstName, lastName, email, password, confirmPassword)) {
            return;
        }

        // Create registration request
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFirstName(firstName);
        registerRequest.setLastName(lastName);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);
        registerRequest.setRole(role.toLowerCase());

        // Show loading state
        setLoadingState(true);

        // Make API call
        Call<RegisterResponse> call = apiService.register(registerRequest);
        call.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                setLoadingState(false);
                if (response.isSuccessful() && response.body() != null) {
                    handleSuccessfulRegistration();
                } else {
                    handleRegistrationError("Registration failed. Please try again.");
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                setLoadingState(false);
                handleRegistrationError("Network error. Please check your connection.");
            }
        });
    }

    private boolean validateInputs(String role, String firstName, String lastName, 
                                 String email, String password, String confirmPassword) {
        boolean isValid = true;

        if (TextUtils.isEmpty(role)) {
            tilRole.setError("Please select a role");
            isValid = false;
        }

        if (TextUtils.isEmpty(firstName)) {
            tilFirstName.setError("First name is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(lastName)) {
            tilLastName.setError("Last name is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Please confirm your password");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            isValid = false;
        }

        return isValid;
    }

    private void setLoadingState(boolean isLoading) {
        btnRegister.setEnabled(!isLoading);
        btnRegister.setText(isLoading ? "Registering..." : "Register");
    }

    private void handleSuccessfulRegistration() {
        Toast.makeText(this, "Registration successful! Please login.", Toast.LENGTH_LONG).show();
        startActivity(new Intent(RegisterActivity.this, LoginFormActivity.class));
        finish();
    }

    private void handleRegistrationError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }
} 