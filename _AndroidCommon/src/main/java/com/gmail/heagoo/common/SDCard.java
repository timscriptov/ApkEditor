package com.gmail.heagoo.common;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;

import com.gmail.heagoo.common.StorageUtils.StorageInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class SDCard {

    public static boolean exist() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    // Return like "/sdcard"
    public static String getRootDirectory() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    public static void copyStream(InputStream is, OutputStream os)
            throws IOException {
        byte[] buffer = new byte[4 * 1024];
        int count = 0;
        while ((count = is.read(buffer)) > 0) {
            os.write(buffer, 0, count);
        }
    }

    // The working directory is /sdcard/.ApkPermRemover/tmp/
    public static String makeWorkingDir(Context ctx) throws Exception {
        return makeDir(ctx, "tmp");
    }

    public static String makeBackupDir(Context ctx) throws Exception {
        return makeDir(ctx, "backup");
    }

    public static String makeImageDir(Context ctx) throws Exception {
        return makeDir(ctx, "image");
    }

    public static String makeDir(Context ctx, String dirName) throws Exception {
        if (!SDCard.exist()) {
            throw new Exception("Can not find sd card.");
        }

        String subDir = "";
        String packagePath = ctx.getPackageName();
        if (packagePath.startsWith("com.gmail.heagoo.apkpermremover")) {
            subDir = "/.ApkPermRemover/" + dirName + "/";
        } else if (packagePath.startsWith("com.gmail.heagoo.pmaster")) {
            subDir = "/PermMaster/" + dirName + "/";
        } else if (packagePath.equals("com.gmail.heagoo.permissionmanager")) {
            subDir = "/PermMaster/" + dirName + "/";
        } else if (packagePath.startsWith("com.gmail.heagoo.apkeditor")) {
            subDir = "/ApkEditor/" + dirName + "/";
        } else if (packagePath.startsWith("com.gmail.heagoo.appdm")) {
            subDir = "/HackAppData/" + dirName + "/";
        }

        String rootDir = SDCard.getRootDirectory();
        String targetDir = rootDir + subDir;
        File f = new File(targetDir);
        if (!f.exists()) {
            f.mkdirs();
        }

        return targetDir;
    }

    @TargetApi(19)
    public static String getExternalStoragePath(Context ctx) {
        if (Build.VERSION.SDK_INT >= 19) {
            String internalPath = Environment.getExternalStorageDirectory().getPath();
            File[] files = ctx.getExternalFilesDirs(null);
            if (files != null) {
                // Find the pattern
                int appendedLen = 0;
                for (File f : files) {
                    String path = f.getPath();
                    if (path.startsWith(internalPath)) {
                        appendedLen = path.length() - internalPath.length();
                        break;
                    }
                }
                for (File f : files) {
                    String path = f.getPath();
                    if (!path.startsWith(internalPath)) {
                        return path.substring(0, path.length() - appendedLen);
                    }
                }
            }
        }

        String path = null;
        List<StorageInfo> storageList = StorageUtils.getStorageList();
        if (storageList != null) {
            for (StorageInfo si : storageList) {
                if (!si.internal && !si.readonly) {
                    path = si.path;
                    break;
                }
            }
        }
        return path;
        // String externalSd = System.getenv("SECONDARY_STORAGE");
        //
        // if (externalSd == null || externalSd.equals("")) {
        // String extsdcardName = null;
        // File storageDir = new File("/mnt/");
        // if (storageDir.isDirectory()) {
        // String[] nameList = storageDir.list();
        // for (String name : nameList) {
        // if (name.equalsIgnoreCase("extsdcard")
        // || (name.startsWith("external") && name
        // .contains("sd"))) {
        // extsdcardName = name;
        // break;
        // }
        // }
        // }
        // if (extsdcardName != null) {
        // externalSd = "/mnt/" + extsdcardName;
        // }
        // }
        // return externalSd;
    }
}
