package com.it342.projectmanagementsystem.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.api.ApiService;
import com.it342.projectmanagementsystem.api.RetrofitClient;
import com.it342.projectmanagementsystem.models.Appointment;
import com.it342.projectmanagementsystem.models.TimestampObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class HomePage extends AppCompatActivity {
    private static final String TAG = "HomePage";
    private LinearLayout appointmentsContainer;
    private TextView welcomeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        initializeViews();
        setupNavigation();
        loadUserData();
        fetchAppointments();
    }

    private void initializeViews() {
        appointmentsContainer = findViewById(R.id.appointmentsContainer);
        welcomeText = findViewById(R.id.welcomeText);
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

        if (token.isEmpty()) {
            Log.e(TAG, "Missing token");
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Fetching all appointments");
        ApiService apiService = RetrofitClient.getInstance().getApiService();
        Call<List<Appointment>> call = apiService.getAppointments("Bearer " + token);

        call.enqueue(new Callback<List<Appointment>>() {
            @Override
            public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Appointment> appointments = response.body();
                    Log.d(TAG, "Received " + appointments.size() + " appointments");
                    displayAppointments(appointments);
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? 
                            response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "Failed to load appointments: " + errorBody);
                        Toast.makeText(HomePage.this, 
                            "Failed to load appointments: " + errorBody, Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Appointment>> call, Throwable t) {
                Log.e(TAG, "Error fetching appointments", t);
                Toast.makeText(HomePage.this,
                    "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayAppointments(List<Appointment> appointments) {
        appointmentsContainer.removeAllViews();
        
        if (appointments.isEmpty()) {
            TextView noAppointmentsText = new TextView(this);
            noAppointmentsText.setText("No appointments found");
            noAppointmentsText.setPadding(32, 32, 32, 32);
            appointmentsContainer.addView(noAppointmentsText);
            return;
        }

        Log.d(TAG, "Displaying " + appointments.size() + " appointments");
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Appointment appointment : appointments) {
            // Log appointment details for debugging
            Log.d(TAG, String.format("Processing appointment - ID: %s, Title: %s", 
                appointment.getIdValue(), appointment.getTitleValue()));

            if (appointment.getIdValue() == null) {
                Log.e(TAG, "Found appointment with null ID - Title: " + appointment.getTitleValue());
                continue;
            }

            CardView appointmentCard = (CardView) inflater.inflate(
                R.layout.appointment_card_template, appointmentsContainer, false);

            // Find all views in the card
            TextView titleText = appointmentCard.findViewById(R.id.titleText);
            TextView descriptionText = appointmentCard.findViewById(R.id.descriptionText);
            TextView timeRemainingText = appointmentCard.findViewById(R.id.timeRemainingText);
            TextView statusText = appointmentCard.findViewById(R.id.statusText);
            Button viewAllButton = appointmentCard.findViewById(R.id.viewAllButton);

            // Set appointment details
            titleText.setText(appointment.getTitleValue());
            
            // Show description or default message
            String description = appointment.getDescriptionValue() != null ? 
                appointment.getDescriptionValue() : "No description available";
            descriptionText.setText(description);
            
            // Show detailed status
            String detailedStatus = "Status: " + appointment.getStatusValue();
            if (appointment.getStartTimeValue() != null) {
                detailedStatus += "\nStart: " + appointment.getFormattedDateTime();
            }
            statusText.setText(detailedStatus);

            // Calculate time remaining
            com.google.firebase.Timestamp startTimeTs = appointment.getStartTimeValue();
            if (startTimeTs != null) {
                long startTimeMillis = startTimeTs.toDate().getTime();
                long nowMillis = System.currentTimeMillis();
                long diffInMillis = startTimeMillis - nowMillis;
                
                if (diffInMillis > 0) {
                    long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis);
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60;
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis) % 60;
                    
                    String timeRemaining = String.format(Locale.US, 
                        "%dh %dm %ds remaining", hours, minutes, seconds);
                    timeRemainingText.setText(timeRemaining);
                } else {
                    timeRemainingText.setText("Appointment time passed");
                }
            } else {
                timeRemainingText.setText("Time not set");
            }

            viewAllButton.setOnClickListener(v -> {
                // Navigate to AppointmentDetailsActivity
                Intent intent = new Intent(HomePage.this, AppointmentDetailsActivity.class);
                intent.putExtra("APPOINTMENT_PARCEL", appointment);
                startActivity(intent);
            });

            appointmentsContainer.addView(appointmentCard);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAppointments(); // Refresh appointments when returning to the page
    }
} 