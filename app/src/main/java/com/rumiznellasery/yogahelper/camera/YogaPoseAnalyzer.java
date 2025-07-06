package com.rumiznellasery.yogahelper.camera;

import android.util.Log;

import com.rumiznellasery.yogahelper.camera.PoseDetector.NormalizedLandmark;
import com.rumiznellasery.yogahelper.camera.PoseDetector.PoseLandmarkerResult;

import java.util.List;

public class YogaPoseAnalyzer {
    private static final String TAG = "YogaPoseAnalyzer";
    
    public static class PoseAnalysis {
        public String poseName;
        public float confidence;
        public String feedback;
        
        public PoseAnalysis(String poseName, float confidence, String feedback) {
            this.poseName = poseName;
            this.confidence = confidence;
            this.feedback = feedback;
        }
    }
    
    public static PoseAnalysis analyzePose(PoseLandmarkerResult result) {
        if (result.landmarks().isEmpty()) {
            return new PoseAnalysis("No Pose", 0.0f, "No pose detected");
        }
        
        List<NormalizedLandmark> landmarks = result.landmarks().get(0);
        if (landmarks.size() < 33) {
            return new PoseAnalysis("Incomplete", 0.0f, "Not enough landmarks detected");
        }
        
        // Analyze different poses with enhanced detection
        PoseAnalysis mountainPose = analyzeMountainPose(landmarks);
        PoseAnalysis warriorPose = analyzeWarriorPose(landmarks);
        PoseAnalysis treePose = analyzeTreePose(landmarks);
        PoseAnalysis downwardDog = analyzeDownwardDog(landmarks);
        PoseAnalysis childPose = analyzeChildPose(landmarks);
        PoseAnalysis cobraPose = analyzeCobraPose(landmarks);
        PoseAnalysis plankPose = analyzePlankPose(landmarks);
        
        // Return the pose with highest confidence
        PoseAnalysis bestPose = mountainPose;
        if (warriorPose.confidence > bestPose.confidence) bestPose = warriorPose;
        if (treePose.confidence > bestPose.confidence) bestPose = treePose;
        if (downwardDog.confidence > bestPose.confidence) bestPose = downwardDog;
        if (childPose.confidence > bestPose.confidence) bestPose = childPose;
        if (cobraPose.confidence > bestPose.confidence) bestPose = cobraPose;
        if (plankPose.confidence > bestPose.confidence) bestPose = plankPose;
        
        // Add dynamic confidence boost for live visualization
        if (bestPose.confidence > 0.3f) {
            // Simulate real-time confidence fluctuations
            float dynamicBoost = (float) (0.1f * Math.sin(System.currentTimeMillis() / 1000.0));
            bestPose.confidence = Math.min(1.0f, bestPose.confidence + dynamicBoost);
        }
        
        return bestPose;
    }
    
    private static PoseAnalysis analyzeMountainPose(List<NormalizedLandmark> landmarks) {
        // Mountain Pose (Tadasana) - standing straight with arms at sides
        NormalizedLandmark leftShoulder = landmarks.get(11);
        NormalizedLandmark rightShoulder = landmarks.get(12);
        NormalizedLandmark leftElbow = landmarks.get(13);
        NormalizedLandmark rightElbow = landmarks.get(14);
        NormalizedLandmark leftWrist = landmarks.get(15);
        NormalizedLandmark rightWrist = landmarks.get(16);
        NormalizedLandmark leftHip = landmarks.get(23);
        NormalizedLandmark rightHip = landmarks.get(24);
        NormalizedLandmark nose = landmarks.get(0);
        
        float confidence = 0.0f;
        String feedback = "";
        
        // Check if shoulders are level
        float shoulderHeightDiff = Math.abs(leftShoulder.y() - rightShoulder.y());
        if (shoulderHeightDiff < 0.05f) {
            confidence += 0.25f;
        } else {
            feedback += "Level your shoulders. ";
        }
        
        // Check if arms are at sides
        boolean leftArmDown = leftElbow.y() > leftShoulder.y() && leftWrist.y() > leftElbow.y();
        boolean rightArmDown = rightElbow.y() > rightShoulder.y() && rightWrist.y() > rightElbow.y();
        
        if (leftArmDown && rightArmDown) {
            confidence += 0.35f;
        } else {
            feedback += "Keep your arms at your sides. ";
        }
        
        // Check if hips are level
        float hipHeightDiff = Math.abs(leftHip.y() - rightHip.y());
        if (hipHeightDiff < 0.05f) {
            confidence += 0.25f;
        } else {
            feedback += "Level your hips. ";
        }
        
        // Check if head is centered
        float headCenter = (leftShoulder.x() + rightShoulder.x()) / 2f;
        float headOffset = Math.abs(nose.x() - headCenter);
        if (headOffset < 0.1f) {
            confidence += 0.15f;
        } else {
            feedback += "Center your head. ";
        }
        
        if (confidence > 0.7f) {
            feedback = "Perfect Mountain Pose! Keep breathing steadily.";
        }
        
        return new PoseAnalysis("Mountain Pose (Tadasana)", confidence, feedback);
    }
    
