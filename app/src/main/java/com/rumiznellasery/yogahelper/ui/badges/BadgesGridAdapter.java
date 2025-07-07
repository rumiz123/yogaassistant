package com.rumiznellasery.yogahelper.ui.badges;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.data.Badge;

import java.util.ArrayList;
import java.util.List;

public class BadgesGridAdapter extends RecyclerView.Adapter<BadgesGridAdapter.BadgeViewHolder> {
    private List<Badge> badges = new ArrayList<>();

    public void setBadges(List<Badge> badges) {
        this.badges = badges;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_badge_grid, parent, false);
        return new BadgeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        holder.bind(badges.get(position));
        
        // Add entrance animation with delay based on position
        holder.itemView.setAlpha(0f);
        holder.itemView.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(position * 50L)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .start();
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    class BadgeViewHolder extends RecyclerView.ViewHolder {
        private final View badgeBackground;
        private final TextView badgeIcon;
        private final ImageView unlockIndicator;

        BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            badgeBackground = itemView.findViewById(R.id.badge_bg_circle);
            badgeIcon = itemView.findViewById(R.id.text_badge_icon);
            unlockIndicator = itemView.findViewById(R.id.icon_unlocked);
        }

        void bind(Badge badge) {
            if (badge.unlocked) {
                // Show unlocked badge
                badgeBackground.setBackgroundResource(R.drawable.badge_profile_circle_bg);
                badgeIcon.setVisibility(View.VISIBLE);
                badgeIcon.setText(badge.icon);
                unlockIndicator.setVisibility(View.VISIBLE);
                
                // Set background color based on rarity
                badgeBackground.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(badge.getRarityColor()));
            } else {
                // Show locked badge
                badgeBackground.setBackgroundResource(R.drawable.badge_placeholder);
                badgeIcon.setVisibility(View.GONE);
                unlockIndicator.setVisibility(View.GONE);
            }
        }
    }
} 