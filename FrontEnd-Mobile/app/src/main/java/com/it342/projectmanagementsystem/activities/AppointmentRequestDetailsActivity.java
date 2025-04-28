package com.it342.projectmanagementsystem.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.Timestamp;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.api.ApiService;
import com.it342.projectmanagementsystem.api.RetrofitClient;
import com.it342.projectmanagementsystem.models.Appointment;
import com.it342.projectmanagementsystem.models.User;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AppointmentRequestDetailsActivity extends AppCompatActivity {

    private static final String TAG = "RequestDetailsActivity";
    private TextView tvTitle, tvDescription, tvStatus, tvStartTime, tvEndTime, tvRequesterInfo;
    private Button btnAccept, btnReject;
    private ApiService apiService;
    private Appointment currentAppointment;
    private SimpleDateFormat dateTimeFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_request_details);

        // Initialize the date format with Asia/Manila timezone
        dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        dateTimeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));

        apiService = RetrofitClient.getInstance().getApiService();
        
        currentAppointment = getIntent().getParcelableExtra("APPOINTMENT_PARCEL");

        initializeViews();

        if (currentAppointment == null) {
            Log.e(TAG, "Appointment data missing in Intent!");
            Toast.makeText(this, "Error: Could not load appointment details.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        updateUI(currentAppointment);
        
        setupButtonListeners();
    }

    private void initializeViews() {
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvStatus = findViewById(R.id.tvDetailStatus);
        tvStartTime = findViewById(R.id.tvDetailStartTime);
        tvEndTime = findViewById(R.id.tvDetailEndTime);
        tvRequesterInfo = findViewById(R.id.tvDetailRequesterInfo);
        btnAccept = findViewById(R.id.btnAcceptRequest);
        btnReject = findViewById(R.id.btnRejectRequest);
    }

    private void updateUI(Appointment appointment) {
        if (appointment == null) return;
        
        tvTitle.setText(appointment.getTitle() != null ? appointment.getTitle() : "N/A");
        tvDescription.setText(appointment.getDescription() != null ? appointment.getDescription() : "N/A");
        tvStatus.setText("Status: " + (appointment.getStatus() != null ? appointment.getStatus() : "N/A"));
        
        String creatorName = appointment.getCreatorName();
        if (creatorName == null && currentAppointment != null) {
            creatorName = currentAppointment.getCreatorName();
        }
        
        if (creatorName != null && !creatorName.isEmpty()) {
            tvRequesterInfo.setText("Requested by: " + creatorName);
        } else {
            tvRequesterInfo.setText("Requested by: Unknown");
            Log.w(TAG, "Creator name is missing in appointment details for ID: " + appointment.getId());
        }
        
        Timestamp startTime = appointment.getStartTime();
        if (startTime != null) {
            tvStartTime.setText("Starts: " + formatTimestamp(startTime));
        } else {
            tvStartTime.setText("Starts: Not set");
        }
        Timestamp endTime = appointment.getEndTime();
        if (endTime != null) {
            tvEndTime.setText("Ends: " + formatTimestamp(endTime));
        } else {
            tvEndTime.setText("Ends: Not set");
        }

        if (!"PENDING_APPROVAL".equalsIgnoreCase(appointment.getStatus())) {
            btnAccept.setEnabled(false);
            btnReject.setEnabled(false);
        } else {
            btnAccept.setEnabled(true);
            btnReject.setEnabled(true);
        }
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        Date date = timestamp.toDate();
        return dateTimeFormat.format(date);
    }

    private void setupButtonListeners() {
        btnAccept.setOnClickListener(v -> updateAppointmentStatus("ACCEPTED"));
        btnReject.setOnClickListener(v -> updateAppointmentStatus("REJECTED"));
    }

    private void updateAppointmentStatus(String newStatus) {
        if (currentAppointment == null || currentAppointment.getId() == null) {
            Toast.makeText(this, "Appointment data not loaded yet or ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        String appointmentIdToUpdate = currentAppointment.getId();

        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");
        if (token.isEmpty()) {
            Toast.makeText(this, "Authentication error.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isApproved = newStatus.equals("ACCEPTED");
        
        Map<String, Boolean> approvalBody = new HashMap<>();
        approvalBody.put("approved", isApproved);

        Log.d(TAG, "Attempting to update appointment approval " + appointmentIdToUpdate + " to approved=" + isApproved);

        apiService.approveAppointment(appointmentIdToUpdate, approvalBody, "Bearer " + token)
                .enqueue(new Callback<Appointment>() {
            @Override
            public void onResponse(Call<Appointment> call, Response<Appointment> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseStatus = response.body().getStatus() != null ? response.body().getStatus() : newStatus;
                    Log.i(TAG, "Appointment approval successful. Status updated to: " + responseStatus);
                    Toast.makeText(AppointmentRequestDetailsActivity.this, "Request " + (isApproved ? "accepted" : "rejected") + ".", Toast.LENGTH_SHORT).show();
                    
                    currentAppointment = response.body(); 
                    updateUI(currentAppointment);
                } else {
                    String action = isApproved ? "accept" : "reject";
                    String errorMsg = "Failed to " + action + " request.";
                     try {
                        if (response.errorBody() != null) {
                            errorMsg += " Error: " + response.errorBody().string();
                        } else {
                            errorMsg += " Code: " + response.code();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, errorMsg);
                    Toast.makeText(AppointmentRequestDetailsActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Appointment> call, Throwable t) {
                String action = isApproved ? "accept" : "reject";
                Log.e(TAG, "Network error during " + action + " request", t);
                Toast.makeText(AppointmentRequestDetailsActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
} 