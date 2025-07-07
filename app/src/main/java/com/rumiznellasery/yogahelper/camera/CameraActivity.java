package com.rumiznellasery.yogahelper.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.WindowManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.MainActivity;
import com.google.common.util.concurrent.ListenableFuture;
import com.rumiznellasery.yogahelper.camera.PoseDetector.PoseLandmarkerResult;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity implements MediaPipePoseDetector.PoseDetectionCallback, WorkoutSession.WorkoutListener {
    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private final String[] REQUIRED_PERMISSIONS = new String[]{ Manifest.permission.CAMERA };
    
    // Extras for pose information
    public static final String EXTRA_POSE_INDEX = "pose_index";
    public static final String EXTRA_TOTAL_POSES = "total_poses";
    public static final String EXTRA_POSE_NAME = "pose_name";
    public static final String EXTRA_POSE_DURATION = "pose_duration";
    
    private PreviewView previewView;
    private ImageView poseOverlayView;
    private TextView poseStatusText;
    private TextView circularTimerText;
    private TextView poseFeedbackText;
    private TextView tipsText;
    private Button backToInstructionsButton;
    private Button backToHomeButton;
    private MediaPipePoseDetector poseDetector;
    private WorkoutSession workoutSession;
    private ExecutorService cameraExecutor;
    private boolean isPoseDetectionEnabled = true; // Always enabled now
    
    // Current pose information
    private int currentPoseIndex = 1;
    private int totalPoses = 10;
    private String currentPoseName = "";
    private int currentPoseDuration = 30;
    
    // Pose validation variables
    private boolean isCorrectPose = false;
    private int correctPoseTime = 0;
    private Handler poseTimerHandler;
    private Runnable poseTimerRunnable;
    
    // Tips timer variables
    private Handler tipsTimerHandler;
    private Runnable tipsTimerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        // Keep screen on during workout
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setContentView(R.layout.activity_camera);
        
        // Initialize views
        previewView = findViewById(R.id.view_finder);
        poseOverlayView = findViewById(R.id.pose_overlay);
        poseStatusText = findViewById(R.id.pose_status_text);
        circularTimerText = findViewById(R.id.circular_timer_text);
        poseFeedbackText = findViewById(R.id.pose_feedback_text);
        tipsText = findViewById(R.id.tips_text);
        backToInstructionsButton = findViewById(R.id.btn_back_to_instructions);
        backToHomeButton = findViewById(R.id.btn_back_to_home);
        
        // Get pose information from intent
        currentPoseIndex = getIntent().getIntExtra(EXTRA_POSE_INDEX, 1);
        totalPoses = getIntent().getIntExtra(EXTRA_TOTAL_POSES, 10);
        currentPoseName = getIntent().getStringExtra(EXTRA_POSE_NAME);
        currentPoseDuration = getIntent().getIntExtra(EXTRA_POSE_DURATION, 30);
        
        // Initialize MediaPipe pose detector
        poseDetector = new MediaPipePoseDetector(this, this);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialize workout session
        workoutSession = new WorkoutSession(this, this);

        // Setup animations
        setupAnimations();

        // Set up back to instructions button
        backToInstructionsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PoseInstructionsActivity.class);
            startActivity(intent);
        });

        // Set up back to home button
        backToHomeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        if (allPermissionsGranted()) {
            startCamera();
            // Start the current pose
            startCurrentPose();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }
    
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Preview use case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Image analysis use case for pose detection
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                
                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                // Front-facing camera selector
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                // Bind to lifecycle
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }
    
    private void analyzeImage(ImageProxy imageProxy) {
        if (poseDetector != null) {
            // Convert ImageProxy to Bitmap for MediaPipe pose detection
            Bitmap bitmap = imageProxyToBitmap(imageProxy);
            if (bitmap != null) {
                long timestamp = System.currentTimeMillis();
                poseDetector.detectPose(bitmap, timestamp);
            }
        }
        imageProxy.close();
    }
    
    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        try {
            ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();
            
            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();
            
            byte[] nv21 = new byte[ySize + uSize + vSize];
            
            // U and V are swapped
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);
            
            YuvImage yuvImage = new YuvImage(nv21, android.graphics.ImageFormat.NV21, imageProxy.getWidth(), imageProxy.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new android.graphics.Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
            byte[] imageBytes = out.toByteArray();
            
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            // Rotate the bitmap to match the device orientation (portrait)
            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
            if (rotationDegrees != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationDegrees);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
            // Mirror the bitmap horizontally for front camera to fix left/right confusion
            Matrix mirrorMatrix = new Matrix();
            mirrorMatrix.setScale(-1, 1); // Flip horizontally
            mirrorMatrix.postTranslate(bitmap.getWidth(), 0); // Move back to positive coordinates
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mirrorMatrix, true);
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e);
            return null;
        }
    }

    @Override
    public void onPoseDetected(PoseDetector.PoseLandmarkerResult result, Bitmap poseOverlay) {
        runOnUiThread(() -> {
            if (result.landmarks().isEmpty()) {
                // Don't override workout instructions if no person detected
                if (!workoutSession.isActive()) {
                    poseStatusText.setText("No person detected\nMove into camera view");
                }
                poseOverlayView.setImageBitmap(null); // Clear overlay
                // Stop timer if no person detected
                stopPoseTimer();
                isCorrectPose = false;
            } else {
                // Use the YogaPoseAnalyzer for better pose detection
                YogaPoseAnalyzer.PoseAnalysis analysis = YogaPoseAnalyzer.analyzePose(result);
                
                // Update detected pose display
                String detectedPoseName = analysis.poseName != null ? analysis.poseName : "Unknown";
                int confidencePercent = (int) (analysis.confidence * 100);
                String statusText = String.format("Pose %d of %d: %s\nDetected: %s (%d%%)", 
                    currentPoseIndex, totalPoses, currentPoseName, detectedPoseName, confidencePercent);
                
                // Check if the current pose matches the expected pose
                boolean poseMatches = validateCurrentPose(analysis, currentPoseName);
                
                if (poseMatches && !isCorrectPose) {
                    // Pose just became correct, start timer
                    isCorrectPose = true;
                    startPoseTimer();
                    poseStatusText.setText(statusText);
                    poseFeedbackText.setText("✅ Correct Pose!");
                    updateCircularTimer(currentPoseDuration - correctPoseTime);
                } else if (!poseMatches && isCorrectPose) {
                    // Pose became incorrect, stop timer
                    isCorrectPose = false;
                    stopPoseTimer();
                    poseStatusText.setText(statusText);
                    poseFeedbackText.setText("❌ Adjust your pose");
                    updateCircularTimer(currentPoseDuration - correctPoseTime);
                } else if (poseMatches && isCorrectPose) {
                    // Pose still correct, update timer display
                    poseStatusText.setText(statusText);
                    poseFeedbackText.setText("✅ Correct Pose!");
                    updateCircularTimer(currentPoseDuration - correctPoseTime);
                } else {
                    // Pose still incorrect
                    poseStatusText.setText(statusText);
                    poseFeedbackText.setText("❌ Adjust your pose");
                    updateCircularTimer(currentPoseDuration - correctPoseTime);
                }
                
                // Show pose feedback as a toast if confidence is low, but don't override workout instructions
                if (analysis.confidence < 0.5f && !analysis.feedback.isEmpty()) {
                    Toast.makeText(this, "Pose Feedback: " + analysis.feedback, Toast.LENGTH_SHORT).show();
                }
                
                // Set the pose overlay (skeleton only, not the camera frame)
                poseOverlayView.setImageBitmap(poseOverlay);
            }
        });
    }
    
    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            Log.e(TAG, "Pose detection error: " + error);
            Toast.makeText(this, getString(R.string.pose_detection_error) + ": " + error, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove screen wake lock
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Clean up pose timer
        stopPoseTimer();
        
        // Clean up tips timer
        if (tipsTimerHandler != null && tipsTimerRunnable != null) {
            tipsTimerHandler.removeCallbacks(tipsTimerRunnable);
        }
        
        if (poseDetector != null) {
            poseDetector.close();
        }
        if (workoutSession != null) {
            workoutSession.stopWorkout();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }

    // WorkoutSession.WorkoutListener implementation
    @Override
    public void onPoseStarted(WorkoutSession.Pose pose, int poseIndex, int totalPoses) {
        runOnUiThread(() -> {
            String statusText = String.format("Pose %d of %d: %s\n\nTime: %d seconds", 
                poseIndex, totalPoses, pose.name, pose.durationSeconds);
            poseStatusText.setText(statusText);
        });
    }

    @Override
    public void onPoseProgress(WorkoutSession.Pose pose, int secondsRemaining) {
        runOnUiThread(() -> {
            String currentText = poseStatusText.getText().toString();
            String[] lines = currentText.split("\n");
            if (lines.length >= 2) {
                lines[1] = "Time: " + secondsRemaining + " seconds";
                poseStatusText.setText(String.join("\n", lines));
            }
        });
    }

    @Override
    public void onPoseCompleted(WorkoutSession.Pose pose, int poseIndex) {
        runOnUiThread(() -> {
            poseStatusText.setText(String.format("Pose %d completed!\n\nGreat job!", poseIndex));
        });
    }

    @Override
    public void onWorkoutCompleted() {
        runOnUiThread(() -> {
            poseStatusText.setText("Workout Complete!\n\nGreat job! You've completed all poses.\n\nTap 'Back to Home' to finish.");
            Toast.makeText(this, "Congratulations! Workout completed!", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onWorkoutPaused() {
        runOnUiThread(() -> {
            poseStatusText.setText("Workout Paused\n\nTap to resume");
        });
    }

    @Override
    public void onWorkoutResumed() {
        // Resume the current pose
        startCurrentPose();
    }
    
    @Override
    public void onLaunchPosePreparation(WorkoutSession.Pose pose, int poseIndex, int totalPoses) {
        // This will be handled by the PosePreparationActivity
        // The CameraActivity will receive the pose data via intent extras
    }

    private void startCurrentPose() {
        // Initialize pose validation variables
        isCorrectPose = false;
        correctPoseTime = 0;
        
        // Set up the initial pose display
        poseStatusText.setText(String.format("Pose %d of %d: %s", currentPoseIndex, totalPoses, currentPoseName));
        poseFeedbackText.setText("❌ Get into the correct pose");
        updateCircularTimer(currentPoseDuration);
        updateTips(currentPoseName);
        
        // Start the pose timer (it will only count when correct pose is detected)
        startPoseTimer();
    }
    
    private void startPoseTimer() {
        // Initialize timer variables
        correctPoseTime = 0;
        poseTimerHandler = new Handler();
        
        poseTimerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isCorrectPose && correctPoseTime < currentPoseDuration) {
                    correctPoseTime++;
                    updatePoseTimer(correctPoseTime);
                    poseTimerHandler.postDelayed(this, 1000);
                } else if (correctPoseTime >= currentPoseDuration) {
                    // Pose completed, launch next pose preparation
                    launchNextPosePreparation();
                } else {
                    // Pose not correct, stop timer
                    stopPoseTimer();
                }
            }
        };
        
        // Start the timer
        poseTimerHandler.postDelayed(poseTimerRunnable, 1000);
    }
    
    private void updatePoseTimer(int secondsRemaining) {
        runOnUiThread(() -> {
            // Update the circular timer
            updateCircularTimer(currentPoseDuration - secondsRemaining);
        });
    }
    
    private void launchNextPosePreparation() {
        runOnUiThread(() -> {
            if (currentPoseIndex < totalPoses) {
                // Launch next pose preparation
                Intent intent = new Intent(this, PosePreparationActivity.class);
                intent.putExtra(PosePreparationActivity.EXTRA_POSE_INDEX, currentPoseIndex + 1);
                intent.putExtra(PosePreparationActivity.EXTRA_TOTAL_POSES, totalPoses);
                // Get pose data from WorkoutSession
                WorkoutSession.Pose nextPose = workoutSession.getPoseByIndex(currentPoseIndex);
                if (nextPose != null) {
                    intent.putExtra(PosePreparationActivity.EXTRA_POSE_NAME, nextPose.name);
                    intent.putExtra(PosePreparationActivity.EXTRA_POSE_DESCRIPTION, nextPose.description);
                    intent.putExtra(PosePreparationActivity.EXTRA_POSE_INSTRUCTIONS, nextPose.instructions);
                    intent.putExtra(PosePreparationActivity.EXTRA_POSE_DURATION, nextPose.durationSeconds);
                }
                startActivity(intent);
                finish();
            } else {
                // Workout completed
                poseStatusText.setText("Workout Complete!\n\nGreat job! You've completed all poses.\n\nTap 'Back to Home' to finish.");
                Toast.makeText(this, "Congratulations! Workout completed!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateCurrentPose(YogaPoseAnalyzer.PoseAnalysis analysis, String expectedPoseName) {
        // Simple pose validation - check if the detected pose matches the expected pose
        // and has sufficient confidence
        if (analysis.confidence < 0.6f) {
            return false; // Not confident enough
        }
        
        // Check if the detected pose name matches the expected pose
        String detectedPose = analysis.poseName.toLowerCase();
        String expectedPose = expectedPoseName.toLowerCase();
        
        // Handle different pose name variations
        if (expectedPose.contains("mountain") && detectedPose.contains("mountain")) {
            return true;
        } else if (expectedPose.contains("cobra") && detectedPose.contains("cobra")) {
            return true;
        } else if (expectedPose.contains("tree") && detectedPose.contains("tree")) {
            return true;
        } else if (expectedPose.contains("gentle forward fold") && detectedPose.contains("forward")) {
            return true;
        } else if (expectedPose.contains("easy squat") && detectedPose.contains("squat")) {
            return true;
        } else if (expectedPose.contains("gentle knee stretch") && detectedPose.contains("knee")) {
            return true;
        } else if (expectedPose.contains("easy back stretch") && detectedPose.contains("back")) {
            return true;
        } else if (expectedPose.contains("gentle neck stretch") && detectedPose.contains("neck")) {
            return true;
        } else if (expectedPose.contains("easy balance") && detectedPose.contains("balance")) {
            return true;
        } else if (expectedPose.contains("relaxation") && detectedPose.contains("relaxation")) {
            return true;
        }
        
        return false;
    }

    private void stopPoseTimer() {
        if (poseTimerHandler != null && poseTimerRunnable != null) {
            poseTimerHandler.removeCallbacks(poseTimerRunnable);
        }
    }

    private void updateCircularTimer(int secondsRemaining) {
        runOnUiThread(() -> {
            circularTimerText.setText(String.valueOf(secondsRemaining));
        });
    }

    private void updateTips(String poseName) {
        String tip = getTipForPose(poseName);
        tipsText.setText("💡 Tips: " + tip);
        tipsText.setVisibility(View.VISIBLE);
        
        // Hide tips after 2 seconds
        if (tipsTimerHandler != null && tipsTimerRunnable != null) {
            tipsTimerHandler.removeCallbacks(tipsTimerRunnable);
        }
        
        tipsTimerHandler = new Handler();
        tipsTimerRunnable = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    tipsText.setVisibility(View.GONE);
                });
            }
        };
        
        tipsTimerHandler.postDelayed(tipsTimerRunnable, 2000); // 2 seconds
    }
    
    private String getTipForPose(String poseName) {
        String poseLower = poseName.toLowerCase();
        
        if (poseLower.contains("mountain")) {
            return "Stand tall, feet together, arms at sides, breathe deeply";
        } else if (poseLower.contains("cobra")) {
            return "Keep pelvis on ground, lift chest gently, engage back muscles";
        } else if (poseLower.contains("tree")) {
            return "Find your balance, place foot on thigh or calf, focus on a point";
        } else if (poseLower.contains("gentle forward fold")) {
            return "Bend knees slightly, fold from hips, let arms hang naturally";
        } else if (poseLower.contains("easy squat")) {
            return "Squat as if sitting in a chair, keep heels on ground, engage legs";
        } else if (poseLower.contains("gentle knee stretch")) {
            return "Gently lift knee toward chest, hold with hands, switch sides";
        } else if (poseLower.contains("easy back stretch")) {
            return "Place hands on lower back, gently arch then round your spine";
        } else if (poseLower.contains("gentle neck stretch")) {
            return "Slowly turn head side to side, keep shoulders relaxed";
        } else if (poseLower.contains("easy balance")) {
            return "Gently lift one foot slightly, find your balance, switch sides";
        } else if (poseLower.contains("relaxation")) {
            return "Stand comfortably, take deep breaths, feel the benefits";
        } else {
            return "Move slowly and comfortably, don't force any position";
        }
    }

    private void setupAnimations() {
        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.scale_in);
        Animation bounceIn = AnimationUtils.loadAnimation(this, R.anim.bounce_in);

        // Apply entrance animations with staggered timing
        // UI elements
        poseStatusText.startAnimation(fadeIn);
        circularTimerText.startAnimation(bounceIn);
        circularTimerText.getAnimation().setStartOffset(200);
        
        poseFeedbackText.startAnimation(slideUp);
        poseFeedbackText.getAnimation().setStartOffset(400);
        
        tipsText.startAnimation(slideUp);
        tipsText.getAnimation().setStartOffset(600);

        // Buttons
        backToInstructionsButton.startAnimation(slideUp);
        backToInstructionsButton.getAnimation().setStartOffset(800);
        
        backToHomeButton.startAnimation(slideUp);
        backToHomeButton.getAnimation().setStartOffset(1000);

        // Add button press animations
        setupButtonAnimations();
    }

    private void setupButtonAnimations() {
        // Back to instructions button
        backToInstructionsButton.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                Animation scaleOut = AnimationUtils.loadAnimation(this, R.anim.scale_out);
                scaleOut.setDuration(75);
                v.startAnimation(scaleOut);
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.scale_in);
                scaleIn.setDuration(75);
                v.startAnimation(scaleIn);
            }
            return false;
        });

        // Back to home button
        backToHomeButton.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                Animation scaleOut = AnimationUtils.loadAnimation(this, R.anim.scale_out);
                scaleOut.setDuration(75);
                v.startAnimation(scaleOut);
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                Animation scaleIn = AnimationUtils.loadAnimation(this, R.anim.scale_in);
                scaleIn.setDuration(75);
                v.startAnimation(scaleIn);
            }
            return false;
        });
    }
}
