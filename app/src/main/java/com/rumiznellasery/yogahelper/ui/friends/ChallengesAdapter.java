package com.rumiznellasery.yogahelper.ui.friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.data.Competition;

import java.util.List;

public class ChallengesAdapter extends RecyclerView.Adapter<ChallengesAdapter.VH> {
    private final List<Competition> data;
    public ChallengesAdapter(List<Competition> data) { this.data = data; }
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_challenge, parent, false);
        return new VH(v);
    }
    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Competition c = data.get(pos);
        h.tvTitle.setText(c.title);
        h.tvGoal.setText(c.description);
        int progress = 0, target = c.targetValue;
        if (c.participants != null && c.participants.containsKey(c.creatorId)) {
            progress = c.participants.get(c.creatorId).currentValue;
        }
        h.progressBar.setMax(target);
        h.progressBar.setProgress(progress);
        h.tvProgress.setText(progress + "/" + target);
        h.tvStatus.setText(c.status);
    }
    @Override
    public int getItemCount() { return data.size(); }
    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle, tvGoal, tvProgress, tvStatus;
        final ProgressBar progressBar;
        VH(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvChallengeTitle);
            tvGoal = v.findViewById(R.id.tvChallengeGoal);
            tvProgress = v.findViewById(R.id.tvChallengeProgress);
            tvStatus = v.findViewById(R.id.tvChallengeStatus);
            progressBar = v.findViewById(R.id.progressBarChallenge);
        }
    }
} 