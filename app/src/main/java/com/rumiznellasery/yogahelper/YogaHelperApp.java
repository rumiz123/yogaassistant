package com.rumiznellasery.yogahelper;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class YogaHelperApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
}

