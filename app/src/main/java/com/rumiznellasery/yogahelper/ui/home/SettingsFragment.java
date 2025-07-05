package com.rumiznellasery.yogahelper.ui.home;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.data.DbKeys;
import com.rumiznellasery.yogahelper.databinding.FragmentSettingsBinding;
import com.rumiznellasery.yogahelper.utils.DeveloperMode;
import com.rumiznellasery.yogahelper.utils.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private SharedPreferences prefs;
    private static final int WORKOUT_REMINDER_REQUEST_CODE = 1001;
    private static final String WORKOUT_REMINDER_ACTION = "com.rumiznellasery.yogahelper.WORKOUT_REMINDER";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        
        setupProfilePicture();
        setupThemeSettings();
        setupNotificationSettings();
        setupAccessibilitySettings();
        setupPrivacyDataSettings();
        setupDeveloperMode();
        setupLogout();
        
        return binding.getRoot();
    }

    private void setupProfilePicture() {
        // Load profile picture from internal storage
        SharedPreferences profilePrefs = requireContext().getSharedPreferences("profile", Context.MODE_PRIVATE);
        String path = profilePrefs.getString("profile_picture_path", null);
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                binding.settingsProfilePic.setImageURI(Uri.fromFile(file));
            } else {
                binding.settingsProfilePic.setImageResource(R.drawable.ic_avatar_placeholder);
            }
        } else {
            binding.settingsProfilePic.setImageResource(R.drawable.ic_avatar_placeholder);
        }

        // Handle profile picture click
        binding.settingsProfilePic.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 1001);
        });
    }

    private void setupThemeSettings() {
        // Dark mode switch
        boolean isDarkMode = prefs.getBoolean("dark_mode", true);
        binding.switchDarkMode.setChecked(isDarkMode);
        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            Toast.makeText(requireContext(), 
                isChecked ? "Dark mode enabled" : "Light mode enabled", 
                Toast.LENGTH_SHORT).show();
        });

        // Auto theme switch
        boolean isAutoTheme = prefs.getBoolean("auto_theme", false);
        binding.switchAutoTheme.setChecked(isAutoTheme);
        binding.switchAutoTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("auto_theme", isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
            Toast.makeText(requireContext(), 
                isChecked ? "Auto theme enabled" : "Auto theme disabled", 
                Toast.LENGTH_SHORT).show();
        });
    }

    private void setupNotificationSettings() {
        // Workout reminders
        boolean workoutReminders = prefs.getBoolean("workout_reminders", true);
        binding.switchWorkoutReminders.setChecked(workoutReminders);
        binding.switchWorkoutReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("workout_reminders", isChecked).apply();
            if (isChecked) {
                scheduleWorkoutReminder();
            } else {
                cancelWorkoutReminder();
            }
            Toast.makeText(requireContext(), 
                isChecked ? "Workout reminders enabled" : "Workout reminders disabled", 
                Toast.LENGTH_SHORT).show();
        });

        // Achievement notifications
        boolean achievementNotifications = prefs.getBoolean("achievement_notifications", true);
        binding.switchAchievementNotifications.setChecked(achievementNotifications);
        binding.switchAchievementNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("achievement_notifications", isChecked).apply();
            Toast.makeText(requireContext(), 
                isChecked ? "Achievement notifications enabled" : "Achievement notifications disabled", 
                Toast.LENGTH_SHORT).show();
        });

        // Friend activity notifications
        boolean friendActivity = prefs.getBoolean("friend_activity", true);
        binding.switchFriendActivity.setChecked(friendActivity);
        binding.switchFriendActivity.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("friend_activity", isChecked).apply();
            Toast.makeText(requireContext(), 
                isChecked ? "Friend activity notifications enabled" : "Friend activity notifications disabled", 
                Toast.LENGTH_SHORT).show();
        });
    }

    private void scheduleWorkoutReminder() {
        try {
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(WORKOUT_REMINDER_ACTION);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(), 
                WORKOUT_REMINDER_REQUEST_CODE, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Schedule for 9:00 AM daily
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 9);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            // If it's already past 9 AM, schedule for tomorrow
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            );

            Logger.info("Workout reminder scheduled for " + calendar.getTime().toString());
        } catch (Exception e) {
            Logger.error("Error scheduling workout reminder", e);
        }
    }

    private void cancelWorkoutReminder() {
        try {
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(WORKOUT_REMINDER_ACTION);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(), 
                WORKOUT_REMINDER_REQUEST_CODE, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(pendingIntent);
            Logger.info("Workout reminder cancelled");
        } catch (Exception e) {
            Logger.error("Error cancelling workout reminder", e);
        }
    }

    private void setupAccessibilitySettings() {
        // Large text
        boolean largeText = prefs.getBoolean("large_text", false);
        binding.switchLargeText.setChecked(largeText);
        binding.switchLargeText.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("large_text", isChecked).apply();
            applyLargeText(isChecked);
            Toast.makeText(requireContext(), 
                isChecked ? "Large text enabled - restart app to see changes" : "Large text disabled - restart app to see changes", 
                Toast.LENGTH_LONG).show();
        });

        // Reduce motion
        boolean reduceMotion = prefs.getBoolean("reduce_motion", false);
        binding.switchReduceMotion.setChecked(reduceMotion);
        binding.switchReduceMotion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("reduce_motion", isChecked).apply();
            applyReducedMotion(isChecked);
            Toast.makeText(requireContext(), 
                isChecked ? "Reduced motion enabled" : "Reduced motion disabled", 
                Toast.LENGTH_SHORT).show();
        });
    }

    private void applyLargeText(boolean enabled) {
        // Apply large text scaling to the current activity
        if (getActivity() != null) {
            if (enabled) {
                getActivity().getResources().getConfiguration().fontScale = 1.3f;
            } else {
                getActivity().getResources().getConfiguration().fontScale = 1.0f;
            }
            getActivity().recreate();
        }
    }

    private void applyReducedMotion(boolean enabled) {
        // Store the setting for use in animations throughout the app
        prefs.edit().putBoolean("reduce_motion", enabled).apply();
        
        // You can check this setting in other parts of the app like:
        // if (prefs.getBoolean("reduce_motion", false)) {
        //     // Use shorter or no animations
        // }
    }

    private void setupPrivacyDataSettings() {
        // Data export
        binding.layoutDataExport.setOnClickListener(v -> exportData());

        // Data backup
        binding.layoutDataBackup.setOnClickListener(v -> backupData());

        // Delete account
        binding.layoutDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void exportData() {
        try {
            // Create data export
            StringBuilder exportData = new StringBuilder();
            exportData.append("=".repeat(50)).append("\n");
            exportData.append("YOGA ASSISTANT - COMPREHENSIVE DATA EXPORT\n");
            exportData.append("=".repeat(50)).append("\n");
            exportData.append("Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
            exportData.append("App Version: 1.0\n");
            exportData.append("=".repeat(50)).append("\n\n");
            
            // Add user info
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                exportData.append("📱 USER INFORMATION\n");
                exportData.append("-".repeat(30)).append("\n");
                exportData.append("Email: ").append(user.getEmail()).append("\n");
                exportData.append("Display Name: ").append(user.getDisplayName() != null ? user.getDisplayName() : "Not set").append("\n");
                exportData.append("Email Verified: ").append(user.isEmailVerified() ? "Yes" : "No").append("\n");
                exportData.append("Account Created: ").append(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(user.getMetadata().getCreationTimestamp()))).append("\n");
                exportData.append("Last Sign In: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(user.getMetadata().getLastSignInTimestamp()))).append("\n\n");
            } else {
                exportData.append("📱 USER INFORMATION\n");
                exportData.append("-".repeat(30)).append("\n");
                exportData.append("No user logged in\n\n");
            }
            
            // Add detailed user stats
            SharedPreferences statsPrefs = requireContext().getSharedPreferences("stats", Context.MODE_PRIVATE);
            exportData.append("📊 WORKOUT STATISTICS\n");
            exportData.append("-".repeat(30)).append("\n");
            int totalWorkouts = statsPrefs.getInt("workouts", 0);
            int currentStreak = statsPrefs.getInt("streak", 0);
            int totalCalories = statsPrefs.getInt("calories", 0);
            int workoutsThisWeek = statsPrefs.getInt("workouts_this_week", 0);
            String lastWorkoutDate = statsPrefs.getString("last_workout_date", "Never");
            
            exportData.append("Total Workouts: ").append(totalWorkouts).append("\n");
            exportData.append("Current Streak: ").append(currentStreak).append(" days\n");
            exportData.append("Total Calories Burned: ").append(totalCalories).append(" calories\n");
            exportData.append("Last Workout: ").append(lastWorkoutDate).append("\n");
            exportData.append("Workouts This Week: ").append(workoutsThisWeek).append("\n");
            
            // Calculate additional stats
            if (totalWorkouts > 0) {
                double avgCaloriesPerWorkout = (double) totalCalories / totalWorkouts;
                exportData.append("Average Calories per Workout: ").append(String.format("%.1f", avgCaloriesPerWorkout)).append(" calories\n");
            }
            
            // Weekly progress
            int currentWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
            int savedWeek = statsPrefs.getInt("workouts_week", -1);
            if (savedWeek == currentWeek) {
                exportData.append("Weekly Goal Progress: ").append(workoutsThisWeek).append("/10 workouts\n");
                double weeklyProgress = (double) workoutsThisWeek / 10 * 100;
                exportData.append("Weekly Goal Completion: ").append(String.format("%.1f%%", weeklyProgress)).append("\n");
            }
            exportData.append("\n");

            // Add detailed achievements
            try {
                com.rumiznellasery.yogahelper.utils.AchievementManager achievementManager = 
                    com.rumiznellasery.yogahelper.utils.AchievementManager.getInstance(requireContext());
                exportData.append("🏆 ACHIEVEMENTS\n");
                exportData.append("-".repeat(30)).append("\n");
                exportData.append("Overall Progress: ").append(achievementManager.getUnlockedCount()).append("/").append(achievementManager.getTotalCount()).append(" unlocked\n");
                exportData.append("Completion Rate: ").append(String.format("%.1f%%", achievementManager.getCompletionPercentage())).append("\n\n");
                
                // List individual achievements with details
                exportData.append("Individual Achievements:\n");
                for (com.rumiznellasery.yogahelper.data.Achievement achievement : achievementManager.getAchievements()) {
                    String status = achievement.unlocked ? "✅ UNLOCKED" : "🔒 LOCKED";
                    String progress = achievement.currentProgress + "/" + achievement.requirement;
                    String percentage = String.format("%.1f%%", achievement.getProgressPercentage());
                    
                    exportData.append("• ").append(achievement.title).append("\n");
                    exportData.append("  Status: ").append(status).append("\n");
                    exportData.append("  Progress: ").append(progress).append(" (").append(percentage).append(")\n");
                    exportData.append("  Description: ").append(achievement.description).append("\n");
                    
                    if (achievement.unlocked && achievement.unlockedDate > 0) {
                        String unlockDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(achievement.unlockedDate));
                        exportData.append("  Unlocked: ").append(unlockDate).append("\n");
                    }
                    exportData.append("\n");
                }
            } catch (Exception e) {
                exportData.append("🏆 ACHIEVEMENTS\n");
                exportData.append("-".repeat(30)).append("\n");
                exportData.append("Error loading achievements: ").append(e.getMessage()).append("\n\n");
            }

            // Add comprehensive app settings
            exportData.append("⚙️ APP SETTINGS\n");
            exportData.append("-".repeat(30)).append("\n");
            exportData.append("Theme Settings:\n");
            exportData.append("  • Dark Mode: ").append(prefs.getBoolean("dark_mode", true) ? "Enabled" : "Disabled").append("\n");
            exportData.append("  • Auto Theme: ").append(prefs.getBoolean("auto_theme", false) ? "Enabled" : "Disabled").append("\n");
            exportData.append("\nNotification Settings:\n");
            exportData.append("  • Workout Reminders: ").append(prefs.getBoolean("workout_reminders", true) ? "Enabled" : "Disabled").append("\n");
            exportData.append("  • Achievement Notifications: ").append(prefs.getBoolean("achievement_notifications", true) ? "Enabled" : "Disabled").append("\n");
            exportData.append("  • Friend Activity: ").append(prefs.getBoolean("friend_activity", true) ? "Enabled" : "Disabled").append("\n");
            exportData.append("\nAccessibility Settings:\n");
            exportData.append("  • Large Text: ").append(prefs.getBoolean("large_text", false) ? "Enabled" : "Disabled").append("\n");
            exportData.append("  • Reduced Motion: ").append(prefs.getBoolean("reduce_motion", false) ? "Enabled" : "Disabled").append("\n\n");

            // Add system information
            exportData.append("📱 SYSTEM INFORMATION\n");
            exportData.append("-".repeat(30)).append("\n");
            exportData.append("Android Version: ").append(android.os.Build.VERSION.RELEASE).append("\n");
            exportData.append("Device Model: ").append(android.os.Build.MODEL).append("\n");
            exportData.append("Manufacturer: ").append(android.os.Build.MANUFACTURER).append("\n");
            exportData.append("App Package: ").append(requireContext().getPackageName()).append("\n");
            exportData.append("Export Date: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n\n");

            // Add summary
            exportData.append("📋 EXPORT SUMMARY\n");
            exportData.append("-".repeat(30)).append("\n");
            exportData.append("• User has completed ").append(totalWorkouts).append(" workouts\n");
            exportData.append("• Current streak: ").append(currentStreak).append(" days\n");
            exportData.append("• Total calories burned: ").append(totalCalories).append("\n");
            exportData.append("• Achievements unlocked: ").append(achievementManager != null ? achievementManager.getUnlockedCount() : 0).append("\n");
            exportData.append("• Last workout: ").append(lastWorkoutDate).append("\n");
            exportData.append("• Export generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n\n");
            
            exportData.append("=".repeat(50)).append("\n");
            exportData.append("END OF EXPORT\n");
            exportData.append("=".repeat(50)).append("\n");

            // Save to file with proper encoding
            String fileName = "yoga_assistant_export_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".txt";
            File exportFile = new File(requireContext().getExternalFilesDir(null), fileName);
            
            FileOutputStream fos = new FileOutputStream(exportFile);
            fos.write(exportData.toString().getBytes("UTF-8"));
            fos.close();

            // Share file with proper MIME type and FileProvider
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            
            // Use FileProvider for better compatibility
            Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                exportFile
            );
            
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Yoga Assistant - Comprehensive Data Export");
            shareIntent.putExtra(Intent.EXTRA_TEXT, 
                "📊 Yoga Assistant Data Export\n\n" +
                "This file contains your complete workout statistics, achievements, and app settings.\n" +
                "Generated on: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n\n" +
                "File includes:\n" +
                "• User profile information\n" +
                "• Workout statistics and progress\n" +
                "• Achievement status and progress\n" +
                "• App settings and preferences\n" +
                "• System information\n\n" +
                "Keep this file safe for your records!"
            );
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "Share Data Export"));
            Toast.makeText(requireContext(), "📊 Data exported successfully! File saved and ready to share.", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Logger.error("Error exporting data", e);
            Toast.makeText(requireContext(), "Error exporting data", Toast.LENGTH_SHORT).show();
        }
    }

    private void backupData() {
        try {
            // Create backup data
            StringBuilder backupData = new StringBuilder();
            backupData.append("Yoga Assistant - Cloud Backup\n");
            backupData.append("Backup Date: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n\n");
            
            // Add user info
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                backupData.append("User Information:\n");
                backupData.append("- Email: ").append(user.getEmail()).append("\n");
                backupData.append("- Display Name: ").append(user.getDisplayName() != null ? user.getDisplayName() : "Not set").append("\n");
                backupData.append("- Email Verified: ").append(user.isEmailVerified()).append("\n");
                backupData.append("- Account Created: ").append(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(user.getMetadata().getCreationTimestamp()))).append("\n\n");
            }
            
            // Add user stats
            SharedPreferences statsPrefs = requireContext().getSharedPreferences("stats", Context.MODE_PRIVATE);
            backupData.append("User Statistics:\n");
            backupData.append("- Total Workouts: ").append(statsPrefs.getInt("workouts", 0)).append("\n");
            backupData.append("- Current Streak: ").append(statsPrefs.getInt("streak", 0)).append("\n");
            backupData.append("- Total Calories: ").append(statsPrefs.getInt("calories", 0)).append("\n");
            backupData.append("- Last Workout: ").append(statsPrefs.getString("last_workout_date", "Never")).append("\n");
            backupData.append("- Workouts This Week: ").append(statsPrefs.getInt("workouts_this_week", 0)).append("\n\n");

            // Add achievements
            try {
                com.rumiznellasery.yogahelper.utils.AchievementManager achievementManager = 
                    com.rumiznellasery.yogahelper.utils.AchievementManager.getInstance(requireContext());
                backupData.append("Achievements:\n");
                backupData.append("- Unlocked: ").append(achievementManager.getUnlockedCount()).append("/").append(achievementManager.getTotalCount()).append("\n");
                backupData.append("- Completion: ").append(String.format("%.0f%%", achievementManager.getCompletionPercentage())).append("\n\n");
            } catch (Exception e) {
                backupData.append("Achievements: Error loading achievements\n\n");
            }

            // Add app settings
            backupData.append("App Settings:\n");
            backupData.append("- Dark Mode: ").append(prefs.getBoolean("dark_mode", true)).append("\n");
            backupData.append("- Auto Theme: ").append(prefs.getBoolean("auto_theme", false)).append("\n");
            backupData.append("- Workout Reminders: ").append(prefs.getBoolean("workout_reminders", true)).append("\n");
            backupData.append("- Achievement Notifications: ").append(prefs.getBoolean("achievement_notifications", true)).append("\n");
            backupData.append("- Friend Activity: ").append(prefs.getBoolean("friend_activity", true)).append("\n");
            backupData.append("- Large Text: ").append(prefs.getBoolean("large_text", false)).append("\n");
            backupData.append("- Reduced Motion: ").append(prefs.getBoolean("reduce_motion", false)).append("\n\n");

            // Save backup to Firebase
            if (user != null) {
                DbKeys keys = DbKeys.get(requireContext());
                DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
                    .getReference(keys.users)
                    .child(user.getUid())
                    .child("backups")
                    .child(new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()));

                ref.setValue(backupData.toString()).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Backup saved to cloud successfully!", Toast.LENGTH_SHORT).show();
                        Logger.info("Cloud backup completed successfully");
                    } else {
                        Toast.makeText(requireContext(), "Backup failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Logger.error("Cloud backup failed", task.getException());
                    }
                });
            } else {
                Toast.makeText(requireContext(), "Backup failed: User not logged in", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Logger.error("Error creating backup", e);
            Toast.makeText(requireContext(), "Backup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently lost.")
            .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    private void deleteAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Delete from Firebase Database
            DbKeys keys = DbKeys.get(requireContext());
            DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
                .getReference(keys.users)
                .child(user.getUid());
            
            ref.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Delete Firebase Auth account
                    user.delete().addOnCompleteListener(deleteTask -> {
                        if (deleteTask.isSuccessful()) {
                            // Clear local data
                            clearLocalData();
                            Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();
                            
                            // Navigate to auth activity
                            Intent intent = new Intent(requireContext(), com.rumiznellasery.yogahelper.AuthActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            Toast.makeText(requireContext(), "Error deleting account", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(requireContext(), "Error deleting account data", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void clearLocalData() {
        // Clear all SharedPreferences
        requireContext().getSharedPreferences("stats", Context.MODE_PRIVATE).edit().clear().apply();
        requireContext().getSharedPreferences("profile", Context.MODE_PRIVATE).edit().clear().apply();
        requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE).edit().clear().apply();
        
        // Delete profile picture
        SharedPreferences profilePrefs = requireContext().getSharedPreferences("profile", Context.MODE_PRIVATE);
        String path = profilePrefs.getString("profile_picture_path", null);
        if (path != null) {
            File file = new File(path);
            if (file.exists()) file.delete();
        }
    }

    private void setupDeveloperMode() {
        // Check if current user is a developer
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && DeveloperMode.isDeveloperEmail(currentUser.getEmail())) {
            binding.developerSection.setVisibility(View.VISIBLE);
            
            // Set current state
            binding.switchDeveloperMode.setChecked(DeveloperMode.isDeveloperMode(requireContext()));
            
            // Handle switch changes
            binding.switchDeveloperMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                DeveloperMode.setDeveloperMode(requireContext(), isChecked);
                Toast.makeText(requireContext(), 
                    isChecked ? "Developer mode enabled" : "Developer mode disabled", 
                    Toast.LENGTH_SHORT).show();
            });
        } else {
            binding.developerSection.setVisibility(View.GONE);
        }
    }

    private void setupLogout() {
        binding.buttonLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void logout() {
        try {
            // Delete profile picture from internal storage
            SharedPreferences profilePrefs = requireContext().getSharedPreferences("profile", Context.MODE_PRIVATE);
            String path = profilePrefs.getString("profile_picture_path", null);
            if (path != null) {
                File file = new File(path);
                if (file.exists()) file.delete();
                profilePrefs.edit().remove("profile_picture_path").apply();
            }
            
            Logger.info("User logging out from SettingsFragment");
            FirebaseAuth.getInstance().signOut();
            
            Intent intent = new Intent(requireContext(), com.rumiznellasery.yogahelper.AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Logger.error("Error during logout in SettingsFragment", e);
            Toast.makeText(requireContext(), "Error during logout", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == android.app.Activity.RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                // Save profile picture to internal storage
                try {
                    File profileDir = new File(requireContext().getFilesDir(), "profile");
                    if (!profileDir.exists()) {
                        profileDir.mkdirs();
                    }
                    
                    File profileFile = new File(profileDir, "profile_picture.jpg");
                    FileOutputStream fos = new FileOutputStream(profileFile);
                    requireContext().getContentResolver().openInputStream(selectedImage).transferTo(fos);
                    fos.close();
                    
                    // Save path to SharedPreferences
                    SharedPreferences profilePrefs = requireContext().getSharedPreferences("profile", Context.MODE_PRIVATE);
                    profilePrefs.edit().putString("profile_picture_path", profileFile.getAbsolutePath()).apply();
                    
                    // Update UI
                    binding.settingsProfilePic.setImageURI(Uri.fromFile(profileFile));
                    Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Logger.error("Error saving profile picture", e);
                    Toast.makeText(requireContext(), "Error saving profile picture", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 