package com.rumiznellasery.yogahelper.ui.achievements;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.data.Achievement;
import com.rumiznellasery.yogahelper.databinding.ItemAchievementBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AchievementsAdapter extends RecyclerView.Adapter<AchievementsAdapter.AchievementViewHolder> {
    private List<Achievement> achievements = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public void setAchievements(List<Achievement> achievements) {
        this.achievements = achievements;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AchievementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAchievementBinding binding = ItemAchievementBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new AchievementViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AchievementViewHolder holder, int position) {
        holder.bind(achievements.get(position));
    }

    @Override
    public int getItemCount() {
        return achievements.size();
    }

    class AchievementViewHolder extends RecyclerView.ViewHolder {
        private final ItemAchievementBinding binding;

        AchievementViewHolder(ItemAchievementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Achievement achievement) {
            // Set achievement icon
            binding.textAchievementIcon.setText(achievement.icon);
            
            // Set achievement details
            binding.textAchievementTitle.setText(achievement.title);
            binding.textAchievementDescription.setText(achievement.description);
            
            // Set progress
            binding.textProgress.setText(achievement.currentProgress + "/" + achievement.requirement);
            binding.textProgressPercentage.setText(achievement.getProgressPercentage() + "%");
            binding.progressAchievement.setProgress(achievement.getProgressPercentage());
            
            // Set status and unlock date
            if (achievement.unlocked) {
                binding.textStatus.setText("UNLOCKED");
                binding.textStatus.setBackgroundResource(R.drawable.button_primary_gradient);
                binding.iconUnlocked.setVisibility(View.VISIBLE);
                
                if (achievement.unlockedDate > 0) {
                    String unlockDate = dateFormat.format(new Date(achievement.unlockedDate));
                    binding.textUnlockDate.setText("Unlocked " + unlockDate);
                } else {
                    binding.textUnlockDate.setText("");
                }
            } else {
                binding.textStatus.setText("LOCKED");
                binding.textStatus.setBackgroundResource(R.drawable.rounded_button);
                binding.iconUnlocked.setVisibility(View.GONE);
                binding.textUnlockDate.setText("");
            }
            
            // Set progress bar color based on completion
            if (achievement.isNearCompletion()) {
                binding.progressAchievement.setIndicatorColor(
                    itemView.getContext().getColor(R.color.warning_orange));
            } else if (achievement.unlocked) {
                binding.progressAchievement.setIndicatorColor(
                    itemView.getContext().getColor(R.color.success_green));
            } else {
                binding.progressAchievement.setIndicatorColor(
                    itemView.getContext().getColor(R.color.progress_purple));
            }
        }
    }
} 