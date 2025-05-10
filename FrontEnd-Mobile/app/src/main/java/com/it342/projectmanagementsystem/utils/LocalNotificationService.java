package com.it342.projectmanagementsystem.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.it342.projectmanagementsystem.R;
import com.it342.projectmanagementsystem.activities.AppointmentDetailsActivity;
import com.it342.projectmanagementsystem.activities.NotificationsActivity;
import com.it342.projectmanagementsystem.services.LocalNotificationReceiver;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service to handle local notifications without depending on FCM
 */
public class LocalNotificationService {
    private static final String TAG = "LocalNotificationService";
    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String KEY_LAST_CHECK = "last_notification_check";
    private static final long CHECK_INTERVAL = TimeUnit.MINUTES.toMillis(5); // Check every 5 minutes
    
    // Notification types
    private static final String TYPE_APPOINTMENT_REMINDER = "APPOINTMENT_REMINDER";
    private static final String TYPE_SYSTEM = "SYSTEM_NOTIFICATION";
    
    // Notification channels
    private static final String CHANNEL_APPOINTMENTS = "appointments";
    private static final String CHANNEL_REMINDERS = "reminders";
    private static final String CHANNEL_SYSTEM = "system";

    /**
     * Schedule periodic checks for new notifications
     */
    public static void scheduleNotificationChecks(Context context) {
        Log.d(TAG, "Scheduling notification checks");
        
        // Create notification channels
        createNotificationChannels(context);
        
        Intent intent = new Intent(context, LocalNotificationReceiver.class);
        intent.setAction("CHECK_NOTIFICATIONS");
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // Schedule repeating alarm
        alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + CHECK_INTERVAL,
                CHECK_INTERVAL,
                pendingIntent
        );
        
        Log.d(TAG, "Notification checks scheduled every " + (CHECK_INTERVAL / 1000 / 60) + " minutes");
        
