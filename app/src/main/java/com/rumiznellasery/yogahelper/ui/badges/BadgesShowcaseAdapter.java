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
            
            // Set badge icon based on badge ID for more specific mapping
            int iconRes = getBadgeIconById(badge.id);
            imageBadge.setImageResource(iconRes);
            
            // Set alpha based on unlock status
            float alpha = badge.unlocked ? 1.0f : 0.3f;
            imageBadge.setAlpha(alpha);
            textBadgeName.setAlpha(alpha);
            
            // Show/hide locked overlay
            overlayLocked.setVisibility(badge.unlocked ? View.GONE : View.VISIBLE);
        }
        
        private int getBadgeIconById(String badgeId) {
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

        private int getBadgeIcon(Badge.BadgeType type) {
            if (type == null) return R.drawable.ic_badge_first_workout;
            switch (type) {
                case WORKOUT_COUNT:
                    return R.drawable.ic_badge_first_workout;
                case STREAK_DAYS:
                    return R.drawable.ic_badge_week_warrior;
                case CALORIES_BURNED:
                    return R.drawable.ic_badge_week_warrior;
                case FRIENDS_COUNT:
                    return R.drawable.ic_badge_social_butterfly;
                case COMPETITION_WINS:
                    return R.drawable.ic_badge_champion;
                case PERFECT_WEEK:
                    return R.drawable.ic_badge_perfect_week;
                case POSE_MASTERY:
                    return R.drawable.ic_badge_pose_master;
                case WORKOUT_TIME:
                    return R.drawable.ic_badge_time_master;
                case CHALLENGE_COMPLETION:
                    return R.drawable.ic_badge_champion;
                default:
                    return R.drawable.ic_badge_first_workout;
            }
        }
    }
} 