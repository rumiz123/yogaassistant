package com.rumiznellasery.yogahelper.ui.workout;

import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

public class WorkoutFragment extends Fragment {

    private FragmentWorkoutBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        WorkoutViewModel workoutViewModel =
                new ViewModelProvider(this).get(WorkoutViewModel.class);

        binding = FragmentWorkoutBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        View.OnClickListener startWorkoutListener = v -> {
            SharedPreferences prefs = requireContext().getSharedPreferences("stats", Context.MODE_PRIVATE);
            int workouts = prefs.getInt("workouts", 0) + 1;
            int calories = prefs.getInt("calories", 0) + 50;

            // --- Streak logic ---
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String today = sdf.format(new Date());
            String lastWorkoutDate = prefs.getString("last_workout_date", "");
            int streak = prefs.getInt("streak", 0);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            String yesterday = sdf.format(cal.getTime());
            boolean streakIncreased = false;
            if (lastWorkoutDate.equals(today)) {
                // Already logged today, do not increment streak
            } else if (lastWorkoutDate.equals(yesterday)) {
                // Consecutive day, increment streak
                streak += 1;
                streakIncreased = true;
            } else {
                // Missed a day or first workout, reset streak
                streak = 1;
                streakIncreased = true;
            }
            // Save new stats
            prefs.edit()
                .putInt("workouts", workouts)
                .putInt("calories", calories)
                .putInt("streak", streak)
                .putString("last_workout_date", today)
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

            Intent intent = new Intent(requireContext(), com.rumiznellasery.yogahelper.camera.CameraActivity.class);
            startActivity(intent);
        };

        binding.buttonPlaceholder1.setOnClickListener(startWorkoutListener);
        binding.buttonPlaceholder2.setOnClickListener(startWorkoutListener);
        binding.buttonPlaceholder3.setOnClickListener(startWorkoutListener);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
