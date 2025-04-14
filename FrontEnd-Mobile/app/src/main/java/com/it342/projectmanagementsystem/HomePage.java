package com.it342.projectmanagementsystem;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class HomePage extends AppCompatActivity {
    private TextView welcomeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // Initialize views
        welcomeText = findViewById(R.id.welcomeText);
        
        // Get user info from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String firstName = prefs.getString("firstName", "");
        String lastName = prefs.getString("lastName", "");
        
        if (firstName != null && !firstName.isEmpty() && lastName != null && !lastName.isEmpty()) {
            welcomeText.setText("Welcome, " + firstName + " " + lastName + "!");
        } else {
            String userId = prefs.getString("userId", "User");
            welcomeText.setText("Welcome, " + userId + "!");
        }
    }
} 