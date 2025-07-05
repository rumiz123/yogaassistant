package com.rumiznellasery.yogahelper.ui.leaderboard;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.databinding.FragmentLeaderboardBinding;
import com.rumiznellasery.yogahelper.data.DbKeys;
import com.rumiznellasery.yogahelper.utils.DeveloperMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LeaderboardFragment extends Fragment {
    private FragmentLeaderboardBinding binding;
    private LeaderboardAdapter adapter;
    private final List<Entry> entries = new ArrayList<>();
    private List<String> userIds = new ArrayList<>(); // Store user IDs for developer mode

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLeaderboardBinding.inflate(inflater, container, false);

        // setup RecyclerView
        RecyclerView rv = binding.rvLeaderboard;
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LeaderboardAdapter(requireContext(), entries, this::onItemLongClick);
        rv.setAdapter(adapter);

        // pull-to-refresh
        binding.swipeRefresh.setOnRefreshListener(this::loadData);
        
        // Check for developer mode
        DeveloperMode.checkAndEnableDeveloperMode(requireContext());
        
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
                userIds.clear();
                for (DataSnapshot c : snap.getChildren()) {
                    Entry e = new Entry();
                    e.name  = c.child(keys.displayName)
                               .getValue(String.class);
                    e.score = safeInt(c.child(keys.score)
                               .getValue(Long.class));
                    e.level = safeInt(c.child(keys.level)
                               .getValue(Long.class));
                    Boolean verified = c.child(keys.emailVerified)
                                         .getValue(Boolean.class);
                    e.verified = verified != null && verified;
                    e.photoUrl = c.child("photoUrl").getValue(String.class);
                    entries.add(e);
                    userIds.add(c.getKey()); // Store user ID for developer mode
                }
                // show highest score first
                Collections.reverse(entries);
                Collections.reverse(userIds); // Keep user IDs in sync
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
    
    private void onItemLongClick(int position) {
        if (!DeveloperMode.isDeveloperMode(requireContext())) {
            return;
        }
        
        showDeveloperOptionsDialog(position);
    }
    
    private void showDeveloperOptionsDialog(int position) {
        String[] options = {"Edit Score", "Reset Leaderboard"};
        
        new AlertDialog.Builder(requireContext())
            .setTitle("Developer Options")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        showEditScoreDialog(position);
                        break;
                    case 1:
                        showResetConfirmationDialog();
                        break;
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void showEditScoreDialog(int position) {
        if (position >= entries.size() || position >= userIds.size()) {
            return;
        }
        
        Entry entry = entries.get(position);
        String userId = userIds.get(position);
        
        View dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_user_score, null);
        
        TextView tvUserName = dialogView.findViewById(R.id.tvUserName);
        EditText etNewScore = dialogView.findViewById(R.id.etNewScore);
        EditText etNewLevel = dialogView.findViewById(R.id.etNewLevel);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnUpdate = dialogView.findViewById(R.id.btnUpdate);
        
        tvUserName.setText("User: " + (entry.name != null ? entry.name : "Unknown"));
        etNewScore.setText(String.valueOf(entry.score));
        etNewLevel.setText(String.valueOf(entry.level));
        
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create();
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnUpdate.setOnClickListener(v -> {
            try {
                int newScore = Integer.parseInt(etNewScore.getText().toString());
                int newLevel = Integer.parseInt(etNewLevel.getText().toString());
                
                DeveloperMode.updateUserScore(requireContext(), userId, newScore, newLevel);
                dialog.dismiss();
                
                // Reload data after update
                loadData();
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }
    
    private void showResetConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Reset Leaderboard")
            .setMessage("Are you sure you want to reset all scores to 0? This action cannot be undone.")
            .setPositiveButton("Reset", (dialog, which) -> {
                DeveloperMode.resetLeaderboard(requireContext());
                loadData(); // Reload data after reset
            })
            .setNegativeButton("Cancel", null)
            .show();
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
        boolean verified;
        String photoUrl;
    }
}
