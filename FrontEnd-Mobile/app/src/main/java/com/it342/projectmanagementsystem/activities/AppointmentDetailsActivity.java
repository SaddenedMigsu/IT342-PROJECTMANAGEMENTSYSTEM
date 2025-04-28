package com.it342.projectmanagementsystem.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.api.ApiService;
import com.it342.projectmanagementsystem.api.RetrofitClient;
import com.it342.projectmanagementsystem.helpers.ColorPickerDialog;
import com.it342.projectmanagementsystem.models.Appointment;
import com.it342.projectmanagementsystem.models.Tag;
import com.it342.projectmanagementsystem.models.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AppointmentDetailsActivity extends AppCompatActivity {
    private static final String TAG = "AppointmentDetails";
    private FirebaseFirestore db;
    private ApiService apiService;
    private Appointment appointment;
    private String appointmentId;
    private String currentUserId;
    private String authToken;

    private TextView tvMeetingTitle, tvFacultyName, tvDescription;
    private TextView tvStartTime, tvEndTime, tvStatus, tvParticipantsList;
    private Button btnEdit, btnDelete, btnAddParticipants, btnRemoveParticipants, btnAddCustomTag;
    private ImageButton btnBack;
    private LinearLayout tagsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_details);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        
        // Initialize API Service
        apiService = RetrofitClient.getInstance().getApiService();
        
        // Get user credentials
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("userId", "");
        authToken = "Bearer " + prefs.getString("token", "");

        // Initialize views
        initializeViews();

        // Get appointment from intent
        appointment = getIntent().getParcelableExtra("APPOINTMENT_PARCEL");
        Log.d(TAG, "Received appointment: " + (appointment != null ? "not null" : "null"));
        
        if (appointment != null) {
            // Check if we have tags and log them
            Map<String, Tag> tags = appointment.getTags();
            Log.d(TAG, "Tags in parcel: " + (tags != null ? tags.size() : "null"));
            if (tags != null && !tags.isEmpty()) {
                for (Map.Entry<String, Tag> entry : tags.entrySet()) {
                    Log.d(TAG, "Tag from parcel: " + entry.getKey() + ", Color: " + entry.getValue().getColor());
                }
            }
        }
        
        if (appointment == null) {
            Log.e(TAG, "Invalid appointment received");
            Toast.makeText(this, "Error: Invalid appointment data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        appointmentId = appointment.getId();
        // Also check if appointment ID was passed separately
        String extraAppointmentId = getIntent().getStringExtra("APPOINTMENT_ID");
        
        if (appointmentId == null || appointmentId.equals("null")) {
            if (extraAppointmentId != null && !extraAppointmentId.equals("null")) {
                // Use the ID passed separately if the parcel's ID is invalid
                Log.d(TAG, "Using appointment ID from intent extra: " + extraAppointmentId);
                appointmentId = extraAppointmentId;
                appointment.setId(appointmentId);
            } else {
                Log.e(TAG, "Invalid appointment ID in appointment object");
                Toast.makeText(this, "Error: Invalid appointment ID", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
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
        tvParticipantsList = findViewById(R.id.tvParticipantsList);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnAddParticipants = findViewById(R.id.btnAddParticipants);
        btnRemoveParticipants = findViewById(R.id.btnRemoveParticipants);
        btnAddCustomTag = findViewById(R.id.btnAddCustomTag);
        btnBack = findViewById(R.id.btnBack);
        tagsList = findViewById(R.id.tagsList);
    }

    private void loadAppointmentDetails() {
        Log.d(TAG, "Loading appointment details for ID: " + appointmentId);
        
        db.collection("appointments")
                .document(appointmentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Document exists, converting to Appointment object");
                        
                        // Get existing tags before loading new data
                        Map<String, Tag> existingTags = (appointment != null) ? appointment.getTags() : null;
                        Log.d(TAG, "Existing tags before reload: " + (existingTags != null ? existingTags.size() : "null"));
                        
                        appointment = documentSnapshot.toObject(Appointment.class);
                        
                        if (appointment != null) {
                            // Ensure tags are preserved if not in the retrieved data
                            Map<String, Tag> newTags = appointment.getTags();
                            Log.d(TAG, "Tags from Firestore: " + (newTags != null ? newTags.size() : "null"));
                            
                            // If tags are missing but we had them before, restore them
                            if ((newTags == null || newTags.isEmpty()) && existingTags != null && !existingTags.isEmpty()) {
                                Log.d(TAG, "Restoring tags that were missing in Firestore data");
                                appointment.setTags(existingTags);
                            }
                            
                            updateUI();
                            // Load participants details
                            loadParticipantsDetails();
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
            dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
            
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
            
            // Load participants details
            loadParticipantsDetails();
            
            // Load and display tags
            updateTagsDisplay();
        } else {
            Log.e(TAG, "Cannot update UI: appointment object is null");
        }
    }
    
    private void loadParticipantsDetails() {
        List<String> participantIds = appointment.getParticipants();
        
        // Debug logging
        Log.d(TAG, "Loading participants. Total IDs: " + (participantIds != null ? participantIds.size() : "null"));
        if (participantIds != null) {
            for (String id : participantIds) {
                Log.d(TAG, "Participant ID: " + id);
            }
        }
        
        if (participantIds == null || participantIds.isEmpty()) {
            tvParticipantsList.setText("No participants");
            return;
        }
        
        // Show loading state
        tvParticipantsList.setText("Loading participants...");
        
        // Use the API to get all students for participant lookup
        apiService.getAllStudents(authToken).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> allStudents = response.body();
                    Log.d(TAG, "Received " + allStudents.size() + " students from API for participant lookup");
                    
                    // Create a map of userId to student data for quick lookup
                    Map<String, Map<String, Object>> studentMap = new HashMap<>();
                    for (Map<String, Object> student : allStudents) {
                        String userId = (String) student.get("userId");
                        if (userId != null) {
                            studentMap.put(userId, student);
                        }
                    }
                    
                    // Now match participant IDs with student data
                    List<String> participantNames = new ArrayList<>();
                    for (String participantId : participantIds) {
                        // Skip null participant IDs
                        if (participantId == null || participantId.isEmpty()) {
                            Log.d(TAG, "Skipping null or empty participant ID");
                            continue;
                        }
                        
                        Map<String, Object> studentData = studentMap.get(participantId);
                        if (studentData != null) {
                            String firstName = (String) studentData.get("firstName");
                            String lastName = (String) studentData.get("lastName");
                            String fullName = firstName + " " + lastName;
                            participantNames.add(fullName);
                            Log.d(TAG, "Found participant in API data: " + fullName + " (ID: " + participantId + ")");
                        } else {
                            Log.d(TAG, "Participant ID not found in API data: " + participantId);
                            // Fallback to Firestore lookup if not found in API data
                            loadParticipantFromFirestore(participantId, participantNames, participantIds.size());
                        }
                    }
                    
                    // Display the names we've found so far
                    if (!participantNames.isEmpty()) {
                        displayParticipantNames(participantNames);
                    } else if (participantNames.isEmpty()) {
                        tvParticipantsList.setText("No participants");
                    }
                } else {
                    // If API call fails, fall back to Firestore lookup
                    Log.e(TAG, "Error fetching students for participant lookup: " + response.code());
                    loadParticipantsFromFirestore(participantIds);
                }
            }
            
            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Log.e(TAG, "API call failed for participant lookup", t);
                // Fallback to Firestore lookup
                loadParticipantsFromFirestore(participantIds);
            }
        });
    }
    
    private void loadParticipantsFromFirestore(List<String> participantIds) {
        List<String> participantNames = new ArrayList<>();
        final int[] participantsLoaded = {0};
        
        // Filter out null participant IDs
        List<String> validParticipantIds = new ArrayList<>();
        for (String id : participantIds) {
            if (id != null && !id.isEmpty()) {
                validParticipantIds.add(id);
            }
        }
        
        if (validParticipantIds.isEmpty()) {
            tvParticipantsList.setText("No participants");
            return;
        }
        
        for (String participantId : validParticipantIds) {
            loadParticipantFromFirestore(participantId, participantNames, validParticipantIds.size());
        }
    }
    
    private void loadParticipantFromFirestore(String participantId, List<String> participantNames, int totalCount) {
        // Skip null participant IDs
        if (participantId == null || participantId.isEmpty()) {
            Log.d(TAG, "Skipping null or empty participant ID");
            return;
        }
        
        Log.d(TAG, "Fetching user details from Firestore for ID: " + participantId);
        db.collection("users")
                .document(participantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Found user document in Firestore for ID: " + participantId);
                        User participant = documentSnapshot.toObject(User.class);
                        if (participant != null) {
                            String fullName = participant.getFirstName() + " " + participant.getLastName();
                            Log.d(TAG, "Added participant from Firestore: " + fullName);
                            participantNames.add(fullName);
                            // Update the UI with the latest names
                            displayParticipantNames(participantNames);
                        } else {
                            Log.d(TAG, "Failed to convert document to User object");
                        }
                    } else {
                        Log.d(TAG, "User document doesn't exist in Firestore for ID: " + participantId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading participant details for ID: " + participantId, e);
                });
    }
    
    private void displayParticipantNames(List<String> participantNames) {
        if (participantNames == null || participantNames.isEmpty()) {
            Log.d(TAG, "No participant names to display");
            tvParticipantsList.setText("No participants");
            return;
        }
        
        Log.d(TAG, "Displaying " + participantNames.size() + " participants");
        StringBuilder participantsText = new StringBuilder();
        for (int i = 0; i < participantNames.size(); i++) {
            participantsText.append(i + 1).append(". ").append(participantNames.get(i));
            if (i < participantNames.size() - 1) {
                participantsText.append("\n");
            }
            Log.d(TAG, "Participant " + (i + 1) + ": " + participantNames.get(i));
        }
        
        tvParticipantsList.setText(participantsText.toString());
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditAppointmentActivity.class);
            intent.putExtra("appointmentId", appointmentId);
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
        
        btnAddParticipants.setOnClickListener(v -> showAddParticipantsDialog());
        
        btnRemoveParticipants.setOnClickListener(v -> showRemoveParticipantsDialog());
        
        btnAddCustomTag.setOnClickListener(v -> showAddTagDialog(null, null));
    }
    
    private void showAddParticipantsDialog() {
        // Show loading indicator or message
        Toast.makeText(this, "Loading students...", Toast.LENGTH_SHORT).show();
        
        // Use the API to fetch all students
        apiService.getAllStudents(authToken).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> studentsList = response.body();
                    Log.d(TAG, "Received " + studentsList.size() + " students from API");
                    
                    List<String> studentNames = new ArrayList<>();
                    Map<String, Map<String, Object>> nameToUserMap = new HashMap<>();
                    
                    // Get current participant IDs for filtering
                    List<String> currentParticipantIds = appointment.getParticipants();
                    if (currentParticipantIds == null) {
                        currentParticipantIds = new ArrayList<>();
                    }
                    
                    Log.d(TAG, "Current user ID: " + currentUserId);
                    Log.d(TAG, "Current participants: " + currentParticipantIds);
                    
                    for (Map<String, Object> student : studentsList) {
                        String userId = (String) student.get("userId");
                        
                        // Debug logging
                        Log.d(TAG, "Processing student: " + student.get("firstName") + " " + 
                               student.get("lastName") + " (ID: " + userId + ")");
                        
                        // Skip if this is the current user
                        if (userId != null && userId.equals(currentUserId)) {
                            Log.d(TAG, "Skipping current user");
                            continue;
                        }
                        
                        // Skip users already in participants list
                        if (currentParticipantIds.contains(userId)) {
                            Log.d(TAG, "Skipping already added participant");
                            continue;
                        }
                        
                        String firstName = (String) student.get("firstName");
                        String lastName = (String) student.get("lastName");
                        String fullName = firstName + " " + lastName;
                        
                        studentNames.add(fullName);
                        nameToUserMap.put(fullName, student);
                        Log.d(TAG, "Added student to dropdown: " + fullName);
                    }
                    
                    if (studentNames.isEmpty()) {
                        Toast.makeText(AppointmentDetailsActivity.this, 
                                "No other students available to add", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Create and configure the dialog with a dropdown
                    AlertDialog.Builder builder = new AlertDialog.Builder(AppointmentDetailsActivity.this);
                    builder.setTitle("Add Participant");
                    
                    // Set up the dropdown adapter
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            AppointmentDetailsActivity.this, 
                            android.R.layout.simple_dropdown_item_1line, 
                            studentNames
                    );
                    
                    // Use a spinner for the dropdown
                    android.widget.Spinner spinner = new android.widget.Spinner(AppointmentDetailsActivity.this);
                    spinner.setAdapter(adapter);
                    builder.setView(spinner);
                    
                    // Configure buttons
                    builder.setPositiveButton("Add", (dialog, which) -> {
                        int selectedPosition = spinner.getSelectedItemPosition();
                        if (selectedPosition != -1) {
                            String selectedName = studentNames.get(selectedPosition);
                            Map<String, Object> selectedUser = nameToUserMap.get(selectedName);
                            
                            if (selectedUser != null) {
                                // Create a User object for addParticipantToAppointment
                                User participant = new User();
                                participant.setId((String) selectedUser.get("userId")); // Use userId from API
                                participant.setFirstName((String) selectedUser.get("firstName"));
                                participant.setLastName((String) selectedUser.get("lastName"));
                                
                                addParticipantToAppointment(participant);
                            }
                        }
                    });
                    builder.setNegativeButton("Cancel", null);
                    
                    // Show the dialog
                    builder.create().show();
                } else {
                    // Handle error response
                    Log.e(TAG, "Error fetching students: " + response.code());
                    Toast.makeText(AppointmentDetailsActivity.this, 
                            "Error: Could not fetch student list", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Log.e(TAG, "API call failed", t);
                Toast.makeText(AppointmentDetailsActivity.this, 
                        "Network error: Could not fetch student list", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void addParticipantToAppointment(User participant) {
        // Skip if participant is null or has null ID
        if (participant == null || participant.getId() == null || participant.getId().isEmpty()) {
            Toast.makeText(this, "Error: Invalid participant data", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get the current participants list or create a new one
        List<String> participants = new ArrayList<>();
        if (appointment.getParticipants() != null) {
            participants.addAll(appointment.getParticipants());
        }
        
        // Add the new participant
        participants.add(participant.getId());
        
        // Update the appointment in Firestore
        db.collection("appointments")
                .document(appointmentId)
                .update("participants", participants)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Successfully added " + participant.getFirstName() + " " + 
                            participant.getLastName() + " to the appointment", Toast.LENGTH_SHORT).show();
                    
                    // Update the appointment object's participants list
                    appointment.setParticipants(participants);
                    
                    // Reload the appointment data
                    loadAppointmentDetails();
                    
                    // Show dialog again to add more participants
                    showAddParticipantsDialog();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding participant to appointment", e);
                    Toast.makeText(this, "Error adding participant", Toast.LENGTH_SHORT).show();
                });
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

    /**
     * Helper method to force reload the participants list from Firestore directly
     * This will solve the issue if participants were added before the UI was updated
     */
    private void forceReloadParticipants() {
        // Fetch the appointment document again to get the latest data
        db.collection("appointments")
                .document(appointmentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get the latest participants list
                        List<String> updatedParticipants = (List<String>) documentSnapshot.get("participants");
                        Log.d(TAG, "Force reloading participants. Found: " + 
                              (updatedParticipants != null ? updatedParticipants.size() : "null"));
                        Log.d(TAG, "Participant IDs from Firestore: " + updatedParticipants);
                        
                        // Clean up the participants list (remove null entries)
                        if (updatedParticipants != null) {
                            List<String> cleanedParticipants = new ArrayList<>();
                            for (String id : updatedParticipants) {
                                if (id != null && !id.isEmpty()) {
                                    cleanedParticipants.add(id);
                                }
                            }
                            
                            // If the cleaned list is different from the original, update Firestore
                            if (cleanedParticipants.size() != updatedParticipants.size()) {
                                Log.d(TAG, "Cleaning participant list. Removed " + 
                                      (updatedParticipants.size() - cleanedParticipants.size()) + " null entries");
                                
                                // Update Firestore with the cleaned list
                                db.collection("appointments")
                                        .document(appointmentId)
                                        .update("participants", cleanedParticipants)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Successfully cleaned participants list in Firestore");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error cleaning participants list in Firestore", e);
                                        });
                                
                                // Update our local appointment object with the cleaned list
                                appointment.setParticipants(cleanedParticipants);
                                updatedParticipants = cleanedParticipants;
                            }
                        }
                        
                        // Update our local appointment object
                        if (updatedParticipants != null && !updatedParticipants.isEmpty()) {
                            appointment.setParticipants(updatedParticipants);
                            // Now load the participant details using the API for better matching
                            loadParticipantsDetails();
                        } else {
                            Log.d(TAG, "No participants found in appointment document");
                            tvParticipantsList.setText("No participants");
                        }
                    } else {
                        Log.e(TAG, "Appointment document doesn't exist");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error force reloading participants", e);
                });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        
        // Store existing tags before reloading
        Map<String, Tag> existingTags = (appointment != null) ? appointment.getTags() : null;
        if (existingTags != null && !existingTags.isEmpty()) {
            Log.d(TAG, "Storing " + existingTags.size() + " existing tags before reload");
            // Make sure these tags are saved to the server
            syncTagsWithServer(existingTags);
        }
        
        // Reload appointment details in case they were updated
        if (appointmentId != null) {
            // Pass existing tags to make sure they are preserved
            loadAppointmentDetailsPreservingTags(existingTags);
            // Also force reload participants separately to ensure we have the latest data
            forceReloadParticipants();
        }
    }
    
    /**
     * Ensure that tags are synced with the server
     */
    private void syncTagsWithServer(Map<String, Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            Log.d(TAG, "No tags to sync with server");
            return;
        }
        
        Log.d(TAG, "Syncing " + tags.size() + " tags with server");
        
        // Create a map for all the tags
        Map<String, Object> tagsMap = new HashMap<>();
        for (Map.Entry<String, Tag> entry : tags.entrySet()) {
            Map<String, Object> tagData = new HashMap<>();
            tagData.put("name", entry.getValue().getName());
            tagData.put("color", entry.getValue().getColor());
            tagsMap.put(entry.getKey(), tagData);
            
            Log.d(TAG, "Adding tag to sync: " + entry.getKey() + ", Color: " + entry.getValue().getColor());
        }
        
        // Create the update map
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("tags", tagsMap);
        
        // Update Firestore
        db.collection("appointments")
                .document(appointmentId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Tags successfully synced with server");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error syncing tags with server", e);
                });
    }
    
    private void loadAppointmentDetailsPreservingTags(Map<String, Tag> existingTags) {
        Log.d(TAG, "Loading appointment details for ID: " + appointmentId + " while preserving tags");
        
        db.collection("appointments")
                .document(appointmentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Document exists, converting to Appointment object");
                        
                        appointment = documentSnapshot.toObject(Appointment.class);
                        
                        if (appointment != null) {
                            // Ensure ID is set
                            appointment.setId(appointmentId);
                            
                            // Restore tags if needed
                            Map<String, Tag> newTags = appointment.getTags();
                            Log.d(TAG, "Tags from Firestore: " + (newTags != null ? newTags.size() : "null"));
                            
                            // If tags are missing but we had them before, restore them
                            if ((newTags == null || newTags.isEmpty()) && existingTags != null && !existingTags.isEmpty()) {
                                Log.d(TAG, "Restoring " + existingTags.size() + " tags that were missing in Firestore data");
                                appointment.setTags(existingTags);
                            }
                            
                            updateUI();
                            // Load participants details
                            loadParticipantsDetails();
                        } else {
                            Log.e(TAG, "Failed to convert document to Appointment object");
                            Toast.makeText(this, "Error: Could not load appointment details", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Document does not exist for ID: " + appointmentId);
                        Toast.makeText(this, "Appointment not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading appointment", e);
                    Toast.makeText(this, "Error loading appointment details", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Show a dialog to select and remove participants from the appointment
     */
    private void showRemoveParticipantsDialog() {
        List<String> participantIds = appointment.getParticipants();
        
        if (participantIds == null || participantIds.isEmpty()) {
            Toast.makeText(this, "No participants to remove", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Filter out null participant IDs to prevent crashes
        List<String> validParticipantIds = new ArrayList<>();
        for (String id : participantIds) {
            if (id != null && !id.isEmpty()) {
                validParticipantIds.add(id);
            }
        }
        
        if (validParticipantIds.isEmpty()) {
            Toast.makeText(this, "No valid participants to remove", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading indicator
        Toast.makeText(this, "Loading participant details...", Toast.LENGTH_SHORT).show();
        
        // Map to store participant names and IDs
        Map<String, String> nameToIdMap = new HashMap<>();
        List<String> participantNames = new ArrayList<>();
        
        // Counter to track when all participants have been loaded
        final int[] participantsLoaded = {0};
        final int totalParticipants = validParticipantIds.size();
        
        // Use the API to get all students for participant lookup
        apiService.getAllStudents(authToken).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> allStudents = response.body();
                    Log.d(TAG, "Received " + allStudents.size() + " students from API for participant lookup");
                    
                    // Create a map of userId to student data for quick lookup
                    Map<String, Map<String, Object>> studentMap = new HashMap<>();
                    for (Map<String, Object> student : allStudents) {
                        String userId = (String) student.get("userId");
                        if (userId != null) {
                            studentMap.put(userId, student);
                        }
                    }
                    
                    // Now match participant IDs with student data
                    for (String participantId : validParticipantIds) {
                        // Double check that the ID is not null (this should never happen due to the filter above)
                        if (participantId == null || participantId.isEmpty()) {
                            participantsLoaded[0]++;
                            continue;
                        }
                        
                        Map<String, Object> studentData = studentMap.get(participantId);
                        if (studentData != null) {
                            String firstName = (String) studentData.get("firstName");
                            String lastName = (String) studentData.get("lastName");
                            String fullName = firstName + " " + lastName;
                            participantNames.add(fullName);
                            nameToIdMap.put(fullName, participantId);
                            participantsLoaded[0]++;
                            Log.d(TAG, "Found participant in API data: " + fullName + " (ID: " + participantId + ")");
                        } else {
                            // If not found in API, try Firestore
                            db.collection("users")
                                    .document(participantId)
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            User participant = documentSnapshot.toObject(User.class);
                                            if (participant != null) {
                                                String fullName = participant.getFirstName() + " " + participant.getLastName();
                                                participantNames.add(fullName);
                                                nameToIdMap.put(fullName, participant.getId());
                                                Log.d(TAG, "Found participant in Firestore: " + fullName);
                                            }
                                        } else {
                                            Log.d(TAG, "Participant with ID " + participantId + " not found in Firestore");
                                        }
                                        
                                        participantsLoaded[0]++;
                                        if (participantsLoaded[0] >= totalParticipants) {
                                            showParticipantSelectionDialog(participantNames, nameToIdMap);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error loading participant: " + e.getMessage());
                                        participantsLoaded[0]++;
                                        if (participantsLoaded[0] >= totalParticipants) {
                                            showParticipantSelectionDialog(participantNames, nameToIdMap);
                                        }
                                    });
                        }
                    }
                    
                    // If all participants were loaded from API data (not from Firestore), show dialog
                    if (participantsLoaded[0] >= totalParticipants) {
                        showParticipantSelectionDialog(participantNames, nameToIdMap);
                    }
                } else {
                    // If API call fails, fall back to Firestore lookup for all participants
                    Log.e(TAG, "Error fetching students: " + response.code());
                    loadParticipantsFromFirestoreForRemoval(validParticipantIds, participantNames, nameToIdMap);
                }
            }
            
            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Log.e(TAG, "API call failed for participant lookup", t);
                // Fallback to Firestore lookup
                loadParticipantsFromFirestoreForRemoval(validParticipantIds, participantNames, nameToIdMap);
            }
        });
    }
    
    /**
     * Load participant details from Firestore when API call fails
     */
    private void loadParticipantsFromFirestoreForRemoval(List<String> participantIds, 
                                                List<String> participantNames,
                                                Map<String, String> nameToIdMap) {
        final int[] participantsLoaded = {0};
        final int totalParticipants = participantIds.size();
        
        // If no participants to load, show selection dialog immediately
        if (participantIds.isEmpty()) {
            showParticipantSelectionDialog(participantNames, nameToIdMap);
            return;
        }
        
        for (String participantId : participantIds) {
            // Skip null participant IDs
            if (participantId == null || participantId.isEmpty()) {
                participantsLoaded[0]++;
                if (participantsLoaded[0] >= totalParticipants) {
                    showParticipantSelectionDialog(participantNames, nameToIdMap);
                }
                continue;
            }
            
            db.collection("users")
                    .document(participantId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User participant = documentSnapshot.toObject(User.class);
                            if (participant != null) {
                                String fullName = participant.getFirstName() + " " + participant.getLastName();
                                participantNames.add(fullName);
                                nameToIdMap.put(fullName, participant.getId());
                                Log.d(TAG, "Added participant from Firestore: " + fullName);
                            }
                        }
                        
                        participantsLoaded[0]++;
                        if (participantsLoaded[0] >= totalParticipants) {
                            showParticipantSelectionDialog(participantNames, nameToIdMap);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading participant details", e);
                        participantsLoaded[0]++;
                        if (participantsLoaded[0] >= totalParticipants) {
                            showParticipantSelectionDialog(participantNames, nameToIdMap);
                        }
                    });
        }
    }
    
    /**
     * Show dialog with list of participants for selection and removal
     */
    private void showParticipantSelectionDialog(List<String> participantNames, Map<String, String> nameToIdMap) {
        if (participantNames.isEmpty()) {
            Toast.makeText(this, "No participants found to remove", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create and configure the dialog with a dropdown
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove Participant");
        
        // Set up the dropdown adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, 
                android.R.layout.simple_dropdown_item_1line, 
                participantNames
        );
        
        // Use a spinner for the dropdown
        android.widget.Spinner spinner = new android.widget.Spinner(this);
        spinner.setAdapter(adapter);
        builder.setView(spinner);
        
        // Configure buttons
        builder.setPositiveButton("Remove", (dialog, which) -> {
            int selectedPosition = spinner.getSelectedItemPosition();
            if (selectedPosition != -1) {
                String selectedName = participantNames.get(selectedPosition);
                String participantId = nameToIdMap.get(selectedName);
                
                if (participantId != null) {
                    removeParticipantFromAppointment(participantId, selectedName);
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        
        // Show the dialog
        builder.create().show();
    }
    
    /**
     * Remove a participant from the appointment
     */
    private void removeParticipantFromAppointment(String participantId, String participantName) {
        // Skip if participantId is null or empty
        if (participantId == null || participantId.isEmpty()) {
            Toast.makeText(this, "Error: Invalid participant ID", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get the current participants list
        List<String> participants = new ArrayList<>();
        if (appointment.getParticipants() != null) {
            participants.addAll(appointment.getParticipants());
        }
        
        // Check if the participant exists in the list
        if (!participants.contains(participantId)) {
            Toast.makeText(this, "Participant not found in appointment", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Remove the participant
        participants.remove(participantId);
        
        // Clean the list: remove any null values
        List<String> cleanedList = new ArrayList<>();
        for (String id : participants) {
            if (id != null && !id.isEmpty()) {
                cleanedList.add(id);
            }
        }
        
        // Store the final list for Firestore update
        final List<String> finalParticipantsList = cleanedList;
        
        // Show a confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Confirm Removal")
                .setMessage("Are you sure you want to remove " + participantName + " from this appointment?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Update the appointment in Firestore
                    db.collection("appointments")
                            .document(appointmentId)
                            .update("participants", finalParticipantsList)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Successfully removed " + participantName + 
                                        " from the appointment", Toast.LENGTH_SHORT).show();
                                
                                // Update the appointment object's participants list
                                appointment.setParticipants(finalParticipantsList);
                                
                                // Reload the appointment data
                                loadAppointmentDetails();
                                
                                // Show dialog again to remove more participants if needed
                                if (!finalParticipantsList.isEmpty()) {
                                    showRemoveParticipantsDialog();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error removing participant from appointment", e);
                                Toast.makeText(this, "Error removing participant", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Shows a dialog to add a new tag to the appointment
     */
    private void showAddTagDialog(String tagName, Tag existingTag) {
        boolean isEdit = tagName != null && existingTag != null;
        
        // Inflate the custom dialog layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_tag, null);
        
        // Initialize views
        final EditText etTagName = dialogView.findViewById(R.id.etTagName);
        final Button btnSelectColor = dialogView.findViewById(R.id.btnSelectColor);
        final View colorPreview = dialogView.findViewById(R.id.colorPreview);
        final TextView tvColorHex = dialogView.findViewById(R.id.tvColorHex);
        
        // Set up the initial state for editing mode
        final String[] selectedColor = {isEdit ? existingTag.getColor() : "#FF0000"};
        if (isEdit) {
            etTagName.setText(tagName);
            try {
                colorPreview.setBackgroundColor(Color.parseColor(selectedColor[0]));
                tvColorHex.setText(selectedColor[0]);
            } catch (IllegalArgumentException e) {
                // Use default color if parsing fails
                selectedColor[0] = "#FF0000";
                colorPreview.setBackgroundColor(Color.RED);
                tvColorHex.setText("#FF0000");
            }
        }
        
        // Set up color picking functionality
        btnSelectColor.setOnClickListener(v -> {
            ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, hexColor -> {
                selectedColor[0] = hexColor;
                colorPreview.setBackgroundColor(Color.parseColor(hexColor));
                tvColorHex.setText(hexColor);
            });
            colorPickerDialog.setInitialColor(selectedColor[0]);
            colorPickerDialog.show();
        });
        
        // Create the dialog with proper title and buttons
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isEdit ? "Edit Tag" : "Add Custom Tag");
        builder.setView(dialogView);
        
        builder.setPositiveButton(isEdit ? "Update" : "Add", (dialog, which) -> {
            String newTagName = etTagName.getText().toString().trim();
            
            if (newTagName.isEmpty()) {
                Toast.makeText(this, "Tag name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Create the tag
            Tag tag = new Tag(newTagName, selectedColor[0]);
            
            // If editing and tag name has changed, remove the old tag first
            if (isEdit) {
                // Always remove the old tag regardless of name change
                // This ensures we don't end up with both old and new tags
                removeTag(tagName);
                
                // Log what we're doing
                if (!tagName.equals(newTagName)) {
                    Log.d(TAG, "Tag name changed from " + tagName + " to " + newTagName + ". Old tag removed.");
                } else {
                    Log.d(TAG, "Tag name unchanged, but removing old tag to update properties.");
                }
            }
            
            // Save the new tag (or updated tag with same name)
            saveTag(newTagName, tag);
        });
        
        builder.setNegativeButton("Cancel", null);
        
        builder.create().show();
    }
    
    /**
     * Saves a tag to Firestore for the current appointment
     */
    private void saveTag(String tagName, Tag tag) {
        Log.d(TAG, "Saving tag: " + tagName + ", Color: " + tag.getColor());
        
        // Create a map for the entire tags structure
        Map<String, Object> tagsMap = new HashMap<>();
        
        // First, preserve any existing tags
        if (appointment.getTags() != null) {
            for (Map.Entry<String, Tag> entry : appointment.getTags().entrySet()) {
                // Skip if it's the same tag name we're trying to add (to avoid duplicates)
                if (entry.getKey().equals(tagName)) {
                    Log.d(TAG, "Skipping existing tag with same name to avoid duplicates");
                    continue;
                }
                
                Map<String, Object> existingTagData = new HashMap<>();
                existingTagData.put("name", entry.getValue().getName());
                existingTagData.put("color", entry.getValue().getColor());
                tagsMap.put(entry.getKey(), existingTagData);
            }
        }
        
        // Add the new tag
        Map<String, Object> newTagData = new HashMap<>();
        newTagData.put("name", tag.getName());
        newTagData.put("color", tag.getColor());
        tagsMap.put(tagName, newTagData);
        
        Log.d(TAG, "Complete tags map to save: " + tagsMap.size() + " tags");
        
        // Create the update map with the full tags structure
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("tags", tagsMap);
        
        // Update Firestore - using update instead of set with merge option
        db.collection("appointments")
                .document(appointmentId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Tag saved successfully", Toast.LENGTH_SHORT).show();
                    
                    // Clear any previous tags with the same name to avoid duplicates
                    if (appointment.getTags() != null) {
                        appointment.removeTag(tagName);
                    }
                    
                    // Update local appointment object
                    if (appointment.getTags() == null) {
                        Log.d(TAG, "Creating new tags map for appointment");
                    }
                    appointment.addTag(tagName, tag);
                    
                    // Log the tags after adding
                    Map<String, Tag> updatedTags = appointment.getTags();
                    Log.d(TAG, "Tags after adding: " + (updatedTags != null ? updatedTags.size() : "null"));
                    if (updatedTags != null) {
                        for (Map.Entry<String, Tag> entry : updatedTags.entrySet()) {
                            Log.d(TAG, "Tag: " + entry.getKey() + ", Color: " + entry.getValue().getColor());
                        }
                    }
                    
                    // Make sure HomePage gets the updated data
                    syncTagsWithServer(appointment.getTags());
                    
                    // Update UI
                    updateTagsDisplay();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving tag", e);
                    Toast.makeText(this, "Error saving tag: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    /**
     * Removes a tag from the appointment
     */
    private void removeTag(String tagName) {
        Log.d(TAG, "Removing tag: " + tagName);
        
        // Create a complete map of the remaining tags
        Map<String, Object> tagsMap = new HashMap<>();
        
        // Add all tags except the one being deleted
        if (appointment.getTags() != null) {
            for (Map.Entry<String, Tag> entry : appointment.getTags().entrySet()) {
                // Skip the tag we want to remove
                if (entry.getKey().equals(tagName)) {
                    Log.d(TAG, "Excluding tag: " + tagName + " from updated tags map");
                    continue;
                }
                
                Map<String, Object> tagData = new HashMap<>();
                tagData.put("name", entry.getValue().getName());
                tagData.put("color", entry.getValue().getColor());
                tagsMap.put(entry.getKey(), tagData);
                
                Log.d(TAG, "Keeping tag: " + entry.getKey() + " in updated tags map");
            }
        }
        
        Log.d(TAG, "Updated tags map after removal: " + tagsMap.size() + " tags");
        
        // Create the update map with the full tags structure
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("tags", tagsMap);
        
        // Update Firestore with the complete tags map
        db.collection("appointments")
                .document(appointmentId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Tag removed successfully", Toast.LENGTH_SHORT).show();
                    
                    // Update local appointment object
                    if (appointment.getTags() != null) {
                        appointment.removeTag(tagName);
                        
                        // Log the tags after removal
                        Map<String, Tag> updatedTags = appointment.getTags();
                        Log.d(TAG, "Tags after removal: " + (updatedTags != null ? updatedTags.size() : "null"));
                        if (updatedTags != null && !updatedTags.isEmpty()) {
                            for (Map.Entry<String, Tag> entry : updatedTags.entrySet()) {
                                Log.d(TAG, "Remaining tag: " + entry.getKey() + ", Color: " + entry.getValue().getColor());
                            }
                        } else {
                            Log.d(TAG, "No tags remaining after removal");
                        }
                        
                        // Make sure HomePage gets the updated data
                        syncTagsWithServer(appointment.getTags());
                    }
                    
                    // Update UI
                    updateTagsDisplay();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing tag", e);
                    Toast.makeText(this, "Error removing tag: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    /**
     * Updates the display of tags in the UI
     */
    private void updateTagsDisplay() {
        // Clear existing tags
        tagsList.removeAllViews();
        
        // Check if there are any tags to display
        Map<String, Tag> tags = appointment.getTags();
        if (tags == null || tags.isEmpty()) {
            // Add a "No tags" message
            TextView tvNoTags = new TextView(this);
            tvNoTags.setText("No tags added yet");
            tvNoTags.setTextSize(14);
            tvNoTags.setPadding(8, 8, 8, 8);
            tagsList.addView(tvNoTags);
            return;
        }
        
        // Add a view for each tag
        for (Map.Entry<String, Tag> entry : tags.entrySet()) {
            String tagName = entry.getKey();
            Tag tag = entry.getValue();
            
            // Inflate the tag item layout
            View tagView = LayoutInflater.from(this).inflate(R.layout.item_tag, null);
            
            // Set up the tag name with the specified color
            TextView tvTagName = tagView.findViewById(R.id.tvTagName);
            tvTagName.setText(tagName);
            try {
                tvTagName.setTextColor(Color.parseColor(tag.getColor()));
            } catch (IllegalArgumentException e) {
                // Use default color if parsing fails
                tvTagName.setTextColor(Color.BLACK);
            }
            
            // Set up edit button
            ImageButton btnEditTag = tagView.findViewById(R.id.btnEditTag);
            btnEditTag.setOnClickListener(v -> showAddTagDialog(tagName, tag));
            
            // Set up delete button
            ImageButton btnDeleteTag = tagView.findViewById(R.id.btnDeleteTag);
            btnDeleteTag.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Delete Tag")
                        .setMessage("Are you sure you want to delete the tag '" + tagName + "'?")
                        .setPositiveButton("Delete", (dialog, which) -> removeTag(tagName))
                        .setNegativeButton("Cancel", null)
                        .show();
            });
            
            // Add the tag view to the container
            tagsList.addView(tagView);
        }
    }

    /**
     * Called when returning from HomePage to ensure tags are preserved
     */
    @Override
    protected void onStart() {
        super.onStart();
        
        // When coming back to this activity, make sure we have the latest tag data
        if (appointmentId != null) {
            // Directly fetch tags from Firestore
            fetchTagsFromFirestore();
        }
    }
    
    /**
     * Fetch tags directly from Firestore to ensure they're up to date
     */
    private void fetchTagsFromFirestore() {
        Log.d(TAG, "Fetching tags directly from Firestore");
        
        db.collection("appointments")
                .document(appointmentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Extract tags data
                        Map<String, Object> data = documentSnapshot.getData();
                        Map<String, Object> tagsData = data != null ? (Map<String, Object>) data.get("tags") : null;
                        
                        if (tagsData != null && !tagsData.isEmpty()) {
                            Log.d(TAG, "Found " + tagsData.size() + " tags in Firestore");
                            
                            // Create tags map
                            Map<String, Tag> tags = new HashMap<>();
                            
                            // Process each tag
                            for (Map.Entry<String, Object> entry : tagsData.entrySet()) {
                                if (entry.getValue() instanceof Map) {
                                    Map<String, Object> tagMap = (Map<String, Object>) entry.getValue();
                                    String name = (String) tagMap.get("name");
                                    String color = (String) tagMap.get("color");
                                    
                                    if (name != null && color != null) {
                                        Tag tag = new Tag(name, color);
                                        tags.put(entry.getKey(), tag);
                                        Log.d(TAG, "Retrieved tag: " + name + ", Color: " + color);
                                    }
                                }
                            }
                            
                            // Update the appointment with the tags
                            if (!tags.isEmpty()) {
                                appointment.setTags(tags);
                                // Update the UI to show the tags
                                updateTagsDisplay();
                            }
                        } else {
                            Log.d(TAG, "No tags found in Firestore");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching tags from Firestore", e);
                });
    }
} 