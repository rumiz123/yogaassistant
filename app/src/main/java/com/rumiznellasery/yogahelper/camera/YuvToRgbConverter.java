package com.rumiznellasery.yogahelper.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;

/**
 * Converts YUV camera frames to RGB Bitmaps.
 *
 * <p>This implementation uses pure Java for YUV to RGB conversion.</p>
 */
public class YuvToRgbConverter {

    public YuvToRgbConverter(Context context) {
        // No initialization needed for pure Java implementation
    }

    public void yuvToRgb(Image image, Bitmap output) {
        // Ensure the image is in YUV_420_888 format
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Expected YUV_420_888 image format");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        Image.Plane[] planes = image.getPlanes();
        Image.Plane yPlane = planes[0];
        Image.Plane uPlane = planes[1];
        Image.Plane vPlane = planes[2];

        java.nio.ByteBuffer yBuffer = yPlane.getBuffer();
        java.nio.ByteBuffer uBuffer = uPlane.getBuffer();
        java.nio.ByteBuffer vBuffer = vPlane.getBuffer();

        int yStride = yPlane.getRowStride();
        int uvStride = uPlane.getRowStride();
        int uvPixelStride = uPlane.getPixelStride();

        // Create output bitmap if needed
        if (output == null || output.getWidth() != width || output.getHeight() != height) {
            output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }

        int[] argb = new int[width * height];
        
        // Convert YUV to RGB
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Get Y value
                int yValue = yBuffer.get(y * yStride + x) & 0xff;
                
                // Get U and V values
                int uvX = x / 2;
                int uvY = y / 2;
                int uValue = uBuffer.get(uvY * uvStride + uvX * uvPixelStride) & 0xff;
                int vValue = vBuffer.get(uvY * uvStride + uvX * uvPixelStride) & 0xff;
                
                // Convert YUV to RGB
                int r = yuvToR(yValue, uValue, vValue);
                int g = yuvToG(yValue, uValue, vValue);
                int b = yuvToB(yValue, uValue, vValue);
                
                // Clamp values
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                
                // Set ARGB value
                argb[y * width + x] = (255 << 24) | (r << 16) | (g << 8) | b;
            }
        }
        
        output.setPixels(argb, 0, width, 0, 0, width, height);
    }

    private int yuvToR(int y, int u, int v) {
        return y + (int) (1.402 * (v - 128));
    }

    private int yuvToG(int y, int u, int v) {
        return y - (int) (0.344 * (u - 128)) - (int) (0.714 * (v - 128));
    }

    private int yuvToB(int y, int u, int v) {
        return y + (int) (1.772 * (u - 128));
    }

    public void release() {
        // No resources to release for pure Java implementation
    }
}
