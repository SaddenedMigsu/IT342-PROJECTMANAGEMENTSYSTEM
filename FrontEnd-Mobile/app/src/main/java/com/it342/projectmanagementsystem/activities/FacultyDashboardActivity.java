package com.it342.projectmanagementsystem.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.utils.Constants;

public class FacultyDashboardActivity extends AppCompatActivity {
    
    private TextView tvFacultyWelcome;
    private Button btnManageRequests, btnViewSchedule, btnManageParticipants;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_dashboard);
        
        initializeViews();
        setupButtonClickListeners();
        setWelcomeMessage();
    }

    private void initializeViews() {
        tvFacultyWelcome = findViewById(R.id.tvFacultyWelcome);
        btnManageRequests = findViewById(R.id.btnManageRequests);
        btnViewSchedule = findViewById(R.id.btnViewSchedule);
        btnManageParticipants = findViewById(R.id.btnManageParticipants);
    }

    private void setWelcomeMessage() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String firstName = prefs.getString(Constants.KEY_FIRST_NAME, "");
        String lastName = prefs.getString(Constants.KEY_LAST_NAME, "");
        
        if (!firstName.isEmpty() && !lastName.isEmpty()) {
            tvFacultyWelcome.setText("Welcome, " + firstName + " " + lastName + "!");
        }
    }

    private void setupButtonClickListeners() {
        btnManageRequests.setOnClickListener(v -> 
            startActivity(new Intent(this, ManageAppointmentRequestsActivity.class)));

        btnViewSchedule.setOnClickListener(v -> 
            startActivity(new Intent(this, FacultyAppointmentScheduleActivity.class)));

        btnManageParticipants.setOnClickListener(v -> 
            startActivity(new Intent(this, ManageAppointmentParticipantsActivity.class)));
    }
} 