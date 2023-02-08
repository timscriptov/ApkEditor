package com.gmail.heagoo.apkeditor.prj;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.ApkInfoExActivity;
import com.gmail.heagoo.apkeditor.GlobalConfig;
import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.ActivityUtil;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


class ProjectListAdapter extends BaseAdapter
        implements AdapterView.OnItemClickListener {
    private final List<ItemInfo> projectItems = new ArrayList<>();
    private int themeId;
    private WeakReference<ProjectListActivity> activityRef;
    private SimpleDateFormat formatter;

    ProjectListAdapter(ProjectListActivity activity, List<ItemInfo> items) {
        this.themeId = GlobalConfig.instance(activity).getThemeId();
        this.activityRef = new WeakReference<>(activity);
        for (ItemInfo item : items) {
            this.projectItems.add(item);
        }
    }

    int getProjectNum() {
        return projectItems.size();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        ItemInfo prjItem = projectItems.get(i);
        String errMessage = getErrorDescription(prjItem);
        if (errMessage != null) {
            Toast.makeText(activityRef.get(), errMessage, Toast.LENGTH_LONG).show();
        } else {
            Intent intent = new Intent(activityRef.get(), ApkInfoExActivity.class);
            ActivityUtil.attachParam(intent, "projectName", projectItems.get(i).name);
            activityRef.get().startActivity(intent);
        }
    }

    private String getErrorDescription(ItemInfo info) {
        String errMessage = null;
        if (!new File(info.decodeDirectory).exists()) {
            String fmt = activityRef.get().getString(R.string.prj_error_decode_dir_notfound);
            errMessage = String.format(fmt, info.decodeDirectory);
        }
        // For APK Parser, do not need to check the APK path
        if (!BuildConfig.PARSER_ONLY) {
            if (!new File(info.apkPath).exists()) {
                String fmt = activityRef.get().getString(R.string.prj_error_apk_notfound);
                errMessage = String.format(fmt, info.apkPath);
            }
        }
        return errMessage;
    }

    void setProjectIcon(Map<String, Drawable> icons) {
        for (Map.Entry<String, Drawable> entry : icons.entrySet()) {
            String path = entry.getKey();
            for (ItemInfo item : projectItems) {
                if (path.equals(item.apkPath)) {
                    item.appIcon = entry.getValue();
                }
            }
        }
    }

    // When remove a project, need to update it
    void updateData(List<ItemInfo> items) {
        Iterator<ItemInfo> it = this.projectItems.iterator();
        while (it.hasNext()) {
            if (!items.contains(it.next())) { // should remove
                it.remove();
            }
        }

        for (ItemInfo item : items) {
            if (!this.projectItems.contains(item)) { // should add it
                this.projectItems.add(item);
            }
        }
    }

    @Override
    public int getCount() {
        return projectItems.size();
    }

    @Override
    public Object getItem(int i) {
        return projectItems.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ItemInfo info = projectItems.get(i);

        ViewHolder holder;
        // sawsem theme
        if (view == null) {
            int resId = R.layout.item_applist;
            view = LayoutInflater.from(activityRef.get()).inflate(resId, null);
            holder = new ViewHolder();
            holder.icon = (ImageView) view.findViewById(R.id.app_icon);
            holder.title = (TextView) view.findViewById(R.id.app_name);
            holder.subTitle1 = (TextView) view.findViewById(R.id.app_desc1);
            holder.subTitle2 = (TextView) view.findViewById(R.id.app_desc2);
            holder.delMenu = view.findViewById(R.id.menu_delete);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        if (info.appIcon != null) {
            holder.icon.setImageDrawable(info.appIcon);
        }
        holder.title.setText(info.name);

        // Check apk path and decode path
        String errMessage = getErrorDescription(info);
        String subTitle1 = (errMessage != null ? errMessage : info.apkPath);
        if ("".equals(subTitle1)) {
            holder.subTitle1.setVisibility(View.GONE);
        } else {
            holder.subTitle1.setVisibility(View.VISIBLE);
            holder.subTitle1.setText(subTitle1);
        }
        if (errMessage != null) {
            if (holder.subTitle1.getTag() == null) {
                int color = holder.subTitle1.getCurrentTextColor();
                holder.subTitle1.setTag(color);
            }
            holder.subTitle1.setTextColor(themeId == GlobalConfig.THEME_LIGHT ? 0xffff4444 : 0xffcc0000);
        } else {
            Integer color = (Integer) holder.subTitle1.getTag();
            if (color != null) { // Set back the orginal color
                holder.subTitle1.setTextColor(color);
            }
        }

        holder.subTitle2.setText(getTimeDesc(info.lastModified));
        holder.subTitle2.setVisibility(View.VISIBLE);
        holder.delMenu.setVisibility(View.VISIBLE);
        holder.delMenu.setTag(i);
        holder.delMenu.setOnClickListener(activityRef.get());
        return view;
    }

    private String getTimeDesc(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        if (formatter == null) {
            formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
        return formatter.format(cal.getTime());
    }

    private static class ViewHolder {
        ImageView icon;
        TextView title;
        TextView subTitle1;
        TextView subTitle2;
        View delMenu;
    }

    static class ItemInfo {
        String name;
        String apkPath;
        String decodeDirectory;
        long lastModified;

        Drawable appIcon;

        ItemInfo(String name, String apkPath, String decodeDirectory, long mt) {
            this.name = name;
            this.apkPath = apkPath;
            this.decodeDirectory = decodeDirectory;
            this.lastModified = mt;
        }

        @Override
        public boolean equals(Object item) {
            return (item instanceof ItemInfo) && name.equals(((ItemInfo) item).name);
        }
    }
}
