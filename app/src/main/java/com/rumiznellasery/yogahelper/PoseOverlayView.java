package com.rumiznellasery.yogahelper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.util.List;

public class PoseOverlayView extends View {
    private static final String TAG = "PoseOverlayView";
    
    private Paint landmarkPaint;
    private Paint connectionPaint;
    private Paint textPaint;
    
    private PoseLandmarkerResult poseResult;
    private int imageWidth;
    private int imageHeight;
    private int viewWidth;
    private int viewHeight;
    
    // Pose landmark connections (MediaPipe pose model has 33 landmarks)
    private static final int[][] POSE_CONNECTIONS = {
        // Face
        {0, 1}, {1, 2}, {2, 3}, {3, 7}, {0, 4}, {4, 5}, {5, 6}, {6, 8},
        {9, 10}, {11, 12}, {11, 13}, {13, 15}, {12, 14}, {14, 16},
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

    public PoseOverlayView(Context context) {
        super(context);
        init();
    }

    public PoseOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PoseOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        landmarkPaint = new Paint();
        landmarkPaint.setColor(Color.RED);
        landmarkPaint.setStyle(Paint.Style.FILL);
        landmarkPaint.setAntiAlias(true);
        
        connectionPaint = new Paint();
        connectionPaint.setColor(Color.GREEN);
        connectionPaint.setStyle(Paint.Style.STROKE);
        connectionPaint.setStrokeWidth(3f);
        connectionPaint.setAntiAlias(true);
        
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30f);
        textPaint.setAntiAlias(true);
    }

    public void setPoseResult(PoseLandmarkerResult result, int imageWidth, int imageHeight) {
        this.poseResult = result;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (poseResult == null || poseResult.landmarks().isEmpty()) {
            return;
        }

        // Use the type as returned by the API (no explicit import)
        java.util.List<?> landmarks = poseResult.landmarks().get(0);

        // Draw connections
        for (int[] connection : POSE_CONNECTIONS) {
            if (connection[0] < landmarks.size() && connection[1] < landmarks.size()) {
                var start = landmarks.get(connection[0]);
                var end = landmarks.get(connection[1]);
                
                // Use the new helper method to handle Optional<Float>
                float startX = getOptionalFloat(start, "x");
                float startY = getOptionalFloat(start, "y");
                float startVis = getOptionalFloat(start, "visibility");
                float endX = getOptionalFloat(end, "x");
                float endY = getOptionalFloat(end, "y");
                float endVis = getOptionalFloat(end, "visibility");
                
                if (startVis > 0.5f && endVis > 0.5f) {
                    PointF startPoint = normalizedToViewCoordinates(startX, startY);
                    PointF endPoint = normalizedToViewCoordinates(endX, endY);
                    
                    canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, connectionPaint);
                }
            }
        }

        // Draw landmarks
        for (int i = 0; i < landmarks.size(); i++) {
            var landmark = landmarks.get(i);
            float x = getOptionalFloat(landmark, "x");
            float y = getOptionalFloat(landmark, "y");
            float vis = getOptionalFloat(landmark, "visibility");
            if (vis > 0.5f) {
                PointF point = normalizedToViewCoordinates(x, y);
                
                // Different colors for different landmark types
                if (i <= 10) {
                    landmarkPaint.setColor(Color.YELLOW); // Face
                } else if (i <= 22) {
                    landmarkPaint.setColor(Color.RED); // Upper body
                } else {
                    landmarkPaint.setColor(Color.BLUE); // Lower body
                }
                
                canvas.drawCircle(point.x, point.y, 8f, landmarkPaint);
                
                // Draw landmark number for debugging
                if (i % 5 == 0) { // Only show every 5th landmark number to avoid clutter
                    canvas.drawText(String.valueOf(i), point.x + 15, point.y - 15, textPaint);
                }
            }
        }
    }

    private PointF normalizedToViewCoordinates(float normalizedX, float normalizedY) {
        // Convert normalized coordinates (0-1) to view coordinates
        float x = normalizedX * viewWidth;
        float y = normalizedY * viewHeight;
        return new PointF(x, y);
    }

    // Helper to handle Optional<Float> return types from MediaPipe API
    private float getOptionalFloat(Object obj, String methodName) {
        try {
            Object result = obj.getClass().getMethod(methodName).invoke(obj);
            if (result instanceof java.util.Optional) {
                java.util.Optional<?> opt = (java.util.Optional<?>) result;
                if (opt.isPresent()) {
                    Object value = opt.get();
                    if (value instanceof Float) {
                        return (Float) value;
                    } else if (value instanceof Double) {
                        return ((Double) value).floatValue();
                    } else if (value instanceof Number) {
                        return ((Number) value).floatValue();
                    }
                }
            } else if (result instanceof Float) {
                return (Float) result;
            } else if (result instanceof Double) {
                return ((Double) result).floatValue();
            } else if (result instanceof Number) {
                return ((Number) result).floatValue();
            }
        } catch (Exception e) {
            // ignore
        }
        return 0f;
    }
} 