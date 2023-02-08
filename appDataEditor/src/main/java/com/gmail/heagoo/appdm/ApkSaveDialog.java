package com.gmail.heagoo.appdm;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.gmail.heagoo.appdm.base.R;
import com.gmail.heagoo.appdm.util.SDCard;
import com.gmail.heagoo.common.IOUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class ApkSaveDialog extends Dialog {

    private View view;
    private WeakReference<Activity> activityRef;
    private String apkPath;
    private String appName;
    private String dstPath;

    @SuppressLint("InflateParams")
    public ApkSaveDialog(Activity activity, String apkPath, String appName) {
        super(activity);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCancelable(false);

        this.activityRef = new WeakReference<Activity>(activity);
        this.apkPath = apkPath;
        this.appName = appName;

        this.view = LayoutInflater.from(activity).inflate(
                R.layout.appdm_dlg_saveapk, null);
        setContentView(view);
    }

    public void start() {
        this.show();

        if (!SDCard.exist()) {
            showToast("Cannot find SD card to save the APK.");
            return;
        }

        String dstDir = SDCard.getBackupDir(activityRef.get());
        this.dstPath = dstDir + "/" + appName + ".apk";

        startCopyThread(apkPath, dstPath);
    }

    private void startCopyThread(final String srcPath, final String dstPath) {
        new Thread() {
            @Override
            public void run() {
                try {
                    FileInputStream in = new FileInputStream(srcPath);
                    FileOutputStream out = new FileOutputStream(dstPath);
                    IOUtils.copy(in, out);
                    onSucceed();
                } catch (IOException e) {
                    onFailed(e.getMessage());
                }
            }
        }.start();
    }

    protected void onSucceed() {
        String str = activityRef.get().getResources()
                .getString(R.string.apk_saved_tip);
        showToastOnUiThread(String.format(str, dstPath));
        cancelDialog();
    }

    protected void onFailed(String msg) {
        showToastOnUiThread("Failed: " + msg);
        cancelDialog();
    }

    private void cancelDialog() {
        final Activity activity = activityRef.get();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ApkSaveDialog.this.cancel();
                }
            });
        }
    }

    private void showToast(String msg) {
        Activity activity = activityRef.get();
        if (activity != null) {
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void showToastOnUiThread(final String msg) {
        final Activity activity = activityRef.get();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

}
