package com.rumiznellasery.yogahelper.data;

public class Achievement {
    public String id;
    public String title;
    public String description;
    public String icon;
    public int requirement;
    public int currentProgress;
    public boolean unlocked;
    public long unlockedDate;
    public AchievementType type;

    public Achievement() {
        // Required for Firebase
    }

    public Achievement(String id, String title, String description, String icon, 
                      int requirement, AchievementType type) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.requirement = requirement;
        this.type = type;
        this.currentProgress = 0;
        this.unlocked = false;
        this.unlockedDate = 0;
    }

    public enum AchievementType {
        WORKOUT_COUNT,
        STREAK_DAYS,
        CALORIES_BURNED,
        FRIENDS_COUNT,
        COMPETITION_WINS,
        PERFECT_WEEK
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
} 