package com.it342.projectmanagementsystem.utils;

import android.os.CountDownTimer;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class to manage countdown timers for appointment items
 */
public class CountdownTimerManager {
    private static final long COUNTDOWN_INTERVAL = 1000; // Update every second
    private Map<String, CountDownTimer> activeTimers = new HashMap<>();

    /**
     * Starts a countdown timer for the given appointment ID
     * 
     * @param appointmentId The unique ID of the appointment
     * @param targetTimeMillis The target time in milliseconds
     * @param textView The TextView to update with countdown text
     * @param onFinishCallback Callback to execute when timer finishes
     */
    public void startTimer(String appointmentId, long targetTimeMillis, TextView textView, Runnable onFinishCallback) {
        // Don't start a timer if the TextView is already showing "Appointment Finished"
        if (textView.getText().toString().equals("Appointment Finished")) {
            return;
        }
        
        // Cancel any existing timer for this appointment
        stopTimer(appointmentId);
        
        // Calculate time remaining
        long currentTimeMillis = System.currentTimeMillis();
        long timeRemainingMillis = targetTimeMillis - currentTimeMillis;
        
        // Check if time has already passed
        if (timeRemainingMillis <= 0) {
            textView.setText("Appointment Finished");
            if (onFinishCallback != null) {
                onFinishCallback.run();
            }
            return;
        }
        
        // Create and start new timer
        CountDownTimer timer = new CountDownTimer(timeRemainingMillis, COUNTDOWN_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Check if the appointment status has been changed externally
                if (textView.getText().toString().equals("Appointment Finished")) {
                    // Status was changed outside, cancel this timer
                    cancel();
                    activeTimers.remove(appointmentId);
                    return;
                }
                
                // Calculate remaining time
                long hours = millisUntilFinished / (60 * 60 * 1000);
                millisUntilFinished %= (60 * 60 * 1000);
                long minutes = millisUntilFinished / (60 * 1000);
                millisUntilFinished %= (60 * 1000);
                long seconds = millisUntilFinished / 1000;
                
                // Update UI with remaining time
                String timeRemainingText = String.format(Locale.getDefault(), 
                    "%dh %dm %ds remaining", hours, minutes, seconds);
                textView.setText(timeRemainingText);
            }
            
            @Override
            public void onFinish() {
                textView.setText("Appointment Finished");
                if (onFinishCallback != null) {
                    onFinishCallback.run();
                }
                activeTimers.remove(appointmentId);
            }
        };
        
        // Store and start the timer
        activeTimers.put(appointmentId, timer);
        timer.start();
    }
    
    /**
     * Stops the timer for the given appointment ID
     * 
     * @param appointmentId The unique ID of the appointment
     */
    public void stopTimer(String appointmentId) {
        CountDownTimer timer = activeTimers.get(appointmentId);
        if (timer != null) {
            timer.cancel();
            activeTimers.remove(appointmentId);
        }
    }
    
    /**
     * Stops all active timers
     */
    public void stopAllTimers() {
        for (CountDownTimer timer : activeTimers.values()) {
            timer.cancel();
        }
        activeTimers.clear();
    }
} 