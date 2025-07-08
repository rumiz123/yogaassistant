package com.rumiznellasery.yogahelper.utils;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rumiznellasery.yogahelper.data.Competition;
import com.rumiznellasery.yogahelper.data.DbKeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompetitionManager {
    
    public interface CompetitionCallback {
        void onCompetitionsLoaded(List<Competition> competitions);
        void onError(String error);
    }
    
    public interface CompetitionCreatedCallback {
        void onCompetitionCreated(Competition competition);
        void onError(String error);
    }
    
    public static void createCompetition(Context context, String title, String description, 
                                       long startTime, long endTime, String type, int targetValue,
                                       List<String> friendIds, CompetitionCreatedCallback callback) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            callback.onError("You must be logged in");
            return;
        }
        
        // Get current user's display name
        DbKeys keys = DbKeys.get(context);
        DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl).getReference();
        
        ref.child(keys.users).child(currentUserId).child(keys.displayName)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    String creatorName = snapshot.getValue(String.class);
                    if (creatorName == null) {
                        creatorName = "Unknown User";
                    }
                    
                    // Create competition
                    Competition competition = new Competition(currentUserId, creatorName, title, 
                                                            description, startTime, endTime, type, targetValue);
                    
                    // Add friends as participants
                    for (String friendId : friendIds) {
                        Competition.Participant participant = new Competition.Participant();
                        participant.userId = friendId;
                        participant.initialValue = 0;
                        participant.currentValue = 0;
                        participant.lastUpdated = System.currentTimeMillis();
                        competition.participants.put(friendId, participant);
                    }
                    
                    // Save to database
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("competitions/" + competition.competitionId, competition);
                    
                    // Add competition to each participant's competitions list
                    for (String participantId : competition.participants.keySet()) {
                        updates.put("userCompetitions/" + participantId + "/" + competition.competitionId, 
                                  competition.competitionId);
                    }
                    
                    ref.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            callback.onCompetitionCreated(competition);
                            Toast.makeText(context, "Competition created successfully!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            callback.onError("Failed to create competition: " + e.getMessage());
                        });
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    callback.onError("Failed to get user info: " + error.getMessage());
                }
            });
    }
    
    public static void loadUserCompetitions(Context context, CompetitionCallback callback) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            callback.onError("You must be logged in");
            return;
        }
        
        DbKeys keys = DbKeys.get(context);
        DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
            .getReference("userCompetitions")
            .child(currentUserId);
        
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<String> competitionIds = new ArrayList<>();
                for (DataSnapshot compSnapshot : snapshot.getChildren()) {
                    String competitionId = compSnapshot.getValue(String.class);
                    if (competitionId != null) {
                        competitionIds.add(competitionId);
                    }
                }
                
                if (competitionIds.isEmpty()) {
                    callback.onCompetitionsLoaded(new ArrayList<>());
                    return;
                }
                
                // Load competition details
                loadCompetitionDetails(context, competitionIds, callback);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError("Failed to load competitions: " + error.getMessage());
            }
        });
    }
    
    private static void loadCompetitionDetails(Context context, List<String> competitionIds, 
                                             CompetitionCallback callback) {
        DbKeys keys = DbKeys.get(context);
        DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
            .getReference("competitions");
        
        final List<Competition> competitions = new ArrayList<>();
        final int[] loadedCount = {0};
        
        for (String competitionId : competitionIds) {
            ref.child(competitionId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    Competition competition = snapshot.getValue(Competition.class);
                    if (competition != null) {
                        competitions.add(competition);
                    }
                    
                    loadedCount[0]++;
                    if (loadedCount[0] == competitionIds.size()) {
                        callback.onCompetitionsLoaded(competitions);
                    }
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    loadedCount[0]++;
                    if (loadedCount[0] == competitionIds.size()) {
                        callback.onCompetitionsLoaded(competitions);
                    }
                }
            });
        }
    }
    
    public static void updateCompetitionProgress(Context context, String competitionId, int newValue) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            Toast.makeText(context, "You must be logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        DbKeys keys = DbKeys.get(context);
        DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
            .getReference("competitions")
            .child(competitionId)
            .child("participants")
            .child(currentUserId);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("currentValue", newValue);
        updates.put("lastUpdated", System.currentTimeMillis());
        
        ref.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                // Success - progress updated
            })
            .addOnFailureListener(e -> {
                Toast.makeText(context, "Failed to update progress: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    public static void joinCompetition(Context context, String competitionId, CompetitionCreatedCallback callback) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            callback.onError("You must be logged in");
            return;
        }
        
        DbKeys keys = DbKeys.get(context);
        DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl).getReference();
        
        // Get current user's info
        ref.child(keys.users).child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String displayName = snapshot.child(keys.displayName).getValue(String.class);
                String photoUrl = snapshot.child("photoUrl").getValue(String.class);
                
                if (displayName == null) {
                    displayName = "Unknown User";
                }
                
                // Add user to competition
                Competition.Participant participant = new Competition.Participant();
                participant.userId = currentUserId;
                participant.displayName = displayName;
                participant.photoUrl = photoUrl;
                participant.initialValue = 0;
                participant.currentValue = 0;
                participant.lastUpdated = System.currentTimeMillis();
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("competitions/" + competitionId + "/participants/" + currentUserId, participant);
                updates.put("userCompetitions/" + currentUserId + "/" + competitionId, competitionId);
                
                ref.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Joined competition successfully!", Toast.LENGTH_SHORT).show();
                        callback.onCompetitionCreated(null);
                    })
                    .addOnFailureListener(e -> {
                        callback.onError("Failed to join competition: " + e.getMessage());
                    });
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError("Failed to get user info: " + error.getMessage());
            }
        });
    }
    
    public static void createFriendChallenge(Context context, String friendId, String goalDescription, long startTime, long endTime, int targetValue, CompetitionCreatedCallback callback) {
        List<String> friendIds = new ArrayList<>();
        friendIds.add(friendId);
        String title = "Friend Challenge";
        String description = goalDescription;
        String type = "friend_challenge";
        createCompetition(context, title, description, startTime, endTime, type, targetValue, friendIds, callback);
    }
} 