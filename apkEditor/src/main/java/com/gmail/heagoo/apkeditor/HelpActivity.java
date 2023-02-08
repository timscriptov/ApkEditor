package com.gmail.heagoo.apkeditor;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.WebView;

import androidx.core.app.NotificationCompat;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.CustomizedLangActivity;

import java.util.Locale;

public class HelpActivity extends CustomizedLangActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        this.setContentView(R.layout.activity_help);

        WebView v = (WebView) this.findViewById(R.id.helpWeb);
        // String url = "file:///android_asset/oldhelp.htm";
        String url = "file:///android_res/raw/help.htm";

        Locale locale = Locale.getDefault();
        String language = locale.getLanguage();
        // if ("ru".equals(language)) {
        // url = "file:///android_asset/help-ru.htm";
        // } else if ("de".equals(language)) {
        // url = "file:///android_asset/help-de.htm";
        //// } else if ("fa".equals(language)) {
        //// url = "file:///android_asset/help-fa.htm";
        // } else if ("hu".equals(language)) {
        // url = "file:///android_asset/help-hu.htm";
        // } else if ("iw".equals(language)) {
        // url = "file:///android_asset/help-iw.htm";
        // } else if ("es".equals(language)) {
        // url = "file:///android_asset/help-es.htm";
        // } else if ("vi".equals(language)) {
        // url = "file:///android_asset/oldhelp-vi.htm";
        // }

        if ("de".equals(language) || "es".equals(language)
                || "hu".equals(language) || "iw".equals(language)) {
            url = "file:///android_res/raw/help.htm";
        }

        v.loadUrl(url);
    }

}


class Constants {
    public interface ACTION {
        public static String MAIN_ACTION = "com.gmail.heagoo.action.apkcompose";
        public static String STARTFOREGROUND_ACTION = "com.gmail.heagoo.action.startforeground";
        public static String STOPFOREGROUND_ACTION = "com.gmail.heagoo.action.stopforeground";
    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 8001;
    }
}

class ForegroundService extends Service {
    private static final String LOG_TAG = "ForegroundService";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            Log.i(LOG_TAG, "Received Start Foreground Intent ");
            Intent notificationIntent = new Intent(this, ApkComposeActivity.class);
            notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.drawable.apkeditor);

            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle("Truiton Music Player")
                    .setTicker("Truiton Music Player")
                    .setContentText("My Music")
                    //.setSmallIcon(R.drawable.apkeditor)
                    .setLargeIcon(
                            Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();
            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                    notification);
        } else if (intent.getAction().equals(
                Constants.ACTION.STOPFOREGROUND_ACTION)) {
            Log.i(LOG_TAG, "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "In onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case of bound services.
        return null;
    }
}
