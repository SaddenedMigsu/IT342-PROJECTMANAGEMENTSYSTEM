package com.it342.projectmanagementsystem;

import android.app.Application;
import com.it342.projectmanagementsystem.api.RetrofitClient;

public class PMSApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitClient.init(this);
    }
} 