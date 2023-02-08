package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection;
import com.gmail.heagoo.apkeditor.base.R;

import java.io.File;
import java.lang.ref.WeakReference;

public class KeySelectDlgHelper implements OnClickListener, IFileSelection {

    private View view;
    private Context context;
    private WeakReference<KeyListPreference> preferenceRef;
    private EditText pk8PathEt;
    private EditText x509PathEt;

    public KeySelectDlgHelper(KeyListPreference p) {
        this.preferenceRef = new WeakReference<KeyListPreference>(p);
    }

    public void showDialog(Context context) {
        this.context = context;

        LayoutInflater inflater = LayoutInflater.from(context);
        this.view = inflater.inflate(R.layout.dlg_keyselect, null, false);
        initView();

        AlertDialog.Builder db = new AlertDialog.Builder(context);
        db.setView(view);
        db.setTitle(R.string.custom_key_setting);
        db.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        setCustomKey();
                    }
                });
        db.setNegativeButton(android.R.string.cancel, null);

        db.show();
    }

    private void initView() {
        this.pk8PathEt = (EditText) view.findViewById(R.id.et_pk8path);
        this.x509PathEt = (EditText) view.findViewById(R.id.et_x509path);
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(context);
        pk8PathEt.setText(sp.getString(SettingActivity.STR_PRIVATEKEYPATH, ""));
        x509PathEt.setText(sp.getString(SettingActivity.STR_PUBLICKEYPATH, ""));

        ImageButton btn1 = (ImageButton) view.findViewById(R.id.btn_select_pk8);
        btn1.setOnClickListener(this);

        ImageButton btn2 = (ImageButton) view
                .findViewById(R.id.btn_select_x509);
        btn2.setOnClickListener(this);
    }

    protected void setCustomKey() {
        String privateKeyPath = pk8PathEt.getText().toString();
        String publicKeyPath = x509PathEt.getText().toString();

        // Check file path
        if ("".equals(privateKeyPath) || "".equals(publicKeyPath)) {
            Toast.makeText(context, R.string.error_filepath_empty,
                    Toast.LENGTH_LONG).show();
            return;
        } else {
            File f1 = new File(publicKeyPath);
            File f2 = new File(privateKeyPath);
            if (!f1.exists() || !f2.exists()) {
                Toast.makeText(context, R.string.error_filepath_notexist,
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        // To save key file path
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(context);
        Editor editor = sp.edit();
        editor.putString(SettingActivity.STR_PRIVATEKEYPATH, privateKeyPath);
        editor.putString(SettingActivity.STR_PUBLICKEYPATH, publicKeyPath);
        editor.commit();

        // Also save the preference value
        preferenceRef.get().setCustomValue();
    }

    // Click at the image button
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_select_pk8) {
            new FileSelectDialog(context, this, ".pk8", ".pk8",
                    context.getString(R.string.select_key_file)).show();
        } else if (id == R.id.btn_select_x509) {
            new FileSelectDialog(context, this, ".x509.pem", ".pem",
                    context.getString(R.string.select_key_file)).show();
        }
    }

    @Override
    public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
        // Private key selected
        if (".pk8".equals(extraStr)) {
            this.pk8PathEt.setText(filePath);
        }
        // Public key selected
        else {
            this.x509PathEt.setText(filePath);
        }
    }

    @Override
    public boolean isInterestedFile(String filename, String extraStr) {
        return (filename.endsWith(".pk8") || filename.endsWith(extraStr));
    }

    @Override
    public String getConfirmMessage(String filePath, String extraStr) {
        return null;
    }
}
