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
import com.rumiznellasery.yogahelper.data.Achievement;
import com.rumiznellasery.yogahelper.data.DbKeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AchievementManager {
    private static final String ACHIEVEMENTS_PREFS = "achievements";
    private static AchievementManager instance;
    private final Context context;
    private final List<Achievement> achievements = new ArrayList<>();
    private final Map<String, Achievement> achievementMap = new HashMap<>();

    private AchievementManager(Context context) {
        this.context = context.getApplicationContext();
        initializeAchievements();
    }

    public static AchievementManager getInstance(Context context) {
        if (instance == null) {
            instance = new AchievementManager(context);
        }
        return instance;
    }

    private void initializeAchievements() {
        // First Workout Achievement
        Achievement firstWorkout = new Achievement(
            "first_workout",
            context.getString(R.string.first_workout),
            context.getString(R.string.first_workout_description),
            "🎯",
            1,
            Achievement.AchievementType.WORKOUT_COUNT
        );

        // Week Warrior Achievement
        Achievement weekWarrior = new Achievement(
            "week_streak",
            context.getString(R.string.week_streak),
            context.getString(R.string.week_streak_description),
            "🔥",
            7,
            Achievement.AchievementType.STREAK_DAYS
        );

        // Monthly Master Achievement
        Achievement monthlyMaster = new Achievement(
            "month_streak",
            context.getString(R.string.month_streak),
            context.getString(R.string.month_streak_description),
            "👑",
            30,
            Achievement.AchievementType.STREAK_DAYS
        );

        // Century Club Achievement
        Achievement centuryClub = new Achievement(
            "hundred_workouts",
            context.getString(R.string.hundred_workouts),
            context.getString(R.string.hundred_workouts_description),
            "💎",
            100,
            Achievement.AchievementType.WORKOUT_COUNT
        );

        // Social Butterfly Achievement
        Achievement socialButterfly = new Achievement(
            "social_butterfly",
            context.getString(R.string.social_butterfly),
            context.getString(R.string.social_butterfly_description),
            "🦋",
            10,
            Achievement.AchievementType.FRIENDS_COUNT
        );

        // Champion Achievement
        Achievement champion = new Achievement(
            "competition_winner",
            context.getString(R.string.competition_winner),
            context.getString(R.string.competition_winner_description),
            "🏆",
            1,
            Achievement.AchievementType.COMPETITION_WINS
        );

        // Add achievements to lists
        achievements.add(firstWorkout);
        achievements.add(weekWarrior);
        achievements.add(monthlyMaster);
        achievements.add(centuryClub);
        achievements.add(socialButterfly);
        achievements.add(champion);

        // Create map for quick access
        for (Achievement achievement : achievements) {
            achievementMap.put(achievement.id, achievement);
        }
    }

    public void checkWorkoutAchievements(int totalWorkouts) {
        updateAchievementProgress("first_workout", totalWorkouts);
        updateAchievementProgress("hundred_workouts", totalWorkouts);
    }

    public void checkStreakAchievements(int currentStreak) {
        updateAchievementProgress("week_streak", currentStreak);
        updateAchievementProgress("month_streak", currentStreak);
    }

    public void checkFriendsAchievements(int friendsCount) {
        updateAchievementProgress("social_butterfly", friendsCount);
    }

    public void checkCompetitionAchievements(int wins) {
        updateAchievementProgress("competition_winner", wins);
    }

    private void updateAchievementProgress(String achievementId, int progress) {
        Achievement achievement = achievementMap.get(achievementId);
        if (achievement != null) {
            boolean wasUnlocked = achievement.unlocked;
            achievement.updateProgress(progress);
            
            if (!wasUnlocked && achievement.unlocked) {
                onAchievementUnlocked(achievement);
            }
        }
    }

    private void onAchievementUnlocked(Achievement achievement) {
        // Show notification
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("achievement_notifications", true);
        
        if (notificationsEnabled) {
            Toast.makeText(context, 
                "🏆 Achievement Unlocked: " + achievement.title + "! 🏆", 
                Toast.LENGTH_LONG).show();
        }

        // Save to Firebase
        saveAchievementToFirebase(achievement);
        
        // Save locally
        saveAchievementLocally(achievement);
    }

    private void saveAchievementToFirebase(Achievement achievement) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DbKeys keys = DbKeys.get(context);
            DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
                .getReference(keys.users)
                .child(user.getUid())
                .child("achievements")
                .child(achievement.id);

            Map<String, Object> achievementData = new HashMap<>();
            achievementData.put("unlocked", achievement.unlocked);
            achievementData.put("unlockedDate", achievement.unlockedDate);
            achievementData.put("currentProgress", achievement.currentProgress);

            ref.updateChildren(achievementData);
        }
    }

    private void saveAchievementLocally(Achievement achievement) {
        SharedPreferences prefs = context.getSharedPreferences(ACHIEVEMENTS_PREFS, Context.MODE_PRIVATE);
        prefs.edit()
            .putBoolean(achievement.id + "_unlocked", achievement.unlocked)
            .putLong(achievement.id + "_date", achievement.unlockedDate)
            .putInt(achievement.id + "_progress", achievement.currentProgress)
            .apply();
    }

    public void loadAchievementsFromFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DbKeys keys = DbKeys.get(context);
            DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
                .getReference(keys.users)
                .child(user.getUid())
                .child("achievements");

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    for (DataSnapshot achievementSnapshot : snapshot.getChildren()) {
                        String achievementId = achievementSnapshot.getKey();
                        Achievement achievement = achievementMap.get(achievementId);
                        
                        if (achievement != null) {
                            Boolean unlocked = achievementSnapshot.child("unlocked").getValue(Boolean.class);
                            Long unlockedDate = achievementSnapshot.child("unlockedDate").getValue(Long.class);
                            Integer progress = achievementSnapshot.child("currentProgress").getValue(Integer.class);
                            
                            if (unlocked != null) achievement.unlocked = unlocked;
                            if (unlockedDate != null) achievement.unlockedDate = unlockedDate;
                            if (progress != null) achievement.currentProgress = progress;
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Logger.error("Error loading achievements from Firebase", error.toException());
                }
            });
        }
    }

    public void loadAchievementsFromLocal() {
        SharedPreferences prefs = context.getSharedPreferences(ACHIEVEMENTS_PREFS, Context.MODE_PRIVATE);
        
        for (Achievement achievement : achievements) {
            boolean unlocked = prefs.getBoolean(achievement.id + "_unlocked", false);
            long unlockedDate = prefs.getLong(achievement.id + "_date", 0);
            int progress = prefs.getInt(achievement.id + "_progress", 0);
            
            achievement.unlocked = unlocked;
            achievement.unlockedDate = unlockedDate;
            achievement.currentProgress = progress;
        }
    }

    public List<Achievement> getAchievements() {
        return new ArrayList<>(achievements);
    }

    public List<Achievement> getUnlockedAchievements() {
        List<Achievement> unlocked = new ArrayList<>();
        for (Achievement achievement : achievements) {
            if (achievement.unlocked) {
                unlocked.add(achievement);
            }
        }
        return unlocked;
    }

    public List<Achievement> getLockedAchievements() {
        List<Achievement> locked = new ArrayList<>();
        for (Achievement achievement : achievements) {
            if (!achievement.unlocked) {
                locked.add(achievement);
            }
        }
        return locked;
    }

    public int getUnlockedCount() {
        int count = 0;
        for (Achievement achievement : achievements) {
            if (achievement.unlocked) count++;
        }
        return count;
    }

    public int getTotalCount() {
        return achievements.size();
    }

    public double getCompletionPercentage() {
        if (achievements.isEmpty()) return 0;
        return (double) getUnlockedCount() / achievements.size() * 100;
    }
} 