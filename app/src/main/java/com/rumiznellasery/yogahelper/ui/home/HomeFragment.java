package com.rumiznellasery.yogahelper.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.rumiznellasery.yogahelper.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            StringBuilder sb = new StringBuilder();
            if (user.getDisplayName() != null) {
                sb.append("Name: ").append(user.getDisplayName()).append('\n');
                binding.editDisplayName.setText(user.getDisplayName());
            }
            if (user.getEmail() != null) sb.append("Email: ").append(user.getEmail()).append('\n');
            if (user.getPhoneNumber() != null) sb.append("Phone: ").append(user.getPhoneNumber()).append('\n');
            sb.append("UID: ").append(user.getUid());
            binding.textAccountDetails.setText(sb.toString());
        } else {
            binding.textAccountDetails.setText("No account data");
        }

        binding.buttonUpdateName.setOnClickListener(v -> {
            String name = binding.editDisplayName.getText().toString().trim();
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null && !name.isEmpty()) {
                UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build();
                currentUser.updateProfile(request).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        binding.textAccountDetails.setText(
                                "Name: " + name + "\nEmail: " + currentUser.getEmail() + "\nUID: " + currentUser.getUid());
                    }
                });
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}