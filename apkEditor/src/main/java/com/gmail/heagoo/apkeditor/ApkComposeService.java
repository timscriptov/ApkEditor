package com.gmail.heagoo.apkeditor;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.ce.IApkMaking;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.ITaskCallback;
import com.gmail.heagoo.common.RefInvoke;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ApkComposeService extends Service implements ITaskCallback {

    private String decodeRootPath;
    private String srcApkPath;
    private Boolean stringModified;
    private Boolean manifestModified;
    private Boolean resFileModified;
    private ArrayList<String> modifiedSmaliFolders;
    // private ArrayList<String> replaceEntries;
    // private ArrayList<String> replaceFiles;
    private Map<String, String> addedFiles;
    private Map<String, String> replacedFiles;
    private Set<String> deletedFiles;
    // Recorded all the relation between file entry to zip entry
    // like res/drawable-hdpi-v4/a.png -> res/drawable-hdpi/a.png
    private Map<String, String> fileEntry2ZipEntry;

    // Output apk path
    private String targetApkPath;

    // Composing thread and result
    private ComposeThread composeThread;
    private ComposeResult composeResult = new ComposeResult();

    private WeakReference<ITaskCallback> observer;

    private IApkMaking extraMaker = null;

    // Foreground notification
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotifyBuilder;
    private boolean foregroundStarted = false;

    // For UI management
    private MyHandler handler = new MyHandler();
    private ComposeServiceBinder binder = new ComposeServiceBinder();

    ;
    private boolean signAPK;
    // Update notification title and description
    // Called from non-UI thread
    private long lastUpdateTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // When the service is restarted, intent = null
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId);
        }

        this.decodeRootPath = ActivityUtil.getParam(intent, "decodeRootPath");
        this.srcApkPath = ActivityUtil.getParam(intent, "srcApkPath");
        this.targetApkPath = ActivityUtil.getParam(intent, "targetApkPath");
        String str = ActivityUtil.getParam(intent, "stringModified");
        this.stringModified = Boolean.valueOf(str);
        str = ActivityUtil.getParam(intent, "manifestModified");
        this.manifestModified = Boolean.valueOf(str);
        str = ActivityUtil.getParam(intent, "resFileModified");
        this.resFileModified = Boolean.valueOf(str);
        this.modifiedSmaliFolders = ActivityUtil.getStringArray(intent, "modifiedSmaliFolders");
        this.signAPK = ActivityUtil.getBoolParam(intent, "signAPK");

        this.addedFiles = ActivityUtil.getMapParam(intent, "addedFiles");
        this.replacedFiles = ActivityUtil.getMapParam(intent, "replacedFiles");
        this.deletedFiles = new HashSet<>();
        List<String> delEntries = ActivityUtil.getStringArray(intent, "deletedFiles");
        for (String name : delEntries) {
            this.deletedFiles.add(name);
        }

        String passedFile = ActivityUtil.getParam(intent, "fileEntry2ZipEntry");
        if (passedFile != null) {
            this.fileEntry2ZipEntry = getMapFromFile(passedFile);
        }

        // For debug only
        dumpModification();

        resetStatus();

        // Initially show notification in pro version
        // For free version, only show it when ad is ready
        if (BuildConfig.IS_PRO) {
            showNotification();
        }

        startComposeThread();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void showNotification() {
        Intent composeIntent = new Intent(this, ApkComposeActivity.class);
        composeIntent.setAction(Constants.ACTION.MAIN_ACTION);
        composeIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, composeIntent, 0);

        int iconId = (int) RefInvoke.invokeStaticMethod(
                "com.gmail.heagoo.seticon.SetIcon", "getIconId", null, null);
        Bitmap icon = BitmapFactory.decodeResource(getResources(), iconId);
        String appName = getString(R.string.app_name);

        this.mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.mNotifyBuilder = new NotificationCompat.Builder(this, ApkComposeActivity.PRIMARY_NOTIF_CHANNEL);
        } else {
            this.mNotifyBuilder = new NotificationCompat.Builder(this);
        }
        this.mNotifyBuilder.setContentTitle(appName)
                .setTicker(appName)
                .setContentText(getString(R.string.build_ongoing))
                .setSmallIcon(iconId)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true);
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, mNotifyBuilder.build());
        this.foregroundStarted = true;
    }

    private void updateNotification(boolean forceShow, String title, String desc) {
        if (mNotificationManager != null) {
            handler.removeMessages(0);
            handler.setInfo(title, desc);
            // If not updated for a long time, then directly update it
            if (forceShow || (System.currentTimeMillis() - lastUpdateTime > 1000)) {
                handler.sendEmptyMessage(0);
            }
            // Otherwise send delayed message
            else {
                handler.sendEmptyMessageDelayed(0, 500);
            }
        }
    }

    private void resetStatus() {
        if (composeThread != null && composeThread.isAlive()) {
            composeThread.stopRunning();
        }

        composeResult.clear();
    }

    private void dumpModification() {
        // // For Debug
        // Log.d("DEBUG", "resFileModified=" + resFileModified);
        // Log.d("DEBUG", "Added Entry: ");
        // for (Map.Entry<String, String> entry : addedFiles.entrySet()) {
        // Log.d("DEBUG", "\t" + entry.getKey() + " --> " + entry.getValue());
        // }
        //
        // Log.d("DEBUG", "Replaced Entry: ");
        // for (Map.Entry<String, String> entry : replacedFiles.entrySet()) {
        // Log.d("DEBUG", "\t" + entry.getKey() + " --> " + entry.getValue());
        // }
        //
        // Log.d("DEBUG", "Deleted Entry: ");
        // for (String entry : deletedFiles) {
        // Log.d("DEBUG", "\t" + entry);
        // }
    }

    private void startComposeThread() {
        if (srcApkPath != null) {
            this.composeThread = new ApkComposeThread(this, decodeRootPath,
                    srcApkPath, targetApkPath);
        }
        // srcApkPath == null, means currently is a full decoding
        else {
            this.composeThread = new ApkComposeThreadNew(this, decodeRootPath, targetApkPath);
        }

        if (extraMaker != null) {
            composeThread.setExtraMaker(extraMaker);
        }
        composeThread.setModification(this.stringModified,
                this.manifestModified, this.resFileModified,
                this.modifiedSmaliFolders, this.addedFiles, this.replacedFiles,
                this.deletedFiles, this.fileEntry2ZipEntry, this.signAPK);
        composeThread.setTaskCallback(this);
        composeThread.start();
    }

    private Map<String, String> getMapFromFile(String filepath) {
        Map<String, String> result = new HashMap<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filepath)));
            String line = br.readLine();
            while (line != null) {
                String key = line;
                String value = br.readLine();
                if (value != null) {
                    result.put(key, value);
                } else {
                    break;
                }

                line = br.readLine();
            }
        } catch (Exception e) {
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }
        return result;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void setTaskStepInfo(TaskStepInfo stepInfo) {
        synchronized (composeResult) {
            this.composeResult.curStep = stepInfo;
        }

        if (observer != null) {
            ITaskCallback ob = observer.get();
            if (ob != null) {
                ob.setTaskStepInfo(stepInfo);
            }
        }

        String desc = String.format(
                getResources().getString(R.string.step) + " %d/%d: %s",
                stepInfo.stepIndex, stepInfo.stepTotal,
                stepInfo.stepDescription);

        boolean forceShow = (stepInfo.stepIndex == stepInfo.stepTotal);
        updateNotification(forceShow, getString(R.string.build_ongoing), desc);
    }

    @Override
    public void setTaskProgress(float progress) {
        if (observer != null) {
            ITaskCallback ob = observer.get();
            if (ob != null) {
                ob.setTaskProgress(progress);
            }
        }
    }

    @Override
    public void taskSucceed() {
        synchronized (composeResult) {
            this.composeResult.finished = true;
            this.composeResult.ret = true;
            this.composeResult.failMessage = null;
        }

        updateNotification(true, getString(R.string.build_finished), getString(R.string.succeed));

        if (observer != null) {
            ITaskCallback ob = observer.get();
            if (ob != null) {
                ob.taskSucceed();
            }
        }
    }

    @Override
    public void taskFailed(String errMessage) {
        synchronized (composeResult) {
            this.composeResult.finished = true;
            this.composeResult.ret = false;
            this.composeResult.failMessage = errMessage;
        }

        updateNotification(true, getString(R.string.build_finished), getString(R.string.failed));

        if (observer != null) {
            ITaskCallback ob = observer.get();
            if (ob != null) {
                ob.taskFailed(errMessage);
            }
        }
    }

    @Override
    public void taskWarning(String message) {
        if (observer != null) {
            ITaskCallback ob = observer.get();
            if (ob != null) {
                ob.taskWarning(message);
            }
        }
    }

    private class MyHandler extends Handler {
        private String title;
        private String desc;

        public void setInfo(String title, String desc) {
            this.title = title;
            this.desc = desc;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (title != null) {
                        mNotifyBuilder.setContentTitle(title);
                    }
                    if (desc != null) {
                        mNotifyBuilder.setContentText(desc);
                    }

                    if (foregroundStarted) {
                        mNotificationManager.notify(
                                Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                                mNotifyBuilder.build());
                    } else {
                        startForeground(
                                Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                                mNotifyBuilder.build());
                        foregroundStarted = true;
                    }
                    lastUpdateTime = System.currentTimeMillis();
                    break;
            }
        }
    }

    public class ComposeResult {
        public boolean finished = false;
        public boolean ret;
        public String failMessage;
        public TaskStepInfo curStep;

        public void clear() {
            finished = false;
            failMessage = null;
            curStep = null;
        }
    }

    public class ComposeServiceBinder extends Binder {
        public void setObserver(ITaskCallback _observer) {
            observer = new WeakReference<>(_observer);
            synchronized (composeResult) {
                if (composeResult.finished) {
                    if (composeResult.ret) {
                        _observer.taskSucceed();
                    } else {
                        _observer.taskFailed(composeResult.failMessage);
                    }
                } else {
                    if (composeResult.curStep != null) {
                        _observer.setTaskStepInfo(composeResult.curStep);
                    }
                }
            }
        }

        public void buildAgain() {
            resetStatus();
            showNotification();
            startComposeThread();
        }

        // Stop the build thread
        public void stopBuilding() {
            if (composeThread != null && composeThread.isAlive()) {
                composeThread.stopRunning();
            }
            hideNotification();
        }

        public void setBuildHooker(IApkMaking extraMaker) {
            ApkComposeService.this.extraMaker = extraMaker;
        }

        // Get key/value maps
        public Map<String, Object> getValues() {
            Map<String, Object> ret = new HashMap<>();
            ret.put("srcApkPath", srcApkPath);
            ret.put("targetApkPath", targetApkPath);
            ret.put("decodeRootPath", decodeRootPath);
            boolean codeModified = (modifiedSmaliFolders != null && !modifiedSmaliFolders.isEmpty());
            ret.put("codeModified", codeModified);
            ret.put("signAPK", signAPK);
            return ret;
        }

        public void hideNotification() {
            if (mNotificationManager != null) {
                mNotificationManager.cancel(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE);
                if (foregroundStarted) {
                    stopForeground(true);
                    foregroundStarted = false;
                }
                mNotificationManager = null;
                Log.e("DEBUG", "notification hided.");
            }
        }

        public void showNotification() {
            ApkComposeService.this.showNotification();
        }

        // Build thread is still running
        public boolean isRunning() {
            return (composeThread != null && composeThread.isAlive());
        }
    }

}
