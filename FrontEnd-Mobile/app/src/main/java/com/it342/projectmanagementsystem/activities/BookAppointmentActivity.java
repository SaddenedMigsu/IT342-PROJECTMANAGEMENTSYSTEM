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
import androidx.appcompat.app.AlertDialog;
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
import com.it342.projectmanagementsystem.models.LoginRequest;
import com.it342.projectmanagementsystem.models.AuthResponse;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Map;
import java.util.HashMap;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.it342.projectmanagementsystem.utils.NotificationHelper;

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
            request.setLocation("Virtual Meeting");
            request.setType("FACULTY");
            
            // Create TimestampObject instances
            TimestampObject startTimeObj = TimestampObject.fromMillis(startTime.getTimeInMillis());
            TimestampObject endTimeObj = TimestampObject.fromMillis(endTime.getTimeInMillis());
            
            request.setStartTime(startTimeObj);
            request.setEndTime(endTimeObj);
            request.setUserId(facultyUserId);

            // Get the current user's ID from SharedPreferences
            String currentUserId = prefs.getString("userId", "");
            if (currentUserId != null && !currentUserId.isEmpty()) {
                List<String> participants = new ArrayList<>();
                participants.add(currentUserId); // Add student
                participants.add(facultyUserId); // Add faculty
                request.setParticipants(participants);
                Log.d(TAG, "Added current user to participants: " + currentUserId + " and faculty: " + facultyUserId);
            }
            
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
                        
                        // Get current user's name for notification
                        String studentFirstName = prefs.getString("firstName", "");
                        String studentLastName = prefs.getString("lastName", "");
                        String studentName = (studentFirstName + " " + studentLastName).trim();
                        if (studentName.isEmpty()) {
                            studentName = "A student";
                        }
                        
                        // Create a notification for the faculty
                        try {
                            Appointment createdAppointment = response.body();
                            NotificationHelper.createAppointmentRequestNotification(
                                createdAppointment,
                                studentName,
                                facultyUserId
                            );
                            Log.d(TAG, "Notification created for faculty: " + facultyUserId);
                        } catch (Exception e) {
                            Log.e(TAG, "Error creating notification", e);
                        }
                        
                        Toast.makeText(BookAppointmentActivity.this, 
                            "Appointment request sent successfully!", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(BookAppointmentActivity.this, HomePage.class));
                        finish();
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? 
                                response.errorBody().string() : "Unknown error";
                            Log.e(TAG, "Failed to create appointment. Error: " + errorBody + ", Code: " + response.code());
                            
                            // Try to parse the error as JSON to check for conflicts
                            if (errorBody.contains("conflicts")) {
                                handleConflictError(errorBody);
                            } else if (response.code() == 403) {
                                // Handle authentication error
                                Toast.makeText(BookAppointmentActivity.this,
                                    "You don't have permission to create appointments. Please check your account type and login status.", 
                                    Toast.LENGTH_LONG).show();
                                    
                                // Try direct JSON method as fallback
                                makeDirectApiCall(request, token);
                            } else {
                                // Default error message for non-conflict errors
                                Toast.makeText(BookAppointmentActivity.this,
                                    "Failed to create appointment: " + errorBody, Toast.LENGTH_LONG).show();
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                            Toast.makeText(BookAppointmentActivity.this,
                                "Failed to create appointment: Unknown error", Toast.LENGTH_LONG).show();
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

    private void handleConflictError(String errorBody) {
        try {
            JSONObject errorJson = new JSONObject(errorBody);
            if (errorJson.has("conflicts")) {
                JSONArray conflicts = errorJson.getJSONArray("conflicts");
                if (conflicts.length() > 0) {
                    // Get the first conflict
                    JSONObject conflict = conflicts.getJSONObject(0);
                    
                    // Get conflict details
                    String conflictingTitle = conflict.optString("conflictingTitle", "Unknown appointment");
                    String conflictingStudent = conflict.optString("conflictingStudent", "Unknown student");
                    
                    // Get conflict times if available
                    String timeInfo = "";
                    if (conflict.has("conflictingStartTime")) {
                        JSONObject startTime = conflict.getJSONObject("conflictingStartTime");
                        JSONObject endTime = conflict.getJSONObject("conflictingEndTime");
                        
                        // Convert seconds to date format if needed
                        long startSeconds = startTime.optLong("seconds", 0);
                        long endSeconds = endTime.optLong("seconds", 0);
                        
                        Log.d(TAG, "Conflict timestamps - start seconds: " + startSeconds + ", end seconds: " + endSeconds);
                        
                        if (startSeconds > 0 && endSeconds > 0) {
                            // Convert to readable time
                            Calendar startCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"));
                            Calendar endCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"));
                            
                            startCal.setTimeInMillis(startSeconds * 1000);
                            endCal.setTimeInMillis(endSeconds * 1000);
                            
                            Log.d(TAG, "Raw start time: " + startCal.getTime());
                            Log.d(TAG, "Raw end time: " + endCal.getTime());
                            
                            // Format just time for message display
                            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                            timeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
                            
                            String formattedStart = timeFormat.format(startCal.getTime());
                            String formattedEnd = timeFormat.format(endCal.getTime());
                            
                            // Get date for the message
                            SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                            dateFormatter.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
                            String dateString = dateFormatter.format(startCal.getTime());
                            
                            Log.d(TAG, "Formatted date: " + dateString);
                            Log.d(TAG, "Formatted times - start: " + formattedStart + ", end: " + formattedEnd);
                            
                            timeInfo = " on " + dateString + " from " + formattedStart + " to " + formattedEnd;
                        }
                    }
                    
                    // Create a clean message
                    String conflictMessage = "This appointment conflicts with \"" + conflictingTitle + "\"" + 
                                            timeInfo + " with " + conflictingStudent + ".";
                    
                    // Show in a nice dialog
                    new AlertDialog.Builder(this)
                        .setTitle("Schedule Conflict")
                        .setMessage(conflictMessage)
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                    
                    return;
                }
            }
            
            // Fallback for other conflict formats or if we couldn't parse specific details
            new AlertDialog.Builder(this)
                .setTitle("Schedule Conflict")
                .setMessage("This appointment conflicts with an existing schedule. Please choose a different time.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing conflict JSON", e);
            
            // Fallback message if JSON parsing fails
            new AlertDialog.Builder(this)
                .setTitle("Schedule Conflict")
                .setMessage("This appointment conflicts with an existing schedule. Please choose a different time.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        }
    }

    private void makeDirectApiCall(FacultyAppointmentRequest request, String token) {
        try {
            // Create a direct JSON object
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("title", request.getTitle());
            jsonBody.put("description", request.getDescription());
            jsonBody.put("startTime", request.getStartTime());
            jsonBody.put("endTime", request.getEndTime());
            jsonBody.put("userId", request.getUserId());
            jsonBody.put("type", "FACULTY");
            jsonBody.put("location", "Virtual Meeting");
            
            // Create RequestBody
            RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonBody.toString()
            );
            
            // Create Request
            Request okRequest = new Request.Builder()
                .url("https://it342-projectmanagementsystem.onrender.com/api/appointments/request-faculty")
                .addHeader("Authorization", "Bearer " + token)
                .post(requestBody)
                .build();
            
            // Create OkHttpClient
            OkHttpClient client = new OkHttpClient.Builder()
                .build();
            
            // Execute the request asynchronously
            client.newCall(okRequest).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Direct API call failed", e);
                        Toast.makeText(BookAppointmentActivity.this,
                            "Network error. Please check your connection.", Toast.LENGTH_LONG).show();
                    });
                }
                
                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                    runOnUiThread(() -> {
                        try {
                            if (response.isSuccessful()) {
                                Log.d(TAG, "Appointment created successfully via direct API call");
                                
                                // Get appointment ID from response
                                String responseBody = response.body().string();
                                JSONObject responseJson = new JSONObject(responseBody);
                                String appointmentId = responseJson.optString("id", "");
                                
                                // Get current user's name for notification
                                SharedPreferences sharedPrefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
                                String studentFirstName = sharedPrefs.getString("firstName", "");
                                String studentLastName = sharedPrefs.getString("lastName", "");
                                String studentName = (studentFirstName + " " + studentLastName).trim();
                                if (studentName.isEmpty()) {
                                    studentName = "A student";
                                }
                                
                                // Create a notification for the faculty
                                if (!appointmentId.isEmpty()) {
                                    // Create a simplified appointment object for the notification
                                    Appointment createdAppointment = new Appointment();
                                    createdAppointment.setId(appointmentId);
                                    createdAppointment.setTitle(request.getTitle());
                                    
                                    // Try to parse and set the date/time
                                    try {
                                        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                                        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                                        
                                        if (request.getStartTime() != null) {
                                            com.google.firebase.Timestamp startTime = 
                                                new com.google.firebase.Timestamp(isoFormat.parse(request.getStartTime()));
                                            createdAppointment.setStartTime(startTime);
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error parsing date/time for notification", e);
                                    }
                                    
                                    NotificationHelper.createAppointmentRequestNotification(
                                        createdAppointment,
                                        studentName,
                                        request.getUserId()
                                    );
                                    Log.d(TAG, "Notification created for faculty: " + request.getUserId());
                                }
                                
                                Toast.makeText(BookAppointmentActivity.this, 
                                    "Appointment request sent successfully!", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(BookAppointmentActivity.this, HomePage.class));
                                finish();
                            } else {
                                String errorBody = response.body().string();
                                Log.e(TAG, "Direct API call failed. Error: " + errorBody);
                                Toast.makeText(BookAppointmentActivity.this,
                                    "Failed to create appointment: " + errorBody, Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing direct API response", e);
                            Toast.makeText(BookAppointmentActivity.this,
                                "Error processing response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error making direct API call", e);
            Toast.makeText(this, "Error creating appointment: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
} 