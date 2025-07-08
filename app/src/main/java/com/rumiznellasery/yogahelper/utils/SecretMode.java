package com.rumiznellasery.yogahelper.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SecretMode {
    private static final String PREFS_NAME = "secret_mode_prefs";
    private static final String KEY_SECRET_MODE = "secret_mode";

    public static boolean isSecretMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SECRET_MODE, false);
    }

    public static void setSecretMode(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SECRET_MODE, enabled).apply();
    }

    public static void toggleSecretMode(Context context) {
        boolean current = isSecretMode(context);
        setSecretMode(context, !current);
    }
} 