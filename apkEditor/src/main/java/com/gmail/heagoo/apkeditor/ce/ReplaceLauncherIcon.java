package com.gmail.heagoo.apkeditor.ce;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.gmail.heagoo.common.SDCard;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import brut.androlib.res.data.ResConfigFlags;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResSpec;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.data.value.ResFileValue;
import brut.androlib.res.data.value.ResStringValue;
import brut.androlib.res.data.value.ResValue;
import brut.androlib.res.decoder.ARSCDecoder;
import brut.androlib.res.decoder.ARSCDecoder.ARSCData;

public class ReplaceLauncherIcon implements IApkMaking, Serializable {

    private static final long serialVersionUID = 1975576048832764645L;

    private int launcherId;
    private String newIconPath;

    public ReplaceLauncherIcon(int launcherId, String newIconPath) {
        this.launcherId = launcherId;
        this.newIconPath = newIconPath;
    }

    @Override
    public void prepareReplaces(Context ctx, String apkFilePath,
                                Map<String, String> allReplaces, IDescriptionUpdate updater) {
        ZipFile zipFile = null;
        InputStream arscStream = null;

        try {
            zipFile = new ZipFile(apkFilePath);
            ZipEntry entry = zipFile.getEntry("resources.arsc");
            arscStream = zipFile.getInputStream(entry);

            ResTable resTable = new ResTable(ctx.getApplicationContext(), false);
            ARSCData arscData = ARSCDecoder.decode(arscStream, false, false,
                    resTable, null, false);
            ResPackage[] packages = arscData.getPackages();

            for (ResPackage pkg : packages) {
                for (ResResSpec spec : pkg.listResSpecs()) {
                    if (launcherId == spec.getId().id) {
                        Map<ResConfigFlags, ResResource> all = spec
                                .getAllResources();
                        int index = 0;
                        Bitmap newIconBitmap = BitmapFactory.decodeFile(newIconPath);
                        for (ResResource res : all.values()) {
                            String filePath = SDCard.makeWorkingDir(ctx)
                                    + ".launcher" + index + ".png";
                            // Get image size
                            // First try to get real entry path
                            String entryPath = null;
                            ResValue resValue = res.getValue();
                            if (resValue instanceof ResFileValue) {
                                entryPath = ((ResFileValue) resValue).getPath();
                            } else if (resValue instanceof ResStringValue) {
                                entryPath = resValue.toString();
                            } else {
                                entryPath = "res/" + res.getFilePath() + ".png";
                            }
                            // Log.d("DEBUG", "entry name = " + entryPath);
                            ImageBounds bounds = getImageBounds(zipFile, entryPath);
                            if (bounds != null) {
                                com.gmail.heagoo.common.ImageTool tool = new com.gmail.heagoo.common.ImageTool();
                                // Log.d("DEBUG", filePath + ": " + bounds.width
                                // + ", " + bounds.height);
                                tool.zoomImage(newIconBitmap, bounds.width,
                                        bounds.height, filePath);
                                allReplaces.put(entryPath, filePath);
                            } else {
                                allReplaces.put(entryPath, newIconPath);
                            }
                            index += 1;
                        }
                        break;
                    }

                }
            }

        } catch (Exception e) {

        } finally {
            if (arscStream != null) {
                try {
                    arscStream.close();
                } catch (IOException e) {
                }
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                }
            }
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

    class ImageBounds {
        int width;
        int height;
    }

//	@Override
//	public String getDescription() {
//		return "";
//	}
}
