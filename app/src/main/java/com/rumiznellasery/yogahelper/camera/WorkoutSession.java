package com.rumiznellasery.yogahelper.camera;

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
    }
    
    private final List<Pose> poses;
    private final WorkoutListener listener;
    private final Handler handler;
    private final Runnable progressRunnable;
    
    private int currentPoseIndex = 0;
    private int currentPoseTimeRemaining = 0;
    private boolean isActive = false;
    private boolean isPaused = false;
    
    public WorkoutSession(WorkoutListener listener) {
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
        List<Pose> workoutPoses = new ArrayList<>();
        
        // Beginner-friendly yoga sequence
        workoutPoses.add(new Pose(
            "Mountain Pose",
            "Stand tall with feet together",
            30,
            "Stand with feet together, arms at sides. Breathe deeply and find your balance."
        ));
        
        workoutPoses.add(new Pose(
            "Forward Fold",
            "Bend forward from hips",
            25,
            "From mountain pose, fold forward from your hips. Let your head hang and relax your neck."
        ));
        
        workoutPoses.add(new Pose(
            "Half Lift",
            "Lift halfway up",
            20,
            "From forward fold, lift your chest halfway up. Keep your back straight and look forward."
        ));
        
        workoutPoses.add(new Pose(
            "Plank Pose",
            "Hold plank position",
            30,
            "Step back into plank. Keep your body in a straight line from head to heels."
        ));
        
        workoutPoses.add(new Pose(
            "Downward Dog",
            "Form an inverted V",
            35,
            "From plank, lift your hips up and back. Press your heels toward the ground."
        ));
        
        workoutPoses.add(new Pose(
            "Warrior I",
            "Step forward into warrior",
            25,
            "Step your right foot forward. Raise your arms overhead and bend your front knee."
        ));
        
        workoutPoses.add(new Pose(
            "Warrior II",
            "Open to the side",
            30,
            "Open your hips to the side. Extend your arms parallel to the ground."
        ));
        
        workoutPoses.add(new Pose(
            "Tree Pose",
            "Balance on one leg",
            20,
            "Stand on your left leg. Place your right foot on your left thigh or calf."
        ));
        
        workoutPoses.add(new Pose(
            "Child's Pose",
            "Rest in child's pose",
            15,
            "Kneel and sit back on your heels. Fold forward and rest your forehead on the ground."
        ));
        
        workoutPoses.add(new Pose(
            "Corpse Pose",
            "Final relaxation",
            20,
            "Lie on your back with arms and legs relaxed. Close your eyes and breathe deeply."
        ));
        
        return workoutPoses;
    }
    
    public void startWorkout() {
        if (poses.isEmpty()) {
            Log.w(TAG, "No poses available for workout");
            return;
        }
        
        isActive = true;
        isPaused = false;
        currentPoseIndex = 0;
        startCurrentPose();
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
    
    private void startCurrentPose() {
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
                startCurrentPose();
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
} 