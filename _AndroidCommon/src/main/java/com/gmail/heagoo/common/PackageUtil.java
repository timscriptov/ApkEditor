package com.gmail.heagoo.common;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class PackageUtil {

    public static void uninstallPackage(Context ctx, String packageName) {
        Uri packageURI = Uri.parse("package:" + packageName);
        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE,
                packageURI);
        ctx.startActivity(uninstallIntent);
    }
}
