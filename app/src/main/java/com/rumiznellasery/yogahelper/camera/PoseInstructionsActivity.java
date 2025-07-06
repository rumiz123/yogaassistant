package com.rumiznellasery.yogahelper.camera;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.rumiznellasery.yogahelper.R;

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
        
        updateInstruction();
        setupButtons();
    }

    private void setupButtons() {
        nextButton.setOnClickListener(v -> {
            if (currentStep < TOTAL_STEPS - 1) {
                currentStep++;
                updateInstruction();
            } else {
                // Start camera activity
                Intent intent = new Intent(this, CameraActivity.class);
                startActivity(intent);
                finish();
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