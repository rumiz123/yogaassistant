package com.rumiznellasery.yogahelper.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.rumiznellasery.yogahelper.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);

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