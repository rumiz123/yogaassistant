package com.rumiznellasery.yogahelper.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;

import java.nio.ByteBuffer;

public class YuvToRgbConverter {
    private final RenderScript rs;
    private final ScriptIntrinsicYuvToRGB script;
    private Allocation in;
    private Allocation out;

    public YuvToRgbConverter(Context context) {
        rs = RenderScript.create(context);
        script = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }

    public void yuvToRgb(Image image, Bitmap output) {
        if (image.getFormat() != ImageFormat.YUV_420_888) return;
        int width = image.getWidth();
        int height = image.getHeight();
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);
        if (in == null) {
            in = Allocation.createSized(rs, Element.U8(rs), nv21.length);
        }
        if (out == null) {
            out = Allocation.createFromBitmap(rs, output);
        } else if (out.getType().getX() != width || out.getType().getY() != height) {
            out.destroy();
            out = Allocation.createFromBitmap(rs, output);
        }
        in.copyFrom(nv21);
        script.setInput(in);
        script.forEach(out);
        out.copyTo(output);
    }

    public void release() {
        rs.destroy();
    }
}
