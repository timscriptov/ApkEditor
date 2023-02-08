package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Button;

import com.gmail.heagoo.apkeditor.base.R;

public class AboutPluginDialog extends Dialog implements
        android.view.View.OnClickListener {

    public AboutPluginDialog(Activity activity) {
        super(activity);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        init(activity);

        // How to set the width, as too small if not
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        getWindow().setLayout((7 * width) / 8, LayoutParams.WRAP_CONTENT);
    }

    private void init(Context context) {

        boolean isDark = GlobalConfig.instance(context).isDarkTheme();
        int resId = R.layout.dlg_about_translate_plugin;
        View view = LayoutInflater.from(context).inflate(resId, null);
        // sawsem theme
        Button btn = (Button) view.findViewById(R.id.btn_close);
        btn.setOnClickListener(this);

        WebView webView = (WebView) view.findViewById(R.id.web_instructions);
        webView.loadUrl("file:///android_res/raw/about_translate_plugin.htm");

        this.setContentView(view);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            this.dismiss();
        }
    }

}
