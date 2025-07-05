package com.rumiznellasery.yogahelper.data;

public class Friend {
    public String userId;
    public String displayName;
    public String photoUrl;
    public int score;
    public int level;
    public boolean verified;
    public String status; // "pending", "accepted", "blocked"
    public long timestamp;
    
    public Friend() {
        // Required for Firebase
    }
    
    public Friend(String userId, String displayName, String photoUrl, int score, int level, boolean verified) {
        this.userId = userId;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.score = score;
        this.level = level;
        this.verified = verified;
        this.status = "pending";
        this.timestamp = System.currentTimeMillis();
    }
} 