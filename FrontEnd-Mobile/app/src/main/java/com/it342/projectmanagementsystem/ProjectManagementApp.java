package com.it342.projectmanagementsystem;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.it342.projectmanagementsystem.api.RetrofitClient;

public class ProjectManagementApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        RetrofitClient.init(this);
    }
} 