package com.rumiznellasery.yogahelper.data;

import java.util.HashMap;
import java.util.Map;

public class Competition {
    public String competitionId;
    public String creatorId;
    public String creatorName;
    public String title;
    public String description;
    public long startTime;
    public long endTime;
    public String status; // "active", "completed", "cancelled"
    public Map<String, Participant> participants;
    public String type; // "workout_count", "score", "streak"
    public int targetValue; // Target to reach (optional)
    
    public Competition() {
        // Required for Firebase
        participants = new HashMap<>();
    }
    
    public Competition(String creatorId, String creatorName, String title, String description, 
                      long startTime, long endTime, String type, int targetValue) {
        this.competitionId = java.util.UUID.randomUUID().toString();
        this.creatorId = creatorId;
        this.creatorName = creatorName;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = "active";
        this.type = type;
        this.targetValue = targetValue;
        this.participants = new HashMap<>();
        
        // Add creator as first participant
        Participant creator = new Participant();
        creator.userId = creatorId;
        creator.displayName = creatorName;
        creator.initialValue = 0;
        creator.currentValue = 0;
        participants.put(creatorId, creator);
    }
    
    public static class Participant {
        public String userId;
        public String displayName;
        public String photoUrl;
        public int initialValue;
        public int currentValue;
        public long lastUpdated;
        
        public Participant() {
            // Required for Firebase
        }
        
        public int getProgress() {
            return currentValue - initialValue;
        }
    }
} 