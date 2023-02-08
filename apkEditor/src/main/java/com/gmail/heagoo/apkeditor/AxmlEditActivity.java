package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.se.ApkCreateActivity;
import com.gmail.heagoo.apkeditor.se.IDirChanged;
import com.gmail.heagoo.apkeditor.se.ZipFileListAdapter;
import com.gmail.heagoo.apkeditor.se.ZipHelper;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.ApkInfoParser;
import com.gmail.heagoo.common.CommandRunner;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.common.RandomUtil;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Map;

public class AxmlEditActivity extends CustomizedLangActivity implements IDirChanged, View.OnClickListener {
    private String apkPath;
    private ApkInfoParser.AppInfo apkInfo;

    private int themeId;
    private ZipFileListAdapter filesAdapter;
    // Thread & handler
    private AxmlEditActivity.MyHandler handler;
    private AxmlEditActivity.MyThread thread;
    // Zip file list view
    private ListView fileListView;
    // Save/Close Button
    private Button saveBtn;
    // Summary text (to show tip)
    private TextView summaryTv;
    // To parse all the information inside the APK
    private ZipHelper zipHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        // Dark theme or not
        // sawsem theme
//        this.themeId = GlobalConfig.instance(this).getThemeId();
//        switch (themeId) {
//            case GlobalConfig.THEME_DARK_DEFAULT:
//                super.setTheme(android.R.style.Theme_Black_NoTitleBar);
//                setContentView(R.layout.activity_axmledit_dark);
//                break;
//            case GlobalConfig.THEME_DARK_RUSSIAN:
//                super.setTheme(android.R.style.Theme_Black_NoTitleBar);
//                setContentView(R.layout.activity_axmledit_dark);
//                break;
//            default:
        setContentView(R.layout.activity_axmledit);
//                break;
//        }

        // Full screen or not
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        this.apkPath = ActivityUtil.getParam(getIntent(), "apkPath");

        try {
            this.apkInfo = new ApkInfoParser().parse(this, apkPath);
        } catch (Exception e) {
            String msg = getResources().getString(R.string.cannot_parse_apk);
            msg += ": " + e.getMessage();
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }

