package com.gmail.heagoo.pngeditor.editor;

import android.graphics.Bitmap;

import com.gmail.heagoo.pngeditor.ImageEditor;

public class RemoveBackground implements ImageEditor {
    private int color = 0xffffff;
    private int tolerance = 0;

    private boolean bModified = false;

    private static int getColorDistance(int c1, int c2) {
        int diff1 = (c1 & 0xff) - (c2 & 0xff);
        int diff2 = ((c1 >> 8) & 0xff) - ((c2 >> 8) & 0xff);
        int diff3 = ((c1 >> 16) & 0xff) - ((c2 >> 16) & 0xff);
        return diff1 * diff1 + diff2 * diff2 + diff3 * diff3;
    }

    // tolerance = 0 means exactly match
    public static Bitmap removeBackground(Bitmap rgbBitmap, int color, int tolerance) {
        // Make sure color is in 32bits
        color = color | 0xff000000;
        int width = rgbBitmap.getWidth();
        int height = rgbBitmap.getHeight();

        tolerance *= 3;
        int tor_square = tolerance * tolerance;

        Bitmap retBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width];
        for (int y = 0; y < height; y++) {
            rgbBitmap.getPixels(pixels, 0, width, 0, y, width, 1);
            if (tolerance == 0) {
                for (int x = 0; x < width; x++) {
                    // If matches the background color, then make it transparent
                    if (pixels[x] == color) {
                        pixels[x] = 0;
                    }
                }
            } else {
                for (int x = 0; x < width; x++) {
                    if (getColorDistance(pixels[x], color) <= tor_square) {
                        pixels[x] = 0;
                    }
                }
            }
            retBitmap.setPixels(pixels, 0, width, 0, y, width, 1);
        }

        return retBitmap;
    }

    @Override
    public void setParam(String name, Object value) {
        if ("color".equals(name)) {
            this.color = (Integer) value;
        } else if ("tolerance".equals(name)) {
            this.tolerance = (Integer) value;
        }
    }

    @Override
    public Bitmap edit(Bitmap bitmap) {
        bModified = true;
        return removeBackground(bitmap, color, tolerance);
    }

    @Override
    public boolean isModified() {
        return bModified;
    }
}
