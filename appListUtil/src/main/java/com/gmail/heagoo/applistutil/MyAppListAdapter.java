package com.gmail.heagoo.applistutil;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gmail.heagoo.util.applist.R;

import java.util.ArrayList;
import java.util.List;

public class MyAppListAdapter extends BaseAdapter {

    public List<AppInfo> appList = new ArrayList<AppInfo>();
    private Context ctx;
    private PackageManager pm;
    private IAppCustomize appCustomize;
    private boolean isDark;

    private LruCache<String, Drawable> pkgDrawables = new LruCache<String, Drawable>(
            32) {
        protected void entryRemoved(boolean evicted, String key,
                                    Drawable oldValue, Drawable newValue) {
            oldValue.setCallback(null);
        }
    };

    public MyAppListAdapter(Context ctx, IAppCustomize appInf, boolean isDark) {
        this.ctx = ctx;
        this.pm = ctx.getPackageManager();
        this.appCustomize = appInf;
        this.isDark = isDark;
    }

    public List<AppInfo> getAppList() {
        synchronized (this.appList) {
            List<AppInfo> retList = new ArrayList<AppInfo>();
            retList.addAll(appList);
            return retList;
        }
    }

    public void setAppList(List<AppInfo> appList) {
        synchronized (this.appList) {
            this.appList.clear();
            for (AppInfo info : appList) {
                this.appList.add(new AppInfo(info));
            }
        }
    }

    protected IAppCustomize getAppCustomize() {
        return this.appCustomize;
    }

    @Override
    public int getCount() {
        synchronized (this.appList) {
            return this.appList.size();
        }
    }

    @Override
    public Object getItem(int arg0) {
        synchronized (this.appList) {
            return this.appList.get(arg0);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final AppInfo appInfo = (AppInfo) getItem(position);
        if (appInfo == null) {
            return null;
        }

        ViewHolder viewHolder = null;
        // sawsem theme
        if (convertView == null) {
            int resId = R.layout.applistutil_app_item;
            convertView = LayoutInflater.from(ctx).inflate(resId, null);

            viewHolder = new ViewHolder();
            viewHolder.icon = (ImageView) convertView
                    .findViewById(R.id.app_icon);
            viewHolder.appName = (TextView) convertView
                    .findViewById(R.id.app_name);
            viewHolder.desc1 = (TextView) convertView
                    .findViewById(R.id.app_desc1);
            viewHolder.desc2 = (TextView) convertView
                    .findViewById(R.id.app_desc2);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        try {
            viewHolder.appName.setText(appInfo.appName);
            // if (appInfo.isSysApp)
            // viewHolder.appName.setTextColor(0xfff75343);
            // else
            // viewHolder.appName.setTextColor(0xff0028c6);

            appCustomize.customizeDetail1(appInfo, viewHolder.desc1);
            // viewHolder.desc1.setText(appCustomize.getDetail1(appInfo));

            Drawable icon = pkgDrawables.get(appInfo.packagePath);
            if (icon == null) {
                icon = appInfo.applicationInfo.loadIcon(pm);
                if (icon != null)
                    pkgDrawables.put(appInfo.packagePath, icon);
            }
            if (icon != null) {
                viewHolder.icon.setImageDrawable(icon);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // appCustomize.setupLongClickListener(convertView, appInfo);

        // appCustomize.setupClickListener(convertView, appInfo);

        return convertView;
    }

    static class ViewHolder {

        public ImageView icon;
        public TextView desc2;
        public TextView desc1;
        public TextView appName;

    }
}