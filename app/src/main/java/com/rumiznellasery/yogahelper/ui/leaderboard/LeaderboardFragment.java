package com.rumiznellasery.yogahelper.ui.leaderboard;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLeaderboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DbKeys keys = DbKeys.get(requireContext());
        DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl).getReference(keys.users);
        LinearLayout containerLayout = binding.containerRows;
        containerLayout.removeAllViews();

        ref.orderByChild(keys.score).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Entry> entries = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = child.child(keys.displayName).getValue(String.class);
                    Long score = child.child(keys.score).getValue(Long.class);
                    Long level = child.child(keys.level).getValue(Long.class);
                    Entry e = new Entry();
                    e.name = name == null ? "" : name;
                    e.score = score == null ? 0 : score.intValue();
                    e.level = level == null ? 1 : level.intValue();
                    entries.add(e);
                }
                Collections.sort(entries, (a, b) -> b.score - a.score);
                containerLayout.removeAllViews();
                int rank = 1;
                for (Entry e : entries) {
                    TextView tv = new TextView(requireContext());
                    tv.setTextColor(getResources().getColor(android.R.color.white));
                    String text = rank + ". " + e.name + " - Level " + e.level + " (" + e.score + ")";
                    tv.setText(text);
                    tv.setTextSize(18);
                    tv.setPadding(0, 8, 0, 8);
                    containerLayout.addView(tv);
                    rank++;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class Entry {
        String name;
        int score;
        int level;
    }
}
