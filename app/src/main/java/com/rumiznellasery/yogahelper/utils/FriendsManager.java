package com.rumiznellasery.yogahelper.utils;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.rumiznellasery.yogahelper.data.DbKeys;
import com.rumiznellasery.yogahelper.data.Friend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendsManager {
    
    public interface FriendsCallback {
        void onFriendsLoaded(List<Friend> friends);
        void onError(String error);
    }
    
    public interface SearchCallback {
        void onUsersFound(List<Friend> users);
        void onError(String error);
    }
    
    public static void searchUsers(Context context, String searchQuery, SearchCallback callback) {
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            callback.onError("Search query cannot be empty");
            return;
        }
        
        DbKeys keys = DbKeys.get(context);
        DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl).getReference(keys.users);
        
        // Search by display name (case-insensitive)
        Query query = ref.orderByChild(keys.displayName)
                        .startAt(searchQuery.trim())
                        .endAt(searchQuery.trim() + "\uf8ff")
                        .limitToFirst(20);
        
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Friend> users = new ArrayList<>();
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    
                    // Skip current user
                    if (userId.equals(currentUserId)) {
                        continue;
                    }
                    
                    String displayName = userSnapshot.child(keys.displayName).getValue(String.class);
                    String photoUrl = userSnapshot.child("photoUrl").getValue(String.class);
                    Long score = userSnapshot.child(keys.score).getValue(Long.class);
                    Long level = userSnapshot.child(keys.level).getValue(Long.class);
                    Boolean verified = userSnapshot.child(keys.emailVerified).getValue(Boolean.class);
                    
                    // Only add if display name matches search query
                    if (displayName != null && displayName.toLowerCase().contains(searchQuery.toLowerCase())) {
                        Friend user = new Friend();
                        user.userId = userId;
                        user.displayName = displayName;
                        user.photoUrl = photoUrl;
                        user.score = score != null ? score.intValue() : 0;
                        user.level = level != null ? level.intValue() : 1;
                        user.verified = verified != null && verified;
                        user.status = "none"; // Not a friend yet
                        
                        users.add(user);
                    }
                }
                
                callback.onUsersFound(users);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError("Failed to search users: " + error.getMessage());
            }
        });
    }
    
    public static void sendFriendRequest(Context context, String targetUserId, String targetDisplayName) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            Toast.makeText(context, "You must be logged in", Toast.LENGTH_SHORT).show();
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
                Long score = snapshot.child(keys.score).getValue(Long.class);
                Long level = snapshot.child(keys.level).getValue(Long.class);
                Boolean verified = snapshot.child(keys.emailVerified).getValue(Boolean.class);
                
                // Create friend request
                Friend friendRequest = new Friend();
                friendRequest.userId = currentUserId;
                friendRequest.displayName = displayName;
                friendRequest.photoUrl = photoUrl;
                friendRequest.score = score != null ? score.intValue() : 0;
                friendRequest.level = level != null ? level.intValue() : 1;
                friendRequest.verified = verified != null && verified;
                friendRequest.status = "pending";
                friendRequest.timestamp = System.currentTimeMillis();
                
                // Add to target user's friend requests
                Map<String, Object> updates = new HashMap<>();
                updates.put("friends/" + targetUserId + "/" + currentUserId, friendRequest);
                
                ref.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Friend request sent to " + targetDisplayName, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to send friend request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(context, "Failed to send friend request: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    public static void acceptFriendRequest(Context context, String friendUserId) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            Toast.makeText(context, "You must be logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        DbKeys keys = DbKeys.get(context);
        DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl).getReference();
        
        // Update both users' friend status to accepted
        Map<String, Object> updates = new HashMap<>();
        updates.put("friends/" + currentUserId + "/" + friendUserId + "/status", "accepted");
        updates.put("friends/" + friendUserId + "/" + currentUserId + "/status", "accepted");
        
        ref.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(context, "Friend request accepted", Toast.LENGTH_SHORT).show();
                
                // Check for social badges after accepting friend request
                checkSocialBadges(context, currentUserId);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(context, "Failed to accept friend request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    public static void rejectFriendRequest(Context context, String friendUserId) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            Toast.makeText(context, "You must be logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        DbKeys keys = DbKeys.get(context);
        DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl).getReference();
        
        // Remove friend request from current user
        ref.child("friends").child(currentUserId).child(friendUserId).removeValue()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(context, "Friend request rejected", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(context, "Failed to reject friend request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    public static void removeFriend(Context context, String friendUserId) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            Toast.makeText(context, "You must be logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        DbKeys keys = DbKeys.get(context);
        DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl).getReference();
        
        // Remove friend from both users
        Map<String, Object> updates = new HashMap<>();
        updates.put("friends/" + currentUserId + "/" + friendUserId, null);
        updates.put("friends/" + friendUserId + "/" + currentUserId, null);
        
        ref.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(context, "Friend removed", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(context, "Failed to remove friend: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    public static void loadFriends(Context context, FriendsCallback callback) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            callback.onError("You must be logged in");
            return;
        }
        
        DbKeys keys = DbKeys.get(context);
        DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
            .getReference("friends")
            .child(currentUserId);
        
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Friend> friends = new ArrayList<>();
                
                for (DataSnapshot friendSnapshot : snapshot.getChildren()) {
                    Friend friend = friendSnapshot.getValue(Friend.class);
                    if (friend != null) {
                        friends.add(friend);
                    }
                }
                
                callback.onFriendsLoaded(friends);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError("Failed to load friends: " + error.getMessage());
            }
        });
    }
    
    private static void checkSocialBadges(Context context, String userId) {
        // Load friends to count them
        DatabaseReference ref = FirebaseDatabase.getInstance(DbKeys.get(context).databaseUrl)
            .getReference("friends")
            .child(userId);
        
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int acceptedFriends = 0;
                for (DataSnapshot friendSnapshot : snapshot.getChildren()) {
                    String status = friendSnapshot.child("status").getValue(String.class);
                    if ("accepted".equals(status)) {
                        acceptedFriends++;
                    }
                }
                
                // Check for social badges
                com.rumiznellasery.yogahelper.utils.BadgeManager badgeManager = 
                    com.rumiznellasery.yogahelper.utils.BadgeManager.getInstance(context);
                
                badgeManager.checkFriendsBadges(acceptedFriends);
                
                // Save badges to Firebase
                badgeManager.saveAllBadgesToFirebase();
                
                com.rumiznellasery.yogahelper.utils.Logger.info("Social badge check completed. Friends: " + acceptedFriends);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                com.rumiznellasery.yogahelper.utils.Logger.error("Error checking social badges", error.toException());
            }
        });
    }
} 