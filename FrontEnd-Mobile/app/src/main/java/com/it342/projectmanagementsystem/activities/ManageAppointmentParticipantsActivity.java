package com.it342.projectmanagementsystem.activities;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.api.ApiService;
import com.it342.projectmanagementsystem.api.RetrofitClient;
import android.content.SharedPreferences;
import android.util.Log;

public class ManageAppointmentParticipantsActivity extends AppCompatActivity {
    private static final String TAG = "ManageAppointmentParticipants";
    private RecyclerView rvParticipants;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_appointment_participants);

        // Initialize views
        rvParticipants = findViewById(R.id.rvParticipants);
        rvParticipants.setLayoutManager(new LinearLayoutManager(this));

        // Initialize API service
        apiService = RetrofitClient.getInstance().getApiService();

        // Load participants
        loadParticipants();
    }

    private void loadParticipants() {
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");
        String userId = prefs.getString("userId", "");

        if (token.isEmpty() || userId.isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // TODO: Implement API call to get participants
        // TODO: Create RecyclerView adapter for participants
        Toast.makeText(this, "Loading participants...", Toast.LENGTH_SHORT).show();
    }
} 