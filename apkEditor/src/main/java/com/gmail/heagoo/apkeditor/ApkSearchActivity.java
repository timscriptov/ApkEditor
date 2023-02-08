package com.gmail.heagoo.apkeditor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.EditModeDialog.IEditModeSelected;
import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.se.SimpleEditActivity;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.CustomizedLangActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ApkSearchActivity extends CustomizedLangActivity implements OnItemClickListener,
        IEditModeSelected {

    //private boolean isDarkTheme;
    private String keyword;
    private String searchPath;
    private List<String> apkFileList = new ArrayList<>();

    private TextView titleTV;
    private View searchingLayout;
    private ApkListAdapter apkListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

//        switch (GlobalConfig.instance(this).getThemeId()) {
//            case GlobalConfig.THEME_DARK_DEFAULT:
//                super.setTheme(android.R.style.Theme_Black_NoTitleBar);
//                setContentView(R.layout.activity_apksearch_dark);
//                break;
//            case GlobalConfig.THEME_DARK_RUSSIAN:
//                super.setTheme(android.R.style.Theme_Black_NoTitleBar);
//                setContentView(R.layout.activity_apksearch_dark_ru);
//                break;
//            default:
        // sawsem theme
        setContentView(R.layout.activity_apksearch);
//                break;
//        }

        Intent intent = getIntent();
        this.keyword = ActivityUtil.getParam(intent, "Keyword");
        this.searchPath = ActivityUtil.getParam(intent, "Path");

        initView();

        // Start the searching thread
        new ApkSearchThread().start();
    }

    private void initView() {
        this.titleTV = (TextView) this.findViewById(R.id.title);
        this.searchingLayout = this.findViewById(R.id.searching_layout);
        ListView apkListView = (ListView) this.findViewById(R.id.listview_apkfiles);
        this.apkListAdapter = new ApkListAdapter(this);
        apkListView.setAdapter(apkListAdapter);
        apkListView.setOnItemClickListener(this);
        setTitleText(0);
    }

    // To notify the an apk file is found
    public void foundApkFile(final String apkPath) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                apkFileList.add(apkPath);
                setTitleText(apkFileList.size());
                apkListAdapter.addApkFile(apkPath);
            }
        });
    }

    private void setTitleText(int apkNum) {
        String title = String.format(
                getString(R.string.str_files_found), apkNum, keyword);
        if ("".equals(keyword)) { // Not show the last "- ''"
            title = title.substring(0, title.length() - 4);
        }
        titleTV.setText(title);
    }

    // To notify the searching is done
    public void apkSearchDone() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                searchingLayout.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        if (position < this.apkFileList.size()) {
            String filePath = this.apkFileList.get(position);
            if (BuildConfig.PARSER_ONLY) {
                UserAppActivity.startFullEditActivity(this, filePath);
            } else if (BuildConfig.LIMIT_NEW_VERSION && !MainActivity.upgradedFromOldVersion(this)) {
                startFullEditActivity(filePath);
            } else {
                EditModeDialog dlg = new EditModeDialog(this, this, filePath);
                dlg.show();
            }
        }
    }

    private void startFullEditActivity(String filePath) {
        UserAppActivity.startFullEditActivity(this, filePath);
    }

    @Override
    public void editModeSelected(int mode, String extraStr) {
        String filePath = extraStr;

        Intent intent = null;
        switch (mode) {
            case EditModeDialog.SIMPLE_EDIT:
                intent = new Intent(this, SimpleEditActivity.class);
                break;
            case EditModeDialog.FULL_EDIT:
                startFullEditActivity(filePath);
                return;
            case EditModeDialog.COMMON_EDIT:
                intent = new Intent(this, CommonEditActivity.class);
                break;
            case EditModeDialog.XML_FILE_EDIT:
                intent = new Intent(this, AxmlEditActivity.class);
                break;
        }

        if (intent != null) {
            ActivityUtil.attachParam(intent, "apkPath", filePath);
            startActivity(intent);
        }
    }

    class ApkSearchThread extends Thread {
        String lcKey;

        ApkSearchThread() {
            lcKey = keyword.toLowerCase();
        }

        @Override
        public void run() {
            searchInFolder(new File(searchPath));
            apkSearchDone();
        }

        private void searchInFolder(File curDir) {
            File[] files = curDir.listFiles();
            if (files != null) {
                List<File> dirList = new ArrayList<>();
                for (File f : files) {
                    if (f.isFile()) {
                        String fileName = f.getName();
                        if (fileName.endsWith(".apk")
                                && fileName.toLowerCase().contains(lcKey)) {
                            foundApkFile(f.getAbsolutePath());
                        }
                    } else { // Directory
                        dirList.add(f);
                    }
                }
                // Search in all sub directories
                for (File dir : dirList) {
                    searchInFolder(dir);
                }
            }
        }
    }
}