        // Also check immediately
        checkForNewNotifications(context);
    }
    
    /**
     * Create notification channels for Android O and above
     */
    private static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            // Appointments channel - high priority
            NotificationChannel appointmentsChannel = new NotificationChannel(
                    CHANNEL_APPOINTMENTS,
                    "Appointments",
                    NotificationManager.IMPORTANCE_HIGH);
            appointmentsChannel.setDescription("Appointment notifications");
            notificationManager.createNotificationChannel(appointmentsChannel);
            
            // Reminders channel - high priority
            NotificationChannel remindersChannel = new NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            remindersChannel.setDescription("Appointment reminders");
            notificationManager.createNotificationChannel(remindersChannel);
            
            // System channel - default priority
            NotificationChannel systemChannel = new NotificationChannel(
                    CHANNEL_SYSTEM,
                    "System",
                    NotificationManager.IMPORTANCE_DEFAULT);
            systemChannel.setDescription("System notifications");
            notificationManager.createNotificationChannel(systemChannel);
        }
    }
    
    /**
     * Check for new notifications in Firestore
     */
    public static void checkForNewNotifications(Context context) {
        Log.d(TAG, "Checking for new notifications");
        
        // Get user info
        SharedPreferences authPrefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        String userId = authPrefs.getString("userId", "");
        String email = authPrefs.getString("email", "");
        String role = authPrefs.getString("role", "");
        
        if (userId.isEmpty() && email.isEmpty()) {
            Log.e(TAG, "Cannot check for notifications: No user ID or email");
            return;
        }
        
        // Get last check time
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastCheckTime = prefs.getLong(KEY_LAST_CHECK, 0);
        Date lastCheckDate = new Date(lastCheckTime);
        
        Log.d(TAG, "Last check time: " + lastCheckDate);
        
        // Update last check time
        prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply();
        
        // Check for new notifications in Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Check appointments collection for upcoming appointments
        checkUpcomingAppointments(context, db, userId, email, role, lastCheckDate);
        
        // Check notifications collection for unread notifications
        checkUnreadNotifications(context, db, userId, email, lastCheckDate);
    }
    
    /**
     * Check for upcoming appointments
     */
    private static void checkUpcomingAppointments(Context context, FirebaseFirestore db, 
                                                 String userId, String email, String role,
                                                 Date lastCheckDate) {
        // Get current time
        Date now = new Date();
        // Get time 30 minutes from now
        Date thirtyMinutesFromNow = new Date(now.getTime() + TimeUnit.MINUTES.toMillis(30));
        
        // Query for appointments that start within the next 30 minutes
        Query query;
        if ("FACULTY".equalsIgnoreCase(role)) {
            query = db.collection("appointments")
                    .whereEqualTo("facultyId", userId)
                    .whereGreaterThan("startTime", Timestamp.now())
                    .whereLessThan("startTime", new Timestamp(thirtyMinutesFromNow));
        } else {
            query = db.collection("appointments")
                    .whereArrayContains("participants", email)
                    .whereGreaterThan("startTime", Timestamp.now())
                    .whereLessThan("startTime", new Timestamp(thirtyMinutesFromNow));
        }
        
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                QuerySnapshot snapshot = task.getResult();
                Log.d(TAG, "Found " + snapshot.size() + " upcoming appointments");
                
                for (DocumentSnapshot document : snapshot.getDocuments()) {
                    String appointmentId = document.getId();
                    String title = document.getString("title");
                    Timestamp startTime = (Timestamp) document.get("startTime");
                    
                    // Create notification for upcoming appointment
                    if (title != null && startTime != null) {
                        long minutesUntilStart = TimeUnit.MILLISECONDS.toMinutes(
                                startTime.toDate().getTime() - System.currentTimeMillis());
                        
                        String notificationTitle = "Upcoming Appointment";
                        String notificationBody = title + " starts in " + minutesUntilStart + " minutes";
                        
                        // Show notification
                        showNotification(
                                context, 
                                notificationTitle, 
                                notificationBody, 
                                appointmentId, 
                                TYPE_APPOINTMENT_REMINDER
                        );
                    }
                }
            } else {
                Log.e(TAG, "Error checking upcoming appointments", task.getException());
            }
        });
    }
    
    /**
     * Check for unread notifications
     */
    private static void checkUnreadNotifications(Context context, FirebaseFirestore db, 
                                               String userId, String email,
                                               Date lastCheckDate) {
        // Query for unread notifications
        Query query = db.collection("notifications")
                .whereEqualTo("read", false)
                .whereGreaterThan("createdAt", new Timestamp(lastCheckDate));
        
        // Add recipient filter
        if (!userId.isEmpty()) {
            query = query.whereEqualTo("recipientId", userId);
        } else if (!email.isEmpty()) {
            query = query.whereEqualTo("userId", email);
        }
        
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                QuerySnapshot snapshot = task.getResult();
                Log.d(TAG, "Found " + snapshot.size() + " unread notifications");
                
                for (DocumentSnapshot document : snapshot.getDocuments()) {
                    String title = document.getString("title");
                    String message = document.getString("message");
                    String appointmentId = document.getString("appointmentId");
                    String type = document.getString("type");
                    
                    // Show notification
                    if (title != null && message != null) {
                        showNotification(context, title, message, appointmentId, type);
                    }
                }
            } else {
                Log.e(TAG, "Error checking unread notifications", task.getException());
            }
        });
    }
    
    /**
     * Create a test notification for debugging
     */
    public static void createTestNotification(Context context) {
        Log.d(TAG, "Creating test notification");
        
        // Show notification
        showNotification(
                context,
                "Test Notification",
                "This is a test notification to verify the notification system is working.",
                null,
                TYPE_SYSTEM
        );
        
        // Also save to local storage
        NotificationUtils.saveNotification(
                context,
                "Test Notification",
                "This is a test notification to verify the notification system is working.",
                null,
                TYPE_SYSTEM
        );
    }
    
    /**
     * Display a notification with the given data
     */
    private static void showNotification(Context context, String title, String body, 
                                        String appointmentId, String type) {
        // Determine the appropriate channel
        String channelId = CHANNEL_APPOINTMENTS;
        if (TYPE_APPOINTMENT_REMINDER.equals(type)) {
            channelId = CHANNEL_REMINDERS;
        } else if (TYPE_SYSTEM.equals(type)) {
            channelId = CHANNEL_SYSTEM;
        }
        
        // Create intent based on notification type
        Intent intent;
        int notificationPriority = NotificationCompat.PRIORITY_DEFAULT;
        
        if (type != null && type.contains("APPOINTMENT_REQUEST")) {
            intent = new Intent(context, NotificationsActivity.class);
            notificationPriority = NotificationCompat.PRIORITY_HIGH;
        } else if (appointmentId != null) {
            intent = new Intent(context, AppointmentDetailsActivity.class);
            intent.putExtra("appointmentId", appointmentId);
        } else {
            intent = new Intent(context, NotificationsActivity.class);
        }
        
        // Add notification type if available
        if (type != null) {
            intent.putExtra("type", type);
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );
        
        // Get default notification sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        
        // Build the notification
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(notificationPriority)
                        .setContentIntent(pendingIntent);
        
        // Show the notification
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Create notification ID based on appointmentId or current time
        int notificationId;
        if (appointmentId != null) {
            notificationId = appointmentId.hashCode();
        } else {
            notificationId = (int) System.currentTimeMillis();
        }
        
        notificationManager.notify(notificationId, notificationBuilder.build());
        
        // Save notification to local storage
        NotificationUtils.saveNotification(context, title, body, appointmentId, type);
    }
} 