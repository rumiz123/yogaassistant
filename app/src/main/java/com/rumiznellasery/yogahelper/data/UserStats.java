package com.rumiznellasery.yogahelper.data;

public class UserStats {
    public String displayName;
    public int workouts;
    public int totalWorkouts;
    public int calories;
    public int streak;
    public int score;
    public int level;

    public UserStats() {}

    public UserStats(String displayName, int workouts, int totalWorkouts, int calories, int streak, int score, int level) {
        this.displayName = displayName;
        this.workouts = workouts;
        this.totalWorkouts = totalWorkouts;
        this.calories = calories;
        this.streak = streak;
        this.score = score;
        this.level = level;
    }
}
