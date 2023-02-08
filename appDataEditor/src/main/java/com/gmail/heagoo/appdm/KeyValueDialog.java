package com.gmail.heagoo.appdm;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.appdm.base.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KeyValueDialog extends Dialog implements
        android.view.View.OnClickListener {

    private PrefDetailActivity activity;
    private int index;
    private List<ValueRecord> valueList = new ArrayList<>();
    private TextView typeTv;
    private EditText keyEdit;
    private EditText valueEdit;
    private View nextIv;
    private View prevIv;
    private boolean valueChanged = false;
    // Google version or not (Now all version are editable)
    //private boolean isGoogleVersion;
    private boolean editable;
    private int themeId;
    public KeyValueDialog(PrefDetailActivity activity,
                          Map<String, Object> values, int index, boolean editable, int themeId) {
        super(activity);
        this.activity = activity;
        this.index = index;
        //this.isGoogleVersion = isGoogleVersion;
        this.editable = editable;
        this.themeId = themeId;

        initData(values);
        initView();
    }

    @SuppressLint("InflateParams")
    private void initView() {
        LayoutInflater inflater = getLayoutInflater();
        int resId = R.layout.appdm_dialog_keyvalue;
        // sawsem theme
//		switch (themeId) {
//			case 1:
//				resId = R.layout.appdm_dialog_keyvalue_dark;
//				break;
//			case 2:
//				resId = R.layout.appdm_dialog_keyvalue_dark_ru;
//				break;
//		}
        View layout = inflater.inflate(resId, null);
        this.typeTv = (TextView) layout.findViewById(R.id.tv_type);
        this.keyEdit = (EditText) layout.findViewById(R.id.et_key);
        this.valueEdit = (EditText) layout.findViewById(R.id.et_valuey);
        this.nextIv = layout.findViewById(R.id.image_next);
        this.prevIv = layout.findViewById(R.id.image_prev);

        nextIv.setClickable(true);
        nextIv.setOnClickListener(this);
        prevIv.setClickable(true);
        prevIv.setOnClickListener(this);

        TextView tipTv = (TextView) layout.findViewById(R.id.tv_noteditable);
        final Button saveBtn = (Button) layout.findViewById(R.id.btn_save);
        if (!this.editable) {
            saveBtn.setVisibility(View.GONE);
            valueEdit.setEnabled(false);
            tipTv.setVisibility(View.VISIBLE);
        } else {
            saveBtn.setOnClickListener(this);
            tipTv.setVisibility(View.GONE);
        }
        Button cancelBtn = (Button) layout.findViewById(R.id.btn_cancel);
        cancelBtn.setOnClickListener(this);

        showItemByIndex(index);
        super.setContentView(layout);
        super.getWindow().setBackgroundDrawableResource(
                android.R.color.transparent);
        super.setCancelable(false);
        super.setCanceledOnTouchOutside(false);
    }

    // Save values to xml
    protected void saveValue() {
        try {
            ValueRecord record = valueList.get(index);
            String valueType = record.valueType;
            String strValue = valueEdit.getText().toString();
            Object newValue = null;

            if ("Integer".equals(valueType)) {
                newValue = Integer.valueOf(strValue);
            } else if ("Float".equals(valueType)) {
                newValue = Float.valueOf(strValue);
            } else if ("Long".equals(valueType)) {
                newValue = Long.valueOf(strValue);
            } else if ("String".equals(valueType)) {
                newValue = strValue;
            } else if ("Boolean".equals(valueType)) {
                newValue = Boolean.valueOf(strValue);
            } else {
                throw new Exception("Value type not supported!");
            }

            record.value = newValue.toString();

            activity.saveValue(record.key, newValue);

            Toast.makeText(activity, "Succeed!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            String msg = e.getMessage();
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void showItemByIndex(int idx) {
        ValueRecord rec = valueList.get(idx);
        typeTv.setText("Type: " + rec.valueType);
        keyEdit.setText(rec.key);
        valueEdit.setText(rec.value);
    }

    protected void showPrevItem() {
        if (index > 0) {
            showItemByIndex(index - 1);
            this.index = index - 1;
        } else {
            Toast.makeText(activity, "No more values!", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    protected void showNextItem() {
        if (index + 1 < valueList.size()) {
            showItemByIndex(index + 1);
            this.index = index + 1;
        } else {
            Toast.makeText(activity, "No more values!", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void initData(Map<String, Object> values) {
        for (String key : values.keySet()) {
            ValueRecord rec = new ValueRecord();
            rec.key = key;
            Object value = values.get(key);
            if (value != null) {
                rec.value = value.toString();
                rec.valueType = value.getClass().getSimpleName();
            } else {
                rec.value = "";
                rec.valueType = "null";
            }
            valueList.add(rec);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.image_next) {
            showNextItem();
        } else if (id == R.id.image_prev) {
            showPrevItem();
        } else if (id == R.id.btn_save) {
            saveValue();
            this.valueChanged = true;
        } else if (id == R.id.btn_cancel) {
            this.cancel();
            if (this.valueChanged) {
                activity.refresh();
            }
        }
    }

    public static class ValueRecord {
        String key;
        String value;
        String valueType;
    }

}
