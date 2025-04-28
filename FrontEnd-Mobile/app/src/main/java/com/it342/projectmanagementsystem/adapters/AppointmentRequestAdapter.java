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

        if (appointment.getStartTime() != null) {
            Date startDate = appointment.getStartTime().toDate();
            holder.tvTime.setText("Time: " + dateTimeFormat.format(startDate));
        } else {
            holder.tvTime.setText("Time: Not set");
        }

        holder.tvStatus.setText("Status: " + appointment.getStatus());

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
} 