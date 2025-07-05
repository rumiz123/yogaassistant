package com.rumiznellasery.yogahelper.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.rumiznellasery.yogahelper.MainActivity;
import com.rumiznellasery.yogahelper.R;

public class WorkoutReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "workout_reminders";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Check if workout reminders are enabled
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean remindersEnabled = prefs.getBoolean("workout_reminders", true);
        
        if (!remindersEnabled) {
            return;
        }

        // Check if user has already worked out today
        SharedPreferences statsPrefs = context.getSharedPreferences("stats", Context.MODE_PRIVATE);
        String lastWorkoutDate = statsPrefs.getString("last_workout_date", "");
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(new java.util.Date());
        
        if (lastWorkoutDate.equals(today)) {
            // User already worked out today, don't send reminder
            return;
        }

        createNotificationChannel(context);
        showWorkoutReminder(context);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Workout Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Daily workout reminders");
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showWorkoutReminder(Context context) {
        // Create intent to open the app
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle("Time for Yoga! 🧘‍♀️")
            .setContentText("Don't break your streak! Start your daily workout now.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        // Show notification
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
        
        Logger.info("Workout reminder notification sent");
    }
} 