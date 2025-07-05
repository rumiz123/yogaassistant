package com.rumiznellasery.yogahelper.ui.leaderboard;

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
import com.rumiznellasery.yogahelper.R;

import java.util.List;

public class LeaderboardAdapter
        extends RecyclerView.Adapter<LeaderboardAdapter.VH> {

  private final List<LeaderboardFragment.Entry> data;
  private final Context ctx;
  private final OnItemLongClickListener longClickListener;

  public interface OnItemLongClickListener {
    void onItemLongClick(int position);
  }

  public LeaderboardAdapter(Context ctx, List<LeaderboardFragment.Entry> data, OnItemLongClickListener longClickListener) {
    this.ctx = ctx;
    this.data = data;
    this.longClickListener = longClickListener;
  }

  @NonNull
  @Override
  public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(ctx)
            .inflate(R.layout.item_leaderboard_entry, parent, false);
    return new VH(v);
  }

  @Override
  public void onBindViewHolder(@NonNull VH h, int pos) {
    LeaderboardFragment.Entry e = data.get(pos);
    int rank = pos + 1;
    h.tvRank.setText(String.valueOf(rank));

    // medal color for top 3, default tint for others
    int colorRes = R.color.medal_bg;
    if (rank == 1) colorRes = R.color.medal_gold;
    else if (rank == 2) colorRes = R.color.medal_silver;
    else if (rank == 3) colorRes = R.color.medal_bronze;
    h.tvRank.getBackground()
            .setTint(ContextCompat.getColor(ctx, colorRes));

    // bind name, level & score
    h.tvName.setText(e.name != null ? e.name : "");
    h.ivVerified.setVisibility(e.verified ? View.VISIBLE : View.GONE);
    h.tvDetails.setText("Level " + e.level);
    h.tvScore.setText(String.valueOf(e.score));

    // Load avatar URL into ivAvatar using Glide
    if (e.photoUrl != null && !e.photoUrl.isEmpty()) {
        Glide.with(ctx)
            .load(e.photoUrl)
            .placeholder(R.drawable.ic_avatar_placeholder)
            .error(R.drawable.ic_avatar_placeholder)
            .circleCrop()
            .into(h.ivAvatar);
    } else {
        h.ivAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
    }
    
    // Set long click listener for developer mode
    h.itemView.setOnLongClickListener(v -> {
      if (longClickListener != null) {
        longClickListener.onItemLongClick(pos);
        return true;
      }
      return false;
    });
  }

  @Override
  public int getItemCount() {
    return data.size();
  }

  static class VH extends RecyclerView.ViewHolder {
    final TextView tvRank, tvName, tvDetails, tvScore;
    final ImageView ivAvatar, ivVerified;

    VH(View v) {
      super(v);
      tvRank     = v.findViewById(R.id.tvRank);
      tvName     = v.findViewById(R.id.tvName);
      tvDetails  = v.findViewById(R.id.tvDetails);
      tvScore    = v.findViewById(R.id.tvScore);
      ivAvatar   = v.findViewById(R.id.ivAvatar);
      ivVerified = v.findViewById(R.id.ivVerified);
    }
  }
}
