package com.gmail.heagoo.apkeditor.util;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.ClipboardUtil;

import java.lang.ref.WeakReference;

public class DebugDialog extends Dialog implements View.OnClickListener {
    private EditText logEt;
    private Button closeBtn;

    private WeakReference<Context> contextRef;
    private String strLog = "";

    public DebugDialog(Context context) {
        super(context);

        this.contextRef = new WeakReference<Context>(context);

        View layout = LayoutInflater.from(context).inflate(R.layout.dialog_debug, null);
        this.logEt = (EditText) layout.findViewById(R.id.et_log);
        this.closeBtn = (Button) layout.findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(this);
        layout.findViewById(R.id.btn_copy).setOnClickListener(this);

        this.setContentView(layout);
    }

    public void addLog(String line) {
        this.strLog += line + "\n";
        logEt.setText(strLog);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_copy) {
            ClipboardUtil.copyToClipboard(contextRef.get(), strLog);
        } else if (id == R.id.btn_close) {
            this.dismiss();
        }
    }
}
