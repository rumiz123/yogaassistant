package com.rumiznellasery.yogahelper.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MediaPipePoseDetector {
    private static final String TAG = "MediaPipePoseDetector";
    private static final String MODEL_PATH = "pose_landmarker_full.task";
    
    private final Context context;
    private final PoseDetectionCallback callback;
    private PoseLandmarker poseLandmarker;
    private boolean isInitialized = false;
    
    public interface PoseDetectionCallback {
        void onPoseDetected(PoseDetector.PoseLandmarkerResult result, Bitmap poseOverlay);
        void onError(String error);
    }
    
    public MediaPipePoseDetector(Context context, PoseDetectionCallback callback) {
        this.context = context;
        this.callback = callback;
        initializePoseLandmarker();
    }
    
    private void initializePoseLandmarker() {
        BaseOptions baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_PATH)
                .build();
        // Create PoseLandmarker with options using the correct API
        poseLandmarker = PoseLandmarker.createFromOptions(context, 
            PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .build());
        isInitialized = true;
        Log.d(TAG, "MediaPipe PoseLandmarker initialized successfully");
    }
    
    public void detectPose(Bitmap inputBitmap, long timestamp) {
        if (!isInitialized || poseLandmarker == null) {
            Log.e(TAG, "MediaPipe PoseLandmarker not initialized");
            return;
        }
        
        try {
            // Convert Bitmap to MPImage for MediaPipe
            MPImage mpImage = new BitmapImageBuilder(inputBitmap).build();
            PoseLandmarkerResult result = poseLandmarker.detect(mpImage);
            onPoseDetectionResult(result, inputBitmap);
        } catch (Exception e) {
            Log.e(TAG, "Error detecting pose", e);
            if (callback != null) {
                callback.onError("Pose detection failed: " + e.getMessage());
            }
        }
    }
    
    private void onPoseDetectionResult(PoseLandmarkerResult result, Bitmap inputBitmap) {
        try {
            if (result == null || result.landmarks().isEmpty()) {
                if (callback != null) {
                    callback.onPoseDetected(new PoseDetector.PoseLandmarkerResult(new ArrayList<>()), null);
                }
            } else {
                Bitmap poseOverlay = createPoseOverlay(inputBitmap.getWidth(), inputBitmap.getHeight(), result);
                List<List<PoseDetector.NormalizedLandmark>> landmarksList = convertToCustomFormat(result);
                PoseDetector.PoseLandmarkerResult customResult = new PoseDetector.PoseLandmarkerResult(landmarksList);
                if (callback != null) {
                    callback.onPoseDetected(customResult, poseOverlay);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing pose detection result", e);
            if (callback != null) {
                callback.onError("Error processing pose result: " + e.getMessage());
            }
        }
    }
    
    private Bitmap createPoseOverlay(int width, int height, PoseLandmarkerResult result) {
        Bitmap overlay = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);
        drawPoseSkeleton(canvas, result, width, height);
        return overlay;
    }
    
    private void drawPoseSkeleton(Canvas canvas, PoseLandmarkerResult result, int width, int height) {
        if (result.landmarks().isEmpty()) return;
        
        // Get the first detected pose
        List<NormalizedLandmark> landmarks = result.landmarks().get(0);
        
        // Define pose connections
        int[][] connections = {
            {0, 1}, {1, 2}, {2, 3}, {3, 7}, {0, 4}, {4, 5}, {5, 6}, {6, 8}, {9, 10}, {11, 12},
            {11, 12}, {11, 23}, {12, 24}, {23, 24},
            {11, 13}, {13, 15}, {15, 17}, {15, 19}, {15, 21}, {17, 19}, {19, 21},
            {12, 14}, {14, 16}, {16, 18}, {16, 20}, {16, 22}, {18, 20}, {20, 22},
            {23, 25}, {25, 27}, {27, 29}, {27, 31}, {29, 31},
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
        
        // Draw connections
        for (int i = 0; i < connections.length; i++) {
            int[] connection = connections[i];
            if (connection[0] < landmarks.size() && connection[1] < landmarks.size()) {
                NormalizedLandmark start = landmarks.get(connection[0]);
                NormalizedLandmark end = landmarks.get(connection[1]);
                Paint linePaint = new Paint();
                linePaint.setColor(connectionColors[i]);
                linePaint.setStrokeWidth(8f);
                linePaint.setStyle(Paint.Style.STROKE);
                linePaint.setStrokeCap(Paint.Cap.ROUND);
                linePaint.setShadowLayer(3f, 2f, 2f, Color.BLACK);
                float startX = start.x() * width;
                float startY = start.y() * height;
                float endX = end.x() * width;
                float endY = end.y() * height;
                canvas.drawLine(startX, startY, endX, endY, linePaint);
            }
        }
        
        // Draw landmarks
        for (int i = 0; i < landmarks.size(); i++) {
            NormalizedLandmark landmark = landmarks.get(i);
            Paint landmarkPaint = new Paint();
            int color = Color.GREEN;
            int alpha = 255; // Full opacity
            landmarkPaint.setColor(Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)));
            landmarkPaint.setStrokeWidth(12f);
            landmarkPaint.setStyle(Paint.Style.FILL);
            landmarkPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK);
            float x = landmark.x() * width;
            float y = landmark.y() * height;
            canvas.drawCircle(x, y, 10f, landmarkPaint);
            Paint borderPaint = new Paint();
            borderPaint.setColor(Color.WHITE);
            borderPaint.setStrokeWidth(3f);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setShadowLayer(1f, 0f, 0f, Color.BLACK);
            canvas.drawCircle(x, y, 10f, borderPaint);
        }
    }
    
    private List<List<PoseDetector.NormalizedLandmark>> convertToCustomFormat(PoseLandmarkerResult result) {
        List<List<PoseDetector.NormalizedLandmark>> landmarksList = new ArrayList<>();
        for (List<NormalizedLandmark> poseLandmarks : result.landmarks()) {
            List<PoseDetector.NormalizedLandmark> customLandmarks = new ArrayList<>();
            for (NormalizedLandmark landmark : poseLandmarks) {
                PoseDetector.NormalizedLandmark customLandmark = new PoseDetector.NormalizedLandmark(
                    landmark.x(), landmark.y(), landmark.z()
                );
                customLandmarks.add(customLandmark);
            }
            landmarksList.add(customLandmarks);
        }
        return landmarksList;
    }
    
    public void close() {
        if (poseLandmarker != null) {
            poseLandmarker.close();
            poseLandmarker = null;
        }
        isInitialized = false;
    }
} 