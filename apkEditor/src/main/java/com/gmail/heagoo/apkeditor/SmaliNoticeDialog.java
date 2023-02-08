package com.gmail.heagoo.apkeditor;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.PreferenceUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SmaliNoticeDialog extends Dialog implements
        android.view.View.OnClickListener {

    private View view;
    private Context context;

    @SuppressLint("InflateParams")
    public SmaliNoticeDialog(Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
// sawsem theme
        this.context = context;
        int layoutId = R.layout.dlg_smali_license;
//		switch (GlobalConfig.instance(context).getThemeId()) {
//			case GlobalConfig.THEME_DARK_DEFAULT:
//				layoutId = R.layout.dlg_smali_license_dark;
//				break;
//			case GlobalConfig.THEME_DARK_RUSSIAN:
//				layoutId = R.layout.dlg_smali_license_dark_ru;
//				break;
//		}

        this.view = LayoutInflater.from(context).inflate(layoutId, null);
        setContentView(view);
        // getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        initView();
    }

    private void initView() {
        InputStream in = null;
        StringBuilder sb = new StringBuilder();
        try {
            in = context.getAssets().open("smali-NOTICE");
            InputStreamReader reader = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(reader);
            String line = br.readLine();
            while (line != null) {
                sb.append(line + "\n");
                line = br.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }

        TextView tv = (TextView) view.findViewById(R.id.content);
        tv.setText(sb.toString());

        Button closeBtn = (Button) view.findViewById(R.id.close_button);
        closeBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        CheckBox cb = (CheckBox) view.findViewById(R.id.cb_show_once);
        if (cb.isChecked()) {
            PreferenceUtil.setBoolean(context, "smali_license_showed", true);
        }
        this.dismiss();
    }

}
