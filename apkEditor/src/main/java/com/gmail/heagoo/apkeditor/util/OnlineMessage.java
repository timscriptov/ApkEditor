package com.gmail.heagoo.apkeditor.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class OnlineMessage {
    private WeakReference<Activity> activityRef;

    public OnlineMessage(Activity activity) {
        this.activityRef = new WeakReference<>(activity);
        SharedPreferences sp = activity.getSharedPreferences("updates", 0);
        long lastTime = sp.getLong("last_pull", 0);
        if (System.currentTimeMillis() - lastTime > 24 * 3600 * 1000l) {
            new Thread() {
                @Override
                public void run() {
                    getOnlineMessage();
                    //emulateMessage();
                }
            }.start();
        }
    }

    private void emulateMessage() {
        SharedPreferences sp = activityRef.get().getSharedPreferences("updates", 0);

        // Save the message (save it only when message changed)
        String message = "<body><font color=red>HELLO</font></body>";
        String savedMsg = sp.getString("message", "");
        if (message.length() > 10 && !savedMsg.equals(message)) {
            SharedPreferences.Editor editor = sp.edit();

            int showNum = 3;
            editor.putInt("num", showNum);
            editor.putString("message", message);

            // Save the pull time
            editor.putLong("last_pull", System.currentTimeMillis());
            editor.commit();
        }
    }

    private void getOnlineMessage() {
        String urlStr = "http://www.apkeditorfree.com/updates/message.htm";
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            InputStream input = conn.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            String firstLine = in.readLine();

            String line = null;
            StringBuilder sb = new StringBuilder();
            while ((line = in.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

            SharedPreferences sp = activityRef.get().getSharedPreferences("updates", 0);

            // Save the message (save it only when message changed)
            String message = sb.toString();
            String savedMsg = sp.getString("message", "");
            if (message.length() > 10 && !savedMsg.equals(message)) {
                SharedPreferences.Editor editor = sp.edit();

                int showNum = Integer.valueOf(firstLine);
                editor.putInt("num", showNum);
                editor.putString("message", message);

                // Save the pull time
                editor.putLong("last_pull", System.currentTimeMillis());
                editor.commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showMessageDialog() {
        SharedPreferences sp = activityRef.get().getSharedPreferences("updates", 0);
        int num = sp.getInt("num", 0);
        String savedMsg = sp.getString("message", "");
        long lastShow = sp.getLong("last_show", 0);

        if ((System.currentTimeMillis() - lastShow > 24 * 3600 * 1000l) &&
                (num > 0) && !"".equals(savedMsg)) {
            AlertDialog.Builder alert = new AlertDialog.Builder(activityRef.get());
            alert.setTitle("Message");

            WebView wv = new WebView(activityRef.get());
            wv.setWebViewClient(new WebViewClient() {

                @SuppressWarnings("deprecation")
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    final Uri uri = Uri.parse(url);
                    return handleUri(uri);
                }

                @TargetApi(Build.VERSION_CODES.N)
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    final Uri uri = request.getUrl();
                    return handleUri(uri);
                }

                private boolean handleUri(final Uri uri) {
                    final String host = uri.getHost();
                    final String scheme = uri.getScheme();
                    // Based on some condition you need to determine if you are going to load the url
                    // in your web view itself or in a browser.
                    // You can use `host` or `scheme` or any part of the `uri` to decide.
//                    if (/* any condition */) {
//                        // Returning false means that you are going to load this url in the webView itself
//                        return false;
//                    } else
                    {
                        // Returning true means that you need to handle what to do with the url
                        // e.g. open web page in a Browser
                        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        activityRef.get().startActivity(intent);
                        return true;
                    }
                }
            });
            wv.loadData(savedMsg, "text/html", null);

            alert.setView(wv);
            alert.setPositiveButton(android.R.string.ok, null);
            alert.show();

            // Update the num
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt("num", num - 1);
            editor.putLong("last_show", System.currentTimeMillis());
            editor.commit();
        }
    }
}
