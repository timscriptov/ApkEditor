package com.gmail.heagoo.apkeditor;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.CustomizedLangActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MfSearchRetActivity extends CustomizedLangActivity implements OnClickListener {

    private String xmlPath;
    private ArrayList<Integer> lineIndexs;
    private ArrayList<String> lineContents;

    private ArrayList<EditText> editViews = new ArrayList<EditText>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        // sawsem theme
        int layoutId = R.layout.activity_mf_searchret;
//		switch (GlobalConfig.instance(this).getThemeId()) {
//			case GlobalConfig.THEME_DARK_DEFAULT:
//				setTheme(R.style.AppDarkTheme);
//				layoutId = R.layout.activity_mf_searchret_dark;
//				break;
//			case GlobalConfig.THEME_DARK_RUSSIAN:
//				setTheme(R.style.AppDarkTheme);
//				layoutId = R.layout.activity_mf_searchret_dark_ru;
//				break;
//		}
        this.setContentView(layoutId);

        Bundle bundle = getIntent().getExtras();
        this.xmlPath = bundle.getString("xmlPath");
        this.lineIndexs = bundle.getIntegerArrayList("lineIndexs");
        this.lineContents = bundle.getStringArrayList("lineContents");


        initView();
    }

    private void initView() {
        TextView titleTv = (TextView) this.findViewById(R.id.title);
        String format = getResources().getString(R.string.mf_search_ret);
        String title = String.format(format, lineIndexs.size());
        titleTv.setText(title);

        Button saveBtn = (Button) findViewById(R.id.btn_save);
        saveBtn.setOnClickListener(this);
        Button closeBtn = (Button) findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(this);

        LinearLayout layout = (LinearLayout) this
                .findViewById(R.id.result_layout);
        for (int i = 0; i < lineContents.size(); i++) {
            EditText et = new EditText(this);
            et.setText(lineContents.get(i));
            et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            layout.addView(et);

            editViews.add(et);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            this.finish();
        } else if (id == R.id.btn_save) {
            saveModification();
        }
    }

    private void saveModification() {
        boolean modified = false;

        // Collect the modification
        for (int i = 0; i < editViews.size(); i++) {
            EditText et = editViews.get(i);
            String newStr = et.getText().toString();
            String oldStr = lineContents.get(i);
            if (!oldStr.equals(newStr)) {
                lineContents.set(i, newStr);
                modified = true;
            }
        }

        if (modified) {
            if (saveManifest()) {
                Toast.makeText(this, R.string.succeed, Toast.LENGTH_SHORT)
                        .show();
                // To indicate the manifest is modified
                this.setResult(1);
                this.finish();
            }
        } else {
            Toast.makeText(this, R.string.no_change_detected,
                    Toast.LENGTH_SHORT).show();
        }
    }

    // The value is already collected before calling
    private boolean saveManifest() {
        boolean succeed = false;
        try {
            FileOutputStream fos = new FileOutputStream(xmlPath + ".tmp");
            FileInputStream fis = new FileInputStream(xmlPath);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            // Read all the contents
            List<String> allContents = new ArrayList<String>();
            String line = br.readLine();
            while (line != null) {
                allContents.add(line);
                line = br.readLine();
            }

            // Revise the content
            for (int i = 0; i < lineIndexs.size(); i++) {
                int lineIndex = lineIndexs.get(i) - 1;
                String newStr = lineContents.get(i);
                String oldStr = allContents.get(lineIndex);
                String head = getHeadPadding(oldStr);
                allContents.set(lineIndex, head + newStr.trim());
            }

            // Save the new content
            for (String lineStr : allContents) {
                fos.write(lineStr.getBytes());
                fos.write('\n');
            }

            br.close();
            fis.close();
            fos.close();

            // Move temp file to overwrite the origin file
            new File(xmlPath + ".tmp").renameTo(new File(xmlPath));

            succeed = true;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return succeed;
    }

    // Get the head blanks
    private String getHeadPadding(String str) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == ' ' || c == '\t') {
                sb.append(c);
            } else {
                break;
            }
        }
        return sb.toString();
    }
}