        if (apkInfo != null) {
            // Start thread to parse the apk file
            this.handler = new AxmlEditActivity.MyHandler(this);
            this.thread = new AxmlEditActivity.MyThread(this);
            thread.start();

            initViews();
        } else {
            this.finish();
        }
    }

    @Override
    public void onDestroy() {
        if (filesAdapter != null) {
            filesAdapter.destroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            // XML file is modified
            if (resultCode == 1) {
                String filePath = data.getStringExtra("xmlPath");
                String entryName = data.getStringExtra("extraString");
                new ProcessingDialog(this, new XmlCompiler(filePath, entryName), -1).show();
            }
        }
    }

    private String getApkPath() {
        String packageName = (BuildConfig.IS_PRO ? "com.gmail.heagoo.apkeditor.pro" : "com.gmail.heagoo.apkeditor");
        PackageManager pm = this.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            String apk = ai.publicSourceDir;
            return apk;
        } catch (Throwable x) {
        }
        return null;
    }

    private String getBinaryPath() {
        // Play tricks to extract files: borrow ApkComposeThread to extract files
        ApkComposeThread tmp = new ApkComposeThread(this, null, null, null);
        tmp.prepare();

        File fileDir = this.getFilesDir();
        String rootDirectory = fileDir.getAbsolutePath();

        return rootDirectory + "/bin/";
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save APK path
        {
            outState.putString("apkPath", this.apkPath);
        }

        super.onSaveInstanceState(outState);
    }

    public void dataReady(boolean succeed) {
        // Make progress bar gone
        this.findViewById(R.id.progress_bar).setVisibility(View.GONE);

        if (succeed) {
            fileListView.setVisibility(View.VISIBLE);
            initCenterView();
        } else {
            Toast.makeText(this, thread.getErrorMessage(), Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

    private void initCenterView() {
        // Files
        this.filesAdapter = new ZipFileListAdapter(this, this, zipHelper, true);
        fileListView.setAdapter(filesAdapter);
        fileListView.setOnItemClickListener(filesAdapter);
        fileListView.setOnItemLongClickListener(filesAdapter);
    }

    private void initViews() {
        // Get View
        this.fileListView = (ListView) findViewById(R.id.files_list);
        this.summaryTv = (TextView) this.findViewById(R.id.tv_summary);
        this.saveBtn = (Button) this.findViewById(R.id.btn_save);
        Button closeBtn = (Button) this.findViewById(R.id.btn_close);

        // Set the list view invisible
        fileListView.setVisibility(View.INVISIBLE);

        this.saveBtn.setOnClickListener(this);
        closeBtn.setOnClickListener(this);

        // Basic info
        if (apkInfo != null) {
            ImageView apkIcon = (ImageView) this.findViewById(R.id.apk_icon);
            apkIcon.setImageDrawable(apkInfo.icon);

            TextView labelTV = (TextView) this.findViewById(R.id.apk_label);
            labelTV.setText(apkInfo.label);
        }
    }

    // To view/download the pro version
    protected void viewProVersion() {
        String pkgName = this.getPackageName() + ".pro";
        Uri uri = Uri.parse("market://details?id=" + pkgName);
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            this.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                    .parse("http://play.google.com/store/apps/details?id="
                            + pkgName)));
        }
    }

    private void initData() throws Exception {
        this.zipHelper = new ZipHelper(this.apkPath);
        this.zipHelper.parse();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            this.finish();
        } else if (id == R.id.btn_save) {
            makeAPK();
            this.finish();
        }
    }

    // To make the new modified APK
    private void makeAPK() {
        Map<String, String> fileReplaces = filesAdapter.getReplaces();

        Intent intent = new Intent(this, ApkCreateActivity.class);
        ActivityUtil.attachParam(intent, "apkPath", this.apkPath);
        ActivityUtil.attachParam(intent, "packageName", apkInfo.packageName);
        if (!fileReplaces.isEmpty()) {
            ActivityUtil.attachParam(intent, "otherReplaces", fileReplaces);
        }

        startActivity(intent);
    }

    @Override
    public void dirChanged(String dir) {
        this.summaryTv.setText(dir);
    }

    private static class MyHandler extends Handler {
        WeakReference<AxmlEditActivity> activityRef;

        public MyHandler(AxmlEditActivity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            AxmlEditActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }
            switch (msg.what) {
                case 0:
                    activity.dataReady(true);
                    break;
                case 1:
                    activity.dataReady(false);
                    break;
            }
        }
    }

    private static class MyThread extends Thread {
        String err;
        WeakReference<AxmlEditActivity> activityRef;

        public MyThread(AxmlEditActivity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            AxmlEditActivity activity = activityRef.get();
            if (activity != null) {
                try {
                    activity.initData();
                    activity.handler.sendEmptyMessage(0);
                } catch (Exception e) {
                    e.printStackTrace();
                    err = e.getMessage();
                    activity.handler.sendEmptyMessage(1);
                }
            }
        }

        String getErrorMessage() {
            return err;
        }
    }

    // Compile xml into AXML
    private class XmlCompiler implements ProcessingDialog.ProcessingInterface {
        private String xmlPath;
        private String axmlPath;
        private String tempPath;
        private String entryName;
        private String outMessage;
        private String errMessage;
        private boolean succeed = false;

        public XmlCompiler(String filePath, String entryName) {
            this.xmlPath = filePath;
            this.axmlPath = filePath + ".bin";
            this.entryName = entryName;
            this.tempPath = filePath + RandomUtil.getRandomString(6);
        }

        @Override
        public void process() throws Exception {
            String binaryPath = getBinaryPath();
            String aaptPath = binaryPath + "aapt";
            String androidPath = binaryPath + "android.jar";
            CommandRunner cr = new CommandRunner();
            cr.runCommand(aaptPath + " z -I " + androidPath + " " + xmlPath + " " + tempPath + " " + getApkPath(), null, 5000, false);
            this.outMessage = cr.getStdOut();
            this.errMessage = cr.getStdError();

            // Rename the generated axml
            File generated = new File(tempPath);
            if (generated.exists()) {
                succeed = true;
                // Rename to target file
                File targetFile = new File(axmlPath);
                boolean ret = generated.renameTo(targetFile);
                if (!ret) {
                    generated.delete();
                }
            }
        }

        @Override
        public void afterProcess() {
            if (succeed && new File(axmlPath).exists()) {
                String str = String.format(getString(R.string.entry_modified), entryName);
                Toast.makeText(AxmlEditActivity.this, str, Toast.LENGTH_SHORT).show();

                filesAdapter.addReplace(entryName, axmlPath);
                saveBtn.setVisibility(View.VISIBLE);
            } else {
                String message = outMessage;
                if (errMessage != null && !errMessage.equals("")) {
                    message = errMessage;
                }
                new AlertDialog.Builder(AxmlEditActivity.this)
                        .setTitle(R.string.error)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        }
    }
}
