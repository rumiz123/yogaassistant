package com.rumiznellasery.yogahelper.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.common.util.concurrent.ListenableFuture;
import com.rumiznellasery.yogahelper.camera.PoseDetector.PoseLandmarkerResult;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity implements PoseDetector.PoseDetectionCallback {
    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private final String[] REQUIRED_PERMISSIONS = new String[]{ Manifest.permission.CAMERA };
    
    private PreviewView previewView;
    private ImageView poseOverlayView;
    private TextView poseStatusText;
    private MaterialButton togglePoseButton;
    private MaterialButton captureButton;
    private PoseDetector poseDetector;
    private ExecutorService cameraExecutor;
    private boolean isPoseDetectionEnabled = true;

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
        togglePoseButton = findViewById(R.id.btn_toggle_pose);
        captureButton = findViewById(R.id.btn_capture);
        
        // Initialize pose detector
        poseDetector = new PoseDetector(this, this);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Set up button click listeners
        setupButtonListeners();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }
    
    private void setupButtonListeners() {
        togglePoseButton.setOnClickListener(v -> {
            isPoseDetectionEnabled = !isPoseDetectionEnabled;
            if (isPoseDetectionEnabled) {
                togglePoseButton.setText("Disable Pose");
                poseStatusText.setText("Pose Detection: ON\nAnalyzing your movements...");
            } else {
                togglePoseButton.setText("Enable Pose");
                poseStatusText.setText("Pose Detection: OFF\nShowing raw camera feed");
                // Show raw camera feed when pose detection is disabled
                poseOverlayView.setImageBitmap(null);
            }
        });
        
        captureButton.setOnClickListener(v -> {
            // Capture current pose analysis
            Toast.makeText(this, getString(R.string.pose_captured), Toast.LENGTH_SHORT).show();
            // Here you could save the current pose analysis or take a screenshot
        });
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
        if (isPoseDetectionEnabled && poseDetector != null) {
            poseDetector.detectPose(imageProxy);
        } else if (!isPoseDetectionEnabled) {
            // Show raw camera feed when pose detection is disabled
            Bitmap bitmap = imageProxyToBitmap(imageProxy);
            if (bitmap != null) {
                runOnUiThread(() -> {
                    poseOverlayView.setImageBitmap(bitmap);
                });
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
            
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e);
            return null;
        }
    }

    @Override
    public void onPoseDetected(PoseLandmarkerResult result, Bitmap inputBitmap) {
        runOnUiThread(() -> {
            if (result.landmarks().isEmpty()) {
                poseStatusText.setText("No person detected\nMove into camera view");
                poseOverlayView.setImageBitmap(inputBitmap); // Show raw camera feed
            } else {
                // Use the YogaPoseAnalyzer for better pose detection
                YogaPoseAnalyzer.PoseAnalysis analysis = YogaPoseAnalyzer.analyzePose(result);
                
                // Create detailed status text
                String statusText = analysis.poseName + "\n" + 
                                  String.format("Confidence: %.1f%%", analysis.confidence * 100);
                
                if (analysis.confidence < 0.7f && !analysis.feedback.isEmpty()) {
                    statusText += "\n" + analysis.feedback;
                }
                
                poseStatusText.setText(statusText);
                
                // Create an enhanced live visualization of what the model sees
                Bitmap liveVisualization = createLivePoseVisualization(inputBitmap, result, analysis);
                poseOverlayView.setImageBitmap(liveVisualization);
                
                // Show feedback if confidence is low
                if (analysis.confidence < 0.5f && !analysis.feedback.isEmpty()) {
                    Toast.makeText(this, analysis.feedback, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private Bitmap createLivePoseVisualization(Bitmap inputBitmap, PoseDetector.PoseLandmarkerResult result, YogaPoseAnalyzer.PoseAnalysis analysis) {
        // Create a bitmap that shows the raw camera feed with pose detection overlay
        Bitmap visualizationBitmap = Bitmap.createBitmap(inputBitmap.getWidth(), inputBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(visualizationBitmap);
        
        // Draw the raw camera feed as the background - this is what the camera actually sees
        Paint backgroundPaint = new Paint();
        canvas.drawBitmap(inputBitmap, 0, 0, backgroundPaint);
        
        if (result.landmarks().isEmpty()) {
            // If no pose detected, just show the raw camera feed
            return visualizationBitmap;
        }
        
        List<PoseDetector.NormalizedLandmark> landmarks = result.landmarks().get(0);
        
        // Draw the pose skeleton overlay on top of the raw camera feed
        drawLiveSkeleton(canvas, landmarks, analysis, inputBitmap.getWidth(), inputBitmap.getHeight());
        
        // Draw pose analysis information
        drawPoseAnalysisInfo(canvas, analysis, inputBitmap.getWidth(), inputBitmap.getHeight());
        
        // Draw confidence indicators
        drawConfidenceIndicators(canvas, landmarks, analysis, inputBitmap.getWidth(), inputBitmap.getHeight());
        
        return visualizationBitmap;
    }
    
    private void drawLiveSkeleton(Canvas canvas, List<PoseDetector.NormalizedLandmark> landmarks, 
                                YogaPoseAnalyzer.PoseAnalysis analysis, int width, int height) {
        
        // Define pose connections with different colors for different body parts
        int[][] connections = {
            // Face - Green
            {0, 1}, {1, 2}, {2, 3}, {3, 7}, {0, 4}, {4, 5}, {5, 6}, {6, 8}, {9, 10}, {11, 12},
            // Torso - Blue
            {11, 12}, {11, 23}, {12, 24}, {23, 24},
            // Left arm - Red
            {11, 13}, {13, 15}, {15, 17}, {15, 19}, {15, 21}, {17, 19}, {19, 21},
            // Right arm - Orange
            {12, 14}, {14, 16}, {16, 18}, {16, 20}, {16, 22}, {18, 20}, {20, 22},
            // Left leg - Purple
            {23, 25}, {25, 27}, {27, 29}, {27, 31}, {29, 31},
            // Right leg - Cyan
            {24, 26}, {26, 28}, {28, 30}, {28, 32}, {30, 32}
        };
        
        int[] connectionColors = {
            Color.GREEN, Color.GREEN, Color.GREEN, Color.GREEN, Color.GREEN, Color.GREEN, Color.GREEN, Color.GREEN, Color.GREEN, Color.GREEN,
            Color.BLUE, Color.BLUE, Color.BLUE, Color.BLUE,
            Color.RED, Color.RED, Color.RED, Color.RED, Color.RED, Color.RED, Color.RED,
            Color.rgb(255, 165, 0), Color.rgb(255, 165, 0), Color.rgb(255, 165, 0), Color.rgb(255, 165, 0), Color.rgb(255, 165, 0), Color.rgb(255, 165, 0), Color.rgb(255, 165, 0),
            Color.rgb(128, 0, 128), Color.rgb(128, 0, 128), Color.rgb(128, 0, 128), Color.rgb(128, 0, 128), Color.rgb(128, 0, 128),
            Color.CYAN, Color.CYAN, Color.CYAN, Color.CYAN, Color.CYAN
        };
        
        // Draw connections with confidence-based opacity and thickness
        for (int i = 0; i < connections.length; i++) {
            int[] connection = connections[i];
            if (connection[0] < landmarks.size() && connection[1] < landmarks.size()) {
                PoseDetector.NormalizedLandmark start = landmarks.get(connection[0]);
                PoseDetector.NormalizedLandmark end = landmarks.get(connection[1]);
                
                Paint linePaint = new Paint();
                int baseColor = connectionColors[i];
                int alpha = (int)(analysis.confidence * 255);
                linePaint.setColor(Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)));
                linePaint.setStrokeWidth(8f); // Thicker lines for better visibility
                linePaint.setStyle(Paint.Style.STROKE);
                linePaint.setStrokeCap(Paint.Cap.ROUND);
                
                // Add shadow for better visibility on camera feed
                linePaint.setShadowLayer(3f, 2f, 2f, Color.BLACK);
                
                float startX = start.x() * width;
                float startY = start.y() * height;
                float endX = end.x() * width;
                float endY = end.y() * height;
                
                canvas.drawLine(startX, startY, endX, endY, linePaint);
            }
        }
        
        // Draw landmarks with confidence-based coloring
        for (int i = 0; i < landmarks.size(); i++) {
            PoseDetector.NormalizedLandmark landmark = landmarks.get(i);
            
            Paint landmarkPaint = new Paint();
            // Color based on confidence: Green (good) -> Yellow -> Red (poor)
            int color;
            if (analysis.confidence > 0.8f) {
                color = Color.GREEN;
            } else if (analysis.confidence > 0.6f) {
                color = Color.YELLOW;
            } else {
                color = Color.RED;
            }
            
            int alpha = (int)(analysis.confidence * 255);
            landmarkPaint.setColor(Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)));
            landmarkPaint.setStrokeWidth(12f);
            landmarkPaint.setStyle(Paint.Style.FILL);
            
            // Add shadow for better visibility
            landmarkPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK);
            
            float x = landmark.x() * width;
            float y = landmark.y() * height;
            canvas.drawCircle(x, y, 10f, landmarkPaint); // Larger circles
            
            // Add a white border for better visibility
            Paint borderPaint = new Paint();
            borderPaint.setColor(Color.WHITE);
            borderPaint.setStrokeWidth(3f);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setShadowLayer(1f, 0f, 0f, Color.BLACK);
            canvas.drawCircle(x, y, 10f, borderPaint);
        }
    }
    
    private void drawPoseAnalysisInfo(Canvas canvas, YogaPoseAnalyzer.PoseAnalysis analysis, int width, int height) {
        // Draw pose name and confidence at the top
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(24f);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setShadowLayer(3f, 2f, 2f, Color.BLACK);
        
        String poseText = analysis.poseName;
        String confidenceText = String.format("%.1f%%", analysis.confidence * 100);
        
        float textX = 20f;
        float textY = 50f;
        
        canvas.drawText("Pose: " + poseText, textX, textY, textPaint);
        canvas.drawText("Confidence: " + confidenceText, textX, textY + 30f, textPaint);
        
        // Draw feedback if confidence is low
        if (analysis.confidence < 0.7f && !analysis.feedback.isEmpty()) {
            textPaint.setColor(Color.YELLOW);
            textPaint.setTextSize(18f);
            canvas.drawText("Feedback: " + analysis.feedback, textX, textY + 60f, textPaint);
        }
    }
    
    private void drawConfidenceIndicators(Canvas canvas, List<PoseDetector.NormalizedLandmark> landmarks, 
                                        YogaPoseAnalyzer.PoseAnalysis analysis, int width, int height) {
        // Draw a confidence bar at the bottom
        Paint barPaint = new Paint();
        barPaint.setStyle(Paint.Style.FILL);
        
        float barWidth = width * 0.8f;
        float barHeight = 20f;
        float barX = (width - barWidth) / 2f;
        float barY = height - 50f;
        
        // Background bar
        barPaint.setColor(Color.GRAY);
        canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, barPaint);
        
        // Confidence level bar
        if (analysis.confidence > 0.8f) {
            barPaint.setColor(Color.GREEN);
        } else if (analysis.confidence > 0.6f) {
            barPaint.setColor(Color.YELLOW);
        } else {
            barPaint.setColor(Color.RED);
        }
        
        float confidenceWidth = barWidth * analysis.confidence;
        canvas.drawRect(barX, barY, barX + confidenceWidth, barY + barHeight, barPaint);
        
        // Draw confidence text
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(16f);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK);
        
        String confidenceText = "Model Confidence: " + String.format("%.1f%%", analysis.confidence * 100);
        float textX = barX;
        float textY = barY - 10f;
        canvas.drawText(confidenceText, textX, textY, textPaint);
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
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
