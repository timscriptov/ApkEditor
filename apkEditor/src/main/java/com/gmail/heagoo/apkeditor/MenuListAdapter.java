package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.base.R;

import java.lang.ref.WeakReference;

// Used for main menu display
// Projects, Settings, About
public class MenuListAdapter extends BaseAdapter {
    public static final int ITEM_PROJECT = 0;
    public static final int ITEM_DONATE = 1;
    public static final int ITEM_SETTING = 2;
    public static final int ITEM_ABOUT = 3;
    public static final int ITEM_IMG_DOWNLOADER = 4;

    // Public version
//    private static final int[] titles = {R.string.projects, R.string.donate, R.string.settings, R.string.about};
//    private static final int[] drawables = {
//            R.drawable.ic_project, R.drawable.ic_donate, R.drawable.ic_setting, R.drawable.ic_about};
//    private static final int[] drawables_dark = {
//            R.drawable.ic_project_white, R.drawable.ic_donate, R.drawable.ic_setting_white, R.drawable.ic_about_white};
//    private static final int[] itemIds = {ITEM_PROJECT, ITEM_DONATE, ITEM_SETTING, ITEM_ABOUT};

    // Google play version
    private static final int[] titles = {R.string.projects, R.string.settings, R.string.image_downloader, R.string.about};
    private static final int[] drawables = {
            R.drawable.ic_project, R.drawable.ic_setting, R.drawable.image_download, R.drawable.ic_about};
    private static final int[] drawables_dark = {
            R.drawable.ic_project_white, R.drawable.ic_setting_white, R.drawable.image_download_white, R.drawable.ic_about_white};
    private static final int[] itemIds = {ITEM_PROJECT, ITEM_SETTING, ITEM_IMG_DOWNLOADER, ITEM_ABOUT};

    private WeakReference<Context> ctxRef;
    private boolean isDark;

    public MenuListAdapter(Context ctx, boolean isDark) {
        this.ctxRef = new WeakReference<>(ctx);
        this.isDark = isDark;
    }

    @Override
    public int getCount() {
        return titles.length;
    }

    @Override
    public Object getItem(int i) {
        return titles[i];
    }

    @Override
    public long getItemId(int i) {
        return itemIds[i];
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (convertView == null) {
            // sawsem theme
            int resId = R.layout.item_main_menu;
            convertView = LayoutInflater.from(ctxRef.get()).inflate(resId, null);

            viewHolder = new ViewHolder();
            viewHolder.iconIv = (ImageView) convertView.findViewById(R.id.menu_icon);
            viewHolder.titleTv = (TextView) convertView.findViewById(R.id.menu_title);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.iconIv.setImageResource(isDark ? drawables_dark[i] : drawables[i]);
        viewHolder.titleTv.setText(titles[i]);

        return convertView;
    }

    private class ViewHolder {
        public ImageView iconIv;
        public TextView titleTv;
    }
}
