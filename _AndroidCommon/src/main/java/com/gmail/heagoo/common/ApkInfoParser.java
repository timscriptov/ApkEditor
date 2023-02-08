package com.gmail.heagoo.common;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

public class ApkInfoParser {

//	public static final String PackageParserPath = "android.content.pm.PackageParser";
//	public static final String AssetManagerPath = "android.content.res.AssetManager";
//
//	private Class<?> pkgParserCls = null;
//	private Constructor<?> pkgParserCt = null;
//	private Object pkgParser;

    public ApkInfoParser() {
//		try {
//			this.pkgParserCls = Class.forName(PackageParserPath);
//
//			if (Build.VERSION.SDK_INT > 20) {
//				this.pkgParserCt = pkgParserCls.getConstructor((Class[]) null);
//				this.pkgParser = pkgParserCt.newInstance((Object[]) null);
//			} else {
//				Class<?>[] typeArgs = new Class[1];
//				typeArgs[0] = String.class;
//				this.pkgParserCt = pkgParserCls.getConstructor(typeArgs);
//			}
//		} catch (Exception ignored) {
//		}
    }

    public AppInfo parse(Context ctx, String apkPath) throws Exception {
        AppInfo apkInfo = null;

        PackageManager packageManager = ctx.getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageArchiveInfo(apkPath, 0);
        if (packageInfo != null) {
            apkInfo = new AppInfo();
            packageInfo.applicationInfo.sourceDir = apkPath;
            packageInfo.applicationInfo.publicSourceDir = apkPath;
            apkInfo.label = packageInfo.applicationInfo.loadLabel(packageManager).toString();
            apkInfo.packageName = packageInfo.packageName;
            apkInfo.icon = packageInfo.applicationInfo.loadIcon(packageManager);
        }

        return apkInfo;
    }

