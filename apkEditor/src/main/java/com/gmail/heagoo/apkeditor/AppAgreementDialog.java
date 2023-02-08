package com.gmail.heagoo.apkeditor;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.gmail.heagoo.apkeditor.base.R;

import java.lang.ref.WeakReference;

public class AppAgreementDialog extends Dialog implements View.OnClickListener {
    private final WeakReference<MainActivity> activityRef;
    private EditText inputEt;

    public AppAgreementDialog(@NonNull MainActivity activity) {
        super(activity, R.style.Dialog_No_Border_2);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        this.activityRef = new WeakReference<>(activity);

        LayoutInflater inflater = LayoutInflater.from(activity);
        View layout = inflater.inflate(R.layout.dlg_app_license, null);
        initView(layout);
        super.setContentView(layout);
    }

    public static boolean appLicenseAccepted(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getBoolean("app_agreement_accepted", false);
    }

    private void initView(View layout) {
        TextView tv = (TextView) layout.findViewById(R.id.tv_content);
        tv.setText("Information in this dialog is provided in connection with APK Editor. No license, express or implied, by esptoppel or otherwise, to any intellectual property rights is granted by this.\n" +
                "\n" +
                "APK Editor is designed for Android fans who know what exactly they are doing, but not intended for hack, please use it under following terms:\n" +
                "\n" +
                "1) Please only modify the apk files which you have intellectual property rights.\n" +
                "\n" +
                "2) For apk files you don't have intellectual property rights, you need to ask for authorities from the developer to modify it. And even though you have rights to modify it, you still need to ask for re-distribution rights to publish it.\n" +
                "\n" +
                "3) \n" +
                "\n" +
                "4) To prevent abuse of APK Editor, sign feature is not provided any more.\n" +
                "\n" +
                "5) We may make changes to specifications and product descriptions at any time, without notice.");

        this.inputEt = (EditText) layout.findViewById(R.id.et_input);
        Button cancelBtn = (Button) layout.findViewById(R.id.btn_cancel);
        Button acceptBtn = (Button) layout.findViewById(R.id.btn_accept);
        cancelBtn.setOnClickListener(this);
        acceptBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_cancel) {
            activityRef.get().finish();
        } else if (id == R.id.btn_accept) {
            String input = this.inputEt.getText().toString();
            if (input != null && input.trim().toLowerCase().equals("accept")) {
                activityRef.get().initFileWithPermissionCheck();

                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activityRef.get());
                SharedPreferences.Editor e = sp.edit();
                e.putBoolean("app_agreement_accepted", true);
                e.commit();

                this.dismiss();
            } else {
                Toast.makeText(activityRef.get(), R.string.input_agree_toast, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
