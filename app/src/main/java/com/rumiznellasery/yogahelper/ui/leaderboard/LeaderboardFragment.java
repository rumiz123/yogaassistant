package com.rumiznellasery.yogahelper.ui.leaderboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rumiznellasery.yogahelper.databinding.FragmentLeaderboardBinding;
import com.rumiznellasery.yogahelper.data.DbKeys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LeaderboardFragment extends Fragment {
    private FragmentLeaderboardBinding binding;
    private LeaderboardAdapter adapter;
    private final List<Entry> entries = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLeaderboardBinding.inflate(inflater, container, false);

        // setup RecyclerView
        RecyclerView rv = binding.rvLeaderboard;
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LeaderboardAdapter(requireContext(), entries);
        rv.setAdapter(adapter);

        // pull-to-refresh
        binding.swipeRefresh.setOnRefreshListener(this::loadData);
        loadData();

        return binding.getRoot();
    }

    private void loadData() {
        binding.swipeRefresh.setRefreshing(true);
        DbKeys keys = DbKeys.get(requireContext());
        DatabaseReference ref = FirebaseDatabase
            .getInstance(keys.databaseUrl)
            .getReference(keys.users);

        ref.orderByChild(keys.score)
           .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                entries.clear();
                for (DataSnapshot c : snap.getChildren()) {
                    Entry e = new Entry();
                    e.name  = c.child(keys.displayName)
                               .getValue(String.class);
                    e.score = safeInt(c.child(keys.score)
                               .getValue(Long.class));
                    e.level = safeInt(c.child(keys.level)
                               .getValue(Long.class));
                    entries.add(e);
                }
                // show highest score first
                Collections.reverse(entries);
                adapter.notifyDataSetChanged();
                binding.swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError err) {
                binding.swipeRefresh.setRefreshing(false);
            }
        });
    }

    private int safeInt(Long v) {
        return v == null ? 0 : v.intValue();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // simple data holder for each row
    static class Entry {
        String name;
        int score;
        int level;
    }
}
