package com.rumiznellasery.yogahelper.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class AccessibilityHelper {
    private static final String SETTINGS_PREFS = "settings";
    
    public static boolean isLargeTextEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean("large_text", false);
    }
    
    public static boolean isReducedMotionEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean("reduce_motion", false);
    }
    
    public static Animation getAnimation(Context context, int animationResId) {
        if (isReducedMotionEnabled(context)) {
            // Return a very short animation or null for reduced motion
            Animation shortAnimation = AnimationUtils.loadAnimation(context, animationResId);
            if (shortAnimation != null) {
                shortAnimation.setDuration(100); // Very short duration
            }
            return shortAnimation;
        } else {
            return AnimationUtils.loadAnimation(context, animationResId);
        }
    }
    
    public static float getTextScale(Context context) {
        if (isLargeTextEnabled(context)) {
            return 1.3f;
        } else {
            return 1.0f;
        }
    }
    
    public static boolean areNotificationsEnabled(Context context, String notificationType) {
        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(notificationType, true);
    }
    
    public static boolean areWorkoutRemindersEnabled(Context context) {
        return areNotificationsEnabled(context, "workout_reminders");
    }
    
    public static boolean areBadgeNotificationsEnabled(Context context) {
        return areNotificationsEnabled(context, "badge_notifications");
    }
    
    public static boolean areFriendActivityNotificationsEnabled(Context context) {
        return areNotificationsEnabled(context, "friend_activity");
    }
} 