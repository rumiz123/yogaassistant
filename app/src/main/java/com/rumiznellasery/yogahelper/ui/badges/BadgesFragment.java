package com.rumiznellasery.yogahelper.ui.badges;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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