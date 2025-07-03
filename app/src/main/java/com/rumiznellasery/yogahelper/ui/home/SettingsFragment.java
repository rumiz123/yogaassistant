package com.rumiznellasery.yogahelper.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.rumiznellasery.yogahelper.R;

public class SettingsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        ImageView backButton = view.findViewById(R.id.button_back);
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        return view;
    }
} 