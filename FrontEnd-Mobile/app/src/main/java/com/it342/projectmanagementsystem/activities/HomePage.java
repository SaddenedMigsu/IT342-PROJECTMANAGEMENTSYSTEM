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
                    
                    // Check if there are any appointments to process
                    if (!appointments.isEmpty()) {
                        // Also fetch from Firestore to get tags
                        fetchAppointmentsFromFirestore(appointments);
                    } else {
                        displayAppointments(appointments);
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? 
                            response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "Failed to load appointments: " + errorBody);
                        Toast.makeText(HomePage.this, 
                            "Failed to load appointments: " + errorBody, Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Appointment>> call, Throwable t) {
                Log.e(TAG, "Error fetching appointments", t);
                Toast.makeText(HomePage.this,
                    "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void fetchAppointmentsFromFirestore(List<Appointment> apiAppointments) {
        Log.d(TAG, "Fetching appointment details from Firestore to get tags");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Appointment> enrichedAppointments = new ArrayList<>();
        final int[] appointmentsProcessed = {0};
        
        for (Appointment apiAppointment : apiAppointments) {
            String appointmentId = apiAppointment.getIdValue();
            if (appointmentId == null || appointmentId.isEmpty()) {
                // Skip appointments with no ID
                appointmentsProcessed[0]++;
                continue;
            }
            
            Log.d(TAG, "Fetching Firestore data for appointment: " + appointmentId);
            
            // Get the appointment from Firestore to check for tags
            db.collection("appointments")
                    .document(appointmentId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Check if the document has tags
                            Map<String, Object> data = documentSnapshot.getData();
                            Map<String, Object> tagData = data != null ? (Map<String, Object>) data.get("tags") : null;
                            
                            // First, clear any existing tags to avoid duplicates or stale data
                            apiAppointment.setTags(null);
                            
                            if (tagData != null && !tagData.isEmpty()) {
                                Log.d(TAG, "Found " + tagData.size() + " tags in Firestore for appointment: " + appointmentId);
                                
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
                                    Log.d(TAG, "Set " + tags.size() + " tags on appointment: " + appointmentId);
                                }
                            } else {
                                Log.d(TAG, "No tags found in Firestore for appointment: " + appointmentId);
                            }
                        } else {
                            Log.d(TAG, "Appointment document not found in Firestore: " + appointmentId);
                        }
                        
                        // Add the enriched appointment to our list
                        enrichedAppointments.add(apiAppointment);
                        
                        // Check if we've processed all appointments
                        appointmentsProcessed[0]++;
                        if (appointmentsProcessed[0] >= apiAppointments.size()) {
                            // Sort if needed and display
                            Log.d(TAG, "All appointments processed with Firestore data. Displaying updated list.");
                            displayAppointments(enrichedAppointments);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching appointment from Firestore: " + appointmentId, e);
                        
                        // Still add the original appointment without tags
                        enrichedAppointments.add(apiAppointment);
                        
                        // Check if we've processed all appointments
                        appointmentsProcessed[0]++;
                        if (appointmentsProcessed[0] >= apiAppointments.size()) {
                            Log.d(TAG, "Finished processing appointments with some errors.");
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
            return;
        }

        // Store current appointments for the timer
        currentAppointments = appointments;
        
        Log.d(TAG, "Displaying " + appointments.size() + " appointments");
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Appointment appointment : appointments) {
            // Log appointment details for debugging
            Log.d(TAG, String.format("Processing appointment - ID: %s, Title: %s", 
                appointment.getIdValue(), appointment.getTitleValue()));
            
            // Log tags for debugging
            Map<String, Tag> tags = appointment.getTags();
            if (tags != null && !tags.isEmpty()) {
                Log.d(TAG, "Appointment has " + tags.size() + " tags:");
                for (Map.Entry<String, Tag> entry : tags.entrySet()) {
                    Log.d(TAG, "  - " + entry.getKey() + " (Color: " + entry.getValue().getColor() + ")");
                }
            } else {
                Log.d(TAG, "Appointment has no tags");
            }

            if (appointment.getIdValue() == null) {
                Log.e(TAG, "Found appointment with null ID - Title: " + appointment.getTitleValue());
                continue;
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

            // Set appointment details
            titleText.setText(appointment.getTitleValue());
            
            // Show description or default message
            String description = appointment.getDescriptionValue() != null ? 
                appointment.getDescriptionValue() : "No description available";
            descriptionText.setText(description);
            
            // Show detailed status
            String detailedStatus = "Status: " + appointment.getStatusValue();
            if (appointment.getStartTimeValue() != null) {
                detailedStatus += "\nStart: " + appointment.getFormattedDateTime();
            }
            statusText.setText(detailedStatus);
            
            // Display tags if available
            displayTags(appointment, tagsContainer);

            // Store the reference to timeRemainingText for countdown updates
            if (appointment.getIdValue() != null) {
                countdownTextViews.put(appointment.getIdValue(), timeRemainingText);
            }
            
            // Set initial countdown value
            updateCountdownForAppointment(appointment, timeRemainingText);

            viewAllButton.setOnClickListener(v -> {
                // Ensure appointment has all data (including tags) before navigation
                Log.d(TAG, "Navigating to AppointmentDetailsActivity for appointment: " + appointment.getIdValue());
                if (appointment.getTags() != null) {
                    Log.d(TAG, "Appointment has " + appointment.getTags().size() + " tags");
                } else {
                    Log.d(TAG, "Appointment has no tags");
                }
                
                // Navigate to AppointmentDetailsActivity
                Intent intent = new Intent(HomePage.this, AppointmentDetailsActivity.class);
                intent.putExtra("APPOINTMENT_PARCEL", appointment);
                intent.putExtra("APPOINTMENT_ID", appointment.getIdValue()); // Add ID separately for safety
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
            String appointmentId = appointment.getIdValue();
            if (appointmentId == null) continue;
            
            TextView timerTextView = countdownTextViews.get(appointmentId);
            if (timerTextView != null) {
                updateCountdownForAppointment(appointment, timerTextView);
            }
        }
    }
    
    private void updateCountdownForAppointment(Appointment appointment, TextView timerTextView) {
        com.google.firebase.Timestamp startTimeTs = appointment.getStartTimeValue();
        if (startTimeTs != null) {
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
    }
    
    /**
     * Display tags for an appointment in the provided container
     */
    private void displayTags(Appointment appointment, LinearLayout tagsContainer) {
        // Clear existing views first
        tagsContainer.removeAllViews();
        
        // Get the tags from the appointment
        Map<String, Tag> tags = appointment.getTags();
        Log.d(TAG, "displayTags for appointment: " + appointment.getIdValue());
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
            
            Log.d(TAG, "Adding tag to UI: " + tagName + ", Color: " + tag.getColor());
            
            try {
                // Inflate tag chip
                TextView tagChip = (TextView) inflater.inflate(
                    R.layout.item_tag_chip, tagsContainer, false);
                
                // Set tag name
                tagChip.setText(tagName);
                
                // Parse the color
                int textColor = android.graphics.Color.parseColor(tag.getColor());
                
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
} 