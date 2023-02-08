package com.gmail.heagoo.apkeditor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.PathUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;


public class ImageDownloadActivity extends Activity implements ProcessingDialog.ProcessingInterface {
    final Handler handler = new Handler();
    private WebView webView;
    private String imageUrl;
    // Searched keyword
    private String keyword;

    // Download image to
    private String targetDir;
    ////////////////////////////////////////////////////////////////////////////////
    // For Processing Dialog
    private boolean downloadSucceed;
    private String downloadPath;

    // Extract the file name from URL
    private static String extractNameFromUrl(String url) {
        StringBuilder sb = new StringBuilder();
        int pos = url.lastIndexOf('/');
        for (int i = pos + 1; i < url.length(); ++i) {
            char c = url.charAt(i);
            if (c == '.') {
                break;
            }
            if (Character.isLetter(c) || Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.webView = new WebView(this);
        setContentView(webView);

        WebSettings ws = webView.getSettings();
        ws.setBuiltInZoomControls(true);// �������Ű�ť
        ws.setUseWideViewPort(true);// �������������
        ws.setLoadWithOverviewMode(true);// setUseWideViewPort��������webview�Ƽ�ʹ�õĴ��ڡ�setLoadWithOverviewMode����������webview���ص�ҳ���ģʽ��
        ws.setSavePassword(true);
        ws.setSaveFormData(true);
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        //ws.setSupportMultipleWindows(true);// �¼�

        Intent intent = getIntent();
        this.targetDir = intent.getStringExtra("Directory");

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                extractSearchKeyword(url);
                return false;
            }

            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                extractSearchKeyword(request.getUrl().toString());
                return false;
            }
        });

        //webView.loadUrl("https://www.google.com/webhp?tbm=isch");
        String url = null; //SettingActivity.getStartURL(this);
        if (url == null) {
            url = "https://icons8.com";
        }
        webView.loadUrl(url);

        showDelayedTip(R.string.download_image_tip1, 500, "download1");

        registerForContextMenu(webView);
    }

    private void showDelayedTip(final int messageId, long delay, final String tag) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean toShowTip = sp.getBoolean("show_tip_" + tag, true);

        if (toShowTip) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder db = new AlertDialog.Builder(ImageDownloadActivity.this);
                    db.setMessage(messageId);
                    db.setTitle(R.string.tip);
                    db.setPositiveButton(android.R.string.ok, null);
                    db.show();

                    // Make it show only once
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putBoolean("show_tip_" + tag, false);
                    editor.apply();
                }
            }, delay);
        }
    }

    // Extract keyword from URL like: https://www.google.co.uk/search?q=search+png&btnG=&dcr=0&tbm=isch
    private void extractSearchKeyword(String strUrl) {
        int position = strUrl.indexOf("search?q=");
        if (position != -1) {
            String keyword = strUrl.substring(position + 9);
            try {
                keyword = URLDecoder.decode(keyword, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            position = keyword.indexOf('&');
            if (position != -1) {
                keyword = keyword.substring(0, position);
            }
            String[] keys = keyword.split(" ");
            for (String key : keys) {
                if ("png".equals(key) || "jpg".equals(key)) {
                    continue;
                }
                this.keyword = key;
                break;
            }

            // When search URL detected, show download tip
            showDelayedTip(R.string.download_image_tip2, 500, "download2");
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        WebView.HitTestResult result = webView.getHitTestResult();

        MenuItem.OnMenuItemClickListener handler = new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                downloadImage();
                return true;
            }
        };

        int ID_SAVEIMAGE = 1;
        if (result.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                result.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            imageUrl = result.getExtra();
            menu.setHeaderTitle(getString(R.string.download) + " " + imageUrl);
            menu.add(0, ID_SAVEIMAGE, 0, R.string.save_image).setOnMenuItemClickListener(handler);
        }
    }

    private void downloadImage() {
        new ProcessingDialog(this, this, -1).show();
    }

    @Override
    public void process() throws Exception {
        downloadSucceed = false;
        boolean isPng = false;
        String tmpPath = targetDir + "/.download.img";

        InputStream input = null;
        FileOutputStream output = null;
        try {
            URL url = new URL(imageUrl);
            input = (InputStream) url.getContent();

            // To check the folder whether exist
            File tmpFile = new File(tmpPath);
            File dir = tmpFile.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }

            output = new FileOutputStream(tmpPath);

            byte pngHeader[] = {-119, 80, 78, 71, 13, 10, 26, 10};
            byte head[] = new byte[8];
            input.read(head);
            output.write(head);
            if (Arrays.equals(pngHeader, head)) {
                isPng = true;
            }

            IOUtils.copy(input, output);
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }

        // The file name either from searched keyword or from image URL
        String name;
        if (keyword != null) {
            name = keyword;
        } else {
            name = extractNameFromUrl(imageUrl);
        }

        // Rename it to a regular path
        String path = targetDir + "/" + name + (isPng ? ".png" : ".jpg");
        File targetFile = new File(path);
        if (targetFile.exists()) {
            targetFile = PathUtil.getTargetNonExistFile(path, false);
        }
        downloadPath = targetFile.getPath();
        boolean ret = new File(tmpPath).renameTo(targetFile);
        if (ret) {
            downloadSucceed = true;
        }
    }

    @Override
    public void afterProcess() {
        if (downloadSucceed) {
            String message = String.format(getString(R.string.image_saved_to), downloadPath);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }
}
