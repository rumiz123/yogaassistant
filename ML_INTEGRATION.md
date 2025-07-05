# Yoga Pose Detection ML Integration

This document describes the machine learning integration for yoga pose detection in the Yoga Assistant app.

## Overview

The app now includes real-time yoga pose detection using TensorFlow Lite. When users start a workout, the camera will analyze the video feed and detect which yoga pose is being performed.

## Components

### 1. PoseClassifier.java
- Main ML classifier that loads the TensorFlow Lite model
- Processes camera frames and returns pose predictions
- Handles model initialization and inference

### 2. ImageUtils.java
- Utility class for converting camera images to bitmaps
- Handles image rotation and cropping for ML processing

### 3. CameraActivity.java (Updated)
- Integrated ML inference into the camera workflow
- Added real-time pose detection overlay
- Shows detected pose name and confidence level

## Model Files

- `model.tflite`: TensorFlow Lite model for pose classification
- `labels.txt`: Labels for the 6 yoga poses (Big Toe, Bridge, Chair, Corpse, Crescent Moon, Pyramid)

## Features

### Real-time Detection
- Camera analyzes frames at 30fps
- Shows detected pose name and confidence percentage
- Green text indicates high confidence (>70%)
- White text indicates low confidence

### UI Integration
- Detection overlay appears at the top of the camera screen
- Shows pose name and confidence percentage
- Non-intrusive design that doesn't block the camera view

## Technical Details

### Dependencies
```gradle
implementation 'org.tensorflow:tensorflow-lite:2.13.0'
implementation 'org.tensorflow:tensorflow-lite-support:0.4.2'
implementation 'org.tensorflow:tensorflow-lite-metadata:0.4.2'
implementation 'org.tensorflow:tensorflow-lite-gpu:2.13.0'
```

### Model Input
- Image size: 224x224 pixels
- Normalization: Mean=127.5, Std=127.5
- Format: RGB

### Model Output
- 6 class probabilities (one for each pose)
- Returns pose with highest confidence

## Usage

1. Navigate to the Workout tab
2. Tap any workout button to start
3. Camera will open with pose detection active
4. Perform yoga poses in front of the camera
5. The app will display the detected pose and confidence

## Performance

- Inference runs on background thread to avoid UI blocking
- Uses GPU acceleration when available
- Optimized for real-time performance on mobile devices

## Error Handling

- Graceful fallback if model fails to load
- Error logging for debugging
- Continues camera operation even if ML fails

## Future Enhancements

- Pose correction feedback
- Workout session tracking
- Pose transition detection
- Performance metrics 