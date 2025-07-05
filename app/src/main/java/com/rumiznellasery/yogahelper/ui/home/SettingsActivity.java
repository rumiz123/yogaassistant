package com.rumiznellasery.yogahelper.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.data.DbKeys;
import com.rumiznellasery.yogahelper.utils.Logger;

public class SettingsActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.fragment_settings);

        // Hide the bottom nav bar - removed for camera functionality
        // View navBar = findViewById(R.id.nav_view);
        // if (navBar != null) navBar.setVisibility(View.GONE);

        // prepare the image-picker launcher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            Logger.info("Image selected in SettingsActivity: " + uri.toString());
                            updateProfilePicture(uri);
                        }
                    } else {
                        Logger.warn("Image picker cancelled in SettingsActivity");
                    }
                }
        );

        // Back button
        ImageView backButton = findViewById(R.id.button_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // Profile picture
        ImageView profilePic = findViewById(R.id.settings_profile_pic);
        if (profilePic == null) {
            Logger.error("Profile picture ImageView not found in SettingsActivity");
            return;
        }

        // Load existing profile picture
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Logger.info("Loading profile picture for user: " + user.getUid());
            Uri photoUri = user.getPhotoUrl();
            if (photoUri != null) {
                Logger.info("Setting profile picture from URI: " + photoUri.toString());
                try {
                    loadProfilePictureFromDatabase(profilePic, photoUri);
                } catch (Exception e) {
                    Logger.error("Error setting profile picture from URI", e);
                    profilePic.setImageResource(R.drawable.ic_avatar_placeholder);
                }
            } else {
                Logger.info("No profile picture URI found, using default");
                profilePic.setImageResource(R.drawable.ic_avatar_placeholder);
            }
        } else {
            Logger.warn("No authenticated user found in SettingsActivity");
            profilePic.setImageResource(R.drawable.ic_avatar_placeholder);
        }

        // Profile picture click to change
        profilePic.setOnClickListener(v -> {
            Logger.info("Profile picture clicked in SettingsActivity");
            try {
                Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                pick.setType("image/*");
                pickImageLauncher.launch(pick);
            } catch (Exception e) {
                Logger.error("Error launching image picker", e);
                Toast.makeText(this, "Error opening image picker", Toast.LENGTH_SHORT).show();
            }
        });

        // Logout button
        View logoutButton = findViewById(R.id.button_logout);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                try {
                    Logger.info("User logging out from SettingsActivity");
                    FirebaseAuth.getInstance().signOut();
                    Intent i = new Intent(SettingsActivity.this, com.rumiznellasery.yogahelper.AuthActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                } catch (Exception e) {
                    Logger.error("Error during logout in SettingsActivity", e);
                    Toast.makeText(SettingsActivity.this, "Error during logout", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateProfilePicture(Uri uri) {
        Logger.info("Updating profile picture in SettingsActivity: " + uri.toString());
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
                    ImageView profilePic = findViewById(R.id.settings_profile_pic);
                    if (profilePic != null) {
                        profilePic.setImageURI(uri);
                        Logger.info("Profile picture ImageView updated successfully");
                    } else {
                        Logger.warn("Profile picture ImageView not found after update");
                    }

                    // save URL in Realtime DB
                    try {
                        DbKeys keys = DbKeys.get(this);
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

                    Toast.makeText(this,
                            "Profile picture updated", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Logger.error("Error updating UI after profile picture change", e);
                    Toast.makeText(this,
                            "Profile picture updated but UI update failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                Logger.error("Failed to update profile picture in Firebase Auth", task.getException());
                Toast.makeText(this,
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

            DbKeys keys = DbKeys.get(this);
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
} 