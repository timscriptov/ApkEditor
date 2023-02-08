package com.gmail.heagoo.apkeditor.ce;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.gmail.heagoo.apkeditor.ApkInfoActivity;
import com.gmail.heagoo.apkeditor.se.ZipImageZoomer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import brut.androlib.res.data.ResConfigFlags;
import brut.androlib.res.data.ResID;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResSpec;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.data.value.ResFileValue;
import brut.androlib.res.data.value.ResStringValue;
import brut.androlib.res.data.value.ResValue;
import brut.androlib.res.decoder.ARSCDecoder;

public class ApkParser {
    private Context ctx;
    private String apkFilePath;

    private String appName;
    private Bitmap appIcon;

    public ApkParser(Context ctx, String apkFilePath) {
        this.ctx = ctx;
        this.apkFilePath = apkFilePath;
    }

    public String getAppName() {
        return appName;
    }

    public Bitmap getAppIcon() {
        return appIcon;
    }

    // appNameId: resource id for app name
    // launcherId: resource id for app icon
    public void parse(int appNameId, int launcherId) {
        ZipFile zipFile = null;
        InputStream arscStream = null;

        try {
            zipFile = new ZipFile(apkFilePath);
            ZipEntry entry = zipFile.getEntry("resources.arsc");
            arscStream = zipFile.getInputStream(entry);

            ResTable resTable = new ResTable(ctx.getApplicationContext(), false);
            ARSCDecoder.ARSCData arscData = ARSCDecoder.decode(arscStream, false, false,
                    resTable, null, false);
            ResPackage[] packages = arscData.getPackages();

            for (ResPackage pkg : packages) {
                if (appNameId != -1) {
                    parseAppName(pkg, appNameId);
                }
                if (launcherId != -1) {
                    parseAppIcon(zipFile, pkg, launcherId);
                }
            }
        } catch (Exception e) {
        } finally {
            closeQuietly(arscStream);
            closeQuietly(zipFile);
        }
    }

    private void closeQuietly(InputStream input) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException e) {
            }
        }
    }

    private void closeQuietly(ZipFile zipFile) {
        if (zipFile != null) {
            try {
                zipFile.close();
            } catch (IOException e) {
            }
        }
    }

    private void parseAppIcon(ZipFile zipFile, ResPackage pkg, int launcherId) {
        try {
            String entryPath = null;
            ResResSpec spec = pkg.getResSpec(new ResID(launcherId));
            Map<ResConfigFlags, ResResource> all = spec.getAllResources();

            int maxWidth = 0;
            ZipImageZoomer zipImageTool = new ZipImageZoomer(zipFile);
            for (ResResource res : all.values()) {
                ResValue resValue = res.getValue();
                if (resValue instanceof ResFileValue) {
                    entryPath = ((ResFileValue) resValue).getPath();
                } else if (resValue instanceof ResStringValue) {
                    entryPath = resValue.toString();
                } else {
                    entryPath = "res/" + res.getFilePath() + ".png";
                }
                ImageBounds bounds = getImageBounds(zipFile, entryPath);
                if (bounds.width > maxWidth) {
                    appIcon = zipImageTool.getImageThumbnail(
                            entryPath, bounds.width, bounds.height);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseAppName(ResPackage pkg, int appNameId) {
        try {
            ResResSpec spec = pkg.getResSpec(new ResID(appNameId));
            Map<ResConfigFlags, ResResource> all = spec.getAllResources();
            ResConfigFlags config = ApkInfoActivity.getBestConfigFlags(all.keySet());
            ResResource resource = spec.getResource(config);
            ResValue resValue = resource.getValue();
            if (resValue instanceof ResStringValue) {
                appName = resValue.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Get the iamge bounds in zip file
    private ImageBounds getImageBounds(ZipFile zipFile, String entryPath) {
        ZipEntry entry = zipFile.getEntry(entryPath);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream is = null;
        try {
            is = zipFile.getInputStream(entry);
            BitmapFactory.decodeStream(is, null, options);

            ImageBounds bounds = new ImageBounds();
            bounds.height = options.outHeight;
            bounds.width = options.outWidth;
            return bounds;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }

        return null;
    }

    private static class ImageBounds {
        int width;
        int height;
    }

}
