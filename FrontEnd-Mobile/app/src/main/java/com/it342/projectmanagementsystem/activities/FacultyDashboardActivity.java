package com.it342.projectmanagementsystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.it342.projectmanagementsystem.R;

public class FacultyDashboardActivity extends AppCompatActivity {
    
    private TextView tvFacultyWelcome;
    private Button btnManageRequests, btnViewSchedule, btnManageParticipants;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_dashboard);
        
        initializeViews();
        setupButtonClickListeners();
    }

    private void initializeViews() {
        tvFacultyWelcome = findViewById(R.id.tvFacultyWelcome);
        btnManageRequests = findViewById(R.id.btnManageRequests);
        btnViewSchedule = findViewById(R.id.btnViewSchedule);
        btnManageParticipants = findViewById(R.id.btnManageParticipants);
        
        // TODO: Optionally, set welcome message using faculty name from SharedPreferences
    }

    private void setupButtonClickListeners() {
        btnManageRequests.setOnClickListener(v -> 
            startActivity(new Intent(this, ManageAppointmentRequestsActivity.class)));

        btnViewSchedule.setOnClickListener(v -> 
            startActivity(new Intent(this, ViewAppointmentScheduleActivity.class)));

        btnManageParticipants.setOnClickListener(v -> 
            startActivity(new Intent(this, ManageAppointmentParticipantsActivity.class)));
    }
} 