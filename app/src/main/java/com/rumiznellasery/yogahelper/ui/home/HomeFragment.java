package com.rumiznellasery.yogahelper.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rumiznellasery.yogahelper.AuthActivity;
import com.rumiznellasery.yogahelper.data.DbKeys;
import com.rumiznellasery.yogahelper.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // default: hide the verified icon
        binding.iconVerified.setVisibility(View.GONE);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // reload to ensure emailVerified is up-to-date
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser fresh = FirebaseAuth.getInstance().getCurrentUser();
                    if (fresh != null && fresh.isEmailVerified()) {
                        binding.iconVerified.setVisibility(View.VISIBLE);
                    }
                }
            });

            // Display name
            String displayName = user.getDisplayName();
            binding.textDisplayName.setText(
                    (displayName != null && !displayName.isEmpty()) ? displayName : "User"
            );

            // UID
            binding.textUserId.setText("UID: " + user.getUid());

            // Email
            String email = user.getEmail();
            binding.textEmail.setText("Email: " + (email != null ? email : "N/A"));
        } else {
            // No user logged in
            binding.textDisplayName.setText("No account data");
            binding.textUserId.setText("");
            binding.textEmail.setText("");
        }

        // Edit display name
        binding.iconEditName.setOnClickListener(v -> {
            android.app.AlertDialog.Builder builder =
                    new android.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Edit Name");
            final android.widget.EditText input =
                    new android.widget.EditText(requireContext());
            input.setText(binding.textDisplayName.getText().toString());
            builder.setView(input);

            builder.setPositiveButton("Update", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (newName.isEmpty()) return;

                FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
                if (current == null) return;

                UserProfileChangeRequest req =
                        new UserProfileChangeRequest.Builder()
                                .setDisplayName(newName)
                                .build();

                current.updateProfile(req).addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        binding.textDisplayName.setText(newName);
                        // update in Realtime Database
                        DbKeys keys = DbKeys.get(requireContext());
                        DatabaseReference ref = FirebaseDatabase
                                .getInstance(keys.databaseUrl)
                                .getReference(keys.users)
                                .child(current.getUid());
                        ref.child(keys.displayName).setValue(newName);
                    } else {
                        Toast.makeText(requireContext(),
                                "Could not update name.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
            builder.setNegativeButton("Cancel", (d, w) -> d.cancel());
            builder.show();
        });

        // Logout
        binding.buttonLogout.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Logging out…", Toast.LENGTH_SHORT).show();
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
