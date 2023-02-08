package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.ActivityUtil;

import java.io.File;
import java.util.ArrayList;

public class TextEditor {
    private static boolean isBigFile(Context ctx, String filepath) {
        boolean isBigFile = false;
        File f = new File(filepath);
        if (f.exists() && f.length() > SettingEditorActivity.getBigFileThreshold(ctx) * 1024) {
            isBigFile = true;
        }
        if (isBigFile) {
            Toast.makeText(ctx, R.string.use_bfe_tip, Toast.LENGTH_SHORT).show();
        }
        return isBigFile;
    }


    public static Intent getEditorIntent(Context ctx, String filepath, String apkPath) {
        Intent intent;
        if (isBigFile(ctx, filepath)) {
            intent = new Intent(ctx, TextEditBigActivity.class);
        } else {
            intent = new Intent(ctx, TextEditNormalActivity.class);
        }
        ActivityUtil.attachParam(intent, "xmlPath", filepath);
        if (apkPath != null) {
            ActivityUtil.attachParam(intent, "apkPath", apkPath);
        }
        return intent;
    }

    public static Intent getEditorIntent(Context ctx, ArrayList<String> filePathList, int index, String apkPath) {
        Intent intent;
        if (isBigFile(ctx, filePathList.get(index))) {
            intent = new Intent(ctx, TextEditBigActivity.class);
        } else {
            intent = new Intent(ctx, TextEditNormalActivity.class);
        }
        ActivityUtil.attachParam(intent, "fileList", filePathList);
        ActivityUtil.attachParam(intent, "curFileIndex", index);
        if (apkPath != null) {
            ActivityUtil.attachParam(intent, "apkPath", apkPath);
        }
        return intent;
    }
}
