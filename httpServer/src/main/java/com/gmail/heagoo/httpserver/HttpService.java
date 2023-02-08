package com.gmail.heagoo.httpserver;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.gmail.heagoo.common.ActivityUtil;

public class HttpService extends Service {

    private String httpDirectory;
    private String projectDirectory;

    private HttpServer httpServer = null;
    private HttpServiceBinder binder = new HttpServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // When the service is restarted, intent = null
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId);
        }

        this.projectDirectory = ActivityUtil.getParam(intent, "projectDirectory");
        this.httpDirectory = ActivityUtil.getParam(intent, "httpDirectory");

        startHttpServer();

        return START_STICKY;
    }

    private void startHttpServer() {
        if (httpServer == null) {
            if (httpDirectory != null && projectDirectory != null) {
                httpServer = new HttpServer(httpDirectory, projectDirectory);
            } else {
                return;
            }
        }

        try {
            if (!httpServer.isAlive()) {
                for (int i = 0; i < 5; ++i) {
                    try {
                        httpServer.tryStart(i);
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            httpServer.stop();
        }
    }

    @Override
    public void onDestroy() {
        if (httpServer != null) {
            if (httpServer.isAlive()) {
                httpServer.stop();
            }
        }
        super.onDestroy();
    }

    public class HttpServiceBinder extends Binder {
        public void setProjectDirectory(String projectDirectory) {
            HttpService.this.projectDirectory = projectDirectory;
            if (httpServer != null) {
                httpServer.setProjectDirectory(projectDirectory);
            }
        }

        public void setHttpDirectory(String httpDirectory) {
            HttpService.this.httpDirectory = httpDirectory;
            if (httpServer != null) {
                httpServer.setHttpDirectory(httpDirectory);
            }
        }

        public String getURL() {
            if (httpServer != null && httpServer.isAlive()) {
                return httpServer.getServiceURL();
            }
            return null;
        }
    }
}
