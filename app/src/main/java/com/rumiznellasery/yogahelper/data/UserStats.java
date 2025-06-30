package com.rumiznellasery.yogahelper.data;

public class UserStats {
    public String displayName;
    public int workouts;
    public int score;
    public int level;

    public UserStats() {}

    public UserStats(String displayName, int workouts, int score, int level) {
        this.displayName = displayName;
        this.workouts = workouts;
        this.score = score;
        this.level = level;
    }
}
