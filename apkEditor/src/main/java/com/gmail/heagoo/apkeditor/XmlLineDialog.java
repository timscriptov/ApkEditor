package com.gmail.heagoo.apkeditor;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.base.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

public class XmlLineDialog extends Dialog implements
        android.view.View.OnClickListener {

    // Record all the value edit text
    List<EditText> valueEtList;
    // private Context ctx;
    private IXmlLineChanged lineChangeListener;
    private int lineIndex;
    private String tag;
    private LinkedHashMap<String, String> keyValues;
    private boolean selfClosed = false;
    // One line may contain several tags, content after first tag is put into
    // extraContent
    private String extraContent;
    private Context ctx;

    // Dialog to add a key/value
    private Dialog keyValueDlg;
    private View keyValueView;
    private LinearLayout keyValueLayout;

    private int itemLayoutId;

    public XmlLineDialog(Context ctx, IXmlLineChanged changeListener,
                         int lineIndex, String lineContent) {
        super(ctx);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        this.ctx = ctx;
        this.lineChangeListener = changeListener;
        this.lineIndex = lineIndex;

        // sawsem theme
        this.itemLayoutId = R.layout.item_stringvalue;

        int endPos = lineContent.indexOf('>');
        if (endPos != -1) {
            extraContent = lineContent.substring(endPos + 1);
            if (endPos != lineContent.length() - 1) {
                lineContent = lineContent.substring(0, endPos + 1);
            }
        }

        // Parse into detail information (tag, key/value, selfClosed)
        this.keyValues = new LinkedHashMap<String, String>();
        String[] words = lineContent.split(" ");
        tag = words[0].trim();
        if (tag.startsWith("<")) {
            tag = tag.substring(1);
        } else {
            tag = "";
        }
        for (int i = 1; i < words.length; i++) {
            String[] segs = words[i].split("=");
            if (segs.length == 2) {
                String value = trimValue(segs[1]);
                if (value != null) {
                    keyValues.put(segs[0], value);
                }
            }
        }
        selfClosed = lineContent.endsWith("/>");

        // Init dialog view
        // sawsem theme
        int layoutId = R.layout.dlg_xmlline;
//		switch (GlobalConfig.instance(ctx).getThemeId()) {
//			case GlobalConfig.THEME_DARK_DEFAULT:
//				layoutId = R.layout.dlg_xmlline_dark;
//				break;
//			case GlobalConfig.THEME_DARK_RUSSIAN:
//				layoutId = R.layout.dlg_xmlline_dark_ru;
//				break;
//		}
        View view = LayoutInflater.from(ctx).inflate(layoutId, null);
        TextView contentTv = (TextView) view.findViewById(R.id.tv_linecontent);
        contentTv.setText(lineContent);
        Button closeBtn = (Button) view.findViewById(R.id.btn_dlgclose);
        closeBtn.setOnClickListener(this);
        Button saveBtn = (Button) view.findViewById(R.id.btn_dlgsave);
        saveBtn.setOnClickListener(this);

        // Add key/values
        this.keyValueLayout = (LinearLayout) view
                .findViewById(R.id.view_keyvalue);
        valueEtList = new ArrayList<EditText>();
        if (keyValues.isEmpty()) {
            View v = new View(ctx);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, 100);
            v.setLayoutParams(lp);
            keyValueLayout.addView(v, 0);
            saveBtn.setVisibility(View.GONE);
        } else {
            int index = 0;
            for (Entry<String, String> entry : keyValues.entrySet()) {
                View child = LayoutInflater.from(ctx).inflate(itemLayoutId,
                        null);
                TextView tv = (TextView) child.findViewById(R.id.string_name);
                tv.setText(entry.getKey());
                EditText valueEt = (EditText) child
                        .findViewById(R.id.string_value);
                valueEtList.add(valueEt);
                valueEt.setText(entry.getValue());
                keyValueLayout.addView(child, index++);
            }

            // Add Image
            ImageView imageView = (ImageView) keyValueLayout
                    .findViewById(R.id.hidden_image);
            imageView.setVisibility(View.VISIBLE);
            imageView.setOnClickListener(this);
        }

        this.setContentView(view);
        // this.getWindow().setBackgroundDrawableResource(
        // android.R.color.transparent);
    }

    // Trim the comma, if it ends with >, also trim it
    private String trimValue(String strValue) {
        if (strValue.startsWith("\"")) {
            if (strValue.endsWith("\"")) {
                return strValue.substring(1, strValue.length() - 1);
            } else if (strValue.endsWith("\">")) {
                return strValue.substring(1, strValue.length() - 2);
            }
        }
        return null;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        // Close
        if (id == R.id.btn_dlgclose) {
            this.dismiss();
        }
        // Save
        else if (id == R.id.btn_dlgsave) {
            if (lineChangeListener != null) {
                lineChangeListener.xmlLineChanged(lineIndex, getLineData());
            }
            this.dismiss();
        }
        // To add a key/value to this line
        else if (id == R.id.hidden_image) {
            this.keyValueDlg = new Dialog(ctx);

// sawsem theme
            int resId = R.layout.dlg_addkeyvalue;
//			switch (GlobalConfig.instance(ctx).getThemeId()) {
//				case GlobalConfig.THEME_DARK_DEFAULT:
//					resId = R.layout.dlg_addkeyvalue_dark;
//					break;
//				case GlobalConfig.THEME_DARK_RUSSIAN:
//					resId = R.layout.dlg_addkeyvalue_dark_ru;
//					break;
//			}

            View view = LayoutInflater.from(ctx).inflate(resId, null);
            Button okBtn = (Button) view.findViewById(R.id.btn_addkeyvalue_ok);
            okBtn.setOnClickListener(this);
            Button cancelBtn = (Button) view
                    .findViewById(R.id.btn_addkeyvalue_cancel);
            cancelBtn.setOnClickListener(this);
            this.keyValueView = view;
            keyValueDlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
            keyValueDlg.setContentView(view);
            // keyValueDlg.getWindow().setBackgroundDrawableResource(
            // android.R.color.transparent);
            keyValueDlg.show();
        }
        // OK button clicked in key/value dialog
        else if (id == R.id.btn_addkeyvalue_ok) {
            EditText keyEt = (EditText) keyValueView.findViewById(R.id.key);
            EditText valueEt = (EditText) keyValueView.findViewById(R.id.value);
            String strKey = keyEt.getText().toString();
            strKey = strKey.trim();
            String strValue = valueEt.getText().toString();
            strValue = strValue.trim();
            if (strKey.equals("")) {
                Toast.makeText(ctx, R.string.empty_key_tip, Toast.LENGTH_SHORT)
                        .show();
            } else {
                // LOGGER.info("key=" + strKey + ", value=" + strValue);
                keyValues.put(strKey, strValue);

                View child = LayoutInflater.from(ctx).inflate(itemLayoutId,
                        null);
                TextView tv = (TextView) child.findViewById(R.id.string_name);
                tv.setText(strKey);
                EditText valueEdit = (EditText) child
                        .findViewById(R.id.string_value);
                valueEtList.add(valueEdit);
                valueEdit.setText(strValue);
                keyValueLayout.addView(child, keyValues.size() - 1);

                keyValueDlg.dismiss();
            }
        }
        // Cancel button clicked in key/value dialog
        else if (id == R.id.btn_addkeyvalue_cancel) {
            keyValueDlg.dismiss();
        }
    }

    // Get modified line from UI
    private String getLineData() {
        StringBuffer sb = new StringBuffer();
        sb.append("<" + tag);
        int index = 0;
        for (Entry<String, String> entry : keyValues.entrySet()) {
            String key = entry.getKey();
            EditText et = valueEtList.get(index);
            String newValue = et.getText().toString();
            sb.append(" " + key + "=\"" + newValue + "\"");
            index++;
        }
        if (selfClosed) {
            sb.append(" />");
        } else {
            sb.append(">");
        }

        sb.append(extraContent);

        return sb.toString();
    }

    // Callback
    public static interface IXmlLineChanged {
        public void xmlLineChanged(int lineIndex, String newLine);
    }

}
