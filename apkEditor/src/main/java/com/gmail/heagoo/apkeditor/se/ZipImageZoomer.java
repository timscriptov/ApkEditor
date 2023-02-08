package com.gmail.heagoo.apkeditor.se;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipImageZoomer {
    private ZipFile zfile;
    private int originWidth;
    private int originHeight;

    public ZipImageZoomer(ZipFile zfile) {
        this.zfile = zfile;
    }

    public Bitmap getImageThumbnail(String entryName, int width, int height) {
        ZipEntry entry = null;
        InputStream input = null;
        try {
            entry = zfile.getEntry(entryName);
            input = zfile.getInputStream(entry);

            Bitmap bitmap = null;
            bitmap = BitmapFactory.decodeStream(input);

            originWidth = bitmap.getWidth();
            originHeight = bitmap.getHeight();
            int beWidth = originWidth / width;
            int beHeight = originHeight / height;
            int be = 1;
            if (beWidth < beHeight) {
                be = beWidth;
            } else {
                be = beHeight;
            }
            if (be <= 0) {
                be = 1;
            }

            if (be > 1) {
                bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                        ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
            }
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                forceClose(input);
            }
        }

        return null;
    }

    private void forceClose(Closeable c) {
        try {
            c.close();
        } catch (IOException e) {
        }
    }

    public int getOriginWidth() {
        return originWidth;
    }

    public int getOriginHeight() {
        return originHeight;
    }
}
