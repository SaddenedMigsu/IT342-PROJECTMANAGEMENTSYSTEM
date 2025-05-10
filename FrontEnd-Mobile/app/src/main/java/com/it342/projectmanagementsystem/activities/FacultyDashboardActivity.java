package com.it342.projectmanagementsystem.activities;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.it342.projectmanagementsystem.PMSApplication;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.adapters.NotificationAdapter;
import com.it342.projectmanagementsystem.models.Notification;
import com.it342.projectmanagementsystem.utils.Constants;
import com.it342.projectmanagementsystem.utils.LocalNotificationService;

import java.util.ArrayList;
import java.util.List;

public class FacultyDashboardActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {
    
    private static final String TAG = "FacultyDashboard";
    
    private TextView tvFacultyWelcome;
    private Button btnManageRequests, btnViewSchedule, btnManageParticipants;
    private MaterialButton btnLogout;
    private ImageButton btnNotifications;
    private TextView tvNotificationBadge;
    
    private FirebaseFirestore db;
    private String userId;
    private List<Notification> notificationsList = new ArrayList<>();
    private int unreadCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_dashboard);
        
        db = FirebaseFirestore.getInstance();
        
        initializeViews();
        setupButtonClickListeners();
        setWelcomeMessage();
        
        // Request notification permissions for Android 13+
        PMSApplication.requestNotificationPermission(this);
        
        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getString(Constants.KEY_USER_ID, "");
        
        if (!userId.isEmpty()) {
            fetchNotifications();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (!userId.isEmpty()) {
            fetchNotifications();
        }
    }

    private void initializeViews() {
        tvFacultyWelcome = findViewById(R.id.tvFacultyWelcome);
        btnManageRequests = findViewById(R.id.btnManageRequests);
        btnViewSchedule = findViewById(R.id.btnViewSchedule);
        btnManageParticipants = findViewById(R.id.btnManageParticipants);
        btnLogout = findViewById(R.id.btnLogout);
        btnNotifications = findViewById(R.id.btnNotifications);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
    }

    private void setWelcomeMessage() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String firstName = prefs.getString(Constants.KEY_FIRST_NAME, "");
        String lastName = prefs.getString(Constants.KEY_LAST_NAME, "");
        
        if (!firstName.isEmpty() && !lastName.isEmpty()) {
            tvFacultyWelcome.setText("Welcome, " + firstName + " " + lastName + "!");
        }
    }

    private void setupButtonClickListeners() {
        btnManageRequests.setOnClickListener(v -> 
            startActivity(new Intent(this, ManageAppointmentRequestsActivity.class)));

        btnViewSchedule.setOnClickListener(v -> 
            startActivity(new Intent(this, FacultyAppointmentScheduleActivity.class)));

        btnManageParticipants.setOnClickListener(v -> 
            startActivity(new Intent(this, ManageAppointmentParticipantsActivity.class)));
            
        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        
        btnNotifications.setOnClickListener(v -> showNotificationsDialog());
        
        // Add long press listener for creating test notifications
        btnNotifications.setOnLongClickListener(v -> {
            createTestNotification();
            return true;
        });
    }
    
    private void fetchNotifications() {
        // Fetch notifications for the current faculty
        db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .orderBy("id", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                notificationsList.clear();
                unreadCount = 0;
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Notification notification = document.toObject(Notification.class);
                    notification.setId(document.getId());
                    notificationsList.add(notification);
                    
                    if (!notification.isRead()) {
                        unreadCount++;
                    }
                }
                
                updateNotificationBadge();
                Log.d(TAG, "Fetched " + notificationsList.size() + " notifications, " + unreadCount + " unread");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching notifications", e);
            });
    }
    
    private void updateNotificationBadge() {
        if (unreadCount > 0) {
            tvNotificationBadge.setVisibility(View.VISIBLE);
            tvNotificationBadge.setText(String.valueOf(unreadCount));
        } else {
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }
    
    private void showNotificationsDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_notifications);
        
        TextView tvEmptyNotifications = dialog.findViewById(R.id.tvEmptyNotifications);
        RecyclerView rvNotifications = dialog.findViewById(R.id.rvNotifications);
        
        if (notificationsList.isEmpty()) {
            rvNotifications.setVisibility(View.GONE);
            tvEmptyNotifications.setVisibility(View.VISIBLE);
        } else {
            rvNotifications.setVisibility(View.VISIBLE);
            tvEmptyNotifications.setVisibility(View.GONE);
            
            // Set up the RecyclerView
            rvNotifications.setLayoutManager(new LinearLayoutManager(this));
            NotificationAdapter adapter = new NotificationAdapter(this, notificationsList, this);
            rvNotifications.setAdapter(adapter);
            
            // Mark all notifications as read
            markAllNotificationsAsRead();
        }
        
        dialog.show();
    }
    
    private void markAllNotificationsAsRead() {
        for (Notification notification : notificationsList) {
            if (!notification.isRead()) {
                notification.setRead(true);
                
                // Update in Firestore
                db.collection("notifications")
                    .document(notification.getId())
                    .update("read", true)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification marked as read: " + notification.getId()))
                    .addOnFailureListener(e -> Log.e(TAG, "Error marking notification as read", e));
            }
        }
        
        // Clear the badge count
        unreadCount = 0;
        updateNotificationBadge();
    }
    
    @Override
    public void onViewDetailsClick(Notification notification) {
        // Open appointment details
        Intent intent = new Intent(this, AppointmentDetailsActivity.class);
        intent.putExtra("appointmentId", notification.getAppointmentId());
        startActivity(intent);
    }
    
    private void showLogoutConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Logout Confirmation");
        builder.setMessage("Do you want to log out?");
        
        // Add "Yes" button
        builder.setPositiveButton("Yes", (dialog, which) -> {
            logout();
        });
        
        // Add "No" button
        builder.setNegativeButton("No", (dialog, which) -> {
            dialog.dismiss();
        });
        
        // Create and show the dialog
        builder.create().show();
    }
    
    private void logout() {
        // Clear user session data
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        
        // Redirect to login form screen
        Intent intent = new Intent(this, LoginFormActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    /**
     * Create a test notification for debugging purposes
     */
    private void createTestNotification() {
        // Create a test notification
        LocalNotificationService.createTestNotification(this);
        
        // Show toast message
        Toast.makeText(this, "Test notification created", Toast.LENGTH_SHORT).show();
        
        // Refresh notifications list
        fetchNotifications();
    }
} 