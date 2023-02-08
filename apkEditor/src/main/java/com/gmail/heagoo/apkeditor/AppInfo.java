package com.gmail.heagoo.apkeditor;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.io.File;
import java.util.Date;

public class AppInfo {

    // The original application info from package manager
    public ApplicationInfo applicationInfo;

    public String packagePath;
    public String appName;
    public long size;
    public Date installTime;
    public Drawable icon;
    public String apkPath;
    public long lastUpdateTime;
    public String otherInfo;

    public boolean isSysApp;
    public boolean autoRunEnabled;

    public static AppInfo create(PackageManager pm,
                                 ApplicationInfo applicationInfo) {
        AppInfo appInfo = new AppInfo();
        appInfo.applicationInfo = applicationInfo;
        appInfo.appName = (String) applicationInfo.loadLabel(pm);
        appInfo.packagePath = applicationInfo.packageName;
        // appInfo.isSysApp = ((applicationInfo.flags &
        // ApplicationInfo.FLAG_SYSTEM) != 0);
        //appInfo.icon = appInfo.applicationInfo.loadIcon(pm);

        try {
            PackageInfo packageInfo = pm.getPackageInfo(
                    applicationInfo.packageName, 0);
            appInfo.lastUpdateTime = packageInfo.lastUpdateTime;

            // Get apk size
            File f = new File(applicationInfo.sourceDir);
            appInfo.size = f.length();
        } catch (Throwable e) {
            appInfo.lastUpdateTime = 0;
        }

        return appInfo;
    }
}
