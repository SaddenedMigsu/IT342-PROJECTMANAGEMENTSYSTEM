package com.cebu.projectmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class StudentRegisterActivity extends AppCompatActivity {

    private EditText etStudentId, etFirstName, etLastName, etEmail, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_register);

        // Initialize views
        etStudentId = findViewById(R.id.etStudentId);
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
                    registerStudent();
                }
            }
        });

        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to login form
                startActivity(new Intent(StudentRegisterActivity.this, LoginFormActivity.class));
                finish();
            }
        });
    }

    private boolean validateInputs() {
        boolean isValid = true;

        String studentId = etStudentId.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Check if fields are empty
        if (studentId.isEmpty()) {
            etStudentId.setError("Student ID is required");
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

    private void registerStudent() {
        // Here you would typically send the registration data to your server or database
        
        // Navigate to login form with success message
        Intent intent = new Intent(StudentRegisterActivity.this, LoginFormActivity.class);
        intent.putExtra("REGISTRATION_SUCCESS", "Account is Successfully created!");
        startActivity(intent);
        finish();
    }
} 