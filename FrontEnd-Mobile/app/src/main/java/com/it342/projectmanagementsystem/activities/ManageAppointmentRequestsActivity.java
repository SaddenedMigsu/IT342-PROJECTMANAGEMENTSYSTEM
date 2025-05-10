package com.it342.projectmanagementsystem.activities;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.adapters.AppointmentRequestAdapter;
import com.it342.projectmanagementsystem.api.ApiService;
import com.it342.projectmanagementsystem.api.RetrofitClient;
import com.it342.projectmanagementsystem.models.Appointment;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Date;
import java.lang.reflect.Method;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.Calendar;

public class ManageAppointmentRequestsActivity extends AppCompatActivity {
    private static final String TAG = "ManageAppointmentRequests";
    private RecyclerView rvAppointmentRequests;
    private AppointmentRequestAdapter adapter;
    private List<Appointment> appointmentRequestList;
    private ApiService apiService;
    private ImageButton btnBack;
    private ChipGroup chipGroup;
    private LinearLayout emptyStateView;
    private String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_appointment_requests);

        // Initialize views
        rvAppointmentRequests = findViewById(R.id.rvAppointmentRequests);
        btnBack = findViewById(R.id.btnBack);
        chipGroup = findViewById(R.id.chipGroup);
        emptyStateView = findViewById(R.id.emptyStateView);
        
        rvAppointmentRequests.setLayoutManager(new LinearLayoutManager(this));

        // Initialize list and adapter
        appointmentRequestList = new ArrayList<>();
        adapter = new AppointmentRequestAdapter(this, appointmentRequestList);
        rvAppointmentRequests.setAdapter(adapter);

        // Initialize API service
        apiService = RetrofitClient.getInstance().getApiService();

        // Set up back button
        btnBack.setOnClickListener(v -> onBackPressed());

        // Set up filter chips
        setupFilterChips();

        // Load appointment requests
        loadAppointmentRequests();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the appointment list when returning to this activity
        loadAppointmentRequests();
    }

    private void loadAppointmentRequests() {
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");
        String facultyUserId = prefs.getString("userId", ""); // Get logged-in faculty's user ID
        String facultyEmail = prefs.getString("email", ""); // Get faculty email for Firestore query

        if (token.isEmpty() || facultyUserId.isEmpty()) { // Check facultyUserId as well
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_SHORT).show();
            // Optionally navigate back to login
            finish();
            return;
        }

        Log.d(TAG, "Fetching appointments for faculty ID: " + facultyUserId);
        
        // First, get appointments from the API
        fetchAppointmentsFromApi(token, facultyUserId, facultyEmail);
    }

    private void fetchAppointmentsFromApi(String token, String facultyUserId, String facultyEmail) {
        // Use getUserAppointments instead of getAppointments
        Call<List<Appointment>> call = apiService.getUserAppointments(facultyUserId, "Bearer " + token);

        call.enqueue(new Callback<List<Appointment>>() {
            @Override
            public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Successfully fetched appointments for faculty from API.");
                    List<Appointment> facultyAppointments = response.body();
                    
                    // Add these appointments to a map to avoid duplicates later
                    Map<String, Appointment> appointmentMap = new HashMap<>();
                    for (Appointment appointment : facultyAppointments) {
                        String id = appointment.getId();
                        if (id != null && !id.isEmpty()) {
                            // For each API appointment, fetch additional details from Firestore
                            fetchAppointmentDetailsFromFirestore(id, appointment, appointmentMap);
                            
                            // Log appointment details
                            Log.d(TAG, "API Appointment: ID=" + id + ", Title=" + appointment.getTitle() + 
                                  ", Status=" + appointment.getStatus());
                        }
                    }
                    
                    // Now also fetch from Firestore to get newest appointments
                    // We'll only call this once to avoid clearing the appointments
                    if (!facultyAppointments.isEmpty()) {
                        fetchAppointmentsFromFirestore(facultyUserId, facultyEmail, appointmentMap);
                    } else {
                        // If no appointments from API, check Firestore anyway
                        fetchAppointmentsFromFirestore(facultyUserId, facultyEmail, appointmentMap);
                    }
                } else {
                    String errorMsg = "Failed to load appointment requests from API.";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " Error: " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, errorMsg + " Code: " + response.code());
                    Toast.makeText(ManageAppointmentRequestsActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    
                    // Even if API fails, try Firestore
                    fetchAppointmentsFromFirestore(facultyUserId, facultyEmail, new HashMap<>());
                }
            }

            @Override
            public void onFailure(Call<List<Appointment>> call, Throwable t) {
                Log.e(TAG, "Network error fetching appointments from API", t);
                Toast.makeText(ManageAppointmentRequestsActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                
                // Even if API fails, try Firestore
                fetchAppointmentsFromFirestore(facultyUserId, facultyEmail, new HashMap<>());
            }
        });
    }
    
    // New method to fetch appointment details from Firestore for appointments from API
    private void fetchAppointmentDetailsFromFirestore(String appointmentId, Appointment appointment, Map<String, Appointment> appointmentMap) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Fetch the specific appointment document to get full details
        db.collection("appointments").document(appointmentId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Log.d(TAG, "Found Firestore details for API appointment: " + appointmentId);
                    
                    // Get the data from Firestore
                    Map<String, Object> data = documentSnapshot.getData();
                    if (data != null) {
                        Log.d(TAG, "Firestore data for appointment " + appointmentId + ": " + data);
                        
                        // Extract creator name from createdBy (email)
                        String creatorName = null;
                        
                        // Prioritize createdBy field (email)
                        if (data.containsKey("createdBy")) {
                            String createdBy = (String) data.get("createdBy");
                            Log.d(TAG, "Found createdBy: " + createdBy + " for appointment: " + appointmentId);
                            
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
                            }
                        }
                        
                        // Set creatorName if found
                        if (creatorName != null && !creatorName.isEmpty()) {
                            appointment.setCreatorName(creatorName);
                            Log.d(TAG, "Set creator name to: " + creatorName + " for API appointment: " + appointmentId);
                        } else {
                            // Default to "Student" if we couldn't find a name
                            appointment.setCreatorName("Student");
                            Log.d(TAG, "Set default creator name for API appointment: " + appointmentId);
                        }
                        
                        // Update the appointment in the map
                        appointmentMap.put(appointmentId, appointment);
                        
                        // Update the UI immediately after processing this appointment
                        updateUIWithCurrentAppointments(appointmentMap.values());
                    }
                } else {
                    Log.d(TAG, "No Firestore document found for API appointment: " + appointmentId);
                    // Still add the appointment to the map even if no Firestore details
                    appointmentMap.put(appointmentId, appointment);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching Firestore details for API appointment: " + appointmentId, e);
                // Still add the appointment to the map even if Firestore fetch fails
                appointmentMap.put(appointmentId, appointment);
            });
    }
    
    private void fetchAppointmentsFromFirestore(String facultyUserId, String facultyEmail, Map<String, Appointment> existingAppointments) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Add log to indicate we're attempting to fetch from Firestore
        Log.d(TAG, "Querying Firestore for appointments with facultyId=" + facultyUserId);
        
        // Only query appointments assigned to this faculty
        Query baseQuery = db.collection("appointments");
        
        // First query: Get appointments assigned to this faculty only
        baseQuery.whereEqualTo("facultyId", facultyUserId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Log.d(TAG, "Successfully fetched " + queryDocumentSnapshots.size() + " appointments from Firestore by facultyId");
                
                for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    String appointmentId = document.getId();
                    
                    // Log each document ID we get from Firestore
                    Log.d(TAG, "Firestore appointment document: " + appointmentId);
                    
                    // Only add if not already in our map
                    if (!existingAppointments.containsKey(appointmentId)) {
                        try {
                            // Log all fields in the document for debugging
                            Map<String, Object> data = document.getData();
                            Log.d(TAG, "Document " + appointmentId + " data: " + data);
                            
                            Appointment appointment = document.toObject(Appointment.class);
                            appointment.setId(appointmentId);
                            appointment.setAppointmentId(appointmentId);
                            
                            // Extract creator name from createdBy (email)
                            String creatorName = null;
                            
                            // Prioritize createdBy field (email)
                            if (data.containsKey("createdBy")) {
                                String createdBy = (String) data.get("createdBy");
                                Log.d(TAG, "Found createdBy: " + createdBy + " for appointment: " + appointmentId);
                                
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
                                }
                            }
                            
                            // Set creatorName if found
                            if (creatorName != null && !creatorName.isEmpty()) {
                                appointment.setCreatorName(creatorName);
                                Log.d(TAG, "Set creator name to: " + creatorName + " for appointment: " + appointmentId);
                            } else {
                                // Default to "Student" if we couldn't find a name
                                appointment.setCreatorName("Student");
                                Log.d(TAG, "Set default creator name for appointment: " + appointmentId);
                            }
                            
                            existingAppointments.put(appointmentId, appointment);
                            Log.d(TAG, "Added appointment from Firestore: " + appointmentId + " - " + appointment.getTitle() + 
                                  ", Status=" + appointment.getStatus() + ", Creator: " + appointment.getCreatorName());
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting Firestore document to Appointment", e);
                        }
                    }
                }
                
                // Display the appointments - no longer looking for specific appointments
                filterAndDisplayAppointments(existingAppointments.values());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching appointments from Firestore", e);
                filterAndDisplayAppointments(existingAppointments.values());
            });
    }
    
    private void updateUIWithCurrentAppointments(Collection<Appointment> appointments) {
        // Clear and update the main list
        appointmentRequestList.clear();
        appointmentRequestList.addAll(appointments);
        // Apply current filter
        filterAndDisplayAppointments(appointmentRequestList);
    }

    private void filterAndDisplayAppointments(Collection<Appointment> allAppointments) {
        List<Appointment> filteredList = new ArrayList<>();
        
        for (Appointment appointment : allAppointments) {
            String status = appointment.getStatus();
            if (status == null) {
                status = "PENDING_APPROVAL"; // Default status if null
                appointment.setStatus(status);
            }

            if (currentFilter.equals("All") || 
                (currentFilter.equals("Pending") && (status.equals("PENDING_APPROVAL") || status.equals("Pending"))) ||
                (currentFilter.equals("Approved") && (status.equals("ACCEPTED") || status.equals("SCHEDULED") || status.equals("Approved"))) ||
                (currentFilter.equals("Rejected") && (status.equals("REJECTED") || status.equals("Rejected")))) {
                filteredList.add(appointment);
            }
        }

        // Update the adapter with filtered list without clearing the main list
        adapter = new AppointmentRequestAdapter(this, filteredList);
        rvAppointmentRequests.setAdapter(adapter);

        // Show/hide empty state
        if (filteredList.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            rvAppointmentRequests.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            rvAppointmentRequests.setVisibility(View.VISIBLE);
        }
    }

    private void setupFilterChips() {
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) {
                currentFilter = "All";
            } else if (checkedId == R.id.chipPending) {
                currentFilter = "Pending";
            } else if (checkedId == R.id.chipApproved) {
                currentFilter = "Approved";
            } else if (checkedId == R.id.chipRejected) {
                currentFilter = "Rejected";
            }
            // Filter from the main list instead of the filtered list
            filterAndDisplayAppointments(appointmentRequestList);
        });
    }
    
    // Helper method to get appointment time in milliseconds for sorting
    private long getAppointmentTimeMillis(Appointment appointment) {
        try {
            Object startTimeObj = appointment.getStartTime();
            if (startTimeObj != null && startTimeObj instanceof com.google.firebase.Timestamp) {
                return ((com.google.firebase.Timestamp) startTimeObj).toDate().getTime();
            }
        } catch (Exception e) {
            try {
                // Try alternate method
                Object startTimeObj = appointment.getClass().getMethod("getStartTimeValue").invoke(appointment);
                if (startTimeObj != null && startTimeObj instanceof com.google.firebase.Timestamp) {
                    return ((com.google.firebase.Timestamp) startTimeObj).toDate().getTime();
                }
            } catch (Exception ignored) {
                // Couldn't get time
            }
        }
        return Long.MAX_VALUE; // Default to sort at the end if time can't be determined
    }
} 