package com.gmail.heagoo.applistutil;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.Date;

public class AppInfo {

    // The original application info from package manager
    public ApplicationInfo applicationInfo;

    public String packagePath;
    public String appName;
    public String sharedUserId;
    public int size;
    public Date installTime;
    public Drawable icon;
    public String apkPath;
    public long lastUpdateTime;
    public String otherInfo;

    public boolean isSysApp;
    public boolean autoRunEnabled;
    public boolean disabled; // App disabled or not

    public AppInfo() {

    }

    public AppInfo(AppInfo info) {
        this.applicationInfo = info.applicationInfo;

        this.packagePath = info.packagePath;
        this.appName = info.appName;
        this.sharedUserId = info.sharedUserId;
        this.size = info.size;
        this.installTime = info.installTime;
        this.icon = info.icon;
        this.apkPath = info.apkPath;
        this.lastUpdateTime = info.lastUpdateTime;
        this.otherInfo = info.otherInfo;

        this.isSysApp = info.isSysApp;
        this.autoRunEnabled = info.autoRunEnabled;
        this.disabled = info.disabled;
    }

    public static AppInfo create(PackageManager pm, ApplicationInfo applicationInfo) {
        AppInfo appInfo = new AppInfo();
        appInfo.applicationInfo = applicationInfo;
        appInfo.appName = (String) applicationInfo.loadLabel(pm);
        appInfo.packagePath = applicationInfo.packageName;
        appInfo.isSysApp = ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);


        try {
            PackageInfo packageInfo = pm.getPackageInfo(applicationInfo.packageName, 0);
            appInfo.sharedUserId = packageInfo.sharedUserId;
            appInfo.lastUpdateTime = packageInfo.lastUpdateTime;
        } catch (Throwable e) {
            appInfo.lastUpdateTime = 0;
        }

        // Disabled or not
        appInfo.disabled = !applicationInfo.enabled;

        return appInfo;
    }
}
