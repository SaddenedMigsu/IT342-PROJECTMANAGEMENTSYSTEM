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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageAppointmentRequestsActivity extends AppCompatActivity {
    private static final String TAG = "ManageAppointmentRequests";
    private RecyclerView rvAppointmentRequests;
    private AppointmentRequestAdapter adapter;
    private List<Appointment> appointmentRequestList;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_appointment_requests);

        // Initialize views
        rvAppointmentRequests = findViewById(R.id.rvAppointmentRequests);
        rvAppointmentRequests.setLayoutManager(new LinearLayoutManager(this));

        // Initialize list and adapter
        appointmentRequestList = new ArrayList<>();
        adapter = new AppointmentRequestAdapter(this, appointmentRequestList);
        rvAppointmentRequests.setAdapter(adapter);

        // Initialize API service
        apiService = RetrofitClient.getInstance().getApiService();

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

        if (token.isEmpty() || facultyUserId.isEmpty()) { // Check facultyUserId as well
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_SHORT).show();
            // Optionally navigate back to login
            finish();
            return;
        }

        Log.d(TAG, "Fetching appointments for faculty ID: " + facultyUserId);
        // Use getUserAppointments instead of getAppointments
        Call<List<Appointment>> call = apiService.getUserAppointments(facultyUserId, "Bearer " + token);

        call.enqueue(new Callback<List<Appointment>>() {
            @Override
            public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Successfully fetched appointments for faculty.");
                    List<Appointment> facultyAppointments = response.body();
                    
                    // Filter for both PENDING_APPROVAL and SCHEDULED statuses
                    List<Appointment> filteredAppointments = facultyAppointments.stream()
                            .filter(app -> {
                                String status = app.getStatus();
                                return "PENDING_APPROVAL".equalsIgnoreCase(status) || 
                                       "SCHEDULED".equalsIgnoreCase(status);
                            })
                            .collect(Collectors.toList());

                    Log.d(TAG, "Found " + filteredAppointments.size() + " pending or scheduled appointment requests.");

                    appointmentRequestList.clear();
                    appointmentRequestList.addAll(filteredAppointments);
                    adapter.notifyDataSetChanged();

                    if (filteredAppointments.isEmpty()) {
                        Toast.makeText(ManageAppointmentRequestsActivity.this, "No appointment requests found.", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    String errorMsg = "Failed to load appointment requests.";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " Error: " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, errorMsg + " Code: " + response.code());
                    Toast.makeText(ManageAppointmentRequestsActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Appointment>> call, Throwable t) {
                Log.e(TAG, "Network error fetching appointments", t);
                Toast.makeText(ManageAppointmentRequestsActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
} 