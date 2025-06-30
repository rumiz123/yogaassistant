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

public class WorkoutFragment extends Fragment {

    private FragmentWorkoutBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        WorkoutViewModel workoutViewModel =
                new ViewModelProvider(this).get(WorkoutViewModel.class);

        binding = FragmentWorkoutBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        View.OnClickListener listener = v -> {
            SharedPreferences prefs = requireContext().getSharedPreferences("stats", Context.MODE_PRIVATE);
            int workouts = prefs.getInt("workouts", 0) + 1;
            int calories = prefs.getInt("calories", 0) + 50;
            prefs.edit().putInt("workouts", workouts).putInt("calories", calories).putInt("streak", workouts).apply();

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                DbKeys keys = DbKeys.get(requireContext());
                DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
                        .getReference(keys.users).child(currentUser.getUid());
                ref.child(keys.workouts).setValue(ServerValue.increment(1));
                ref.child(keys.totalWorkouts).setValue(ServerValue.increment(1));
                ref.child(keys.calories).setValue(ServerValue.increment(50));
                ref.child(keys.streak).setValue(ServerValue.increment(1));
                ref.child(keys.score).setValue(ServerValue.increment(1));
            }

            Intent intent = new Intent(requireContext(), com.rumiznellasery.yogahelper.temp.TempActivity.class);
            startActivity(intent);
        };

        binding.buttonPlaceholder1.setOnClickListener(listener);
        binding.buttonPlaceholder2.setOnClickListener(listener);
        binding.buttonPlaceholder3.setOnClickListener(listener);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
