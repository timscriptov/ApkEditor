package com.gmail.heagoo.apkeditor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.IOUtils;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class SmaliCodeDialog extends Dialog
        implements android.view.View.OnClickListener {
    static int[] smaliNameIds = {R.string.show_a_toast, R.string.log_a_message,
            R.string.dump_a_value, R.string.print_stack_trace};
    static String[] smaliCodes = {
            "    const-string v0, \"This is a toast.\"\n"
                    + "    # p0 (this object) must be an object of Context\n"
                    + "    invoke-static {p0, v0}, Lapkeditor/Utils;->showToast(Landroid/content/Context;Ljava/lang/String;)V",
            "    # use 'adb logcat APKEDITOR:* *:S' to view the log\n"
                    + "    const-string v0, \"I am here.\"\n"
                    + "    invoke-static {v0}, Lapkeditor/Utils;->log(Ljava/lang/String;)V",
            "    # use 'adb logcat APKEDITOR:* *:S' to view the value\n"
                    + "    invoke-static {v0}, Lapkeditor/Utils;->dumpValue(Ljava/lang/Object;)V",
            "    # use 'adb logcat APKEDITOR:* *:S' to view the stack trace\n"
                    + "    invoke-static {}, Lapkeditor/Utils;->printCallStack()V",
//            "    # 0x60 is the offset, change it to get a different IMEI\n" +
//                    "    const/16 v0, 0x60\n" +
//                    "    invoke-static {v0}, Lapkeditor/Utils;->generateImei(I)Ljava/lang/String;\n" +
//                    "    move-result-object v0",
    };
    boolean isPro;
    private WeakReference<Activity> activityRef;
    private String smaliRootFolder;
    private Spinner spinner;
    private EditText codeEt;

    // filePath: The path for current editing file
    public SmaliCodeDialog(Activity activity, String filePath) {
        super(activity);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        this.activityRef = new WeakReference<Activity>(activity);
        this.isPro = BuildConfig.IS_PRO;
        this.smaliRootFolder = getSmaliRootFolder(filePath);

        init(activity);

        // How to set the width, as too small if not
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        getWindow().setLayout((6 * width) / 7, LayoutParams.WRAP_CONTENT);
    }

    private String getSmaliRootFolder(String filePath) {
        String[] dirs = filePath.split("/");
        String smaliPath = "";
        for (String dir : dirs) {
            smaliPath += dir + "/";
            if ("smali".equals(dir) || dir.startsWith("smali_")) {
                break;
            }
        }
        return smaliPath;
    }

    @SuppressLint("InflateParams")
    private void init(final Activity activity) {

        View view = LayoutInflater.from(activity)
                .inflate(R.layout.dlg_smalicode, null);

        // Spinner
        this.spinner = (Spinner) view.findViewById(R.id.spinner_codename);
        String names[] = new String[smaliNameIds.length];
        for (int i = 0; i < smaliNameIds.length; ++i) {
            names[i] = activity.getString(smaliNameIds[i]);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Event listener
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int position, long arg3) {
                updateSmaliCode(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        // Code Content
        this.codeEt = (EditText) view.findViewById(R.id.et_samplecode);

        // Copy button
        Button copyBtn = (Button) view.findViewById(R.id.btn_copy);
        copyBtn.setOnClickListener(this);

        // Close button
        Button closeBtn = (Button) view.findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(this);

        setContentView(view);
    }

    protected void updateSmaliCode(int position) {
        if (position < smaliCodes.length) {
            codeEt.setText(smaliCodes[position]);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            this.dismiss();
        } else if (id == R.id.btn_copy) {
            // Copy to clipboard
            Activity activity = activityRef.get();
            ClipboardManager clipboard = (ClipboardManager) activity
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("code",
                    codeEt.getText().toString());
            clipboard.setPrimaryClip(clip);

            // Copy Utils.smali to some folder
            copyUtilSmali();

            // Toast
            Toast.makeText(activity, R.string.smali_copied, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    // Copy from assets to decode smali folder
    private void copyUtilSmali() {
        String dirPath = this.smaliRootFolder + "apkeditor";
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdir();
        }

        String filePath = dirPath + "/Utils.smali";
        File file = new File(filePath);
        if (!file.exists()) {
            FileOutputStream fos = null;
            InputStream is = null;
            try {
                fos = new FileOutputStream(file);
                AssetManager am = activityRef.get().getAssets();
                is = am.open("smali_patch/Utils.smali");
                IOUtils.copy(is, fos);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeQuietly(fos);
                closeQuietly(is);
            }
        }
    }

    private void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }
}