    public static class AppInfo {
        public String label;
        public String packageName;
        public Drawable icon;
        public boolean isSysApp; // Sometimes available
    }

//	public AppInfo parse(Context ctx, String apkPath) throws Exception {
//		// >= Android 5.0
//		if (Build.VERSION.SDK_INT > 20) {
//			return parse_new(ctx, apkPath);
//		} else {
//			return parse_old(ctx, apkPath);
//		}
//	}
//
//	public AppInfo parse_old(Context ctx, String apkPath) throws Exception {
//		if (this.pkgParserCls == null || pkgParserCt == null) {
//			return null;
//		}
//
//		Object[] valueArgs = new Object[1];
//		valueArgs[0] = apkPath;
//		Object pkgParser = pkgParserCt.newInstance(valueArgs);
//
//		// Display metrics
//		DisplayMetrics metrics = new DisplayMetrics();
//		metrics.setToDefaults();
//
//		// PackageParser.Package mPkgInfo = packageParser.parsePackage(new
//		// File(apkPath), apkPath, metrics, 0);
//		Class<?>[] typeArgs = new Class[4];
//		typeArgs[0] = File.class;
//		typeArgs[1] = String.class;
//		typeArgs[2] = DisplayMetrics.class;
//		typeArgs[3] = Integer.TYPE;
//
//		Method pkgParser_parsePackageMtd = pkgParserCls.getDeclaredMethod(
//				"parsePackage", typeArgs);
//		valueArgs = new Object[4];
//		valueArgs[0] = new File(apkPath);
//		valueArgs[1] = apkPath;
//		valueArgs[2] = metrics;
//		valueArgs[3] = 0;
//
//		Object pkgParserPkg = pkgParser_parsePackageMtd.invoke(pkgParser,
//				valueArgs);
//
//		// ApplicationInfo info = mPkgInfo.applicationInfo;
//		Field appInfoFld = pkgParserPkg.getClass().getDeclaredField(
//				"applicationInfo");
//
//		ApplicationInfo info = (ApplicationInfo) appInfoFld.get(pkgParserPkg);
//
//		Class<?> assetMagCls = Class.forName(AssetManagerPath);
//
//		Constructor<?> assetMagCt = assetMagCls.getConstructor((Class[]) null);
//
//		Object assetMag = assetMagCt.newInstance((Object[]) null);
//		typeArgs = new Class[1];
//		typeArgs[0] = String.class;
//		Method assetMag_addAssetPathMtd = assetMagCls.getDeclaredMethod(
//				"addAssetPath", typeArgs);
//		valueArgs = new Object[1];
//		valueArgs[0] = apkPath;
//		assetMag_addAssetPathMtd.invoke(assetMag, valueArgs);
//		Resources res = ctx.getResources();
//
//		typeArgs = new Class[3];
//		typeArgs[0] = assetMag.getClass();
//		typeArgs[1] = res.getDisplayMetrics().getClass();
//		typeArgs[2] = res.getConfiguration().getClass();
//
//		Constructor<?> resCt = Resources.class.getConstructor(typeArgs);
//
//		valueArgs = new Object[3];
//		valueArgs[0] = assetMag;
//		valueArgs[1] = res.getDisplayMetrics();
//		valueArgs[2] = res.getConfiguration();
//
//		res = (Resources) resCt.newInstance(valueArgs);
//
//		CharSequence label = null;
//		if (info.labelRes != 0) {
//			label = res.getText(info.labelRes);
//		}
//		if (label == null) {
//			label = info.nonLocalizedLabel;
//		}
//
//		// Icon
//		Drawable icon = null;
//		if (info.icon != 0) {
//			icon = res.getDrawable(info.icon);
//		}
//
//		AppInfo appInfoData = new AppInfo();
//		appInfoData.label = (label != null ? label.toString() : "");
//		appInfoData.packageName = info.packageName;
//		appInfoData.icon = icon;
//		return appInfoData;
//	}
//
//	private AppInfo parse_new(Context ctx, String apkPath) throws Exception {
//		if (this.pkgParserCls == null || pkgParserCt == null || pkgParser == null) {
//			return null;
//		}
//
//		// PackageParser.Package mPkgInfo = packageParser.parsePackage(new
//		// File(apkPath), 0);
//		Class<?>[] typeArgs = new Class[2];
//		typeArgs[0] = File.class;
//		typeArgs[1] = Integer.TYPE;
//
//		Method pkgParser_parsePackageMtd = pkgParserCls.getDeclaredMethod(
//				"parsePackage", typeArgs);
//		Object[] valueArgs = new Object[2];
//		valueArgs[0] = new File(apkPath);
//		valueArgs[1] = 0;
//
//		Object pkgParserPkg = pkgParser_parsePackageMtd.invoke(pkgParser,
//				valueArgs);
//
//		// ApplicationInfo info = mPkgInfo.applicationInfo;
//		Field appInfoFld = pkgParserPkg.getClass().getDeclaredField(
//				"applicationInfo");
//
//		ApplicationInfo info = (ApplicationInfo) appInfoFld.get(pkgParserPkg);
//
//		Class<?> assetMagCls = Class.forName(AssetManagerPath);
//
//		Constructor<?> assetMagCt = assetMagCls.getConstructor((Class[]) null);
//
//		Object assetMag = assetMagCt.newInstance((Object[]) null);
//		typeArgs = new Class[1];
//		typeArgs[0] = String.class;
//		Method assetMag_addAssetPathMtd = assetMagCls.getDeclaredMethod(
//				"addAssetPath", typeArgs);
//		valueArgs = new Object[1];
//		valueArgs[0] = apkPath;
//		assetMag_addAssetPathMtd.invoke(assetMag, valueArgs);
//		Resources res = ctx.getResources();
//
//		typeArgs = new Class[3];
//		typeArgs[0] = assetMag.getClass();
//		typeArgs[1] = res.getDisplayMetrics().getClass();
//		typeArgs[2] = res.getConfiguration().getClass();
//
//		Constructor<?> resCt = Resources.class.getConstructor(typeArgs);
//
//		valueArgs = new Object[3];
//		valueArgs[0] = assetMag;
//		valueArgs[1] = res.getDisplayMetrics();
//		valueArgs[2] = res.getConfiguration();
//
//		res = (Resources) resCt.newInstance(valueArgs);
//
//		CharSequence label = null;
//		if (info.labelRes != 0) {
//			label = res.getText(info.labelRes);
//		}
//		if (label == null) {
//			label = info.nonLocalizedLabel;
//		}
//
//		// Icon
//		Drawable icon = null;
//		if (info.icon != 0) {
//			icon = res.getDrawable(info.icon);
//		}
//
//		AppInfo appInfoData = new AppInfo();
//		appInfoData.label = (label != null ? label.toString() : "");
//		appInfoData.packageName = info.packageName;
//		appInfoData.icon = icon;
//		return appInfoData;
//	}
}
