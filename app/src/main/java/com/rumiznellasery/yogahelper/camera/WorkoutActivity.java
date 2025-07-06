package com.rumiznellasery.yogahelper.camera;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.rumiznellasery.yogahelper.R;

public class WorkoutActivity extends AppCompatActivity {

    public static final String EXTRA_POSE_INDEX = "pose_index";
    public static final String EXTRA_TOTAL_POSES = "total_poses";
    public static final String EXTRA_POSE_NAME = "pose_name";
    public static final String EXTRA_POSE_DURATION = "pose_duration";

    private TextView workoutStatusText;
    private Button backToHomeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_workout);
        
        workoutStatusText = findViewById(R.id.workout_status_text);
        backToHomeButton = findViewById(R.id.btn_back_to_home);
        
        // Get pose information from intent
        int poseIndex = getIntent().getIntExtra(EXTRA_POSE_INDEX, 1);
        int totalPoses = getIntent().getIntExtra(EXTRA_TOTAL_POSES, 10);
        String poseName = getIntent().getStringExtra(EXTRA_POSE_NAME);
        int poseDuration = getIntent().getIntExtra(EXTRA_POSE_DURATION, 30);
        
        // Set up the workout display
        setupWorkoutDisplay(poseIndex, totalPoses, poseName, poseDuration);
        setupButtons();
    }

    private void setupWorkoutDisplay(int poseIndex, int totalPoses, String poseName, int poseDuration) {
        String statusText = String.format("Workout Started!\n\nPose %d of %d: %s\n\nDuration: %d seconds", 
            poseIndex, totalPoses, poseName, poseDuration);
        workoutStatusText.setText(statusText);
    }

    private void setupButtons() {
        backToHomeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.rumiznellasery.yogahelper.MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
} 