package com.gmail.heagoo.appdm.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;

public class VersionChecker {

    public static boolean isProVersion(Context ctx) {
        ApplicationInfo ai = ctx.getApplicationInfo();
        return ai.packageName.endsWith(".pro");
    }

    public static boolean isFreeVersion(Context ctx) {
        return !isProVersion(ctx);
    }

//	@SuppressLint("NewApi")
//	public static void showGetProDialog(final Context ctx, String msg) {
//		AlertDialog.Builder builder = null;
//		if (Build.VERSION.SDK_INT >= 11) {
//			builder = new AlertDialog.Builder(ctx, AlertDialog.THEME_HOLO_LIGHT);
//		} else {
//			builder = new AlertDialog.Builder(ctx);
//		}
//		builder.setTitle(R.string.not_available)
//				.setMessage(R.string.to_buy_tip)
//				.setPositiveButton(R.string.view_pro_version,
//						new DialogInterface.OnClickListener() {
//							public void onClick(
//									DialogInterface dialoginterface, int i) {
//								viewProVersion(ctx);
//							}
//						}).setNegativeButton(android.R.string.cancel, null)
//				.show();
//	}

    // To view/download the pro version
    public static void viewProVersion(Context ctx) {
        String pkgName = ctx.getPackageName() + ".pro";
        Uri uri = Uri.parse("market://details?id=" + pkgName);
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            ctx.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                    .parse("http://play.google.com/store/apps/details?id="
                            + pkgName)));
        }
    }
}