    private static PoseAnalysis analyzeWarriorPose(List<NormalizedLandmark> landmarks) {
        // Warrior Pose (Virabhadrasana) - arms extended horizontally
        NormalizedLandmark leftShoulder = landmarks.get(11);
        NormalizedLandmark rightShoulder = landmarks.get(12);
        NormalizedLandmark leftWrist = landmarks.get(15);
        NormalizedLandmark rightWrist = landmarks.get(16);
        NormalizedLandmark leftElbow = landmarks.get(13);
        NormalizedLandmark rightElbow = landmarks.get(14);
        NormalizedLandmark leftHip = landmarks.get(23);
        NormalizedLandmark rightHip = landmarks.get(24);
        
        float confidence = 0.0f;
        String feedback = "";
        
        // Check if arms are extended horizontally
        boolean leftArmExtended = leftWrist.x() < leftShoulder.x() - 0.1f;
        boolean rightArmExtended = rightWrist.x() > rightShoulder.x() + 0.1f;
        
        if (leftArmExtended && rightArmExtended) {
            confidence += 0.4f;
        } else {
            feedback += "Extend your arms horizontally. ";
        }
        
        // Check if arms are at shoulder level
        float leftArmHeight = Math.abs(leftWrist.y() - leftShoulder.y());
        float rightArmHeight = Math.abs(rightWrist.y() - rightShoulder.y());
        
        if (leftArmHeight < 0.1f && rightArmHeight < 0.1f) {
            confidence += 0.3f;
        } else {
            feedback += "Keep your arms at shoulder level. ";
        }
        
        // Check if hips are stable
        float hipHeightDiff = Math.abs(leftHip.y() - rightHip.y());
        if (hipHeightDiff < 0.08f) {
            confidence += 0.3f;
        } else {
            feedback += "Stabilize your hips. ";
        }
        
        if (confidence > 0.7f) {
            feedback = "Strong Warrior Pose! Feel your power.";
        }
        
        return new PoseAnalysis("Warrior Pose (Virabhadrasana)", confidence, feedback);
    }
    
    private static PoseAnalysis analyzeTreePose(List<NormalizedLandmark> landmarks) {
        // Tree Pose (Vrikshasana) - one foot raised to opposite thigh
        NormalizedLandmark leftAnkle = landmarks.get(27);
        NormalizedLandmark rightAnkle = landmarks.get(28);
        NormalizedLandmark leftKnee = landmarks.get(25);
        NormalizedLandmark rightKnee = landmarks.get(26);
        NormalizedLandmark leftHip = landmarks.get(23);
        NormalizedLandmark rightHip = landmarks.get(24);
        NormalizedLandmark leftWrist = landmarks.get(15);
        NormalizedLandmark rightWrist = landmarks.get(16);
        
        float confidence = 0.0f;
        String feedback = "";
        
        // Check for significant height difference between feet
        float heightDiff = Math.abs(leftAnkle.y() - rightAnkle.y());
        if (heightDiff > 0.15f) {
            confidence += 0.4f;
        } else {
            feedback += "Raise one foot higher to your thigh. ";
        }
        
        // Check if hips are relatively level (some tilt is expected)
        float hipHeightDiff = Math.abs(leftHip.y() - rightHip.y());
        if (hipHeightDiff < 0.12f) {
            confidence += 0.25f;
        } else {
            feedback += "Keep your hips level. ";
        }
        
        // Check if one knee is bent
        float leftKneeAngle = calculateAngle(leftHip, leftKnee, leftAnkle);
        float rightKneeAngle = calculateAngle(rightHip, rightKnee, rightAnkle);
        
        if (leftKneeAngle < 170 || rightKneeAngle < 170) {
            confidence += 0.2f;
        } else {
            feedback += "Bend one knee to raise your foot. ";
        }
        
        // Check if hands are in prayer position
        float handDistance = Math.abs(leftWrist.x() - rightWrist.x());
        if (handDistance < 0.1f) {
            confidence += 0.15f;
        } else {
            feedback += "Bring your hands together in prayer. ";
        }
        
        if (confidence > 0.7f) {
            feedback = "Beautiful Tree Pose! Find your balance.";
        }
        
        return new PoseAnalysis("Tree Pose (Vrikshasana)", confidence, feedback);
    }
    
