package com.rumiznellasery.yogahelper.data;

public class Badge {
    public String id;
    public String title;
    public String description;
    public String icon;
    public int requirement;
    public int currentProgress;
    public boolean unlocked;
    public long unlockedDate;
    public BadgeType type;
    public BadgeRarity rarity;

    public Badge() {
        // Required for Firebase
    }

    public Badge(String id, String title, String description, String icon, 
                  int requirement, BadgeType type, BadgeRarity rarity) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.requirement = requirement;
        this.type = type;
        this.rarity = rarity;
        this.currentProgress = 0;
        this.unlocked = false;
        this.unlockedDate = 0;
    }

    public enum BadgeType {
        WORKOUT_COUNT,
        STREAK_DAYS,
        CALORIES_BURNED,
        FRIENDS_COUNT,
        COMPETITION_WINS,
        PERFECT_WEEK,
        POSE_MASTERY,
        WORKOUT_TIME,
        CHALLENGE_COMPLETION
    }

    public enum BadgeRarity {
        COMMON,
        UNCOMMON,
        RARE,
        EPIC,
        LEGENDARY
    }

    public void updateProgress(int progress) {
        this.currentProgress = progress;
        if (currentProgress >= requirement && !unlocked) {
            unlock();
        }
    }

    public void unlock() {
        this.unlocked = true;
        this.unlockedDate = System.currentTimeMillis();
    }

    public int getProgressPercentage() {
        if (requirement == 0) return 0;
        return Math.min(100, (currentProgress * 100) / requirement);
    }

    public boolean isNearCompletion() {
        return !unlocked && getProgressPercentage() >= 80;
    }

    public int getRarityColor() {
        switch (rarity) {
            case COMMON:
                return 0xFF808080; // Gray
            case UNCOMMON:
                return 0xFF4CAF50; // Green
            case RARE:
                return 0xFF2196F3; // Blue
            case EPIC:
                return 0xFF9C27B0; // Purple
            case LEGENDARY:
                return 0xFFFF9800; // Orange
            default:
                return 0xFF808080; // Gray
        }
    }

    public String getRarityName() {
        switch (rarity) {
            case COMMON:
                return "Common";
            case UNCOMMON:
                return "Uncommon";
            case RARE:
                return "Rare";
            case EPIC:
                return "Epic";
            case LEGENDARY:
                return "Legendary";
            default:
                return "Common";
        }
    }
} 