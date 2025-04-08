package com.cebu.projectmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.regex.Pattern;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;

public class LoginFormActivity extends AppCompatActivity {

    private static final String TAG = "LoginFormActivity";
    private EditText etIdNumber, etPassword;
    private Button btnSignIn;
    private TextView tvForgotPassword;
    private RequestQueue requestQueue;
    private static final String LOGIN_URL = "http://10.0.2.2:8080/auth1/login"; // Using 10.0.2.2 for Android emulator to access localhost

    // Define ID pattern: XX-XXXX-XXX where X is a digit
    private static final Pattern ID_PATTERN = Pattern.compile("^\\d{2}-\\d{4}-\\d{3}$"); // e.g., 21-2023-123

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_form);

        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(this);

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
                    attemptLogin();
                }
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginFormActivity.this, "Forgot password feature coming soon", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void attemptLogin() {
        String idNumber = etIdNumber.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        try {
            // Create JSON request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("identifier", idNumber);
            requestBody.put("password", password);

            Log.d(TAG, "Attempting login with ID: " + idNumber);

            // Create the request
            JsonObjectRequest loginRequest = new JsonObjectRequest(
                Request.Method.POST,
                LOGIN_URL,
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.d(TAG, "Login response received: " + response.toString());
                            
                            // Check if response contains token
                            if (response.has("token")) {
                                String token = response.getString("token");
                                String userId = response.optString("userId");
                                String studId = response.optString("studId");
                                String email = response.optString("email");

                                Log.d(TAG, "Login successful for user: " + studId);

                                // Navigate to MainActivity
                                Intent intent = new Intent(LoginFormActivity.this, MainActivity.class);
                                intent.putExtra("USER_ID", userId);
                                intent.putExtra("STUD_ID", studId);
                                intent.putExtra("EMAIL", email);
                                startActivity(intent);
                                finish();
                            } else {
                                Log.e(TAG, "Login response missing token");
                                Toast.makeText(LoginFormActivity.this, 
                                    "Invalid login response from server", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing login response: " + e.getMessage());
                            Toast.makeText(LoginFormActivity.this, 
                                "Error processing login response", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        NetworkResponse response = error.networkResponse;
                        if (response != null && response.data != null) {
                            String errorMessage = new String(response.data, StandardCharsets.UTF_8);
                            Log.e(TAG, "Login error: " + errorMessage + ", Status Code: " + response.statusCode);
                            
                            if (response.statusCode == 401) {
                                Toast.makeText(LoginFormActivity.this, 
                                    "Invalid ID or password", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LoginFormActivity.this, 
                                    "Login failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "Network error during login: " + error.toString());
                            Toast.makeText(LoginFormActivity.this, 
                                "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            ) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };

            // Add request to queue
            requestQueue.add(loginRequest);

        } catch (Exception e) {
            Log.e(TAG, "Error creating login request: " + e.getMessage());
            Toast.makeText(this, "Error creating login request", Toast.LENGTH_SHORT).show();
        }
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