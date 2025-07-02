package com.rumiznellasery.yogahelper.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.media.Image;
import android.widget.ImageView;

import org.pytorch.Module;
import org.pytorch.LiteModuleLoader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.rumiznellasery.yogahelper.R;
import com.rumiznellasery.yogahelper.camera.AssetUtils;
import com.rumiznellasery.yogahelper.camera.YuvToRgbConverter;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.io.IOException;

public class CameraActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private PreviewView previewView;
    private ImageView modelView;
    private Module module;
    private YuvToRgbConverter converter;
    private Bitmap bitmapBuffer;
    private static final int MODEL_INPUT_SIZE = 224;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        previewView = findViewById(R.id.view_finder);
        modelView = findViewById(R.id.model_input_view);
        converter = new YuvToRgbConverter(this);

        try {
            module = LiteModuleLoader.load(AssetUtils.assetFilePath(this, "yolo11s-yoga.pt"));
        } catch (IOException e) {
            throw new RuntimeException("Error loading model", e);
        }

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
                    Image mediaImage = image.getImage();
                    if (mediaImage != null) {
                        if (bitmapBuffer == null) {
                            bitmapBuffer = Bitmap.createBitmap(mediaImage.getWidth(), mediaImage.getHeight(), Bitmap.Config.ARGB_8888);
                        }
                        converter.yuvToRgb(mediaImage, bitmapBuffer);
                        Bitmap scaled = Bitmap.createScaledBitmap(bitmapBuffer, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true);
                        runOnUiThread(() -> modelView.setImageBitmap(scaled));
                    }
                    image.close();
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (converter != null) {
            converter.release();
        }
    }
}
