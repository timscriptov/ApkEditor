package com.gmail.heagoo.apkeditor.util;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Build;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.RefInvoke;

import java.util.ArrayList;
import java.util.List;

public class IconPickerPreference extends ListPreference {

    private static final String KEY = "MyIcon";
    private Context context;
    private int[] iconResIds;
    private CharSequence[] iconNames; // android:entries
    private CharSequence[] iconValues; // android:entryValues
    // Make it as the original list preference, no icon
    // private ImageView icon;
    private List<IconItem> icons;
    private SharedPreferences preferences;
    private Resources resources;
    private String selectedIconValue, defaultIconValue;
    private TextView summary;
    public IconPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        resources = context.getResources();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        this.iconNames = resources.getStringArray(R.array.appicon_key);
        this.iconValues = resources.getStringArray(R.array.appicon_value);

        this.defaultIconValue = iconValues[0].toString();
        this.selectedIconValue = preferences.getString(KEY, defaultIconValue);

        this.iconResIds = (int[]) RefInvoke.invokeStaticMethod(
                "com.gmail.heagoo.seticon.SetIcon", "getAllIcons", null, null);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        if (Build.VERSION.SDK_INT < 21) {
            int padLeft = view.getPaddingLeft();
            float ratio = padLeft / 16.0f;
            view.setPadding((int) (6 * ratio), view.getPaddingTop(),
                    view.getPaddingRight(), view.getPaddingBottom());
        }

        TextView titleTv = (TextView) view.findViewById(R.id.title);
        titleTv.setText(R.string.launcher_icon);

        // Set summary as selected icon
        summary = (TextView) view.findViewById(R.id.summary);
        for (int i = 0; i < iconValues.length; i++) {
            if (this.selectedIconValue.equals(iconValues[i])) {
                summary.setText(this.iconNames[i]);
                break;
            }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (icons != null) {
            for (int i = 0; i < iconNames.length; i++) {
                IconItem item = icons.get(i);
                if (item.isChecked) {
                    // Save to shared preference
                    Editor editor = preferences.edit();
                    editor.putString(KEY, item.value);
                    editor.commit();

                    // Change summary
                    summary.setText(item.name);

                    // Change activity status
                    enableDisableActivity(item.value);

                    break;
                }
            }
        }

    }

    private void enableDisableActivity(String newIconValue) {
        // The icon is not changed
        if (this.selectedIconValue.equals(newIconValue)) {
            return;
        }

        this.selectedIconValue = newIconValue;

        RefInvoke.invokeStaticMethod("com.gmail.heagoo.seticon.SetIcon",
                "setIcon", new Class<?>[]{Activity.class, String.class},
                new Object[]{(Activity) context, newIconValue});

        Toast.makeText(context, R.string.icon_changed_tip, Toast.LENGTH_LONG)
                .show();
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {

        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(null, null);

        iconNames = getEntries();
        iconValues = getEntryValues();

        if (iconNames == null || iconValues == null
                || iconNames.length != iconValues.length) {
            throw new IllegalStateException("Invalid arguments.");
        }

        icons = new ArrayList<IconItem>();
        for (int i = 0; i < iconNames.length; i++) {
            boolean isSelected = selectedIconValue.equals(iconValues[i]) ? true
                    : false;
            IconItem item = new IconItem(iconNames[i], iconValues[i],
                    iconResIds[i], isSelected);
            icons.add(item);
        }
// sawsem theme
        int resId = R.layout.item_iconpicker;
        CustomListPreferenceAdapter customListPreferenceAdapter = new CustomListPreferenceAdapter(
                context, resId, icons);
        builder.setAdapter(customListPreferenceAdapter, null);

    }

    private static class IconItem {

        private int iconResId;
        private boolean isChecked;
        private String name;
        private String value;

        public IconItem(CharSequence name, CharSequence value, int iconResId,
                        boolean isChecked) {
            this.name = name.toString();
            this.value = value.toString();
            this.iconResId = iconResId;
            this.isChecked = isChecked;
        }

    }

    private static class ViewHolder {
        protected ImageView iconImage;
        protected TextView iconName;
        protected RadioButton radioButton;
    }

    private class CustomListPreferenceAdapter extends ArrayAdapter<IconItem> {

        private Context context;
        private List<IconItem> icons;
        private int resource;

        public CustomListPreferenceAdapter(Context context, int resource,
                                           List<IconItem> objects) {
            super(context, resource, objects);
            this.context = context;
            this.resource = resource;
            this.icons = objects;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            ViewHolder holder;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(resource, parent, false);

                holder = new ViewHolder();
                holder.iconName = (TextView) convertView.findViewById(R.id.iconName);
                holder.iconImage = (ImageView) convertView.findViewById(R.id.iconImage);
                holder.radioButton = (RadioButton) convertView.findViewById(R.id.iconRadio);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            IconItem curItem = icons.get(position);
            holder.iconName.setText(curItem.name);
            holder.iconImage.setImageResource(curItem.iconResId);
            holder.radioButton.setChecked(curItem.isChecked);

            convertView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // ViewHolder holder = (ViewHolder) v.getTag();
                    for (int i = 0; i < icons.size(); i++) {
                        if (i == position)
                            icons.get(i).isChecked = true;
                        else
                            icons.get(i).isChecked = false;
                    }
                    getDialog().dismiss();
                }
            });

            return convertView;
        }

    }
}