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

        // Start Workout button logic
        binding.buttonStartWorkout.setOnClickListener(v -> {
            // Switch to workout tab in bottom navigation
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
                requireActivity().findViewById(R.id.nav_view);
            bottomNav.setSelectedItemId(R.id.navigation_workout);
        });

        // Reset bars button logic
        binding.buttonResetBars.setOnClickListener(v -> {
            prefs.edit()
                .putInt("calories", 0)
                .putInt("workouts_this_week", 0)
                .putInt("workouts_week", 0)
                .apply();

            // Also reset in Firebase
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                DbKeys keys = DbKeys.get(requireContext());
                DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
                        .getReference(keys.users)
                        .child(user.getUid());
                ref.child(keys.calories).setValue(0);
                // Optionally reset workouts and streak in Firebase as well if you want
                // ref.child(keys.workouts).setValue(0);
                // ref.child(keys.streak).setValue(0);
            }

            updateUI(0, prefs.getInt("streak", 0), 0);
        });

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