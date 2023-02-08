package com.gmail.heagoo.appdm;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gmail.heagoo.appdm.base.R;
import com.gmail.heagoo.appdm.util.StringPair;

import java.lang.ref.WeakReference;
import java.util.List;

public class NameAndPathAdapter extends BaseAdapter {

    private WeakReference<Activity> activityRef;
    private List<StringPair> data;
    private boolean isDark;

    public NameAndPathAdapter(Activity activity, List<StringPair> data,
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
        final StringPair info = (StringPair) getItem(position);
        if (info == null) {
            return null;
        }

        ViewHolder viewHolder = null;
        // sawsem theme
        if (convertView == null) {
            convertView = LayoutInflater.from(activityRef.get()).inflate(
                    (R.layout.appdm_item_nameandpath), null);

            viewHolder = new ViewHolder();
            viewHolder.firstTv = (TextView) convertView
                    .findViewById(R.id.tv_first);
            viewHolder.secondTv = (TextView) convertView
                    .findViewById(R.id.tv_second);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.firstTv.setText(info.first);
        viewHolder.secondTv.setText(info.second);

        return convertView;

    }

    static class ViewHolder {
        public TextView firstTv;
        public TextView secondTv;
    }
}
