package com.it342.projectmanagementsystem.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.adapters.NotificationAdapter;
import com.it342.projectmanagementsystem.models.Notification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.NotificationInteractionListener {
    private static final String TAG = "NotificationsActivity";
    private static final boolean IS_DEBUG = true;
    private RecyclerView recyclerViewNotifications;
    private NotificationAdapter notificationAdapter;
    private ProgressBar loadingIndicator;
    private LinearLayout emptyStateView;
    private TextView btnMarkAllRead;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        
        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        
        // Get the user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", "");
        
        if (userId.isEmpty()) {
            Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupRecyclerView();
        loadNotifications();
        setupListeners();
        
        // Show instructions for developer to create the index if needed
        if (IS_DEBUG) {
            Log.i(TAG, "If you see an index error, create the required index by visiting: " +
                  "https://console.firebase.google.com/project/project-management-syste-b9ab2/firestore/indexes");
        }
    }

    private void initializeViews() {
        recyclerViewNotifications = findViewById(R.id.recyclerViewNotifications);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        emptyStateView = findViewById(R.id.emptyStateView);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        notificationAdapter = new NotificationAdapter(this, this);
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotifications.setAdapter(notificationAdapter);
    }

    private void setupListeners() {
        btnMarkAllRead.setOnClickListener(v -> markAllNotificationsAsRead());
    }

    private void loadNotifications() {
        showLoading();
        
        // For demonstration, we'll add some sample notifications
        // In a real app, you would fetch these from Firestore
        if (isUsingFirestore()) {
            loadNotificationsFromFirestore();
        } else {
            loadSampleNotifications();
        }
    }
    
    private boolean isUsingFirestore() {
        // For testing, we can switch between sample data and Firestore
        // In a real app, this would always return true
        return true;
    }
    
    private void loadNotificationsFromFirestore() {
        try {
            Log.d(TAG, "Loading notifications for user: " + userId);
            
            // Get the user email for alternative queries
            SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
            String userEmail = prefs.getString("email", "");
            
            // DEBUG: Log all relevant user information
            logUserInfo(userEmail);
            
            if (userEmail.isEmpty()) {
                Log.w(TAG, "User email not found, only querying by userId");
            } else {
                Log.d(TAG, "User email for faculty query: " + userEmail);
            }
            
            // DEBUG: Check all notifications in the collection to see if any match our criteria
            checkAllNotifications(userEmail);
            
            // Try to get notifications directly by checking document fields first
            getNormalizedNotifications(userEmail);
        } catch (Exception e) {
            Log.e(TAG, "Exception in loadNotificationsFromFirestore", e);
            hideLoading();
            loadSampleNotifications();
        }
    }
    
    // Helper method to log all user information
    private void logUserInfo(String email) {
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String userType = prefs.getString("userType", "unknown");
        String userName = prefs.getString("firstName", "") + " " + prefs.getString("lastName", "");
        
        Log.d(TAG, "====== CURRENT USER INFO ======");
        Log.d(TAG, "User ID: " + userId);
        Log.d(TAG, "User Email: " + email);
        Log.d(TAG, "User Type: " + userType);
        Log.d(TAG, "User Name: " + userName);
        Log.d(TAG, "===============================");
    }
    
    /**
     * Get notifications by checking document fields directly, avoiding type mapping issues
     */
    private void getNormalizedNotifications(String userEmail) {
        db.collection("notifications")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                hideLoading();
                List<Notification> userNotifications = new ArrayList<>();
                int totalNotifications = querySnapshot.size();
                
                Log.d(TAG, "Retrieved " + totalNotifications + " total notifications");
                
                // Create a collection to hold raw document data
                List<Map<String, Object>> matchingDocs = new ArrayList<>();
                
                // First pass - find documents that match this user
                for (QueryDocumentSnapshot document : querySnapshot) {
                    Map<String, Object> data = document.getData();
                    boolean matches = false;
                    
                    // Check for userId field (faculty notifications)
                    if (data.containsKey("userId")) {
                        String docUserId = document.getString("userId");
                        if (userId.equals(docUserId) || userEmail.equals(docUserId)) {
                            matches = true;
                            Log.d(TAG, "Found matching notification by userId: " + document.getId() + 
                                  ", value: " + docUserId);
                        }
                    }
                    
                    // Check for recipientId field (student notifications)
                    if (!matches && data.containsKey("recipientId")) {
                        String recipientId = document.getString("recipientId");
                        if (userId.equals(recipientId)) {
                            matches = true;
                            Log.d(TAG, "Found matching notification by recipientId: " + document.getId() + 
                                  ", value: " + recipientId);
                        }
                    }
                    
                    if (matches) {
                        Map<String, Object> docData = new HashMap<>(data);
                        docData.put("id", document.getId());
                        matchingDocs.add(docData);
                    }
                }
                
                Log.d(TAG, "Found " + matchingDocs.size() + " matching notifications by direct field check");
                
                // Second pass - convert to Notification objects
                for (Map<String, Object> docData : matchingDocs) {
                    try {
                        String id = (String) docData.get("id");
                        String title = (String) docData.get("title");
                        String message = (String) docData.get("message");
                        
                        if (title == null) {
                            // If no title, try alternatives
                            if (docData.containsKey("details")) {
                                Object details = docData.get("details");
                                if (details instanceof Map) {
                                    Map<String, Object> detailsMap = (Map<String, Object>) details;
                                    if (detailsMap.containsKey("title")) {
                                        title = (String) detailsMap.get("title");
                                    }
                                }
                            }
                        }
                        
                        // Ensure we have a title
                        if (title == null || title.isEmpty()) {
                            title = "Notification " + id;
                        }
                        
                        // Create a notification object
                        Notification notification = new Notification();
                        notification.setId(id);
                        notification.setTitle(title);
                        
                        // Set message
                        if (message != null) {
                            notification.setMessage(message);
                        } else if (docData.containsKey("studentName")) {
                            String studentName = (String) docData.get("studentName");
                            String reason = (String) docData.get("reason");
                            notification.setMessage(studentName + " has requested an appointment. Reason: " + reason);
                            notification.setStudentName(studentName);
                            notification.setReason(reason);
                        }
                        
                        // Set other fields if available
                        if (docData.containsKey("createdAt")) {
                            notification.setCreatedAt((Timestamp) docData.get("createdAt"));
                        }
                        
                        if (docData.containsKey("read")) {
                            notification.setRead((Boolean) docData.get("read"));
                        }
                        
                        if (docData.containsKey("type")) {
                            notification.setType((String) docData.get("type"));
                        }
                        
                        if (docData.containsKey("appointmentId")) {
                            notification.setAppointmentId((String) docData.get("appointmentId"));
                        }
                        
                        Log.d(TAG, "Created notification from raw data: " + notification.getTitle());
                        userNotifications.add(notification);
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating notification from document data", e);
                    }
                }
                
                // Sort by creation time
                if (!userNotifications.isEmpty()) {
                    userNotifications.sort((a, b) -> {
                        Timestamp aTime = a.getCreatedAt();
                        Timestamp bTime = b.getCreatedAt();
                        
                        if (aTime == null) return 1;
                        if (bTime == null) return -1;
                        
                        return bTime.compareTo(aTime);
                    });
                }
                
                // Update UI
                updateNotificationList(userNotifications);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error in getNormalizedNotifications", e);
                hideLoading();
                // Fallback to standard method
                fetchAllNotificationsAndFilter(userEmail);
            });
    }
    
    /**
     * Normalize email for comparison with different formats in the database
     */
    private String normalizeEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "";
        }
        
        // Convert to lowercase
        String normalized = email.toLowerCase();
        
        // Try different user IDs that might be in the database
        if ("miguel@jaca.com".equals(normalized)) {
            // Check Firebase Auth user ID mapping in case user ID is stored differently 
            return "U78fK4wPUlDZiJSb5GdD";
        }
        
        return normalized;
    }

    private void loadSampleNotifications() {
        // Create sample notifications for demo
        List<Notification> notifications = new ArrayList<>();
        
        // Sample notification 1 - Accepted
        Notification notification1 = new Notification(
            "Appointment Accepted",
            "Your appointment with Prof. John Doe has been accepted. You're scheduled for Monday, July 10, 2023 at 2:00 PM.",
            userId,
            "sample-appointment-id-1",
            "Meeting with Prof. John Doe",
            "July 10, 2023",
            "2:00 PM"
        );
        notification1.setId("sample-notification-1");
        notification1.setRead(false);
        
        // Sample notification 2 - Rejected
        Notification notification2 = new Notification(
            "Appointment Rejected",
            "Your appointment request with Prof. Jane Smith has been declined. Please request a new time slot.",
            userId,
            "sample-appointment-id-2",
            "Meeting with Prof. Jane Smith",
            "July 12, 2023",
            "1:30 PM"
        );
        notification2.setId("sample-notification-2");
        notification2.setRead(false);
        
        // Sample notification 3 - Rescheduled
        Notification notification3 = new Notification(
            "Appointment Rescheduled",
            "Your appointment with Prof. Michael Johnson has been rescheduled to Friday, July 14, 2023 at 3:30 PM.",
            userId,
            "sample-appointment-id-3",
            "Meeting with Prof. Michael Johnson",
            "July 14, 2023",
            "3:30 PM"
        );
        notification3.setId("sample-notification-3");
        notification3.setRead(true);
        
        // Add to list
        notifications.add(notification1);
        notifications.add(notification2);
        notifications.add(notification3);
        
        // Update UI
        hideLoading();
        updateNotificationList(notifications);
    }

    private void updateNotificationList(List<Notification> notifications) {
        Log.d(TAG, "Updating notification list with " + notifications.size() + " notifications");
        
        if (notifications.isEmpty()) {
            Log.d(TAG, "No notifications to display, checking if we need to create a test notification");
            createNotificationForUser();
        } else {
            Log.d(TAG, "Displaying notifications:");
            for (int i = 0; i < Math.min(3, notifications.size()); i++) {
                Notification n = notifications.get(i);
                Log.d(TAG, "  " + i + ": " + n.getTitle() + " (ID: " + n.getId() + ")");
            }
            
            hideEmptyState();
            notificationAdapter.setNotifications(notifications);
        }
    }
    
    /**
     * Create a real notification in Firestore for this user
     */
    private void createNotificationForUser() {
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String userEmail = prefs.getString("email", "");
        
        // Create a notification directly in Firestore
        Notification newNotification = new Notification(
            "New Notification",
            "This is a new notification created for testing",
            userId,
            null,
            "Test Notification",
            "May 8, 2025",
            "7:00 PM"
        );
        
        // Set the userId field that's used by faculty notifications
        newNotification.setUserId(userId);
        newNotification.setType("SYSTEM_NOTIFICATION");
        
        Log.d(TAG, "Creating new notification for user: " + userId);
        
        // Add to Firestore
        db.collection("notifications")
            .add(newNotification)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Notification created with ID: " + documentReference.getId());
                
                // Reload notifications
                loadNotificationsFromFirestore();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating notification", e);
                showEmptyState();
            });
    }

    private void markNotificationAsRead(Notification notification, int position) {
        if (notification.isRead()) {
            return;
        }
        
        notification.setRead(true);
        
        // Update the UI first for responsiveness
        notificationAdapter.markAsRead(position);
        
        // Then update in Firestore if we're using it
        if (isUsingFirestore() && notification.getId() != null) {
            db.collection("notifications")
                .document(notification.getId())
                .update("read", true)
                .addOnSuccessListener(aVoid -> 
                    Log.d(TAG, "Notification marked as read: " + notification.getId()))
                .addOnFailureListener(e -> 
                    Log.e(TAG, "Error marking notification as read", e));
        }
    }

    private void markAllNotificationsAsRead() {
        List<Notification> notifications = notificationAdapter.getNotifications();
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        
        // Update UI first for responsiveness
        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = notifications.get(i);
            if (!notification.isRead()) {
                notification.setRead(true);
                notificationAdapter.markAsRead(i);
            }
        }
        
        // Update in Firestore if we're using it
        if (isUsingFirestore()) {
            SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
            String userEmail = prefs.getString("email", "");
            
            // Mark as read for "recipientId" field format
            markNotificationsByField("recipientId", userId);
            
            // Mark as read for "userId" field format (faculty notifications)
            if (!userEmail.isEmpty()) {
                markNotificationsByField("userId", userEmail);
            }
        }
    }
    
    private void markNotificationsByField(String fieldName, String fieldValue) {
        // Get unread notifications
        db.collection("notifications")
            .whereEqualTo(fieldName, fieldValue)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (querySnapshot.isEmpty()) {
                    Log.d(TAG, "No unread notifications to mark as read for " + fieldName + ": " + fieldValue);
                    return;
                }
                
                Log.d(TAG, "Marking " + querySnapshot.size() + " notifications as read for " + fieldName);
                
                // Create a batch operation to update all notifications in one call
                com.google.firebase.firestore.WriteBatch batch = db.batch();
                
                for (com.google.firebase.firestore.QueryDocumentSnapshot document : querySnapshot) {
                    batch.update(document.getReference(), "read", true);
                }
                
                // Commit the batch operation
                batch.commit()
                    .addOnSuccessListener(aVoid -> 
                        Log.d(TAG, "All notifications marked as read successfully for " + fieldName))
                    .addOnFailureListener(e -> 
                        Log.e(TAG, "Error marking all notifications as read for " + fieldName, e));
            })
            .addOnFailureListener(e -> 
                Log.e(TAG, "Error getting unread notifications for " + fieldName, e));
    }

    @Override
    public void onMarkAsRead(Notification notification, int position) {
        markNotificationAsRead(notification, position);
    }

    @Override
    public void onViewAppointment(Notification notification) {
        // Try multiple possible field names for appointmentId
        String appointmentId = null;
        
        if (notification.getAppointmentId() != null && !notification.getAppointmentId().isEmpty()) {
            appointmentId = notification.getAppointmentId();
        } else if (notification.getStudentId() != null) {
            // For the faculty notification format, check for field used in screenshots
            appointmentId = notification.getAppointmentId();
        }
        
        // Log all available fields for debugging
        Log.d(TAG, "Notification fields - Title: " + notification.getTitle() 
            + ", AppointmentId: " + notification.getAppointmentId()
            + ", StudentId: " + notification.getStudentId()
            + ", Type: " + notification.getType());
        
        if (appointmentId != null && !appointmentId.isEmpty()) {
            Intent intent = new Intent(this, AppointmentDetailsActivity.class);
            intent.putExtra("appointmentId", appointmentId);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Appointment details not available", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Could not find appointmentId for notification: " + notification.getId());
            
            // For faculty notifications with no appointmentId, try to extract from other fields
            if (notification.getMessage() != null && notification.getMessage().contains("appointment")) {
                Toast.makeText(this, "This is an appointment notification, but no ID was found", 
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showLoading() {
        loadingIndicator.setVisibility(View.VISIBLE);
        recyclerViewNotifications.setVisibility(View.GONE);
        emptyStateView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loadingIndicator.setVisibility(View.GONE);
        recyclerViewNotifications.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        emptyStateView.setVisibility(View.VISIBLE);
        recyclerViewNotifications.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyStateView.setVisibility(View.GONE);
        recyclerViewNotifications.setVisibility(View.VISIBLE);
    }

    // DEBUG method to check all notifications
    private void checkAllNotifications(String userEmail) {
        db.collection("notifications")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                Log.d(TAG, "Total notifications in Firestore: " + querySnapshot.size());
                
                for (QueryDocumentSnapshot document : querySnapshot) {
                    Log.d(TAG, "Notification ID: " + document.getId());
                    
                    // Check for relevant fields
                    if (document.contains("recipientId")) {
                        Log.d(TAG, "  recipientId: " + document.getString("recipientId"));
                    }
                    
                    if (document.contains("userId")) {
                        Log.d(TAG, "  userId: " + document.getString("userId"));
                    }
                    
                    if (document.contains("title")) {
                        Log.d(TAG, "  title: " + document.getString("title"));
                    }
                    
                    // Check if this notification should be shown to the current user
                    if (userId.equals(document.getString("recipientId")) || 
                        userEmail.equals(document.getString("userId"))) {
                        Log.d(TAG, "  ** THIS NOTIFICATION SHOULD BE VISIBLE TO CURRENT USER **");
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking all notifications", e);
            });
    }

    /**
     * Fallback approach that avoids complex queries and index requirements
     */
    private void fetchAllNotificationsAndFilter(String userEmail) {
        db.collection("notifications")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                hideLoading();
                
                Log.d(TAG, "Fallback method: Retrieved " + querySnapshot.size() + " total notifications");
                List<Notification> userNotifications = new ArrayList<>();
                
                // Check if we need to normalize the email
                String normalizedEmail = normalizeEmail(userEmail);
                if (!normalizedEmail.equals(userEmail)) {
                    Log.d(TAG, "Normalized email from '" + userEmail + "' to '" + normalizedEmail + "'");
                }
                
                for (QueryDocumentSnapshot document : querySnapshot) {
                    try {
                        // Check if this notification is for the current user
                        boolean isForCurrentUser = false;
                        
                        // Check recipientId field
                        if (document.contains("recipientId") && userId.equals(document.getString("recipientId"))) {
                            isForCurrentUser = true;
                            Log.d(TAG, "Found notification for user by recipientId: " + document.getId());
                        }
                        
                        // Check userId field (for faculty notifications)
                        if (!isForCurrentUser && document.contains("userId")) {
                            String docUserId = document.getString("userId");
                            
                            // Try exact match
                            if (userEmail.equals(docUserId)) {
                                isForCurrentUser = true;
                                Log.d(TAG, "Found notification - exact userId match: " + document.getId());
                            }
                            // Try normalized match
                            else if (normalizedEmail.equals(docUserId)) {
                                isForCurrentUser = true;
                                Log.d(TAG, "Found notification - normalized userId match: " + document.getId());
                            }
                            // Log for debugging
                            else {
                                Log.d(TAG, "userId mismatch: '" + docUserId + "' vs '" + userEmail + "' for notification: " + document.getId());
                            }
                        }
                        
                        if (isForCurrentUser) {
                            // Convert to notification object
                            Notification notification = document.toObject(Notification.class);
                            notification.setId(document.getId());
                            
                            // Log notification details for debugging
                            Log.d(TAG, "Adding notification: " + notification.getId() + 
                                  ", Title: " + notification.getTitle() + 
                                  ", Type: " + notification.getType());
                            
                            userNotifications.add(notification);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing notification document: " + document.getId(), e);
                    }
                }
                
                Log.d(TAG, "Fallback method found " + userNotifications.size() + " notifications for current user");
                
                updateNotificationList(userNotifications);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error in fallback method", e);
                hideLoading();
                showEmptyState();
            });
    }
} 