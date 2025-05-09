package com.it342.projectmanagementsystem.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.models.Notification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.util.Log;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private static final String TAG = "NotificationAdapter";
    private List<Notification> notifications;
    private Context context;
    private OnNotificationClickListener clickListener;
    private NotificationInteractionListener interactionListener;

    // Interface for the OnNotificationClickListener (used by FacultyDashboardActivity)
    public interface OnNotificationClickListener {
        void onViewDetailsClick(Notification notification);
    }

    // Interface for the NotificationInteractionListener (used by NotificationsActivity)
    public interface NotificationInteractionListener {
        void onMarkAsRead(Notification notification, int position);
        void onViewAppointment(Notification notification);
    }

    // Constructor for FacultyDashboardActivity
    public NotificationAdapter(Context context, List<Notification> notifications, OnNotificationClickListener listener) {
        this.context = context;
        this.notifications = notifications;
        this.clickListener = listener;
    }

    // Constructor for NotificationsActivity
    public NotificationAdapter(Context context, NotificationInteractionListener listener) {
        this.context = context;
        this.notifications = new ArrayList<>();
        this.interactionListener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification, position);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void updateData(List<Notification> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }

    // Method used by NotificationsActivity
    public void setNotifications(List<Notification> notifications) {
        Log.d(TAG, "Setting " + (notifications != null ? notifications.size() : 0) + " notifications");
        if (notifications != null && !notifications.isEmpty()) {
            for (int i = 0; i < Math.min(3, notifications.size()); i++) {
                Notification n = notifications.get(i);
                Log.d(TAG, "Notification " + i + ": " + n.getTitle() + 
                      ", Read: " + n.isRead() + 
                      ", Type: " + n.getType() + 
                      ", StudentName: " + n.getStudentName());
            }
        }
        this.notifications = notifications;
        notifyDataSetChanged();
    }
    
    // Getter for notifications
    public List<Notification> getNotifications() {
        return notifications;
    }
    
    // Method used by NotificationsActivity to mark notification as read
    public void markAsRead(int position) {
        if (position >= 0 && position < notifications.size()) {
            Notification notification = notifications.get(position);
            notification.setRead(true);
            notifyItemChanged(position);
        }
    }

    public class NotificationViewHolder extends RecyclerView.ViewHolder {
        private View notificationIndicator;
        private TextView tvNotificationTitle, tvNotificationMessage, tvNotificationDate;
        private Button btnViewDetails;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            notificationIndicator = itemView.findViewById(R.id.notificationIndicator);
            tvNotificationTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvNotificationMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvNotificationDate = itemView.findViewById(R.id.tvNotificationDate);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }

        public void bind(final Notification notification, final int position) {
            // Get the appropriate title based on notification type
            String displayTitle = notification.getTitle();
            String displayMessage = notification.getMessage();
            
            Log.d(TAG, "Binding notification: " + notification.getId() + 
                  ", Type: " + notification.getType() + 
                  ", Title: " + displayTitle);
            
            // Check if we need to use the alternative fields for student appointment requests
            if ("FACULTY_APPOINTMENT_REQUEST".equals(notification.getType()) || 
                (notification.getStudentName() != null && !notification.getStudentName().isEmpty())) {
                // For faculty notifications where a student has requested an appointment
                if (displayMessage == null || displayMessage.isEmpty()) {
                    String studentName = notification.getDisplayName();
                    String reason = notification.getDisplayReason();
                    displayMessage = studentName + " has requested an appointment with you. Reason: " + reason;
                    Log.d(TAG, "Created display message from student info: " + displayMessage);
                }
            }
            
            tvNotificationTitle.setText(displayTitle);
            tvNotificationMessage.setText(displayMessage);
            
            // Set read/unread indicator
            notificationIndicator.setVisibility(notification.isRead() ? View.INVISIBLE : View.VISIBLE);
            
            // For NotificationsActivity
            if (interactionListener != null) {
                // Format time based on createdAt timestamp
                if (notification.getCreatedAt() != null) {
                    long timeInMillis = notification.getCreatedAt().toDate().getTime();
                    String relativeTime = DateUtils.getRelativeTimeSpanString(
                            timeInMillis,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE
                    ).toString();
                    tvNotificationDate.setText(relativeTime);
                } else {
                    tvNotificationDate.setText("Just now");
                }
                
                // Set click listeners for NotificationsActivity
                itemView.setOnClickListener(v -> {
                    if (!notification.isRead()) {
                        interactionListener.onMarkAsRead(notification, position);
                    }
                });
                
                btnViewDetails.setOnClickListener(v -> {
                    if (!notification.isRead()) {
                        interactionListener.onMarkAsRead(notification, position);
                    }
                    interactionListener.onViewAppointment(notification);
                });
            }
            // For FacultyDashboardActivity
            else if (clickListener != null) {
                String dateString = notification.getAppointmentDate() + " at " + notification.getAppointmentTime();
                tvNotificationDate.setText(dateString);
                
                btnViewDetails.setOnClickListener(v -> {
                    if (clickListener != null) {
                        clickListener.onViewDetailsClick(notification);
                    }
                });
            }
        }
    }
} 