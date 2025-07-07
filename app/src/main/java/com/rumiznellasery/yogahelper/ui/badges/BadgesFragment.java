package com.rumiznellasery.yogahelper.ui.badges;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.databinding.FragmentBadgesBinding;
import com.rumiznellasery.yogahelper.utils.BadgeManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BadgesFragment extends Fragment {
    private FragmentBadgesBinding binding;
    private BadgeManager badgeManager;
    private BadgesAdapter adapter;
    private boolean badgesAreUnlocked = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBadgesBinding.inflate(inflater, container, false);
        
        badgeManager = BadgeManager.getInstance(requireContext());
        
        setupRecyclerView();
        setupSwipeRefresh();
        setupBackButton();
        setupAnimations();

        // Setup Test Badges button
        binding.buttonTestBadges.setEnabled(true);
        binding.buttonTestBadges.setVisibility(View.VISIBLE);
        binding.buttonTestBadges.setAlpha(1.0f);
        binding.buttonTestBadges.setClickable(true);
        binding.buttonTestBadges.setOnClickListener(v -> {
            android.widget.Toast.makeText(requireContext(), "Test Badges button pressed", android.widget.Toast.LENGTH_SHORT).show();
            List<com.rumiznellasery.yogahelper.data.Badge> badges = badgeManager.getBadges();
            if (!badgesAreUnlocked) {
                // Unlock all badges
                for (com.rumiznellasery.yogahelper.data.Badge badge : badges) {
                    badge.unlocked = true;
                    badge.currentProgress = badge.requirement;
                    badge.unlockedDate = System.currentTimeMillis();
                    badgeManager.saveBadgeLocally(badge);
                }
                badgesAreUnlocked = true;
                android.widget.Toast.makeText(requireContext(), "All badges unlocked!", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                // Reset all badges
                for (com.rumiznellasery.yogahelper.data.Badge badge : badges) {
                    badge.unlocked = false;
                    badge.currentProgress = 0;
                    badge.unlockedDate = 0;
                    badgeManager.saveBadgeLocally(badge);
                }
                badgesAreUnlocked = false;
                android.widget.Toast.makeText(requireContext(), "All badges reset!", android.widget.Toast.LENGTH_SHORT).show();
            }
            updateBadgesList();
            updateProgressOverview();
        });

        loadBadges();
        
        return binding.getRoot();
    }

    private void setupRecyclerView() {
        adapter = new BadgesAdapter();
        binding.recyclerBadges.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerBadges.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(this::loadBadges);
    }

    private void setupBackButton() {
        binding.buttonBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                // Show flat grey overlay immediately
                android.view.View loadingOverlay = getActivity().findViewById(R.id.loading_overlay);
                if (loadingOverlay != null) {
                    loadingOverlay.setVisibility(android.view.View.VISIBLE);
                }
                
                // Hide the overlay container
                android.view.View overlayContainer = getActivity().findViewById(R.id.overlay_container);
                if (overlayContainer != null) {
                    overlayContainer.setVisibility(android.view.View.GONE);
                }
                
                // Navigate back
                getActivity().onBackPressed();
                
                // Hide loading overlay after navigation completes
                if (loadingOverlay != null) {
                    loadingOverlay.postDelayed(() -> {
                        loadingOverlay.setVisibility(android.view.View.GONE);
                    }, 125);
                }
            }
        });
    }

    private void setupAnimations() {
        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up);
        Animation scaleIn = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_in);
        Animation bounceIn = AnimationUtils.loadAnimation(requireContext(), R.anim.bounce_in);

        // Apply entrance animations with staggered timing
        // Header
        View header = (View) binding.buttonBack.getParent();
        if (header != null) {
            header.startAnimation(fadeIn);
        }

        // Progress overview card
        View progressCard = (View) binding.getRoot().findViewById(R.id.progress_badges).getParent().getParent();
        if (progressCard != null) {
            progressCard.startAnimation(slideUp);
            progressCard.getAnimation().setStartOffset(200);
        }

        // Test badges button
        binding.buttonTestBadges.startAnimation(slideUp);
        binding.buttonTestBadges.getAnimation().setStartOffset(400);

        // Swipe refresh layout
        binding.swipeRefresh.startAnimation(slideUp);
        binding.swipeRefresh.getAnimation().setStartOffset(600);

        // Add button press animations
        setupButtonAnimations();
    }

    private void setupButtonAnimations() {
        // Back button
        binding.buttonBack.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                Animation scaleOut = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_out);
                scaleOut.setDuration(75);
                v.startAnimation(scaleOut);
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                Animation scaleIn = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_in);
                scaleIn.setDuration(75);
                v.startAnimation(scaleIn);
            }
            return false;
        });

        // Test badges button
        binding.buttonTestBadges.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                Animation scaleOut = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_out);
                scaleOut.setDuration(75);
                v.startAnimation(scaleOut);
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                Animation scaleIn = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_in);
                scaleIn.setDuration(75);
                v.startAnimation(scaleIn);
            }
            return false;
        });
    }

    private void loadBadges() {
        // Load from local storage first
        badgeManager.loadBadgesFromLocal();
        
        // Then load from Firebase
        badgeManager.loadBadgesFromFirebase();
        
        // Update UI
        updateBadgesList();
        updateProgressOverview();
        
        binding.swipeRefresh.setRefreshing(false);
    }

    private void updateBadgesList() {
        List<com.rumiznellasery.yogahelper.data.Badge> badges = badgeManager.getBadges();
        adapter.setBadges(badges);
    }

    private void updateProgressOverview() {
        int unlockedCount = badgeManager.getUnlockedCount();
        int totalCount = badgeManager.getTotalCount();
        double completionPercentage = badgeManager.getCompletionPercentage();
        
        binding.textBadgeCount.setText(unlockedCount + "/" + totalCount + " Unlocked");
        binding.textCompletionPercentage.setText(String.format(Locale.getDefault(), "%.0f%%", completionPercentage));
        binding.progressBadges.setProgress((int) completionPercentage);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 