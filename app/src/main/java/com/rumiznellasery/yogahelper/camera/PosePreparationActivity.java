package com.rumiznellasery.yogahelper.camera;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.rumiznellasery.yogahelper.R;

public class PosePreparationActivity extends AppCompatActivity {

    public static final String EXTRA_POSE_INDEX = "pose_index";
    public static final String EXTRA_TOTAL_POSES = "total_poses";
    public static final String EXTRA_POSE_NAME = "pose_name";
    public static final String EXTRA_POSE_DESCRIPTION = "pose_description";
    public static final String EXTRA_POSE_INSTRUCTIONS = "pose_instructions";
    public static final String EXTRA_POSE_DURATION = "pose_duration";

    private TextView poseNameText;
    private TextView poseDescriptionText;
    private TextView poseInstructionsText;
    private TextView poseProgressText;
    private Button startWorkoutButton;
    private Button backToHomeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_pose_preparation);
        
        poseNameText = findViewById(R.id.pose_name_text);
        poseDescriptionText = findViewById(R.id.pose_description_text);
        poseInstructionsText = findViewById(R.id.pose_instructions_text);
        poseProgressText = findViewById(R.id.pose_progress_text);
        startWorkoutButton = findViewById(R.id.start_workout_button);
        backToHomeButton = findViewById(R.id.btn_back_to_home);
        
        // Get pose data from intent
        int poseIndex = getIntent().getIntExtra(EXTRA_POSE_INDEX, 1);
        int totalPoses = getIntent().getIntExtra(EXTRA_TOTAL_POSES, 10);
        String poseName = getIntent().getStringExtra(EXTRA_POSE_NAME);
        String poseDescription = getIntent().getStringExtra(EXTRA_POSE_DESCRIPTION);
        String poseInstructions = getIntent().getStringExtra(EXTRA_POSE_INSTRUCTIONS);
        int poseDuration = getIntent().getIntExtra(EXTRA_POSE_DURATION, 30);
        
        // Set up the UI
        setupUI(poseIndex, totalPoses, poseName, poseDescription, poseInstructions, poseDuration);
        setupButtons();
        setupAnimations();
    }

    private void setupUI(int poseIndex, int totalPoses, String poseName, String poseDescription, 
                        String poseInstructions, int poseDuration) {
        poseNameText.setText(poseName);
        poseDescriptionText.setText(poseDescription);
        poseInstructionsText.setText(poseInstructions);
        poseProgressText.setText(String.format("Pose %d of %d", poseIndex, totalPoses));
    }

    private void setupButtons() {
        startWorkoutButton.setOnClickListener(v -> {
            // Start the camera activity with pose detection
            Intent intent = new Intent(this, CameraActivity.class);
            intent.putExtra(CameraActivity.EXTRA_POSE_INDEX, 
                getIntent().getIntExtra(EXTRA_POSE_INDEX, 1));
            intent.putExtra(CameraActivity.EXTRA_TOTAL_POSES, 
                getIntent().getIntExtra(EXTRA_TOTAL_POSES, 10));
            intent.putExtra(CameraActivity.EXTRA_POSE_NAME, 
                getIntent().getStringExtra(EXTRA_POSE_NAME));
            intent.putExtra(CameraActivity.EXTRA_POSE_DURATION, 
                getIntent().getIntExtra(EXTRA_POSE_DURATION, 30));
            startActivity(intent);
            finish();
        });

        backToHomeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.rumiznellasery.yogahelper.MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void setupAnimations() {
        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.scale_in);
        Animation bounceIn = AnimationUtils.loadAnimation(this, R.anim.bounce_in);

        // Apply entrance animations with staggered timing
        // Progress indicator
        poseProgressText.startAnimation(bounceIn);
        
        // Pose name
        poseNameText.startAnimation(fadeIn);
        poseNameText.getAnimation().setStartOffset(200);
        
        // Pose description
        poseDescriptionText.startAnimation(slideUp);
        poseDescriptionText.getAnimation().setStartOffset(400);
        
        // Pose instructions
        poseInstructionsText.startAnimation(slideUp);
        poseInstructionsText.getAnimation().setStartOffset(600);
        
        // Buttons
        backToHomeButton.startAnimation(slideUp);
        backToHomeButton.getAnimation().setStartOffset(800);
        
        startWorkoutButton.startAnimation(slideUp);
        startWorkoutButton.getAnimation().setStartOffset(1000);

        // Add button press animations
        setupButtonAnimations();
    }

    private void setupButtonAnimations() {
        // Back to home button
        backToHomeButton.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                Animation scaleOut = AnimationUtils.loadAnimation(this, R.anim.scale_out);
                scaleOut.setDuration(100);
                v.startAnimation(scaleOut);
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.scale_in);
                scaleIn.setDuration(100);
                v.startAnimation(scaleIn);
            }
            return false;
        });

        // Start workout button
        startWorkoutButton.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                Animation scaleOut = AnimationUtils.loadAnimation(this, R.anim.scale_out);
                scaleOut.setDuration(100);
                v.startAnimation(scaleOut);
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.scale_in);
                scaleIn.setDuration(100);
                v.startAnimation(scaleIn);
            }
            return false;
        });
    }
} 