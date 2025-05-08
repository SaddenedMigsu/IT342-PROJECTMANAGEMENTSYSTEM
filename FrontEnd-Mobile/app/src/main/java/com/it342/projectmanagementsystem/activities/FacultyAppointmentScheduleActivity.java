package com.it342.projectmanagementsystem.activities;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.applandeo.materialcalendarview.listeners.OnDayClickListener;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.adapters.AppointmentAdapter;
import com.it342.projectmanagementsystem.api.ApiService;
import com.it342.projectmanagementsystem.api.RetrofitClient;
import com.it342.projectmanagementsystem.models.Appointment;
import com.it342.projectmanagementsystem.models.Tag;
import com.it342.projectmanagementsystem.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

// Use Firebase Timestamp
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

public class FacultyAppointmentScheduleActivity extends AppCompatActivity implements AppointmentAdapter.OnAppointmentClickListener {
    private static final String TAG = "FacultyAppointmentSchedule";
    
    private CalendarView calendarView;
    private TextView tvAppointmentsToday;
    private RecyclerView rvAppointments;
    private TextView tvNoAppointments;
    
    private ApiService apiService;
    private String facultyId;
    private List<Appointment> allAppointments = new ArrayList<>();
    private Map<Calendar, List<Appointment>> appointmentsByDate = new HashMap<>();
    private AppointmentAdapter appointmentAdapter;
    
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
    
