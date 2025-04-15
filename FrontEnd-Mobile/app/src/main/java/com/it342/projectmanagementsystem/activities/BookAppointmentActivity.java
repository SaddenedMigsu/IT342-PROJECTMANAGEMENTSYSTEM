package com.it342.projectmanagementsystem.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
import com.it342.projectmanagementsystem.models.AppointmentRequest;
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

public class BookAppointmentActivity extends AppCompatActivity {
    private static final String TAG = "BookAppointmentActivity";
    private EditText etFullName, etEmail, etDescription, etReason, etDate, etTime, etFaculty;
    private Button btnSubmit;
    private Calendar selectedDateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);

        initializeViews();
        setupClickListeners();
        loadUserData();
    }

    private void initializeViews() {
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etDescription = findViewById(R.id.etDescription);
        etReason = findViewById(R.id.etReason);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etFaculty = findViewById(R.id.etFaculty);
        btnSubmit = findViewById(R.id.btnSubmit);

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

    private void submitAppointment() {
        if (!validateInputs()) {
            return;
        }

        // Get the auth token
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");
        String userId = prefs.getString("userId", "");

        try {
            // Create appointment data
            Appointment appointment = new Appointment();
            appointment.setTitle("Meeting with " + etFaculty.getText().toString().trim());
            appointment.setDescription(etDescription.getText().toString().trim() + "\nReason: " + etReason.getText().toString().trim());
            
            // Format date and time in ISO 8601
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            
            // Format date and time
            Calendar startTime = (Calendar) selectedDateTime.clone();
            Calendar endTime = (Calendar) startTime.clone();
            endTime.add(Calendar.HOUR_OF_DAY, 1);

            // Convert Calendar to ISO 8601 string
            String startTimeStr = isoFormat.format(startTime.getTime());
            String endTimeStr = isoFormat.format(endTime.getTime());

            // Create the request object
            AppointmentRequest request = new AppointmentRequest();
            request.setTitle("Meeting with " + etFaculty.getText().toString().trim());
            request.setDescription(etDescription.getText().toString().trim() + "\nReason: " + etReason.getText().toString().trim());
            request.setStartTime(startTimeStr);
            request.setEndTime(endTimeStr);
            request.setCreatedBy(userId);
            request.setRole("student");  // Set the role explicitly
            
            // Add faculty as participant
            List<String> participants = new ArrayList<>();
            participants.add(etFaculty.getText().toString().trim());
            request.setParticipants(participants);
            
            // Make API call
            ApiService apiService = RetrofitClient.getInstance().getApiService();
            Call<Appointment> call = apiService.createAppointment(request, "Bearer " + token);
            
            call.enqueue(new Callback<Appointment>() {
                @Override
                public void onResponse(Call<Appointment> call, Response<Appointment> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(BookAppointmentActivity.this, 
                            "Appointment request sent successfully!", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(BookAppointmentActivity.this, HomePage.class));
                        finish();
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? 
                                response.errorBody().string() : "Unknown error";
                            Toast.makeText(BookAppointmentActivity.this,
                                "Failed to create appointment: " + errorBody, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Error: " + errorBody);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFailure(Call<Appointment> call, Throwable t) {
                    Log.e(TAG, "Error creating appointment", t);
                    Toast.makeText(BookAppointmentActivity.this,
                        "Network error. Please check your connection.", Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating appointment", e);
            Toast.makeText(this, "Error creating appointment: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean validateInputs() {
        String facultyName = etFaculty.getText().toString().trim();
        if (facultyName.isEmpty()) {
            etFaculty.setError("Please enter faculty name");
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
        return true;
    }
} 