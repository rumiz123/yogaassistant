package com.rumiznellasery.yogahelper.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.YuvImage;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class PoseDetector {
    private static final String TAG = "PoseDetector";
    private static final String MODEL_PATH = "pose_landmarker_full.task";
    
    private final Context context;
    private final PoseDetectionCallback callback;
    private boolean isInitialized = false;
    
    public interface PoseDetectionCallback {
        void onPoseDetected(PoseLandmarkerResult result, Bitmap inputBitmap);
        void onError(String error);
    }
    
    // Simple result class for pose detection
    public static class PoseLandmarkerResult {
        public List<List<NormalizedLandmark>> landmarks;
        
        public PoseLandmarkerResult(List<List<NormalizedLandmark>> landmarks) {
            this.landmarks = landmarks;
        }
        
        public List<List<NormalizedLandmark>> landmarks() {
            return landmarks;
        }
    }
    
    // Simple landmark class
    public static class NormalizedLandmark {
        public float x, y, z;
        
        public NormalizedLandmark(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public float x() { return x; }
        public float y() { return y; }
        public float z() { return z; }
    }
    
    public PoseDetector(Context context, PoseDetectionCallback callback) {
        this.context = context;
        this.callback = callback;
        initializePoseLandmarker();
    }
    
    private void initializePoseLandmarker() {
        try {
            // For now, we'll simulate pose detection
            // In a real implementation, you would initialize MediaPipe here
            Log.d(TAG, "PoseLandmarker initialization started");
            isInitialized = true;
            Log.d(TAG, "PoseLandmarker initialized successfully (simulated)");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing PoseLandmarker", e);
            if (callback != null) {
                callback.onError("Failed to initialize pose detector: " + e.getMessage());
            }
        }
    }
    
    public void detectPose(ImageProxy imageProxy) {
        if (!isInitialized) {
            Log.e(TAG, "PoseLandmarker not initialized");
            return;
        }
        
        try {
            // Convert ImageProxy to Bitmap
            Bitmap bitmap = imageProxyToBitmap(imageProxy);
            if (bitmap != null) {
                // For now, simulate pose detection
                // In a real implementation, you would process the bitmap with MediaPipe
                simulatePoseDetection(bitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error detecting pose", e);
            if (callback != null) {
                callback.onError("Pose detection failed: " + e.getMessage());
            }
        }
    }
    
    private void simulatePoseDetection(Bitmap bitmap) {
        // Simulate pose detection for demonstration
        // In a real implementation, this would use MediaPipe
        try {
            // Create a dynamic simulated pose result that changes over time
            // This simulates what the model "sees" in real-time
            List<NormalizedLandmark> simulatedLandmarks = createDynamicSimulatedLandmarks();
            List<List<NormalizedLandmark>> landmarksList = List.of(simulatedLandmarks);
            PoseLandmarkerResult result = new PoseLandmarkerResult(landmarksList);
            
            if (callback != null) {
                callback.onPoseDetected(result, bitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in simulated pose detection", e);
        }
    }
    
    private List<NormalizedLandmark> createDynamicSimulatedLandmarks() {
        // Create dynamic landmarks that simulate different yoga poses
        // This simulates what the model "paints" as it sees the user
        long currentTime = System.currentTimeMillis();
        float timeFactor = (currentTime % 5000) / 5000.0f; // 5 second cycle
        
        // Simulate different poses based on time
        if (timeFactor < 0.2f) {
            return createStandingPose(timeFactor);
        } else if (timeFactor < 0.4f) {
            return createWarriorPose(timeFactor);
        } else if (timeFactor < 0.6f) {
            return createTreePose(timeFactor);
        } else if (timeFactor < 0.8f) {
            return createDownwardDog(timeFactor);
        } else {
            return createChildPose(timeFactor);
        }
    }
    
    private List<NormalizedLandmark> createStandingPose(float timeFactor) {
        // Mountain pose - standing straight
        float sway = (float) Math.sin(timeFactor * Math.PI * 2) * 0.02f;
        return List.of(
            new NormalizedLandmark(0.5f + sway, 0.1f, 0.0f),   // nose
            new NormalizedLandmark(0.45f + sway, 0.1f, 0.0f),  // left eye inner
            new NormalizedLandmark(0.42f + sway, 0.1f, 0.0f),  // left eye
            new NormalizedLandmark(0.38f + sway, 0.1f, 0.0f),  // left eye outer
            new NormalizedLandmark(0.55f + sway, 0.1f, 0.0f),  // right eye inner
            new NormalizedLandmark(0.58f + sway, 0.1f, 0.0f),  // right eye
            new NormalizedLandmark(0.62f + sway, 0.1f, 0.0f),  // right eye outer
            new NormalizedLandmark(0.35f + sway, 0.12f, 0.0f), // left ear
            new NormalizedLandmark(0.65f + sway, 0.12f, 0.0f), // right ear
            new NormalizedLandmark(0.48f + sway, 0.15f, 0.0f), // mouth left
            new NormalizedLandmark(0.52f + sway, 0.15f, 0.0f), // mouth right
            new NormalizedLandmark(0.45f + sway, 0.25f, 0.0f), // left shoulder
            new NormalizedLandmark(0.55f + sway, 0.25f, 0.0f), // right shoulder
            new NormalizedLandmark(0.4f + sway, 0.35f, 0.0f),  // left elbow
            new NormalizedLandmark(0.6f + sway, 0.35f, 0.0f),  // right elbow
            new NormalizedLandmark(0.35f + sway, 0.45f, 0.0f), // left wrist
            new NormalizedLandmark(0.65f + sway, 0.45f, 0.0f), // right wrist
            new NormalizedLandmark(0.35f + sway, 0.55f, 0.0f), // left pinky
            new NormalizedLandmark(0.65f + sway, 0.55f, 0.0f), // right pinky
            new NormalizedLandmark(0.35f + sway, 0.55f, 0.0f), // left index
            new NormalizedLandmark(0.65f + sway, 0.55f, 0.0f), // right index
            new NormalizedLandmark(0.35f + sway, 0.55f, 0.0f), // left thumb
            new NormalizedLandmark(0.65f + sway, 0.55f, 0.0f), // right thumb
            new NormalizedLandmark(0.45f + sway, 0.65f, 0.0f), // left hip
            new NormalizedLandmark(0.55f + sway, 0.65f, 0.0f), // right hip
            new NormalizedLandmark(0.4f + sway, 0.75f, 0.0f),  // left knee
            new NormalizedLandmark(0.6f + sway, 0.75f, 0.0f),  // right knee
            new NormalizedLandmark(0.35f + sway, 0.85f, 0.0f), // left ankle
            new NormalizedLandmark(0.65f + sway, 0.85f, 0.0f), // right ankle
            new NormalizedLandmark(0.3f + sway, 0.95f, 0.0f),  // left heel
            new NormalizedLandmark(0.7f + sway, 0.95f, 0.0f),  // right heel
            new NormalizedLandmark(0.35f + sway, 0.95f, 0.0f), // left foot index
            new NormalizedLandmark(0.65f + sway, 0.95f, 0.0f)  // right foot index
        );
    }
    
    private List<NormalizedLandmark> createWarriorPose(float timeFactor) {
        // Warrior II pose - arms extended, legs apart
        float armRaise = (float) Math.sin(timeFactor * Math.PI * 2) * 0.05f;
        return List.of(
            new NormalizedLandmark(0.5f, 0.1f, 0.0f),   // nose
            new NormalizedLandmark(0.45f, 0.1f, 0.0f),  // left eye inner
            new NormalizedLandmark(0.42f, 0.1f, 0.0f),  // left eye
            new NormalizedLandmark(0.38f, 0.1f, 0.0f),  // left eye outer
            new NormalizedLandmark(0.55f, 0.1f, 0.0f),  // right eye inner
            new NormalizedLandmark(0.58f, 0.1f, 0.0f),  // right eye
            new NormalizedLandmark(0.62f, 0.1f, 0.0f),  // right eye outer
            new NormalizedLandmark(0.35f, 0.12f, 0.0f), // left ear
            new NormalizedLandmark(0.65f, 0.12f, 0.0f), // right ear
            new NormalizedLandmark(0.48f, 0.15f, 0.0f), // mouth left
            new NormalizedLandmark(0.52f, 0.15f, 0.0f), // mouth right
            new NormalizedLandmark(0.35f, 0.25f, 0.0f), // left shoulder
            new NormalizedLandmark(0.65f, 0.25f, 0.0f), // right shoulder
            new NormalizedLandmark(0.25f, 0.25f + armRaise, 0.0f),  // left elbow
            new NormalizedLandmark(0.75f, 0.25f + armRaise, 0.0f),  // right elbow
            new NormalizedLandmark(0.15f, 0.25f + armRaise, 0.0f), // left wrist
            new NormalizedLandmark(0.85f, 0.25f + armRaise, 0.0f), // right wrist
            new NormalizedLandmark(0.15f, 0.25f + armRaise, 0.0f), // left pinky
            new NormalizedLandmark(0.85f, 0.25f + armRaise, 0.0f), // right pinky
            new NormalizedLandmark(0.15f, 0.25f + armRaise, 0.0f), // left index
            new NormalizedLandmark(0.85f, 0.25f + armRaise, 0.0f), // right index
            new NormalizedLandmark(0.15f, 0.25f + armRaise, 0.0f), // left thumb
            new NormalizedLandmark(0.85f, 0.25f + armRaise, 0.0f), // right thumb
            new NormalizedLandmark(0.35f, 0.65f, 0.0f), // left hip
            new NormalizedLandmark(0.65f, 0.65f, 0.0f), // right hip
            new NormalizedLandmark(0.25f, 0.75f, 0.0f),  // left knee
            new NormalizedLandmark(0.75f, 0.75f, 0.0f),  // right knee
            new NormalizedLandmark(0.2f, 0.85f, 0.0f), // left ankle
            new NormalizedLandmark(0.8f, 0.85f, 0.0f), // right ankle
            new NormalizedLandmark(0.15f, 0.95f, 0.0f),  // left heel
            new NormalizedLandmark(0.85f, 0.95f, 0.0f),  // right heel
            new NormalizedLandmark(0.2f, 0.95f, 0.0f), // left foot index
            new NormalizedLandmark(0.8f, 0.95f, 0.0f)  // right foot index
        );
    }
    
    private List<NormalizedLandmark> createTreePose(float timeFactor) {
        // Tree pose - one leg raised, arms in prayer
        float balance = (float) Math.sin(timeFactor * Math.PI * 4) * 0.03f;
        return List.of(
            new NormalizedLandmark(0.5f + balance, 0.1f, 0.0f),   // nose
            new NormalizedLandmark(0.45f + balance, 0.1f, 0.0f),  // left eye inner
            new NormalizedLandmark(0.42f + balance, 0.1f, 0.0f),  // left eye
            new NormalizedLandmark(0.38f + balance, 0.1f, 0.0f),  // left eye outer
            new NormalizedLandmark(0.55f + balance, 0.1f, 0.0f),  // right eye inner
            new NormalizedLandmark(0.58f + balance, 0.1f, 0.0f),  // right eye
            new NormalizedLandmark(0.62f + balance, 0.1f, 0.0f),  // right eye outer
            new NormalizedLandmark(0.35f + balance, 0.12f, 0.0f), // left ear
            new NormalizedLandmark(0.65f + balance, 0.12f, 0.0f), // right ear
            new NormalizedLandmark(0.48f + balance, 0.15f, 0.0f), // mouth left
            new NormalizedLandmark(0.52f + balance, 0.15f, 0.0f), // mouth right
            new NormalizedLandmark(0.45f + balance, 0.25f, 0.0f), // left shoulder
            new NormalizedLandmark(0.55f + balance, 0.25f, 0.0f), // right shoulder
            new NormalizedLandmark(0.45f + balance, 0.35f, 0.0f),  // left elbow
            new NormalizedLandmark(0.55f + balance, 0.35f, 0.0f),  // right elbow
            new NormalizedLandmark(0.48f + balance, 0.45f, 0.0f), // left wrist
            new NormalizedLandmark(0.52f + balance, 0.45f, 0.0f), // right wrist
            new NormalizedLandmark(0.48f + balance, 0.45f, 0.0f), // left pinky
            new NormalizedLandmark(0.52f + balance, 0.45f, 0.0f), // right pinky
            new NormalizedLandmark(0.48f + balance, 0.45f, 0.0f), // left index
            new NormalizedLandmark(0.52f + balance, 0.45f, 0.0f), // right index
            new NormalizedLandmark(0.48f + balance, 0.45f, 0.0f), // left thumb
            new NormalizedLandmark(0.52f + balance, 0.45f, 0.0f), // right thumb
            new NormalizedLandmark(0.45f + balance, 0.65f, 0.0f), // left hip
            new NormalizedLandmark(0.55f + balance, 0.65f, 0.0f), // right hip
            new NormalizedLandmark(0.4f + balance, 0.75f, 0.0f),  // left knee
            new NormalizedLandmark(0.6f + balance, 0.75f, 0.0f),  // right knee (raised)
            new NormalizedLandmark(0.35f + balance, 0.85f, 0.0f), // left ankle
            new NormalizedLandmark(0.65f + balance, 0.65f, 0.0f), // right ankle (raised)
            new NormalizedLandmark(0.3f + balance, 0.95f, 0.0f),  // left heel
            new NormalizedLandmark(0.7f + balance, 0.65f, 0.0f),  // right heel (raised)
            new NormalizedLandmark(0.35f + balance, 0.95f, 0.0f), // left foot index
            new NormalizedLandmark(0.65f + balance, 0.65f, 0.0f)  // right foot index (raised)
        );
    }
    
    private List<NormalizedLandmark> createDownwardDog(float timeFactor) {
        // Downward dog pose - inverted V shape
        float breathing = (float) Math.sin(timeFactor * Math.PI * 2) * 0.02f;
        return List.of(
            new NormalizedLandmark(0.5f, 0.3f + breathing, 0.0f),   // nose (lowered)
            new NormalizedLandmark(0.45f, 0.3f + breathing, 0.0f),  // left eye inner
            new NormalizedLandmark(0.42f, 0.3f + breathing, 0.0f),  // left eye
            new NormalizedLandmark(0.38f, 0.3f + breathing, 0.0f),  // left eye outer
            new NormalizedLandmark(0.55f, 0.3f + breathing, 0.0f),  // right eye inner
            new NormalizedLandmark(0.58f, 0.3f + breathing, 0.0f),  // right eye
            new NormalizedLandmark(0.62f, 0.3f + breathing, 0.0f),  // right eye outer
            new NormalizedLandmark(0.35f, 0.32f + breathing, 0.0f), // left ear
            new NormalizedLandmark(0.65f, 0.32f + breathing, 0.0f), // right ear
            new NormalizedLandmark(0.48f, 0.35f + breathing, 0.0f), // mouth left
            new NormalizedLandmark(0.52f, 0.35f + breathing, 0.0f), // mouth right
            new NormalizedLandmark(0.45f, 0.45f + breathing, 0.0f), // left shoulder
            new NormalizedLandmark(0.55f, 0.45f + breathing, 0.0f), // right shoulder
            new NormalizedLandmark(0.4f, 0.55f + breathing, 0.0f),  // left elbow
            new NormalizedLandmark(0.6f, 0.55f + breathing, 0.0f),  // right elbow
            new NormalizedLandmark(0.35f, 0.65f + breathing, 0.0f), // left wrist
            new NormalizedLandmark(0.65f, 0.65f + breathing, 0.0f), // right wrist
            new NormalizedLandmark(0.35f, 0.65f + breathing, 0.0f), // left pinky
            new NormalizedLandmark(0.65f, 0.65f + breathing, 0.0f), // right pinky
            new NormalizedLandmark(0.35f, 0.65f + breathing, 0.0f), // left index
            new NormalizedLandmark(0.65f, 0.65f + breathing, 0.0f), // right index
            new NormalizedLandmark(0.35f, 0.65f + breathing, 0.0f), // left thumb
            new NormalizedLandmark(0.65f, 0.65f + breathing, 0.0f), // right thumb
            new NormalizedLandmark(0.45f, 0.75f + breathing, 0.0f), // left hip
            new NormalizedLandmark(0.55f, 0.75f + breathing, 0.0f), // right hip
            new NormalizedLandmark(0.4f, 0.85f + breathing, 0.0f),  // left knee
            new NormalizedLandmark(0.6f, 0.85f + breathing, 0.0f),  // right knee
            new NormalizedLandmark(0.35f, 0.95f + breathing, 0.0f), // left ankle
            new NormalizedLandmark(0.65f, 0.95f + breathing, 0.0f), // right ankle
            new NormalizedLandmark(0.3f, 0.95f + breathing, 0.0f),  // left heel
            new NormalizedLandmark(0.7f, 0.95f + breathing, 0.0f),  // right heel
            new NormalizedLandmark(0.35f, 0.95f + breathing, 0.0f), // left foot index
            new NormalizedLandmark(0.65f, 0.95f + breathing, 0.0f)  // right foot index
        );
    }
    
    private List<NormalizedLandmark> createChildPose(float timeFactor) {
        // Child's pose - kneeling, arms extended forward
        float breathing = (float) Math.sin(timeFactor * Math.PI * 2) * 0.01f;
        return List.of(
            new NormalizedLandmark(0.5f, 0.2f + breathing, 0.0f),   // nose
            new NormalizedLandmark(0.45f, 0.2f + breathing, 0.0f),  // left eye inner
            new NormalizedLandmark(0.42f, 0.2f + breathing, 0.0f),  // left eye
            new NormalizedLandmark(0.38f, 0.2f + breathing, 0.0f),  // left eye outer
            new NormalizedLandmark(0.55f, 0.2f + breathing, 0.0f),  // right eye inner
            new NormalizedLandmark(0.58f, 0.2f + breathing, 0.0f),  // right eye
            new NormalizedLandmark(0.62f, 0.2f + breathing, 0.0f),  // right eye outer
            new NormalizedLandmark(0.35f, 0.22f + breathing, 0.0f), // left ear
            new NormalizedLandmark(0.65f, 0.22f + breathing, 0.0f), // right ear
            new NormalizedLandmark(0.48f, 0.25f + breathing, 0.0f), // mouth left
            new NormalizedLandmark(0.52f, 0.25f + breathing, 0.0f), // mouth right
            new NormalizedLandmark(0.45f, 0.35f + breathing, 0.0f), // left shoulder
            new NormalizedLandmark(0.55f, 0.35f + breathing, 0.0f), // right shoulder
            new NormalizedLandmark(0.4f, 0.45f + breathing, 0.0f),  // left elbow
            new NormalizedLandmark(0.6f, 0.45f + breathing, 0.0f),  // right elbow
            new NormalizedLandmark(0.35f, 0.55f + breathing, 0.0f), // left wrist
            new NormalizedLandmark(0.65f, 0.55f + breathing, 0.0f), // right wrist
            new NormalizedLandmark(0.35f, 0.55f + breathing, 0.0f), // left pinky
            new NormalizedLandmark(0.65f, 0.55f + breathing, 0.0f), // right pinky
            new NormalizedLandmark(0.35f, 0.55f + breathing, 0.0f), // left index
            new NormalizedLandmark(0.65f, 0.55f + breathing, 0.0f), // right index
            new NormalizedLandmark(0.35f, 0.55f + breathing, 0.0f), // left thumb
            new NormalizedLandmark(0.65f, 0.55f + breathing, 0.0f), // right thumb
            new NormalizedLandmark(0.45f, 0.65f + breathing, 0.0f), // left hip
            new NormalizedLandmark(0.55f, 0.65f + breathing, 0.0f), // right hip
            new NormalizedLandmark(0.4f, 0.75f + breathing, 0.0f),  // left knee
            new NormalizedLandmark(0.6f, 0.75f + breathing, 0.0f),  // right knee
            new NormalizedLandmark(0.35f, 0.85f + breathing, 0.0f), // left ankle
            new NormalizedLandmark(0.65f, 0.85f + breathing, 0.0f), // right ankle
            new NormalizedLandmark(0.3f, 0.95f + breathing, 0.0f),  // left heel
            new NormalizedLandmark(0.7f, 0.95f + breathing, 0.0f),  // right heel
            new NormalizedLandmark(0.35f, 0.95f + breathing, 0.0f), // left foot index
            new NormalizedLandmark(0.65f, 0.95f + breathing, 0.0f)  // right foot index
        );
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
    
    public static Bitmap drawPoseLandmarks(Bitmap originalBitmap, PoseLandmarkerResult result) {
        if (result.landmarks().isEmpty()) {
            return originalBitmap;
        }
        
        Bitmap outputBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(outputBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(8f);
        paint.setStyle(Paint.Style.FILL);
        
        Paint linePaint = new Paint();
        linePaint.setColor(Color.BLUE);
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);
        
        // Draw landmarks for the first detected pose
        List<NormalizedLandmark> landmarks = result.landmarks().get(0);
        
        // Draw landmark points
        for (NormalizedLandmark landmark : landmarks) {
            float x = landmark.x() * originalBitmap.getWidth();
            float y = landmark.y() * originalBitmap.getHeight();
            canvas.drawCircle(x, y, 8f, paint);
        }
        
        // Draw connections between landmarks
        drawPoseConnections(canvas, landmarks, linePaint, originalBitmap.getWidth(), originalBitmap.getHeight());
        
        return outputBitmap;
    }
    
    private static void drawPoseConnections(Canvas canvas, 
                                          List<NormalizedLandmark> landmarks,
                                          Paint paint, int width, int height) {
        // Define pose connections (MediaPipe pose landmarks)
        int[][] connections = {
            // Face
            {0, 1}, {1, 2}, {2, 3}, {3, 7}, {0, 4}, {4, 5}, {5, 6}, {6, 8}, {9, 10}, {11, 12},
            // Torso
            {11, 12}, {11, 23}, {12, 24}, {23, 24},
            // Left arm
            {11, 13}, {13, 15}, {15, 17}, {15, 19}, {15, 21}, {17, 19}, {19, 21},
            // Right arm
            {12, 14}, {14, 16}, {16, 18}, {16, 20}, {16, 22}, {18, 20}, {20, 22},
            // Left leg
            {23, 25}, {25, 27}, {27, 29}, {27, 31}, {29, 31},
            // Right leg
            {24, 26}, {26, 28}, {28, 30}, {28, 32}, {30, 32}
        };
        
        for (int[] connection : connections) {
            if (connection[0] < landmarks.size() && connection[1] < landmarks.size()) {
                NormalizedLandmark start = landmarks.get(connection[0]);
                NormalizedLandmark end = landmarks.get(connection[1]);
                
                float startX = start.x() * width;
                float startY = start.y() * height;
                float endX = end.x() * width;
                float endY = end.y() * height;
                
                canvas.drawLine(startX, startY, endX, endY, paint);
            }
        }
    }
    
    public void close() {
        isInitialized = false;
    }
} 