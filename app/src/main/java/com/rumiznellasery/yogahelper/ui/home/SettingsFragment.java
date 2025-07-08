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
import com.rumiznellasery.yogahelper.utils.BadgeManager;
import com.rumiznellasery.yogahelper.ui.badges.BadgesShowcaseAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rumiznellasery.yogahelper.utils.SecretMode;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private SharedPreferences prefs;
    private BadgeManager badgeManager;
    private BadgesShowcaseAdapter showcaseAdapter;
    private boolean badgesAreUnlocked = false;

    // Adapter for profile badge row
    private class ProfileBadgesAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<ProfileBadgesAdapter.BadgeViewHolder> {
        private final java.util.List<com.rumiznellasery.yogahelper.data.Badge> badges;
        ProfileBadgesAdapter(java.util.List<com.rumiznellasery.yogahelper.data.Badge> badges) {
            this.badges = badges;
        }
        @NonNull
        @Override
        public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge_profile_icon, parent, false);
            return new BadgeViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
            holder.bind(badges.get(position));
        }
        @Override
        public int getItemCount() { return badges.size(); }
        class BadgeViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            private final android.widget.ImageView imageBadge;
            BadgeViewHolder(@NonNull android.view.View itemView) {
                super(itemView);
                imageBadge = itemView.findViewById(R.id.image_badge_profile);
            }
            void bind(com.rumiznellasery.yogahelper.data.Badge badge) {
                // Set icon based on badge ID for more specific mapping
                int iconRes = getBadgeIconById(badge.id);
                imageBadge.setImageResource(iconRes);
                imageBadge.setAlpha(1.0f);
            }
            int getBadgeIconById(String badgeId) {
                if (badgeId == null) return R.drawable.ic_badge_first_workout;
                
                switch (badgeId) {
                    case "first_workout":
                        return R.drawable.ic_badge_first_workout;
                    case "week_streak":
                        return R.drawable.ic_badge_week_warrior;
                    case "month_streak":
                        return R.drawable.ic_badge_monthly_master;
                    case "hundred_workouts":
                        return R.drawable.ic_badge_century_club;
                    case "social_butterfly":
                        return R.drawable.ic_badge_social_butterfly;
                    case "competition_winner":
                        return R.drawable.ic_badge_champion;
                    case "pose_master":
                        return R.drawable.ic_badge_pose_master;
                    case "time_master":
                        return R.drawable.ic_badge_time_master;
                    case "perfect_week":
                        return R.drawable.ic_badge_perfect_week;
                    default:
                        return R.drawable.ic_badge_first_workout;
                }
            }
            int getBadgeIcon(com.rumiznellasery.yogahelper.data.Badge.BadgeType type) {
                if (type == null) return R.drawable.ic_badge_first_workout;
                switch (type) {
                    case WORKOUT_COUNT: return R.drawable.ic_badge_first_workout;
                    case STREAK_DAYS: return R.drawable.ic_badge_week_warrior;
                    case CALORIES_BURNED: return R.drawable.ic_badge_week_warrior;
                    case FRIENDS_COUNT: return R.drawable.ic_badge_social_butterfly;
                    case COMPETITION_WINS: return R.drawable.ic_badge_champion;
                    case PERFECT_WEEK: return R.drawable.ic_badge_perfect_week;
                    case POSE_MASTERY: return R.drawable.ic_badge_pose_master;
                    case WORKOUT_TIME: return R.drawable.ic_badge_time_master;
                    case CHALLENGE_COMPLETION: return R.drawable.ic_badge_champion;
                    default: return R.drawable.ic_badge_first_workout;
                }
            }
        }
    }

    private void setupProfileBadgesRow() {
        java.util.List<com.rumiznellasery.yogahelper.data.Badge> unlocked = badgeManager.getUnlockedBadges();
        if (unlocked.isEmpty()) {
            binding.recyclerProfileBadges.setVisibility(android.view.View.GONE);
            return;
        }
        binding.recyclerProfileBadges.setVisibility(android.view.View.VISIBLE);
        java.util.List<com.rumiznellasery.yogahelper.data.Badge> toShow = unlocked.size() > 5 ? unlocked.subList(0, 5) : unlocked;
        ProfileBadgesAdapter adapter = new ProfileBadgesAdapter(toShow);
        binding.recyclerProfileBadges.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerProfileBadges.setAdapter(adapter);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        badgeManager = com.rumiznellasery.yogahelper.utils.BadgeManager.getInstance(requireContext());
        
        setupProfilePicture();
        setupThemeSettings();
        setupNotificationSettings();
        setupAccessibilitySettings();
        setupPrivacyDataSettings();
        setupBadgesShowcase();
        setupProfileBadgesRow();
        setupDeveloperMode();
        setupLogout();
        setupSecretMode();
        setupThemeSelector();
        
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (badgeManager == null) {
            badgeManager = com.rumiznellasery.yogahelper.utils.BadgeManager.getInstance(requireContext());
        }
        setupProfileBadgesRow();
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

        // Badge notifications
        boolean badgeNotifications = prefs.getBoolean("badge_notifications", true);
        binding.switchAchievementNotifications.setChecked(badgeNotifications);
        binding.switchAchievementNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("badge_notifications", isChecked).apply();
            Toast.makeText(requireContext(), 
                isChecked ? "Badge notifications enabled" : "Badge notifications disabled", 
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
        // Data backup
        binding.layoutDataBackup.setOnClickListener(v -> backupData());

        // Delete account
        binding.layoutDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void setupBadgesShowcase() {
        badgeManager = BadgeManager.getInstance(requireContext());
        
        // Force badge initialization and loading
        badgeManager.loadBadgesFromLocal();
        badgeManager.loadBadgesFromFirebase();
        
        // Debug: Show badge count
        java.util.List<com.rumiznellasery.yogahelper.data.Badge> debugBadges = badgeManager.getBadges();
        Toast.makeText(requireContext(), "Badges loaded: " + debugBadges.size(), Toast.LENGTH_LONG).show();

        // Setup RecyclerView
        showcaseAdapter = new BadgesShowcaseAdapter();
        binding.recyclerBadgesShowcase.setLayoutManager(
            new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 4)
        );
        binding.recyclerBadgesShowcase.setAdapter(showcaseAdapter);
        
        // Load badges
        updateBadgesShowcase();
        
        // Ensure test button is enabled and visible
        binding.buttonTestBadges.setEnabled(true);
        binding.buttonTestBadges.setVisibility(View.VISIBLE);
        binding.buttonTestBadges.setAlpha(1.0f);
        binding.buttonTestBadges.setClickable(true);
        
        // Setup Test Badges button
        binding.buttonTestBadges.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Test Badges button pressed", Toast.LENGTH_SHORT).show();
            java.util.List<com.rumiznellasery.yogahelper.data.Badge> badges = badgeManager.getBadges();
            if (!badgesAreUnlocked) {
                // Unlock all badges
                for (com.rumiznellasery.yogahelper.data.Badge badge : badges) {
                    badge.unlocked = true;
                    badge.currentProgress = badge.requirement;
                    badge.unlockedDate = System.currentTimeMillis();
                    badgeManager.saveBadgeLocally(badge);
                }
                badgesAreUnlocked = true;
                Toast.makeText(requireContext(), "All badges unlocked!", Toast.LENGTH_SHORT).show();
            } else {
                // Reset all badges
                for (com.rumiznellasery.yogahelper.data.Badge badge : badges) {
                    badge.unlocked = false;
                    badge.currentProgress = 0;
                    badge.unlockedDate = 0;
                    badgeManager.saveBadgeLocally(badge);
                }
                badgesAreUnlocked = false;
                Toast.makeText(requireContext(), "All badges reset!", Toast.LENGTH_SHORT).show();
            }
            updateBadgesShowcase();
        });

        // Setup "View All Badges" button
        binding.buttonViewAllBadges.setOnClickListener(v -> {
            // Navigate to badges fragment
            if (getActivity() != null) {
                android.view.View overlayContainer = getActivity().findViewById(R.id.overlay_container);
                if (overlayContainer != null) {
                    overlayContainer.setVisibility(android.view.View.VISIBLE);
                    getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.overlay_container, new com.rumiznellasery.yogahelper.ui.badges.BadgesFragment())
                        .addToBackStack("badges")
                        .commit();
                }
            }
        });
    }

    private void loadBadgesShowcase() {
        // Load from local storage first
        badgeManager.loadBadgesFromLocal();
        
        // Then load from Firebase
        badgeManager.loadBadgesFromFirebase();
        
        // Update showcase
        updateBadgesShowcase();
    }

    private void updateBadgesShowcase() {
        java.util.List<com.rumiznellasery.yogahelper.data.Badge> badges = badgeManager.getBadges();
        
        // Show only first 8 badges in showcase
        java.util.List<com.rumiznellasery.yogahelper.data.Badge> showcaseBadges = badges.size() > 8 
            ? badges.subList(0, 8) 
            : badges;
        
        showcaseAdapter.setBadges(showcaseBadges);
        
        // Update count
        int unlockedCount = badgeManager.getUnlockedCount();
        int totalCount = badgeManager.getTotalCount();
        binding.textBadgesCount.setText(unlockedCount + "/" + totalCount);
    }

    private void backupData() {
        // TODO: Implement cloud backup functionality
        Toast.makeText(requireContext(), "Cloud backup feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void showDeleteAccountDialog() {
        new MaterialAlertDialogBuilder(requireContext())
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
            new MaterialAlertDialogBuilder(requireContext())
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
                    InputStream in = requireContext().getContentResolver().openInputStream(selectedImage);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    in.close();
                    
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

    private void setupSecretMode() {
        boolean isSecretUnlocked = SecretMode.isSecretMode(requireContext());
        if (isSecretUnlocked) {
            binding.layoutSecretMode.setVisibility(View.VISIBLE);
            binding.switchSecretMode.setChecked(true);
            binding.switchSecretMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SecretMode.setSecretMode(requireContext(), isChecked);
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // Ensure not night
                    requireActivity().setTheme(R.style.Theme_YogaHelper_Secret);
                    Toast.makeText(requireContext(), "✨ Secret Mode Activated! Enjoy the magic! ✨", Toast.LENGTH_LONG).show();
                    // Simple color flash as placeholder for confetti
                    binding.getRoot().setBackgroundColor(getResources().getColor(R.color.secret_bg));
                    binding.getRoot().postDelayed(() -> binding.getRoot().setBackgroundColor(getResources().getColor(R.color.theme_dark)), 800);
                } else {
                    requireActivity().setTheme(R.style.Theme_YogaHelper);
                    Toast.makeText(requireContext(), "Secret Mode deactivated.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            binding.layoutSecretMode.setVisibility(View.GONE);
        }
    }

    private void setupThemeSelector() {
        Spinner spinner = binding.spinnerThemeSelector;
        List<String> themeNames = new ArrayList<>();
        List<String> themeKeys = new ArrayList<>();
        themeNames.add("Default");
        themeKeys.add("default");
        if (BadgeManager.isThemeUnlocked(requireContext(), "theme_ocean")) {
            themeNames.add("Ocean");
            themeKeys.add("ocean");
        }
        if (BadgeManager.isThemeUnlocked(requireContext(), "theme_forest")) {
            themeNames.add("Forest");
            themeKeys.add("forest");
        }
        if (SecretMode.isSecretMode(requireContext())) {
            themeNames.add("Secret");
            themeKeys.add("secret");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, themeNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        // Load saved theme
        String savedTheme = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE).getString("selected_theme", "default");
        int selectedIndex = themeKeys.indexOf(savedTheme);
        if (selectedIndex >= 0) spinner.setSelection(selectedIndex);
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String key = themeKeys.get(position);
                int styleId = R.style.Theme_YogaHelper;
                switch (key) {
                    case "ocean": styleId = R.style.Theme_YogaHelper_Ocean; break;
                    case "forest": styleId = R.style.Theme_YogaHelper_Forest; break;
                    case "secret": styleId = R.style.Theme_YogaHelper_Secret; break;
                }
                requireActivity().setTheme(styleId);
                requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("selected_theme", key).apply();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 