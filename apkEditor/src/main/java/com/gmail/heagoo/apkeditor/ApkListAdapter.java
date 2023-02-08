package com.gmail.heagoo.apkeditor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.ApkInfoParser;

import java.util.ArrayList;
import java.util.List;

public class ApkListAdapter extends BaseAdapter {

    List<String> fileList = new ArrayList<>();
    private Context ctx;
    private int themeId;
    private LruCache<String, ApkInfoParser.AppInfo> apkInfoCache = new LruCache<String, ApkInfoParser.AppInfo>(
            64) {
//		protected void entryRemoved(boolean evicted, String key,
//				ApkInfoParser.AppInfo oldValue, ApkInfoParser.AppInfo newValue) {
//			Drawable drawable = oldValue.icon;
//			if (drawable instanceof BitmapDrawable) {
//				BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
//				Bitmap bitmap = bitmapDrawable.getBitmap();
//				bitmap.recycle();
//			}
//		}
    };

    public ApkListAdapter(Context ctx) {
        this.ctx = ctx;
        this.themeId = GlobalConfig.instance(ctx).getThemeId();
    }

    @Override
    public int getCount() {
        return fileList.size();
    }

    @Override
    public Object getItem(int position) {
        return fileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String apkFilePath = fileList.get(position);
        ViewHolder viewHolder;
        if (convertView == null) {
// sawsem theme
            int resId = R.layout.item_file;
//			switch (themeId) {
//				case GlobalConfig.THEME_DARK_DEFAULT:
//					resId = R.layout.item_file_dark;
//					break;
//				case GlobalConfig.THEME_DARK_RUSSIAN:
//					resId = R.layout.item_file_dark_ru;
//					break;
//			}

            convertView = LayoutInflater.from(ctx).inflate(resId, null);

            viewHolder = new ViewHolder();
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.file_icon);
            viewHolder.filename = (TextView) convertView.findViewById(R.id.filename);
            viewHolder.desc1 = (TextView) convertView.findViewById(R.id.detail1);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Parse the apk icon, label
        ApkInfoParser.AppInfo apkInfo = apkInfoCache.get(apkFilePath);
        if (apkInfo == null) {
            try {
                apkInfo = new ApkInfoParser().parse(ctx, apkFilePath);
            } catch (Throwable ignored) {
            }
            // Cannot parse it
            if (apkInfo == null) {
                apkInfo = new ApkInfoParser.AppInfo();
                apkInfo.icon = ctx.getResources().getDrawable(R.drawable.apk_icon);
            }
        }
        apkInfoCache.put(apkFilePath, apkInfo);

        viewHolder.icon.setImageDrawable(apkInfo.icon);
        if (apkInfo.label != null) {
            viewHolder.filename.setText(apkInfo.label);
            viewHolder.desc1.setText(apkFilePath);
            viewHolder.desc1.setVisibility(View.VISIBLE);
        } else {
            viewHolder.filename.setText(apkFilePath);
            viewHolder.desc1.setVisibility(View.GONE);
        }

        return convertView;
    }

    // Add one apk file to the list
    public void addApkFile(String apkPath) {
        fileList.add(apkPath);
        this.notifyDataSetChanged();
    }

    private static class ViewHolder {
        ImageView icon;
        TextView filename;
        TextView desc1;
    }

}
