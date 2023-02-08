package com.gmail.heagoo.appdm.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;

import com.gmail.heagoo.common.CommandRunner;

import java.io.File;

public class SDCard {

    private static String appDir = null;

    // SD card exist or not
    public static boolean exist() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    // Root directory now returns the internal app dir
    // private static String getDataDirectory(Context ctx) {
    // return ctx.getFilesDir().getAbsolutePath();
    // }

    // Backup directory is in SD card
    public static String getBackupDir(Context ctx) {
        initFolderName(ctx);

        File f = Environment.getExternalStorageDirectory();
        String path = f.getPath() + "/" + appDir + "/backups";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return path;
    }

    // Temp dir is in the internal storage
    public static String getTempDir(Context ctx) {
        initFolderName(ctx);

        File f = Environment.getExternalStorageDirectory();
        String path = f.getPath() + "/" + appDir + "/temp";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return path;
    }

    private static void initFolderName(Context ctx) {
        if (appDir != null) {
            return;
        }

        try {
            ApplicationInfo appInfo = ctx.getPackageManager()
                    .getApplicationInfo(ctx.getPackageName(),
                            PackageManager.GET_META_DATA);
            appDir = appInfo.metaData.getString("heagoo.sdcard_folder");
        } catch (NameNotFoundException e) {
        }
    }

    public static void removeTempDir(Context ctx) {
        initFolderName(ctx);

        File f = Environment.getExternalStorageDirectory();
        String path = f.getPath() + "/" + appDir + "/temp";

        File dir = new File(path);
        if (dir.exists()) {
            CommandRunner cr = new CommandRunner();
            cr.runCommand("rm -rf " + path, null, 10 * 1000);
        }
    }

    // private static void removeDirectory(Context ctx, String subDir) {
    // String dataDir = getDataDirectory(ctx);
    // if (!dataDir.endsWith("/")) {
    // dataDir += "/";
    // }
    // String dirPath = dataDir + subDir;
    // File dir = new File(dirPath);
    // if (dir.exists()) {
    // CommandRunner cr = new CommandRunner();
    // cr.runCommand("rm -rf " + dirPath, null, 10 * 1000);
    // }
    // }

    // private static String getDirectory(Context ctx, String subDir) {
    // String dataDir = getDataDirectory(ctx);
    // if (!dataDir.endsWith("/")) {
    // dataDir += "/";
    // }
    //
    // String dirPath = dataDir + subDir;
    // File dir = new File(dirPath);
    // if (!dir.exists()) {
    // dir.mkdirs();
    // }
    //
    // return dirPath;
    // }

    @SuppressLint("DefaultLocale")
    public static String getSizeDescription(long fileSize) {
        if (fileSize >= 1024 * 1024) {
            float mb = 1.0f * fileSize / 1024 / 1024;
            return String.format("%.2f M", mb);
        } else if (fileSize >= 1024) {
            float kb = 1.0f * fileSize / 1024;
            return String.format("%.2f K", kb);
        }

        return "" + fileSize + " B";
    }
}
