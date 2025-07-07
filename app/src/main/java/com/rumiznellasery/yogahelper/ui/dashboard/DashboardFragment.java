package com.rumiznellasery.yogahelper.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rumiznellasery.yogahelper.data.DbKeys;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.rumiznellasery.yogahelper.databinding.FragmentDashboardBinding;
import com.rumiznellasery.yogahelper.R;
import android.widget.TextView;
    
public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        SharedPreferences prefs = requireContext().getSharedPreferences("stats", Context.MODE_PRIVATE);
        int streak = prefs.getInt("streak", 0);
        int workoutsThisWeek = getWeeklyWorkouts(prefs);

        updateUI(streak, workoutsThisWeek);
        setupQuickActions();
        // checkAchievements(streak, workoutsThisWeek); // Disabled achievement notifications on launch

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Set welcome message with name if available
            String name = user.getDisplayName();
            if (name != null && !name.isEmpty()) {
                binding.textWelcome.setText("Welcome back, " + name + "!");
            } else {
                binding.textWelcome.setText("Welcome back!");
            }

            DbKeys keys = DbKeys.get(requireContext());
            DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
                    .getReference(keys.users)
                    .child(user.getUid());
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Integer s = snapshot.child(keys.streak).getValue(Integer.class);
                    Integer w = snapshot.child(keys.workouts).getValue(Integer.class);
                    int newStreak = s == null ? streak : s;
                    int newWorkouts = w == null ? workoutsThisWeek : w;
                    prefs.edit().putInt("streak", newStreak)
                            .apply();
                    updateUI(newStreak, getWeeklyWorkouts(prefs));
                    // checkAchievements(newStreak, getWeeklyWorkouts(prefs));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        } else {
            binding.textWelcome.setText("Welcome back!");
        }

        return root;
    }

    private void setupQuickActions() {
        // Start Workout button logic
        binding.buttonStartWorkout.setOnClickListener(v -> {
            // Switch to workout tab in bottom navigation
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
                requireActivity().findViewById(R.id.nav_view);
            bottomNav.setSelectedItemId(R.id.navigation_workout);
        });

        // View Badges button
        binding.buttonViewAchievements.setOnClickListener(v -> {
            // Navigate to badges fragment using overlay container
            if (getActivity() != null) {
                // Show the overlay container
                android.view.View overlayContainer = getActivity().findViewById(R.id.overlay_container);
                if (overlayContainer != null) {
                    overlayContainer.setVisibility(android.view.View.VISIBLE);
                    
                    // Add badges fragment to overlay container
                    getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.overlay_container, new com.rumiznellasery.yogahelper.ui.badges.BadgesFragment())
                        .addToBackStack("badges")
                        .commit();
                }
            }
        });

        // View Friends button
        binding.buttonViewFriends.setOnClickListener(v -> {
            // Switch to friends tab
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
                requireActivity().findViewById(R.id.nav_view);
            bottomNav.setSelectedItemId(R.id.navigation_friends);
        });

        // View Leaderboard button
        binding.buttonViewLeaderboard.setOnClickListener(v -> {
            // Switch to leaderboard tab
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
                requireActivity().findViewById(R.id.nav_view);
            bottomNav.setSelectedItemId(R.id.navigation_leaderboard);
        });



        // Quick Timer button
        binding.buttonQuickTimer.setOnClickListener(v -> {
            showQuickTimerDialog();
        });

        // Pose Guide button
        binding.buttonPoseGuide.setOnClickListener(v -> {
            showPoseGuideDialog();
        });

        // Workout History button
        binding.buttonWorkoutHistory.setOnClickListener(v -> {
            showWorkoutHistoryDialog();
        });

        // Quick Settings button
        binding.buttonQuickSettings.setOnClickListener(v -> {
            showQuickSettingsDialog();
        });
    }

    private void checkBadges(int streak, int workoutsThisWeek) {
        // Initialize badge manager
        com.rumiznellasery.yogahelper.utils.BadgeManager badgeManager = 
            com.rumiznellasery.yogahelper.utils.BadgeManager.getInstance(requireContext());
        
        // Load badges from Firebase
        badgeManager.loadBadgesFromFirebase();
        
        // Check workout badges
        SharedPreferences prefs = requireContext().getSharedPreferences("stats", Context.MODE_PRIVATE);
        int totalWorkouts = prefs.getInt("workouts", 0);
        badgeManager.checkWorkoutBadges(totalWorkouts);
        
        // Check streak badges
        badgeManager.checkStreakBadges(streak);
        
        // TODO: Check other badge types as they become available
    }

    private void showQuickTimerDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("⏱️ Quick Timer");
        
        android.view.View dialogView = android.view.LayoutInflater.from(requireContext()).inflate(android.R.layout.simple_list_item_1, null);
        android.widget.ListView listView = new android.widget.ListView(requireContext());
        
        String[] timerOptions = {"5 minutes", "10 minutes", "15 minutes", "20 minutes", "30 minutes"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, timerOptions);
        listView.setAdapter(adapter);
        
        builder.setView(listView);
        builder.setNegativeButton("Cancel", null);
        
        android.app.AlertDialog dialog = builder.create();
        
        listView.setOnItemClickListener((parent, view, position, id) -> {
            int minutes = (position + 1) * 5;
            startQuickTimer(minutes);
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void startQuickTimer(int minutes) {
        android.widget.Toast.makeText(requireContext(), "Timer started for " + minutes + " minutes", android.widget.Toast.LENGTH_SHORT).show();
        // TODO: Implement actual timer functionality
    }
    
    private void showPoseGuideDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("🧘 Pose Guide");
        
        String[] poses = {
            "Mountain Pose (Tadasana)",
            "Cobra Pose (Bhujangasana)", 
            "Tree Pose (Vrikshasana)",
            "Downward Dog (Adho Mukha Svanasana)",
            "Child's Pose (Balasana)"
        };
        
        builder.setItems(poses, (dialog, which) -> {
            showPoseInstructions(poses[which]);
        });
        
        builder.setNegativeButton("Close", null);
        builder.show();
    }
    
    private void showPoseInstructions(String poseName) {
        String instructions = getPoseInstructions(poseName);
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle(poseName);
        builder.setMessage(instructions);
        builder.setPositiveButton("Practice Now", (dialog, which) -> {
            // TODO: Launch pose practice mode
            android.widget.Toast.makeText(requireContext(), "Launching " + poseName + " practice", android.widget.Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }
    
    private String getPoseInstructions(String poseName) {
        switch (poseName) {
            case "Mountain Pose (Tadasana)":
                return "Stand with feet together, arms at sides. Take deep breaths and feel grounded. This is your foundation.";
            case "Cobra Pose (Bhujangasana)":
                return "Lie on your stomach with legs extended. Place your hands under your shoulders. Gently lift your chest off the ground, keeping your pelvis on the floor.";
            case "Tree Pose (Vrikshasana)":
                return "Stand with feet hip-width apart. Shift weight to your left foot. Place your right foot on your left thigh or calf. Bring your hands to prayer position.";
            case "Downward Dog (Adho Mukha Svanasana)":
                return "Start on hands and knees. Lift your hips up and back, forming an inverted V shape. Keep your arms and legs straight.";
            case "Child's Pose (Balasana)":
                return "Kneel on the floor, sit back on your heels. Fold forward, extending your arms in front of you. Rest your forehead on the floor.";
            default:
                return "Instructions not available for this pose.";
        }
    }
    
    private void showWorkoutHistoryDialog() {
        SharedPreferences statsPrefs = requireContext().getSharedPreferences("stats", Context.MODE_PRIVATE);
        int totalWorkouts = statsPrefs.getInt("workouts", 0);
        String lastWorkout = statsPrefs.getString("last_workout_date", "Never");
        int currentStreak = statsPrefs.getInt("streak", 0);
        int longestStreak = statsPrefs.getInt("longest_streak", 0);
        int workoutsThisWeek = getWeeklyWorkouts(statsPrefs);
        
        String historyText = "📊 Workout History\n\n" +
            "Total Workouts: " + totalWorkouts + "\n" +
            "Current Streak: " + currentStreak + " days\n" +
            "Longest Streak: " + longestStreak + " days\n" +
            "This Week: " + workoutsThisWeek + " workouts\n" +
            "Last Workout: " + lastWorkout + "\n\n";
        
        // Add motivational message based on stats
        if (currentStreak > 0) {
            historyText += "🔥 Keep your streak alive! 🔥\n";
        } else if (totalWorkouts > 0) {
            historyText += "💪 Ready to start a new streak? 💪\n";
        } else {
            historyText += "🚀 Time to begin your yoga journey! 🚀\n";
        }
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("📈 Workout History");
        builder.setMessage(historyText);
        builder.setPositiveButton("Start New Workout", (dialog, which) -> {
            // Launch workout
            android.content.Intent intent = new android.content.Intent(requireContext(), com.rumiznellasery.yogahelper.camera.PoseInstructionsActivity.class);
            startActivity(intent);
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }
    
    private void showQuickSettingsDialog() {
        SharedPreferences prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("⚙️ Quick Settings");
        
        android.view.View dialogView = android.view.LayoutInflater.from(requireContext()).inflate(android.R.layout.simple_list_item_1, null);
        android.widget.ListView listView = new android.widget.ListView(requireContext());
        
        String[] settingsOptions = {
            "🔔 Workout Reminders: " + (prefs.getBoolean("workout_reminders", false) ? "ON" : "OFF"),
            "🌙 Dark Mode: " + (prefs.getBoolean("dark_mode", false) ? "ON" : "OFF"),
            "📱 Large Text: " + (prefs.getBoolean("large_text", false) ? "ON" : "OFF"),
            "🎯 Badge Notifications: " + (prefs.getBoolean("badge_notifications", true) ? "ON" : "OFF"),
            "👥 Friend Activity: " + (prefs.getBoolean("friend_activity", true) ? "ON" : "OFF")
        };
        
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, settingsOptions);
        listView.setAdapter(adapter);
        
        builder.setView(listView);
        builder.setNegativeButton("Close", null);
        builder.setPositiveButton("Full Settings", (dialog, which) -> {
            // Switch to settings tab
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
                requireActivity().findViewById(R.id.nav_view);
            bottomNav.setSelectedItemId(R.id.navigation_home);
        });
        
        android.app.AlertDialog dialog = builder.create();
        
        listView.setOnItemClickListener((parent, view, position, id) -> {
            toggleQuickSetting(position, prefs);
            dialog.dismiss();
            showQuickSettingsDialog(); // Refresh the dialog
        });
        
        dialog.show();
    }
    
    private void toggleQuickSetting(int position, SharedPreferences prefs) {
        switch (position) {
            case 0: // Workout Reminders
                boolean reminders = !prefs.getBoolean("workout_reminders", false);
                prefs.edit().putBoolean("workout_reminders", reminders).apply();
                android.widget.Toast.makeText(requireContext(), 
                    "Workout reminders " + (reminders ? "enabled" : "disabled"), 
                    android.widget.Toast.LENGTH_SHORT).show();
                break;
            case 1: // Dark Mode
                boolean darkMode = !prefs.getBoolean("dark_mode", false);
                prefs.edit().putBoolean("dark_mode", darkMode).apply();
                android.widget.Toast.makeText(requireContext(), 
                    "Dark mode " + (darkMode ? "enabled" : "disabled"), 
                    android.widget.Toast.LENGTH_SHORT).show();
                break;
            case 2: // Large Text
                boolean largeText = !prefs.getBoolean("large_text", false);
                prefs.edit().putBoolean("large_text", largeText).apply();
                android.widget.Toast.makeText(requireContext(), 
                    "Large text " + (largeText ? "enabled" : "disabled"), 
                    android.widget.Toast.LENGTH_SHORT).show();
                break;
            case 3: // Badge Notifications
                boolean badgeNotifs = !prefs.getBoolean("badge_notifications", true);
                prefs.edit().putBoolean("badge_notifications", badgeNotifs).apply();
                android.widget.Toast.makeText(requireContext(), 
                    "Badge notifications " + (badgeNotifs ? "enabled" : "disabled"), 
                    android.widget.Toast.LENGTH_SHORT).show();
                break;
            case 4: // Friend Activity
                boolean friendActivity = !prefs.getBoolean("friend_activity", true);
                prefs.edit().putBoolean("friend_activity", friendActivity).apply();
                android.widget.Toast.makeText(requireContext(), 
                    "Friend activity " + (friendActivity ? "enabled" : "disabled"), 
                    android.widget.Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void updateUI(int streak, int workoutsThisWeek) {
        // Streak number
        binding.textStreakNumber.setText(String.valueOf(streak));

        // Workouts this week bar (max 10)
        binding.progressWorkouts.setMax(10);
        int progress = Math.min(workoutsThisWeek, 10);
        binding.progressWorkouts.setProgressCompat(progress, true);

        // Show workouts done count
        binding.textWorkoutsCount.setText(workoutsThisWeek + "/10 workouts");
        
        // Update motivational message based on progress
        updateMotivationalMessage(streak, workoutsThisWeek);
    }
    
    private void updateMotivationalMessage(int streak, int workoutsThisWeek) {
        String title;
        String message;
        
        if (streak >= 7) {
            title = "🔥 Amazing Streak! 🔥";
            message = "You're on fire! " + streak + " days strong. Keep this incredible momentum going!";
        } else if (streak >= 3) {
            title = "💪 Great Progress! 💪";
            message = "You're building a solid foundation with " + streak + " days in a row. Consistency is key!";
        } else if (workoutsThisWeek >= 5) {
            title = "⭐ Weekly Warrior! ⭐";
            message = "You've completed " + workoutsThisWeek + " workouts this week. You're crushing it!";
        } else if (workoutsThisWeek >= 3) {
            title = "🌱 Growing Strong! 🌱";
            message = "You've done " + workoutsThisWeek + " workouts this week. Every session counts!";
        } else if (workoutsThisWeek == 0) {
            title = "🚀 Ready to Start? 🚀";
            message = "Today is the perfect day to begin your yoga journey. Let's get started!";
        } else {
            title = "🌟 Keep Going! 🌟";
            message = "Every workout brings you closer to your goals. Stay consistent and watch your progress grow!";
        }
        
        // Add streak reminder if streak is at risk
        if (streak > 0 && workoutsThisWeek == 0) {
            message += "\n\n⚠️ Don't break your " + streak + "-day streak! Do a quick workout today.";
        }
        
        binding.textMotivationTitle.setText(title);
        binding.textMotivationMessage.setText(message);
    }

    // Helper to get weekly workouts, resets if week changes
    private int getWeeklyWorkouts(SharedPreferences prefs) {
        int savedWeek = prefs.getInt("workouts_week", -1);
        int workouts = prefs.getInt("workouts_this_week", 0);
        int currentWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
        if (savedWeek != currentWeek) {
            // New week, reset
            prefs.edit().putInt("workouts_week", currentWeek).putInt("workouts_this_week", 0).apply();
            return 0;
        }
        return workouts;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}