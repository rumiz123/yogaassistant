package com.rumiznellasery.yogahelper.ui.workout;

import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.rumiznellasery.yogahelper.databinding.FragmentWorkoutBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.rumiznellasery.yogahelper.data.DbKeys;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;
import com.rumiznellasery.yogahelper.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import com.rumiznellasery.yogahelper.utils.BadgeManager;
import com.rumiznellasery.yogahelper.utils.Logger;

public class WorkoutFragment extends Fragment {

    private FragmentWorkoutBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        WorkoutViewModel workoutViewModel =
                new ViewModelProvider(this).get(WorkoutViewModel.class);

        binding = FragmentWorkoutBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Hide/show bottom nav bar on scroll removed to keep navigation visible

        View.OnClickListener startWorkoutListener = v -> {
            SharedPreferences prefs = requireContext().getSharedPreferences("stats", Context.MODE_PRIVATE);
            int workouts = prefs.getInt("workouts", 0) + 1;
            int calories = prefs.getInt("calories", 0) + 50;

            // --- Robust Streak logic (UTC) ---
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            String todayStr = today.format(formatter);
            String lastWorkoutDateStr = prefs.getString("last_workout_date", "");
            int streak = prefs.getInt("streak", 0);
            boolean streakIncreased = false;
            if (lastWorkoutDateStr.equals(todayStr)) {
                // Already logged today, do not increment streak
            } else {
                LocalDate lastWorkoutDate = null;
                try {
                    lastWorkoutDate = LocalDate.parse(lastWorkoutDateStr, formatter);
                } catch (Exception ignored) {}
                if (lastWorkoutDate != null) {
                    long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastWorkoutDate, today);
                    if (daysBetween == 1) {
                        // Consecutive day, increment streak
                        streak += 1;
                        streakIncreased = true;
                    } else {
                        // Missed one or more days, reset streak
                        streak = 1;
                        streakIncreased = true;
                    }
                } else {
                    // First workout ever
                    streak = 1;
                    streakIncreased = true;
                }
            }
            // Save new stats
            prefs.edit()
                .putInt("workouts", workouts)
                .putInt("calories", calories)
                .putInt("streak", streak)
                .putString("last_workout_date", todayStr)
                .apply();

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                DbKeys keys = DbKeys.get(requireContext());
                DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
                        .getReference(keys.users).child(currentUser.getUid());
                ref.child(keys.workouts).setValue(ServerValue.increment(1));
                ref.child(keys.totalWorkouts).setValue(ServerValue.increment(1));
                ref.child(keys.calories).setValue(ServerValue.increment(50));
                if (streakIncreased) {
                    ref.child(keys.streak).setValue(streak);
                }
                ref.child(keys.score).setValue(ServerValue.increment(1));
            }

            // --- Weekly workouts logic ---
            Calendar calendar = Calendar.getInstance();
            int currentWeek = calendar.get(Calendar.WEEK_OF_YEAR);
            int savedWeek = prefs.getInt("workouts_week", -1);
            int workoutsThisWeek = prefs.getInt("workouts_this_week", 0);
            if (savedWeek != currentWeek) {
                // New week, reset
                workoutsThisWeek = 1;
                prefs.edit().putInt("workouts_week", currentWeek).putInt("workouts_this_week", 1).apply();
            } else {
                workoutsThisWeek += 1;
                prefs.edit().putInt("workouts_this_week", workoutsThisWeek).apply();
            }

            // Check for badges after updating stats
            checkAndAwardBadges(workouts, streak, calories);

            Intent intent = new Intent(requireContext(), com.rumiznellasery.yogahelper.camera.PoseInstructionsActivity.class);
            startActivity(intent);
        };

        binding.buttonPlaceholder1.setOnClickListener(startWorkoutListener);
        binding.buttonPlaceholder2.setOnClickListener(startWorkoutListener);
        binding.buttonPlaceholder3.setOnClickListener(startWorkoutListener);

        // Setup animations
        setupAnimations();
        
        // Setup workout enhancements
        setupWorkoutEnhancements();

        // Fetch yogaLevel from Firebase and highlight recommended workout
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("users").child(currentUser.getUid()).child("yogaLevel");
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String yogaLevel = snapshot.getValue(String.class);
                    int recommended = 1; // default to 1 if not found
                    if (yogaLevel != null) {
                        switch (yogaLevel) {
                            case "Brand New":
                            case "Beginner":
                                recommended = 1;
                                break;
                            case "Average":
                                recommended = 2;
                                break;
                            case "Expert":
                                recommended = 3;
                                break;
                            case "Professional":
                                recommended = 4;
                                break;
                        }
                    }
                    highlightRecommendedWorkout(recommended);
                    updateWorkoutRecommendations(yogaLevel);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        return root;
    }

    private void setupAnimations() {
        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up);
        Animation scaleIn = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_in);
        Animation bounceIn = AnimationUtils.loadAnimation(requireContext(), R.anim.bounce_in);

        // Animate header (Available Workouts title)
        if (getView() != null) {
            TextView header = getView().findViewById(R.id.text_workout_header);
            if (header != null) {
                header.startAnimation(fadeIn);
            }
        }

        // Animate workout cards with staggered timing
        int[] cardIds = {R.id.card_workout1, R.id.card_workout2, R.id.card_workout3};
        for (int i = 0; i < cardIds.length; i++) {
            if (getView() != null) {
                View card = getView().findViewById(cardIds[i]);
                if (card != null) {
                    card.startAnimation(slideUp);
                    card.getAnimation().setStartOffset(200 + (i * 200));
                }
            }
        }

        // Add button press animations
        setupButtonAnimations();
    }

    private void setupButtonAnimations() {
        // Start buttons
        View[] startButtons = {
            binding.buttonPlaceholder1,
            binding.buttonPlaceholder2,
            binding.buttonPlaceholder3
        };

        for (View button : startButtons) {
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
    }

    private void highlightRecommendedWorkout(int recommended) {
        int[] cardIds = {R.id.card_workout1, R.id.card_workout2, R.id.card_workout3};
        int[] labelIds = {R.id.label_recommended1, R.id.label_recommended2, R.id.label_recommended3};
        for (int i = 0; i < cardIds.length; i++) {
            MaterialCardView card = getView().findViewById(cardIds[i]);
            TextView label = getView().findViewById(labelIds[i]);
            if (card != null) {
                if (i == recommended - 1) {
                    card.setStrokeWidth(8);
                    card.setStrokeColor(getResources().getColor(R.color.teal_200));
                    if (label != null) label.setVisibility(View.VISIBLE);
                } else {
                    card.setStrokeWidth(0);
                    if (label != null) label.setVisibility(View.GONE);
                }
            }
        }
    }

    private void setupWorkoutEnhancements() {
        // Add workout tips
        showWorkoutTips();
        
        // Add recent activity
        showRecentActivity();
    }

    private void showWorkoutTips() {
        try {
            // Show helpful tips for better workouts
            String[] tips = {
                "💡 Tip: Warm up for 5 minutes before starting",
                "💡 Tip: Focus on your breath throughout the session",
                "💡 Tip: Don't push beyond your comfort zone",
                "💡 Tip: Stay hydrated during your practice"
            };
            
            // Create a tips card
            androidx.cardview.widget.CardView tipsCard = new androidx.cardview.widget.CardView(requireContext());
            tipsCard.setRadius(20);
            tipsCard.setElevation(8);
            tipsCard.setCardBackgroundColor(getResources().getColor(R.color.card_background));
            
            LinearLayout tipsLayout = new LinearLayout(requireContext());
            tipsLayout.setOrientation(LinearLayout.VERTICAL);
            tipsLayout.setPadding(24, 24, 24, 24);
            
            TextView tipsTitle = new TextView(requireContext());
            tipsTitle.setText("💡 Workout Tips");
            tipsTitle.setTextColor(getResources().getColor(R.color.text_primary));
            tipsTitle.setTextSize(18);
            tipsTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tipsTitle.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            tipsTitle.setPadding(0, 0, 0, 16);
            
            tipsLayout.addView(tipsTitle);
            
            // Add random tip
            String randomTip = tips[(int)(Math.random() * tips.length)];
            TextView tipText = new TextView(requireContext());
            tipText.setText(randomTip);
            tipText.setTextColor(getResources().getColor(R.color.text_secondary));
            tipText.setTextSize(14);
            tipText.setLineSpacing(4, 1);
            
            tipsLayout.addView(tipText);
            tipsCard.addView(tipsLayout);
            
            // Add to the main layout safely
            if (getView() != null) {
                View scrollView = getView().findViewById(R.id.scrollView);
                if (scrollView instanceof android.widget.ScrollView) {
                    android.widget.ScrollView sv = (android.widget.ScrollView) scrollView;
                    if (sv.getChildCount() > 0) {
                        View child = sv.getChildAt(0);
                        if (child instanceof LinearLayout) {
                            LinearLayout mainLayout = (LinearLayout) child;
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            params.setMargins(12, 12, 12, 12);
                            tipsCard.setLayoutParams(params);
                            mainLayout.addView(tipsCard);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Log error but don't crash
            android.util.Log.e("WorkoutFragment", "Error showing workout tips", e);
        }
    }

    private void showRecentActivity() {
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences("stats", Context.MODE_PRIVATE);
            int totalWorkouts = prefs.getInt("workouts", 0);
            
            if (totalWorkouts > 0) {
                // Show recent activity summary
                String lastWorkoutDate = prefs.getString("last_workout_date", "");
                if (!lastWorkoutDate.isEmpty()) {
                    // Create recent activity card
                    androidx.cardview.widget.CardView activityCard = new androidx.cardview.widget.CardView(requireContext());
                    activityCard.setRadius(20);
                    activityCard.setElevation(8);
                    activityCard.setCardBackgroundColor(getResources().getColor(R.color.theme_purple));
                    
                    LinearLayout activityLayout = new LinearLayout(requireContext());
                    activityLayout.setOrientation(LinearLayout.VERTICAL);
                    activityLayout.setPadding(24, 24, 24, 24);
                    
                    TextView activityTitle = new TextView(requireContext());
                    activityTitle.setText("🎯 Recent Activity");
                    activityTitle.setTextColor(getResources().getColor(android.R.color.white));
                    activityTitle.setTextSize(18);
                    activityTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                    activityTitle.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    activityTitle.setPadding(0, 0, 0, 8);
                    
                    TextView activityText = new TextView(requireContext());
                    activityText.setText("Last workout: " + lastWorkoutDate + "\nTotal workouts: " + totalWorkouts);
                    activityText.setTextColor(getResources().getColor(android.R.color.white));
                    activityText.setTextSize(14);
                    activityText.setAlpha(0.9f);
                    
                    activityLayout.addView(activityTitle);
                    activityLayout.addView(activityText);
                    activityCard.addView(activityLayout);
                    
                    // Add to the main layout safely
                    if (getView() != null) {
                        View scrollView = getView().findViewById(R.id.scrollView);
                        if (scrollView instanceof android.widget.ScrollView) {
                            android.widget.ScrollView sv = (android.widget.ScrollView) scrollView;
                            if (sv.getChildCount() > 0) {
                                View child = sv.getChildAt(0);
                                if (child instanceof LinearLayout) {
                                    LinearLayout mainLayout = (LinearLayout) child;
                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                    );
                                    params.setMargins(12, 12, 12, 12);
                                    activityCard.setLayoutParams(params);
                                    mainLayout.addView(activityCard);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Log error but don't crash
            android.util.Log.e("WorkoutFragment", "Error showing recent activity", e);
        }
    }

    private void updateWorkoutRecommendations(String yogaLevel) {
        try {
            String recommendation = "";
            
            if (yogaLevel == null || yogaLevel.equals("Brand New") || yogaLevel.equals("Beginner")) {
                recommendation = "Start with Workout 1 - it's perfect for beginners!";
            } else if (yogaLevel.equals("Average")) {
                recommendation = "Try Workout 2 to challenge yourself with intermediate poses.";
            } else if (yogaLevel.equals("Expert") || yogaLevel.equals("Professional")) {
                recommendation = "Workout 3 will push your limits with advanced sequences.";
            }
            
            // Show recommendation safely
            if (getView() != null) {
                TextView recommendationText = getView().findViewById(R.id.text_workout_header);
                if (recommendationText != null) {
                    recommendationText.setText("Available Workouts\n" + recommendation);
                    recommendationText.setTextSize(16);
                }
            }
        } catch (Exception e) {
            // Log error but don't crash
            android.util.Log.e("WorkoutFragment", "Error updating workout recommendations", e);
        }
    }

    private void checkAndAwardBadges(int workouts, int streak, int calories) {
        try {
            // Initialize badge manager
            BadgeManager badgeManager = BadgeManager.getInstance(requireContext());
            
            // Load existing badges from Firebase first
            badgeManager.loadBadgesFromFirebase();
            
            // Check workout count badges
            badgeManager.checkWorkoutBadges(workouts);
            
            // Check streak badges
            badgeManager.checkStreakBadges(streak);
            
            // Check time master badges (assuming 15 minutes per workout)
            int totalMinutes = workouts * 15;
            badgeManager.checkTimeMasterBadges(totalMinutes);
            
            // Check perfect week badges (if workouts this week = 7)
            SharedPreferences prefs = requireContext().getSharedPreferences("stats", Context.MODE_PRIVATE);
            int workoutsThisWeek = prefs.getInt("workouts_this_week", 0);
            if (workoutsThisWeek >= 7) {
                badgeManager.checkPerfectWeekBadges(workoutsThisWeek);
            }
            
            // Save all badges to Firebase to ensure they're stored
            badgeManager.saveAllBadgesToFirebase();
            
            Logger.info("Badge checking completed for workout. Total workouts: " + workouts + ", Streak: " + streak);
            
        } catch (Exception e) {
            Logger.error("Error checking badges in WorkoutFragment", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
