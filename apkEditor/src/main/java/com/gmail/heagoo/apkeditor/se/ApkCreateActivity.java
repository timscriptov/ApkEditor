package com.gmail.heagoo.apkeditor.se;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.ApkInfoActivity;
import com.gmail.heagoo.apkeditor.GlobalConfig;
import com.gmail.heagoo.apkeditor.MainActivity;
import com.gmail.heagoo.apkeditor.SettingActivity;
import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.ce.IApkMaking;
import com.gmail.heagoo.apkeditor.ce.IDescriptionUpdate;
import com.gmail.heagoo.apkeditor.ce.e.ResourceEditor;
import com.gmail.heagoo.apkeditor.dex.DexStringEditor;
import com.gmail.heagoo.apkeditor.util.SignHelper;
import com.gmail.heagoo.apklib.sign.ImageTools;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.ApkInfoParser;
import com.gmail.heagoo.common.ApkInfoParser.AppInfo;
import com.gmail.heagoo.common.CheckUtil;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.common.RefInvoke;
import com.gmail.heagoo.common.SDCard;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ApkCreateActivity extends CustomizedLangActivity implements OnClickListener,
        IDescriptionUpdate {

    MyHandler handler;
    CreatingThread thread;
    private String apkPath;
    private String packageName;
    private Map<String, String> imgReplaces;
    // Image replace & otherReplaces will merge to allReplaces
    private Map<String, String> allReplaces;
    // When to modify the dex file, record class replaces
    private Map<String, String> clsNameReplaces;
    // When newPackageNameInArsc != null, means package name is changed
    // need to revise resources.arsc
    private String newPackageNameInArsc;
    // This means need to change app name in resources.arsc
    private String oldAppNameInArsc;
    private String newAppNameInArsc;
    private LinearLayout waitingLayout;
    private LinearLayout installLayout;
    // Result image and result text view & buttons
    private ImageView resultImg;
    private TextView resultTv;
    private Button removeBtn;
    private Button installBtn;
    private TextView detailTv;
    // Record status
    private String outApkPath;
    private String errorMessage;
    private boolean modifyFinished = false;
    private boolean succeed;

    // APK information
    private AppInfo apkInfo;

    // Directory to save temp files
    private String workingDir;

    // Time when the compose started
    private long startTime;

    // Extra preparing interface
    private ArrayList<IApkMaking> makeInterfaces;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_simpleedit_making);

        // Full screen or not
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        // Activity re-creation
        if (savedInstanceState != null) {
            this.apkPath = savedInstanceState.getString("apkPath");
            this.packageName = savedInstanceState.getString("packageName");
            this.modifyFinished = savedInstanceState.getBoolean("modifyFinished");
            this.succeed = savedInstanceState.getBoolean("succeed");
            this.outApkPath = savedInstanceState.getString("outApkPath");
            this.errorMessage = savedInstanceState.getString("errorMessage");
        } else {
            Intent intent = getIntent();
            this.apkPath = ActivityUtil.getParam(intent, "apkPath");
            this.packageName = ActivityUtil.getParam(intent, "packageName");
            this.imgReplaces = ActivityUtil.getMapParam(intent, "imageReplaces");
            Map<String, String> otherReplaces = ActivityUtil.getMapParam(intent, "otherReplaces");
            // Extra preparing interface
            Bundle bundle = intent.getExtras();
            this.makeInterfaces = (ArrayList<IApkMaking>) bundle.getSerializable("interfaces");
            this.allReplaces = new HashMap<>();
            if (otherReplaces != null) {
                allReplaces.putAll(otherReplaces);
            }
            try {
                this.workingDir = SDCard.makeWorkingDir(this);
            } catch (Exception ignored) {
            }

            this.oldAppNameInArsc = ActivityUtil.getParam(intent, "oldAppNameInArsc");
            this.newAppNameInArsc = ActivityUtil.getParam(intent, "newAppNameInArsc");
            this.newPackageNameInArsc = ActivityUtil.getParam(intent, "newPackageNameInArsc");
            this.clsNameReplaces = ActivityUtil.getMapParam(intent, "classRenames");
        }

        try {
            this.apkInfo = new ApkInfoParser().parse(this, apkPath);
        } catch (Exception e) {
            String msg = getResources().getString(R.string.cannot_parse_apk);
            msg += ": " + e.getMessage();
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }

        this.handler = new MyHandler(this);

        // Already revised the permission (second time to create the activity)
        if (this.modifyFinished) {
            initView(true);
        } else {
            thread = new CreatingThread(this);
            thread.start();

            initView(false);
        }

        // Record the start time
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("apkPath", this.apkPath);
        outState.putString("packageName", this.packageName);
        outState.putString("outApkPath", this.outApkPath);
        outState.putString("errorMessage", this.errorMessage);
        outState.putBoolean("modifyFinished", this.modifyFinished);
        outState.putBoolean("succeed", this.succeed);

        super.onSaveInstanceState(outState);
    }

    private void initView(boolean showInstallLayout) {
        this.resultImg = (ImageView) this.findViewById(R.id.result_image);
        this.resultTv = (TextView) this.findViewById(R.id.result);
        this.detailTv = (TextView) this.findViewById(R.id.tv_detail);

        this.removeBtn = (Button) this.findViewById(R.id.button_uninstall);
        this.installBtn = (Button) this.findViewById(R.id.button_reinstall);
        Button closeBtn = (Button) this.findViewById(R.id.button_close);
        removeBtn.setOnClickListener(this);
        installBtn.setOnClickListener(this);
        closeBtn.setOnClickListener(this);

        this.waitingLayout = (LinearLayout) this
                .findViewById(R.id.layout_apk_generating);
        this.installLayout = (LinearLayout) this
                .findViewById(R.id.layout_apk_reinstall);
        if (showInstallLayout) {
            waitingLayout.setVisibility(View.INVISIBLE);
            installLayout.setVisibility(View.VISIBLE);
        } else {
            waitingLayout.setVisibility(View.VISIBLE);
            installLayout.setVisibility(View.INVISIBLE);
        }

        if (this.modifyFinished) {
            displayResult();
        }
    }

    private void installApk(String apkFilePath) {
//        int result = Settings.Secure.getInt(getContentResolver(),
//                Settings.Secure.INSTALL_NON_MARKET_APPS, 0);
//        if (result == 0) {
//            Toast.makeText(ApkCreateActivity.this,
//                    R.string.must_allow_non_market_app, Toast.LENGTH_LONG)
//                    .show();
//            try {
//                Intent intent = new Intent();
//                if (android.os.Build.VERSION.SDK_INT >= 14) {
//                    intent.setAction(Settings.ACTION_SECURITY_SETTINGS);
//                } else {
//                    intent.setAction(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
//                    // ACTION_APPLICATION_SETTINGS);
//                }
//                startActivity(intent);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else {
//            // Succeed when apk is installed
//            this.setResult(1000);
//
//            ApkInstaller.doSafeInstall(this, apkFilePath);
//        }

        // Do not call this any more, as it may be taken as unauthorized manner
        //ApkInstaller.doSafeInstall(this, apkFilePath);
        ApkInstaller.install(this, apkFilePath);
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setDataAndType(Uri.fromFile(new File(apkFilePath)),
//                "application/vnd.android.package-archive");
//        startActivity(intent);
    }

    private boolean isAppInstalled() {
        try {
            ApkInfoParser.AppInfo info = new ApkInfoParser().parse(this, this.outApkPath);

            if (info != null) {
                PackageManager pm = this.getPackageManager();
                pm.getPackageInfo(info.packageName, 0);

                // If the package name is found, return true
                return true;
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private void prepareImages() throws IOException {
        if (imgReplaces == null || imgReplaces.isEmpty()) {
            return;
        }

        ZipFile zfile = new ZipFile(apkPath);

        // Arrange the replaces
        Map<String, List<String>> filePath2Entries = new HashMap<>();
        for (Map.Entry<String, String> entry : imgReplaces.entrySet()) {
            String entryPath = entry.getKey();
            String filePath = entry.getValue();
            List<String> pathList = filePath2Entries.get(filePath);
            if (pathList == null) {
                pathList = new ArrayList<>();
                filePath2Entries.put(filePath, pathList);
            }
            pathList.add(entryPath);
        }

        // To scale the image file
        for (String filePath : filePath2Entries.keySet()) {
            List<String> entryList = filePath2Entries.get(filePath);
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);
            for (String entryName : entryList) {
                // Get the size of original resource file
                ZipEntry entry = zfile.getEntry(entryName);
                InputStream input = zfile.getInputStream(entry);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(input, null, options);

                int width = options.outWidth;
                int height = options.outHeight;

                // Log.d("DEBUG", String.format(
                // "entry=%s, ewidth=%d, height=%d\n", entryName, width,
                // height));

                // Get the resized bitmap
                Bitmap resizeBitmap = ImageTools.zoomBitmap(bitmap, width,
                        height);

                // Save resized image
                String outFilePath = workingDir
                        + entryName.replaceAll("/", "_");
                FileOutputStream os = new FileOutputStream(outFilePath);

                if (outFilePath.endsWith(".png")) {
                    resizeBitmap.compress(CompressFormat.PNG, 80, os);
                } else {
                    resizeBitmap.compress(CompressFormat.JPEG, 80, os);
                }

                os.close();

                // Record to replaces
                allReplaces.put(entryName, outFilePath);
            }
        }

        zfile.close();
    }

    // Set the display text according to the result
    private void displayResult() {
        if (succeed) {
            String str = getResources().getString(R.string.apk_savedas_1);
            String strPlace = String.format(str, outApkPath);
            String strSucceed = getResources().getString(R.string.succeed);
            String message = strSucceed + "!\n" + strPlace + "\n\n";

            // Check need reinstall or not
            boolean needReinstall = false;
            if (isAppInstalled()) {
                needReinstall = true;
            }

            if (BuildConfig.WITH_SIGN) {
                if (needReinstall) {
                    String allStr = message
                            + getResources().getString(R.string.remove_tip);
                    SpannableStringBuilder style = new SpannableStringBuilder(allStr);
                    style.setSpan(new AbsoluteSizeSpan(22), message.length(),
                            allStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    this.resultTv.setText(style);
                    this.removeBtn.setVisibility(View.VISIBLE);
                } else {
                    this.resultTv.setText(message);
                    this.removeBtn.setVisibility(View.GONE);
                }
            }
            // APK is not signed
            else {
                String allStr = message + getResources().getString(R.string.not_signed_tip);
                SpannableStringBuilder style = new SpannableStringBuilder(allStr);
                style.setSpan(new AbsoluteSizeSpan(22), message.length(),
                        allStr.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                ForegroundColorSpan fcs = new ForegroundColorSpan(Color.rgb(255, 30, 30));
                style.setSpan(fcs, message.length(),
                        allStr.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                this.resultTv.setText(style);

                this.removeBtn.setVisibility(View.GONE);
                this.installBtn.setVisibility(View.GONE);
            }

            resultImg.setImageResource(R.drawable.succeed);
        }

        // Failed
        else {
            resultImg.setImageResource(R.drawable.failed);
            resultTv.setText(this.errorMessage);
            removeBtn.setVisibility(View.GONE);
            installBtn.setVisibility(View.GONE);
        }
    }

    // The apk is successfully modified or not
    public void modifyFinished(boolean result) {
        this.succeed = result;

        if (result) {
            this.outApkPath = thread.outputApkPath;
        } else {
            this.errorMessage = thread.getError();
        }

        displayResult();

        waitingLayout.setVisibility(View.INVISIBLE);
        installLayout.setVisibility(View.VISIBLE);

        this.modifyFinished = true;
    }

    private boolean isSameSignature() {
        String signature = CheckUtil.getSign(this, apkInfo.packageName);
        return CheckUtil.isRevisedSignature(signature);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        // Remove the package
        if (id == R.id.button_uninstall) {
            if (apkInfo != null) {
                Uri packageURI = Uri.parse("package:" + apkInfo.packageName);
                Intent intent = new Intent(Intent.ACTION_DELETE, packageURI);
                startActivity(intent);
            }
        }

        // Install
        else if (id == R.id.button_reinstall) {
            installApk(outApkPath);
        }

        // Close
        else if (id == R.id.button_close) {
            ApkCreateActivity.this.finish();
        }
    }

    public boolean isFreeVersion() {
        return !BuildConfig.IS_PRO;
    }

    // This function is called on another running thread (not the UI thread)
    @Override
    public void updateDescription(String strDesc) {
        this.handler.updateDescription(strDesc);
    }

    private static class CreatingThread extends Thread {

        WeakReference<ApkCreateActivity> activityRef;
        private String outputApkPath;
        private String error;

        private boolean finished = false;

        CreatingThread(ApkCreateActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            ApkCreateActivity activity = activityRef.get();

            try {
                // Call extra preparing interface
                if (activity.makeInterfaces != null) {
                    for (IApkMaking interf : activity.makeInterfaces) {
                        interf.prepareReplaces(activity, activity.apkPath,
                                activity.allReplaces, activityRef.get());
                    }
                }

                // To modify the class name in DEX file
                if (activity.clsNameReplaces != null) {
                    String targetPath = activity.workingDir + ".dex";
                    DexStringEditor dexEditor = new DexStringEditor(activity.apkPath);
                    dexEditor.replaceDexString(activity.clsNameReplaces, targetPath);
                    activity.allReplaces.put("classes.dex", targetPath);
                }

                // To modify resources.arsc
                if ((activity.newPackageNameInArsc != null)
                        || toModifyAppName(activity)) {
                    modifyResourceFile();
                }

                // To prepare/zoom all the images
                activity.prepareImages();


                String outApkName;
                String strTail = (BuildConfig.WITH_SIGN ? "_signed" : "_unsigned");
                int nameRule = SettingActivity.getOutputApkRule(activity);
                switch (nameRule) {
                    case 0:
                        outApkName = activity.packageName + strTail;
                        break;
                    case 2:
                        outApkName = activity.apkInfo.label + strTail;
                        break;
                    default:
                        outApkName = "gen" + strTail;
                        break;
                }
                String apkPath = activity.apkPath;
                this.outputApkPath = ApkInfoActivity.createOutputPath(
                        apkPath, activity.workingDir, outApkName);

                Map<String, String> replaces = activity.allReplaces;

                // Sign the new APK (or merge it if does not need sign)
                Map<String, String> jarPath2FilePath = new HashMap<>();
                jarPath2FilePath.putAll(replaces);

                if (BuildConfig.WITH_SIGN) {
                    SignHelper.sign(activity, apkPath, outputApkPath,
                            jarPath2FilePath, null, null);
                } else {
                    StringBuilder sb = new StringBuilder();
                    int replaceLen = 0;
                    for (Map.Entry<String, String> entry : jarPath2FilePath.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        sb.append(key);
                        sb.append('\n');
                        sb.append(value);
                        sb.append('\n');
                        replaceLen += key.getBytes().length + value.getBytes().length + 2;
                    }
                    MainActivity.md(outputApkPath, apkPath, "", 0, "", 0, sb.toString(), replaceLen);
                }

                activity.handler.sendEmptyMessage(0);

            } catch (Throwable e) {
                String clsName = e.getClass().getSimpleName();
                String msg = e.getMessage();
                if (msg == null) {
                    this.error = clsName;
                } else {
                    this.error = clsName + ": " + msg;
                }
                activity.handler.sendEmptyMessage(1);
                e.printStackTrace();
            }

            finished = true;
        }

        private boolean toModifyAppName(ApkCreateActivity activity) {
            return activity.oldAppNameInArsc != null
                    && activity.newAppNameInArsc != null
                    && (!activity.oldAppNameInArsc
                    .equals(activity.newAppNameInArsc));
        }

        // Modify resources.arsc
        private void modifyResourceFile() {
            ZipFile zipFile = null;
            InputStream is = null;
            try {
                ApkCreateActivity activity = activityRef.get();

                zipFile = new ZipFile(activity.apkPath);
                ZipEntry entry = zipFile.getEntry("resources.arsc");
                is = zipFile.getInputStream(entry);

                String targetPath = activity.workingDir + ".arsc";
                ResourceEditor resEditor = new ResourceEditor(is, targetPath);

                // Modify package name in resources.arsc
                if (activity.newPackageNameInArsc != null) {
                    resEditor.modifyPackageName(activity.newPackageNameInArsc);
                }

                // Modify app name and package name
                if (activity.newAppNameInArsc != null || activity.newPackageNameInArsc != null) {
                    resEditor.modifyString(activity.oldAppNameInArsc,
                            activity.newAppNameInArsc,
                            activity.apkInfo.packageName,
                            activity.newPackageNameInArsc);
                }

                // Save and record the replace
                resEditor.save();
                activity.allReplaces.put("resources.arsc", targetPath);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeQuietly(is);
                closeQuietly(zipFile);
            }
        }

        private void closeQuietly(ZipFile zfile) {
            try {
                zfile.close();
            } catch (IOException ignored) {
            }
        }

        private void closeQuietly(Closeable c) {
            if (c != null) {
                try {
                    c.close();
                } catch (Throwable ignored) {
                }
            }
        }

        public String getError() {
            return error;
        }

        @SuppressWarnings("unused")
        public boolean isFinished() {
            return finished;
        }
    }

    private static class MyHandler extends Handler {
        WeakReference<ApkCreateActivity> activityReference;
        private String strDesc;

        MyHandler(ApkCreateActivity activity) {
            activityReference = new WeakReference<>(activity);
        }

        void updateDescription(String strDesc) {
            this.strDesc = strDesc;
            this.sendEmptyMessage(100);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final ApkCreateActivity activity = activityReference.get();
            switch (msg.what) {
                case 0:
                    // Succeed
                    if (activity != null) {
                        if (activity.isFreeVersion()
                                && (System.currentTimeMillis() - activity.startTime) < 5000) {
                            long delay = 5500 - (System.currentTimeMillis() - activity.startTime);
                            this.sendEmptyMessageDelayed(0, delay);
                        } else {
                            activity.modifyFinished(true);
                        }
                    }
                    break;
                case 1:
                    if (activity != null) {
                        activity.modifyFinished(false);
                    }
                    break;
                case 100:
                    // Update description
                    if (activity != null) {
                        activity.detailTv.setText(strDesc);
                    }
                    break;
            }
        }
    }
}
