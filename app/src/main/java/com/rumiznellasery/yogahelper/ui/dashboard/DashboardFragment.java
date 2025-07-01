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

import com.rumiznellasery.yogahelper.databinding.FragmentDashboardBinding;

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
        binding.textCaloriesCenter.setText(calories + "\ncalories");

        // Streak number
        binding.textStreakNumber.setText(String.valueOf(streak));
        // Fire icon is static

        // Workouts this week bar (max 10)
        binding.progressWorkouts.setMax(10);
        int progress = Math.min(workoutsThisWeek, 10);
        binding.progressWorkouts.setProgressCompat(progress, true);

        // Show workouts done count
        binding.textWorkoutsCount.setText(workoutsThisWeek + "/10 workouts");
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