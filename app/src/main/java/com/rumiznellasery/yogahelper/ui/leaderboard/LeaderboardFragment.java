package com.rumiznellasery.yogahelper.ui.leaderboard;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.rumiznellasery.yogahelper.databinding.FragmentLeaderboardBinding;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardFragment extends Fragment {

    private FragmentLeaderboardBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLeaderboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        List<String> names = new ArrayList<>();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getDisplayName() != null) {
            names.add(user.getDisplayName());
        } else {
            names.add("You");
        }
        names.add("YogiBot");
        names.add("StretchMaster");
        names.add("ZenAI");
        names.add("FlexBot");

        String[] ranks = new String[]{
                "Master Yogurt",
                "Platinum Puller",
                "Golden Gymnist",
                "Silver Streacher",
                "Bronze Beginner"
        };

        SharedPreferences prefs = requireContext().getSharedPreferences("stats", Context.MODE_PRIVATE);
        int workouts = prefs.getInt("workouts", 0);

        LinearLayout containerLayout = binding.containerRows;
        containerLayout.removeAllViews();
        for (int i = 0; i < names.size(); i++) {
            TextView tv = new TextView(requireContext());
            tv.setTextColor(getResources().getColor(android.R.color.white));
            String text = (i + 1) + ". " + names.get(i) + " - " + ranks[i];
            if (i == 0) {
                text += " (" + workouts + " workouts)";
            }
            tv.setText(text);
            tv.setTextSize(18);
            tv.setPadding(0, 8, 0, 8);
            containerLayout.addView(tv);
        }
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
