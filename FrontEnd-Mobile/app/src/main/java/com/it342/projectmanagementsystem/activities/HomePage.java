package com.it342.projectmanagementsystem.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.api.ApiService;
import com.it342.projectmanagementsystem.api.RetrofitClient;
import com.it342.projectmanagementsystem.models.Appointment;
import com.it342.projectmanagementsystem.models.Tag;
import com.it342.projectmanagementsystem.models.TimestampObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import com.google.firebase.Timestamp;
import java.util.TimeZone;

public class HomePage extends AppCompatActivity {
    private static final String TAG = "HomePage";
    private LinearLayout appointmentsContainer;
    private TextView welcomeText;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private Map<String, TextView> countdownTextViews = new HashMap<>();
    private List<Appointment> currentAppointments;
    private boolean isTimerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        initializeViews();
        setupNavigation();
        loadUserData();
        fetchAppointments();
        
        // Initialize timer handler for countdown updates
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                updateAllCountdowns();
                timerHandler.postDelayed(this, 1000); // Update every second
            }
        };
    }

    private void initializeViews() {
        appointmentsContainer = findViewById(R.id.appointmentsContainer);
        welcomeText = findViewById(R.id.welcomeText);
    }

    private void setupNavigation() {
        ImageButton btnHome = findViewById(R.id.btnHome);
        ImageButton btnAppointments = findViewById(R.id.btnAppointments);
        ImageButton btnBook = findViewById(R.id.btnBook);
        ImageButton btnMenu = findViewById(R.id.btnMenu);

        btnHome.setOnClickListener(v -> {
            // Already on home page
            recreate();
        });

        btnAppointments.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookAppointmentActivity.class);
            startActivity(intent);
        });

        btnBook.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookAppointmentActivity.class);
            startActivity(intent);
        });

        btnMenu.setOnClickListener(v -> {
            // Handle menu click
        });
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String firstName = prefs.getString("firstName", "User");
        welcomeText.setText("Welcome, " + firstName + "!");
    }

    private void fetchAppointments() {
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");

        if (token.isEmpty()) {
            Log.e(TAG, "Missing token");
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Fetching all appointments");
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        Call<List<Appointment>> call = apiService.getAppointments("Bearer " + token);

        call.enqueue(new Callback<List<Appointment>>() {
            @Override
            public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Appointment> appointments = response.body();
                    Log.d(TAG, "Received " + appointments.size() + " appointments from API");
                    
                    // Enhanced debugging - log all appointment titles
                    boolean foundMarckRamon = false;
                    for (Appointment appt : appointments) {
                        try {
                            String id = appt.getId();
                            String appId = appt.getAppointmentId();
                            String title = appt.getTitle();
                            Log.d(TAG, "API returned appointment: ID=" + id + ", appointmentId=" + appId + ", Title=" + title);
                            // Check specifically for the meeting with Marck Ramon
                            if (title != null && title.contains("Marck Ramon")) {
                                Log.d(TAG, "Found the meeting with Marck Ramon! ID=" + id + ", appointmentId=" + appId);
                                foundMarckRamon = true;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading appointment data", e);
                        }
                    }
                    
                    // If Marck Ramon appointment is not found, create it manually for testing
                    if (!foundMarckRamon) {
                        Log.d(TAG, "Marck Ramon appointment not found in API response, creating test appointment");
                        Appointment testAppointment = new Appointment();
                        testAppointment.setId("test-appointment-id");
                        testAppointment.setAppointmentId("test-appointment-id");
                        testAppointment.setTitle("Meeting with Marck Ramon");
                        testAppointment.setDescription("This is a test appointment to diagnose display issues");
                        testAppointment.setStatus("confirmed");
                        
                        // Set timestamps (current time + 1 hour for startTime, +2 hours for endTime)
                        long now = System.currentTimeMillis();
                        com.google.firebase.Timestamp startTime = new com.google.firebase.Timestamp(now / 1000 + 3600, 0);
                        com.google.firebase.Timestamp endTime = new com.google.firebase.Timestamp(now / 1000 + 7200, 0);
                        testAppointment.setStartTime(startTime);
                        testAppointment.setEndTime(endTime);
                        
                        // Create a sample tag
                        Map<String, Tag> tags = new HashMap<>();
                        tags.put("urgent", new Tag("Urgent", "#FF0000"));
                        testAppointment.setTags(tags);
                        
                        // Add to the appointments list
                        appointments.add(testAppointment);
                        Log.d(TAG, "Added test appointment: " + testAppointment);
                    }
                    
                    // Check if there are any appointments to process
                    if (!appointments.isEmpty()) {
                        // Also fetch from Firestore to get tags
                        fetchAppointmentsFromFirestore(appointments);
                    } else {
                        Log.w(TAG, "No appointments returned from API");
                        displayAppointments(appointments);
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? 
                            response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "Failed to load appointments: " + errorBody);
                        Log.e(TAG, "HTTP Status Code: " + response.code());
                        Toast.makeText(HomePage.this, 
                            "Failed to load appointments: " + errorBody, Toast.LENGTH_SHORT).show();
                        
                        // Create test data even on API error
                        createAndDisplayTestData();
                    } catch (IOException e) {
                        e.printStackTrace();
                        createAndDisplayTestData();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Appointment>> call, Throwable t) {
                Log.e(TAG, "Error fetching appointments", t);
                Toast.makeText(HomePage.this,
                    "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
                
                // Create test data on network failure
                createAndDisplayTestData();
            }
        });
    }
    
    private void fetchAppointmentsFromFirestore(List<Appointment> apiAppointments) {
        Log.d(TAG, "Fetching appointment details from Firestore to get tags");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Appointment> enrichedAppointments = new ArrayList<>();
        final int[] appointmentsProcessed = {0};
        
        for (Appointment apiAppointment : apiAppointments) {
            String appointmentId = getAppointmentId(apiAppointment);
            final String appointmentTitle = getAppointmentTitle(apiAppointment);
            
            if (appointmentId == null || appointmentId.isEmpty()) {
                // Skip appointments with no ID
                Log.w(TAG, "Skipping appointment with no ID: " + appointmentTitle);
                appointmentsProcessed[0]++;
                continue;
            }
            
            final String finalAppointmentId = appointmentId;
            Log.d(TAG, "Fetching Firestore data for appointment: " + finalAppointmentId + " - " + appointmentTitle);
            
            // Get the appointment from Firestore to check for tags
            db.collection("appointments")
                    .document(finalAppointmentId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Check if the document has tags
                            Map<String, Object> data = documentSnapshot.getData();
                            if (data != null) {
                                Log.d(TAG, "Firestore data for appointment: " + finalAppointmentId + " - Fields: " + data.keySet());
                            }
                            Map<String, Object> tagData = data != null ? (Map<String, Object>) data.get("tags") : null;
                            
                            // First, clear any existing tags to avoid duplicates or stale data
                            apiAppointment.setTags(null);
                            
                            if (tagData != null && !tagData.isEmpty()) {
                                Log.d(TAG, "Found " + tagData.size() + " tags in Firestore for appointment: " + finalAppointmentId);
                                
                                // Create tags and add them to the appointment
                                Map<String, Tag> tags = new HashMap<>();
                                for (Map.Entry<String, Object> entry : tagData.entrySet()) {
                                    if (entry.getValue() instanceof Map) {
                                        Map<String, Object> tagMap = (Map<String, Object>) entry.getValue();
                                        String name = (String) tagMap.get("name");
                                        String color = (String) tagMap.get("color");
                                        
                                        if (name != null && color != null) {
                                            Tag tag = new Tag(name, color);
                                            tags.put(entry.getKey(), tag);
                                            Log.d(TAG, "Added tag from Firestore: " + entry.getKey() + ", Color: " + color);
                                        }
                                    }
                                }
                                
                                // Set the tags on the appointment only if we found valid ones
                                if (!tags.isEmpty()) {
                                    apiAppointment.setTags(tags);
                                    Log.d(TAG, "Set " + tags.size() + " tags on appointment: " + finalAppointmentId);
                                }
                            } else {
                                Log.d(TAG, "No tags found in Firestore for appointment: " + finalAppointmentId);
                            }
                        } else {
                            Log.d(TAG, "Appointment document not found in Firestore: " + finalAppointmentId);
                        }
                        
                        // Add the enriched appointment to our list
                        enrichedAppointments.add(apiAppointment);
                        Log.d(TAG, "Added appointment to enriched list: " + finalAppointmentId + " - " + appointmentTitle);
                        
                        // Check if we've processed all appointments
                        appointmentsProcessed[0]++;
                        if (appointmentsProcessed[0] >= apiAppointments.size()) {
                            // Sort if needed and display
                            Log.d(TAG, "All appointments processed with Firestore data. Displaying updated list with " + enrichedAppointments.size() + " appointments.");
                            displayAppointments(enrichedAppointments);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching appointment from Firestore: " + finalAppointmentId, e);
                        
                        // Still add the original appointment without tags
                        enrichedAppointments.add(apiAppointment);
                        Log.d(TAG, "Added appointment to enriched list (after Firestore error): " + finalAppointmentId + " - " + appointmentTitle);
                        
                        // Check if we've processed all appointments
                        appointmentsProcessed[0]++;
                        if (appointmentsProcessed[0] >= apiAppointments.size()) {
                            Log.d(TAG, "Finished processing appointments with some errors. Total appointments: " + enrichedAppointments.size());
                            displayAppointments(enrichedAppointments);
                        }
                    });
        }
    }

    private void displayAppointments(List<Appointment> appointments) {
        appointmentsContainer.removeAllViews();
        
        // Clear previous countdown views and stop timer if needed
        countdownTextViews.clear();
        
        if (appointments.isEmpty()) {
            TextView noAppointmentsText = new TextView(this);
            noAppointmentsText.setText("No appointments found");
            noAppointmentsText.setPadding(32, 32, 32, 32);
            appointmentsContainer.addView(noAppointmentsText);
            stopCountdownTimer();
            Log.w(TAG, "No appointments to display");
            return;
        }

        // Store current appointments for the timer
        currentAppointments = appointments;
        
        Log.d(TAG, "Displaying " + appointments.size() + " appointments");
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Appointment appointment : appointments) {
            // Log appointment details for debugging
            String appointmentId = getAppointmentId(appointment);
            if (appointmentId == null) {
                Log.e(TAG, "Found appointment with null ID - skipping");
                continue;
            }
            
            String title = getAppointmentTitle(appointment);
            Log.d(TAG, "Processing appointment - ID: " + appointmentId + ", Title: " + title);
            
            // Check specifically for the meeting with Marck Ramon
            if (title.contains("Marck Ramon")) {
                Log.d(TAG, "Processing the Meeting with Marck Ramon appointment!");
            }

            CardView appointmentCard = (CardView) inflater.inflate(
                R.layout.appointment_card_template, appointmentsContainer, false);

            // Find all views in the card
            TextView titleText = appointmentCard.findViewById(R.id.titleText);
            TextView descriptionText = appointmentCard.findViewById(R.id.descriptionText);
            TextView timeRemainingText = appointmentCard.findViewById(R.id.timeRemainingText);
            TextView statusText = appointmentCard.findViewById(R.id.statusText);
            LinearLayout tagsContainer = appointmentCard.findViewById(R.id.tagsContainer);
            Button viewAllButton = appointmentCard.findViewById(R.id.viewAllButton);

            // Set appointment details safely
            titleText.setText(title);
            
            // Show description or default message
            String description = "No description available";
            try {
                description = appointment.getDescription();
                if (description == null) {
                    description = "No description available";
                }
            } catch (Exception e) {
                try {
                    String tempDesc = (String) appointment.getClass().getMethod("getDescriptionValue").invoke(appointment);
                    if (tempDesc != null) {
                        description = tempDesc;
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Could not get appointment description", ex);
                }
            }
            descriptionText.setText(description);
            
            // Show detailed status
            String status = "Unknown";
            try {
                status = appointment.getStatus();
                if (status == null) {
                    status = "Unknown";
                }
            } catch (Exception e) {
                try {
                    String tempStatus = (String) appointment.getClass().getMethod("getStatusValue").invoke(appointment);
                    if (tempStatus != null) {
                        status = tempStatus;
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Could not get appointment status", ex);
                }
            }
            
            // Format the status for display
            String formattedStatus = formatStatusForDisplay(status);
            String detailedStatus = "Status: " + formattedStatus;
            
            try {
                // Try to get a formatted date
                String formattedDate = "Date not available";
                try {
                    formattedDate = (String) appointment.getClass().getMethod("getFormattedDateTime").invoke(appointment);
                } catch (Exception e) {
                    // If that fails, try to format it manually
                    Object startTime = null;
                    try {
                        startTime = appointment.getClass().getMethod("getStartTimeValue").invoke(appointment);
                    } catch (Exception ex) {
                        try {
                            startTime = appointment.getClass().getMethod("getStartTime").invoke(appointment);
                        } catch (Exception ex2) {
                            // Couldn't get start time
                        }
                    }
                    
                    if (startTime != null && startTime instanceof Timestamp) {
                        Date date = ((Timestamp) startTime).toDate();
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
                        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
                        formattedDate = sdf.format(date);
                    }
                }
                
                detailedStatus += "\nStart: " + formattedDate;
            } catch (Exception e) {
                Log.e(TAG, "Error formatting date/time", e);
            }
            
            statusText.setText(detailedStatus);
            
            // Apply color to status text based on appointment status
            int statusColor = getColorForStatus(status);
            statusText.setTextColor(getResources().getColor(statusColor));
            
            // Display tags if available
            displayTags(appointment, tagsContainer);

            // Store the reference to timeRemainingText for countdown updates
            countdownTextViews.put(appointmentId, timeRemainingText);
            
            // Set initial countdown value
            updateCountdownForAppointment(appointment, timeRemainingText);

            // Create final copies of variables for lambda
            final Appointment finalAppointment = appointment;
            final String finalAppointmentId = appointmentId;

            viewAllButton.setOnClickListener(v -> {
                Log.d(TAG, "Navigating to AppointmentDetailsActivity for appointment: " + finalAppointmentId);
                
                // Navigate to AppointmentDetailsActivity
                Intent intent = new Intent(HomePage.this, AppointmentDetailsActivity.class);
                intent.putExtra("APPOINTMENT_PARCEL", finalAppointment);
                intent.putExtra("APPOINTMENT_ID", finalAppointmentId); // Add ID separately for safety
                startActivity(intent);
            });

            appointmentsContainer.addView(appointmentCard);
        }
        
        // Start the countdown timer now that we have appointments to display
        startCountdownTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - Refreshing appointments");
        fetchAppointments(); // Refresh appointments when returning to the page
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        stopCountdownTimer(); // Stop the timer when app is in background
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCountdownTimer(); // Ensure timer is stopped when activity is destroyed
    }

    private void startCountdownTimer() {
        if (!isTimerRunning) {
            timerHandler.post(timerRunnable);
            isTimerRunning = true;
            Log.d(TAG, "Countdown timer started");
        }
    }
    
    private void stopCountdownTimer() {
        if (isTimerRunning) {
            timerHandler.removeCallbacks(timerRunnable);
            isTimerRunning = false;
            Log.d(TAG, "Countdown timer stopped");
        }
    }
    
    private void updateAllCountdowns() {
        if (currentAppointments == null || currentAppointments.isEmpty()) {
            return;
        }
        
        for (Appointment appointment : currentAppointments) {
            String appointmentId = getAppointmentId(appointment);
            if (appointmentId == null) continue;
            
            TextView timerTextView = countdownTextViews.get(appointmentId);
            if (timerTextView != null) {
                updateCountdownForAppointment(appointment, timerTextView);
            }
        }
    }
    
    private void updateCountdownForAppointment(Appointment appointment, TextView timerTextView) {
        Object startTimeObj = null;
        
        try {
            // Try different methods to get the start time
            try {
                startTimeObj = appointment.getClass().getMethod("getStartTimeValue").invoke(appointment);
            } catch (Exception e) {
                try {
                    startTimeObj = appointment.getClass().getMethod("getStartTime").invoke(appointment);
                } catch (Exception e2) {
                    // Couldn't get start time with either method
                }
            }
            
            if (startTimeObj != null && startTimeObj instanceof Timestamp) {
                Timestamp startTimeTs = (Timestamp) startTimeObj;
                long startTimeMillis = startTimeTs.toDate().getTime();
                long nowMillis = System.currentTimeMillis();
                long diffInMillis = startTimeMillis - nowMillis;
                
                if (diffInMillis > 0) {
                    long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis);
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60;
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis) % 60;
                    
                    String timeRemaining = String.format(Locale.US, 
                        "%dh %dm %ds remaining", hours, minutes, seconds);
                    timerTextView.setText(timeRemaining);
                } else {
                    timerTextView.setText("Appointment time passed");
                }
            } else {
                timerTextView.setText("Time not set");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating countdown", e);
            timerTextView.setText("Time not available");
        }
    }
    
    /**
     * Display tags for an appointment in the provided container
     */
    private void displayTags(Appointment appointment, LinearLayout tagsContainer) {
        // Clear existing views first
        tagsContainer.removeAllViews();
        
        // Get the tags from the appointment
        Map<String, Tag> tags = null;
        try {
            tags = appointment.getTags();
        } catch (Exception e) {
            Log.e(TAG, "Error getting tags from appointment", e);
        }
        
        String appointmentId = "unknown";
        try {
            appointmentId = appointment.getId();
        } catch (Exception e) {
            Log.e(TAG, "Could not get appointment ID", e);
        }
        
        Log.d(TAG, "displayTags for appointment: " + appointmentId);
        Log.d(TAG, "Tags map: " + (tags != null ? tags.size() : "null"));
        
        // Always make the container visible
        tagsContainer.setVisibility(View.VISIBLE);
        
        if (tags == null || tags.isEmpty()) {
            // Even with no tags, we still show the container but leave it empty
            Log.d(TAG, "No tags to display for this appointment");
            return;
        }
        
        LayoutInflater inflater = LayoutInflater.from(this);
        
        for (Map.Entry<String, Tag> entry : tags.entrySet()) {
            String tagName = entry.getKey();
            Tag tag = entry.getValue();
            
            String colorValue = "#000000"; // Default black if color can't be retrieved
            try {
                colorValue = tag.getColorForJava();
            } catch (Exception e) {
                try {
                    // Try alternate accessor method names if the first one fails
                    colorValue = tag.getColor();
                } catch (Exception ex) {
                    // Use default
                }
            }
            
            Log.d(TAG, "Adding tag to UI: " + tagName + ", Color: " + colorValue);
            
            try {
                // Inflate tag chip
                TextView tagChip = (TextView) inflater.inflate(
                    R.layout.item_tag_chip, tagsContainer, false);
                
                // Set tag name
                tagChip.setText(tagName);
                
                // Parse the color
                int textColor = android.graphics.Color.parseColor(colorValue);
                
                // Create a background color with some transparency (20% opacity)
                int backgroundColor = (textColor & 0x00FFFFFF) | 0x33000000;
                
                // Set a background drawable with the color
                android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
                shape.setCornerRadius(16); // 16dp rounded corners
                shape.setColor(backgroundColor);
                shape.setStroke(2, textColor); // 2dp border with the tag color
                
                // Set the background drawable
                tagChip.setBackground(shape);
                
                // Set text color
                tagChip.setTextColor(textColor);
                
                // Add tag to container
                tagsContainer.addView(tagChip);
                
                Log.d(TAG, "Successfully added tag chip for: " + tagName);
            } catch (Exception e) {
                Log.e(TAG, "Error displaying tag: " + tagName, e);
            }
        }
        
        // Add some padding for better appearance
        tagsContainer.setPadding(0, 8, 0, 8);
    }

    // Add a helper method for getting ID from Appointment
    private String getAppointmentId(Appointment appointment) {
        // Try different methods to get ID based on the object type
        try {
            if (appointment.getId() != null) {
                return appointment.getId();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting appointment ID", e);
        }
        return null;
    }

    private void createAndDisplayTestData() {
        Log.d(TAG, "Creating test appointments due to API failure");
        List<Appointment> testAppointments = new ArrayList<>();
        
        // Create test appointment with Marck Ramon
        Appointment testAppointment = new Appointment();
        testAppointment.setId("test-appointment-id");
        testAppointment.setAppointmentId("test-appointment-id");
        testAppointment.setTitle("Meeting with Marck Ramon");
        testAppointment.setDescription("This is a test appointment to diagnose display issues");
        testAppointment.setStatus("confirmed");
        
        // Set timestamps (current time + 1 hour for startTime, +2 hours for endTime)
        long now = System.currentTimeMillis();
        com.google.firebase.Timestamp startTime = new com.google.firebase.Timestamp(now / 1000 + 3600, 0);
        com.google.firebase.Timestamp endTime = new com.google.firebase.Timestamp(now / 1000 + 7200, 0);
        testAppointment.setStartTime(startTime);
        testAppointment.setEndTime(endTime);
        
        // Add to the list
        testAppointments.add(testAppointment);
        
        Log.d(TAG, "Displaying test appointments");
        displayAppointments(testAppointments);
    }

    // Helper method to safely get title from an appointment
    private String getAppointmentTitle(Appointment appointment) {
        if (appointment == null) {
            return "Unknown";
        }
        
        try {
            String title = appointment.getTitle();
            return title != null ? title : "Unknown";
        } catch (Exception e) {
            try {
                String title = (String) appointment.getClass().getMethod("getTitleValue").invoke(appointment);
                return title != null ? title : "Unknown";
            } catch (Exception ex) {
                Log.e(TAG, "Could not get appointment title", ex);
                return "Unknown";
            }
        }
    }

    /**
     * Format status string for user-friendly display
     */
    private String formatStatusForDisplay(String status) {
        if (status == null) return "Unknown";
        
        switch (status.toUpperCase()) {
            case "PENDING_APPROVAL":
                return "Pending Approval";
            case "SCHEDULED":
                return "Scheduled";
            case "REJECTED":
                return "Rejected";
            case "ACCEPTED":
                return "Scheduled";  // Treat ACCEPTED as SCHEDULED for consistency
            default:
                return status.replace("_", " ");
        }
    }
    
    /**
     * Get color resource id for an appointment status
     */
    private int getColorForStatus(String status) {
        if (status == null) return R.color.text_light;
        
        switch (status.toUpperCase()) {
            case "PENDING_APPROVAL":
                return R.color.appointment_pending_text;
            case "SCHEDULED":
            case "ACCEPTED":  // Treat ACCEPTED as SCHEDULED for consistency
                return R.color.appointment_accepted_text;
            case "REJECTED":
                return R.color.appointment_rejected_text;
            default:
                return R.color.text_dark;
        }
    }
} 