package com.rumiznellasery.yogahelper.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.content.SharedPreferences;

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

        binding.textTotalWorkouts.setText("Total workouts: " + workouts);
        binding.textCalories.setText("Calories burned: " + calories);
        binding.textStreak.setText("Streak: " + streak + " days");

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}