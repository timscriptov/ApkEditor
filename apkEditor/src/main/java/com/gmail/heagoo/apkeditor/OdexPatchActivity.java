package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.util.OdexPatcher;
import com.gmail.heagoo.common.ApkInfoParser;

/**
 * Created by phe3 on 1/30/2018.
 */

public class OdexPatchActivity extends Activity implements View.OnClickListener, FileSelectDialog.IFileSelection {
    private EditText apkPathEt;
    private String apkPath;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.activity_odex_patch);

        initView();
    }

    private void initView() {
        this.apkPathEt = (EditText) findViewById(R.id.et_apkpath);

        Button selectBtn = (Button) findViewById(R.id.btn_select_apkpath);
        selectBtn.setOnClickListener(this);
        Button applyBtn = (Button) findViewById(R.id.btn_apply_patch);
        applyBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_select_apkpath) {
            FileSelectDialog dlg = new FileSelectDialog(this, this, ".apk", "", null);
            dlg.show();
        } else if (id == R.id.btn_apply_patch) {
            this.apkPath = apkPathEt.getText().toString();
            ProcessingDialog dlg = new ProcessingDialog(this, new PatchProcessor(), -1);
            dlg.show();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Processing Dialog

    @Override
    public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
        apkPathEt.setText(filePath);
    }

    ////////////////////////////////////////////////////////////////////////////////
    // File selection

    @Override
    public boolean isInterestedFile(String filename, String extraStr) {
        return filename.endsWith(".apk");
    }

    @Override
    public String getConfirmMessage(String filePath, String extraStr) {
        return null;
    }

    class PatchProcessor implements ProcessingDialog.ProcessingInterface {
        private String errMessage;
        private String odexPath;

        @Override
        public void process() throws Exception {
            ApkInfoParser parser = new ApkInfoParser();
            ApkInfoParser.AppInfo info = parser.parse(OdexPatchActivity.this, apkPath);
            if (info == null) {
                return;
            }

            String packageName = info.packageName;
            OdexPatcher patcher = new OdexPatcher(packageName);
            patcher.applyPatch(OdexPatchActivity.this, apkPath);

            odexPath = patcher.targetOdex;
            if (patcher.errMessage != null) {
                this.errMessage = patcher.errMessage;
                throw new Exception(errMessage);
            }
        }

        @Override
        public void afterProcess() {
            if (errMessage == null) {
                Toast.makeText(OdexPatchActivity.this, "Patched to " + odexPath, Toast.LENGTH_LONG).show();
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
}
