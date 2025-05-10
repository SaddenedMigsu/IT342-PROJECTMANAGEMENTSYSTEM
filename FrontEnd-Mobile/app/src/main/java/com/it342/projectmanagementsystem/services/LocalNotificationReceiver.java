package com.it342.projectmanagementsystem.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.it342.projectmanagementsystem.utils.LocalNotificationService;

/**
 * BroadcastReceiver to handle scheduled notification checks
 */
public class LocalNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "LocalNotifReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received broadcast: " + intent.getAction());
        
        if ("CHECK_NOTIFICATIONS".equals(intent.getAction())) {
            // Check for new notifications
            LocalNotificationService.checkForNewNotifications(context);
        } else if ("BOOT_COMPLETED".equals(intent.getAction())) {
            // Reschedule notification checks after device reboot
            LocalNotificationService.scheduleNotificationChecks(context);
        }
    }
} 