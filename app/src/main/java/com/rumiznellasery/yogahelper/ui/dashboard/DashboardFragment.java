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



        // Export Data button
        binding.buttonExportData.setOnClickListener(v -> {
            exportUserData();
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
            exportData.append("- Last Workout: ").append(statsPrefs.getString("last_workout_date", "Never")).append("\n\n");

            // Add badges
            com.rumiznellasery.yogahelper.utils.BadgeManager badgeManager = 
                com.rumiznellasery.yogahelper.utils.BadgeManager.getInstance(requireContext());
            exportData.append("Badges:\n");
            exportData.append("- Unlocked: ").append(badgeManager.getUnlockedCount()).append("/").append(badgeManager.getTotalCount()).append("\n");
            exportData.append("- Completion: ").append(String.format("%.0f%%", badgeManager.getCompletionPercentage())).append("\n\n");

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
            message = "You're on fire! Keep this incredible momentum going. You're building healthy habits that will last a lifetime!";
        } else if (streak >= 3) {
            title = "🌟 Great Progress! 🌟";
            message = "You're building a solid foundation! Consistency is key - you're doing fantastic!";
        } else if (workoutsThisWeek >= 5) {
            title = "💪 Strong Week! 💪";
            message = "You're crushing your weekly goals! Keep up this amazing energy!";
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