package com.gmail.heagoo.pngeditor.editor;

import android.graphics.Bitmap;

import com.gmail.heagoo.pngeditor.ImageEditor;

public class Transparency implements ImageEditor {
    private int opaque = 0xff;
    private boolean bModified = false;

    @Override
    public void setParam(String name, Object value) {
        if ("transparency".equals(name)) {
            int transparency = (Integer) value;
            if (transparency > 255) transparency = 255;
            if (transparency < 0) transparency = 0;
            opaque = 255 - transparency;
        }
    }

    @Override
    public Bitmap edit(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Bitmap retBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width];
        for (int y = 0; y < height; y++) {
            bitmap.getPixels(pixels, 0, width, 0, y, width, 1);
            for (int x = 0; x < width; x++) {
                // opaque: 0->0, 255->opaque
                int oldOpaque = (pixels[x] >> 24) & 0xff;
                // Not transparent
                if (pixels[x] != 0) {
                    int o = opaque * oldOpaque / 255;
                    pixels[x] = (o << 24) | (pixels[x] & 0xffffff);
                }
            }
            retBitmap.setPixels(pixels, 0, width, 0, y, width, 1);
        }

        bModified = true;
        return retBitmap;
    }

    @Override
    public boolean isModified() {
        return bModified;
    }
}
