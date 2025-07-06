package com.rumiznellasery.yogahelper.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

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
    
    private PreviewView previewView;
    private ImageView poseOverlayView;
    private TextView poseStatusText;
    private Button backToInstructionsButton;
    private Button backToHomeButton;
    private MediaPipePoseDetector poseDetector;
    private WorkoutSession workoutSession;
    private ExecutorService cameraExecutor;
    private boolean isPoseDetectionEnabled = true; // Always enabled now

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_camera);
        
        // Initialize views
        previewView = findViewById(R.id.view_finder);
        poseOverlayView = findViewById(R.id.pose_overlay);
        poseStatusText = findViewById(R.id.pose_status_text);
        backToInstructionsButton = findViewById(R.id.btn_back_to_instructions);
        backToHomeButton = findViewById(R.id.btn_back_to_home);
        
        // Initialize MediaPipe pose detector
        poseDetector = new MediaPipePoseDetector(this, this);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialize workout session
        workoutSession = new WorkoutSession(this);

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
            // Start the guided workout
            workoutSession.startWorkout();
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
            } else {
                // Use the YogaPoseAnalyzer for better pose detection
                YogaPoseAnalyzer.PoseAnalysis analysis = YogaPoseAnalyzer.analyzePose(result);
                
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
            String statusText = String.format("Pose %d of %d: %s\n\n%s\n\nTime: %d seconds", 
                poseIndex, totalPoses, pose.name, pose.instructions, pose.durationSeconds);
            poseStatusText.setText(statusText);
        });
    }

    @Override
    public void onPoseProgress(WorkoutSession.Pose pose, int secondsRemaining) {
        runOnUiThread(() -> {
            String currentText = poseStatusText.getText().toString();
            // Update the time remaining in the status text
            String[] lines = currentText.split("\n");
            if (lines.length >= 3) {
                lines[2] = "Time: " + secondsRemaining + " seconds";
                poseStatusText.setText(String.join("\n", lines));
            }
        });
    }

    @Override
    public void onPoseCompleted(WorkoutSession.Pose pose, int poseIndex) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Great job! " + pose.name + " completed!", Toast.LENGTH_SHORT).show();
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
            poseStatusText.setText("Workout Paused\n\nTap 'Resume' to continue.");
        });
    }

    @Override
    public void onWorkoutResumed() {
        runOnUiThread(() -> {
            WorkoutSession.Pose currentPose = workoutSession.getCurrentPose();
            if (currentPose != null) {
                onPoseStarted(currentPose, workoutSession.getCurrentPoseIndex() + 1, workoutSession.getTotalPoses());
            }
        });
    }
}