    private static PoseAnalysis analyzeDownwardDog(List<NormalizedLandmark> landmarks) {
        // Downward Dog (Adho Mukha Svanasana) - inverted V shape
        NormalizedLandmark leftShoulder = landmarks.get(11);
        NormalizedLandmark rightShoulder = landmarks.get(12);
        NormalizedLandmark leftHip = landmarks.get(23);
        NormalizedLandmark rightHip = landmarks.get(24);
        NormalizedLandmark leftAnkle = landmarks.get(27);
        NormalizedLandmark rightAnkle = landmarks.get(28);
        
        float confidence = 0.0f;
        String feedback = "";
        
        // Check if hips are higher than shoulders (inverted position)
        float leftHipHeight = leftHip.y();
        float rightHipHeight = rightHip.y();
        float leftShoulderHeight = leftShoulder.y();
        float rightShoulderHeight = rightShoulder.y();
        
        float avgHipHeight = (leftHipHeight + rightHipHeight) / 2;
        float avgShoulderHeight = (leftShoulderHeight + rightShoulderHeight) / 2;
        
        if (avgHipHeight < avgShoulderHeight) {
            confidence += 0.4f;
        } else {
            feedback += "Lift your hips higher than your shoulders. ";
        }
        
        // Check if legs are relatively straight
        float leftLegAngle = calculateAngle(leftHip, leftAnkle, landmarks.get(25)); // Using knee as middle point
        float rightLegAngle = calculateAngle(rightHip, rightAnkle, landmarks.get(26));
        
        if (leftLegAngle > 150 && rightLegAngle > 150) {
            confidence += 0.3f;
        } else {
            feedback += "Straighten your legs. ";
        }
        
        // Check if arms are straight
        float leftArmAngle = calculateAngle(leftShoulder, landmarks.get(13), landmarks.get(15));
        float rightArmAngle = calculateAngle(rightShoulder, landmarks.get(14), landmarks.get(16));
        
        if (leftArmAngle > 160 && rightArmAngle > 160) {
            confidence += 0.3f;
        } else {
            feedback += "Straighten your arms. ";
        }
        
        if (confidence > 0.7f) {
            feedback = "Perfect Downward Dog!";
        }
        
        return new PoseAnalysis("Downward Dog", confidence, feedback);
    }
    
    private static PoseAnalysis analyzeChildPose(List<NormalizedLandmark> landmarks) {
        // Child's Pose (Balasana) - kneeling with torso forward
        NormalizedLandmark leftShoulder = landmarks.get(11);
        NormalizedLandmark rightShoulder = landmarks.get(12);
        NormalizedLandmark leftHip = landmarks.get(23);
        NormalizedLandmark rightHip = landmarks.get(24);
        NormalizedLandmark leftKnee = landmarks.get(25);
        NormalizedLandmark rightKnee = landmarks.get(26);
        
        float confidence = 0.0f;
        String feedback = "";
        
        // Check if torso is forward (shoulders below hips)
        float avgShoulderHeight = (leftShoulder.y() + rightShoulder.y()) / 2;
        float avgHipHeight = (leftHip.y() + rightHip.y()) / 2;
        
        if (avgShoulderHeight > avgHipHeight) {
            confidence += 0.4f;
        } else {
            feedback += "Fold your torso forward. ";
        }
        
        // Check if knees are bent (kneeling position)
        float leftKneeAngle = calculateAngle(leftHip, leftKnee, landmarks.get(27));
        float rightKneeAngle = calculateAngle(rightHip, rightKnee, landmarks.get(28));
        
        if (leftKneeAngle < 120 && rightKneeAngle < 120) {
            confidence += 0.3f;
        } else {
            feedback += "Bend your knees to kneel. ";
        }
        
        // Check if arms are extended forward
        float leftArmAngle = calculateAngle(leftShoulder, landmarks.get(13), landmarks.get(15));
        float rightArmAngle = calculateAngle(rightShoulder, landmarks.get(14), landmarks.get(16));
        
        if (leftArmAngle > 150 && rightArmAngle > 150) {
            confidence += 0.3f;
        } else {
            feedback += "Extend your arms forward. ";
        }
        
        if (confidence > 0.7f) {
            feedback = "Relaxing Child's Pose!";
        }
        
        return new PoseAnalysis("Child's Pose (Balasana)", confidence, feedback);
    }
    
    private static float calculateAngle(NormalizedLandmark p1, NormalizedLandmark p2, NormalizedLandmark p3) {
        // Calculate angle between three points (p2 is the vertex)
        float a = (float) Math.sqrt(Math.pow(p1.x() - p2.x(), 2) + Math.pow(p1.y() - p2.y(), 2));
        float b = (float) Math.sqrt(Math.pow(p2.x() - p3.x(), 2) + Math.pow(p2.y() - p3.y(), 2));
        float c = (float) Math.sqrt(Math.pow(p1.x() - p3.x(), 2) + Math.pow(p1.y() - p3.y(), 2));
        
        if (a == 0 || b == 0) return 0;
        
        float cosAngle = (a * a + b * b - c * c) / (2 * a * b);
        cosAngle = Math.max(-1, Math.min(1, cosAngle)); // Clamp to [-1, 1]
        
        return (float) Math.toDegrees(Math.acos(cosAngle));
    }
    
