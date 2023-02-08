package com.gmail.heagoo.appdm.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

// Check whether bought any paid apps
public class PaidAppsChecker {

    private static String[] paidApps = {"com.gmail.heagoo.pmaster.pro",
            "com.gmail.heagoo.apkpermremover.pro",
            "com.gmail.heagoo.apkeditor.pro", "com.gmail.heagoo.autorun.pro"};

    public static boolean isPaidAppsExist(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        for (int i = 0; i < paidApps.length; i++) {
            if (isPackageExist(pm, paidApps[i])) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPackageExist(PackageManager pm, String pkgName) {
        try {
            pm.getApplicationInfo(pkgName, 0);
            return true;
        } catch (NameNotFoundException e) {
        }
        return false;
    }
}