    // Add a calendar variable to store currently selected date
    private Calendar selectedCalendar = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_appointment_schedule);
        
        // Set the timezone for date formatters
        dateFormatter.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
        timeFormatter.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
        
        // Initialize API service
        apiService = RetrofitClient.getInstance().getApiService();
        
        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        facultyId = prefs.getString(Constants.KEY_USER_ID, "");
        
        // Initialize views
        initViews();
        
        // Setup calendar
        setupCalendar();
        
        // Load appointments with slight delay to ensure UI is ready
        new android.os.Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                // Load appointments
                loadFacultyAppointments();
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh calendar and appointments when returning to the screen
        if (calendarView != null) {
            new android.os.Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    loadFacultyAppointments();
                }
            });
        }
    }
    
    private void initViews() {
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        
        calendarView = findViewById(R.id.calendarView);
        tvAppointmentsToday = findViewById(R.id.tvAppointmentsToday);
        rvAppointments = findViewById(R.id.rvAppointments);
        tvNoAppointments = findViewById(R.id.tvNoAppointments);
        
        // Set up RecyclerView and adapter
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        appointmentAdapter = new AppointmentAdapter(this, this);
        rvAppointments.setAdapter(appointmentAdapter);
    }
    
    private void setupCalendar() {
        // Set day click listener
        calendarView.setOnDayClickListener(new OnDayClickListener() {
            @Override
            public void onDayClick(EventDay eventDay) {
                Calendar selectedDate = eventDay.getCalendar();
                Log.d(TAG, "Day clicked: " + dateFormatter.format(selectedDate.getTime()));
                updateAppointmentsForDate(selectedDate);
            }
        });
    }
    
    private void loadFacultyAppointments() {
        if (facultyId.isEmpty()) {
            Toast.makeText(this, "Faculty ID not found. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Loading appointments for faculty ID: " + facultyId);
        
        // Show loading indicator or message
        tvNoAppointments.setText("Loading appointments...");
        tvNoAppointments.setVisibility(View.VISIBLE);
        
        // First try to get appointments from the API
        loadAppointmentsFromAPI();
    }
    
    private void loadAppointmentsFromAPI() {
        // Get auth token from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");
        
        if (token.isEmpty()) {
            Log.e(TAG, "Auth token not found");
            // Fallback to Firebase if token is not available
            loadAppointmentsFromFirebase();
            return;
        }
        
        // Make API call to get faculty appointments using getUserAppointments
        Call<List<Appointment>> call = apiService.getUserAppointments(facultyId, "Bearer " + token);
        
        call.enqueue(new Callback<List<Appointment>>() {
            @Override
            public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Appointment> appointments = response.body();
                    Log.d(TAG, "Received " + appointments.size() + " appointments from API");
                    
                    if (!appointments.isEmpty()) {
                        allAppointments = new ArrayList<>(appointments); // Use a mutable copy
                        // Now fetch tags for these appointments from Firestore.
                        // fetchTagsFromFirestore will eventually call processAppointments.
                        fetchTagsFromFirestore();
                    } else {
                        Log.d(TAG, "No appointments found for faculty via API. Falling back to Firestore.");
                        // Fallback to Firestore if API returns no appointments
                        loadAppointmentsFromFirebase();
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? 
                            response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "Failed to load appointments from API: " + errorBody);
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body from API response", e);
                    }
                    
                    // Fallback to Firebase if API fails
                    Log.d(TAG, "API call failed or returned error. Falling back to Firestore.");
                    loadAppointmentsFromFirebase();
                }
            }
            
            @Override
            public void onFailure(Call<List<Appointment>> call, Throwable t) {
                Log.e(TAG, "Error fetching appointments from API", t);
                // Fallback to Firebase if API call fails
                Log.d(TAG, "API call failed due to network or other issue. Falling back to Firestore.");
                loadAppointmentsFromFirebase();
            }
        });
    }
    
    private void loadAppointmentsFromFirebase() {
        Log.d(TAG, "Loading appointments from Firebase");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("appointments")
            .whereEqualTo("facultyId", facultyId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Appointment> appointments = new ArrayList<>();
                
                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                    Appointment appointment = document.toObject(Appointment.class);
                    if (appointment != null) {
                        // Ensure ID is set
                        if (appointment.getId() == null) {
                            appointment.setId(document.getId());
                        }
                        
                        // Don't process tags here - we'll do it in a separate step
                        appointments.add(appointment);
                    }
                }
                
                Log.d(TAG, "Received " + appointments.size() + " appointments from Firestore");
                
                if (!appointments.isEmpty()) {
                    allAppointments = appointments;
                    // Now fetch and add tags separately
                    fetchTagsFromFirestore();
                } else {
                    Log.d(TAG, "No appointments found in Firestore");
                    // Display empty state
                    processAppointments(new ArrayList<>());
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading appointments from Firestore", e);
                // Display empty state
                processAppointments(new ArrayList<>());
            });
    }
    
    /**
     * Fetch tags directly from Firestore for each appointment
     * This matches the approach used in HomePage for consistency
     */
    private void fetchTagsFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Appointment> enrichedAppointments = new ArrayList<>();
        
        // Process each appointment one by one
        for (Appointment appointment : allAppointments) {
            // Get the appointment document directly by ID
            String appointmentId = appointment.getId() != null ? appointment.getId() : appointment.getAppointmentId();
            
            if (appointmentId == null) {
                Log.e(TAG, "Cannot fetch tags for appointment without ID");
                enrichedAppointments.add(appointment);
                continue;
            }
            
            // Get the document data to extract tags
            db.collection("appointments").document(appointmentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Log all available fields for debugging
                        Log.d(TAG, "Firestore data for appointment: " + appointmentId + 
                             " - Fields: " + documentSnapshot.getData().keySet());
                        
                        // Check for and set creatorName if missing or unknown
                        String currentCreatorName = appointment.getCreatorName();
                        if (currentCreatorName == null || currentCreatorName.isEmpty() || 
                            currentCreatorName.contains("Unknown")) {
                            
                            // Try to get creatorName directly from Firestore
                            String firebaseCreatorName = documentSnapshot.getString("creatorName");
                            if (firebaseCreatorName != null && !firebaseCreatorName.isEmpty()) {
                                appointment.setCreatorName(firebaseCreatorName);
                                Log.d(TAG, "Set creatorName from Firestore: " + firebaseCreatorName + 
                                    " for appointment: " + appointmentId);
                            } else {
                                // If no creatorName, try to get createdBy
                                String createdBy = documentSnapshot.getString("createdBy");
                                if (createdBy != null && !createdBy.isEmpty()) {
                                    // If createdBy is an email, extract username part
                                    if (createdBy.contains("@")) {
                                        String username = createdBy.split("@")[0];
                                        // Format username nicely (replace dots with spaces, capitalize words)
                                        username = username.replace(".", " ");
                                        StringBuilder formattedName = new StringBuilder();
                                        for (String part : username.split(" ")) {
                                            if (!part.isEmpty()) {
                                                formattedName.append(Character.toUpperCase(part.charAt(0)))
                                                    .append(part.substring(1))
                                                    .append(" ");
                                            }
                                        }
                                        appointment.setCreatorName(formattedName.toString().trim());
                                        Log.d(TAG, "Set creatorName from createdBy email: " + 
                                            formattedName.toString().trim() + " for appointment: " + appointmentId);
                                    } else {
                                        // Use createdBy as is
                                        appointment.setCreatorName(createdBy);
                                        Log.d(TAG, "Set creatorName to createdBy: " + createdBy + 
                                            " for appointment: " + appointmentId);
                                    }
                                }
                            }
                        }
                        
                        // Check for tags field
                        Map<String, Object> tagsMap = (Map<String, Object>) documentSnapshot.get("tags");
                        if (tagsMap != null && !tagsMap.isEmpty()) {
                            Log.d(TAG, "Found " + tagsMap.size() + " tags in Firestore for appointment: " + appointmentId);
                            
                            // Process tags from Firestore
                            for (Map.Entry<String, Object> entry : tagsMap.entrySet()) {
                                String tagKey = entry.getKey();
                                Map<String, Object> tagData = (Map<String, Object>) entry.getValue();
                                
                                String tagName = tagKey;
                                String tagColor = "#FF0000"; // Default red color
                                
                                if (tagData.containsKey("name")) {
                                    tagName = (String) tagData.get("name");
                                }
                                
                                if (tagData.containsKey("color")) {
                                    tagColor = (String) tagData.get("color");
                                }
                                
                                // Create tag and add to appointment
                                Tag tag = new Tag();
                                tag.setNameForJava(tagName);
                                tag.setColorForJava(tagColor);
                                appointment.addTag(tagKey, tag);
                                
                                Log.d(TAG, "Added tag from Firestore: " + tagName + ", Color: " + tagColor);
                            }
                            
                            Log.d(TAG, "Set " + tagsMap.size() + " tags on appointment: " + appointmentId);
                        } else {
                            // Fallback to special tag for Mr. Marck's appointments
                            if (appointment.getTitle() != null && 
                                appointment.getTitle().contains("Marck Ramon")) {
                                Tag loveMarckTag = new Tag();
                                loveMarckTag.setNameForJava("I Love Mr Marck");
                                loveMarckTag.setColorForJava("#FF0000");
                                appointment.addTag("I Love Mr Marck", loveMarckTag);
                                Log.d(TAG, "Added special 'I Love Mr Marck' tag to: " + appointmentId);
                            }
                        }
                    } else {
                        Log.d(TAG, "Appointment document not found in Firestore: " + appointmentId);
                    }
                    
                    // Add the processed appointment to our list
                    enrichedAppointments.add(appointment);
                    Log.d(TAG, "Added appointment to enriched list: " + appointmentId + " - " + appointment.getTitle());
                    
                    // If all appointments have been processed, update the UI
                    if (enrichedAppointments.size() == allAppointments.size()) {
                        Log.d(TAG, "All appointments processed with Firestore data. Displaying updated list with " + 
                             enrichedAppointments.size() + " appointments.");
                        allAppointments = enrichedAppointments;
                        processAppointments(allAppointments);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching tags for appointment: " + appointmentId, e);
                    // Still add the appointment even if tag fetch fails
                    enrichedAppointments.add(appointment);
                    
                    // If all appointments have been processed, update the UI
                    if (enrichedAppointments.size() == allAppointments.size()) {
                        allAppointments = enrichedAppointments;
                        processAppointments(allAppointments);
                    }
                });
        }
    }
    
    private void createTestAppointmentWithMarckRamon() {
        Log.d(TAG, "Creating test appointment with Marck Ramon");
        allAppointments = new ArrayList<>();
        
        // Create a test appointment for April 30th with Marck Ramon
        Appointment marckAppointment = new Appointment();
        marckAppointment.setId("marck-ramon-appointment");
        marckAppointment.setTitle("Meeting with Marck Ramon");
        marckAppointment.setDescription("Meeting with Mr. C\nReason: Need help for our Capstone");
        // No location as per requirement
        marckAppointment.setStatus("PENDING_APPROVAL");
        marckAppointment.setFacultyId(facultyId);
        marckAppointment.setCreatorName("Marck Ramon");
        
        // Set the start and end time for April 30th, 9:00 AM - 10:00 AM
        Calendar aprilCal = Calendar.getInstance();
        aprilCal.set(2025, Calendar.APRIL, 30);
        
        Calendar startTime = (Calendar) aprilCal.clone();
        startTime.set(Calendar.HOUR_OF_DAY, 9);
        startTime.set(Calendar.MINUTE, 0);
        startTime.set(Calendar.SECOND, 0);
        marckAppointment.setStartTime(new Timestamp(startTime.getTime()));
        
        Calendar endTime = (Calendar) aprilCal.clone();
        endTime.set(Calendar.HOUR_OF_DAY, 10);
        endTime.set(Calendar.MINUTE, 0);
        endTime.set(Calendar.SECOND, 0);
        marckAppointment.setEndTime(new Timestamp(endTime.getTime()));
        
        // Add the "I Love Mr Marck" tag directly
        Tag loveMarckTag = new Tag();
        loveMarckTag.setNameForJava("I Love Mr Marck");
        loveMarckTag.setColorForJava("#FF0000");
        marckAppointment.addTag("I Love Mr Marck", loveMarckTag);
        Log.d(TAG, "Added 'I Love Mr Marck' tag to test appointment");
        
        allAppointments.add(marckAppointment);
        
        // Add a completed appointment for April 16th
        Appointment completedAppointment = new Appointment();
        completedAppointment.setId("completed-appointment");
        completedAppointment.setTitle("Meeting with Marck Ramon");
        completedAppointment.setDescription("Meeting with Mr. C\nReason: Because he is Mr. C");
        completedAppointment.setStatus("SCHEDULED"); // This will auto-show as completed because time has passed
        completedAppointment.setFacultyId(facultyId);
        completedAppointment.setCreatorName("Marck Ramon");
        
        // Set the start and end time for April 16th, 8:30 AM - 9:30 AM (in the past)
        Calendar april16Cal = Calendar.getInstance();
        april16Cal.set(2025, Calendar.APRIL, 16);
        
        Calendar april16StartTime = (Calendar) april16Cal.clone();
        april16StartTime.set(Calendar.HOUR_OF_DAY, 8);
        april16StartTime.set(Calendar.MINUTE, 30);
        april16StartTime.set(Calendar.SECOND, 0);
        completedAppointment.setStartTime(new Timestamp(april16StartTime.getTime()));
        
        Calendar april16EndTime = (Calendar) april16Cal.clone();
        april16EndTime.set(Calendar.HOUR_OF_DAY, 9);
        april16EndTime.set(Calendar.MINUTE, 30);
        april16EndTime.set(Calendar.SECOND, 0);
        completedAppointment.setEndTime(new Timestamp(april16EndTime.getTime()));
        
        // Add the same "I Love Mr Marck" tag to the second appointment
        completedAppointment.addTag("I Love Mr Marck", loveMarckTag);
        
        allAppointments.add(completedAppointment);
        
        // Process the appointments directly without fetching tags
        processAppointments(allAppointments);
    }
    
    private void processAppointments(List<Appointment> appointments) {
        appointmentsByDate.clear();
        List<EventDay> events = new ArrayList<>();
        
        Log.d(TAG, "Processing " + appointments.size() + " appointments");
        
        for (Appointment appointment : appointments) {
            Timestamp startTimestamp = appointment.getStartTime();
            if (startTimestamp != null) {
                Date startDate = startTimestamp.toDate();
                Calendar cal = Calendar.getInstance();
                cal.setTime(startDate);
                
                // Clear time portion to match dates only
                Calendar dateKey = Calendar.getInstance();
                dateKey.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
                dateKey.set(Calendar.MILLISECOND, 0);
                
                if (!appointmentsByDate.containsKey(dateKey)) {
                    appointmentsByDate.put(dateKey, new ArrayList<>());
                }
                appointmentsByDate.get(dateKey).add(appointment);
                
                // Create a clean calendar instance with only the date part for the event
                Calendar eventCal = Calendar.getInstance();
                eventCal.clear();
                eventCal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
                
                Log.d(TAG, "Adding event for date: " + dateFormatter.format(eventCal.getTime()) + 
                      " - Appointment: " + appointment.getTitle() + 
                      " (ID: " + appointment.getAppointmentId() + ")");
                events.add(new EventDay(eventCal, R.drawable.ic_event_marker));
            }
        }
        
        // Add events to calendar
        Log.d(TAG, "Setting " + events.size() + " events on calendar");
        calendarView.setEvents(events);
        
        // Force refresh the calendar view
        calendarView.refreshDrawableState();
        
        // Update UI for today
        Calendar today = Calendar.getInstance();
        updateAppointmentsForDate(today);
    }
    
    private void updateAppointmentsForDate(Calendar day) {
        // Save reference to selected calendar
        selectedCalendar = day;
        
        // Clear time portion to match dates only
        Calendar dateKey = Calendar.getInstance();
        dateKey.set(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        dateKey.set(Calendar.MILLISECOND, 0);
        
        List<Appointment> appointmentsForDay = appointmentsByDate.getOrDefault(dateKey, new ArrayList<>());
        
        // Update header
        String formattedDate = dateFormatter.format(day.getTime());
        tvAppointmentsToday.setText("Appointments for " + formattedDate);
        
        // Update adapter - important to properly update without recreating the adapter
        // This prevents timers from being reset
        appointmentAdapter.setAppointments(appointmentsForDay);
        
        // Show/hide no appointments message
        if (appointmentsForDay.isEmpty()) {
            tvNoAppointments.setText("No Appointments Today");
            tvNoAppointments.setVisibility(View.VISIBLE);
            rvAppointments.setVisibility(View.GONE);
        } else {
            tvNoAppointments.setVisibility(View.GONE);
            rvAppointments.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAppointmentClick(Appointment appointment) {
        showAppointmentDetailsDialog(appointment);
    }
    
    private void showAppointmentDetailsDialog(Appointment appointment) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_appointment_details);
        
        // Initialize dialog views
        TextView tvTitle = dialog.findViewById(R.id.tvDialogAppointmentTitle);
        TextView tvDateTime = dialog.findViewById(R.id.tvDialogDateTime);
        TextView tvLocation = dialog.findViewById(R.id.tvDialogLocation);
        TextView tvParticipants = dialog.findViewById(R.id.tvDialogParticipants);
        TextView tvDescription = dialog.findViewById(R.id.tvDialogDescription);
        Button btnClose = dialog.findViewById(R.id.btnCloseDialog);
        Button btnViewFull = dialog.findViewById(R.id.btnViewFullDetails);
        
        // Hide the Full Details button
        btnViewFull.setVisibility(View.GONE);
        
        // Set appointment details
        tvTitle.setText(appointment.getTitle());
        
        // Format date and time
        String dateTimeStr = "Date and Time: ";
        if (appointment.getStartTime() != null && appointment.getEndTime() != null) {
            Date startDate = appointment.getStartTime().toDate();
            Date endDate = appointment.getEndTime().toDate();
            String date = dateFormatter.format(startDate);
            String startTimeStr = timeFormatter.format(startDate);
            String endTimeStr = timeFormatter.format(endDate);
            dateTimeStr += date + " " + startTimeStr + " - " + endTimeStr;
        } else {
            dateTimeStr += "Not specified";
        }
        tvDateTime.setText(dateTimeStr);
        
        // Hide location as per requirement
        tvLocation.setVisibility(View.GONE);
        
        // Set participants
        String requesterName = appointment.getCreatorName();
        if (requesterName != null && !requesterName.isEmpty()) {
            tvParticipants.setText("Requested by: " + requesterName);
        } else {
            tvParticipants.setText("No requester information");
        }
        
        // Set description
        String description = appointment.getDescription();
        if (description != null && !description.isEmpty()) {
            tvDescription.setText(description);
        } else {
            tvDescription.setText("No description provided");
        }
        
        // Set button listeners
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // Helper methods for Date to Firebase Timestamp conversion
    private Timestamp dateToFirebaseTimestamp(Date date) {
        if (date == null) return null;
        return new Timestamp(date.getTime() / 1000, 0);
    }

    private Date firebaseTimestampToDate(Timestamp timestamp) {
        if (timestamp == null) return null;
        return timestamp.toDate();
    }

    // Method to convert selected date to Timestamp for appointment
    private Timestamp getSelectedDateAsTimestamp() {
        if (selectedCalendar == null) return null;
        return dateToFirebaseTimestamp(selectedCalendar.getTime());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up timers to prevent memory leaks
        if (appointmentAdapter != null) {
            appointmentAdapter.onDestroy();
        }
    }

    /**
     * Display tags for an appointment in the provided container
     * This method is copied from HomePage.java to keep consistency between student and faculty views
     */
    public void displayTags(Appointment appointment, LinearLayout tagsContainer) {
        Log.d(TAG, "==== DISPLAYING TAGS START ====");
        // Clear existing views first
        tagsContainer.removeAllViews();
        
        if (appointment == null) {
            Log.e(TAG, "Appointment is null in displayTags");
            return;
        }
        
        // Get the tags from the appointment
        Map<String, Tag> tags = null;
        try {
            tags = appointment.getTags();
            Log.d(TAG, "Got tags from appointment.getTags(): " + (tags != null ? tags.size() : "null"));
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
        Log.d(TAG, "Tags map size: " + (tags != null ? tags.size() : "null"));
        
        if (appointment.toString() != null) {
            Log.d(TAG, "Appointment details: " + appointment.toString());
        }
        
        // Always make the container visible
        tagsContainer.setVisibility(View.VISIBLE);
        
        // If tags is null or empty, just return - no emergency tags
        if (tags == null || tags.isEmpty()) {
            Log.d(TAG, "No tags to display for this appointment");
            return;
        }
        
        LayoutInflater inflater = LayoutInflater.from(this);
        
        for (Map.Entry<String, Tag> entry : tags.entrySet()) {
            String tagName = entry.getKey();
            Tag tag = entry.getValue();
            
            if (tag == null) {
                Log.e(TAG, "Tag object is null for key: " + tagName);
                continue;
            }
            
            String colorValue = "#000000"; // Default black if color can't be retrieved
            try {
                colorValue = tag.getColorForJava();
                Log.d(TAG, "Got color for tag: " + tagName + ", color: " + colorValue);
            } catch (Exception e) {
                Log.e(TAG, "Error getting tag color with getColorForJava()", e);
                try {
                    // Try alternate accessor method names if the first one fails
                    colorValue = tag.getColor();
                    Log.d(TAG, "Got color using fallback method: " + colorValue);
                } catch (Exception ex) {
                    // Use default
                    Log.e(TAG, "Error getting tag color with getColor()", ex);
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
        Log.d(TAG, "==== DISPLAYING TAGS END ====");
    }
} 