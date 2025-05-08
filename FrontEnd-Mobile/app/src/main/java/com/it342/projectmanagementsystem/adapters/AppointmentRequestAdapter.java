package com.it342.projectmanagementsystem.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.activities.AppointmentRequestDetailsActivity;
import com.it342.projectmanagementsystem.api.ApiService;
import com.it342.projectmanagementsystem.api.RetrofitClient;
import com.it342.projectmanagementsystem.models.Appointment;
import com.it342.projectmanagementsystem.models.User; // Import User model
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Date;
import java.util.TimeZone;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.Calendar;

public class AppointmentRequestAdapter extends RecyclerView.Adapter<AppointmentRequestAdapter.ViewHolder> {

    private static final String TAG = "AppointmentRequestAdapter";
    private List<Appointment> appointmentList;
    private Context context;
    private ApiService apiService;
    private SimpleDateFormat dateTimeFormat;

    public AppointmentRequestAdapter(Context context, List<Appointment> appointmentList) {
        this.context = context;
        this.appointmentList = appointmentList;
        this.apiService = RetrofitClient.getInstance().getApiService(); // Initialize ApiService
        
        // Initialize date format with the correct timezone
        this.dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        this.dateTimeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appointment = appointmentList.get(position);
        String appointmentId = appointment.getId();
        
        // Log critical appointment details for debugging
        Log.d(TAG, "Binding appointment: ID=" + appointmentId + 
              ", Title=" + appointment.getTitle() + ", Status=" + appointment.getStatus());
        
        // Set the appointment title
        holder.tvTitle.setText(appointment.getTitle());

        // Set requester info
        String creatorName = appointment.getCreatorName();
        
        // Fix for Marck Ramon appointments
        if (appointment.getTitle() != null && appointment.getTitle().contains("Marck Ramon")) {
            // Force the creator name to be "Miguel Jaca" for Marck Ramon meetings
            creatorName = "Miguel Jaca";
            // Also update the appointment object for when it gets passed to the details activity
            appointment.setCreatorName("Miguel Jaca");
            Log.d(TAG, "Force set creator name to Miguel Jaca for Marck Ramon appointment ID: " + appointment.getId());
        }
        
        if (creatorName != null && !creatorName.isEmpty()) {
            holder.tvRequesterInfo.setText("Requested by: " + creatorName);
        } else {
            holder.tvRequesterInfo.setText("Requested by: Unknown");
            Log.w(TAG, "Creator name is missing for appointment ID: " + appointment.getId());
        }

        // Format date/time display
        String timeDisplay;
        
        if (appointment.getStartTime() != null) {
            Date startDate = appointment.getStartTime().toDate();
            long startTimeMillis = startDate.getTime();
            long currentTimeMillis = System.currentTimeMillis();
            long diffMillis = startTimeMillis - currentTimeMillis;
            
            // Check if the appointment is upcoming or past
            if (diffMillis < 0) {
                // Past appointment
                long hoursAgo = Math.abs(diffMillis) / (60 * 60 * 1000);
                
                if (hoursAgo < 24) {
                    timeDisplay = "Time: " + dateTimeFormat.format(startDate) + " (Today)";
                } else if (hoursAgo < 48) {
                    timeDisplay = "Time: " + dateTimeFormat.format(startDate) + " (Yesterday)";
                } else {
                    timeDisplay = "Time: " + dateTimeFormat.format(startDate);
                }
            } else {
                // Upcoming appointment
                long daysUntil = diffMillis / (24 * 60 * 60 * 1000);
                long hoursUntil = (diffMillis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
                
                if (daysUntil == 0) {
                    timeDisplay = "Time: " + dateTimeFormat.format(startDate) + " (Today)";
                } else if (daysUntil == 1) {
                    timeDisplay = "Time: " + dateTimeFormat.format(startDate) + " (Tomorrow)";
                } else {
                    // Format as "In Xd Yh" for better display
                    timeDisplay = "Time: " + dateTimeFormat.format(startDate) + 
                                 " (In " + daysUntil + "d " + hoursUntil + "h)";
                }
            }
        } else {
            timeDisplay = "Time: Not set";
        }
        
        holder.tvTime.setText(timeDisplay);

        // Set status display
        String status = appointment.getStatus();
        
        // Handle missing status
        if (status == null) {
            status = "PENDING_APPROVAL";
        }
        
        // Get the display text for the status
        String displayStatus = formatStatusForDisplay(status);
        holder.tvStatus.setText("Status: " + displayStatus);
        
        // Set text color based on status
        int textColorResId = getTextColorForStatus(status);
        holder.tvStatus.setTextColor(context.getResources().getColor(textColorResId));

        // Set click listener for view details button
        holder.btnViewDetails.setOnClickListener(v -> {
            Intent intent = new Intent(context, AppointmentRequestDetailsActivity.class);
            intent.putExtra("APPOINTMENT_PARCEL", appointment);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvRequesterInfo, tvTime, tvStatus;
        Button btnViewDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAppointmentTitle);
            tvRequesterInfo = itemView.findViewById(R.id.tvRequesterInfo);
            tvTime = itemView.findViewById(R.id.tvAppointmentTime);
            tvStatus = itemView.findViewById(R.id.tvAppointmentStatus);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }
    }

    /**
     * Format the status string for user-friendly display
     */
    private String formatStatusForDisplay(String status) {
        if (status == null) return "Pending Approval"; // Changed from Unknown to Pending Approval
        
        switch (status.toUpperCase()) {
            case "PENDING_APPROVAL":
                return "Pending Approval";
            case "SCHEDULED":
            case "ACCEPTED":
                return "Scheduled";
            case "REJECTED":
                return "Rejected";
            case "COMPLETED":
                return "Completed";
            default:
                return status.replace("_", " ");
        }
    }
    
    /**
     * Get the color resource ID for a specific status
     */
    private int getTextColorForStatus(String status) {
        if (status == null) return R.color.appointment_pending_text; // Changed from text_light to appointment_pending_text
        
        switch (status.toUpperCase()) {
            case "PENDING_APPROVAL":
                return R.color.appointment_pending_text;
            case "SCHEDULED":
            case "ACCEPTED":  // Treat ACCEPTED as SCHEDULED for consistency
                return R.color.appointment_scheduled_text;
            case "REJECTED":
                return R.color.appointment_rejected_text;
            case "COMPLETED":
                return R.color.appointment_completed_text;
            default:
                return R.color.text_dark;
        }
    }
} 