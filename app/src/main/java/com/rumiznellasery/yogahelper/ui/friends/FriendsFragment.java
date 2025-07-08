package com.rumiznellasery.yogahelper.ui.friends;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.data.Friend;
import com.rumiznellasery.yogahelper.databinding.FragmentFriendsBinding;
import com.rumiznellasery.yogahelper.utils.FriendsManager;
import com.rumiznellasery.yogahelper.utils.CompetitionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rumiznellasery.yogahelper.ui.friends.ChallengesFragment;

import java.util.ArrayList;
import java.util.List;

public class FriendsFragment extends Fragment {
    private FragmentFriendsBinding binding;
    private FriendsAdapter friendsAdapter;
    private SearchUsersAdapter searchAdapter;
    private final List<Friend> friends = new ArrayList<>();
    private final List<Friend> searchResults = new ArrayList<>();
    private boolean isSearchMode = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFriendsBinding.inflate(inflater, container, false);

        setupRecyclerViews();
        setupSearchFunctionality();
        setupTabButtons();
        setupCompetitionButton();
        setupAnimations();
        loadFriends();

        binding.btnViewChallenges.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment_activity_main, new ChallengesFragment())
                .addToBackStack("challenges")
                .commit();
        });

        return binding.getRoot();
    }

    private void setupRecyclerViews() {
        // Friends RecyclerView
        RecyclerView friendsRv = binding.rvFriends;
        friendsRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        friendsAdapter = new FriendsAdapter(requireContext(), friends, this::onFriendAction);
        friendsRv.setAdapter(friendsAdapter);

        // Search Results RecyclerView
        RecyclerView searchRv = binding.rvSearchResults;
        searchRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        searchAdapter = new SearchUsersAdapter(requireContext(), searchResults, this::onSearchUserAction);
        searchRv.setAdapter(searchAdapter);
    }

    private void setupSearchFunctionality() {
        EditText searchEditText = binding.etSearch;
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    searchUsers(query);
                } else {
                    clearSearchResults();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupTabButtons() {
        binding.btnFriends.setOnClickListener(v -> switchToFriendsTab());
        binding.btnAddFriends.setOnClickListener(v -> switchToAddFriendsTab());
        
        // Start with friends tab
        switchToFriendsTab();
    }

    private void switchToFriendsTab() {
        isSearchMode = false;
        binding.btnFriends.setBackgroundResource(R.drawable.button_primary_gradient);
        binding.btnAddFriends.setBackgroundResource(R.drawable.button_secondary_gradient);
        binding.btnFriends.setTextColor(getResources().getColor(R.color.white));
        binding.btnAddFriends.setTextColor(getResources().getColor(R.color.text_primary));
        
        binding.llFriends.setVisibility(View.VISIBLE);
        binding.llAddFriends.setVisibility(View.GONE);
    }

    private void switchToAddFriendsTab() {
        isSearchMode = true;
        binding.btnAddFriends.setBackgroundResource(R.drawable.button_primary_gradient);
        binding.btnFriends.setBackgroundResource(R.drawable.button_secondary_gradient);
        binding.btnAddFriends.setTextColor(getResources().getColor(R.color.white));
        binding.btnFriends.setTextColor(getResources().getColor(R.color.text_primary));
        
        binding.llFriends.setVisibility(View.GONE);
        binding.llAddFriends.setVisibility(View.VISIBLE);
    }

    private void loadFriends() {
        binding.progressBar.setVisibility(View.VISIBLE);
        FriendsManager.loadFriends(requireContext(), new FriendsManager.FriendsCallback() {
            @Override
            public void onFriendsLoaded(List<Friend> friendsList) {
                friends.clear();
                friends.addAll(friendsList);
                friendsAdapter.notifyDataSetChanged();
                binding.progressBar.setVisibility(View.GONE);
                
                updateFriendsStats();
                
                if (friends.isEmpty()) {
                    binding.llNoFriends.setVisibility(View.VISIBLE);
                } else {
                    binding.llNoFriends.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateFriendsStats() {
        int totalFriends = 0;
        int pendingRequests = 0;
        
        for (Friend friend : friends) {
            if ("accepted".equals(friend.status)) {
                totalFriends++;
            } else if ("pending".equals(friend.status)) {
                pendingRequests++;
            }
        }
        
        binding.tvTotalFriends.setText(String.valueOf(totalFriends));
        binding.tvPendingRequests.setText(String.valueOf(pendingRequests));
    }

    private void searchUsers(String query) {
        binding.progressBarSearch.setVisibility(View.VISIBLE);
        FriendsManager.searchUsers(requireContext(), query, new FriendsManager.SearchCallback() {
            @Override
            public void onUsersFound(List<Friend> users) {
                searchResults.clear();
                searchResults.addAll(users);
                searchAdapter.notifyDataSetChanged();
                binding.progressBarSearch.setVisibility(View.GONE);
                
                if (searchResults.isEmpty()) {
                    binding.llNoSearchResults.setVisibility(View.VISIBLE);
                } else {
                    binding.llNoSearchResults.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                binding.progressBarSearch.setVisibility(View.GONE);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearSearchResults() {
        searchResults.clear();
        searchAdapter.notifyDataSetChanged();
        binding.tvNoSearchResults.setVisibility(View.GONE);
    }

    private void onFriendAction(Friend friend, String action) {
        switch (action) {
            case "accept":
                acceptFriendRequest(friend);
                break;
            case "reject":
                rejectFriendRequest(friend);
                break;
            case "remove":
                showRemoveFriendDialog(friend);
                break;
            case "challenge":
                showFriendChallengeDialog(friend);
                break;
        }
    }

    private void onSearchUserAction(Friend user, String action) {
        if ("add".equals(action)) {
            sendFriendRequest(user);
        }
    }

    private void acceptFriendRequest(Friend friend) {
        FriendsManager.acceptFriendRequest(requireContext(), friend.userId);
        loadFriends(); // Reload friends list
    }

    private void rejectFriendRequest(Friend friend) {
        FriendsManager.rejectFriendRequest(requireContext(), friend.userId);
        loadFriends(); // Reload friends list
    }

    private void sendFriendRequest(Friend user) {
        FriendsManager.sendFriendRequest(requireContext(), user.userId, user.displayName);
    }

    private void showRemoveFriendDialog(Friend friend) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove Friend")
            .setMessage("Are you sure you want to remove " + friend.displayName + " from your friends?")
            .setPositiveButton("Remove", (dialog, which) -> {
                FriendsManager.removeFriend(requireContext(), friend.userId);
                loadFriends();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void setupCompetitionButton() {
        binding.btnCompetition.setOnClickListener(v -> {
            // Check if user has accepted friends
            List<Friend> acceptedFriends = new ArrayList<>();
            for (Friend friend : friends) {
                if ("accepted".equals(friend.status)) {
                    acceptedFriends.add(friend);
                }
            }
            
            if (acceptedFriends.isEmpty()) {
                Toast.makeText(requireContext(), "You need at least one friend to create a challenge!", Toast.LENGTH_LONG).show();
                return;
            }
            
            showCreateCompetitionDialog(acceptedFriends);
        });
    }
    
    private void showCreateCompetitionDialog(List<Friend> acceptedFriends) {
        // This would open the competition creation dialog
        // For now, just show a simple message
        Toast.makeText(requireContext(), "Competition feature coming soon! You have " + acceptedFriends.size() + " friends to challenge.", Toast.LENGTH_LONG).show();
    }

    private void showFriendChallengeDialog(Friend friend) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_competition, null);
        EditText etGoal = dialogView.findViewById(R.id.etCompetitionTitle);
        EditText etDescription = dialogView.findViewById(R.id.etCompetitionDescription);
        new MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Send Challenge", (dialog, which) -> {
                String goal = etGoal.getText().toString().trim();
                String desc = etDescription.getText().toString().trim();
                int target = 1; // Default target value
                long now = System.currentTimeMillis();
                long week = 7 * 24 * 60 * 60 * 1000L;
                CompetitionManager.createFriendChallenge(requireContext(), friend.userId, goal + (desc.isEmpty() ? "" : (": " + desc)), now, now + week, target, new CompetitionManager.CompetitionCreatedCallback() {
                    @Override
                    public void onCompetitionCreated(com.rumiznellasery.yogahelper.data.Competition competition) {
                        Toast.makeText(requireContext(), "Challenge sent!", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onError(String error) {
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void setupAnimations() {
        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up);
        Animation scaleIn = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_in);
        Animation bounceIn = AnimationUtils.loadAnimation(requireContext(), R.anim.bounce_in);

        // Animate header (Friends title)
        TextView header = null;
        if (getView() != null) {
            header = getView().findViewById(R.id.text_friends_header);
        }
        if (header != null) {
            header.startAnimation(fadeIn);
        }

        // Animate tab card (MaterialCardView)
        if (binding != null && binding.getRoot() != null) {
            View tabCard = binding.getRoot().findViewById(R.id.btnFriends);
            if (tabCard != null) {
                tabCard.startAnimation(slideUp);
                tabCard.getAnimation().setStartOffset(200);
            }
        }

        // Add button press animations
        setupButtonAnimations();
    }

    private void setupButtonAnimations() {
        // Tab buttons
        View[] tabButtons = {binding.btnFriends, binding.btnAddFriends};
        for (View button : tabButtons) {
            button.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    Animation scaleOut = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_out);
                    scaleOut.setDuration(75);
                    v.startAnimation(scaleOut);
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    Animation scaleIn = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_in);
                    scaleIn.setDuration(75);
                    v.startAnimation(scaleIn);
                }
                return false;
            });
        }

        // Competition button
        binding.btnCompetition.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                Animation scaleOut = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_out);
                scaleOut.setDuration(75);
                v.startAnimation(scaleOut);
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                Animation scaleIn = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_in);
                scaleIn.setDuration(75);
                v.startAnimation(scaleIn);
            }
            return false;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 