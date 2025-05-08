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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.it342.projectmanagementsystem.PMSApplication;
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
import java.util.Collections;

public class HomePage extends AppCompatActivity {
    private static final String TAG = "HomePage";
    private LinearLayout appointmentsContainer;
    private TextView welcomeText;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private Map<String, TextView> countdownTextViews = new HashMap<>();
    private List<Appointment> currentAppointments;
    private boolean isTimerRunning = false;
    private ImageButton btnNotification;
    private ImageButton btnLogout;
    private TextView tvNotificationBadge;
    private int unreadNotificationCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        initializeViews();
        setupNavigation();
        loadUserData();
        
        // Request notification permissions for Android 13+
        PMSApplication.requestNotificationPermission(this);
        
        // Initialize timer handler for countdown updates - do this before fetching appointments
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                updateAllCountdowns();
                // Schedule next update
                timerHandler.postDelayed(this, 1000); // Update every second
            }
        };
        
        fetchAppointments();
    }

    private void initializeViews() {
        appointmentsContainer = findViewById(R.id.appointmentsContainer);
        welcomeText = findViewById(R.id.welcomeText);
        btnNotification = findViewById(R.id.btnNotification);
        btnLogout = findViewById(R.id.btnLogout);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        
        // Set click listener for notification button
        btnNotification.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            startActivity(intent);
        });
        
        // Set click listener for logout button
        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
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
        String userId = prefs.getString("userId", "");
        String email = prefs.getString("email", "");

        if (token.isEmpty()) {
            Log.e(TAG, "Missing token");
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Fetching all appointments for user: " + email);
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        Call<List<Appointment>> call = apiService.getAppointments("Bearer " + token);

        call.enqueue(new Callback<List<Appointment>>() {
            @Override
            public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Appointment> appointments = response.body();
                    Log.d(TAG, "Received " + appointments.size() + " appointments from API");
                    
                    // If API returns 0 appointments, log this and proceed with Firestore
                    if (appointments.isEmpty()) {
                        Log.w(TAG, "API returned 0 appointments for user: " + email + ". Checking Firestore...");
                    }
                    
                    // Always check Firestore, even if API returns appointments
                    fetchFirestoreAppointments(appointments, userId, email);
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? 
                            response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "Failed to load appointments from API: " + errorBody);
                        Log.e(TAG, "HTTP Status Code: " + response.code());
                        
                        // If API fails, proceed with Firestore
                        Log.d(TAG, "Proceeding with Firestore fetch after API failure");
                        fetchFirestoreAppointments(new ArrayList<>(), userId, email);
                    } catch (IOException e) {
                        e.printStackTrace();
                        fetchFirestoreAppointments(new ArrayList<>(), userId, email);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Appointment>> call, Throwable t) {
                Log.e(TAG, "Error fetching appointments from API", t);
                
                // If API fails, proceed with Firestore
                Log.d(TAG, "Proceeding with Firestore fetch after API failure");
                fetchFirestoreAppointments(new ArrayList<>(), userId, email);
            }
        });
    }
    
    private void fetchFirestoreAppointments(List<Appointment> apiAppointments, String userId, String email) {
        Log.d(TAG, "Fetching appointments directly from Firestore for user email: " + email);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Create a set of existing appointment IDs to avoid duplicates
        Map<String, Appointment> appointmentMap = new HashMap<>();
        for (Appointment appointment : apiAppointments) {
            String id = getAppointmentId(appointment);
            if (id != null) {
                appointmentMap.put(id, appointment);
            }
        }
        
        // Query appointments where user is the creator
        db.collection("appointments")
            .whereEqualTo("createdBy", email)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " appointments created by user");
                
                for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    String appointmentId = document.getId();
                    Log.d(TAG, "Processing appointment: " + appointmentId);
                    
                    try {
                        // Convert Firestore document to Appointment
                        Appointment appointment = document.toObject(Appointment.class);
                        appointment.setId(appointmentId);
                        appointment.setAppointmentId(appointmentId);
                        appointmentMap.put(appointmentId, appointment);
                        Log.d(TAG, "Added appointment: " + appointmentId + " - " + appointment.getTitle());
                    } catch (Exception e) {
                        Log.e(TAG, "Error converting Firestore document to Appointment", e);
                    }
                }
                
                // Query appointments where user is a participant
                db.collection("appointments")
                    .whereArrayContains("participants", email)
                    .get()
                    .addOnSuccessListener(participantAppointments -> {
                        Log.d(TAG, "Found " + participantAppointments.size() + " appointments where user is participant");
                        
                        for (com.google.firebase.firestore.QueryDocumentSnapshot document : participantAppointments) {
                            String appointmentId = document.getId();
                            
                            if (!appointmentMap.containsKey(appointmentId)) {
                                try {
                                    Appointment appointment = document.toObject(Appointment.class);
                                    appointment.setId(appointmentId);
                                    appointment.setAppointmentId(appointmentId);
                                    appointmentMap.put(appointmentId, appointment);
                                    Log.d(TAG, "Added participant appointment: " + appointmentId + " - " + appointment.getTitle());
                                } catch (Exception e) {
                                    Log.e(TAG, "Error converting Firestore document to Appointment", e);
                                }
                            }
                        }
                        
                        // Process all appointments
                        List<Appointment> combinedAppointments = new ArrayList<>(appointmentMap.values());
                        Log.d(TAG, "Total appointments found: " + combinedAppointments.size());
                        
                        if (!combinedAppointments.isEmpty()) {
                            fetchAppointmentsFromFirestore(combinedAppointments);
                        } else {
                            Log.w(TAG, "No appointments found in Firestore");
                            displayAppointments(combinedAppointments);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching participant appointments", e);
                        List<Appointment> combinedAppointments = new ArrayList<>(appointmentMap.values());
                        if (!combinedAppointments.isEmpty()) {
                            fetchAppointmentsFromFirestore(combinedAppointments);
                        } else {
                            displayAppointments(combinedAppointments);
                        }
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching created appointments", e);
                if (!apiAppointments.isEmpty()) {
                    fetchAppointmentsFromFirestore(apiAppointments);
                } else {
                    displayAppointments(apiAppointments);
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
                            apiAppointment.setTags((Map<String, Object>)null);
                            
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
                                    apiAppointment.setTagsFromTagMap(tags);
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
        stopCountdownTimer();
        
        // Filter out past appointments and only keep upcoming ones
        List<Appointment> upcomingAppointments = new ArrayList<>();
        long currentTimeMillis = System.currentTimeMillis();
        
        for (Appointment appointment : appointments) {
            String title = getAppointmentTitle(appointment);
            
            Object startTimeObj = null;
            try {
                try {
                    startTimeObj = appointment.getClass().getMethod("getStartTimeValue").invoke(appointment);
                } catch (Exception e) {
                    try {
                        startTimeObj = appointment.getClass().getMethod("getStartTime").invoke(appointment);
                    } catch (Exception e2) {
                        // Couldn't get start time
                    }
                }
                
                if (startTimeObj != null && startTimeObj instanceof Timestamp) {
                    Timestamp startTimeTs = (Timestamp) startTimeObj;
                    long startTimeMillis = startTimeTs.toDate().getTime();
                    
                    // Include appointments that:
                    // 1. Are in the future (start time > current time)
                    // 2. Started in the last 24 hours (to show appointments that just started or are about to end)
                    // 3. Have a valid status
                    String status = "unknown";
                    try {
                        status = appointment.getStatus();
                    } catch (Exception ex) {
                        try {
                            status = (String) appointment.getClass().getMethod("getStatusValue").invoke(appointment);
                        } catch (Exception ex2) {
                            Log.e(TAG, "Could not determine appointment status", ex2);
                        }
                    }
                    
                    // Don't show rejected appointments
                    if (status == null || !status.toUpperCase().equals("REJECTED")) {
                        // Always include appointments with valid status
                        upcomingAppointments.add(appointment);
                        Log.d(TAG, "Keeping appointment: " + title + 
                              " - Status: " + status + ", Start time: " + startTimeTs.toDate());
                    } else {
                        Log.d(TAG, "Filtering out rejected appointment: " + title);
                    }
                } else {
                    // For appointments with no time, check status
                    String status = "unknown";
                    try {
                        status = appointment.getStatus();
                    } catch (Exception ex) {
                        try {
                            status = (String) appointment.getClass().getMethod("getStatusValue").invoke(appointment);
                        } catch (Exception ex2) {
                            Log.e(TAG, "Could not determine appointment status", ex2);
                        }
                    }
                    
                    // Include non-rejected appointments with unknown time
                    if (status == null || !status.toUpperCase().equals("REJECTED")) {
                        upcomingAppointments.add(appointment);
                        Log.d(TAG, "Keeping appointment with unknown time: " + title +
                              " - Status: " + status);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking appointment time", e);
                // If there's an error, include the appointment to be safe
                upcomingAppointments.add(appointment);
            }
        }
        
        // Sort upcoming appointments by start time (earliest first)
        Collections.sort(upcomingAppointments, (a1, a2) -> {
            long time1 = getAppointmentStartTimeMillis(a1);
            long time2 = getAppointmentStartTimeMillis(a2);
            return Long.compare(time1, time2);
        });
        
        // Use the filtered list of upcoming appointments
        if (upcomingAppointments.isEmpty()) {
            TextView noAppointmentsText = new TextView(this);
            noAppointmentsText.setText("No Upcoming Appointments\n\nYour upcoming appointments will appear here");
            noAppointmentsText.setTextSize(16);
            noAppointmentsText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            noAppointmentsText.setPadding(32, 64, 32, 32);
            appointmentsContainer.addView(noAppointmentsText);
            stopCountdownTimer();
            Log.w(TAG, "No upcoming appointments to display");
            return;
        }

        // Store current appointments for the timer
        currentAppointments = upcomingAppointments;
        
        Log.d(TAG, "Displaying " + upcomingAppointments.size() + " upcoming appointments");
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Appointment appointment : upcomingAppointments) {
            // Log appointment details for debugging
            String appointmentId = getAppointmentId(appointment);
            if (appointmentId == null) {
                Log.e(TAG, "Found appointment with null ID - skipping");
                continue;
            }
            
            String title = getAppointmentTitle(appointment);
            Log.d(TAG, "Processing appointment - ID: " + appointmentId + ", Title: " + title);

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
        Log.d(TAG, "Started countdown timer for " + upcomingAppointments.size() + " appointments");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - Refreshing appointments");
        // Restart the timer if we have appointments
        if (currentAppointments != null && !currentAppointments.isEmpty()) {
            startCountdownTimer();
        }
        fetchAppointments(); // Refresh appointments when returning to the page
        fetchUnreadNotificationsCount(); // Check for unread notifications
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Always stop the timer in onPause
        stopCountdownTimer();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Always stop the timer in onDestroy
        stopCountdownTimer();
    }

    private void startCountdownTimer() {
        if (!isTimerRunning) {
            // Remove any existing callbacks to avoid duplicates
            timerHandler.removeCallbacks(timerRunnable);
            // Post the runnable to start the timer immediately
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
        
        try {
            // Update each appointment's countdown timer
        for (Appointment appointment : currentAppointments) {
            String appointmentId = getAppointmentId(appointment);
            if (appointmentId == null) continue;
            
            TextView timerTextView = countdownTextViews.get(appointmentId);
            if (timerTextView != null) {
                updateCountdownForAppointment(appointment, timerTextView);
            }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in updateAllCountdowns", e);
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
                
                String title = getAppointmentTitle(appointment);
                
                if (diffInMillis > 0) {
                    // Upcoming appointment
                    long days = TimeUnit.MILLISECONDS.toDays(diffInMillis);
                    long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis) % 24;
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60;
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis) % 60;
                    
                    String timeRemaining;
                    if (days > 0) {
                        // Show days and hours for longer periods
                        timeRemaining = String.format(Locale.US, 
                            "%dd %dh %dm %ds remaining", days, hours, minutes, seconds);
                        // Debug log for dynamic timer updates
                        if (seconds % 10 == 0) { // Log every 10 seconds to avoid spam
                            Log.d(TAG, "Timer update for " + title + ": " + timeRemaining);
                        }
                    } else if (hours > 0) {
                        // Show hours and minutes for medium periods
                        timeRemaining = String.format(Locale.US, 
                        "%dh %dm %ds remaining", hours, minutes, seconds);
                        // Debug log for dynamic timer updates
                        if (seconds % 5 == 0) { // Log every 5 seconds
                            Log.d(TAG, "Timer update for " + title + ": " + timeRemaining);
                        }
                    } else {
                        // Show minutes and seconds for short periods
                        timeRemaining = String.format(Locale.US, 
                            "%dm %ds remaining", minutes, seconds);
                        // Debug log for dynamic timer updates
                        Log.d(TAG, "Timer update for " + title + ": " + timeRemaining);
                    }
                    timerTextView.setText(timeRemaining);
                    timerTextView.setTextColor(getResources().getColor(R.color.text_dark));
                } else {
                    // Past appointment or just starting
                    long absDiff = Math.abs(diffInMillis);
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(absDiff);
                    
                    if (minutes < 5) {
                        timerTextView.setText("Starting now");
                        timerTextView.setTextColor(getResources().getColor(R.color.appointment_accepted_text));
                    } else if (minutes < 60) {
                        timerTextView.setText("Started " + minutes + " minutes ago");
                        timerTextView.setTextColor(getResources().getColor(R.color.appointment_pending_text));
                    } else {
                        // Should not appear due to filtering
                        timerTextView.setText("In progress");
                        timerTextView.setTextColor(getResources().getColor(R.color.appointment_pending_text));
                    }
                }
            } else {
                timerTextView.setText("Time not available");
                timerTextView.setTextColor(getResources().getColor(R.color.text_light));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating countdown", e);
            timerTextView.setText("Time not available");
            timerTextView.setTextColor(getResources().getColor(R.color.text_light));
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
        Log.d(TAG, "Display empty appointments instead of test data");
        displayAppointments(new ArrayList<>());
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
     * Helper method to get appointment start time in milliseconds for sorting
     */
    private long getAppointmentStartTimeMillis(Appointment appointment) {
        Object startTimeObj = null;
        try {
            try {
                startTimeObj = appointment.getClass().getMethod("getStartTimeValue").invoke(appointment);
            } catch (Exception e) {
                try {
                    startTimeObj = appointment.getClass().getMethod("getStartTime").invoke(appointment);
                } catch (Exception e2) {
                    // Couldn't get start time
                }
            }
            
            if (startTimeObj != null && startTimeObj instanceof Timestamp) {
                Timestamp startTimeTs = (Timestamp) startTimeObj;
                return startTimeTs.toDate().getTime();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting appointment start time", e);
        }
        
        // If we can't determine the time, return max value (so it sorts to the end)
        return Long.MAX_VALUE;
    }

    /**
     * Format status string for user-friendly display
     */
    private String formatStatusForDisplay(String status) {
        if (status == null) return "Unknown";
        
        switch (status.toUpperCase()) {
            case "PENDING_APPROVAL":
            case "PENDING":
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
            case "PENDING":
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

    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Do you want to log out?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            logout();
        });
        builder.setNegativeButton("No", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.create().show();
    }
    
    private void logout() {
        // Clear user data from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        
        // Navigate to login form
        Intent intent = new Intent(this, LoginFormActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void fetchUnreadNotificationsCount() {
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", "");
        
        if (userId.isEmpty()) {
            Log.e(TAG, "User ID is empty, cannot fetch notifications");
            return;
        }
        
        // Query Firestore for unread notifications
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                unreadNotificationCount = querySnapshot.size();
                updateNotificationBadge();
                Log.d(TAG, "Unread notifications: " + unreadNotificationCount);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching unread notifications", e);
            });
    }
    
    private void updateNotificationBadge() {
        if (unreadNotificationCount > 0) {
            tvNotificationBadge.setVisibility(View.VISIBLE);
            tvNotificationBadge.setText(String.valueOf(unreadNotificationCount));
        } else {
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }
} 