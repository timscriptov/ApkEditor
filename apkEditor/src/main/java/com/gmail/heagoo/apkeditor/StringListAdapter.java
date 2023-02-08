package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.base.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.types.StringItem;

class StringListAdapter extends BaseAdapter implements
        OnItemClickListener {
    private final List<StringItem> valueList = new ArrayList<>();
    int editingIndex = -1;
    private WeakReference<Activity> activityRef;
    // Record changed value
    private Map<String, Map<String, String>> changedValues = new HashMap<>();

    // Current configuration (which language)
    private String curConfig;

    private int layoutId;

    StringListAdapter(Activity activity) {
        this.activityRef = new WeakReference<>(activity);
        this.curConfig = null;

        this.layoutId = R.layout.item_stringvaluestatic;
        // sawsem theme
//		switch (GlobalConfig.instance(activity).getThemeId()) {
//			case GlobalConfig.THEME_DARK_DEFAULT:
//				layoutId = R.layout.item_stringvaluestatic_dark;
//				break;
//			case GlobalConfig.THEME_DARK_RUSSIAN:
//				layoutId = R.layout.item_stringvaluestatic_dark_ru;
//				break;
//		}
    }

    @Override
    public int getCount() {
        synchronized (valueList) {
            return valueList.size();
        }
    }

    @Override
    public Object getItem(int position) {
        synchronized (valueList) {
            return valueList.get(position);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        StringItem value;
        synchronized (valueList) {
            value = valueList.get(position);

            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(activityRef.get()).inflate(layoutId, null);

                viewHolder = new ViewHolder();
                viewHolder.name = (TextView) convertView
                        .findViewById(R.id.string_name);
                viewHolder.value = (TextView) convertView
                        .findViewById(R.id.string_value);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
                // viewHolder.value.removeTextChangedListener(viewHolder.watcher);
                // viewHolder.watcher = null;
            }

            viewHolder.name.setText(value.name);
            viewHolder.value.setText(value.value);

            // Add text change listener
            // viewHolder.watcher = new MyTextWatcher(this, position);
            // viewHolder.value.addTextChangedListener(viewHolder.watcher);

            // EditText editText = viewHolder.value;
            // editText.setOnTouchListener(new OnTouchListener() {
            // public boolean onTouch(View view, MotionEvent event) {
            // if (event.getAction() == MotionEvent.ACTION_UP) {
            // editingIndex = position;
            // }
            // return false;
            // }
            // });
            //
            // editText.clearFocus();
            // if (editingIndex != -1 && editingIndex == position) {
            // editText.requestFocus();
            // }
        }

        return convertView;
    }

    // Update a new display
    void updateData(String curConfig, List<StringItem> list) {
        synchronized (valueList) {
            this.curConfig = curConfig;
            valueList.clear();
            for (StringItem item : list) {
                valueList.add(item);
            }
        }

        this.notifyDataSetChanged();
    }

    // private static class MyTextWatcher implements TextWatcher {
    //
    // private WeakReference<StringListAdapter> adapterRef;
    // private int position = -1;
    //
    // public MyTextWatcher(StringListAdapter adapter, int position) {
    // adapterRef = new WeakReference<StringListAdapter>(adapter);
    // this.position = position;
    // }
    //
    // @Override
    // public void afterTextChanged(Editable s) {
    // String value = s.toString();
    // StringListAdapter adapter = adapterRef.get();
    // adapter.checkTextChange(position, value);
    // }
    //
    // @Override
    // public void beforeTextChanged(CharSequence s, int start, int count,
    // int after) {
    // }
    //
    // @Override
    // public void onTextChanged(CharSequence s, int start, int before,
    // int count) {
    // }
    //
    // }

    void checkTextChange(int position, String newValue) {
        boolean valueChanged = false;

        synchronized (valueList) {
            if (position >= 0 && position < valueList.size()) {
                StringItem item = valueList.get(position);
                if (!item.value.equals(newValue)) {
                    if (curConfig != null) {
                        // Here will change the original value (allStringValues in ApkInfoActivity)
                        item.value = newValue;

                        Map<String, String> valueMap = changedValues.get(curConfig);
                        if (valueMap == null) {
                            valueMap = new HashMap<>();
                            changedValues.put(curConfig, valueMap);
                        }
                        valueMap.put(item.name, newValue);
                    }

                    valueChanged = true;
                    // LOGGER.info(pair.m1 + " changed value: " + value);
                }
            }
        }

        if (valueChanged) {
            this.notifyDataSetChanged();
        }
    }

    Map<String, Map<String, String>> getChangedValues() {
        return changedValues;
    }

    void setChangedValues(Map<String, Map<String, String>> changedStringValues) {
        changedValues = changedStringValues;
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                            long id) {
        StringValueDialog dlg = new StringValueDialog(activityRef.get(), this, position);
        synchronized (valueList) {
            StringItem item = valueList.get(position);
            dlg.setKeyValue(item.name, item.value);
        }
        dlg.show();
    }

    private static class ViewHolder {
        public TextView name;
        // public EditText value;
        public TextView value;
        // public MyTextWatcher watcher;
    }
}
