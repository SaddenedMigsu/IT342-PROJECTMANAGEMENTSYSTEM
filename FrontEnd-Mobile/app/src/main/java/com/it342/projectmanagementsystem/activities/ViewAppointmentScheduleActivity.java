package com.it342.projectmanagementsystem.activities;

import android.content.Intent;
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
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.applandeo.materialcalendarview.listeners.OnDayClickListener;
import com.it342.projectmanagementsystem.adapters.AppointmentAdapter;
import com.it342.projectmanagementsystem.models.Appointment;
import com.it342.projectmanagementsystem.models.Tag;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ViewAppointmentScheduleActivity extends AppCompatActivity implements AppointmentAdapter.OnAppointmentClickListener {
    private static final String TAG = "ViewAppointmentSchedule";
    private RecyclerView rvAppointmentSchedule;
    private ApiService apiService;
    private CalendarView calendarView;
    private TextView tvNoAppointments;
    private AppointmentAdapter appointmentAdapter;
    private List<Appointment> allAppointments = new ArrayList<>();
    private Map<Calendar, List<Appointment>> appointmentsByDate = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_appointment_schedule);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        
        // Initialize views
        initializeViews();

        // Initialize API service
        apiService = RetrofitClient.getInstance().getApiService();

        // Setup calendar
        setupCalendar();

        // Load appointment schedule
        loadAppointmentSchedule();
    }

    private void initializeViews() {
        rvAppointmentSchedule = findViewById(R.id.rvAppointmentSchedule);
        rvAppointmentSchedule.setLayoutManager(new LinearLayoutManager(this));
        
        calendarView = findViewById(R.id.calendarView);
        tvNoAppointments = findViewById(R.id.tvNoAppointments);
        
        // Set up adapter
        appointmentAdapter = new AppointmentAdapter(this, this);
        rvAppointmentSchedule.setAdapter(appointmentAdapter);
    }
    
    private void setupCalendar() {
        calendarView.setOnDayClickListener(new OnDayClickListener() {
            @Override
            public void onDayClick(EventDay eventDay) {
                Calendar selectedDate = eventDay.getCalendar();
                updateAppointmentsForDate(selectedDate);
            }
        });
    }

    private void loadAppointmentSchedule() {
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");
        String userId = prefs.getString("userId", "");

        if (token.isEmpty() || userId.isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Show loading indicator
        tvNoAppointments.setText("Loading appointments...");
        tvNoAppointments.setVisibility(View.VISIBLE);
        
        // Try to load from Firebase first
        loadAppointmentsFromFirebase(userId);
    }
    
    /**
     * Load appointments from Firebase
     */
    private void loadAppointmentsFromFirebase(String userId) {
        // Get Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Clear any existing appointments
        allAppointments.clear();
        
        Log.d(TAG, "Loading appointments for user: " + userId);
        
        // Query appointments where the user is a participant
        db.collection("appointments")
            .whereArrayContains("participants", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    Log.d(TAG, "No appointments found in Firestore");
                    // No appointments found, create sample ones
                    createSampleAppointments();
                    return;
                }
                
                Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " appointments");
                
                // Process appointments from Firestore
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    try {
                        // Convert the document to an Appointment
                        Appointment appointment = document.toObject(Appointment.class);
                        if (appointment != null) {
                            // Ensure the ID is set
                            if (appointment.getId() == null) {
                                appointment.setId(document.getId());
                            }
                            
                            // Log appointment details
                            Log.d(TAG, "Loaded appointment: " + appointment.getTitle() + 
                                  ", Status: " + appointment.getStatus());
                            
                            // Add to the list
                            allAppointments.add(appointment);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing appointment document", e);
                    }
                }
                
                // Now fetch tags for these appointments
                fetchTagsForAppointments();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching appointments", e);
                // Fall back to sample appointments
                createSampleAppointments();
            });
    }
    
    /**
     * Fetch tags for the appointments from Firestore
     */
    private void fetchTagsForAppointments() {
        if (allAppointments.isEmpty()) {
            Log.d(TAG, "No appointments to fetch tags for");
            processAppointments(allAppointments);
            return;
        }
        
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
    
    // Create sample appointments for testing
    private void createSampleAppointments() {
        allAppointments = new ArrayList<>();
        
        // Sample appointment for today
        Appointment todayApp = new Appointment();
        todayApp.setId("1");
        todayApp.setTitle("Project Review Meeting");
        todayApp.setDescription("Review progress on the semester project");
        todayApp.setLocation("Room 101");
        todayApp.setStatus("ACCEPTED");
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 0);
        long startTimeMillis = calendar.getTimeInMillis();
        Timestamp startTime = new Timestamp(startTimeMillis / 1000, 0);
        todayApp.setStartTime(startTime);
        
        calendar.set(Calendar.HOUR_OF_DAY, 11);
        calendar.set(Calendar.MINUTE, 0);
        long endTimeMillis = calendar.getTimeInMillis();
        Timestamp endTime = new Timestamp(endTimeMillis / 1000, 0);
        todayApp.setEndTime(endTime);
        
        // Add sample tags to today's appointment
        addSampleTags(todayApp);
        
        allAppointments.add(todayApp);
        
        // Sample appointment for tomorrow
        Appointment tomorrowApp = new Appointment();
        tomorrowApp.setId("2");
        tomorrowApp.setTitle("Thesis Discussion");
        tomorrowApp.setDescription("Discussion about thesis progress");
        tomorrowApp.setLocation("Faculty Office");
        tomorrowApp.setStatus("ACCEPTED");
        
        Calendar tomorrowCalendar = Calendar.getInstance();
        tomorrowCalendar.add(Calendar.DAY_OF_MONTH, 1);
        tomorrowCalendar.set(Calendar.HOUR_OF_DAY, 14);
        tomorrowCalendar.set(Calendar.MINUTE, 0);
        long tomorrowStartMillis = tomorrowCalendar.getTimeInMillis();
        Timestamp tomorrowStartTime = new Timestamp(tomorrowStartMillis / 1000, 0);
        tomorrowApp.setStartTime(tomorrowStartTime);
        
        tomorrowCalendar.set(Calendar.HOUR_OF_DAY, 15);
        tomorrowCalendar.set(Calendar.MINUTE, 0);
        long tomorrowEndMillis = tomorrowCalendar.getTimeInMillis();
        Timestamp tomorrowEndTime = new Timestamp(tomorrowEndMillis / 1000, 0);
        tomorrowApp.setEndTime(tomorrowEndTime);
        
        // Add sample tags to tomorrow's appointment
        addSampleTags(tomorrowApp);
        
        allAppointments.add(tomorrowApp);
        
        // Process and display appointments
        processAppointments(allAppointments);
    }
    
    /**
     * Helper method to add sample tags to an appointment
     */
    private void addSampleTags(Appointment appointment) {
        try {
            // Create a sample tag with red color
            Tag projectTag = new Tag();
            projectTag.setNameForJava("Project");
            projectTag.setColorForJava("#FF5722");
            appointment.addTag("Project", projectTag);
            
            // Create a sample tag with blue color
            Tag urgentTag = new Tag();
            urgentTag.setNameForJava("Urgent");
            urgentTag.setColorForJava("#2196F3");
            appointment.addTag("Urgent", urgentTag);
            
            Log.d(TAG, "Added sample tags to appointment: " + appointment.getId());
        } catch (Exception e) {
            Log.e(TAG, "Error adding sample tags to appointment", e);
        }
    }
    
    private void processAppointments(List<Appointment> appointments) {
        appointmentsByDate.clear();
        List<EventDay> events = new ArrayList<>();
        
        Log.d(TAG, "Processing " + appointments.size() + " appointments for calendar");
        
        for (Appointment appointment : appointments) {
            Timestamp startTimestamp = appointment.getStartTime();
            if (startTimestamp != null) {
                Date startDate = startTimestamp.toDate();
                Calendar cal = Calendar.getInstance();
                cal.setTime(startDate);
                
                Log.d(TAG, "Adding event for date: " + cal.getTime().toString() + ", Title: " + appointment.getTitle());
                
                // Clear time portion to match dates only
                Calendar dateKey = Calendar.getInstance();
                dateKey.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
                dateKey.set(Calendar.MILLISECOND, 0);
                
                if (!appointmentsByDate.containsKey(dateKey)) {
                    appointmentsByDate.put(dateKey, new ArrayList<>());
                }
                appointmentsByDate.get(dateKey).add(appointment);
                
                // Add event to calendar
                events.add(new EventDay(cal, R.drawable.ic_event_marker));
            }
        }
        
        // Add events to calendar
        Log.d(TAG, "Setting " + events.size() + " events on calendar");
        calendarView.setEvents(events);
        
        // Update UI for today
        Calendar today = Calendar.getInstance();
        updateAppointmentsForDate(today);
    }
    
    private void updateAppointmentsForDate(Calendar day) {
        // Clear time portion to match dates only
        Calendar dateKey = Calendar.getInstance();
        dateKey.set(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        dateKey.set(Calendar.MILLISECOND, 0);
        
        List<Appointment> appointmentsForDay = appointmentsByDate.getOrDefault(dateKey, new ArrayList<>());
        
        // Update adapter
        appointmentAdapter.setAppointments(appointmentsForDay);
        
        // Show/hide no appointments message
        if (appointmentsForDay.isEmpty()) {
            tvNoAppointments.setVisibility(View.VISIBLE);
            rvAppointmentSchedule.setVisibility(View.GONE);
        } else {
            tvNoAppointments.setVisibility(View.GONE);
            rvAppointmentSchedule.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAppointmentClick(Appointment appointment) {
        // Handle appointment click - e.g., show details
        Intent intent = new Intent(this, AppointmentDetailsActivity.class);
        intent.putExtra("APPOINTMENT_PARCEL", appointment);
        intent.putExtra("APPOINTMENT_ID", appointment.getId());
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    /**
     * Display tags for an appointment in the provided container
     * This method is required by the OnAppointmentClickListener interface
     */
    @Override
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