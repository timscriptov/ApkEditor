package com.gmail.heagoo.apkeditor;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.base.R;

import java.lang.ref.WeakReference;
import java.util.Locale;

import brut.util.LanguageMapping;

public class LanguageSelectDialog extends Dialog implements
        android.view.View.OnClickListener {

    private WeakReference<ApkInfoActivity> activityRef;
    private String[] codes;
    private String[] languages;

    private View contentView;
    private EditText codeEt;

    private boolean isAutoTranslate;

    @SuppressLint("InflateParams")
    public LanguageSelectDialog(ApkInfoActivity activity, String[] _lang, String[] _codes) {
        super(activity);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        this.languages = _lang;
        this.codes = _codes;
        this.isAutoTranslate = (languages != null);

        // Check dark theme or not
        // sawsem theme
        int layoutId = R.layout.dlg_selectlanguage;
//		switch (GlobalConfig.instance(activity).getThemeId()) {
//			case GlobalConfig.THEME_DARK_DEFAULT:
//				layoutId = R.layout.dlg_selectlanguage_dark;
//				break;
//			case GlobalConfig.THEME_DARK_RUSSIAN:
//				layoutId = R.layout.dlg_selectlanguage_dark_ru;
//				break;
//		}
        this.activityRef = new WeakReference<ApkInfoActivity>(activity);
        this.contentView = activity.getLayoutInflater().inflate(
                layoutId, null, false);
        this.setContentView(contentView);

        this.codeEt = (EditText) contentView.findViewById(R.id.language_code);
        if (isAutoTranslate) { // Do not allow to modify
            codeEt.setEnabled(false);
        }

        initSpinner();
        initButton();
    }

    public void setTitle(int resId) {
        TextView titleTv = (TextView) contentView.findViewById(R.id.tv_title);
        titleTv.setText(resId);
    }

    private void initButton() {
        Button okBtn = (Button) contentView.findViewById(R.id.btn_addlang_ok);
        okBtn.setOnClickListener(this);

        Button cancelBtn = (Button) contentView
                .findViewById(R.id.btn_addlang_cancel);
        cancelBtn.setOnClickListener(this);
    }

    private void initSpinner() {
        int size = LanguageMapping.getSize();
        if (this.codes == null || this.languages == null) {
            this.codes = new String[size];
            this.languages = new String[size];
            LanguageMapping.getLanguages(codes, languages);
        }

        // Initialize spinner by setting adapter
        Spinner spinner = (Spinner) findViewById(R.id.language_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                activityRef.get(), android.R.layout.simple_spinner_item,
                languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Set the default value of language
        Locale locale = Locale.getDefault();
        String code = "-" + locale.getLanguage();
        int selected = getCodeIndex(code);
        if (selected != -1) {
            spinner.setSelection(selected);
        }

        // Event listener
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int position, long arg3) {
                updateLanguageCode(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

    }

    private int getCodeIndex(String code) {
        int idx = -1;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].startsWith(code)) {
                idx = i;
                break;
            }
        }
        return idx;
    }

    protected void updateLanguageCode(int position) {
        codeEt.setText(codes[position]);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_addlang_ok) {
            String strCode = codeEt.getText().toString();
            if (this.isAutoTranslate) {
                translateLanguage(strCode);
                this.dismiss();
            } else {
                if (addLanguage(strCode)) {
                    this.dismiss();
                }
            }
        } else if (id == R.id.btn_addlang_cancel) {
            this.dismiss();
        }
    }

    // Add a language
    private boolean addLanguage(String strCode) {
        ApkInfoActivity activity = activityRef.get();
        String error = activity.addLanguageRetError(strCode);
        if (error == null) {
            return true;
        } else {
            Toast.makeText(activity, error, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    // Translate to target language
    private void translateLanguage(String strCode) {
        ApkInfoActivity activity = activityRef.get();
        activity.translateLanguage(strCode);
    }
}
