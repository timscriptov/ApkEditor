package com.gmail.heagoo.apkeditor;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.base.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

// Control the resource navigation
public class ResNavigationMgr implements OnClickListener {

    private WeakReference<ApkInfoActivity> activityRef;
    private String rootDir;
    private String currDir;
    private LinearLayout viewContainer;
    private HorizontalScrollView scrollView;

    // All the added directory tabs
    private List<View> addedViewList = new ArrayList<>();

    public ResNavigationMgr(ApkInfoActivity ctx, String rootDir,
                            LinearLayout container, HorizontalScrollView scrollView) {
        this.activityRef = new WeakReference<ApkInfoActivity>(ctx);
        this.rootDir = rootDir;
        this.currDir = rootDir;
        this.viewContainer = container;
        this.scrollView = scrollView;
    }

    // Navigate to a new directory
    public void gotoDirectory(String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // Same path, nothing to do
        if (path.equals(currDir)) {
            return;
        }

        // Got to sub-directory
        if (path.startsWith(currDir + "/")) {
            String subPath = path.substring(currDir.length() + 1);
            String[] subDirs = subPath.split("/");
            for (String dir : subDirs) {
                String nextDir = currDir + "/" + dir;
                View tabView = createTab(nextDir);
                viewContainer.addView(tabView);
                addedViewList.add(tabView);
                currDir = nextDir;
            }
        }
        // Go to parent directory
        else if (currDir.startsWith(path + "/")) {
            String unusedPath = currDir.substring(path.length() + 1);
            String[] dirs = unusedPath.split("/");
            for (int i = dirs.length - 1; i >= 0; --i) {
                int position = addedViewList.size() - 1;
                View view = addedViewList.get(position);
                addedViewList.remove(position);
                viewContainer.removeView(view);
            }
            currDir = path;
        }
        // TODO: Go to other paths, not support yet
        else {
        }

        //scrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
        scrollView.postDelayed(new Runnable() {
            public void run() {
                scrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
            }
        }, 100L);
    }

    @SuppressLint("InflateParams")
    private View createTab(String path) {
        int resId = R.layout.item_navigation_dir;
        // sawsem theme
//        switch (GlobalConfig.instance(activityRef.get()).getThemeId()) {
//            case GlobalConfig.THEME_DARK_DEFAULT:
//                resId = R.layout.item_navigation_dir_dark;
//                break;
//            case GlobalConfig.THEME_DARK_RUSSIAN:
//                resId = R.layout.item_navigation_dir_dark_ru;
//                break;
//        }
        View view = LayoutInflater.from(activityRef.get()).inflate(resId, null);

        View tab = view.findViewById(R.id.menu_dirtab);
        tab.setTag(path);
        tab.setOnClickListener(this);

        TextView titleTv = (TextView) view.findViewById(R.id.dirname);
        titleTv.setText(getNameByPath(path));

        return view;
    }

    private String getNameByPath(String path) {
        int pos = path.lastIndexOf('/');
        return path.substring(pos + 1);
    }

    @Override
    public void onClick(View v) {
        String path = (String) v.getTag();
        if (path == null) {
            return;
        }

        String oldPath = currDir;
        gotoDirectory(path);

        // Also need to notify the list view to update
        if (activityRef.get() != null) {
            int upLevel = getUpLevel(oldPath, path);
            activityRef.get().resDirectoryChanged(path, upLevel);
        }
    }

    // Get how many level is up
    // The new path is in the parent path
    private int getUpLevel(String oldPath, String newPath) {
        if (oldPath.startsWith(newPath + "/")) {
            String unusedPath = oldPath.substring(newPath.length() + 1);
            String[] dirs = unusedPath.split("/");
            return dirs.length;
        }

        return 0;
    }

    // Notify us home is went (other component already got the change)
    public void gotoHome() {
        int i = addedViewList.size();
        for (; i > 0; --i) {
            int position = i - 1;
            View view = addedViewList.get(position);
            addedViewList.remove(position);
            viewContainer.removeView(view);
        }
        currDir = rootDir;
    }
}
