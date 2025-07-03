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
import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.data.DbKeys;
import com.rumiznellasery.yogahelper.databinding.FragmentHomeBinding;
import com.rumiznellasery.yogahelper.utils.Logger;
import com.rumiznellasery.yogahelper.ui.home.SettingsActivity;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.info("HomeFragment onCreate started");

        // prepare the image-picker launcher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            Logger.info("Profile picture selected in HomeFragment: " + uri.toString());
                            updateProfilePicture(uri);
                        }
                    } else {
                        Logger.warn("Image picker cancelled in HomeFragment");
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Logger.info("HomeFragment onCreateView started");
        new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        try {
            // hide verified icon until we know state
            binding.iconVerified.setVisibility(View.GONE);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                // load existing photo
                Uri photoUri = user.getPhotoUrl();
                if (photoUri != null) {
                    try {
                        binding.imageProfile.setImageURI(photoUri);
                    } catch (Exception e) {
                        Logger.error("Error setting profile image URI in HomeFragment", e);
                        binding.imageProfile.setImageResource(R.drawable.ic_avatar_placeholder);
                    }
                } else {
                    binding.imageProfile.setImageResource(R.drawable.ic_avatar_placeholder);
                }

                // reload for up-to-date emailVerified
                user.reload().addOnCompleteListener(task -> {
                    FirebaseUser fresh = FirebaseAuth.getInstance().getCurrentUser();
                    if (fresh != null && fresh.isEmailVerified()) {
                        binding.iconVerified.setVisibility(View.VISIBLE);
                    }
                });

                // Load display name from database
                DbKeys keys = DbKeys.get(requireContext());
                DatabaseReference ref = FirebaseDatabase
                        .getInstance(keys.databaseUrl)
                        .getReference(keys.users)
                        .child(user.getUid());
                
                ref.child(keys.displayName).get().addOnCompleteListener(task -> {
                    try {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            String displayName = task.getResult().getValue(String.class);
                            if (displayName != null && !displayName.isEmpty()) {
                                binding.textDisplayName.setText(displayName);
                            } else {
                                binding.textDisplayName.setText("User");
                            }
                        } else {
                            // Fallback to Firebase Auth display name
                            String name = user.getDisplayName();
                            binding.textDisplayName.setText(
                                    (name != null && !name.isEmpty()) ? name : "User"
                            );
                        }
                    } catch (Exception e) {
                        Logger.error("Error loading display name in HomeFragment", e);
                        binding.textDisplayName.setText("User");
                    }
                });

                ref.child("developer").get().addOnCompleteListener(devTask -> {
                    try {
                        boolean isDev = false;
                        if (devTask.isSuccessful() && devTask.getResult() != null && devTask.getResult().exists()) {
                            Boolean devFlag = devTask.getResult().getValue(Boolean.class);
                            isDev = devFlag != null && devFlag;
                        }
                        if (isDev) {
                            binding.textUserId.setVisibility(View.VISIBLE);
                        } else {
                            binding.textUserId.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        Logger.error("Error loading developer flag in HomeFragment", e);
                        binding.textUserId.setVisibility(View.GONE);
                    }
                });

                binding.textEmail.setText("Email: " + (user.getEmail() != null ? user.getEmail() : "N/A"));
            } else {
                Logger.warn("No user found in HomeFragment");
                binding.textDisplayName.setText("No account data");
                binding.textUserId.setText("");
                binding.textEmail.setText("");
            }

            // tap avatar → pick new image
            binding.imageProfile.setOnClickListener(v -> {
                try {
                    Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                    pick.setType("image/*");
                    pickImageLauncher.launch(pick);
                } catch (Exception e) {
                    Logger.error("Error launching image picker in HomeFragment", e);
                }
            });

            // edit name…
            binding.iconEditName.setOnClickListener(v -> {
                try {
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

                        // Update both Firebase Auth and database
                        UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                                .setDisplayName(newName)
                                .build();

                        curr.updateProfile(req).addOnCompleteListener(t -> {
                            if (t.isSuccessful()) {
                                // Update database
                                DbKeys keys = DbKeys.get(requireContext());
                                DatabaseReference ref = FirebaseDatabase
                                        .getInstance(keys.databaseUrl)
                                        .getReference(keys.users)
                                        .child(curr.getUid());
                                ref.child(keys.displayName).setValue(newName).addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful()) {
                                        binding.textDisplayName.setText(newName);
                                        Toast.makeText(requireContext(),
                                                "Name updated successfully", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Logger.error("Could not update name in database", dbTask.getException());
                                        Toast.makeText(requireContext(),
                                                "Could not update name in database.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                Logger.error("Could not update name in Firebase Auth", t.getException());
                                Toast.makeText(requireContext(),
                                        "Could not update name.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });

                    builder.setNegativeButton("Cancel", (d, w) -> d.cancel());
                    builder.show();
                } catch (Exception e) {
                    Logger.error("Error showing edit name dialog in HomeFragment", e);
                }
            });

            // Add this after binding = FragmentHomeBinding.inflate(...)
            binding.buttonSettings.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(requireContext(), SettingsActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Logger.error("Error opening SettingsActivity from HomeFragment", e);
                }
            });

            Logger.info("HomeFragment onCreateView completed successfully");
        } catch (Exception e) {
            Logger.error("Critical error in HomeFragment onCreateView", e);
            if (binding != null) {
                binding.textDisplayName.setText("Error loading account");
            }
        }
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        View navBar = requireActivity().findViewById(R.id.nav_view);
        if (navBar != null) navBar.setVisibility(View.VISIBLE);
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
