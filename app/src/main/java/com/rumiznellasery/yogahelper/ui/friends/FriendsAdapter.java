package com.rumiznellasery.yogahelper.ui.friends;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.data.Friend;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.VH> {

    private final List<Friend> data;
    private final Context ctx;
    private final OnFriendActionListener actionListener;

    public interface OnFriendActionListener {
        void onFriendAction(Friend friend, String action);
    }

    public FriendsAdapter(Context ctx, List<Friend> data, OnFriendActionListener actionListener) {
        this.ctx = ctx;
        this.data = data;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx)
                .inflate(R.layout.item_friend, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Friend friend = data.get(pos);
        
        // Set user info
        h.tvName.setText(friend.displayName != null ? friend.displayName : "Unknown");
        h.tvDetails.setText("Level " + friend.level + " • " + friend.score + " pts");
        h.ivVerified.setVisibility(friend.verified ? View.VISIBLE : View.GONE);
        
        // Load avatar
        if (friend.photoUrl != null && !friend.photoUrl.isEmpty()) {
            Glide.with(ctx)
                .load(friend.photoUrl)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .circleCrop()
                .into(h.ivAvatar);
        } else {
            h.ivAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
        }
        
        // Handle different friend statuses
        switch (friend.status) {
            case "pending":
                h.tvStatus.setText("Friend Request");
                h.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.warning_orange));
                h.btnAction1.setVisibility(View.VISIBLE);
                h.btnAction2.setVisibility(View.VISIBLE);
                h.btnAction1.setText("Accept");
                h.btnAction2.setText("Reject");
                h.btnAction1.setBackgroundResource(R.drawable.button_primary_gradient);
                h.btnAction2.setBackgroundResource(R.drawable.button_red_rounded);
                h.btnChallenge.setVisibility(View.GONE);
                break;
                
            case "accepted":
                h.tvStatus.setText("Friend");
                h.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.success_green));
                h.btnAction1.setVisibility(View.VISIBLE);
                h.btnAction2.setVisibility(View.VISIBLE);
                h.btnAction1.setText("Remove");
                h.btnAction2.setText("Challenge");
                h.btnAction1.setBackgroundResource(R.drawable.button_red_rounded);
                h.btnAction2.setBackgroundResource(R.drawable.button_primary_gradient);
                h.btnChallenge.setVisibility(View.VISIBLE);
                break;
                
            default:
                h.tvStatus.setText("Unknown");
                h.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary));
                h.btnAction1.setVisibility(View.GONE);
                h.btnAction2.setVisibility(View.GONE);
                h.btnChallenge.setVisibility(View.GONE);
                break;
        }
        
        // Set button click listeners
        h.btnAction1.setOnClickListener(v -> {
            if ("pending".equals(friend.status)) {
                actionListener.onFriendAction(friend, "accept");
            } else if ("accepted".equals(friend.status)) {
                actionListener.onFriendAction(friend, "remove");
            }
        });
        
        h.btnAction2.setOnClickListener(v -> {
            if ("pending".equals(friend.status)) {
                actionListener.onFriendAction(friend, "reject");
            }
        });
        
        h.btnChallenge.setOnClickListener(v -> {
            actionListener.onFriendAction(friend, "challenge");
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvName, tvDetails, tvStatus;
        final ImageView ivAvatar, ivVerified;
        final MaterialButton btnAction1, btnAction2, btnChallenge;

        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvDetails = v.findViewById(R.id.tvDetails);
            tvStatus = v.findViewById(R.id.tvStatus);
            ivAvatar = v.findViewById(R.id.ivAvatar);
            ivVerified = v.findViewById(R.id.ivVerified);
            btnAction1 = v.findViewById(R.id.btnAction1);
            btnAction2 = v.findViewById(R.id.btnAction2);
            btnChallenge = v.findViewById(R.id.btnChallenge);
        }
    }
} 