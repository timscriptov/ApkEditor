package com.gmail.heagoo.apkeditor;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.ClipboardUtil;

import java.lang.ref.WeakReference;

public class StringValueDialog extends Dialog
        implements android.view.View.OnClickListener {

    private WeakReference<Context> ctxRef;
    private StringListAdapter strListAdapter;
    private int position;

    private View view;
    private TextView keyTv;
    private EditText valueEt;

    @SuppressLint("InflateParams")
    public StringValueDialog(Context context, StringListAdapter strListAdapter,
                             int position) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        this.ctxRef = new WeakReference<Context>(context);
        this.strListAdapter = strListAdapter;
        this.position = position;
// sawsem theme
        int layoutId = R.layout.dlg_stringvalue;
//        switch (GlobalConfig.instance(context).getThemeId()) {
//            case GlobalConfig.THEME_DARK_DEFAULT:
//                layoutId = R.layout.dlg_stringvalue_dark;
//                break;
//            case GlobalConfig.THEME_DARK_RUSSIAN:
//                layoutId = R.layout.dlg_stringvalue_dark_ru;
//                break;
//        }
        this.view = LayoutInflater.from(context).inflate(layoutId, null);

        setContentView(view);

        // getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        this.keyTv = (TextView) view.findViewById(R.id.key);
        this.valueEt = (EditText) view.findViewById(R.id.value);
        View menu = view.findViewById(R.id.menu_clipboard);
        menu.setOnClickListener(this);

        Button okBtn = (Button) view.findViewById(R.id.btn_editstring_ok);
        okBtn.setOnClickListener(this);

        Button cancelBtn = (Button) view
                .findViewById(R.id.btn_editstring_cancel);
        cancelBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_editstring_ok) {
            String newValue = valueEt.getText().toString();
            strListAdapter.checkTextChange(position, newValue);
            this.dismiss();
        } else if (id == R.id.btn_editstring_cancel) {
            this.cancel();
        } else if (id == R.id.menu_clipboard) {
            Context ctx = ctxRef.get();
            String str = keyTv.getText().toString();
            ClipboardUtil.copyToClipboard(ctx, str);

            String msg = ctx.getString(R.string.copied_to_clipboard);
            msg = String.format(msg, str);
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
        }
    }

    public void setKeyValue(String key, String val) {
        keyTv.setText(key);
        valueEt.setText(val);
        valueEt.setSelection(val != null ? val.length() : 0);
    }
}
