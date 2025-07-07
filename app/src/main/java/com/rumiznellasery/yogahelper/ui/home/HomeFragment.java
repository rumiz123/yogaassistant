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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.rumiznellasery.yogahelper.ui.badges.BadgesGridAdapter;
import com.bumptech.glide.Glide;
import android.content.SharedPreferences;
import java.io.File;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.info("HomeFragment onCreate started");
    }

    // Adapter for profile badge row
    private class ProfileBadgesAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<ProfileBadgesAdapter.BadgeViewHolder> {
        private final java.util.List<com.rumiznellasery.yogahelper.data.Badge> badges;
        ProfileBadgesAdapter(java.util.List<com.rumiznellasery.yogahelper.data.Badge> badges) {
            this.badges = badges;
        }
        @NonNull
        @Override
        public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge_profile_icon, parent, false);
            return new BadgeViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
            holder.bind(badges.get(position));
        }
        @Override
        public int getItemCount() { return badges.size(); }
        class BadgeViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            private final android.widget.ImageView imageBadge;
            BadgeViewHolder(@NonNull android.view.View itemView) {
                super(itemView);
                imageBadge = itemView.findViewById(R.id.image_badge_profile);
            }
            void bind(com.rumiznellasery.yogahelper.data.Badge badge) {
                int iconRes = getBadgeIconById(badge.id);
                imageBadge.setImageResource(iconRes);
                imageBadge.setAlpha(1.0f);
            }
            int getBadgeIconById(String badgeId) {
                if (badgeId == null) return R.drawable.ic_badge_first_workout;
                
                switch (badgeId) {
                    case "first_workout":
                        return R.drawable.ic_badge_first_workout;
                    case "week_streak":
                        return R.drawable.ic_badge_week_warrior;
                    case "month_streak":
                        return R.drawable.ic_badge_monthly_master;
                    case "hundred_workouts":
                        return R.drawable.ic_badge_century_club;
                    case "social_butterfly":
                        return R.drawable.ic_badge_social_butterfly;
                    case "competition_winner":
                        return R.drawable.ic_badge_champion;
                    case "pose_master":
                        return R.drawable.ic_badge_pose_master;
                    case "time_master":
                        return R.drawable.ic_badge_time_master;
                    case "perfect_week":
                        return R.drawable.ic_badge_perfect_week;
                    default:
                        return R.drawable.ic_badge_first_workout;
                }
            }
            int getBadgeIcon(com.rumiznellasery.yogahelper.data.Badge.BadgeType type) {
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

    private com.rumiznellasery.yogahelper.utils.BadgeManager badgeManager;
    private BadgesGridAdapter badgesAdapter;
    private boolean isBadgesExpanded = false;

    private void setupBadgesSection() {
        try {
            if (badgeManager == null) {
                badgeManager = com.rumiznellasery.yogahelper.utils.BadgeManager.getInstance(requireContext());
            }
            
            java.util.List<com.rumiznellasery.yogahelper.data.Badge> allBadges = badgeManager.getBadges();
            java.util.List<com.rumiznellasery.yogahelper.data.Badge> unlockedBadges = badgeManager.getUnlockedBadges();
            
            // Update badge count
            binding.textBadgesCount.setText(unlockedBadges.size() + "/" + allBadges.size());
            
            // Setup badges grid
            badgesAdapter = new BadgesGridAdapter();
            badgesAdapter.setBadges(allBadges);
            binding.recyclerBadgesGrid.setLayoutManager(new GridLayoutManager(requireContext(), 4));
            binding.recyclerBadgesGrid.setAdapter(badgesAdapter);
            
            // Add spacing between items
            binding.recyclerBadgesGrid.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(android.graphics.Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                    int spacing = 8;
                    outRect.left = spacing;
                    outRect.right = spacing;
                    outRect.top = spacing;
                    outRect.bottom = spacing;
                }
            });
            
            // Setup view all badges button
            binding.buttonViewAllBadges.setOnClickListener(v -> {
                try {
                    toggleBadgesExpansion();
                } catch (Exception e) {
                    Logger.error("Error toggling badges expansion", e);
                }
            });
        } catch (Exception e) {
            Logger.error("Error in setupBadgesSection", e);
            if (binding != null) {
                binding.textBadgesCount.setText("0/0");
            }
        }
    }

    private void toggleBadgesExpansion() {
        try {
            if (isBadgesExpanded) {
                // Collapse the badges section
                collapseBadgesSection();
            } else {
                // Expand the badges section
                expandBadgesSection();
            }
        } catch (Exception e) {
            Logger.error("Error in toggleBadgesExpansion", e);
        }
    }

    private void expandBadgesSection() {
        try {
            // Change button text
            binding.buttonViewAllBadges.setText("Show Less");
            
            // Get current height and target height
            binding.recyclerBadgesGrid.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(binding.recyclerBadgesGrid.getWidth(), android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
            );
            int targetHeight = binding.recyclerBadgesGrid.getMeasuredHeight();
            
            // Animate height expansion
            android.animation.ValueAnimator heightAnimator = android.animation.ValueAnimator.ofInt(300, targetHeight);
            heightAnimator.setDuration(400);
            heightAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
            
            heightAnimator.addUpdateListener(animation -> {
                int height = (int) animation.getAnimatedValue();
                android.view.ViewGroup.LayoutParams params = binding.recyclerBadgesGrid.getLayoutParams();
                params.height = height;
                binding.recyclerBadgesGrid.setLayoutParams(params);
            });
            
            heightAnimator.addListener(new android.animation.Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(android.animation.Animator animation) {
                    binding.recyclerBadgesGrid.setAlpha(0.9f);
                }
                
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    binding.recyclerBadgesGrid.setAlpha(1f);
                    isBadgesExpanded = true;
                }
                
                @Override
                public void onAnimationCancel(android.animation.Animator animation) {}
                
                @Override
                public void onAnimationRepeat(android.animation.Animator animation) {}
            });
            
            heightAnimator.start();
                
        } catch (Exception e) {
            Logger.error("Error in expandBadgesSection", e);
        }
    }

    private void collapseBadgesSection() {
        try {
            // Change button text back
            binding.buttonViewAllBadges.setText(requireContext().getString(R.string.view_all_badges));
            
            // Get current height
            int currentHeight = binding.recyclerBadgesGrid.getHeight();
            
            // Animate height collapse
            android.animation.ValueAnimator heightAnimator = android.animation.ValueAnimator.ofInt(currentHeight, 300);
            heightAnimator.setDuration(400);
            heightAnimator.setInterpolator(new android.view.animation.AccelerateInterpolator());
            
            heightAnimator.addUpdateListener(animation -> {
                int height = (int) animation.getAnimatedValue();
                android.view.ViewGroup.LayoutParams params = binding.recyclerBadgesGrid.getLayoutParams();
                params.height = height;
                binding.recyclerBadgesGrid.setLayoutParams(params);
            });
            
            heightAnimator.addListener(new android.animation.Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(android.animation.Animator animation) {
                    binding.recyclerBadgesGrid.setAlpha(1f);
                }
                
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    binding.recyclerBadgesGrid.setAlpha(1f);
                    isBadgesExpanded = false;
                }
                
                @Override
                public void onAnimationCancel(android.animation.Animator animation) {}
                
                @Override
                public void onAnimationRepeat(android.animation.Animator animation) {}
            });
            
            heightAnimator.start();
                
        } catch (Exception e) {
            Logger.error("Error in collapseBadgesSection", e);
        }
    }
    
    private void addEntranceAnimations() {
        try {
            // Animate the account info card
            android.view.animation.Animation slideUp = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up);
            binding.accountInfoContainer.startAnimation(slideUp);
            
            // Animate the quick stats card with delay
            binding.accountInfoContainer.postDelayed(() -> {
                try {
                    android.view.animation.Animation scaleIn = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.scale_in);
                    View quickStatsCard = requireActivity().findViewById(R.id.quick_stats_card);
                    if (quickStatsCard != null) {
                        quickStatsCard.startAnimation(scaleIn);
                    }
                } catch (Exception e) {
                    Logger.error("Error animating quick stats card", e);
                }
            }, 200);
            
            // Animate the badges section with delay
            binding.accountInfoContainer.postDelayed(() -> {
                try {
                    android.view.animation.Animation bounceIn = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.bounce_in);
                    View badgesCard = requireActivity().findViewById(R.id.badges_card);
                    if (badgesCard != null) {
                        badgesCard.startAnimation(bounceIn);
                    }
                } catch (Exception e) {
                    Logger.error("Error animating badges card", e);
                }
            }, 400);
            
            // Animate action buttons with delay
            binding.accountInfoContainer.postDelayed(() -> {
                try {
                    android.view.animation.Animation fadeIn = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in);
                    binding.buttonStartWorkout.startAnimation(fadeIn);
                    binding.buttonViewProgress.startAnimation(fadeIn);
                    
                    // Add button press animations
                    addButtonPressAnimations();
                } catch (Exception e) {
                    Logger.error("Error animating action buttons", e);
                }
            }, 600);
            
        } catch (Exception e) {
            Logger.error("Error in addEntranceAnimations", e);
        }
    }
    
    private void addButtonPressAnimations() {
        try {
            // Add press animation to start workout button
            binding.buttonStartWorkout.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP || 
                           event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                }
                return false;
            });
            
            // Add press animation to view progress button
            binding.buttonViewProgress.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP || 
                           event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                }
                return false;
            });
            
            // Add press animation to view all badges button
            binding.buttonViewAllBadges.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP || 
                           event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                }
                return false;
            });
            
        } catch (Exception e) {
            Logger.error("Error in addButtonPressAnimations", e);
        }
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
                // Load profile picture from internal storage
                SharedPreferences prefs = requireContext().getSharedPreferences("profile", android.content.Context.MODE_PRIVATE);
                String path = prefs.getString("profile_picture_path", null);
                if (path != null) {
                    File file = new File(path);
                    if (file.exists()) {
                        binding.imageProfile.setImageURI(android.net.Uri.fromFile(file));
                    } else {
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

            // Remove tap avatar → pick new image
            binding.imageProfile.setOnClickListener(null);

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
                    // Add button press animation
                    v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .withEndAction(() -> {
                            v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .withEndAction(() -> {
                                    Intent intent = new Intent(requireContext(), SettingsActivity.class);
                                    startActivity(intent);
                                })
                                .start();
                        })
                        .start();
                } catch (Exception e) {
                    Logger.error("Error opening SettingsActivity from HomeFragment", e);
                }
            });

            // Setup badges section
            setupBadgesSection();
            
            // Add entrance animations
            addEntranceAnimations();

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
        setupBadgesSection();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
