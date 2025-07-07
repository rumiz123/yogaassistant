package com.rumiznellasery.yogahelper.camera;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.camera.WorkoutSession;

public class PoseInstructionsActivity extends AppCompatActivity {

    private TextView instructionText;
    private Button nextButton;
    private Button backButton;
    private int currentStep = 0;
    private final int TOTAL_STEPS = 3;

    private final String[] instructions = {
        "Welcome to Yoga Pose Detection!\n\n" +
        "This feature uses your camera to analyze your yoga poses and provide real-time feedback.\n\n" +
        "Make sure you have good lighting and are clearly visible in the camera.",
        
        "Getting Started:\n\n" +
        "• Stand about 3-6 feet from your camera\n" +
        "• Ensure your full body is visible\n" +
        "• Wear form-fitting clothing for better detection\n" +
        "• Find a well-lit area\n\n" +
        "The app will detect your pose and show a skeleton overlay.",
        
        "Ready to Start!\n\n" +
        "• The camera will automatically start detecting your poses\n" +
        "• You'll see real-time feedback on your form\n" +
        "• Try different yoga poses to see the detection in action\n\n" +
        "Tap 'Start Detection' to begin!"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_pose_instructions);
        
        instructionText = findViewById(R.id.instruction_text);
        nextButton = findViewById(R.id.next_button);
        backButton = findViewById(R.id.back_button);
        
        // Setup animations
        setupAnimations();
        
        updateInstruction();
        setupButtons();
    }

    private void setupAnimations() {
        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.scale_in);
        Animation bounceIn = AnimationUtils.loadAnimation(this, R.anim.bounce_in);

        // Apply entrance animations with staggered timing
        // Title
        TextView titleText = findViewById(R.id.title_text);
        titleText.startAnimation(fadeIn);
        
        // Instruction text
        instructionText.startAnimation(slideUp);
        instructionText.getAnimation().setStartOffset(200);
        
        // Buttons
        backButton.startAnimation(slideUp);
        backButton.getAnimation().setStartOffset(400);
        
        nextButton.startAnimation(slideUp);
        nextButton.getAnimation().setStartOffset(600);

        // Add button press animations
        setupButtonAnimations();
    }

    private void setupButtonAnimations() {
        // Back button
        backButton.setOnTouchListener((v, event) -> {
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

        // Next button
        nextButton.setOnTouchListener((v, event) -> {
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

    private void setupButtons() {
        nextButton.setOnClickListener(v -> {
            if (currentStep < TOTAL_STEPS - 1) {
                currentStep++;
                updateInstruction();
            } else {
                // Start the pose preparation activity for the first pose
                Intent intent = new Intent(this, PosePreparationActivity.class);
                intent.putExtra(PosePreparationActivity.EXTRA_POSE_INDEX, 1);
                intent.putExtra(PosePreparationActivity.EXTRA_TOTAL_POSES, 10);
                
                // Get the first pose data from WorkoutSession
                WorkoutSession workoutSession = new WorkoutSession(this, null);
                WorkoutSession.Pose firstPose = workoutSession.getPoseByIndex(0);
                if (firstPose != null) {
                    intent.putExtra(PosePreparationActivity.EXTRA_POSE_NAME, firstPose.name);
                    intent.putExtra(PosePreparationActivity.EXTRA_POSE_DESCRIPTION, firstPose.description);
                    intent.putExtra(PosePreparationActivity.EXTRA_POSE_INSTRUCTIONS, firstPose.instructions);
                    intent.putExtra(PosePreparationActivity.EXTRA_POSE_DURATION, firstPose.durationSeconds);
                }
                
                startActivity(intent);
            }
        });

        backButton.setOnClickListener(v -> {
            if (currentStep > 0) {
                currentStep--;
                updateInstruction();
            } else {
                // Go back to previous activity
                finish();
            }
        });
    }

    private void updateInstruction() {
        instructionText.setText(instructions[currentStep]);
        
        // Update button text
        if (currentStep == 0) {
            backButton.setText("Back");
        } else {
            backButton.setText("Previous");
        }
        
        if (currentStep == TOTAL_STEPS - 1) {
            nextButton.setText("Start Detection");
        } else {
            nextButton.setText("Next");
        }
    }
} 