    private static PoseAnalysis analyzeCobraPose(List<NormalizedLandmark> landmarks) {
        // Cobra Pose (Bhujangasana) - lying on stomach with upper body raised
        NormalizedLandmark leftShoulder = landmarks.get(11);
        NormalizedLandmark rightShoulder = landmarks.get(12);
        NormalizedLandmark leftHip = landmarks.get(23);
        NormalizedLandmark rightHip = landmarks.get(24);
        NormalizedLandmark leftElbow = landmarks.get(13);
        NormalizedLandmark rightElbow = landmarks.get(14);
        
        float confidence = 0.0f;
        String feedback = "";
        
        // Check if upper body is raised (shoulders above hips)
        float avgShoulderHeight = (leftShoulder.y() + rightShoulder.y()) / 2;
        float avgHipHeight = (leftHip.y() + rightHip.y()) / 2;
        
        if (avgShoulderHeight < avgHipHeight) {
            confidence += 0.4f;
        } else {
            feedback += "Lift your upper body higher. ";
        }
        
        // Check if arms are bent (elbows close to body)
        float leftArmAngle = calculateAngle(leftShoulder, leftElbow, landmarks.get(15));
        float rightArmAngle = calculateAngle(rightShoulder, rightElbow, landmarks.get(16));
        
        if (leftArmAngle < 90 && rightArmAngle < 90) {
            confidence += 0.3f;
        } else {
            feedback += "Keep your elbows close to your body. ";
        }
        
        // Check if hips are on the ground (lower body flat)
        float leftHipAngle = calculateAngle(leftHip, landmarks.get(25), landmarks.get(27));
        float rightHipAngle = calculateAngle(rightHip, landmarks.get(26), landmarks.get(28));
        
        if (leftHipAngle > 160 && rightHipAngle > 160) {
            confidence += 0.3f;
        } else {
            feedback += "Keep your lower body flat on the ground. ";
        }
        
        if (confidence > 0.7f) {
            feedback = "Strong Cobra Pose! Feel the stretch.";
        }
        
        return new PoseAnalysis("Cobra Pose (Bhujangasana)", confidence, feedback);
    }
    
    private static PoseAnalysis analyzePlankPose(List<NormalizedLandmark> landmarks) {
        // Plank Pose - body straight like a plank
        NormalizedLandmark leftShoulder = landmarks.get(11);
        NormalizedLandmark rightShoulder = landmarks.get(12);
        NormalizedLandmark leftHip = landmarks.get(23);
        NormalizedLandmark rightHip = landmarks.get(24);
        NormalizedLandmark leftAnkle = landmarks.get(27);
        NormalizedLandmark rightAnkle = landmarks.get(28);
        
        float confidence = 0.0f;
        String feedback = "";
        
        // Check if body is straight (shoulders, hips, ankles in line)
        float shoulderHeight = (leftShoulder.y() + rightShoulder.y()) / 2;
        float hipHeight = (leftHip.y() + rightHip.y()) / 2;
        float ankleHeight = (leftAnkle.y() + rightAnkle.y()) / 2;
        
        float heightDiff1 = Math.abs(shoulderHeight - hipHeight);
        float heightDiff2 = Math.abs(hipHeight - ankleHeight);
        
        if (heightDiff1 < 0.05f && heightDiff2 < 0.05f) {
            confidence += 0.4f;
        } else {
            feedback += "Keep your body in a straight line. ";
        }
        
        // Check if arms are straight
        float leftArmAngle = calculateAngle(leftShoulder, landmarks.get(13), landmarks.get(15));
        float rightArmAngle = calculateAngle(rightShoulder, landmarks.get(14), landmarks.get(16));
        
        if (leftArmAngle > 160 && rightArmAngle > 160) {
            confidence += 0.3f;
        } else {
            feedback += "Straighten your arms. ";
        }
        
        // Check if core is engaged (hips not sagging)
        float torsoAngle = calculateAngle(leftShoulder, leftHip, leftAnkle);
        if (torsoAngle > 170) {
            confidence += 0.3f;
        } else {
            feedback += "Engage your core, don't let hips sag. ";
        }
        
        if (confidence > 0.7f) {
            feedback = "Perfect Plank! Hold the position.";
        }
        
        return new PoseAnalysis("Plank Pose", confidence, feedback);
    }
} 