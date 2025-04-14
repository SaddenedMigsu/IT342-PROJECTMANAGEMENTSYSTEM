package com.it342.projectmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.it342.projectmanagementsystem.api.ApiService;
import com.it342.projectmanagementsystem.api.RetrofitClient;
import com.it342.projectmanagementsystem.models.RegisterRequest;
import com.it342.projectmanagementsystem.models.RegisterResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FacultyRegisterActivity extends AppCompatActivity {

    private static final String TAG = "FacultyRegisterActivity";
    private EditText etFacultyId, etFirstName, etLastName, etEmail, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private TextView tvLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_register);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        etFacultyId = findViewById(R.id.etFacultyId);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvLogin = findViewById(R.id.tvLogin);

        // Set up click listeners
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Validate input fields
                if (validateInputs()) {
                    // Perform registration
                    registerFaculty();
                }
            }
        });

        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to login form
                startActivity(new Intent(FacultyRegisterActivity.this, LoginFormActivity.class));
                finish();
            }
        });
    }

    private boolean validateInputs() {
        boolean isValid = true;

        String facultyId = etFacultyId.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Check if fields are empty
        if (facultyId.isEmpty()) {
            etFacultyId.setError("Faculty ID is required");
            isValid = false;
        }

        if (firstName.isEmpty()) {
            etFirstName.setError("First name is required");
            isValid = false;
        }

        if (lastName.isEmpty()) {
            etLastName.setError("Last name is required");
            isValid = false;
        }

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            isValid = false;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.setError("Confirm password is required");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            isValid = false;
        }

        return isValid;
    }

    private void registerFaculty() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String facultyId = etFacultyId.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();

        // First create the user in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registration success
                        Log.d(TAG, "createUserWithEmail:success");
                        
                        // Now register in your backend
                        RegisterRequest registerRequest = new RegisterRequest(
                            facultyId, email, password, firstName, lastName
                        );
                        
                        ApiService apiService = RetrofitClient.getInstance().getApiService();
                        Call<RegisterResponse> call = apiService.register(registerRequest);
                        
                        call.enqueue(new Callback<RegisterResponse>() {
                            @Override
                            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                                if (response.isSuccessful()) {
                                    // Both Firebase and backend registration successful
                                    Intent intent = new Intent(FacultyRegisterActivity.this, LoginFormActivity.class);
                                    intent.putExtra("REGISTRATION_SUCCESS", "Account successfully created! Please login.");
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // Backend registration failed
                                    Toast.makeText(FacultyRegisterActivity.this, 
                                        "Registration completed but backend sync failed. Please try logging in.", 
                                        Toast.LENGTH_LONG).show();
                                    startActivity(new Intent(FacultyRegisterActivity.this, LoginFormActivity.class));
                                    finish();
                                }
                            }

                            @Override
                            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                                // Network error during backend registration
                                Toast.makeText(FacultyRegisterActivity.this, 
                                    "Registration completed but backend sync failed. Please try logging in.", 
                                    Toast.LENGTH_LONG).show();
                                startActivity(new Intent(FacultyRegisterActivity.this, LoginFormActivity.class));
                                finish();
                            }
                        });
                    } else {
                        // If registration fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(FacultyRegisterActivity.this, "Registration failed: " + 
                            task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
} 