package com.rumiznellasery.yogahelper.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rumiznellasery.yogahelper.AuthActivity;
import com.rumiznellasery.yogahelper.data.DbKeys;

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
            // Display name
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                binding.textDisplayName.setText(user.getDisplayName());
            } else {
                binding.textDisplayName.setText("User");
            }
            
            // Display UID
            binding.textUserId.setText(user.getUid());
        } else {
            binding.textDisplayName.setText("No account data");
            binding.textUserId.setText("");
        }

        binding.iconEditName.setOnClickListener(v -> {
            // Show dialog to edit name
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Edit Name");
            
            final android.widget.EditText input = new android.widget.EditText(requireContext());
            input.setText(binding.textDisplayName.getText().toString());
            builder.setView(input);
            
            builder.setPositiveButton("Update", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty()) {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                                .setDisplayName(newName)
                                .build();
                        currentUser.updateProfile(request).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                binding.textDisplayName.setText(newName);
                                DbKeys keys = DbKeys.get(requireContext());
                                DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
                                        .getReference(keys.users).child(currentUser.getUid());
                                ref.child(keys.displayName).setValue(newName);
                            }
                        });
                    }
                }
            });
            
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        binding.buttonLogout.setOnClickListener(v -> {
            android.widget.Toast.makeText(requireContext(), "Logout button clicked!", android.widget.Toast.LENGTH_SHORT).show();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireContext(), AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}