package com.gmail.heagoo.apkeditor;

import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.WebView;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.CustomizedLangActivity;

public class EditorHelpActivity extends CustomizedLangActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        this.setContentView(R.layout.activity_help);

        WebView v = (WebView) this.findViewById(R.id.helpWeb);
        String url = "file:///android_res/raw/editor_help.htm";

        v.loadUrl(url);
    }

}
