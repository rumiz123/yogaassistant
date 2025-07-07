package com.rumiznellasery.yogahelper.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.data.Badge;
import com.rumiznellasery.yogahelper.data.DbKeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BadgeManager {
    private static final String BADGES_PREFS = "badges";
    private static BadgeManager instance;
    private final Context context;
    private final List<Badge> badges = new ArrayList<>();
    private final Map<String, Badge> badgeMap = new HashMap<>();

    private BadgeManager(Context context) {
        this.context = context.getApplicationContext();
        initializeBadges();
    }

    public static BadgeManager getInstance(Context context) {
        if (instance == null) {
            instance = new BadgeManager(context);
        }
        return instance;
    }

    private void initializeBadges() {
        // Beginner Badges
        Badge firstWorkout = new Badge(
            "first_workout",
            context.getString(R.string.badge_first_workout),
            context.getString(R.string.badge_first_workout_description),
            "🔰",
            1,
            Badge.BadgeType.WORKOUT_COUNT,
            Badge.BadgeRarity.COMMON
        );

        Badge weekWarrior = new Badge(
            "week_streak",
            context.getString(R.string.badge_week_warrior),
            context.getString(R.string.badge_week_warrior_description),
            "🔥",
            7,
            Badge.BadgeType.STREAK_DAYS,
            Badge.BadgeRarity.UNCOMMON
        );

        Badge monthlyMaster = new Badge(
            "month_streak",
            context.getString(R.string.badge_monthly_master),
            context.getString(R.string.badge_monthly_master_description),
            "👑",
            30,
            Badge.BadgeType.STREAK_DAYS,
            Badge.BadgeRarity.RARE
        );

        // Milestone Badges
        Badge centuryClub = new Badge(
            "hundred_workouts",
            context.getString(R.string.badge_century_club),
            context.getString(R.string.badge_century_club_description),
            "💎",
            100,
            Badge.BadgeType.WORKOUT_COUNT,
            Badge.BadgeRarity.EPIC
        );

        Badge socialButterfly = new Badge(
            "social_butterfly",
            context.getString(R.string.badge_social_butterfly),
            context.getString(R.string.badge_social_butterfly_description),
            "🦋",
            10,
            Badge.BadgeType.FRIENDS_COUNT,
            Badge.BadgeRarity.UNCOMMON
        );

        Badge champion = new Badge(
            "competition_winner",
            context.getString(R.string.badge_champion),
            context.getString(R.string.badge_champion_description),
            "🏆",
            1,
            Badge.BadgeType.COMPETITION_WINS,
            Badge.BadgeRarity.RARE
        );

        // Advanced Badges
        Badge poseMaster = new Badge(
            "pose_master",
            context.getString(R.string.badge_pose_master),
            context.getString(R.string.badge_pose_master_description),
            "🧘",
            50,
            Badge.BadgeType.POSE_MASTERY,
            Badge.BadgeRarity.EPIC
        );

        Badge timeMaster = new Badge(
            "time_master",
            context.getString(R.string.badge_time_master),
            context.getString(R.string.badge_time_master_description),
            "⏰",
            60,
            Badge.BadgeType.WORKOUT_TIME,
            Badge.BadgeRarity.RARE
        );

        Badge perfectWeek = new Badge(
            "perfect_week",
            context.getString(R.string.badge_perfect_week),
            context.getString(R.string.badge_perfect_week_description),
            "⭐",
            7,
            Badge.BadgeType.PERFECT_WEEK,
            Badge.BadgeRarity.LEGENDARY
        );

        // Add badges to lists
        badges.add(firstWorkout);
        badges.add(weekWarrior);
        badges.add(monthlyMaster);
        badges.add(centuryClub);
        badges.add(socialButterfly);
        badges.add(champion);
        badges.add(poseMaster);
        badges.add(timeMaster);
        badges.add(perfectWeek);

        // Create map for quick access
        for (Badge badge : badges) {
            badgeMap.put(badge.id, badge);
        }
    }

    public void checkWorkoutBadges(int totalWorkouts) {
        updateBadgeProgress("first_workout", totalWorkouts);
        updateBadgeProgress("hundred_workouts", totalWorkouts);
    }

    public void checkStreakBadges(int currentStreak) {
        updateBadgeProgress("week_streak", currentStreak);
        updateBadgeProgress("month_streak", currentStreak);
    }

    public void checkFriendsBadges(int friendsCount) {
        updateBadgeProgress("social_butterfly", friendsCount);
    }

    public void checkCompetitionBadges(int wins) {
        updateBadgeProgress("competition_winner", wins);
    }

    public void checkPoseMasteryBadges(int perfectPoses) {
        updateBadgeProgress("pose_master", perfectPoses);
    }

    public void checkTimeMasterBadges(int totalMinutes) {
        updateBadgeProgress("time_master", totalMinutes);
    }

    public void checkPerfectWeekBadges(int perfectDays) {
        updateBadgeProgress("perfect_week", perfectDays);
    }

    private void updateBadgeProgress(String badgeId, int progress) {
        Badge badge = badgeMap.get(badgeId);
        if (badge != null) {
            boolean wasUnlocked = badge.unlocked;
            badge.updateProgress(progress);
            
            if (!wasUnlocked && badge.unlocked) {
                onBadgeUnlocked(badge);
            }
        }
    }

    private void onBadgeUnlocked(Badge badge) {
        // Show notification
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("badge_notifications", true);
        
        if (notificationsEnabled) {
            String rarityEmoji = getRarityEmoji(badge.rarity);
            Toast.makeText(context, 
                rarityEmoji + " Badge Unlocked: " + badge.title + "! " + rarityEmoji, 
                Toast.LENGTH_LONG).show();
        }

        // Save to Firebase
        saveBadgeToFirebase(badge);
        
        // Save locally
        saveBadgeLocally(badge);
    }

    private String getRarityEmoji(Badge.BadgeRarity rarity) {
        switch (rarity) {
            case COMMON:
                return "🔰";
            case UNCOMMON:
                return "🟢";
            case RARE:
                return "🔵";
            case EPIC:
                return "🟣";
            case LEGENDARY:
                return "🟠";
            default:
                return "🔰";
        }
    }

    private void saveBadgeToFirebase(Badge badge) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DbKeys keys = DbKeys.get(context);
            DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
                .getReference(keys.users)
                .child(user.getUid())
                .child(keys.badges)
                .child(badge.id);

            Map<String, Object> badgeData = new HashMap<>();
            badgeData.put("id", badge.id);
            badgeData.put("title", badge.title);
            badgeData.put("description", badge.description);
            badgeData.put("icon", badge.icon);
            badgeData.put("requirement", badge.requirement);
            badgeData.put("unlocked", badge.unlocked);
            badgeData.put("unlockedDate", badge.unlockedDate);
            badgeData.put("currentProgress", badge.currentProgress);
            badgeData.put("type", badge.type.name());
            badgeData.put("rarity", badge.rarity.name());

            ref.setValue(badgeData).addOnSuccessListener(aVoid -> {
                Logger.info("Badge " + badge.id + " saved to Firebase successfully");
            }).addOnFailureListener(e -> {
                Logger.error("Failed to save badge " + badge.id + " to Firebase", e);
            });
        }
    }

    public void saveAllBadgesToFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DbKeys keys = DbKeys.get(context);
            DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
                .getReference(keys.users)
                .child(user.getUid())
                .child(keys.badges);

            for (Badge badge : badges) {
                Map<String, Object> badgeData = new HashMap<>();
                badgeData.put("id", badge.id);
                badgeData.put("title", badge.title);
                badgeData.put("description", badge.description);
                badgeData.put("icon", badge.icon);
                badgeData.put("requirement", badge.requirement);
                badgeData.put("unlocked", badge.unlocked);
                badgeData.put("unlockedDate", badge.unlockedDate);
                badgeData.put("currentProgress", badge.currentProgress);
                badgeData.put("type", badge.type.name());
                badgeData.put("rarity", badge.rarity.name());

                ref.child(badge.id).setValue(badgeData);
            }
            Logger.info("All badges saved to Firebase");
        }
    }

    public void saveBadgeLocally(Badge badge) {
        SharedPreferences prefs = context.getSharedPreferences(BADGES_PREFS, Context.MODE_PRIVATE);
        prefs.edit()
            .putBoolean(badge.id + "_unlocked", badge.unlocked)
            .putLong(badge.id + "_date", badge.unlockedDate)
            .putInt(badge.id + "_progress", badge.currentProgress)
            .apply();
    }

    public void loadBadgesFromFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DbKeys keys = DbKeys.get(context);
            DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
                .getReference(keys.users)
                .child(user.getUid())
                .child(keys.badges);

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    for (DataSnapshot badgeSnapshot : snapshot.getChildren()) {
                        String badgeId = badgeSnapshot.getKey();
                        Badge badge = badgeMap.get(badgeId);
                        
                        if (badge != null) {
                            Boolean unlocked = badgeSnapshot.child("unlocked").getValue(Boolean.class);
                            Long unlockedDate = badgeSnapshot.child("unlockedDate").getValue(Long.class);
                            Integer progress = badgeSnapshot.child("currentProgress").getValue(Integer.class);
                            
                            if (unlocked != null) badge.unlocked = unlocked;
                            if (unlockedDate != null) badge.unlockedDate = unlockedDate;
                            if (progress != null) badge.currentProgress = progress;
                            
                            // Also save locally for offline access
                            saveBadgeLocally(badge);
                        }
                    }
                    Logger.info("Loaded " + snapshot.getChildrenCount() + " badges from Firebase");
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Logger.error("Error loading badges from Firebase", error.toException());
                }
            });
        }
    }

    public void loadBadgesFromLocal() {
        SharedPreferences prefs = context.getSharedPreferences(BADGES_PREFS, Context.MODE_PRIVATE);
        
        for (Badge badge : badges) {
            boolean unlocked = prefs.getBoolean(badge.id + "_unlocked", false);
            long unlockedDate = prefs.getLong(badge.id + "_date", 0);
            int progress = prefs.getInt(badge.id + "_progress", 0);
            
            badge.unlocked = unlocked;
            badge.unlockedDate = unlockedDate;
            badge.currentProgress = progress;
        }
    }

    public List<Badge> getBadges() {
        return new ArrayList<>(badges);
    }

    public List<Badge> getUnlockedBadges() {
        List<Badge> unlocked = new ArrayList<>();
        for (Badge badge : badges) {
            if (badge.unlocked) {
                unlocked.add(badge);
            }
        }
        return unlocked;
    }

    public List<Badge> getLockedBadges() {
        List<Badge> locked = new ArrayList<>();
        for (Badge badge : badges) {
            if (!badge.unlocked) {
                locked.add(badge);
            }
        }
        return locked;
    }

    public List<Badge> getBadgesByRarity(Badge.BadgeRarity rarity) {
        List<Badge> filtered = new ArrayList<>();
        for (Badge badge : badges) {
            if (badge.rarity == rarity) {
                filtered.add(badge);
            }
        }
        return filtered;
    }

    public int getUnlockedCount() {
        int count = 0;
        for (Badge badge : badges) {
            if (badge.unlocked) count++;
        }
        return count;
    }

    public int getTotalCount() {
        return badges.size();
    }

    public double getCompletionPercentage() {
        if (badges.isEmpty()) return 0;
        return (double) getUnlockedCount() / badges.size() * 100;
    }

    public int getRarityCount(Badge.BadgeRarity rarity) {
        int count = 0;
        for (Badge badge : badges) {
            if (badge.rarity == rarity && badge.unlocked) {
                count++;
            }
        }
        return count;
    }
} 