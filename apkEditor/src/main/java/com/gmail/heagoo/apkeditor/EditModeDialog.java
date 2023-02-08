package com.gmail.heagoo.apkeditor;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;

import java.lang.ref.WeakReference;

public class EditModeDialog extends Dialog implements OnClickListener {

    public static final int FULL_EDIT = 0;
    public static final int SIMPLE_EDIT = 1;
    public static final int COMMON_EDIT = 2;
    public static final int DATA_EDIT = 3;
    public static final int XML_FILE_EDIT = 4;
    private static Boolean bX86;
    private WeakReference<Context> ctxRef;
    private View view;
    private IEditModeSelected callback;
    private String apkPath;
    private String packageName;
    private boolean isProVersion;

    public EditModeDialog(Context context, IEditModeSelected callback, String apkPath) {
        this(context, callback, apkPath, null);
    }

    @SuppressLint("InflateParams")
    public EditModeDialog(Context context, IEditModeSelected callback, String apkPath,
                          String packageName) {
        super(context);//, R.style.Dialog_No_Border);
        this.ctxRef = new WeakReference<>(context);
        this.callback = callback;
        this.apkPath = apkPath;
        this.packageName = packageName;

        this.isProVersion = BuildConfig.IS_PRO;

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        this.view = LayoutInflater.from(context).inflate(
                R.layout.dlg_editmode, null);
        setContentView(view);

        initView();
    }

    private static boolean isX86() {
        if (bX86 == null) {
            int isX86Val = MainActivity.isX86();
            bX86 = (isX86Val != 0);
        }
        return bX86;
    }

    private void initView() {
        TextView seTv = (TextView) view.findViewById(R.id.simple_edit);
        TextView feTv = (TextView) view.findViewById(R.id.full_edit);
        TextView ceTv = (TextView) view.findViewById(R.id.common_edit);
        TextView xmlTv = (TextView) view.findViewById(R.id.xml_edit);

        seTv.setOnClickListener(this);
        feTv.setOnClickListener(this);
        ceTv.setOnClickListener(this);
        xmlTv.setOnClickListener(this);

        // Only for pro version
        // Disable data edit now
        if (packageName != null && isProVersion) {
            View divideView = view.findViewById(R.id.data_edit_divide);
            divideView.setVisibility(View.VISIBLE);
            TextView deTv = (TextView) view.findViewById(R.id.data_edit);
            deTv.setVisibility(View.VISIBLE);
            deTv.setOnClickListener(this);
        }

        // Not supported system
        if (isX86() || Build.VERSION.SDK_INT < 21) {
            View xmlEditLayout = view.findViewById(R.id.xml_edit_layout);
            xmlEditLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        this.dismiss();

        if (id == R.id.simple_edit) {
            callback.editModeSelected(SIMPLE_EDIT, apkPath);
        } else if (id == R.id.full_edit) {
            callback.editModeSelected(FULL_EDIT, apkPath);
        } else if (id == R.id.common_edit) {
            callback.editModeSelected(COMMON_EDIT, apkPath);
        } else if (id == R.id.xml_edit) {
            if (BuildConfig.IS_PRO) {
                callback.editModeSelected(XML_FILE_EDIT, apkPath);
            } else {
                Context ctx = ctxRef.get();
                if (ctx != null) {
                    ManifestListAdapter.showPromoteDialog(ctxRef.get());
                }
            }
        } else if (id == R.id.data_edit) {
            callback.editModeSelected(DATA_EDIT, packageName);
        }
    }

    public static interface IEditModeSelected {
        public void editModeSelected(int mode, String extraStr);
    }

}
