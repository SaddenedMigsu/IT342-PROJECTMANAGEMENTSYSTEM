package com.it342.projectmanagementsystem.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.activities.FacultyAppointmentScheduleActivity;
import com.it342.projectmanagementsystem.models.Appointment;
import com.it342.projectmanagementsystem.models.Tag;
import com.it342.projectmanagementsystem.utils.CountdownTimerManager;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Date;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    private List<Appointment> appointments;
    private final Context context;
    private final OnAppointmentClickListener listener;
    private static final String TAG = "AppointmentAdapter";
    private CountdownTimerManager timerManager;

    public interface OnAppointmentClickListener {
        void onAppointmentClick(Appointment appointment);
        void displayTags(Appointment appointment, LinearLayout tagsContainer);
    }

    public AppointmentAdapter(Context context, OnAppointmentClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.appointments = new ArrayList<>();
        this.timerManager = new CountdownTimerManager();
    }

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);
        
        // Set appointment details
        holder.tvAppointmentTitle.setText(appointment.getTitle());
        
        // Use the description which now contains the reason as well
        String description = appointment.getDescription();
        holder.tvAppointmentDescription.setText(description != null ? description : "No description available");
        
        // Hide location view - check if it exists first
        if (holder.tvAppointmentLocation != null) {
            holder.tvAppointmentLocation.setVisibility(View.GONE);
        }
        
        // Display tags if available - use the activity's displayTags method
        if (listener != null) {
            listener.displayTags(appointment, holder.tagsContainer);
        }
        
        // Check if appointment is already marked as completed or time has passed
        boolean isCompletedAppointment = false;
        String status = appointment.getStatus();
        
        // Check if appointment is explicitly marked as completed
        if ("COMPLETED".equalsIgnoreCase(status)) {
            isCompletedAppointment = true;
            holder.tvTimeRemaining.setText("Appointment Finished");
            holder.tvTimeRemaining.setVisibility(View.VISIBLE);
        }
        // Check if appointment time has passed
        else if (appointment.getStartTime() != null) {
            try {
                long startTimeMillis = appointment.getStartTime().toDate().getTime();
                long currentTimeMillis = System.currentTimeMillis();
                
                // Visual status update for completed appointments
                if (startTimeMillis <= currentTimeMillis) {
                    // Appointment time has passed, mark as completed if it was scheduled/accepted
                    if ("SCHEDULED".equalsIgnoreCase(status) || 
                        "ACCEPTED".equalsIgnoreCase(status)) {
                        // This is a visual update only - we don't modify the actual appointment object
                        isCompletedAppointment = true;
                    }
                    holder.tvTimeRemaining.setText("Appointment Finished");
                    holder.tvTimeRemaining.setVisibility(View.VISIBLE);
                } else {
                    // Only start timer for non-completed appointments
                    // Start or update the countdown timer for this appointment
                    String appointmentId = appointment.getId();
                    if (appointmentId != null && !appointmentId.isEmpty()) {
                        // Set an initial loading text
                        holder.tvTimeRemaining.setText("Calculating time remaining...");
                        
                        // Start the dynamic countdown timer
                        timerManager.startTimer(
                            appointmentId, 
                            startTimeMillis, 
                            holder.tvTimeRemaining,
                            // Callback when timer finishes
                            () -> {
                                // Update UI when timer finishes
                                if ("SCHEDULED".equalsIgnoreCase(appointment.getStatus()) || 
                                    "ACCEPTED".equalsIgnoreCase(appointment.getStatus())) {
                                    // Visual update for the status
                                    holder.tvStatus.setText("Status: Completed");
                                    holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.appointment_completed_text));
                                    holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.appointment_completed_bg));
                                    // Update time remaining text
                                    holder.tvTimeRemaining.setText("Appointment Finished");
                                }
                            }
                        );
                    } else {
                        // Fallback for appointments without IDs
                        long timeRemainingMillis = startTimeMillis - currentTimeMillis;
                        long hours = timeRemainingMillis / (60 * 60 * 1000);
                        timeRemainingMillis %= (60 * 60 * 1000);
                        long minutes = timeRemainingMillis / (60 * 1000);
                        timeRemainingMillis %= (60 * 1000);
                        long seconds = timeRemainingMillis / 1000;
                        
                        String timeRemainingText = String.format(Locale.getDefault(), 
                            "%dh %dm %ds remaining", hours, minutes, seconds);
                        holder.tvTimeRemaining.setText(timeRemainingText);
                    }
                    holder.tvTimeRemaining.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calculating time remaining", e);
                holder.tvTimeRemaining.setText("Time not available");
                holder.tvTimeRemaining.setVisibility(View.VISIBLE);
            }
        } else {
            holder.tvTimeRemaining.setVisibility(View.GONE);
        }
        
        // Get the status to display (either the original or COMPLETED if time passed)
        String displayStatus;
        
        if (isCompletedAppointment) {
            displayStatus = "Completed";
        } else {
            displayStatus = formatStatusForDisplay(status);
        }
        
        holder.tvStatus.setText("Status: " + displayStatus);
        
        // Date and time display using the formatting methods
        String appointmentDate = "Start: " + appointment.getDisplayDate() + " " + appointment.getDisplayTime();
        holder.tvDateTime.setText(appointmentDate);
        
        // Set color based on status
        int statusColor;
        int backgroundColorResId;
        
        if (isCompletedAppointment) {
            // Use completed color scheme (green)
            statusColor = R.color.appointment_completed_text;
            backgroundColorResId = R.color.appointment_completed_bg;
        } else {
            // Use regular status colors
            switch (status != null ? status.toUpperCase() : "") {
                case "ACCEPTED":
                case "SCHEDULED":
                    statusColor = R.color.appointment_scheduled_text;
                    backgroundColorResId = R.color.appointment_scheduled_bg;
                    break;
                case "REJECTED":
                    statusColor = R.color.appointment_rejected_text;
                    backgroundColorResId = R.color.appointment_rejected_bg;
                    break;
                case "PENDING_APPROVAL":
                    statusColor = R.color.appointment_pending_text;
                    backgroundColorResId = R.color.appointment_pending_bg;
                    break;
                default:
                    statusColor = R.color.appointment_default_text;
                    backgroundColorResId = R.color.appointment_default_bg;
                    break;
            }
        }
        
        // Apply background color to the card
        holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, backgroundColorResId));
        
        // Apply text color to status
        holder.tvStatus.setTextColor(ContextCompat.getColor(context, statusColor));
        
        // Set click listener for the card
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAppointmentClick(appointment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    // Clean up method to be called when adapter is no longer in use
    public void onDestroy() {
        if (timerManager != null) {
            timerManager.stopAllTimers();
        }
    }

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView tvAppointmentTitle;
        TextView tvAppointmentDescription;
        TextView tvDateTime;
        TextView tvStatus;
        TextView tvTimeRemaining;
        TextView tvAppointmentLocation;
        LinearLayout tagsContainer;
        CardView cardView;

        AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAppointmentTitle = itemView.findViewById(R.id.tvAppointmentTitle);
            tvAppointmentDescription = itemView.findViewById(R.id.tvAppointmentDescription);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvTimeRemaining = itemView.findViewById(R.id.tvTimeRemaining);
            tvAppointmentLocation = itemView.findViewById(R.id.tvAppointmentLocation);
            tagsContainer = itemView.findViewById(R.id.llTagsContainer);
            cardView = itemView.findViewById(R.id.cardAppointment);
        }
    }

    // Helper method to format status text for display
    private String formatStatusForDisplay(String status) {
        if (status == null) return "Unknown";
        
        switch (status.toUpperCase()) {
            case "ACCEPTED":
            case "SCHEDULED":
                return "Scheduled";
            case "REJECTED":
                return "Rejected";
            case "PENDING_APPROVAL":
                return "Pending Approval";
            case "COMPLETED":
                return "Completed";
            default:
                return status.replace("_", " ");
        }
    }
} 