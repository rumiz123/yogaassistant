package com.rumiznellasery.yogahelper.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.data.DbKeys;
import com.rumiznellasery.yogahelper.utils.Logger;

public class SettingsFragment extends Fragment {
    
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.info("SettingsFragment onCreate started");

        // prepare the image-picker launcher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            Logger.info("Image selected in SettingsFragment: " + uri.toString());
                            updateProfilePicture(uri);
                        }
                    } else {
                        Logger.warn("Image picker cancelled in SettingsFragment");
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Logger.info("SettingsFragment onCreateView started");
        try {
            View view = inflater.inflate(R.layout.fragment_settings, container, false);
            
            // Back button
            ImageView backButton = view.findViewById(R.id.button_back);
            if (backButton != null) {
                backButton.setOnClickListener(v -> {
                    Logger.info("Back button clicked in SettingsFragment");
                    try {
                        requireActivity().getSupportFragmentManager().popBackStack();
                    } catch (Exception e) {
                        Logger.error("Error navigating back from SettingsFragment", e);
                    }
                });
            } else {
                Logger.warn("Back button not found in SettingsFragment");
            }
            
            // Profile picture
            ImageView profilePic = view.findViewById(R.id.settings_profile_pic);
            if (profilePic == null) {
                Logger.error("Profile picture ImageView not found in SettingsFragment");
                return view;
            }
            
            // Load existing profile picture
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                Logger.info("Loading profile picture for user: " + user.getUid());
                Uri photoUri = user.getPhotoUrl();
                if (photoUri != null) {
                    Logger.info("Setting profile picture from URI: " + photoUri.toString());
                    try {
                        // Try to load from database first, then fallback to Firebase Auth
                        loadProfilePictureFromDatabase(profilePic, photoUri);
                    } catch (Exception e) {
                        Logger.error("Error setting profile picture from URI", e);
                        // Fallback to default image
                        profilePic.setImageResource(R.drawable.ic_avatar_placeholder);
                    }
                } else {
                    Logger.info("No profile picture URI found, using default");
                    profilePic.setImageResource(R.drawable.ic_avatar_placeholder);
                }
            } else {
                Logger.warn("No authenticated user found in SettingsFragment");
                profilePic.setImageResource(R.drawable.ic_avatar_placeholder);
            }
            
            // Profile picture click to change
            profilePic.setOnClickListener(v -> {
                Logger.info("Profile picture clicked in SettingsFragment");
                try {
                    Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                    pick.setType("image/*");
                    pickImageLauncher.launch(pick);
                } catch (Exception e) {
                    Logger.error("Error launching image picker", e);
                    Toast.makeText(requireContext(), "Error opening image picker", Toast.LENGTH_SHORT).show();
                }
            });
            
            Logger.info("SettingsFragment onCreateView completed successfully");
            return view;
        } catch (Exception e) {
            Logger.error("Critical error in SettingsFragment onCreateView", e);
            throw e; // Re-throw to see the crash in logs
        }
    }
    
    private void updateProfilePicture(Uri uri) {
        Logger.info("Updating profile picture in SettingsFragment: " + uri.toString());
        FirebaseUser curr = FirebaseAuth.getInstance().getCurrentUser();
        if (curr == null) {
            Logger.error("No authenticated user found when updating profile picture");
            return;
        }

        UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                .setPhotoUri(uri)
                .build();

        curr.updateProfile(req).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Logger.info("Profile picture updated successfully in Firebase Auth");
                try {
                    ImageView profilePic = getView().findViewById(R.id.settings_profile_pic);
                    if (profilePic != null) {
                        profilePic.setImageURI(uri);
                        Logger.info("Profile picture ImageView updated successfully");
                    } else {
                        Logger.warn("Profile picture ImageView not found after update");
                    }
                    
                    // save URL in Realtime DB
                    try {
                        DbKeys keys = DbKeys.get(requireContext());
                        DatabaseReference ref = FirebaseDatabase
                                .getInstance(keys.databaseUrl)
                                .getReference(keys.users)
                                .child(curr.getUid());
                        ref.child("photoUrl").setValue(uri.toString()).addOnCompleteListener(dbTask -> {
                            if (dbTask.isSuccessful()) {
                                Logger.info("Profile picture URL saved to database successfully");
                            } else {
                                Logger.error("Failed to save profile picture URL to database", dbTask.getException());
                            }
                        });
                    } catch (Exception e) {
                        Logger.error("Error saving profile picture URL to database", e);
                    }

                    Toast.makeText(requireContext(),
                            "Profile picture updated", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Logger.error("Error updating UI after profile picture change", e);
                    Toast.makeText(requireContext(),
                            "Profile picture updated but UI update failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                Logger.error("Failed to update profile picture in Firebase Auth", task.getException());
                Toast.makeText(requireContext(),
                        "Failed to update profile picture", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadProfilePictureFromDatabase(ImageView profilePic, Uri fallbackUri) {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Logger.warn("No user found when loading profile picture from database");
                profilePic.setImageResource(R.drawable.ic_avatar_placeholder);
                return;
            }
            
            DbKeys keys = DbKeys.get(requireContext());
            DatabaseReference ref = FirebaseDatabase
                    .getInstance(keys.databaseUrl)
                    .getReference(keys.users)
                    .child(user.getUid());
            
            ref.child("photoUrl").get().addOnCompleteListener(task -> {
                try {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        String photoUrl = task.getResult().getValue(String.class);
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Logger.info("Loading profile picture from database: " + photoUrl);
                            Uri uri = Uri.parse(photoUrl);
                            profilePic.setImageURI(uri);
                        } else {
                            Logger.info("No photo URL in database, using fallback");
                            profilePic.setImageURI(fallbackUri);
                        }
                    } else {
                        Logger.info("No photo URL in database, using fallback");
                        profilePic.setImageURI(fallbackUri);
                    }
                } catch (Exception e) {
                    Logger.error("Error loading profile picture from database", e);
                    profilePic.setImageResource(R.drawable.ic_avatar_placeholder);
                }
            });
        } catch (Exception e) {
            Logger.error("Error in loadProfilePictureFromDatabase", e);
            profilePic.setImageResource(R.drawable.ic_avatar_placeholder);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        View navBar = requireActivity().findViewById(R.id.nav_view);
        if (navBar != null) navBar.setVisibility(View.VISIBLE);
    }
} 