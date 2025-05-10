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

        // Initialize date formatter
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
        
        Log.d(TAG, "Updating UI with appointment: " + appointment.getId() + " - " + appointment.getTitle());
        Log.d(TAG, "Appointment data: " + appointment.toString());
        
        tvTitle.setText(appointment.getTitle() != null ? appointment.getTitle() : "N/A");
        tvDescription.setText(appointment.getDescription() != null ? appointment.getDescription() : "N/A");
        tvStatus.setText("Status: " + (appointment.getStatus() != null ? appointment.getStatus() : "N/A"));
        
        // Get appointment details from Firestore to get the createdBy field
        fetchAppointmentCreator(appointment);
        
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

    private void fetchAppointmentCreator(Appointment appointment) {
        // Get the appointment ID
        String appointmentId = appointment.getId();
        if (appointmentId == null || appointmentId.isEmpty()) {
            appointmentId = appointment.getAppointmentId();
        }
        
        if (appointmentId == null || appointmentId.isEmpty()) {
            Log.e(TAG, "Cannot fetch creator - appointment ID is missing");
            setRequesterName("Student");
            return;
        }
        
        Log.d(TAG, "Fetching appointment creator from Firestore for ID: " + appointmentId);
        
        // Create a final copy of the appointment for use in lambda
        final Appointment finalAppointment = appointment;
        final String finalAppointmentId = appointmentId;
        
        // Query Firestore for this specific appointment
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        db.collection("appointments").document(appointmentId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Get the data from Firestore
                    Map<String, Object> data = documentSnapshot.getData();
                    if (data != null) {
                        Log.d(TAG, "Firestore data for appointment " + finalAppointmentId + ": " + data);
                        
                        // Extract creator name from createdBy (email)
                        String creatorName = null;
                        
                        // Prioritize createdBy field (email)
                        if (data.containsKey("createdBy")) {
                            String createdBy = (String) data.get("createdBy");
                            Log.d(TAG, "Found createdBy: " + createdBy + " for appointment: " + finalAppointmentId);
                            
                            if (createdBy != null && !createdBy.isEmpty()) {
                                // If it's an email address, extract and format the name
                                if (createdBy.contains("@")) {
                                    // Get name part before the @ symbol and domain part
                                    String[] emailParts = createdBy.split("@");
                                    String emailName = emailParts[0];
                                    String domain = emailParts.length > 1 ? emailParts[1] : "";
                                    
                                    // Replace dots with spaces and capitalize words for the username
                                    emailName = emailName.replace(".", " ");
                                    String[] nameParts = emailName.split(" ");
                                    StringBuilder nameBuilder = new StringBuilder();
                                    for (String part : nameParts) {
                                        if (!part.isEmpty()) {
                                            nameBuilder.append(part.substring(0, 1).toUpperCase())
                                                      .append(part.substring(1))
                                                      .append(" ");
                                        }
                                    }
                                    
                                    // For the last name, try to extract from domain if it's not a common domain
                                    if (domain.contains("jaca") || domain.contains("jaca.com")) {
                                        nameBuilder.append("Jaca");
                                    }
                                    
                                    creatorName = nameBuilder.toString().trim();
                                    Log.d(TAG, "Extracted name from email: " + creatorName);
                                } else {
                                    creatorName = createdBy;
                                }
                                
                                // Set the creator name
                                setRequesterName(creatorName);
                                return;
                            }
                        }
                    }
                }
                
                // If we get here, we couldn't get the creator name from Firestore
                // Fall back to the existing logic
                fallbackToExistingLogic(finalAppointment);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching appointment from Firestore", e);
                // Fall back to the existing logic
                fallbackToExistingLogic(finalAppointment);
            });
    }
    
    private void fallbackToExistingLogic(Appointment appointment) {
        // Get the student name who created the appointment
        String studentName = null;
        
        // First try: get from appointment data
        try {
            studentName = appointment.getCreatorName();
            Log.d(TAG, "Using creatorName from appointment: " + studentName);
        } catch (Exception e) {
            Log.e(TAG, "Error getting creatorName from appointment", e);
        }
        
        // Second try: get from SharedPreferences if appointment data wasn't found
        if (studentName == null || studentName.isEmpty()) {
            try {
                SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
                String firstName = prefs.getString("firstName", "");
                String lastName = prefs.getString("lastName", "");
                
                if (!firstName.isEmpty()) {
                    studentName = firstName + (lastName.isEmpty() ? "" : " " + lastName);
                    Log.d(TAG, "Using student name from SharedPreferences: " + studentName);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting student name from preferences", e);
            }
        }
        
        // Third try: Try reflection to get additional fields
        if (studentName == null || studentName.isEmpty()) {
            try {
                // Check if there's a studentName field
                java.lang.reflect.Method method = appointment.getClass().getMethod("getStudentName");
                Object result = method.invoke(appointment);
                if (result != null && !result.toString().isEmpty()) {
                    studentName = result.toString();
                    Log.d(TAG, "Found studentName via reflection: " + studentName);
                }
            } catch (Exception e) {
                // Ignore reflection errors
            }
        }
        
        // Set the requester name
        setRequesterName(studentName != null && !studentName.isEmpty() ? studentName : "Student");
    }
    
    private void setRequesterName(String name) {
        // Set the requester info text
        if (name != null && !name.isEmpty()) {
            tvRequesterInfo.setText("Requested by: " + name);
            Log.d(TAG, "Setting requester name to: " + name);
        } else {
            // Fallback to a generic student identifier if we can't get the name
            tvRequesterInfo.setText("Requested by: Student");
            Log.w(TAG, "Could not determine student name, using default name");
        }
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        Date date = timestamp.toDate();
        return dateTimeFormat.format(date);
    }

    private void setupButtonListeners() {
        btnAccept.setOnClickListener(v -> updateAppointmentStatus("SCHEDULED"));
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

        boolean isApproved = newStatus.equals("SCHEDULED");
        
        Map<String, Boolean> approvalBody = new HashMap<>();
        approvalBody.put("approved", isApproved);

        Log.d(TAG, "Attempting to update appointment approval " + appointmentIdToUpdate + " to approved=" + isApproved + ", status=" + newStatus);

        apiService.approveAppointment(appointmentIdToUpdate, approvalBody, "Bearer " + token)
                .enqueue(new Callback<Appointment>() {
            @Override
            public void onResponse(Call<Appointment> call, Response<Appointment> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Manually update the status to SCHEDULED or REJECTED based on our button action
                    // rather than relying on the server response which might use a different status name
                    Appointment updatedAppointment = response.body();
                    updatedAppointment.setStatus(newStatus);
                    
                    // Save the updated appointment back to server to ensure the status is correct
                    updateAppointmentOnServer(updatedAppointment, token);
                    
                    // Also update the status in Firestore
                    updateFirestoreAppointmentStatus(appointmentIdToUpdate, newStatus);
                    
                    Log.i(TAG, "Appointment approval successful. Status updated to: " + newStatus);
                    Toast.makeText(AppointmentRequestDetailsActivity.this, 
                        "Request " + (isApproved ? "accepted" : "rejected") + ".", Toast.LENGTH_SHORT).show();
                    
                    currentAppointment = updatedAppointment; 
                    updateUI(currentAppointment);
                    
                    // If appointment was rejected, finish the activity after a short delay
                    if (newStatus.equals("REJECTED")) {
                        new android.os.Handler().postDelayed(() -> {
                            finish();
                        }, 1500); // 1.5 seconds
                    }
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
    
    // Add a method to update the appointment status in Firestore
    private void updateFirestoreAppointmentStatus(String appointmentId, String newStatus) {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        
        Log.d(TAG, "Updating Firestore appointment status: " + appointmentId + " to " + newStatus);
        
        db.collection("appointments").document(appointmentId)
            .update("status", newStatus)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Firestore appointment status updated successfully");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating Firestore appointment status", e);
            });
    }
    
    // Add a method to update the appointment on the server with the new status
    private void updateAppointmentOnServer(Appointment appointment, String token) {
        apiService.updateAppointment(appointment.getId(), appointment, "Bearer " + token)
            .enqueue(new Callback<Appointment>() {
                @Override
                public void onResponse(Call<Appointment> call, Response<Appointment> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Successfully updated appointment status to " + appointment.getStatus());
                    } else {
                        Log.e(TAG, "Failed to update appointment status on server: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<Appointment> call, Throwable t) {
                    Log.e(TAG, "Network error updating appointment status", t);
                }
            });
    }
} 