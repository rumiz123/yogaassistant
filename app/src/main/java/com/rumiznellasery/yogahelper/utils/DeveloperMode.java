package com.rumiznellasery.yogahelper.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rumiznellasery.yogahelper.data.DbKeys;

import java.util.HashMap;
import java.util.Map;

public class DeveloperMode {
    private static final String PREFS_NAME = "developer_prefs";
    private static final String KEY_DEVELOPER_MODE = "developer_mode";
    private static final String KEY_DEVELOPER_EMAIL = "developer_email";
    
    // Developer email addresses (add your email here)
    private static final String[] DEVELOPER_EMAILS = {
        "your.email@example.com",  // Replace with actual developer emails
        "admin@yogahelper.com"
    };
    
    public static boolean isDeveloperMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_DEVELOPER_MODE, false);
    }
    
    public static void setDeveloperMode(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DEVELOPER_MODE, enabled).apply();
    }
    
    public static boolean isDeveloperEmail(String email) {
        if (email == null) return false;
        for (String devEmail : DEVELOPER_EMAILS) {
            if (devEmail.equalsIgnoreCase(email)) {
                return true;
            }
        }
        return false;
    }
    
    public static void checkAndEnableDeveloperMode(Context context) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String email = auth.getCurrentUser().getEmail();
            if (isDeveloperEmail(email)) {
                setDeveloperMode(context, true);
                Toast.makeText(context, "Developer mode enabled", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    public static void resetLeaderboard(Context context) {
        if (!isDeveloperMode(context)) {
            Toast.makeText(context, "Developer mode required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        DbKeys keys = DbKeys.get(context);
        DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl).getReference(keys.users);
        
        // Reset all scores to 0
        Map<String, Object> updates = new HashMap<>();
        ref.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                for (com.google.firebase.database.DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    if (userId != null) {
                        updates.put(userId + "/" + keys.score, 0);
                        updates.put(userId + "/" + keys.level, 1);
                        updates.put(userId + "/" + keys.streak, 0);
                        updates.put(userId + "/" + keys.totalWorkouts, 0);
                        updates.put(userId + "/" + keys.calories, 0);
                    }
                }
                
                if (!updates.isEmpty()) {
                    ref.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Leaderboard reset successfully", Toast.LENGTH_LONG).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Failed to reset leaderboard: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                }
            }
            
            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                Toast.makeText(context, "Failed to reset leaderboard: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    public static void updateUserScore(Context context, String userId, int newScore, int newLevel) {
        if (!isDeveloperMode(context)) {
            Toast.makeText(context, "Developer mode required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        DbKeys keys = DbKeys.get(context);
        DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
            .getReference(keys.users)
            .child(userId);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put(keys.score, newScore);
        updates.put(keys.level, newLevel);
        
        ref.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(context, "User score updated successfully", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(context, "Failed to update score: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
} 