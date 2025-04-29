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

        holder.tvTitle.setText(appointment.getTitle());

        String creatorName = appointment.getCreatorName();
        if (creatorName != null && !creatorName.isEmpty()) {
            holder.tvRequesterInfo.setText("Requested by: " + creatorName);
        } else {
            holder.tvRequesterInfo.setText("Requested by: Unknown");
            Log.w(TAG, "Creator name is missing for appointment ID: " + appointment.getId());
        }

        // Check if appointment is in the past (for COMPLETED status)
        boolean isCompletedAppointment = false;
        if (appointment.getStartTime() != null) {
            Date startDate = appointment.getStartTime().toDate();
            long startTimeMillis = startDate.getTime();
            long currentTimeMillis = System.currentTimeMillis();
            
            if (startTimeMillis < currentTimeMillis) {
                // Time has passed for this appointment
                String status = appointment.getStatus();
                if ("SCHEDULED".equalsIgnoreCase(status) || "ACCEPTED".equalsIgnoreCase(status)) {
                    isCompletedAppointment = true;
                    holder.tvTime.setText("Time: " + dateTimeFormat.format(startDate) + " (Completed)");
                } else {
                    holder.tvTime.setText("Time: " + dateTimeFormat.format(startDate) + " (Time has passed)");
                }
            } else {
                holder.tvTime.setText("Time: " + dateTimeFormat.format(startDate));
            }
        } else {
            holder.tvTime.setText("Time: Not set");
        }

        // Format status string and set color based on status
        String status = appointment.getStatus();
        String displayStatus;
        
        if (isCompletedAppointment) {
            displayStatus = "Completed";
        } else {
            displayStatus = formatStatusForDisplay(status);
        }
        
        holder.tvStatus.setText("Status: " + displayStatus);
        
        // Set text color based on status
        int textColorResId;
        if (isCompletedAppointment) {
            textColorResId = R.color.appointment_completed_text;
        } else {
            textColorResId = getTextColorForStatus(status);
        }
        
        holder.tvStatus.setTextColor(context.getResources().getColor(textColorResId));

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
        if (status == null) return "Unknown";
        
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
        if (status == null) return R.color.text_light;
        
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