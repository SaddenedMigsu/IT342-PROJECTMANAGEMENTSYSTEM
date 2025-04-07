package com.cebu.projectmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.regex.Pattern;
import androidx.appcompat.app.AppCompatActivity;

public class LoginFormActivity extends AppCompatActivity {

    private EditText etIdNumber, etPassword;
    private Button btnSignIn;
    private TextView tvForgotPassword;

    // Define ID pattern: XX-XXXX-XXX where X is a digit
    private static final Pattern ID_PATTERN = Pattern.compile("^\\d{2}-\\d{4}-\\d{3}$"); // e.g., 21-2023-123

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
        etIdNumber = findViewById(R.id.etIdNumber);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Set click listeners
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInputs()) {
                    // Normally would authenticate against your backend
                    // For now, just navigate to main
                    startActivity(new Intent(LoginFormActivity.this, MainActivity.class));
                    finish();
                }
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show forgot password dialog or navigate to forgot password screen
                Toast.makeText(LoginFormActivity.this, "Forgot password feature coming soon", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateInputs() {
        String idNumber = etIdNumber.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        boolean isValid = true;

        if (idNumber.isEmpty()) {
            etIdNumber.setError("ID Number is required");
            isValid = false;
        } else if (!isValidIdNumber(idNumber)) {
            etIdNumber.setError("Invalid ID format. Use XX-XXXX-XXX format (e.g., 21-2023-123)");
            isValid = false;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            isValid = false;
        }

        return isValid;
    }

    private boolean isValidIdNumber(String idNumber) {
        return ID_PATTERN.matcher(idNumber).matches();
    }
} 