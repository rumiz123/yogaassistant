package com.rumiznellasery.yogahelper.ui.friends;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.data.Friend;

import java.util.List;

public class SearchUsersAdapter extends RecyclerView.Adapter<SearchUsersAdapter.VH> {

    private final List<Friend> data;
    private final Context ctx;
    private final OnSearchUserActionListener actionListener;

    public interface OnSearchUserActionListener {
        void onSearchUserAction(Friend user, String action);
    }

    public SearchUsersAdapter(Context ctx, List<Friend> data, OnSearchUserActionListener actionListener) {
        this.ctx = ctx;
        this.data = data;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx)
                .inflate(R.layout.item_search_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Friend user = data.get(pos);
        
        // Set user info
        h.tvName.setText(user.displayName != null ? user.displayName : "Unknown");
        h.tvDetails.setText("Level " + user.level + " • " + user.score + " pts");
        h.ivVerified.setVisibility(user.verified ? View.VISIBLE : View.GONE);
        
        // Load avatar
        if (user.photoUrl != null && !user.photoUrl.isEmpty()) {
            Glide.with(ctx)
                .load(user.photoUrl)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .circleCrop()
                .into(h.ivAvatar);
        } else {
            h.ivAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
        }
        
        // Set add friend button
        h.btnAddFriend.setOnClickListener(v -> {
            actionListener.onSearchUserAction(user, "add");
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvName, tvDetails;
        final ImageView ivAvatar, ivVerified;
        final MaterialButton btnAddFriend;

        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvDetails = v.findViewById(R.id.tvDetails);
            ivAvatar = v.findViewById(R.id.ivAvatar);
            ivVerified = v.findViewById(R.id.ivVerified);
            btnAddFriend = v.findViewById(R.id.btnAddFriend);
        }
    }
} 