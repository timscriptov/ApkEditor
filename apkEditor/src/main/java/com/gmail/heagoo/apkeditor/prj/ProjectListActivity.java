package com.gmail.heagoo.apkeditor.prj;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.ApkInfoActivity;
import com.gmail.heagoo.apkeditor.GlobalConfig;
import com.gmail.heagoo.apkeditor.ProcessingDialog;
import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.ApkInfoParser;
import com.gmail.heagoo.common.SDCard;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.types.ProjectInfo_V1;

public class ProjectListActivity extends Activity implements View.OnClickListener {
    private ProjectListAdapter adapter;
    private String projectFolder; // like "/sdcard/ApkEditor/.projects/"
    private List<ProjectListAdapter.ItemInfo> projectItems;

    private IconParseThread thread;
    private MyHandler handler = new MyHandler(this);

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.btn_close) {
            finish();
        } else if (id == R.id.menu_delete) {
            int i = (Integer) view.getTag();
            if (i < projectItems.size()) {
                ProjectListAdapter.ItemInfo item = projectItems.get(i);
                removeProject(item);
            }
        }
    }

    private void removeProject(ProjectListAdapter.ItemInfo item) {
        new ProcessingDialog(this, new ProjectRemover(this, item), -1).show();
    }

    void updateProjectList() {
        projectItems = listProjects(projectFolder);
        adapter.updateData(projectItems);
        adapter.notifyDataSetChanged();

        // Update Title
        TextView titleTv = (TextView) findViewById(R.id.tv_title);
        titleTv.setText(String.format(getString(R.string.project_num), adapter.getProjectNum()));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        int layoutId;
        // sawsem theme
//        switch (GlobalConfig.instance(this).getThemeId()) {
//            case GlobalConfig.THEME_DARK_DEFAULT:
//            case GlobalConfig.THEME_DARK_RUSSIAN:
//                layoutId = R.layout.activity_projectlist_dark;
//                super.setTheme(android.R.style.Theme_Black_NoTitleBar);
//                break;
//            default:
        layoutId = R.layout.activity_projectlist;
//                break;
//        }
        setContentView(layoutId);

        initUI();
    }

    @Override
    public void onDestroy() {
        if (thread != null && thread.isAlive()) {
            thread.stopParse();
        }
        super.onDestroy();
    }

    private void initUI() {
        ListView projectList = (ListView) findViewById(R.id.project_list);

        try {
            // For APK Parser, no extra project information
            if (BuildConfig.PARSER_ONLY) {
                this.projectFolder = SDCard.getRootDirectory() + "/ApkParser";
            } else {
                this.projectFolder = SDCard.makeDir(this, ".projects");
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        // Set list adapter
        projectItems = listProjects(projectFolder);
        adapter = new ProjectListAdapter(this, projectItems);
        projectList.setAdapter(adapter);
        projectList.setOnItemClickListener(adapter);
        thread = new IconParseThread();
        thread.start();

        // Title
        TextView titleTv = (TextView) findViewById(R.id.tv_title);
        titleTv.setText(String.format(getString(R.string.project_num), adapter.getProjectNum()));

        // Close current activity
        findViewById(R.id.btn_close).setOnClickListener(this);
    }

    protected List<ProjectListAdapter.ItemInfo> listProjects(String projectFolder) {
        File prjDir = new File(projectFolder);

        List<ProjectListAdapter.ItemInfo> items = new ArrayList<>();

        do {
            File[] files = prjDir.listFiles();
            if (files == null) {
                break;
            }

            for (File f : files) {
                if (f.isFile()) {
                    continue;
                }
                File prj = findProjectFile(f.listFiles());
                if (prj == null) {
                    continue;
                }

                ProjectInfo_V1 info = ApkInfoActivity.loadProject(f.getPath());
                if (info == null) {
                    continue;
                }

                items.add(new ProjectListAdapter.ItemInfo(f.getName(),
                        info.apkPath, info.decodeRootPath, prj.lastModified()));
            }
        } while (false);

        if (!items.isEmpty()) {
            Comparator<ProjectListAdapter.ItemInfo> comparator =
                    (arg0, arg1) -> arg0.lastModified < arg1.lastModified ? 1 : -1;
            Collections.sort(items, comparator);
        }

        return items;
    }

    // Look for ae.prj
    private File findProjectFile(File[] files) {
        if (files == null) {
            return null;
        }
        for (File f : files) {
            if (f.isFile() && f.getName().equals("ae.prj")) {
                return f;
            }
        }
        return null;
    }

    static class MyHandler extends Handler {
        private final Map<String, Drawable> icons = new HashMap<>();
        private WeakReference<ProjectListActivity> actRef;

        MyHandler(ProjectListActivity activity) {
            actRef = new WeakReference<>(activity);
        }

        void setIcon(String apkPath, Drawable drawable) {
            synchronized (icons) {
                icons.put(apkPath, drawable);
            }
            sendEmptyMessage(0);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    synchronized (icons) {
                        actRef.get().adapter.setProjectIcon(icons);
                    }
                    actRef.get().adapter.notifyDataSetChanged();
                    break;
            }
        }
    }

    class IconParseThread extends Thread {
        private boolean stopFlag = false;

        void stopParse() {
            stopFlag = true;
        }

        @Override
        public void run() {
            ApkInfoParser parser = new ApkInfoParser();
            int index = 0;
            while (!stopFlag && index < projectItems.size()) {
                ProjectListAdapter.ItemInfo item = projectItems.get(index);
                try {
                    ApkInfoParser.AppInfo info =
                            parser.parse(ProjectListActivity.this, item.apkPath);
                    handler.setIcon(item.apkPath, info.icon);
                } catch (Exception ignored) {
                }
                index += 1;
            }
        }
    }
}
