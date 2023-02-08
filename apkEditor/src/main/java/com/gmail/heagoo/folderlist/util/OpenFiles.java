package com.gmail.heagoo.folderlist.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.gmail.heagoo.apkeditor.base.R;

import java.io.File;

public class OpenFiles {
    private static Intent getHtmlFileIntent(Uri uri) {
//		uri = uri.buildUpon()
//				.encodedAuthority("com.android.htmlfileprovider")
//				.scheme("content").encodedPath(file.toString()).build();
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(uri, "text/html");
        return intent;
    }

    private static Intent getImageFileIntent(Uri uri) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(uri, "image/*");
        return intent;
    }

    private static Intent getPdfFileIntent(Uri uri) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(uri, "application/pdf");
        return intent;
    }

    private static Intent getTextFileIntent(Uri uri) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(uri, "text/plain");
        return intent;
    }

    private static Intent getAudioFileIntent(Uri uri) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.putExtra("oneshot", 0);
        intent.putExtra("configchange", 0);
        intent.setDataAndType(uri, "audio/*");
        return intent;
    }

    private static Intent getVideoFileIntent(Uri uri) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.putExtra("oneshot", 0);
        intent.putExtra("configchange", 0);
        intent.setDataAndType(uri, "video/*");
        return intent;
    }

//	private static Intent getChmFileIntent(Uri uri) {
//		Intent intent = new Intent("android.intent.action.VIEW");
//		intent.addCategory("android.intent.category.DEFAULT");
//		intent.setDataAndType(uri, "application/x-chm");
//		return intent;
//	}

    private static Intent getWordFileIntent(Uri uri) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(uri, "application/msword");
        return intent;
    }

    private static Intent getExcelFileIntent(Uri uri) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(uri, "application/vnd.ms-excel");
        return intent;
    }

    private static Intent getPPTFileIntent(Uri uri) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(uri, "application/vnd.ms-powerpoint");
        return intent;
    }

    private static Intent getApkFileIntent(Uri uri) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        return intent;
    }

    private static boolean checkEndsWithInStringArray(String checkItsEnd,
                                                      String[] fileTypes) {
        for (String aEnd : fileTypes) {
            if (checkItsEnd.endsWith(aEnd))
                return true;
        }
        return false;
    }

    public static Intent getIntent(Context ctx, String filePath) {
        Intent intent;
        Uri uri = null;
        try {
            String authorityName = ctx.getPackageName() + ".fileprovider";
            uri = FileProvider.getUriForFile(ctx, authorityName, new File(filePath));
        } catch (Throwable ignored) {
        }

        if (uri == null) {
            return null;
        }

        if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypeImage))) {
            intent = OpenFiles.getImageFileIntent(uri);
        } else if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypeWebText))) {
            intent = OpenFiles.getHtmlFileIntent(uri);
        } else if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypePackage))) {
            intent = OpenFiles.getApkFileIntent(uri);
        } else if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypeAudio))) {
            intent = OpenFiles.getAudioFileIntent(uri);
        } else if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypeVideo))) {
            intent = OpenFiles.getVideoFileIntent(uri);
        } else if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypeText))) {
            intent = OpenFiles.getTextFileIntent(uri);
        } else if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypePdf))) {
            intent = OpenFiles.getPdfFileIntent(uri);
        } else if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypeWord))) {
            intent = OpenFiles.getWordFileIntent(uri);
        } else if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypeExcel))) {
            intent = OpenFiles.getExcelFileIntent(uri);
        } else if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypePPT))) {
            intent = OpenFiles.getPPTFileIntent(uri);
        } else {
            intent = new Intent(Intent.ACTION_VIEW);
            // Don't set new task flag, as it will trigger onActivityResult immediately
            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(uri, "*/*");
        }

        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }

    public static void openFile(Context ctx, String filePath) {
        Intent intent = getIntent(ctx, filePath);
        if (intent != null) {
            ctx.startActivity(intent);
        }
    }

    public static void openFile(Activity activity, String filePath, int requestCode) {
        Intent intent = getIntent(activity, filePath);
        if (intent != null) {
            activity.startActivityForResult(intent, requestCode);
        }
    }
}