package com.rumiznellasery.yogahelper.ui.workout;

import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.rumiznellasery.yogahelper.databinding.FragmentWorkoutBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.rumiznellasery.yogahelper.data.DbKeys;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;
import com.rumiznellasery.yogahelper.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class WorkoutFragment extends Fragment {

    private FragmentWorkoutBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        WorkoutViewModel workoutViewModel =
                new ViewModelProvider(this).get(WorkoutViewModel.class);

        binding = FragmentWorkoutBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Hide/show bottom nav bar on scroll removed to keep navigation visible

        View.OnClickListener startWorkoutListener = v -> {
            SharedPreferences prefs = requireContext().getSharedPreferences("stats", Context.MODE_PRIVATE);
            int workouts = prefs.getInt("workouts", 0) + 1;
            int calories = prefs.getInt("calories", 0) + 50;

            // --- Robust Streak logic (UTC) ---
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            String todayStr = today.format(formatter);
            String lastWorkoutDateStr = prefs.getString("last_workout_date", "");
            int streak = prefs.getInt("streak", 0);
            boolean streakIncreased = false;
            if (lastWorkoutDateStr.equals(todayStr)) {
                // Already logged today, do not increment streak
            } else {
                LocalDate lastWorkoutDate = null;
                try {
                    lastWorkoutDate = LocalDate.parse(lastWorkoutDateStr, formatter);
                } catch (Exception ignored) {}
                if (lastWorkoutDate != null) {
                    long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastWorkoutDate, today);
                    if (daysBetween == 1) {
                        // Consecutive day, increment streak
                        streak += 1;
                        streakIncreased = true;
                    } else {
                        // Missed one or more days, reset streak
                        streak = 1;
                        streakIncreased = true;
                    }
                } else {
                    // First workout ever
                    streak = 1;
                    streakIncreased = true;
                }
            }
            // Save new stats
            prefs.edit()
                .putInt("workouts", workouts)
                .putInt("calories", calories)
                .putInt("streak", streak)
                .putString("last_workout_date", todayStr)
                .apply();

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                DbKeys keys = DbKeys.get(requireContext());
                DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
                        .getReference(keys.users).child(currentUser.getUid());
                ref.child(keys.workouts).setValue(ServerValue.increment(1));
                ref.child(keys.totalWorkouts).setValue(ServerValue.increment(1));
                ref.child(keys.calories).setValue(ServerValue.increment(50));
                if (streakIncreased) {
                    ref.child(keys.streak).setValue(streak);
                }
                ref.child(keys.score).setValue(ServerValue.increment(1));
            }

            // --- Weekly workouts logic ---
            Calendar calendar = Calendar.getInstance();
            int currentWeek = calendar.get(Calendar.WEEK_OF_YEAR);
            int savedWeek = prefs.getInt("workouts_week", -1);
            int workoutsThisWeek = prefs.getInt("workouts_this_week", 0);
            if (savedWeek != currentWeek) {
                // New week, reset
                workoutsThisWeek = 1;
                prefs.edit().putInt("workouts_week", currentWeek).putInt("workouts_this_week", 1).apply();
            } else {
                workoutsThisWeek += 1;
                prefs.edit().putInt("workouts_this_week", workoutsThisWeek).apply();
            }

            Intent intent = new Intent(requireContext(), com.rumiznellasery.yogahelper.camera.PoseInstructionsActivity.class);
            startActivity(intent);
        };

        binding.buttonPlaceholder1.setOnClickListener(startWorkoutListener);
        binding.buttonPlaceholder2.setOnClickListener(startWorkoutListener);
        binding.buttonPlaceholder3.setOnClickListener(startWorkoutListener);

        // Fetch yogaLevel from Firebase and highlight recommended workout
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("users").child(currentUser.getUid()).child("yogaLevel");
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String yogaLevel = snapshot.getValue(String.class);
                    int recommended = 1; // default to 1 if not found
                    if (yogaLevel != null) {
                        switch (yogaLevel) {
                            case "Brand New":
                            case "Beginner":
                                recommended = 1;
                                break;
                            case "Average":
                                recommended = 2;
                                break;
                            case "Expert":
                                recommended = 3;
                                break;
                            case "Professional":
                                recommended = 4;
                                break;
                        }
                    }
                    highlightRecommendedWorkout(recommended);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        return root;
    }

    private void highlightRecommendedWorkout(int recommended) {
        int[] cardIds = {R.id.card_workout1, R.id.card_workout2, R.id.card_workout3};
        int[] labelIds = {R.id.label_recommended1, R.id.label_recommended2, R.id.label_recommended3};
        for (int i = 0; i < cardIds.length; i++) {
            MaterialCardView card = getView().findViewById(cardIds[i]);
            TextView label = getView().findViewById(labelIds[i]);
            if (card != null) {
                if (i == recommended - 1) {
                    card.setStrokeWidth(8);
                    card.setStrokeColor(getResources().getColor(R.color.teal_200));
                    if (label != null) label.setVisibility(View.VISIBLE);
                } else {
                    card.setStrokeWidth(0);
                    if (label != null) label.setVisibility(View.GONE);
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
