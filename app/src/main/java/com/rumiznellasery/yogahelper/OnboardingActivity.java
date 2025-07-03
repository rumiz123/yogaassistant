package com.rumiznellasery.yogahelper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rumiznellasery.yogahelper.data.DbKeys;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button nextButton, skipButton;
    private TextView stepIndicator;
    private int currentStep = 0;
    private final int TOTAL_STEPS = 4;
    
    // User data to collect
    private String userName = "";
    private String userPhotoUrl = "";
    private String referralSource = "";
    private String yogaLevel = "";
    
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_onboarding);
        
        // Initialize image picker
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            userPhotoUrl = uri.toString();
                            updateProfilePicture(uri);
                        }
                    }
                }
        );

        viewPager = findViewById(R.id.viewPager);
        nextButton = findViewById(R.id.nextButton);
        skipButton = findViewById(R.id.skipButton);
        stepIndicator = findViewById(R.id.stepIndicator);

        // Set up ViewPager
        OnboardingAdapter adapter = new OnboardingAdapter();
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false); // Disable swipe

        updateStepIndicator();
        setupButtons();
    }

    private void setupButtons() {
        nextButton.setOnClickListener(v -> {
            if (currentStep < TOTAL_STEPS - 1) {
                if (validateCurrentStep()) {
                    currentStep++;
                    viewPager.setCurrentItem(currentStep);
                    updateStepIndicator();
                    updateButtonText();
                }
            } else {
                // Final step - complete onboarding
                completeOnboarding();
            }
        });

        skipButton.setOnClickListener(v -> {
            // Skip to next step or complete if on last step
            if (currentStep < TOTAL_STEPS - 1) {
                currentStep++;
                viewPager.setCurrentItem(currentStep);
                updateStepIndicator();
                updateButtonText();
            } else {
                completeOnboarding();
            }
        });
    }

    private boolean validateCurrentStep() {
        switch (currentStep) {
            case 0: // Name step
                EditText nameEdit = findViewById(R.id.nameEditText);
                if (nameEdit != null) {
                    userName = nameEdit.getText().toString().trim();
                    if (userName.isEmpty()) {
                        Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
                break;
            case 1: // Profile picture step
                // Photo is optional, so always valid
                break;
            case 2: // Referral source step
                Spinner referralSpinner = findViewById(R.id.referralSpinner);
                if (referralSpinner != null) {
                    referralSource = referralSpinner.getSelectedItem().toString();
                }
                break;
            case 3: // Yoga level step
                Spinner levelSpinner = findViewById(R.id.levelSpinner);
                if (levelSpinner != null) {
                    yogaLevel = levelSpinner.getSelectedItem().toString();
                }
                break;
        }
        return true;
    }

    private void updateProfilePicture(Uri uri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                .setPhotoUri(uri)
                .build();

        user.updateProfile(req).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ImageView profilePic = findViewById(R.id.profilePicPreview);
                if (profilePic != null) {
                    profilePic.setImageURI(uri);
                }
                Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void completeOnboarding() {
        // Save all data to database
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DbKeys keys = DbKeys.get(this);
        DatabaseReference ref = FirebaseDatabase.getInstance(keys.databaseUrl)
                .getReference(keys.users)
                .child(user.getUid());

        // Update display name
        if (!userName.isEmpty()) {
            UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                    .setDisplayName(userName)
                    .build();
            user.updateProfile(req);
        }

        // Save to database
        ref.child(keys.displayName).setValue(userName);
        if (!userPhotoUrl.isEmpty()) {
            ref.child("photoUrl").setValue(userPhotoUrl);
        }
        ref.child("referralSource").setValue(referralSource);
        ref.child("yogaLevel").setValue(yogaLevel);
        ref.child(keys.workouts).setValue(0);
        ref.child(keys.totalWorkouts).setValue(0);
        ref.child(keys.calories).setValue(0);
        ref.child(keys.streak).setValue(0);
        ref.child(keys.score).setValue(0);
        ref.child(keys.level).setValue(1);

        Toast.makeText(this, "Welcome to Yoga Helper!", Toast.LENGTH_SHORT).show();
        
        // Navigate to main activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateStepIndicator() {
        stepIndicator.setText((currentStep + 1) + " of " + TOTAL_STEPS);
    }

    private void updateButtonText() {
        if (currentStep == TOTAL_STEPS - 1) {
            nextButton.setText("Get Started");
            skipButton.setVisibility(View.GONE);
        } else {
            nextButton.setText("Next");
            skipButton.setVisibility(View.VISIBLE);
        }
    }

    // ViewPager adapter
    private class OnboardingAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<OnboardingAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.onboarding_step, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            switch (position) {
                case 0:
                    setupNameStep(holder.itemView);
                    break;
                case 1:
                    setupPhotoStep(holder.itemView);
                    break;
                case 2:
                    setupReferralStep(holder.itemView);
                    break;
                case 3:
                    setupLevelStep(holder.itemView);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return TOTAL_STEPS;
        }

        private void setupNameStep(View view) {
            TextView title = view.findViewById(R.id.stepTitle);
            TextView description = view.findViewById(R.id.stepDescription);
            EditText nameEdit = view.findViewById(R.id.nameEditText);
            
            title.setText("What's your name?");
            description.setText("We'd love to know what to call you!");
            nameEdit.setHint("Enter your name");
            nameEdit.setVisibility(View.VISIBLE);
        }

        private void setupPhotoStep(View view) {
            TextView title = view.findViewById(R.id.stepTitle);
            TextView description = view.findViewById(R.id.stepDescription);
            Button selectPhotoBtn = view.findViewById(R.id.selectPhotoButton);
            Button noPhotoBtn = view.findViewById(R.id.noPhotoButton);
            ImageView profilePic = view.findViewById(R.id.profilePicPreview);
            LinearLayout profileSection = view.findViewById(R.id.profilePictureSection);
            
            title.setText("Add a profile picture");
            description.setText("Choose a photo or skip for now");
            profileSection.setVisibility(View.VISIBLE);
            
            selectPhotoBtn.setOnClickListener(v -> {
                Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                pick.setType("image/*");
                pickImageLauncher.launch(pick);
            });
            
            noPhotoBtn.setOnClickListener(v -> {
                userPhotoUrl = "";
                profilePic.setImageResource(R.drawable.ic_avatar_placeholder);
                Toast.makeText(OnboardingActivity.this, "No profile picture selected", Toast.LENGTH_SHORT).show();
            });
        }

        private void setupReferralStep(View view) {
            TextView title = view.findViewById(R.id.stepTitle);
            TextView description = view.findViewById(R.id.stepDescription);
            Spinner referralSpinner = view.findViewById(R.id.referralSpinner);
            
            title.setText("How did you hear about us?");
            description.setText("Help us understand how you found Yoga Helper");
            referralSpinner.setVisibility(View.VISIBLE);
            
            String[] referralOptions = {"Instagram", "YouTube", "Friend", "Other"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(OnboardingActivity.this, 
                    android.R.layout.simple_spinner_item, referralOptions);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            referralSpinner.setAdapter(adapter);
        }

        private void setupLevelStep(View view) {
            TextView title = view.findViewById(R.id.stepTitle);
            TextView description = view.findViewById(R.id.stepDescription);
            Spinner levelSpinner = view.findViewById(R.id.levelSpinner);
            
            title.setText("What's your yoga experience?");
            description.setText("This helps us personalize your experience");
            levelSpinner.setVisibility(View.VISIBLE);
            
            String[] levelOptions = {"Brand New", "Beginner", "Average", "Expert", "Professional"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(OnboardingActivity.this, 
                    android.R.layout.simple_spinner_item, levelOptions);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            levelSpinner.setAdapter(adapter);
        }

        class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
} 