package com.rumiznellasery.yogahelper.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.data.Badge;
import com.rumiznellasery.yogahelper.data.DbKeys;
import com.rumiznellasery.yogahelper.utils.BadgeManager;
import com.rumiznellasery.yogahelper.utils.Logger;
import com.rumiznellasery.yogahelper.databinding.FragmentProfileBinding;
import com.rumiznellasery.yogahelper.ui.badges.BadgesAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private BadgeManager badgeManager;
    private BadgesAdapter badgesAdapter;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    // Adapter for profile badge row
    private class ProfileBadgesAdapter extends RecyclerView.Adapter<ProfileBadgesAdapter.BadgeViewHolder> {
        private final List<Badge> badges;
        
        ProfileBadgesAdapter(List<Badge> badges) {
            this.badges = badges;
        }
        
        @NonNull
        @Override
        public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge_profile_icon, parent, false);
            return new BadgeViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
            holder.bind(badges.get(position));
        }
        
        @Override
        public int getItemCount() { 
            return badges.size(); 
        }
        
        class BadgeViewHolder extends RecyclerView.ViewHolder {
            private final View badgeIcon;
            private final View badgeBackground;
            
            BadgeViewHolder(@NonNull View itemView) {
                super(itemView);
                badgeIcon = itemView.findViewById(R.id.image_badge_profile);
                badgeBackground = itemView.findViewById(R.id.badge_bg_circle);
            }
            
            void bind(Badge badge) {
                if (badge.unlocked) {
                    badgeBackground.setBackgroundResource(R.drawable.badge_profile_circle_bg);
                    badgeIcon.setVisibility(View.VISIBLE);
                    badgeIcon.setBackgroundResource(getBadgeIcon(badge.type));
                } else {
                    badgeBackground.setBackgroundResource(R.drawable.badge_placeholder);
                    badgeIcon.setVisibility(View.GONE);
                }
            }
            
            int getBadgeIcon(Badge.BadgeType type) {
                if (type == null) return R.drawable.ic_badge_first_workout;
                switch (type) {
                    case WORKOUT_COUNT: return R.drawable.ic_badge_first_workout;
                    case STREAK_DAYS: return R.drawable.ic_badge_week_warrior;
                    case CALORIES_BURNED: return R.drawable.ic_badge_week_warrior;
                    case FRIENDS_COUNT: return R.drawable.ic_badge_social_butterfly;
                    case COMPETITION_WINS: return R.drawable.ic_badge_champion;
                    case PERFECT_WEEK: return R.drawable.ic_badge_perfect_week;
                    case POSE_MASTERY: return R.drawable.ic_badge_pose_master;
                    case WORKOUT_TIME: return R.drawable.ic_badge_time_master;
                    case CHALLENGE_COMPLETION: return R.drawable.ic_badge_champion;
                    default: return R.drawable.ic_badge_first_workout;
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Register the image picker launcher
        pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    if (selectedImage != null) {
                        handleImageSelection(selectedImage);
                    }
                }
            }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            binding = FragmentProfileBinding.inflate(inflater, container, false);
            badgeManager = BadgeManager.getInstance(requireContext());
            
            setupProfilePicture();
            setupProfileInfo();
            setupProfileBadgesRow();
            setupBadgesSection();
            setupSettingsButton();
            
            return binding.getRoot();
        } catch (Exception e) {
            Logger.error("Error in ProfileFragment onCreateView", e);
            // Return a simple view to prevent crash
            return new View(requireContext());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            if (badgeManager == null) {
                badgeManager = BadgeManager.getInstance(requireContext());
            }
            setupProfileBadgesRow();
            setupBadgesSection();
        } catch (Exception e) {
            Logger.error("Error in ProfileFragment onResume", e);
        }
    }

    private void setupProfilePicture() {
        try {
            if (binding == null) return;
            
            // Load profile picture from internal storage
            SharedPreferences profilePrefs = requireContext().getSharedPreferences("profile", Context.MODE_PRIVATE);
            String path = profilePrefs.getString("profile_picture_path", null);
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    binding.profilePicture.setImageURI(Uri.fromFile(file));
                } else {
                    binding.profilePicture.setImageResource(R.drawable.ic_avatar_placeholder);
                }
            } else {
                binding.profilePicture.setImageResource(R.drawable.ic_avatar_placeholder);
            }

            // Handle profile picture click
            binding.profilePicture.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                pickImageLauncher.launch(intent);
            });
        } catch (Exception e) {
            Logger.error("Error in setupProfilePicture", e);
            if (binding != null) {
                binding.profilePicture.setImageResource(R.drawable.ic_avatar_placeholder);
            }
        }
    }

    private void setupProfileInfo() {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && binding != null) {
                // Load display name from database
                DbKeys keys = DbKeys.get(requireContext());
                DatabaseReference userRef = FirebaseDatabase.getInstance(keys.databaseUrl)
                        .getReference(keys.users)
                        .child(user.getUid());
                
                userRef.child(keys.displayName).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists() && binding != null) {
                        String displayName = task.getResult().getValue(String.class);
                        if (displayName != null && !displayName.isEmpty()) {
                            binding.profileName.setText(displayName);
                        } else {
                            binding.profileName.setText(user.getEmail());
                        }
                    } else if (binding != null) {
                        binding.profileName.setText(user.getEmail());
                    }
                });

                // Show verified icon if email is verified
                if (user.isEmailVerified()) {
                    binding.verifiedIcon.setVisibility(View.VISIBLE);
                } else {
                    binding.verifiedIcon.setVisibility(View.GONE);
                }
            } else if (binding != null) {
                binding.profileName.setText("Guest User");
                binding.verifiedIcon.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Logger.error("Error in setupProfileInfo", e);
            if (binding != null) {
                binding.profileName.setText("Error loading profile");
                binding.verifiedIcon.setVisibility(View.GONE);
            }
        }
    }

    private void setupProfileBadgesRow() {
        try {
            if (badgeManager == null || binding == null) return;
            
            List<Badge> unlocked = badgeManager.getUnlockedBadges();
            if (unlocked.isEmpty()) {
                binding.profileBadgesRow.setVisibility(View.GONE);
                return;
            }
            binding.profileBadgesRow.setVisibility(View.VISIBLE);
            List<Badge> toShow = unlocked.size() > 5 ? unlocked.subList(0, 5) : unlocked;
            ProfileBadgesAdapter adapter = new ProfileBadgesAdapter(toShow);
            binding.profileBadgesRow.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            binding.profileBadgesRow.setAdapter(adapter);
        } catch (Exception e) {
            Logger.error("Error in setupProfileBadgesRow", e);
            if (binding != null) {
                binding.profileBadgesRow.setVisibility(View.GONE);
            }
        }
    }

    private void setupBadgesSection() {
        try {
            if (badgeManager == null || binding == null) return;
            
            List<Badge> allBadges = badgeManager.getBadges();
            List<Badge> unlockedBadges = badgeManager.getUnlockedBadges();
            
            // Update badge count
            binding.badgesCount.setText(unlockedBadges.size() + "/" + allBadges.size());
            
            // Setup badges grid
            badgesAdapter = new BadgesAdapter();
            badgesAdapter.setBadges(allBadges);
            binding.badgesGrid.setLayoutManager(new GridLayoutManager(requireContext(), 3));
            binding.badgesGrid.setAdapter(badgesAdapter);
        } catch (Exception e) {
            Logger.error("Error in setupBadgesSection", e);
            if (binding != null) {
                binding.badgesCount.setText("0/0");
            }
        }
    }

    private void setupSettingsButton() {
        try {
            if (binding == null) return;
            
            binding.settingsButton.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(requireContext(), com.rumiznellasery.yogahelper.ui.home.SettingsActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Logger.error("Error opening SettingsActivity from ProfileFragment", e);
                }
            });
        } catch (Exception e) {
            Logger.error("Error in setupSettingsButton", e);
        }
    }

    private void handleImageSelection(Uri selectedImage) {
        try {
            // Save profile picture to internal storage
            File profileDir = new File(requireContext().getFilesDir(), "profile");
            if (!profileDir.exists()) {
                profileDir.mkdirs();
            }
            
            File profileFile = new File(profileDir, "profile_picture.jpg");
            FileOutputStream fos = new FileOutputStream(profileFile);
            InputStream in = requireContext().getContentResolver().openInputStream(selectedImage);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            in.close();
            fos.close();
            
            // Save path to SharedPreferences
            SharedPreferences profilePrefs = requireContext().getSharedPreferences("profile", Context.MODE_PRIVATE);
            profilePrefs.edit().putString("profile_picture_path", profileFile.getAbsolutePath()).apply();
            
            // Update UI
            if (binding != null) {
                binding.profilePicture.setImageURI(Uri.fromFile(profileFile));
                Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Logger.error("Error saving profile picture", e);
            Toast.makeText(requireContext(), "Failed to update profile picture", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 