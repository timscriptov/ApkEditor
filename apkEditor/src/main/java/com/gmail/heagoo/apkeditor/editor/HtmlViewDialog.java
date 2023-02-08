package com.gmail.heagoo.apkeditor.editor;

import android.app.Activity;
import android.app.Dialog;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.GlobalConfig;
import com.gmail.heagoo.apkeditor.SmaliMethodWindowHelper;
import com.gmail.heagoo.apkeditor.TextEditNormalActivity;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.util.Smali2Html;
import com.gmail.heagoo.apkeditor.util.ValuesXml2Html;
import com.gmail.heagoo.apkeditor.util.Xml2Html;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.TextFileReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class HtmlViewDialog extends Dialog implements
        View.OnClickListener, SmaliMethodWindowHelper.ISmaliMethodClicked {

    private final MyHandler handler = new MyHandler(this);
    private final SmaliMethodWindowHelper popupWindowHelper = new SmaliMethodWindowHelper(this);
    private TextView filenameTv;
    private View methodMenu;
    private WebView webView;
    private String filePath; // Text file path
    private File htmlFile;
    private WeakReference<Activity> activityRef;
    public HtmlViewDialog(Activity activity) {
        super(activity);
        this.activityRef = new WeakReference<>(activity);
        // Full screen
        if (GlobalConfig.instance(activity).isFullScreen()) {
            Window win = getWindow();
            if (win != null) {
                win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }
        setContentView(R.layout.dlg_htmlview);
        init();
    }

    @Override
    public void gotoLine(int lineNO) {
        loadHtml(lineNO);
    }

    private void init() {
        this.filenameTv = (TextView) this.findViewById(R.id.filename);
        this.methodMenu = this.findViewById(R.id.menu_methods);
        this.webView = (WebView) this.findViewById(R.id.webView);
        View editorBtn = this.findViewById(R.id.editorBtn);

        methodMenu.setOnClickListener(this);
        editorBtn.setOnClickListener(this);
    }

    // lineNO start at 0
    private void loadHtml(int lineNO) {
        if (htmlFile != null) {
            String append = "";
            if (lineNO > 0) {
                append = "#line" + lineNO;
            }
            webView.loadUrl("file://" + htmlFile.getAbsolutePath() + append);
        }
    }

    private void convert2Html(final String filePath) {
        new Thread() {
            public void run() {
                boolean ret = false;
                if (TextEditNormalActivity.isXml(filePath)) {
                    ret = convertXml2Html(filePath);
                } else if (TextEditNormalActivity.isSmali(filePath)) {
                    ret = convertSmali2Html(filePath);
                }
                if (ret) {
                    handler.sendEmptyMessage(MyHandler.HTML_LOADED);
                }
            }
        }.start();
    }

    // Convert the file to html format
    private boolean convertXml2Html(String filePath) {
        ArrayList<String> xmlLines = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String data;
            while ((data = br.readLine()) != null) {
                xmlLines.add(data);
                // sb.append(data);
                // sb.append("\n");
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        // Transform to html
        copyCssFile();
        File rootFile = activityRef.get().getFilesDir();
        File htmlFile = new File(rootFile, ".html");
        try {
            if (TextEditNormalActivity.isValuesXml(filePath)) {
                new ValuesXml2Html().transform(xmlLines,
                        htmlFile.getAbsolutePath());
            } else {
                Xml2Html.transform(xmlLines, htmlFile.getAbsolutePath());
            }
            this.htmlFile = htmlFile;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    // Smali -> html
    private boolean convertSmali2Html(String filePath) {
        Smali2Html sh = new Smali2Html(filePath);
        File rootFile = activityRef.get().getFilesDir();
        File htmlFile = new File(rootFile, ".html");
        try {
            sh.transformTo(htmlFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        this.htmlFile = htmlFile;
        return true;
    }

    // If viewsource.css not copied, copy it
    private void copyCssFile() {
        try {
            File rootFile = activityRef.get().getFilesDir();
            File cssFile = new File(rootFile, "viewsource.css");
            if (!cssFile.exists()) {
                InputStream input = activityRef.get().getAssets().open("viewsource.css");
                FileOutputStream fos = new FileOutputStream(cssFile);
                IOUtils.copy(input, fos);
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.menu_methods) {
            showPopWindow(view);
        } else if (id == R.id.editorBtn) {
            dismiss();
        }
    }

    public void show(String filePath, String displayName) {
        if (!filePath.equals(this.filePath)) {
            this.filePath = filePath;
            filenameTv.setText(displayName);
            if (TextEditNormalActivity.isSmali(filePath)) {
                methodMenu.setVisibility(View.VISIBLE);
            } else {
                methodMenu.setVisibility(View.GONE);
            }
            convert2Html(filePath);
        }
        show();
    }

    private void showPopWindow(View parent) {
        // The popup window is initialized
        if (popupWindowHelper != null && popupWindowHelper.getFile() != null) {
            popupWindowHelper.doPopWindowShow(parent);
        } else {
            try {
                String content = new TextFileReader(filePath).getContents();
                popupWindowHelper.asyncShowPopup(activityRef.get(), filePath, content, parent);
            } catch (Exception ignored) {
            }
        }
    }

    private static class MyHandler extends Handler {
        private static final int HTML_LOADED = 0;

        private WeakReference<HtmlViewDialog> dlgRef;

        MyHandler(HtmlViewDialog dlg) {
            dlgRef = new WeakReference<>(dlg);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HTML_LOADED:
                    if (dlgRef.get() != null) {
                        dlgRef.get().loadHtml(-1);
                    }
                    break;
            }
        }
    }
}
