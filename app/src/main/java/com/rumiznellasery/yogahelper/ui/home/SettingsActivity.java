package com.rumiznellasery.yogahelper.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.rumiznellasery.yogahelper.utils.DeveloperMode;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

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
        loadProfilePictureFromInternalStorage(profilePic);

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

        // Setup developer mode
        setupDeveloperMode();

        // Logout button
        View logoutButton = findViewById(R.id.button_logout);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                try {
                    // Delete profile picture from internal storage
                    SharedPreferences prefs = getSharedPreferences("profile", MODE_PRIVATE);
                    String path = prefs.getString("profile_picture_path", null);
                    if (path != null) {
                        File file = new File(path);
                        if (file.exists()) file.delete();
                        prefs.edit().remove("profile_picture_path").apply();
                    }
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
        try {
            // Save image to internal storage
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) throw new Exception("Unable to open input stream for URI: " + uri);
            File file = new File(getFilesDir(), "profile_picture.jpg");
            try (OutputStream outputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            inputStream.close();
            // Save file path to SharedPreferences
            SharedPreferences prefs = getSharedPreferences("profile", MODE_PRIVATE);
            prefs.edit().putString("profile_picture_path", file.getAbsolutePath()).apply();
            // Update UI
            ImageView profilePic = findViewById(R.id.settings_profile_pic);
            if (profilePic != null) {
                profilePic.setImageURI(Uri.fromFile(file));
                Logger.info("Profile picture ImageView updated successfully");
            }
            Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Logger.error("Error saving profile picture to internal storage", e);
            Toast.makeText(this, "Failed to update profile picture", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfilePictureFromInternalStorage(ImageView profilePic) {
        SharedPreferences prefs = getSharedPreferences("profile", MODE_PRIVATE);
        String path = prefs.getString("profile_picture_path", null);
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                profilePic.setImageURI(Uri.fromFile(file));
                return;
            }
        }
        profilePic.setImageResource(R.drawable.ic_avatar_placeholder);
    }
    
    private void setupDeveloperMode() {
        View developerSection = findViewById(R.id.developer_section);
        androidx.appcompat.widget.SwitchCompat developerSwitch = findViewById(R.id.switch_developer_mode);
        
        if (developerSection == null || developerSwitch == null) {
            return;
        }
        
        // Check if current user is a developer
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && DeveloperMode.isDeveloperEmail(currentUser.getEmail())) {
            developerSection.setVisibility(View.VISIBLE);
            
            // Set current state
            developerSwitch.setChecked(DeveloperMode.isDeveloperMode(this));
            
            // Handle switch changes
            developerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                DeveloperMode.setDeveloperMode(this, isChecked);
                Toast.makeText(this, 
                    isChecked ? "Developer mode enabled" : "Developer mode disabled", 
                    Toast.LENGTH_SHORT).show();
            });
        } else {
            developerSection.setVisibility(View.GONE);
        }
    }
} 