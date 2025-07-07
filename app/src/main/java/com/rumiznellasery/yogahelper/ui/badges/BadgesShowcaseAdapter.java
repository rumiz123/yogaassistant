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

public class BadgesShowcaseAdapter extends RecyclerView.Adapter<BadgesShowcaseAdapter.BadgeViewHolder> {
    private List<Badge> badges = new ArrayList<>();
    private OnBadgeClickListener listener;

    public interface OnBadgeClickListener {
        void onBadgeClick(Badge badge);
    }

    public void setOnBadgeClickListener(OnBadgeClickListener listener) {
        this.listener = listener;
    }

    public void setBadges(List<Badge> badges) {
        this.badges = badges != null ? badges : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge_showcase, parent, false);
        return new BadgeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        Badge badge = badges.get(position);
        holder.bind(badge);
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    class BadgeViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageBadge;
        private TextView textBadgeName;
        private View overlayLocked;

        public BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            imageBadge = itemView.findViewById(R.id.image_badge);
            textBadgeName = itemView.findViewById(R.id.text_badge_name);
            overlayLocked = itemView.findViewById(R.id.overlay_locked);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onBadgeClick(badges.get(position));
                }
            });
        }

        public void bind(Badge badge) {
            textBadgeName.setText(badge.title);
            
            // Set badge icon based on type
            int iconRes = getBadgeIcon(badge.type);
            imageBadge.setImageResource(iconRes);
            
            // Set alpha based on unlock status
            float alpha = badge.unlocked ? 1.0f : 0.3f;
            imageBadge.setAlpha(alpha);
            textBadgeName.setAlpha(alpha);
            
            // Show/hide locked overlay
            overlayLocked.setVisibility(badge.unlocked ? View.GONE : View.VISIBLE);
        }

        private int getBadgeIcon(Badge.BadgeType type) {
            if (type == null) return R.drawable.ic_prize_black_24dp;
            switch (type) {
                case WORKOUT_COUNT:
                    return R.drawable.ic_prize_black_24dp;
                case STREAK_DAYS:
                    return R.drawable.ic_fire;
                case CALORIES_BURNED:
                    return R.drawable.ic_fire;
                case FRIENDS_COUNT:
                    return R.drawable.ic_friend_tab;
                case COMPETITION_WINS:
                    return R.drawable.ic_prize_black_24dp;
                case PERFECT_WEEK:
                    return R.drawable.ic_fire;
                case POSE_MASTERY:
                    return R.drawable.ic_prize_black_24dp;
                case WORKOUT_TIME:
                    return R.drawable.ic_notifications_black_24dp;
                case CHALLENGE_COMPLETION:
                    return R.drawable.ic_prize_black_24dp;
                default:
                    return R.drawable.ic_prize_black_24dp;
            }
        }
    }
} 