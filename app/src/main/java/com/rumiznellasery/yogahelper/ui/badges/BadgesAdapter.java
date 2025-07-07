package com.rumiznellasery.yogahelper.ui.badges;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.data.Badge;
import com.rumiznellasery.yogahelper.databinding.ItemBadgeBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BadgesAdapter extends RecyclerView.Adapter<BadgesAdapter.BadgeViewHolder> {
    private List<Badge> badges = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public void setBadges(List<Badge> badges) {
        this.badges = badges;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBadgeBinding binding = ItemBadgeBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new BadgeViewHolder(binding);
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
        private final ItemBadgeBinding binding;

        BadgeViewHolder(ItemBadgeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Badge badge) {
            // Set badge icon
            binding.textBadgeIcon.setText(badge.icon);
            
            // Set badge details
            binding.textBadgeTitle.setText(badge.title);
            binding.textBadgeDescription.setText(badge.description);
            
            // Set rarity
            binding.textBadgeRarity.setText(badge.getRarityName());
            binding.textBadgeRarity.setTextColor(badge.getRarityColor());
            
            // Set progress
            binding.textProgress.setText(badge.currentProgress + "/" + badge.requirement);
            binding.textProgressPercentage.setText(badge.getProgressPercentage() + "%");
            binding.progressBadge.setProgress(badge.getProgressPercentage());
            
            // Set status and unlock date
            if (badge.unlocked) {
                binding.textStatus.setText("UNLOCKED");
                binding.textStatus.setBackgroundResource(R.drawable.button_primary_gradient);
                binding.iconUnlocked.setVisibility(View.VISIBLE);
                
                if (badge.unlockedDate > 0) {
                    String unlockDate = dateFormat.format(new Date(badge.unlockedDate));
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
            
            // Set progress bar color based on completion and rarity
            if (badge.isNearCompletion()) {
                binding.progressBadge.setIndicatorColor(
                    itemView.getContext().getColor(R.color.warning_orange));
            } else if (badge.unlocked) {
                binding.progressBadge.setIndicatorColor(badge.getRarityColor());
            } else {
                binding.progressBadge.setIndicatorColor(
                    itemView.getContext().getColor(R.color.progress_purple));
            }
            
            // Set badge background based on rarity
            if (badge.unlocked) {
                binding.badgeIconContainer.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(badge.getRarityColor()));
            } else {
                binding.badgeIconContainer.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                        itemView.getContext().getColor(R.color.card_background_accent)));
            }
        }
    }
} 