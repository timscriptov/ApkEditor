package com.gmail.heagoo.common;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageTool {

    public static boolean saveAsPng(Bitmap bitmap, String filepath) {
        FileOutputStream out = null;

        try {
            out = new FileOutputStream(filepath);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(out);
        }

        return false;
    }

    // inSampleSize should be 2, 4, 8, 16, ...
    public void zoomToSmaller(Context ctx, String srcFile, String dstFile,
                              int inSampleSize) throws Exception {

        ContentResolver cr = ctx.getContentResolver();
        Uri uri = Uri.fromFile(new File(srcFile));

        // InputStream in = cr.openInputStream(uri);
        // BitmapFactory.Options options = new BitmapFactory.Options();
        // options.inJustDecodeBounds = true;
        // BitmapFactory.decodeStream(in, null, options);
        // try {
        // in.close();
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        // int mWidth = options.outWidth;
        // int mHeight = options.outHeight;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;
        InputStream in = cr.openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(in, null, options);
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (null == bitmap) {
            Toast.makeText(ctx,
                    "Head is not set successful,Decode bitmap failure",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // 原始图片的尺寸
        int bmpWidth = bitmap.getWidth();
        int bmpHeight = bitmap.getHeight();

        // 缩放图片的尺寸
        float scaleWidth = (float) 1.0 / inSampleSize;
        float scaleHeight = scaleWidth;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        // 产生缩放后的Bitmap对象
        Bitmap resizeBitmap = Bitmap.createBitmap(bitmap, 0, 0, bmpWidth,
                bmpHeight, matrix, false);
        bitmap.recycle();
        // Bitmap to byte[]
        byte[] photoData = bitmap2Bytes(resizeBitmap);

        // save file
        FileUtil.writeToFile(dstFile, photoData);
    }

    private byte[] bitmap2Bytes(Bitmap bitmap) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.JPEG, 80, os);
        return os.toByteArray();
    }

    // Zoom to target width and height
    public void zoomImage(Bitmap bitmap, int width, int height,
                          String outFilePath) {

        int bmpWidth = bitmap.getWidth();
        int bmpHeight = bitmap.getHeight();

        // Zoom ratio
        float scaleWidth = (float) width / bmpWidth;
        float scaleHeight = (float) height / bmpHeight;

        Matrix matrix = new Matrix();

        matrix.postScale(scaleWidth, scaleHeight);

        Bitmap resizeBitmap = Bitmap.createBitmap(bitmap, 0, 0, bmpWidth,
                bmpHeight, matrix, false);

        // Save to file
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(outFilePath);
            if (outFilePath.endsWith(".png")) {
                resizeBitmap.compress(CompressFormat.PNG, 80, os);
            } else {
                resizeBitmap.compress(CompressFormat.JPEG, 80, os);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuietly(os);
        }
    }

    private void closeQuietly(OutputStream os) {
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
            }
        }
    }
}