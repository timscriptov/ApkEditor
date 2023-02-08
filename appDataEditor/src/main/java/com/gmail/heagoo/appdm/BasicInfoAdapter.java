package com.gmail.heagoo.appdm;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.gmail.heagoo.appdm.base.R;

import java.lang.ref.WeakReference;
import java.util.List;

public class BasicInfoAdapter extends BaseAdapter {

    private WeakReference<Activity> activityRef;
    private List<BasicInfoItem> data;
    private boolean isDark;

    public BasicInfoAdapter(Activity activity, List<BasicInfoItem> data,
                            boolean isDark) {
        this.activityRef = new WeakReference<Activity>(activity);
        this.data = data;
        this.isDark = isDark;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final BasicInfoItem info = (BasicInfoItem) getItem(position);
        if (info == null) {
            return null;
        }

        ViewHolder viewHolder = null;
        // sawsem theme
        if (convertView == null) {
            convertView = LayoutInflater.from(activityRef.get()).inflate(
                    (R.layout.appdm_item_basicinfo), null);

            viewHolder = new ViewHolder();
            viewHolder.titleTv = (TextView) convertView
                    .findViewById(R.id.tv_title);
            viewHolder.valueTv = (TextView) convertView
                    .findViewById(R.id.tv_value);
            viewHolder.btn = (Button) convertView
                    .findViewById(R.id.btn_operation);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.titleTv.setText(info.title);
        viewHolder.valueTv.setText(info.value);
        if (info.listener != null) {
            viewHolder.btn.setVisibility(View.VISIBLE);
            viewHolder.btn.setText(info.opName);
            viewHolder.btn.setOnClickListener(info.listener);
        } else {
            viewHolder.btn.setVisibility(View.GONE);
        }

        return convertView;

    }

    static class ViewHolder {
        public TextView titleTv;
        public TextView valueTv;
        public Button btn;
    }
}
