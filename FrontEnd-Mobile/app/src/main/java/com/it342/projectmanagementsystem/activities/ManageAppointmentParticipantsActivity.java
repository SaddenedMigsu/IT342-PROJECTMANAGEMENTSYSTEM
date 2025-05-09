package com.it342.projectmanagementsystem.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.adapters.ParticipantRequestAdapter;
import com.it342.projectmanagementsystem.models.Appointment;
import com.it342.projectmanagementsystem.models.ParticipantRequest;
import com.it342.projectmanagementsystem.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageAppointmentParticipantsActivity extends AppCompatActivity implements ParticipantRequestAdapter.ParticipantRequestListener {
    private static final String TAG = "ManageParticipants";
    
    private ImageButton btnBack;
    private Spinner appointmentSpinner;
    private TextView tvInstructions;
    private RecyclerView recyclerParticipants;
    private LinearLayout emptyStateView;
    private ProgressBar progressBar;
    
    private FirebaseFirestore db;
    private ParticipantRequestAdapter adapter;
    private String facultyId;
    
    private List<Appointment> facultyAppointments = new ArrayList<>();
    private Map<String, String> appointmentIdToTitleMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_appointment_participants);

        db = FirebaseFirestore.getInstance();
        
        // Get faculty ID from shared preferences
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        facultyId = prefs.getString(Constants.KEY_USER_ID, "");
        
        if (facultyId.isEmpty()) {
            Toast.makeText(this, "Faculty ID not found. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Enable Firebase custom class mapping for appointmentType field
        FirebaseFirestore.setLoggingEnabled(true);

        initializeViews();
        setupListeners();
        setupRecyclerView();
        loadFacultyAppointments();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        appointmentSpinner = findViewById(R.id.appointmentSpinner);
        tvInstructions = findViewById(R.id.tvInstructions);
        recyclerParticipants = findViewById(R.id.recyclerParticipants);
        emptyStateView = findViewById(R.id.emptyStateView);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        appointmentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Skip the first item (hint)
                    String appointmentTitle = (String) parent.getItemAtPosition(position);
                    String appointmentId = getAppointmentIdFromTitle(appointmentTitle);
                    if (appointmentId != null) {
                        loadParticipantRequests(appointmentId);
                    }
                } else {
                    // Clear the list if "Select Appointment" is selected
                    adapter.setParticipantRequests(new ArrayList<>());
                    showEmptyState();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new ParticipantRequestAdapter(this, this);
        recyclerParticipants.setLayoutManager(new LinearLayoutManager(this));
        recyclerParticipants.setAdapter(adapter);
    }

    private void loadFacultyAppointments() {
        showLoading();
        
        Log.d(TAG, "Starting to load faculty appointments for faculty ID: " + facultyId);
        
        // First try to get faculty email
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String facultyEmail = prefs.getString(Constants.KEY_EMAIL, "");
        
        Log.d(TAG, "Faculty email from prefs: " + facultyEmail);
        
        // Query 1: Get appointments where faculty is in participants list
        db.collection("appointments")
            .whereArrayContains("participants", facultyId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                facultyAppointments.clear();
                appointmentIdToTitleMap.clear();
                
                Log.d(TAG, "Got " + queryDocumentSnapshots.size() + " appointments from participants query");
                
                processAppointments(queryDocumentSnapshots);
                
                // Query 2: Also get appointments where facultyId field matches
                db.collection("appointments")
                    .whereEqualTo("facultyId", facultyId)
                    .get()
                    .addOnSuccessListener(facultyIdResults -> {
                        Log.d(TAG, "Got " + facultyIdResults.size() + " appointments from facultyId query");
                        
                        processAppointments(facultyIdResults);
                        
                        // Query 3: Also get appointments where createdBy field matches
                        db.collection("appointments")
                            .whereEqualTo("createdBy", facultyId)
                            .get()
                            .addOnSuccessListener(createdByResults -> {
                                Log.d(TAG, "Got " + createdByResults.size() + " appointments from createdBy query");
                                
                                processAppointments(createdByResults);
                                
                                // If we have faculty email, try one more query
                                if (!facultyEmail.isEmpty()) {
                                    db.collection("appointments")
                                        .whereArrayContains("participants", facultyEmail)
                                        .get()
                                        .addOnSuccessListener(emailResults -> {
                                            Log.d(TAG, "Got " + emailResults.size() + " appointments from email participants query");
                                            
                                            processAppointments(emailResults);
                                            finishLoading();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error loading appointments by email", e);
                                            finishLoading();
                                        });
                                } else {
                                    finishLoading();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error loading appointments by createdBy", e);
                                finishLoading();
                            });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading appointments by facultyId", e);
                        finishLoading();
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading faculty appointments", e);
                Toast.makeText(this, "Failed to load appointments: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                hideLoading();
            });
    }
    
    private void processAppointments(com.google.firebase.firestore.QuerySnapshot querySnapshot) {
        for (QueryDocumentSnapshot document : querySnapshot) {
            // Skip duplicates
            if (appointmentIdToTitleMap.containsKey(document.getId())) {
                continue;
            }
            
            Appointment appointment = document.toObject(Appointment.class);
            appointment.setId(document.getId());
            
            // Log the raw data for debugging
            Map<String, Object> data = document.getData();
            Log.d(TAG, "Appointment data: " + data);
            
            // Get participants list
            List<String> participants = appointment.getParticipants();
            
            // Log participant info
            if (participants != null) {
                Log.d(TAG, "Appointment " + appointment.getId() + 
                      " has " + participants.size() + " participants");
                for (String participantId : participants) {
                    Log.d(TAG, "  Participant ID: " + participantId);
                }
            } else {
                Log.d(TAG, "Appointment " + appointment.getId() + 
                      " has null participants list");
            }
            
            // Only add appointments that have participants
            if (participants != null && !participants.isEmpty()) {
                Log.d(TAG, "Found appointment with " + participants.size() + " participants: " + appointment.getTitle());
                facultyAppointments.add(appointment);
                // Map appointment ID to title for spinner selection
                appointmentIdToTitleMap.put(appointment.getId(), appointment.getTitle());
            }
        }
    }
    
    private void finishLoading() {
        Log.d(TAG, "Added " + facultyAppointments.size() + " appointments with participants to the list");
        populateAppointmentSpinner();
        hideLoading();
    }

    private void populateAppointmentSpinner() {
        List<String> appointmentTitles = new ArrayList<>();
        appointmentTitles.add("Select Appointment"); // Hint item
        
        // Add all appointment titles
        appointmentTitles.addAll(appointmentIdToTitleMap.values());
        
        Log.d(TAG, "Populating spinner with " + (appointmentTitles.size() - 1) + " appointments");
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                appointmentTitles
        );
        
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        appointmentSpinner.setAdapter(adapter);
        
        // Show instructions
        if (appointmentTitles.size() <= 1) {
            Log.d(TAG, "No appointments available in spinner");
            tvInstructions.setText("No appointments with participants available");
            
            // Update the empty state text
            if (emptyStateView != null) {
                TextView emptyStateTitle = (TextView) emptyStateView.findViewWithTag("emptyStateTitle");
                if (emptyStateTitle == null) {
                    // Find the title and details TextViews - they're the second and third children
                    if (emptyStateView.getChildCount() >= 3) {
                        View titleView = emptyStateView.getChildAt(1);
                        View detailsView = emptyStateView.getChildAt(2);
                        
                        if (titleView instanceof TextView) {
                            ((TextView) titleView).setText("No Appointments With Participants");
                        }
                        
                        if (detailsView instanceof TextView) {
                            ((TextView) detailsView).setText("You have no appointments with participants to manage.\nAdd participants to appointments to manage them here.");
                        }
                    }
                }
            }
            
            showEmptyState();
        } else {
            Log.d(TAG, "Appointments available, showing instructions");
            tvInstructions.setText("Select an appointment to view and manage participants");
            
            // Select the first appointment automatically after a short delay
            // to let the UI finish rendering
            new Handler().postDelayed(() -> {
                if (appointmentSpinner.getSelectedItemPosition() == 0 && appointmentTitles.size() > 1) {
                    Log.d(TAG, "Auto-selecting first appointment: " + appointmentTitles.get(1));
                    appointmentSpinner.setSelection(1); // Select the first actual appointment
                }
            }, 300);
        }
    }

    private String getAppointmentIdFromTitle(String title) {
        for (Map.Entry<String, String> entry : appointmentIdToTitleMap.entrySet()) {
            if (entry.getValue().equals(title)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void loadParticipantRequests(String appointmentId) {
        showLoading();
        
        Log.d(TAG, "Loading participant requests for appointment ID: " + appointmentId);
        
        // Get the appointment to access its participants
        db.collection("appointments")
            .document(appointmentId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Appointment appointment = documentSnapshot.toObject(Appointment.class);
                    appointment.setId(documentSnapshot.getId());
                    
                    // Get participants list
                    List<String> participantIds = appointment.getParticipants();
                    if (participantIds == null || participantIds.isEmpty()) {
                        Log.d(TAG, "No participants found for appointment: " + appointmentId);
                        showEmptyState();
                        hideLoading();
                        return;
                    }
                    
                    Log.d(TAG, "Found " + participantIds.size() + " participants for appointment: " + appointment.getTitle());
                    
                    // Now load participant user details
                    loadParticipantDetails(appointment, participantIds);
                } else {
                    Log.e(TAG, "Appointment document not found for ID: " + appointmentId);
                    showEmptyState();
                    hideLoading();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading appointment details", e);
                Toast.makeText(this, "Failed to load appointment details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                showEmptyState();
                hideLoading();
            });
    }
    
    private void loadParticipantDetails(Appointment appointment, List<String> participantIds) {
        List<ParticipantRequest> participantRequests = new ArrayList<>();
        final int[] participantsLoaded = {0};
        final int totalParticipants = participantIds.size();
        
        // Get current faculty name for dialog
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String facultyFirstName = prefs.getString(Constants.KEY_FIRST_NAME, "");
        String facultyLastName = prefs.getString(Constants.KEY_LAST_NAME, "");
        String currentUserName = facultyFirstName + " " + facultyLastName;
        
        if (currentUserName.trim().isEmpty()) {
            currentUserName = "Faculty";
        }
        final String facultyName = currentUserName;
        
        Log.d(TAG, "Loading details for " + participantIds.size() + " participants in appointment: " + appointment.getTitle());
        Log.d(TAG, "Faculty name for confirmation dialog: " + facultyName);
        
        if (participantIds.isEmpty()) {
            Log.d(TAG, "No participants to load for appointment: " + appointment.getId());
            showEmptyState();
            hideLoading();
            return;
        }
        
        // Force the UI to show we're loading participants
        showLoading();
        ProgressBar loadingIndicator = findViewById(R.id.progressBar);
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.VISIBLE);
        }
        
        for (String participantId : participantIds) {
            // Skip null or empty IDs
            if (participantId == null || participantId.isEmpty()) {
                Log.d(TAG, "Skipping null/empty participant ID");
                participantsLoaded[0]++;
                continue;
            }
            
            // Skip if participant ID is the current faculty ID
            if (participantId.equals(facultyId)) {
                Log.d(TAG, "Skipping faculty's own ID from participants list: " + participantId);
                participantsLoaded[0]++;
                continue;
            }
            
            Log.d(TAG, "Loading user details for participant ID: " + participantId);
            
            // Load user details from Firestore
            db.collection("users")
                .document(participantId)
                .get()
                .addOnSuccessListener(userDocument -> {
                    participantsLoaded[0]++;
                    
                    if (userDocument.exists()) {
                        // Create a participant request from the user data
                        String firstName = userDocument.getString("firstName");
                        String lastName = userDocument.getString("lastName");
                        String studentName = (firstName != null ? firstName : "") + " " + 
                                        (lastName != null ? lastName : "");
                        
                        // Clean up name if needed
                        if (studentName.trim().isEmpty()) {
                            studentName = "Unknown User";
                        }
                        
                        Log.d(TAG, "Found user: " + studentName + " (ID: " + participantId + ")");
                        
                        // Create a participant request object - status is set to "PENDING_CONFIRMATION"
                        // to indicate it needs faculty confirmation
                        ParticipantRequest request = new ParticipantRequest(
                                appointment.getId(),
                                participantId,
                                studentName.trim(),
                                appointment.getCreatedAt() != null ? appointment.getCreatedAt() : Timestamp.now(),
                                "PENDING_CONFIRMATION"
                        );
                        
                        // Set a generated ID (to handle updates)
                        request.setId(appointment.getId() + "_" + participantId);
                        
                        participantRequests.add(request);
                        Log.d(TAG, "Added participant: " + studentName + " for appointment: " + appointment.getTitle());
                    } else {
                        Log.d(TAG, "User document not found for ID: " + participantId);
                    }
                    
                    // If all participants are loaded, update the UI
                    if (participantsLoaded[0] >= totalParticipants) {
                        updateUIWithParticipants(participantRequests);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user details for ID: " + participantId, e);
                    participantsLoaded[0]++;
                    
                    // If all participants are loaded, update the UI
                    if (participantsLoaded[0] >= totalParticipants) {
                        updateUIWithParticipants(participantRequests);
                    }
                });
        }
        
        // If we have no valid participants after filtering, show empty state
        if (participantIds.size() == 0 || (participantIds.size() == 1 && participantIds.contains(facultyId))) {
            Log.d(TAG, "No valid participants to display after filtering");
            showEmptyState();
            hideLoading();
        }
    }
    
    private void updateUIWithParticipants(List<ParticipantRequest> participantRequests) {
        runOnUiThread(() -> {
            if (participantRequests.isEmpty()) {
                Log.d(TAG, "No participant requests to display");
                showEmptyState();
            } else {
                Log.d(TAG, "Displaying " + participantRequests.size() + " participant requests");
                adapter.setParticipantRequests(participantRequests);
                hideEmptyState();
            }
            hideLoading();
        });
    }

    @Override
    public void onAcceptClicked(ParticipantRequest request, int position) {
        // Get faculty name from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String facultyFirstName = prefs.getString(Constants.KEY_FIRST_NAME, "");
        String facultyLastName = prefs.getString(Constants.KEY_LAST_NAME, "");
        String facultyName = (facultyFirstName + " " + facultyLastName).trim();
        
        if (facultyName.isEmpty()) {
            facultyName = "Faculty";
        }
        
        // Get appointment title
        String appointmentTitle = appointmentIdToTitleMap.get(request.getAppointmentId());
        if (appointmentTitle == null) {
            appointmentTitle = "this appointment";
        }
        
        Log.d(TAG, "Showing confirmation dialog for participant: " + request.getStudentName() + 
              " on appointment: " + appointmentTitle);
        
        showConfirmationDialog(
                "Confirm Participant",
                facultyName + " wants to add " + request.getStudentName() + " as a participant on " + appointmentTitle + ".",
                () -> updateParticipantRequestStatus(request, position, "APPROVED"),
                "Confirm",
                "Decline"
        );
    }

    @Override
    public void onRejectClicked(ParticipantRequest request, int position) {
        // Get faculty name from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String facultyFirstName = prefs.getString(Constants.KEY_FIRST_NAME, "");
        String facultyLastName = prefs.getString(Constants.KEY_LAST_NAME, "");
        String facultyName = facultyFirstName + " " + facultyLastName;
        
        // Get appointment title
        String appointmentTitle = appointmentIdToTitleMap.get(request.getAppointmentId());
        if (appointmentTitle == null) {
            appointmentTitle = "this appointment";
        }
        
        showConfirmationDialog(
                "Remove Participant",
                "Are you sure you want to remove " + request.getStudentName() + " from " + appointmentTitle + "?",
                () -> updateParticipantRequestStatus(request, position, "REJECTED"),
                "Remove",
                "Cancel"
        );
    }

    private void updateParticipantRequestStatus(ParticipantRequest request, int position, String newStatus) {
        showLoading();
        
        // Get the appointment document to update
        DocumentReference appointmentRef = db.collection("appointments").document(request.getAppointmentId());
        
        // Based on the new status, either keep or remove the participant
        if ("APPROVED".equals(newStatus)) {
            // Keep participant - just update the adapter status
            adapter.updateRequestStatus(position, newStatus);
            hideLoading();
            Toast.makeText(this, "Participant confirmed", Toast.LENGTH_SHORT).show();
            
            // Send confirmation to the participant via API if possible
            sendParticipantConfirmation(request);
        } else {
            // Remove participant from the appointment
            appointmentRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Appointment appointment = documentSnapshot.toObject(Appointment.class);
                        
                        if (appointment != null) {
                            List<String> participants = appointment.getParticipants();
                            if (participants != null) {
                                // Remove the student from the participants list
                                participants.remove(request.getStudentId());
                                
                                // Update the appointment with the new participants list
                                appointmentRef.update("participants", participants)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Student removed from appointment successfully");
                                        
                                        // Update the adapter
                                        adapter.updateRequestStatus(position, newStatus);
                                        
                                        // Also remove the request from the list after a short delay
                                        new Handler().postDelayed(() -> {
                                            // Remove this item
                                            List<ParticipantRequest> currentRequests = new ArrayList<>(adapter.getParticipantRequests());
                                            currentRequests.remove(position);
                                            adapter.setParticipantRequests(currentRequests);
                                            
                                            // Show empty state if no more requests
                                            if (currentRequests.isEmpty()) {
                                                showEmptyState();
                                            }
                                        }, 1000); // 1 second delay
                                        
                                        // Also remove via API if possible
                                        removeParticipantViaApi(request);
                                        
                                        hideLoading();
                                        Toast.makeText(
                                            ManageAppointmentParticipantsActivity.this, 
                                            "Participant removed successfully", 
                                            Toast.LENGTH_SHORT
                                        ).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error removing student from appointment", e);
                                        hideLoading();
                                        Toast.makeText(
                                            ManageAppointmentParticipantsActivity.this, 
                                            "Failed to remove participant: " + e.getMessage(), 
                                            Toast.LENGTH_SHORT
                                        ).show();
                                    });
                            } else {
                                hideLoading();
                                Toast.makeText(
                                    ManageAppointmentParticipantsActivity.this, 
                                    "No participants found in appointment", 
                                    Toast.LENGTH_SHORT
                                ).show();
                            }
                        } else {
                            hideLoading();
                            Toast.makeText(
                                ManageAppointmentParticipantsActivity.this, 
                                "Error: Could not parse appointment data", 
                                Toast.LENGTH_SHORT
                            ).show();
                        }
                    } else {
                        hideLoading();
                        Toast.makeText(
                            ManageAppointmentParticipantsActivity.this, 
                            "Error: Appointment not found", 
                            Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting appointment", e);
                    hideLoading();
                    Toast.makeText(
                        ManageAppointmentParticipantsActivity.this, 
                        "Failed to get appointment: " + e.getMessage(), 
                        Toast.LENGTH_SHORT
                    ).show();
                });
        }
    }
    
    private void sendParticipantConfirmation(ParticipantRequest request) {
        // This method would send a notification or update to the participant
        // via the backend API to confirm their participation
        Log.d(TAG, "Would send confirmation to participant: " + request.getStudentName());
    }
    
    private void removeParticipantViaApi(ParticipantRequest request) {
        // Get auth token
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String token = prefs.getString(Constants.KEY_TOKEN, "");
        
        if (token.isEmpty()) {
            Log.e(TAG, "Cannot remove participant via API: No authentication token");
            return;
        }
        
        Log.d(TAG, "Attempting to remove participant via API: " + request.getStudentId() + 
               " from appointment: " + request.getAppointmentId());
        
        // Implementation would call your backend API
        // This is optional and would depend on whether you want to sync with backend
    }

    private void showConfirmationDialog(String title, String message, Runnable onConfirm, String positiveButtonText, String negativeButtonText) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText, (dialog, which) -> onConfirm.run())
                .setNegativeButton(negativeButtonText, null)
                .show();
    }

    // Keep the old method for backward compatibility
    private void showConfirmationDialog(String title, String message, Runnable onConfirm) {
        showConfirmationDialog(title, message, onConfirm, "Yes", "No");
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        recyclerParticipants.setVisibility(View.GONE);
        emptyStateView.setVisibility(View.VISIBLE);
        Log.d(TAG, "Showing empty state");
        // Try to update the empty state text directly
        try {
            if (emptyStateView.getChildCount() >= 3) {
                View titleView = emptyStateView.getChildAt(1);
                View detailsView = emptyStateView.getChildAt(2);
                
                if (titleView instanceof TextView) {
                    String currentAppointment = (String) appointmentSpinner.getSelectedItem();
                    if (currentAppointment != null && !currentAppointment.equals("Select Appointment")) {
                        ((TextView) titleView).setText("No Participant Requests Found");
                        if (detailsView instanceof TextView) {
                            ((TextView) detailsView).setText("There are no participants to manage for this appointment.");
                        }
                    } else {
                        ((TextView) titleView).setText("Select an Appointment First");
                        if (detailsView instanceof TextView) {
                            ((TextView) detailsView).setText("Choose an appointment from the dropdown to view and manage its participants.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating empty state text", e);
        }
    }

    private void hideEmptyState() {
        recyclerParticipants.setVisibility(View.VISIBLE);
        emptyStateView.setVisibility(View.GONE);
        Log.d(TAG, "Hiding empty state");
    }
} 