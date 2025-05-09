package com.it342.projectmanagementsystem.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.models.ParticipantRequest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ParticipantRequestAdapter extends RecyclerView.Adapter<ParticipantRequestAdapter.ParticipantRequestViewHolder> {

    private final Context context;
    private List<ParticipantRequest> participantRequests;
    private final ParticipantRequestListener listener;

    public interface ParticipantRequestListener {
        void onAcceptClicked(ParticipantRequest request, int position);
        void onRejectClicked(ParticipantRequest request, int position);
    }

    public ParticipantRequestAdapter(Context context, ParticipantRequestListener listener) {
        this.context = context;
        this.participantRequests = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ParticipantRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_participant_request, parent, false);
        return new ParticipantRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantRequestViewHolder holder, int position) {
        ParticipantRequest request = participantRequests.get(position);
        holder.bind(request, position);
    }

    @Override
    public int getItemCount() {
        return participantRequests.size();
    }

    public void setParticipantRequests(List<ParticipantRequest> requests) {
        this.participantRequests = requests;
        notifyDataSetChanged();
    }

    public List<ParticipantRequest> getParticipantRequests() {
        return participantRequests;
    }

    public void updateRequestStatus(int position, String newStatus) {
        if (position >= 0 && position < participantRequests.size()) {
            ParticipantRequest request = participantRequests.get(position);
            request.setStatus(newStatus);
            notifyItemChanged(position);
        }
    }

    public class ParticipantRequestViewHolder extends RecyclerView.ViewHolder {
        private final View statusIndicator;
        private final TextView tvStudentName;
        private final TextView tvStatus;
        private final TextView tvRequestDate;
        private final Button btnAccept;
        private final Button btnReject;
        private final View actionButtons;

        public ParticipantRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvRequestDate = itemView.findViewById(R.id.tvRequestDate);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            actionButtons = itemView.findViewById(R.id.actionButtons);
        }

        public void bind(final ParticipantRequest request, final int position) {
            // Set student name
            tvStudentName.setText(request.getStudentName());
            
            // Format request date
            String formattedDate = "Requested: Unknown date";
            if (request.getRequestTime() != null) {
                Date date = request.getRequestTime().toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
                formattedDate = "Requested: " + sdf.format(date);
            }
            
            tvRequestDate.setText(formattedDate);
            
            // Set status and styling based on status
            String status = request.getStatus();
            if (status == null) {
                status = "PENDING"; // Default status
            }
            
            tvStatus.setText(formatStatus(status));
            
            // Update colors and visibility based on status
            int statusColor;
            int indicatorColor;
            
            switch (status.toUpperCase()) {
                case "APPROVED":
                    statusColor = Color.parseColor("#4CAF50"); // Green
                    indicatorColor = Color.parseColor("#4CAF50");
                    actionButtons.setVisibility(View.GONE);
                    break;
                case "REJECTED":
                    statusColor = Color.parseColor("#F44336"); // Red
                    indicatorColor = Color.parseColor("#F44336");
                    actionButtons.setVisibility(View.GONE);
                    break;
                case "PENDING_CONFIRMATION":
                    statusColor = Color.parseColor("#2196F3"); // Blue
                    indicatorColor = Color.parseColor("#2196F3");
                    actionButtons.setVisibility(View.VISIBLE);
                    // Update button text for confirmation
                    btnAccept.setText("Confirm");
                    btnReject.setText("Remove");
                    break;
                case "PENDING":
                default:
                    statusColor = Color.parseColor("#FF9800"); // Orange
                    indicatorColor = Color.parseColor("#FF9800");
                    actionButtons.setVisibility(View.VISIBLE);
                    // Ensure button text is set to default
                    btnAccept.setText("Accept");
                    btnReject.setText("Reject");
                    break;
            }
            
            tvStatus.setTextColor(statusColor);
            statusIndicator.setBackgroundColor(indicatorColor);
            
            // Set button click listeners
            btnAccept.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAcceptClicked(request, position);
                }
            });
            
            btnReject.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRejectClicked(request, position);
                }
            });
        }
        
        private String formatStatus(String status) {
            switch (status.toUpperCase()) {
                case "APPROVED":
                    return "Approved";
                case "REJECTED":
                    return "Rejected";
                case "PENDING_CONFIRMATION":
                    return "Needs Confirmation";
                case "PENDING":
                default:
                    return "Pending";
            }
        }
    }
} 