package com.rumiznellasery.yogahelper;

import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.rumiznellasery.yogahelper.databinding.ActivityMainBinding;
import com.rumiznellasery.yogahelper.utils.Logger;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.info("MainActivity onCreate started");

        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_workout,
                R.id.navigation_leaderboard,
                R.id.navigation_friends)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);
        Logger.info("MainActivity onCreate completed - Navigation setup finished");

        // Initialize badges
        initializeBadges();

        // Show flat grey overlay immediately and handle navigation
        navView.setOnItemSelectedListener(item -> {
            android.view.View overlayContainer = findViewById(R.id.overlay_container);
            android.view.View loadingOverlay = findViewById(R.id.loading_overlay);
            
            // Show flat grey overlay immediately
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(android.view.View.VISIBLE);
            }
            
            // Hide badges overlay if visible
            if (overlayContainer != null && overlayContainer.getVisibility() == android.view.View.VISIBLE) {
                overlayContainer.setVisibility(android.view.View.GONE);
                // Remove the badges fragment from the overlay container
                getSupportFragmentManager().popBackStack("badges", androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
            
            // Let the navigation controller handle the tab switch
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            
            // Hide loading overlay after navigation completes with smooth animation
            if (loadingOverlay != null) {
                loadingOverlay.postDelayed(() -> {
                    android.view.animation.Animation fadeOut = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fade_out);
                    fadeOut.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(android.view.animation.Animation animation) {}
                        
                        @Override
                        public void onAnimationEnd(android.view.animation.Animation animation) {
                            loadingOverlay.setVisibility(android.view.View.GONE);
                        }
                        
                        @Override
                        public void onAnimationRepeat(android.view.animation.Animation animation) {}
                    });
                    loadingOverlay.startAnimation(fadeOut);
                }, 125); // Shorter delay for faster response
            }
            
            return handled;
        });
    }

    @Override
    public void onBackPressed() {
        // Check if overlay container is visible
        android.view.View overlayContainer = findViewById(R.id.overlay_container);
        android.view.View loadingOverlay = findViewById(R.id.loading_overlay);
        
        if (overlayContainer != null && overlayContainer.getVisibility() == android.view.View.VISIBLE) {
            // Show loading overlay immediately with fade-in animation
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(android.view.View.VISIBLE);
                android.view.animation.Animation fadeIn = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fade_in);
                loadingOverlay.startAnimation(fadeIn);
            }
            
            // Hide overlay container
            overlayContainer.setVisibility(android.view.View.GONE);
            
            // Delay back navigation slightly to ensure loading overlay is visible
            loadingOverlay.post(() -> {
                super.onBackPressed();
                
                // Hide loading overlay with fade-out animation after navigation
                if (loadingOverlay != null) {
                    loadingOverlay.postDelayed(() -> {
                        android.view.animation.Animation fadeOut = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fade_out);
                        fadeOut.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(android.view.animation.Animation animation) {}
                            
                            @Override
                            public void onAnimationEnd(android.view.animation.Animation animation) {
                                loadingOverlay.setVisibility(android.view.View.GONE);
                            }
                            
                            @Override
                            public void onAnimationRepeat(android.view.animation.Animation animation) {}
                        });
                        loadingOverlay.startAnimation(fadeOut);
                    }, 200);
                }
            });
        } else {
            super.onBackPressed();
        }
    }

    private void initializeBadges() {
        try {
            // Initialize badge manager
            com.rumiznellasery.yogahelper.utils.BadgeManager badgeManager = 
                com.rumiznellasery.yogahelper.utils.BadgeManager.getInstance(this);
            
            // Load badges from Firebase first
            badgeManager.loadBadgesFromFirebase();
            
            // Save all badges to Firebase to ensure they're stored
            badgeManager.saveAllBadgesToFirebase();
            
            Logger.info("Badge initialization completed in MainActivity");
            
        } catch (Exception e) {
            Logger.error("Error initializing badges in MainActivity", e);
        }
    }

}