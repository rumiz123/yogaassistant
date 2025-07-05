package com.rumiznellasery.yogahelper;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MediaPipePose";
    private static final String MODEL_ASSET = "pose_landmarker_full.task";

    private PoseLandmarker poseLandmarker;
    private ExecutorService cameraExecutor;
    private PreviewView previewView;
    private PoseOverlayView poseOverlay;
    private TextView poseNameText;
    private TextView landmarkCountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        poseOverlay = findViewById(R.id.poseOverlay);
        poseNameText = findViewById(R.id.pose_name);
        landmarkCountText = findViewById(R.id.landmark_count);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            setupCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 10);
        }

        initializePoseLandmarker();
    }

    private void setupCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error setting up camera", e);
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void initializePoseLandmarker() {
        try {
            // Create PoseLandmarker directly with model path
            poseLandmarker = PoseLandmarker.createFromFile(this, MODEL_ASSET);
            Log.d(TAG, "PoseLandmarker initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize PoseLandmarker", e);
            Toast.makeText(this, "Failed to initialize pose detector: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (poseLandmarker == null) {
            imageProxy.close();
            return;
        }
        
        try {
            Bitmap bitmap = imageProxyToBitmap(imageProxy);
            if (bitmap != null) {
                MPImage mpImage = new BitmapImageBuilder(bitmap).build();
                PoseLandmarkerResult result = poseLandmarker.detect(mpImage);
                onPoseResult(result, mpImage, imageProxy.getImageInfo().getTimestamp(), bitmap.getWidth(), bitmap.getHeight());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error analyzing image", e);
        } finally {
            imageProxy.close();
        }
    }

    private void onPoseResult(PoseLandmarkerResult result, MPImage inputImage, long timestamp, int imageWidth, int imageHeight) {
        if (result == null) {
            runOnUiThread(() -> {
                poseNameText.setText("No pose detected");
                landmarkCountText.setText("Landmarks: 0");
                poseOverlay.setPoseResult(null, 0, 0);
            });
            return;
        }

        if (result.landmarks().isEmpty()) {
            runOnUiThread(() -> {
                poseNameText.setText("No pose detected");
                landmarkCountText.setText("Landmarks: 0");
                poseOverlay.setPoseResult(null, 0, 0);
            });
            return;
        }

        // Get the first detected person's landmarks
        var landmarks = result.landmarks().get(0);
        int landmarkCount = landmarks.size();
        
        Log.d(TAG, "Detected pose with " + landmarkCount + " landmarks");
        
        // Log key landmarks (nose, shoulders, hips)
        if (landmarkCount > 0) {
            var nose = landmarks.get(0); // Nose
            var leftShoulder = landmarks.get(11); // Left shoulder
            var rightShoulder = landmarks.get(12); // Right shoulder
            var leftHip = landmarks.get(23); // Left hip
            var rightHip = landmarks.get(24); // Right hip
            
            Log.d(TAG, String.format("Key landmarks - Nose: (%.2f, %.2f), Left Shoulder: (%.2f, %.2f), Right Shoulder: (%.2f, %.2f)", 
                nose.x(), nose.y(), leftShoulder.x(), leftShoulder.y(), rightShoulder.x(), rightShoulder.y()));
        }

        runOnUiThread(() -> {
            poseNameText.setText("Pose detected!");
            landmarkCountText.setText("Landmarks: " + landmarkCount);
            poseOverlay.setPoseResult(result, imageWidth, imageHeight);
        });
    }

    // Utility: Convert ImageProxy to Bitmap (YUV_420_888 to RGB)
    private Bitmap imageProxyToBitmap(ImageProxy image) {
        try {
            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            android.graphics.YuvImage yuvImage = new android.graphics.YuvImage(
                    nv21,
                    android.graphics.ImageFormat.NV21,
                    image.getWidth(),
                    image.getHeight(),
                    null
            );
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            yuvImage.compressToJpeg(new android.graphics.Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);
            byte[] imageBytes = out.toByteArray();
            Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            // Rotate if needed
            Matrix matrix = new Matrix();
            matrix.postRotate(image.getImageInfo().getRotationDegrees());
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e);
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (poseLandmarker != null) {
            poseLandmarker.close();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupCamera();
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
        }
    }
}