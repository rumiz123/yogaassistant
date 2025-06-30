package com.rumiznellasery.yogahelper.ui.workout;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.rumiznellasery.yogahelper.databinding.FragmentWorkoutBinding;

public class WorkoutFragment extends Fragment {

    private FragmentWorkoutBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        WorkoutViewModel workoutViewModel =
                new ViewModelProvider(this).get(WorkoutViewModel.class);

        binding = FragmentWorkoutBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        View.OnClickListener listener = v -> {
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
