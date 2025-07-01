package com.rumiznellasery.yogahelper.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.content.SharedPreferences;

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

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        SharedPreferences prefs = requireContext().getSharedPreferences("stats", Context.MODE_PRIVATE);
        int workouts = prefs.getInt("workouts", 0);
        int calories = prefs.getInt("calories", 0);
        int streak = prefs.getInt("streak", 0);

        updateUI(workouts, calories, streak);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DbKeys keys = DbKeys.get(requireContext());
            DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
                    .getReference(keys.users)
                    .child(user.getUid());
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Integer w = snapshot.child(keys.workouts).getValue(Integer.class);
                    Integer c = snapshot.child(keys.calories).getValue(Integer.class);
                    Integer s = snapshot.child(keys.streak).getValue(Integer.class);
                    int newWorkouts = w == null ? workouts : w;
                    int newCalories = c == null ? calories : c;
                    int newStreak = s == null ? streak : s;
                    prefs.edit().putInt("workouts", newWorkouts)
                            .putInt("calories", newCalories)
                            .putInt("streak", newStreak)
                            .apply();
                    updateUI(newWorkouts, newCalories, newStreak);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }

        return root;
    }

    private void updateUI(int workouts, int calories, int streak) {
        binding.textTotalWorkouts.setText("Total workouts: " + workouts);
        binding.progressWorkouts.setMax(100);
        binding.progressWorkouts.setProgress(Math.min(workouts, 100));

        binding.textCalories.setText("Calories burned: " + calories);
        binding.progressCalories.setMax(1000);
        binding.progressCalories.setProgress(Math.min(calories, 1000));

        binding.textStreak.setText("Streak: " + streak + " days");
        binding.progressStreak.setMax(30);
        binding.progressStreak.setProgress(Math.min(streak, 30));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}