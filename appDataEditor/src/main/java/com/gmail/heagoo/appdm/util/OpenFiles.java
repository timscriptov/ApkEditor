package com.gmail.heagoo.appdm.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.gmail.heagoo.appdm.base.R;

import java.io.File;

public class OpenFiles {
    public static Intent getHtmlFileIntent(File file) {
        Uri uri = Uri.parse(file.toString()).buildUpon()
                .encodedAuthority("com.android.htmlfileprovider")
                .scheme("content").encodedPath(file.toString()).build();
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(uri, "text/html");
        return intent;
    }

    public static Intent getImageFileIntent(File file) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        Uri uri = Uri.fromFile(file);
        intent.setDataAndType(uri, "image/*");
        return intent;
    }

    public static Intent getPdfFileIntent(File file) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        Uri uri = Uri.fromFile(file);
        intent.setDataAndType(uri, "application/pdf");
        return intent;
    }

    public static Intent getTextFileIntent(File file) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        Uri uri = Uri.fromFile(file);
        intent.setDataAndType(uri, "text/plain");
        return intent;
    }

    public static Intent getAudioFileIntent(File file) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.putExtra("oneshot", 0);
        intent.putExtra("configchange", 0);
        Uri uri = Uri.fromFile(file);
        intent.setDataAndType(uri, "audio/*");
        return intent;
    }

    public static Intent getVideoFileIntent(File file) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.putExtra("oneshot", 0);
        intent.putExtra("configchange", 0);
        Uri uri = Uri.fromFile(file);
        intent.setDataAndType(uri, "video/*");
        return intent;
    }

    public static Intent getChmFileIntent(File file) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        Uri uri = Uri.fromFile(file);
        intent.setDataAndType(uri, "application/x-chm");
        return intent;
    }

    public static Intent getWordFileIntent(File file) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        Uri uri = Uri.fromFile(file);
        intent.setDataAndType(uri, "application/msword");
        return intent;
    }

    public static Intent getExcelFileIntent(File file) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        Uri uri = Uri.fromFile(file);
        intent.setDataAndType(uri, "application/vnd.ms-excel");
        return intent;
    }

    public static Intent getPPTFileIntent(File file) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        Uri uri = Uri.fromFile(file);
        intent.setDataAndType(uri, "application/vnd.ms-powerpoint");
        return intent;
    }

    public static Intent getApkFileIntent(File file) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file),
                "application/vnd.android.package-archive");
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

    private static Intent getIntent(Context ctx, String filePath) {
        Intent intent;

        if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypeImage))) {
            intent = OpenFiles.getImageFileIntent(new File(filePath));
        } else if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypeWebText))) {
            intent = OpenFiles.getHtmlFileIntent(new File(filePath));
        } else if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypePackage))) {
            intent = OpenFiles.getApkFileIntent(new File(filePath));
        } else if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypeAudio))) {
            intent = OpenFiles.getAudioFileIntent(new File(filePath));
        } else if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypeVideo))) {
            intent = OpenFiles.getVideoFileIntent(new File(filePath));
        } else if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypeText))) {
            intent = OpenFiles.getTextFileIntent(new File(filePath));
        } else if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypePdf))) {
            intent = OpenFiles.getPdfFileIntent(new File(filePath));
        } else if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypeWord))) {
            intent = OpenFiles.getWordFileIntent(new File(filePath));
        } else if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypeExcel))) {
            intent = OpenFiles.getExcelFileIntent(new File(filePath));
        } else if (checkEndsWithInStringArray(filePath, ctx.getResources()
                .getStringArray(R.array.fileTypePPT))) {
            intent = OpenFiles.getPPTFileIntent(new File(filePath));
        } else {
            intent = new Intent(Intent.ACTION_VIEW);
            // Don't set new task flag, as it will trigger onActivityResult immediately
            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri uri = Uri.fromFile(new File(filePath));
            intent.setDataAndType(uri, "*/*");
        }

        return intent;
    }

    public static void openFile(Context ctx, String filePath) {
        Intent intent = getIntent(ctx, filePath);
        ctx.startActivity(intent);
    }

    public static void openFile(Activity activity, String filePath, int requestCode) {
        Intent intent = getIntent(activity, filePath);
        activity.startActivityForResult(intent, requestCode);
    }
}