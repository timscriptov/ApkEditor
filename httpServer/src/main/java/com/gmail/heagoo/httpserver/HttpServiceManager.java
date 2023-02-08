package com.gmail.heagoo.httpserver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.Toast;

import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.ZipUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

//import android.support.v7.app.AlertDialog;

public class HttpServiceManager {
    private static HttpServiceManager manager = null;

    private final List<MyServiceConnection> connections = new ArrayList<>();

    private HttpServiceManager() {
    }

    public static HttpServiceManager instance() {
        if (manager == null) {
            manager = new HttpServiceManager();
        }
        return manager;
    }

    public void startWebService(Activity activity, String projectDirectory) {
        HttpService.HttpServiceBinder binder = findBinder(activity);
        if (binder == null) {
            InputStream input = null;
            OutputStream output = null;
            File httpDir = new File(activity.getFilesDir(), "http_server");
            if (!httpDir.exists()) {
                try {
                    input = activity.getAssets().open("http.zip");
                    File tmpFile = new File(activity.getFilesDir(), "http.zip");
                    output = new FileOutputStream(tmpFile);
                    IOUtils.copy(input, output);
                    httpDir.mkdir();
                    ZipUtil.unzip(tmpFile.getPath(), httpDir.getPath());
                    tmpFile.delete();
                } catch (Exception e) {
                    Toast.makeText(activity, "Init Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                } finally {
                    IOUtils.closeQuietly(input);
                    IOUtils.closeQuietly(output);
                }
            }

            Intent intent = new Intent(activity, HttpService.class);
            ActivityUtil.attachParam(intent, "httpDirectory", httpDir.getPath());
            ActivityUtil.attachParam(intent, "projectDirectory", projectDirectory);
            activity.startService(intent);
            activity.bindService(intent, new MyServiceConnection(activity), Context.BIND_AUTO_CREATE);
        } else {
            binder.setProjectDirectory(projectDirectory);
            String strURL = binder.getURL();
            showDialog(activity, strURL);
        }
    }

    public void unbindService(Activity activity) {
        MyServiceConnection existConn = null;
        synchronized (connections) {
            for (MyServiceConnection conn : connections) {
                if (conn.activityRef.get() == activity) {
                    existConn = conn;
                    break;
                }
            }
        }
        if (existConn != null) {
            activity.unbindService(existConn);
            synchronized (connections) {
                connections.remove(existConn);
            }
        }
    }

    public void stopWebService(Activity activity) {
        Intent intent = new Intent(activity, HttpService.class);
        activity.stopService(intent);
    }

    private void showDialog(Activity activity, String strURL) {
        String format = activity.getString(R.string.web_server_started);
        String message = String.format(format, strURL);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.web_server)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    // Try to finder binder from existing connections
    private HttpService.HttpServiceBinder findBinder(Activity activity) {
        synchronized (connections) {
            for (MyServiceConnection conn : connections) {
                if (conn.activityRef.get() == activity) {
                    return conn.binder;
                }
            }
        }
        return null;
    }

    private class MyServiceConnection implements ServiceConnection {
        private WeakReference<Activity> activityRef;
        private HttpService.HttpServiceBinder binder;

        MyServiceConnection(Activity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (HttpService.HttpServiceBinder) service;
            synchronized (connections) {
                connections.add(this);
            }
            String strURL = binder.getURL();
            showDialog(activityRef.get(), strURL);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }
}
