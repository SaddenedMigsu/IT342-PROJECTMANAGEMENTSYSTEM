package com.it342.projectmanagementsystem;

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
        String userId = getSharedPreferences("AuthPrefs", MODE_PRIVATE)
            .getString("userId", "User");
        welcomeText.setText("Welcome, " + userId + "!");
    }
} 