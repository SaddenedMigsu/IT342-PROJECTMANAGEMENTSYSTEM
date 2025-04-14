package com.it342.projectmanagementsystem;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class ProjectManagementApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
    }
} 