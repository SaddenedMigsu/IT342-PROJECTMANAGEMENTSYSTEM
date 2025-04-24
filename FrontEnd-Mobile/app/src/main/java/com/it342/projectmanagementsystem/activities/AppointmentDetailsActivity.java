package com.it342.projectmanagementsystem.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.models.Appointment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppointmentDetailsActivity extends AppCompatActivity {
    private static final String TAG = "AppointmentDetails";
    private FirebaseFirestore db;
    private Appointment appointment;
    private String appointmentId;

    private TextView tvMeetingTitle, tvFacultyName, tvDescription;
    private TextView tvStartTime, tvEndTime, tvStatus;
    private Button btnEdit, btnDelete;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_details);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Get appointment from intent
        appointment = getIntent().getParcelableExtra("APPOINTMENT_PARCEL");
        Log.d(TAG, "Received appointment: " + (appointment != null ? "not null" : "null"));
        
        if (appointment == null) {
            Log.e(TAG, "Invalid appointment received");
            Toast.makeText(this, "Error: Invalid appointment data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        appointmentId = appointment.getId();
        if (appointmentId == null || appointmentId.equals("null")) {
            Log.e(TAG, "Invalid appointment ID in appointment object");
            Toast.makeText(this, "Error: Invalid appointment ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Update UI with the appointment data we already have
        updateUI();

        // Set click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        tvMeetingTitle = findViewById(R.id.tvMeetingTitle);
        tvFacultyName = findViewById(R.id.tvFacultyName);
        tvDescription = findViewById(R.id.tvDescription);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvEndTime = findViewById(R.id.tvEndTime);
        tvStatus = findViewById(R.id.tvStatus);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnBack = findViewById(R.id.btnBack);
    }

    private void loadAppointmentDetails() {
        Log.d(TAG, "Loading appointment details for ID: " + appointmentId);
        
        db.collection("appointments")
                .document(appointmentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Document exists, converting to Appointment object");
                        appointment = documentSnapshot.toObject(Appointment.class);
                        if (appointment != null) {
                            Log.d(TAG, "Successfully converted document to Appointment object");
                            updateUI();
                        } else {
                            Log.e(TAG, "Failed to convert document to Appointment object");
                            Toast.makeText(this, "Error: Could not load appointment details", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Log.e(TAG, "Document does not exist for ID: " + appointmentId);
                        Toast.makeText(this, "Appointment not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading appointment", e);
                    Toast.makeText(this, "Error loading appointment details", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void updateUI() {
        if (appointment != null) {
            Log.d(TAG, "Updating UI with appointment details");
            
            tvMeetingTitle.setText(appointment.getTitle());
            tvDescription.setText(appointment.getDescription());
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            
            Timestamp startTimestamp = appointment.getStartTime();
            if (startTimestamp != null) {
                Date startDate = startTimestamp.toDate();
                tvStartTime.setText("Start: " + dateFormat.format(startDate));
            } else {
                tvStartTime.setText("Start time not set");
            }
            
            Timestamp endTimestamp = appointment.getEndTime();
            if (endTimestamp != null) {
                Date endDate = endTimestamp.toDate();
                tvEndTime.setText("End: " + dateFormat.format(endDate));
            } else {
                tvEndTime.setText("End time not set");
            }
            
            tvStatus.setText("Status: " + (appointment.getStatus() != null ? appointment.getStatus() : "Pending"));

            // Set status color based on appointment status
            int statusColor;
            String status = appointment.getStatus() != null ? appointment.getStatus().toLowerCase() : "pending";
            switch (status) {
                case "approved":
                    statusColor = getResources().getColor(android.R.color.holo_green_dark);
                    break;
                case "pending":
                case "pending_approval":
                    statusColor = getResources().getColor(android.R.color.holo_orange_dark);
                    break;
                case "rejected":
                    statusColor = getResources().getColor(android.R.color.holo_red_dark);
                    break;
                default:
                    statusColor = getResources().getColor(android.R.color.black);
            }
            tvStatus.setTextColor(statusColor);
        } else {
            Log.e(TAG, "Cannot update UI: appointment object is null");
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditAppointmentActivity.class);
            intent.putExtra("appointmentId", appointmentId);
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Appointment")
                .setMessage("Are you sure you want to delete this appointment?")
                .setPositiveButton("Delete", (dialog, which) -> deleteAppointment())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAppointment() {
        db.collection("appointments")
                .document(appointmentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Appointment deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting appointment", e);
                    Toast.makeText(this, "Error deleting appointment", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload appointment details in case they were updated
        if (appointmentId != null) {
            loadAppointmentDetails();
        }
    }
} 