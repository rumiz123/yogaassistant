package com.rumiznellasery.yogahelper.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // prepare the image-picker launcher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) updateProfilePicture(uri);
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // hide verified icon until we know state
        binding.iconVerified.setVisibility(View.GONE);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // load existing photo
            Uri photoUri = user.getPhotoUrl();
            if (photoUri != null) {
                binding.imageProfile.setImageURI(photoUri);
            }

            // reload for up-to-date emailVerified
            user.reload().addOnCompleteListener(task -> {
                FirebaseUser fresh = FirebaseAuth.getInstance().getCurrentUser();
                if (fresh != null && fresh.isEmailVerified()) {
                    binding.iconVerified.setVisibility(View.VISIBLE);
                }
            });

            // name, UID, email
            String name = user.getDisplayName();
            binding.textDisplayName.setText(
                    (name != null && !name.isEmpty()) ? name : "User"
            );
            binding.textUserId.setText("UID: " + user.getUid());
            binding.textEmail.setText("Email: " + (user.getEmail() != null ? user.getEmail() : "N/A"));
        } else {
            binding.textDisplayName.setText("No account data");
            binding.textUserId.setText("");
            binding.textEmail.setText("");
        }

        // tap avatar → pick new image
        binding.imageProfile.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
            pick.setType("image/*");
            pickImageLauncher.launch(pick);
        });

        // edit name…
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
                FirebaseUser curr = FirebaseAuth.getInstance().getCurrentUser();
                if (curr == null) return;

                UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build();

                curr.updateProfile(req).addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        binding.textDisplayName.setText(newName);
                        // also save to Realtime DB if desired
                        DbKeys keys = DbKeys.get(requireContext());
                        DatabaseReference ref = FirebaseDatabase
                                .getInstance(keys.databaseUrl)
                                .getReference(keys.users)
                                .child(curr.getUid());
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

        // logout…
        binding.buttonLogout.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Logging out…", Toast.LENGTH_SHORT).show();
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(requireContext(), AuthActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        // Add this after binding = FragmentHomeBinding.inflate(...)
        binding.buttonSettings.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(((ViewGroup) requireView().getParent()).getId(), new com.rumiznellasery.yogahelper.ui.home.SettingsFragment())
                .addToBackStack(null)
                .commit();
        });

        return root;
    }

    private void updateProfilePicture(Uri uri) {
        FirebaseUser curr = FirebaseAuth.getInstance().getCurrentUser();
        if (curr == null) return;

        UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                .setPhotoUri(uri)
                .build();

        curr.updateProfile(req).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                binding.imageProfile.setImageURI(uri);
                // save URL in Realtime DB if you like:
                DbKeys keys = DbKeys.get(requireContext());
                DatabaseReference ref = FirebaseDatabase
                        .getInstance(keys.databaseUrl)
                        .getReference(keys.users)
                        .child(curr.getUid());
                ref.child("photoUrl").setValue(uri.toString());

                Toast.makeText(requireContext(),
                        "Profile picture updated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(),
                        "Failed to update profile picture", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
