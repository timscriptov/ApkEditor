package com.gmail.heagoo.sqliteutil;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.List;

public class TableRecordDialog extends Dialog {

    private WeakReference<SqliteRowViewActivity> activityRef;
    private int index;

    private String dbFilePath;
    private String tableName;
    private List<String> typeList;
    private List<String> nameList;
    private List<String> valueList;
    private List<String> pkFlagList;

    private TextView typeTv;
    private TextView pkFlagTv;
    private EditText nameEdit;
    private EditText valueEdit;
    private View nextIv;
    private View prevIv;
    private Button saveBtn;
    private Button cancelBtn;

    private boolean editable;
    private int themeId;

    public TableRecordDialog(SqliteRowViewActivity activity,
                             List<String> typeList, List<String> nameList,
                             List<String> pkFlagList, List<String> valueList, int index,
                             boolean editable, int themeId) {
        super(activity);
        this.activityRef = new WeakReference<>(activity);
        this.index = index;

        this.typeList = typeList;
        this.nameList = nameList;
        this.valueList = valueList;
        this.pkFlagList = pkFlagList;
        this.editable = editable;
        this.themeId = themeId;

        initView();
    }

    @SuppressLint("InflateParams")
    private void initView() {
        LayoutInflater inflater = getLayoutInflater();
        // sawsem theme
        int resId = R.layout.sql_dialog_tablerecord;
//		switch (resId) {
//			case 1:
//				resId = R.layout.sql_dialog_tablerecord_dark;
//				break;
//			case 2:
//				resId = R.layout.sql_dialog_tablerecord_dark_ru;
//				break;
//		}
        View layout = inflater.inflate(resId, null);
        this.typeTv = (TextView) layout.findViewById(R.id.tv_type);
        this.nameEdit = (EditText) layout.findViewById(R.id.et_name);
        this.valueEdit = (EditText) layout.findViewById(R.id.et_valuey);
        this.nextIv = layout.findViewById(R.id.image_next);
        this.prevIv = layout.findViewById(R.id.image_prev);
        this.pkFlagTv = (TextView) layout.findViewById(R.id.tv_pkflag);

        TextView tipTv = (TextView) layout.findViewById(R.id.tv_noteditable);
        if (!this.editable) {
            tipTv.setVisibility(View.VISIBLE);
            this.valueEdit.setEnabled(false);
        } else {
            tipTv.setVisibility(View.INVISIBLE);
        }

        nextIv.setClickable(true);
        nextIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNextItem();
            }
        });
        prevIv.setClickable(true);
        prevIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPrevItem();
            }
        });

        this.saveBtn = (Button) layout.findViewById(R.id.btn_save);
        if (!this.editable) {
            saveBtn.setVisibility(View.GONE);
        } else {
            saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // saveBtn.setEnabled(false);
                    saveValue();
                    // saveBtn.setEnabled(true);
                }
            });
        }
        this.cancelBtn = (Button) layout.findViewById(R.id.btn_cancel);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TableRecordDialog.this.cancel();
                // activity.refresh();
            }
        });

        showItemByIndex(index);
        super.setContentView(layout);
        super.getWindow().setBackgroundDrawableResource(
                android.R.color.transparent);
        super.setCancelable(false);
        super.setCanceledOnTouchOutside(false);
    }

    // Save values to DB
    @SuppressLint("DefaultLocale")
    protected void saveValue() {
        try {
            boolean isPK = isPrimaryKey(index);
            if (isPK) {
                throw new Exception("Can not edit primary key!");
            }

            String valueType = this.typeList.get(index);
            valueType = valueType.toUpperCase();
            String strValue = valueEdit.getText().toString();
            Object newValue = null;

            if (SqliteTableViewActivity.isStringType(valueType)) {
                newValue = strValue;
            } else if (SqliteTableViewActivity.isIntType(valueType)) {
                newValue = Long.valueOf(strValue);
            } else if (SqliteTableViewActivity.isBoolType(valueType)) {
                newValue = Boolean.valueOf(strValue);
            } else if (SqliteTableViewActivity.isFloatType(valueType)) {
                newValue = Float.valueOf(strValue);
            } else if (SqliteTableViewActivity.isDoubleType(valueType)) {
                newValue = Double.valueOf(strValue);
            } else if (SqliteTableViewActivity.isBlobType(valueType)) {
                throw new Exception("Value type not supported!");
            } else {
                newValue = strValue;
            }
            // if ("string".equals(valueType) || "text".equals(valueType)
            // || "varchar".equals(valueType)) {
            // } else if ("integer".equals(valueType) ||
            // "int".equals(valueType)) {
            // } else if ("long".equals(valueType)) {
            // } else if ("boolean".equals(valueType)) {
            // } else if ("float".equals(valueType)) {
            //
            // } else if ("double".equals(valueType)) {
            //
            // } else {
            //
            // }

            activityRef.get().saveValue(index, newValue);

            Toast.makeText(activityRef.get(), "Succeed!", Toast.LENGTH_SHORT)
                    .show();

        } catch (Exception e) {
            String msg = e.getMessage();
            Toast.makeText(activityRef.get(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isPrimaryKey(int idx) {
        boolean isPK = (!"0".equals(pkFlagList.get(idx)));
        return isPK;
    }

    private void showItemByIndex(int idx) {
        boolean isPK = isPrimaryKey(idx);
        typeTv.setText("Type: " + typeList.get(idx));
        pkFlagTv.setText("Primary Key: " + isPK);
        nameEdit.setText(nameList.get(idx));
        valueEdit.setText(valueList.get(idx));

        if (!editable) {
            if (isPK) {
                valueEdit.setEnabled(false);
                saveBtn.setVisibility(View.GONE);
            } else {
                valueEdit.setEnabled(true);
                saveBtn.setVisibility(View.VISIBLE);
            }
        }
    }

    protected void showPrevItem() {
        if (index > 0) {
            showItemByIndex(index - 1);
            this.index = index - 1;
        } else {
            Toast.makeText(activityRef.get(), "No more values!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    protected void showNextItem() {
        if (index + 1 < valueList.size()) {
            showItemByIndex(index + 1);
            this.index = index + 1;
        } else {
            Toast.makeText(activityRef.get(), "No more values!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void setTableInfo(String dbFilePath, String tableName) {
        this.dbFilePath = dbFilePath;
        this.tableName = tableName;
    }

}
