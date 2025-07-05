package com.rumiznellasery.yogahelper.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
import java.util.Date;
import java.util.Locale;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private SharedPreferences prefs;

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
            // TODO: Schedule/cancel workout reminders
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

    private void setupAccessibilitySettings() {
        // Large text
        boolean largeText = prefs.getBoolean("large_text", false);
        binding.switchLargeText.setChecked(largeText);
        binding.switchLargeText.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("large_text", isChecked).apply();
            // TODO: Apply large text scaling
            Toast.makeText(requireContext(), 
                isChecked ? "Large text enabled" : "Large text disabled", 
                Toast.LENGTH_SHORT).show();
        });

        // Reduce motion
        boolean reduceMotion = prefs.getBoolean("reduce_motion", false);
        binding.switchReduceMotion.setChecked(reduceMotion);
        binding.switchReduceMotion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("reduce_motion", isChecked).apply();
            // TODO: Apply reduced motion
            Toast.makeText(requireContext(), 
                isChecked ? "Reduced motion enabled" : "Reduced motion disabled", 
                Toast.LENGTH_SHORT).show();
        });
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
            exportData.append("Yoga Assistant - Data Export\n");
            exportData.append("Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n\n");
            
            // Add user stats
            SharedPreferences statsPrefs = requireContext().getSharedPreferences("stats", Context.MODE_PRIVATE);
            exportData.append("User Statistics:\n");
            exportData.append("- Total Workouts: ").append(statsPrefs.getInt("workouts", 0)).append("\n");
            exportData.append("- Current Streak: ").append(statsPrefs.getInt("streak", 0)).append("\n");
            exportData.append("- Total Calories: ").append(statsPrefs.getInt("calories", 0)).append("\n");
            exportData.append("- Last Workout: ").append(statsPrefs.getString("last_workout_date", "Never")).append("\n\n");

            // Save to file
            String fileName = "yoga_assistant_export_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".txt";
            File exportFile = new File(requireContext().getExternalFilesDir(null), fileName);
            
            FileOutputStream fos = new FileOutputStream(exportFile);
            fos.write(exportData.toString().getBytes());
            fos.close();

            // Share file
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(exportFile));
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Yoga Assistant Data Export");
            startActivity(Intent.createChooser(shareIntent, "Share Data Export"));

            Toast.makeText(requireContext(), "Data exported successfully", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Logger.error("Error exporting data", e);
            Toast.makeText(requireContext(), "Error exporting data", Toast.LENGTH_SHORT).show();
        }
    }

    private void backupData() {
        // TODO: Implement cloud backup functionality
        Toast.makeText(requireContext(), "Cloud backup feature coming soon!", Toast.LENGTH_SHORT).show();
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