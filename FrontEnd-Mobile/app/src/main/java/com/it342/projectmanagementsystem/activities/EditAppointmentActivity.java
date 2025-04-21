package com.it342.projectmanagementsystem.activities;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.models.Appointment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditAppointmentActivity extends AppCompatActivity {
    private static final String TAG = "EditAppointment";
    private FirebaseFirestore db;
    private String appointmentId;
    private Appointment currentAppointment;

    private EditText etTitle, etDescription;
    private Button btnStartTime, btnEndTime, btnSave;
    private ImageButton btnBack;

    private Calendar startTime = Calendar.getInstance();
    private Calendar endTime = Calendar.getInstance();

    private Date startDate;
    private Date endDate;

    private ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_appointment);

        // Initialize views
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        btnStartTime = findViewById(R.id.btnStartTime);
        btnEndTime = findViewById(R.id.btnEndTime);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);

        // Initialize loading dialog
        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Updating appointment...");
        loadingDialog.setCancelable(false);

        // Get appointment ID from intent
        appointmentId = getIntent().getStringExtra("appointmentId");
        if (appointmentId == null) {
            Toast.makeText(this, "Error: No appointment ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Set click listeners
        btnStartTime.setOnClickListener(v -> showDateTimePicker(true));
        btnEndTime.setOnClickListener(v -> showDateTimePicker(false));
        btnSave.setOnClickListener(v -> updateAppointment());
        btnBack.setOnClickListener(v -> finish());

        // Load appointment data
        loadAppointment();
    }

    private void loadAppointment() {
        db.collection("appointments")
                .document(appointmentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentAppointment = documentSnapshot.toObject(Appointment.class);
                        if (currentAppointment != null) {
                            updateUI();
                        }
                    } else {
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
        if (currentAppointment != null) {
            etTitle.setText(currentAppointment.getTitle());
            etDescription.setText(currentAppointment.getDescription());
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            
            Timestamp startTimestamp = currentAppointment.getStartTime();
            if (startTimestamp != null) {
                startDate = startTimestamp.toDate();
                btnStartTime.setText(dateFormat.format(startDate));
            }
            
            Timestamp endTimestamp = currentAppointment.getEndTime();
            if (endTimestamp != null) {
                endDate = endTimestamp.toDate();
                btnEndTime.setText(dateFormat.format(endDate));
            }
        }
    }

    private void showDateTimePicker(boolean isStartTime) {
        Calendar calendar = Calendar.getInstance();
        if (isStartTime && startDate != null) {
            calendar.setTime(startDate);
        } else if (!isStartTime && endDate != null) {
            calendar.setTime(endDate);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // Show time picker after date is selected
                    new TimePickerDialog(
                            this,
                            (timeView, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);

                                // Update the corresponding date and button
                                if (isStartTime) {
                                    startDate = calendar.getTime();
                                    updateTimeButton(btnStartTime, startDate);
                                } else {
                                    endDate = calendar.getTime();
                                    updateTimeButton(btnEndTime, endDate);
                                }
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false
                    ).show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateTimeButton(Button button, Date date) {
        if (date != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            button.setText(dateFormat.format(date));
        }
    }

    private void updateAppointment() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Title is required");
            return;
        }

        if (description.isEmpty()) {
            etDescription.setError("Description is required");
            return;
        }

        if (startDate == null) {
            Toast.makeText(this, "Please select start time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endDate == null) {
            Toast.makeText(this, "Please select end time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endDate.before(startDate)) {
            Toast.makeText(this, "End time cannot be before start time", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update the appointment object
        currentAppointment.setTitle(title);
        currentAppointment.setDescription(description);
        currentAppointment.setStartTime(new Timestamp(startDate));
        currentAppointment.setEndTime(new Timestamp(endDate));

        // Show loading dialog
        loadingDialog.show();

        // Update in Firestore
        db.collection("appointments")
                .document(appointmentId)
                .set(currentAppointment)
                .addOnSuccessListener(aVoid -> {
                    loadingDialog.dismiss();
                    Toast.makeText(this, "Appointment updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    Toast.makeText(this, "Failed to update appointment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
} 