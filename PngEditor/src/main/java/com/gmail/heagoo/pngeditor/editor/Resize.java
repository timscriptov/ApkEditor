package com.gmail.heagoo.pngeditor.editor;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.gmail.heagoo.pngeditor.ImageEditor;

public class Resize implements ImageEditor {
    private int newWidth = 0;
    private int newHeight = 0;
    private boolean zooming = true;
    private boolean bModified = false;

    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();

        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    public static Bitmap getExpandShrinkBitmap(Bitmap bm, int newWidth, int newHeight) {
        Bitmap image = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        int width = bm.getWidth();
        int height = bm.getHeight();
        int left = (newWidth - width) / 2;
        int top = (newHeight - height) / 2;
        canvas.drawBitmap(bm, left, top, new Paint());
        return image;
    }

    @Override
    public void setParam(String name, Object value) {
        if ("width".equals(name)) {
            newWidth = (Integer) value;
        } else if ("height".equals(name)) {
            newHeight = (Integer) value;
        } else if ("zooming".equals(name)) {
            zooming = (Boolean) value;
        }
    }

    @Override
    public Bitmap edit(Bitmap bitmap) {
        if (newWidth > 0 && newHeight > 0) {
            bModified = true;
            if (zooming) {
                return getResizedBitmap(bitmap, newWidth, newHeight);
            } else {
                return getExpandShrinkBitmap(bitmap, newWidth, newHeight);
            }
        }
        return null;
    }

    @Override
    public boolean isModified() {
        return bModified;
    }
}
