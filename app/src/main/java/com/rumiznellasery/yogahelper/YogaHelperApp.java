package com.rumiznellasery.yogahelper;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;
import com.rumiznellasery.yogahelper.utils.Logger;

public class YogaHelperApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Logger.init(this);
        Logger.info("Application started");
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        Logger.info("Application terminated");
        Logger.close();
    }
}

