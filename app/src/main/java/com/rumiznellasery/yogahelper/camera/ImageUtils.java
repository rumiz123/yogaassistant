package com.rumiznellasery.yogahelper.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ImageUtils {
    private static final String TAG = "ImageUtils";

    public static Bitmap imageToBitmap(Image image) {
        if (image == null) {
            return null;
        }

        try {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error converting image to bitmap", e);
            return null;
        }
    }

    public static Bitmap yuvToBitmap(Image image) {
        if (image == null) {
            return null;
        }

        try {
            Image.Plane[] planes = image.getPlanes();
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

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
            byte[] imageBytes = out.toByteArray();
            
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error converting YUV image to bitmap", e);
            return null;
        }
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int rotation) {
        if (bitmap == null) {
            return null;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap cropBitmap(Bitmap bitmap, int targetWidth, int targetHeight) {
        if (bitmap == null) {
            return null;
        }

        int x = (bitmap.getWidth() - targetWidth) / 2;
        int y = (bitmap.getHeight() - targetHeight) / 2;
        
        if (x < 0 || y < 0 || x + targetWidth > bitmap.getWidth() || y + targetHeight > bitmap.getHeight()) {
            // If crop would be out of bounds, scale the bitmap instead
            return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
        }
        
        return Bitmap.createBitmap(bitmap, x, y, targetWidth, targetHeight);
    }
} 