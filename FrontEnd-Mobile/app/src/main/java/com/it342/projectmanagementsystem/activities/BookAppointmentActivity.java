package com.it342.projectmanagementsystem.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.api.ApiService;
import com.it342.projectmanagementsystem.api.RetrofitClient;
import com.it342.projectmanagementsystem.models.Appointment;
import com.it342.projectmanagementsystem.models.FacultyAppointmentRequest;
import com.it342.projectmanagementsystem.models.Faculty;
import com.it342.projectmanagementsystem.models.TimestampObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Map;
import java.util.HashMap;

public class BookAppointmentActivity extends AppCompatActivity {
    private static final String TAG = "BookAppointmentActivity";
    private EditText etFullName, etEmail, etDescription, etReason, etDate, etTime;
    private AutoCompleteTextView actvFaculty;
    private EditText etDuration;
    private Button btnSubmit;
    private Calendar selectedDateTime;
    private List<Faculty> facultyList = new ArrayList<>();
    private Map<String, Faculty> facultyMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);

        initializeViews();
        setupClickListeners();
        loadUserData();
        fetchFacultyData();
    }

    private void initializeViews() {
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etDescription = findViewById(R.id.etDescription);
        etReason = findViewById(R.id.etReason);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etDuration = findViewById(R.id.etDuration);
        actvFaculty = findViewById(R.id.actvFaculty);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Set default duration to 1 hour
        etDuration.setText("60");

        // Initialize navigation buttons with correct type
        MaterialButton btnHome = findViewById(R.id.btnHome);
        MaterialButton btnAppointments = findViewById(R.id.btnAppointments);
        MaterialButton btnBook = findViewById(R.id.btnBook);
        MaterialButton btnMenu = findViewById(R.id.btnMenu);

        // Set navigation click listeners
        btnHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomePage.class));
            finish();
        });

        btnAppointments.setOnClickListener(v -> {
            recreate();
        });

        btnBook.setOnClickListener(v -> {
            // Already on Book page
        });

        btnMenu.setOnClickListener(v -> {
            // Handle menu click
        });
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String fullName = prefs.getString("firstName", "") + " " + prefs.getString("lastName", "");
        String email = prefs.getString("email", "");
        
        etFullName.setText(fullName);
        etEmail.setText(email);
        
        // Make name and email non-editable since they come from the logged-in user
        etFullName.setEnabled(false);
        etEmail.setEnabled(false);
    }

    private void setupClickListeners() {
        selectedDateTime = Calendar.getInstance();
        selectedDateTime.setTimeZone(TimeZone.getTimeZone("Asia/Manila")); // Set to Philippine timezone

        etDate.setOnClickListener(v -> showDatePicker());
        etTime.setOnClickListener(v -> showTimePicker());
        btnSubmit.setOnClickListener(v -> submitAppointment());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                selectedDateTime.set(Calendar.YEAR, year);
                selectedDateTime.set(Calendar.MONTH, month);
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateField();
            },
            selectedDateTime.get(Calendar.YEAR),
            selectedDateTime.get(Calendar.MONTH),
            selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, hourOfDay, minute) -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDateTime.set(Calendar.MINUTE, minute);
                updateTimeField();
            },
            selectedDateTime.get(Calendar.HOUR_OF_DAY),
            selectedDateTime.get(Calendar.MINUTE),
            false
        );
        timePickerDialog.show();
    }

    private void updateDateField() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
        etDate.setText(dateFormat.format(selectedDateTime.getTime()));
    }

    private void updateTimeField() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
        timeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
        etTime.setText(timeFormat.format(selectedDateTime.getTime()));
    }

    private void fetchFacultyData() {
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");

        ApiService apiService = RetrofitClient.getInstance().getApiService();
        Call<List<Faculty>> call = apiService.getAllFaculties("Bearer " + token);

        call.enqueue(new Callback<List<Faculty>>() {
            @Override
            public void onResponse(Call<List<Faculty>> call, Response<List<Faculty>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    facultyList = response.body();
                    setupFacultyDropdown();
                } else {
                    Toast.makeText(BookAppointmentActivity.this,
                        "Failed to fetch faculty list", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Faculty>> call, Throwable t) {
                Toast.makeText(BookAppointmentActivity.this,
                    "Network error while fetching faculty list", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFacultyDropdown() {
        List<String> facultyNames = new ArrayList<>();
        facultyMap.clear();
        
        for (Faculty faculty : facultyList) {
            String displayName = faculty.getFullName();
            facultyNames.add(displayName);
            facultyMap.put(displayName, faculty);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            facultyNames
        );
        actvFaculty.setAdapter(adapter);
    }

    private void submitAppointment() {
        if (!validateInputs()) {
            return;
        }

        // Get the auth token
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");

        try {
            // Format date and time
            Calendar startTime = (Calendar) selectedDateTime.clone();
            Calendar endTime = (Calendar) startTime.clone();
            
            // Get duration in minutes
            int durationMinutes = Integer.parseInt(etDuration.getText().toString());
            endTime.add(Calendar.MINUTE, durationMinutes);

            String selectedFacultyName = actvFaculty.getText().toString().trim();
            Faculty selectedFaculty = facultyMap.get(selectedFacultyName);
            
            if (selectedFaculty == null) {
                Log.e(TAG, "Selected faculty is null for name: " + selectedFacultyName);
                Toast.makeText(this, "Please select a valid faculty member", Toast.LENGTH_SHORT).show();
                return;
            }

            String facultyUserId = selectedFaculty.getUserId();
            if (facultyUserId == null || facultyUserId.isEmpty()) {
                Log.e(TAG, "Faculty User ID is null or empty for faculty: " + selectedFacultyName);
                Toast.makeText(this, "Invalid faculty selection (missing user ID)", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Creating appointment with faculty: " + selectedFacultyName + " (User ID: " + facultyUserId + ")");

            // Create the request object
            FacultyAppointmentRequest request = new FacultyAppointmentRequest();
            request.setTitle("Meeting with " + selectedFaculty.getFullName());
            request.setDescription(etDescription.getText().toString().trim() + "\nReason: " + etReason.getText().toString().trim());
            
            // Create TimestampObject instances
            TimestampObject startTimeObj = TimestampObject.fromMillis(startTime.getTimeInMillis());
            TimestampObject endTimeObj = TimestampObject.fromMillis(endTime.getTimeInMillis());
            
            request.setStartTime(startTimeObj);
            request.setEndTime(endTimeObj);
            request.setUserId(facultyUserId);
            
            Log.d(TAG, "Appointment request details:");
            Log.d(TAG, "Title: " + request.getTitle());
            Log.d(TAG, "Description: " + request.getDescription());
            Log.d(TAG, "Start Time: " + startTimeObj);
            Log.d(TAG, "End Time: " + endTimeObj);
            Log.d(TAG, "Faculty User ID: " + request.getUserId());
            
            // Make API call
            ApiService apiService = RetrofitClient.getInstance().getApiService();
            Call<Appointment> call = apiService.createAppointment(request, "Bearer " + token);
            
            call.enqueue(new Callback<Appointment>() {
                @Override
                public void onResponse(Call<Appointment> call, Response<Appointment> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "Appointment created successfully");
                        Toast.makeText(BookAppointmentActivity.this, 
                            "Appointment request sent successfully!", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(BookAppointmentActivity.this, HomePage.class));
                        finish();
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? 
                                response.errorBody().string() : "Unknown error";
                            Log.e(TAG, "Failed to create appointment. Error: " + errorBody);
                            Toast.makeText(BookAppointmentActivity.this,
                                "Failed to create appointment: " + errorBody, Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                }

                @Override
                public void onFailure(Call<Appointment> call, Throwable t) {
                    Log.e(TAG, "Network error creating appointment", t);
                    Toast.makeText(BookAppointmentActivity.this,
                        "Network error. Please check your connection.", Toast.LENGTH_LONG).show();
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid duration format", e);
            Toast.makeText(this, "Please enter a valid duration", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error creating appointment", e);
            Toast.makeText(this, "Error creating appointment: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean validateInputs() {
        String selectedFacultyName = actvFaculty.getText().toString().trim();
        if (selectedFacultyName.isEmpty() || facultyMap.get(selectedFacultyName) == null) {
            actvFaculty.setError("Please select a faculty member");
            return false;
        }
        if (etDescription.getText().toString().trim().isEmpty()) {
            etDescription.setError("Please enter description");
            return false;
        }
        if (etReason.getText().toString().trim().isEmpty()) {
            etReason.setError("Please enter reason for meeting");
            return false;
        }
        if (etDate.getText().toString().trim().isEmpty()) {
            etDate.setError("Please select date");
            return false;
        }
        if (etTime.getText().toString().trim().isEmpty()) {
            etTime.setError("Please select time");
            return false;
        }
        try {
            int duration = Integer.parseInt(etDuration.getText().toString().trim());
            if (duration <= 0) {
                etDuration.setError("Duration must be greater than 0 minutes");
                return false;
            }
        } catch (NumberFormatException e) {
            etDuration.setError("Please enter a valid duration in minutes");
            return false;
        }
        return true;
    }
} 