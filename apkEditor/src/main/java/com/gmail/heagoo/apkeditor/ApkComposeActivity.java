package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.ce.IApkMaking;
import com.gmail.heagoo.apkeditor.se.ApkInstaller;
import com.gmail.heagoo.apkeditor.util.AxmlStringModifier;
import com.gmail.heagoo.apkeditor.util.ErrorFixManager;
import com.gmail.heagoo.apkeditor.util.OdexPatcher;
import com.gmail.heagoo.common.ApkInfoParser;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.common.ITaskCallback;
import com.gmail.heagoo.common.PackageUtil;
import com.gmail.heagoo.common.PreferenceUtil;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Map;

public class ApkComposeActivity extends CustomizedLangActivity
        implements ITaskCallback, OnClickListener {

    public static final int SUCCEED = 10005;
    public static final int FAILED = -1;
    public static final String PRIMARY_NOTIF_CHANNEL = "default";
    public static final int PRIMARY_FOREGROUND_NOTIF_SERVICE_ID = 1001;
    // Created from notification or not (by clicking at notification)
    public boolean createdFromNotification = false;
    // Generated apk path, Package name of the apk file
    protected String srcApkPath;
    // View related
    private LinearLayout composingLayout;
    private LinearLayout composedLayout;
    private TextView progressTv;
    private TextView resultTv;
    private ListView failedLv;
    private ImageView resultImgView;
    private Button hideWarningBtn;
    private Button removeBtn;
    private LinearLayout fixLayout;
    private TextView fixTipTv;
    private TextView patchTip;
    private Button patchBtn;
    private Button fixBtn;
    private Button copyBtn;
    private Button bgButton;
    // Apply patch to code cache succeed or not
    private boolean patchSucceed = false;
    // Progressing and result
    private MyHandler msgHandler;
    private TaskStepInfo stepInfo;
    private String errMessage;
    // Time of starting composing
    private long startTime;
    private String targetApkPath;
    private String packageName;
    private String decodeRootPath;
    private boolean codeModified;
    private boolean signAPK;
    private ApkComposeService.ComposeServiceBinder binder;
    // Use to automatically fix the error
    private ErrorFixManager errFixer;
    // Activity visible or not
    private boolean isActivityVisible;
    // To different the invoke from service
    private String intentAction;
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (ApkComposeService.ComposeServiceBinder) service;

            // Cancel the notification if invoked from service
            // When activity is created by clicking at the notification, will into following code
            if (intentAction != null && Constants.ACTION.MAIN_ACTION.equals(intentAction)) {
                ApkComposeActivity.this.createdFromNotification = true;
                if (!binder.isRunning()) {
                    binder.hideNotification();
                }
                // Show "Put to Background" button
                bgButton.setVisibility(View.VISIBLE);
            }

            binder.setObserver(ApkComposeActivity.this);

            Map<String, Object> keyValues = binder.getValues();
            srcApkPath = (String) keyValues.get("srcApkPath");
            targetApkPath = (String) keyValues.get("targetApkPath");
            decodeRootPath = (String) keyValues.get("decodeRootPath");
            codeModified = (Boolean) keyValues.get("codeModified");
            signAPK = (Boolean) keyValues.get("signAPK");
            errFixer = new ErrorFixManager(decodeRootPath);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel chan1 = new NotificationChannel(
                    PRIMARY_NOTIF_CHANNEL,
                    "default",
                    NotificationManager.IMPORTANCE_DEFAULT);

            chan1.setLightColor(Color.TRANSPARENT);
            chan1.enableVibration(false);
            chan1.setVibrationPattern(new long[]{0L});
            chan1.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            notificationManager.createNotificationChannel(chan1);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create notification channel
        createChannel();

        Intent intent = getIntent();
        this.intentAction = intent.getAction();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
// sawsem theme
//        switch (GlobalConfig.instance(this).getThemeId()) {
//            case GlobalConfig.THEME_DARK_DEFAULT:
//                super.setTheme(android.R.style.Theme_Black_NoTitleBar);
//                setContentView(R.layout.activity_apkcompose_dark);
//                break;
//            case GlobalConfig.THEME_DARK_RUSSIAN:
//                super.setTheme(android.R.style.Theme_Black_NoTitleBar);
//                setContentView(R.layout.activity_apkcompose_dark_ru);
//                break;
//            default:
        setContentView(R.layout.activity_apkcompose);
//                break;
//        }

        initView();

        bind2Service();

        // Record the start time
        this.startTime = System.currentTimeMillis();

        // For free version, show the AD
        if (!BuildConfig.IS_PRO) {
            try {
                // 20180915: as it it removed by google play, to remove admob
                // load popup ad only when > 10 minutes
//                if (System.currentTimeMillis() - getLastPopAdTime() > 600 * 1000) {
//                    this.interestAd = (IInterstitialAd) RefInvoke.invokeStaticMethod(
//                            "com.gmail.heagoo.apkeditor.free.InterestAdManager",
//                            "createInterstitialAd",
//                            new Class<?>[]{Activity.class},
//                            new Object[]{this});
//                    this.interestAd.load();
//                    msgHandler.sendEmptyMessageDelayed(MyHandler.CHECK_INTERSTITIAL_AD, 2000);
//                }
//                // Big banner ad
//                else {
//                    this.adManager = ADManager.init(this, ADManager.AdSource.bigadmob);
//                    msgHandler.sendEmptyMessageDelayed(MyHandler.SHOW_BG_BUTTON, 2000);
//                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        intentAction = intent.getAction();
        // Cancel the notification if invoked from service
        if (intentAction != null && Constants.ACTION.MAIN_ACTION.equals(intentAction)) {
            if (binder != null && !binder.isRunning()) {
                binder.hideNotification();
            }
        }
    }

    private void bind2Service() {
        Intent intent = new Intent(this, ApkComposeService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        isActivityVisible = false;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (connection != null) {
            unbindService(connection);
            connection = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityVisible = true;
    }

    private void initView() {
        this.msgHandler = new MyHandler(this);
        this.composingLayout = (LinearLayout) this.findViewById(R.id.layout_apk_composing);
        this.composedLayout = (LinearLayout) this.findViewById(R.id.layout_apk_composed);
        this.progressTv = (TextView) this.findViewById(R.id.progress_tip);
        this.resultTv = (TextView) this.findViewById(R.id.result);
        this.failedLv = (ListView) this.findViewById(R.id.failed_view);
        this.resultImgView = (ImageView) this.findViewById(R.id.result_image);
        this.fixLayout = (LinearLayout) this.findViewById(R.id.fix_layout);
        this.fixTipTv = (TextView) this.findViewById(R.id.tv_fix_tip);

        resultTv.setOnClickListener(this);
        composedLayout.setVisibility(View.INVISIBLE);

        // Close button
        Button closeBtn = (Button) this.findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(this);

        // Remove the old app
        this.removeBtn = (Button) this.findViewById(R.id.btn_remove);
        removeBtn.setOnClickListener(this);

        // Fix the issue
        this.fixBtn = (Button) this.findViewById(R.id.btn_fix);
        fixBtn.setOnClickListener(this);

        // Copy error message
        this.copyBtn = (Button) findViewById(R.id.btn_copy_errmsg);
        copyBtn.setOnClickListener(this);

        // Hide warning
        this.hideWarningBtn = (Button) findViewById(R.id.btn_hide_warning);
        hideWarningBtn.setOnClickListener(this);

        // Put it to background
        this.bgButton = (Button) findViewById(R.id.btn_bg);
        bgButton.setOnClickListener(this);
        // For free version, hide the button at first
        if (!BuildConfig.IS_PRO) {
            bgButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void setTaskStepInfo(TaskStepInfo stepInfo) {
        this.stepInfo = stepInfo;
        msgHandler.sendEmptyMessage(MyHandler.STEPINFO);
    }

    @Override
    public void setTaskProgress(float progress) {
        // progress not returned yet
    }

    @Override
    public void taskSucceed() {
        msgHandler.sendEmptyMessage(MyHandler.SUCCEED);
    }

    @Override
    public void taskFailed(String errMessage) {
        this.errMessage = errMessage;
        msgHandler.sendEmptyMessage(MyHandler.FAILED);
    }

    @Override
    public void taskWarning(String message) {
        msgHandler.toast(message);
    }

    public void updateComposeInfo() {
        progressTv.setText(String.format(
                getResources().getString(R.string.step) + " %d/%d: %s",
                stepInfo.stepIndex, stepInfo.stepTotal,
                stepInfo.stepDescription));
    }

    public boolean isFreeVersion() {
        return !BuildConfig.IS_PRO;
    }

    public void composeFinished(boolean ret) {
        this.composingLayout.setVisibility(View.INVISIBLE);
        this.composedLayout.setVisibility(View.VISIBLE);

        // Clear notification in status bar when activity not finished
        if (binder != null && isActivityVisible) {
            binder.hideNotification();
        }

        Button installBtn = (Button) this.findViewById(R.id.btn_install);

        if (ret) {
            this.setResult(SUCCEED);

            installBtn.setOnClickListener(this);

            // Hide the failed view
            this.findViewById(R.id.succeeded_view).setVisibility(View.VISIBLE);
            this.findViewById(R.id.failed_view).setVisibility(View.GONE);

            // Show succeed image
            resultImgView.setImageResource(R.drawable.succeed);

            String str = getResources().getString(R.string.apk_savedas_1);
            String strPlace = String.format(str, targetApkPath);
            String strSucceed = getResources().getString(R.string.succeed);
            String message = strSucceed + "!\n" + strPlace + "\n\n";


            // APK is signed
            if (BuildConfig.WITH_SIGN && signAPK) {
                // Check if already installed
                this.packageName = getApkPackageName();
                if (this.isPackageInstalled(packageName)) {
                    String allStr = message + getResources().getString(R.string.remove_tip);
                    SpannableStringBuilder style = new SpannableStringBuilder(allStr);
                    style.setSpan(new AbsoluteSizeSpan(22), message.length(),
                            allStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    this.resultTv.setText(style);
                    this.removeBtn.setVisibility(View.VISIBLE);
                } else {
                    this.resultTv.setText(message);
                    this.removeBtn.setVisibility(View.GONE);
                }
                this.copyBtn.setVisibility(View.GONE);
                installBtn.setVisibility(View.VISIBLE);
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
                this.copyBtn.setVisibility(View.GONE);
                this.removeBtn.setVisibility(View.GONE);
                installBtn.setVisibility(View.GONE);
            }


            // Patch the DEX cache
            if (BuildConfig.ODEX_PATCH) {
                if (codeModified && isArtRuntime()) {
                    this.patchTip = (TextView) this.findViewById(R.id.tv_patch_tip);
                    this.findViewById(R.id.patch_dex_layout).setVisibility(View.VISIBLE);
                    this.patchBtn = (Button) this.findViewById(R.id.btn_patch);
                    patchBtn.setOnClickListener(this);
                } else {
                    this.findViewById(R.id.patch_dex_layout).setVisibility(View.GONE);
                }
            }
        } else {
            this.setResult(FAILED);

            // Hide the succeed view
            this.findViewById(R.id.succeeded_view).setVisibility(View.GONE);
            this.findViewById(R.id.failed_view).setVisibility(View.VISIBLE);

            this.failedLv.setDivider(null);
            this.failedLv.setAdapter(new ApkComposeFailAdapter(this, errMessage));
            Log.d("sawsem", errMessage);
            resultImgView.setImageResource(R.drawable.failed);

            // Show "Hide Warnings" button or not
            if (errMessage.contains("warning:")) {
                this.hideWarningBtn.setVisibility(View.VISIBLE);
            } else {
                this.hideWarningBtn.setVisibility(View.GONE);
            }

            // Auto fix
            errFixer.setErrMessage(errMessage);
            if (errFixer.isErrorFixable()) {
                int resId = getTipResourceId();
                fixTipTv.setText(resId);
                fixLayout.setVisibility(View.VISIBLE);
            }

            // Update button
            installBtn.setVisibility(View.GONE);
            removeBtn.setVisibility(View.GONE);
            copyBtn.setVisibility(View.VISIBLE);
        }
    }

    private boolean isArtRuntime() {
        final String vmVersion = System.getProperty("java.vm.version");
        return vmVersion != null && vmVersion.charAt(0) > '1';
    }

    private int getTipResourceId() {
        int fid = errFixer.getFixerId();
        int resId = -1;
        switch (fid) {
            case ErrorFixManager.FIXER_INVALID_FILENAME:
                resId = R.string.fix_invalid_name_tip;
                break;
            case ErrorFixManager.FIXER_INVALID_TOKEN:
                resId = R.string.fix_invalid_token_tip;
                break;
            case ErrorFixManager.FIXER_INVALID_ATTR:
                resId = R.string.fix_invalid_attr_tip;
                break;
            case ErrorFixManager.FIXER_INVALID_SYMBOL:
                resId = R.string.fix_invalid_symbol_tip;
                break;
            case ErrorFixManager.FIXER_ERROR_EQUIVALENT:
                resId = R.string.fix_error_equivalent;
                break;
        }
        return resId;
    }

    private boolean isPackageInstalled(String packageName) {
        PackageManager pkgMgr = this.getPackageManager();
        try {
            pkgMgr.getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Get the package name of the new build apk file
    private String getApkPackageName() {
        try {
            ApkInfoParser.AppInfo info = new ApkInfoParser().parse(this, targetApkPath);
            if (info != null) {
                return info.packageName;
            }
        } catch (Exception e1) {
        }

        return null;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            this.finish();
        } else if (id == R.id.btn_install) {
            ApkInstaller.install(this, targetApkPath);
            // this.finish();
        } else if (id == R.id.btn_remove) {
            if (this.packageName != null) {
                PackageUtil.uninstallPackage(this, packageName);
            }
        } else if (id == R.id.btn_copy_errmsg) {
            com.gmail.heagoo.common.ClipboardUtil.copyToClipboard(this,
                    errMessage);
            Toast.makeText(this, R.string.errmsg_copied, Toast.LENGTH_SHORT)
                    .show();
        } else if (id == R.id.btn_fix) {
            if (this.errFixer != null) {
                this.fixLayout.setVisibility(View.GONE);
                this.errFixer.fixErrors(this);
            }
        } else if (id == R.id.btn_patch) {
            if (!patchSucceed) {
                applyCodePatch();
            } else {
                launchApp();
            }
        } else if (id == R.id.btn_hide_warning) {
            StringBuilder sb = new StringBuilder();
            String[] lines = errMessage.split("\n");
            for (String line : lines) {
                if (!line.startsWith("warning:")) {
                    sb.append(line);
                    sb.append("\n");
                }
            }
            // Hide the warning message
            ApkComposeFailAdapter adapter = (ApkComposeFailAdapter) failedLv.getAdapter();
            adapter.updateMessage(sb.toString());
            adapter.notifyDataSetChanged();
            this.hideWarningBtn.setVisibility(View.GONE);
        }
        // Put it to background
        else if (id == R.id.btn_bg) {
            // For pro version, as it already made as foreground service, just finish this activity
            if (BuildConfig.IS_PRO) {
                this.finish();
            }
            // For free version,
            else {
                if (binder != null) {
                    binder.showNotification();
                }
                this.finish();
            }
        }
        // Result text view
        else if (id == R.id.result) {
            if (this.targetApkPath != null) {
                showFileInExplorer(this.targetApkPath);
            }
        }
    }

    private void showFileInExplorer(String filepath) {
        if (!new File(filepath).exists()) {
            return;
        }

        int position = filepath.lastIndexOf("/");
        if (position == -1) {
            return;
        }

        String path = filepath.substring(0, position + 1);

        File file = new File(path);
        if (null == file || !file.exists()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.fromFile(file), "text/csv");
        try {
            startActivity(intent);
            //startActivity(Intent.createChooser(intent,"选择浏览工具"));
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }

//        Uri selectedUri = Uri.parse(path);
//Intent intent = new Intent(Intent.ACTION_VIEW);
//intent.setDataAndType(selectedUri, "resource/folder");
//
//if (intent.resolveActivityInfo(getPackageManager(), 0) != null)
//{
//    startActivity(intent);
//}
//else
//{
//    // if you reach this place, it means there is no any file
//    // explorer app installed on your device
//}

    }

    private void launchApp() {
        PackageManager pm = this.getPackageManager();
        try {
            Intent it = pm.getLaunchIntentForPackage(packageName);
            if (null != it) {
                this.startActivity(it);
            }
        } catch (ActivityNotFoundException e) {
        }
    }

    // Apply the DEX patch to the cache
    private void applyCodePatch() {
        new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
            private String errMessage = null;
            private String targetOdex = null;

            @Override
            public void process() throws Exception {
                OdexPatcher patcher = new OdexPatcher(packageName);
                patcher.applyPatch(ApkComposeActivity.this, targetApkPath);
                this.targetOdex = patcher.targetOdex;
                if (patcher.errMessage != null) {
                    this.errMessage = patcher.errMessage;
                    throw new Exception(errMessage);
                }
            }

            @Override
            public void afterProcess() {
                if (errMessage == null) {
                    patchSucceed = true;
                    String fmt = ApkComposeActivity.this.getString(R.string.patch_code_cache_done);
                    String msg = String.format(fmt, targetOdex);
                    patchTip.setText(msg);
                    patchBtn.setText(R.string.launch);
                } else {
                    patchTip.setText(errMessage);
                }
            }
        }, -1).show();
    }

    public void buildAgain() {
        if (binder != null) {
            // Set extra AXML Modifier
            Map<String, Map<String, String>> m = errFixer.getModifications();
            if (this.binder != null && !m.isEmpty()) {
                binder.setBuildHooker(createBuildHooker(m));
            }

            // Switch the layout and build again
            progressTv.setText("");
            composingLayout.setVisibility(View.VISIBLE);
            composedLayout.setVisibility(View.GONE);
            binder.buildAgain();
        }
    }

    private IApkMaking createBuildHooker(
            Map<String, Map<String, String>> modifications) {
        return new AxmlStringModifier(decodeRootPath, modifications);
    }

    private void showTipDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dlg_tip, null);
        TextView msgTv = (TextView) view.findViewById(R.id.tv_message);
        msgTv.setText(R.string.build_still_running_tip);
        final CheckBox cb = (CheckBox) view.findViewById(R.id.cb_show_once);

        AlertDialog.Builder tipDlg = new AlertDialog.Builder(this)
                .setTitle(R.string.tip)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Still running means task is still in status bar
                        if (binder != null && binder.isRunning()) {
                            ApkComposeActivity.this.finish();
                        }
                        // Remember the option and save to preference
                        if (cb.isChecked()) {
                            PreferenceUtil.setBoolean(
                                    ApkComposeActivity.this, "donot_show_compose_tip", true);
                        }
                    }
                });

        tipDlg.setView(view);

        tipDlg.show();
    }

    private void stopBuildAndGoBack() {
        try {
            if (binder != null) {
                binder.stopBuilding();
            }

            unbindService(connection);
            connection = null;

            Intent intent = new Intent(this, ApkComposeService.class);
            stopService(intent);
        } catch (Throwable ignored) {
        }
        this.finish();
    }

    // Ask user: are you sure to stop build?
    private void showStopBuildDialog() {
        AlertDialog.Builder confirmDlg = new AlertDialog.Builder(this)
                .setTitle(R.string.please_note)
                .setMessage(R.string.sure_to_stop_build)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopBuildAndGoBack();
                    }
                })
                .setNegativeButton(android.R.string.no, null);
        confirmDlg.show();
    }

    @Override
    public void onBackPressed() {
        if (binder != null && binder.isRunning()) {
            // For pro version, show tip if needed
            if (BuildConfig.IS_PRO) {
                if (!PreferenceUtil.getBoolean(this, "donot_show_compose_tip", false)) {
                    showTipDialog();
                } else {
                    this.finish();
                }
            }
            // For free version, stop the service
            else {
                showStopBuildDialog();
            }
        }
        // Build service is not running, just finish this activity
        else {
            this.finish();
        }
    }

    private static class MyHandler extends Handler {
        public static final int STEPINFO = 1;
        public static final int SUCCEED = 2;
        public static final int FAILED = 3;
        public static final int SHOW_MESSAGE = 4;

        private WeakReference<ApkComposeActivity> activityRef;

        private String tmpMessage;

        public MyHandler(ApkComposeActivity a) {
            this.activityRef = new WeakReference<>(a);
        }

        public void toast(String msg) {
            if ("x".equals(msg)) {
                this.tmpMessage = getString();
            } else {
                this.tmpMessage = msg;
            }
            this.sendEmptyMessage(SHOW_MESSAGE);
        }

        private String getString() {
            StringBuffer sb = new StringBuffer();
            sb.append(
                    "Nbojgftu!fejujoh!jt!ejtbcmfe!)tffnt!opu!b!hfovjof!wfstjpo*");
            for (int i = 0; i < sb.length(); i++) {
                char c = sb.charAt(i);
                sb.setCharAt(i, (char) (c - 1));
            }
            return sb.toString();
        }

        @Override
        public void handleMessage(Message msg) {
            ApkComposeActivity activity = activityRef.get();
            if (activity != null) {
                switch (msg.what) {
                    case STEPINFO:
                        activity.updateComposeInfo();
                        break;
                    case SUCCEED:
                        activity.composeFinished(true);
                        break;
                    case FAILED:
                        activity.composeFinished(false);
                        break;
                    case SHOW_MESSAGE:
                        Toast.makeText(activity, tmpMessage, Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }
    }
}
