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
        int calories = prefs.getInt("calories", 0);
        int streak = prefs.getInt("streak", 0);
        int workoutsThisWeek = getWeeklyWorkouts(prefs);

        updateUI(calories, streak, workoutsThisWeek);
        setupQuickActions();
        checkAchievements(calories, streak, workoutsThisWeek);

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
                    Integer c = snapshot.child(keys.calories).getValue(Integer.class);
                    Integer s = snapshot.child(keys.streak).getValue(Integer.class);
                    Integer w = snapshot.child(keys.workouts).getValue(Integer.class);
                    int newCalories = c == null ? calories : c;
                    int newStreak = s == null ? streak : s;
                    int newWorkouts = w == null ? workoutsThisWeek : w;
                    prefs.edit().putInt("calories", newCalories)
                            .putInt("streak", newStreak)
                            .apply();
                    updateUI(newCalories, newStreak, getWeeklyWorkouts(prefs));
                    checkAchievements(newCalories, newStreak, getWeeklyWorkouts(prefs));
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

        // View Achievements button
        binding.buttonViewAchievements.setOnClickListener(v -> {
            // Navigate to achievements fragment
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment_activity_main, new com.rumiznellasery.yogahelper.ui.achievements.AchievementsFragment())
                    .addToBackStack(null)
                    .commit();
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



        // Export Data button
        binding.buttonExportData.setOnClickListener(v -> {
            exportUserData();
        });
    }

    private void checkAchievements(int calories, int streak, int workoutsThisWeek) {
        // Initialize achievement manager
        com.rumiznellasery.yogahelper.utils.AchievementManager achievementManager = 
            com.rumiznellasery.yogahelper.utils.AchievementManager.getInstance(requireContext());
        
        // Load achievements from Firebase
        achievementManager.loadAchievementsFromFirebase();
        
        // Check workout achievements
        SharedPreferences prefs = requireContext().getSharedPreferences("stats", Context.MODE_PRIVATE);
        int totalWorkouts = prefs.getInt("workouts", 0);
        achievementManager.checkWorkoutAchievements(totalWorkouts);
        
        // Check streak achievements
        achievementManager.checkStreakAchievements(streak);
        
        // TODO: Check other achievement types as they become available
    }

    private void exportUserData() {
        try {
            // Create data export
            StringBuilder exportData = new StringBuilder();
            exportData.append("Yoga Assistant - Data Export\n");
            exportData.append("Generated: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date())).append("\n\n");
            
            // Add user stats
            SharedPreferences statsPrefs = requireContext().getSharedPreferences("stats", Context.MODE_PRIVATE);
            exportData.append("User Statistics:\n");
            exportData.append("- Total Workouts: ").append(statsPrefs.getInt("workouts", 0)).append("\n");
            exportData.append("- Current Streak: ").append(statsPrefs.getInt("streak", 0)).append("\n");
            exportData.append("- Total Calories: ").append(statsPrefs.getInt("calories", 0)).append("\n");
            exportData.append("- Last Workout: ").append(statsPrefs.getString("last_workout_date", "Never")).append("\n\n");

            // Add achievements
            com.rumiznellasery.yogahelper.utils.AchievementManager achievementManager = 
                com.rumiznellasery.yogahelper.utils.AchievementManager.getInstance(requireContext());
            exportData.append("Achievements:\n");
            exportData.append("- Unlocked: ").append(achievementManager.getUnlockedCount()).append("/").append(achievementManager.getTotalCount()).append("\n");
            exportData.append("- Completion: ").append(String.format("%.0f%%", achievementManager.getCompletionPercentage())).append("\n\n");

            // Save to file
            String fileName = "yoga_assistant_export_" + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date()) + ".txt";
            java.io.File exportFile = new java.io.File(requireContext().getExternalFilesDir(null), fileName);
            
            java.io.FileOutputStream fos = new java.io.FileOutputStream(exportFile);
            fos.write(exportData.toString().getBytes());
            fos.close();

            // Share file
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri.fromFile(exportFile));
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Yoga Assistant Data Export");
            startActivity(android.content.Intent.createChooser(shareIntent, "Share Data Export"));

            android.widget.Toast.makeText(requireContext(), "Data exported successfully", android.widget.Toast.LENGTH_SHORT).show();
        } catch (java.io.IOException e) {
            com.rumiznellasery.yogahelper.utils.Logger.error("Error exporting data", e);
            android.widget.Toast.makeText(requireContext(), "Error exporting data", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI(int calories, int streak, int workoutsThisWeek) {
        // Calories circular progress (max 500)
        binding.circleCalories.setMax(500);
        binding.circleCalories.setProgressCompat(Math.min(calories, 500), true);
        binding.textCaloriesCenter.setText(calories + " calories");

        // Streak number
        binding.textStreakNumber.setText(String.valueOf(streak));

        // Workouts this week bar (max 10)
        binding.progressWorkouts.setMax(10);
        int progress = Math.min(workoutsThisWeek, 10);
        binding.progressWorkouts.setProgressCompat(progress, true);

        // Show workouts done count
        binding.textWorkoutsCount.setText(workoutsThisWeek + "/10 workouts");
        
        // Update motivational message based on progress
        updateMotivationalMessage(calories, streak, workoutsThisWeek);
    }
    
    private void updateMotivationalMessage(int calories, int streak, int workoutsThisWeek) {
        String title;
        String message;
        
        if (streak >= 7) {
            title = "🔥 Amazing Streak! 🔥";
            message = "You're on fire! Keep this incredible momentum going. You're building healthy habits that will last a lifetime!";
        } else if (streak >= 3) {
            title = "🌟 Great Progress! 🌟";
            message = "You're building a solid foundation! Consistency is key - you're doing fantastic!";
        } else if (workoutsThisWeek >= 5) {
            title = "💪 Strong Week! 💪";
            message = "You're crushing your weekly goals! Keep up this amazing energy!";
        } else if (calories >= 300) {
            title = "🔥 Burning Bright! 🔥";
            message = "Look at those calories burn! Your body is thanking you for this amazing workout!";
        } else {
            title = "🌟 Keep Going! 🌟";
            message = "Every workout brings you closer to your goals. Stay consistent and watch your progress grow!";
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