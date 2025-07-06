package com.rumiznellasery.yogahelper.camera;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class WorkoutSession {
    private static final String TAG = "WorkoutSession";
    
    public static class Pose {
        public final String name;
        public final String description;
        public final int durationSeconds;
        public final String instructions;
        
        public Pose(String name, String description, int durationSeconds, String instructions) {
            this.name = name;
            this.description = description;
            this.durationSeconds = durationSeconds;
            this.instructions = instructions;
        }
    }
    
    public interface WorkoutListener {
        void onPoseStarted(Pose pose, int poseIndex, int totalPoses);
        void onPoseProgress(Pose pose, int secondsRemaining);
        void onPoseCompleted(Pose pose, int poseIndex);
        void onWorkoutCompleted();
        void onWorkoutPaused();
        void onWorkoutResumed();
        void onLaunchPosePreparation(Pose pose, int poseIndex, int totalPoses);
    }
    
    private final List<Pose> poses;
    private final WorkoutListener listener;
    private final Handler handler;
    private final Runnable progressRunnable;
    private final Context context;
    
    private int currentPoseIndex = 0;
    private int currentPoseTimeRemaining = 0;
    private boolean isActive = false;
    private boolean isPaused = false;
    
    public WorkoutSession(Context context, WorkoutListener listener) {
        this.context = context;
        this.listener = listener;
        this.handler = new Handler(Looper.getMainLooper());
        this.poses = createWorkoutPoses();
        
        this.progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (isActive && !isPaused) {
                    updateProgress();
                }
            }
        };
    }
    
    private List<Pose> createWorkoutPoses() {
        List<Pose> poses = new ArrayList<>();
        
        // 1. Easy Standing Pose - Foundation
        poses.add(new Pose(
            "Mountain Pose",
            "Stand tall with feet together",
            25,
            "Stand with feet together, arms at sides. Take deep breaths and feel grounded. This is your foundation."
        ));
        
        // 2. Gentle Arm Stretch - Shoulder mobility
        poses.add(new Pose(
            "Gentle Arm Stretch",
            "Raise arms slowly overhead",
            20,
            "Stand with feet hip-width apart. Slowly raise your arms overhead, keeping them shoulder-width apart. Hold and breathe."
        ));
        
        // 3. Easy Side Stretch - Gentle side bend
        poses.add(new Pose(
            "Easy Side Stretch",
            "Gently bend to one side",
            20,
            "Stand with feet hip-width apart. Raise your right arm overhead and gently bend to the left. Switch sides halfway through."
        ));
        
        // 4. Gentle Forward Fold - Hamstring stretch
        poses.add(new Pose(
            "Gentle Forward Fold",
            "Bend forward with bent knees",
            25,
            "Stand with feet hip-width apart, knees slightly bent. Fold forward from your hips, letting your arms hang. Don't force the stretch."
        ));
        
        // 5. Easy Squat - Leg strength
        poses.add(new Pose(
            "Easy Squat",
            "Squat down comfortably",
            20,
            "Stand with feet hip-width apart. Slowly squat down as if sitting in a chair. Keep your heels on the ground."
        ));
        
        // 6. Gentle Knee Stretch - Hip mobility
        poses.add(new Pose(
            "Gentle Knee Stretch",
            "Bring knee toward chest",
            20,
            "Stand with feet hip-width apart. Gently lift your right knee toward your chest, holding it with your hands. Switch legs halfway through."
        ));
        
        // 7. Easy Back Stretch - Spine mobility
        poses.add(new Pose(
            "Easy Back Stretch",
            "Gentle back arch and curl",
            25,
            "Stand with feet hip-width apart. Place your hands on your lower back. Gently arch your back, then round it forward."
        ));
        
        // 8. Gentle Neck Stretch - Neck mobility
        poses.add(new Pose(
            "Gentle Neck Stretch",
            "Slowly turn head side to side",
            20,
            "Stand with feet hip-width apart. Slowly turn your head to the right, then to the left. Keep your shoulders relaxed."
        ));
        
        // 9. Easy Balance - Simple balance
        poses.add(new Pose(
            "Easy Balance",
            "Stand on one leg briefly",
            20,
            "Stand with feet hip-width apart. Gently lift your right foot slightly off the ground. Hold for a few seconds, then switch legs."
        ));
        
        // 10. Relaxation Pose - Final rest
        poses.add(new Pose(
            "Relaxation Pose",
            "Stand and breathe deeply",
            25,
            "Stand with feet hip-width apart, arms at sides. Take deep breaths and feel the benefits of your practice."
        ));
        
        return poses;
    }
    
    public void startWorkout() {
        if (poses.isEmpty()) {
            Log.w(TAG, "No poses available for workout");
            return;
        }
        
        isActive = true;
        isPaused = false;
        currentPoseIndex = 0;
        launchPosePreparation();
    }
    
    public void pauseWorkout() {
        isPaused = true;
        if (listener != null) {
            listener.onWorkoutPaused();
        }
    }
    
    public void resumeWorkout() {
        isPaused = false;
        if (listener != null) {
            listener.onWorkoutResumed();
        }
        // Continue with current pose
        if (isActive) {
            scheduleNextProgressUpdate();
        }
    }
    
    public void stopWorkout() {
        isActive = false;
        isPaused = false;
        handler.removeCallbacks(progressRunnable);
    }
    
    public void skipToNextPose() {
        if (currentPoseIndex < poses.size() - 1) {
            currentPoseIndex++;
            startCurrentPose();
        } else {
            completeWorkout();
        }
    }
    
    public void launchPosePreparation() {
        if (currentPoseIndex >= poses.size()) {
            completeWorkout();
            return;
        }
        
        Pose currentPose = poses.get(currentPoseIndex);
        
        if (listener != null) {
            listener.onLaunchPosePreparation(currentPose, currentPoseIndex + 1, poses.size());
        }
    }
    
    public void startCurrentPose() {
        if (currentPoseIndex >= poses.size()) {
            completeWorkout();
            return;
        }
        
        Pose currentPose = poses.get(currentPoseIndex);
        currentPoseTimeRemaining = currentPose.durationSeconds;
        
        if (listener != null) {
            listener.onPoseStarted(currentPose, currentPoseIndex + 1, poses.size());
        }
        
        scheduleNextProgressUpdate();
    }
    
    private void updateProgress() {
        if (!isActive || isPaused) {
            return;
        }
        
        Pose currentPose = poses.get(currentPoseIndex);
        
        if (listener != null) {
            listener.onPoseProgress(currentPose, currentPoseTimeRemaining);
        }
        
        currentPoseTimeRemaining--;
        
        if (currentPoseTimeRemaining <= 0) {
            // Pose completed
            if (listener != null) {
                listener.onPoseCompleted(currentPose, currentPoseIndex + 1);
            }
            
            // Move to next pose
            currentPoseIndex++;
            if (currentPoseIndex < poses.size()) {
                // Launch preparation for next pose
                launchPosePreparation();
            } else {
                completeWorkout();
            }
        } else {
            // Continue current pose
            scheduleNextProgressUpdate();
        }
    }
    
    private void scheduleNextProgressUpdate() {
        handler.postDelayed(progressRunnable, 1000); // Update every second
    }
    
    private void completeWorkout() {
        isActive = false;
        handler.removeCallbacks(progressRunnable);
        
        if (listener != null) {
            listener.onWorkoutCompleted();
        }
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public boolean isPaused() {
        return isPaused;
    }
    
    public int getCurrentPoseIndex() {
        return currentPoseIndex;
    }
    
    public int getTotalPoses() {
        return poses.size();
    }
    
    public Pose getCurrentPose() {
        if (currentPoseIndex < poses.size()) {
            return poses.get(currentPoseIndex);
        }
        return null;
    }
    
    public Pose getPoseByIndex(int index) {
        if (index >= 0 && index < poses.size()) {
            return poses.get(index);
        }
        return null;
    }
} 