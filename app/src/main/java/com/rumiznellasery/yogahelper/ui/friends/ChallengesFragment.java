package com.rumiznellasery.yogahelper.ui.friends;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.data.Competition;
import com.rumiznellasery.yogahelper.utils.CompetitionManager;

import java.util.ArrayList;
import java.util.List;

public class ChallengesFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ChallengesAdapter adapter;
    private final List<Competition> challenges = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_challenges, container, false);
        recyclerView = v.findViewById(R.id.recyclerChallenges);
        progressBar = v.findViewById(R.id.progressBarChallenges);
        tvEmpty = v.findViewById(R.id.tvNoChallenges);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ChallengesAdapter(challenges);
        recyclerView.setAdapter(adapter);
        loadChallenges();
        return v;
    }

    private void loadChallenges() {
        progressBar.setVisibility(View.VISIBLE);
        CompetitionManager.loadUserCompetitions(requireContext(), new CompetitionManager.CompetitionCallback() {
            @Override
            public void onCompetitionsLoaded(List<Competition> competitions) {
                challenges.clear();
                for (Competition c : competitions) {
                    if ("friend_challenge".equals(c.type)) {
                        challenges.add(c);
                    }
                }
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(challenges.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setText(error);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }
} 