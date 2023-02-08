package com.gmail.heagoo.apkeditor;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.base.R;

import java.lang.ref.WeakReference;

public class DecodeModeDialog extends Dialog implements OnClickListener {

    private WeakReference<Context> ctxRef;
    private View view;

    private IDecodeModeSelected callback;
    private String apkPath;
    private String packageName;

    public DecodeModeDialog(Context context, IDecodeModeSelected callback, String apkPath) {
        this(context, callback, apkPath, null);
    }

    @SuppressLint("InflateParams")
    public DecodeModeDialog(Context context, IDecodeModeSelected callback, String apkPath,
                            String packageName) {
        super(context);//, R.style.Dialog_No_Border);
        this.ctxRef = new WeakReference<>(context);
        this.callback = callback;
        this.apkPath = apkPath;
        this.packageName = packageName;

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        this.view = LayoutInflater.from(context).inflate(R.layout.dlg_decodemode, null);
        setContentView(view);

        initView();
    }

    private void initView() {
        TextView tv1 = (TextView) view.findViewById(R.id.decode_all_files);
        TextView tv2 = (TextView) view.findViewById(R.id.decode_partial_files);
        TextView tv3 = (TextView) view.findViewById(R.id.cancel);

        tv1.setOnClickListener(this);
        tv2.setOnClickListener(this);
        tv3.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.decode_all_files) {
            callback.decodeModeSelected(0, apkPath);
        } else if (id == R.id.decode_partial_files) {
            callback.decodeModeSelected(1, apkPath);
        }

        this.dismiss();
    }

    public static interface IDecodeModeSelected {
        public void decodeModeSelected(int mode, String extraStr);
    }

}
