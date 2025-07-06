package com.rumiznellasery.yogahelper.ui.achievements;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.rumiznellasery.yogahelper.databinding.FragmentAchievementsBinding;
import com.rumiznellasery.yogahelper.utils.AchievementManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AchievementsFragment extends Fragment {
    private FragmentAchievementsBinding binding;
    private AchievementManager achievementManager;
    private AchievementsAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAchievementsBinding.inflate(inflater, container, false);
        
        achievementManager = AchievementManager.getInstance(requireContext());
        
        setupRecyclerView();
        setupSwipeRefresh();
        setupBackButton();
        
        loadAchievements();
        
        return binding.getRoot();
    }

    private void setupRecyclerView() {
        adapter = new AchievementsAdapter();
        binding.recyclerAchievements.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerAchievements.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(this::loadAchievements);
    }

    private void setupBackButton() {
        binding.buttonBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    private void loadAchievements() {
        // Load from local storage first
        achievementManager.loadAchievementsFromLocal();
        
        // Then load from Firebase
        achievementManager.loadAchievementsFromFirebase();
        
        // Update UI
        updateAchievementsList();
        updateProgressOverview();
        
        binding.swipeRefresh.setRefreshing(false);
    }

    private void updateAchievementsList() {
        List<com.rumiznellasery.yogahelper.data.Achievement> achievements = achievementManager.getAchievements();
        adapter.setAchievements(achievements);
    }

    private void updateProgressOverview() {
        int unlockedCount = achievementManager.getUnlockedCount();
        int totalCount = achievementManager.getTotalCount();
        double completionPercentage = achievementManager.getCompletionPercentage();
        
        binding.textAchievementCount.setText(unlockedCount + "/" + totalCount + " Unlocked");
        binding.textCompletionPercentage.setText(String.format(Locale.getDefault(), "%.0f%%", completionPercentage));
        binding.progressAchievements.setProgress((int) completionPercentage);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 