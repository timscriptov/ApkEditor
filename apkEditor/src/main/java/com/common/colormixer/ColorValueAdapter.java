package com.common.colormixer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.GlobalConfig;
import com.gmail.heagoo.apkeditor.base.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ColorValueAdapter extends BaseAdapter {

    private WeakReference<Activity> activityRef;
    private List<ColorValue> values;
    private int themeId;

    public ColorValueAdapter(Activity activity, List<ColorValue> values) {
        this.activityRef = new WeakReference<Activity>(activity);
        this.values = new ArrayList<ColorValue>();
        this.values.addAll(values);
        this.themeId = GlobalConfig.instance(activity).getThemeId();
    }

    @Override
    public int getCount() {
        return values.size();
    }

    @Override
    public Object getItem(int position) {
        return values.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ColorValue colorVal = values.get(position);

        ViewHolder viewHolder = null;
        if (convertView == null) {
            int resId = R.layout.item_color_value;
            // sawsem theme
//            switch (themeId) {
//                case GlobalConfig.THEME_DARK_DEFAULT:
//                    resId = R.layout.item_color_value_dark;
//                    break;
//                case GlobalConfig.THEME_DARK_RUSSIAN:
//                    resId = R.layout.item_color_value_dark_ru;
//                    break;
//            }
            convertView = LayoutInflater.from(activityRef.get()).inflate(resId,
                    null);

            viewHolder = new ViewHolder();
            viewHolder.colorView = (View) convertView
                    .findViewById(R.id.color_view);
            viewHolder.nameTv = (TextView) convertView
                    .findViewById(R.id.tv_name);
            viewHolder.valueTv = (TextView) convertView
                    .findViewById(R.id.tv_value);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        try {
            viewHolder.nameTv.setText(colorVal.name);
            viewHolder.valueTv.setText(colorVal.strColorValue);
            if (colorVal.parsed) {
                viewHolder.colorView.setBackgroundColor(colorVal.intColorValue);
            } else {
                viewHolder.colorView.setBackgroundColor(0xffffffff);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return convertView;
    }

    public void updateData(ArrayList<ColorValue> colorValues) {
        this.values.clear();
        this.values.addAll(colorValues);
        this.notifyDataSetChanged();
    }

    private static final class ViewHolder {
        View colorView;
        TextView nameTv;
        TextView valueTv;
    }
}
