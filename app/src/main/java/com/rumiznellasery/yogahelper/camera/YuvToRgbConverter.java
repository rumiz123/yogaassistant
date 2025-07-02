package com.rumiznellasery.yogahelper.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;

/**
 * Converts YUV camera frames to RGB Bitmaps.
 *
 * <p>This wrapper uses the AndroidX {@code YuvToRgbConverter} so that the
 * application no longer depends on the platform RenderScript API, which was
 * removed in newer Android versions.</p>
 */
public class YuvToRgbConverter {
    private final androidx.core.graphics.YuvToRgbConverter converter;

    public YuvToRgbConverter(Context context) {
        converter = new androidx.core.graphics.YuvToRgbConverter(context);
    }

    public void yuvToRgb(Image image, Bitmap output) {
        converter.yuvToRgb(image, output);
    }

    public void release() {
        // no-op for AndroidX implementation
    }
}
