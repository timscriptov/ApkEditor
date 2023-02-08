package com.gmail.heagoo.applistutil;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.util.applist.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class BaseAppActivity extends CustomizedLangActivity implements
        IConsumeSearch, OnItemClickListener {

    private IAppSearch searcher;
    private AppSearchThread searchThread;
    private MyAppListAdapter adapter;

    private ListView appListView;
    private LinearLayout searchLayout;

    private List<AppInfo> displayAppList = new ArrayList<AppInfo>();

    // Search support
    private EditText keyworkEdit;
    private Button findBtn;
    private String lastSearchKeyword = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // sawsem theme
//        int themeId = ActivityUtil.getIntParam(getIntent(), "themeId");
//        switch (themeId) {
//            case 1:
//                super.setTheme(android.R.style.Theme_Black_NoTitleBar);
//                setContentView(R.layout.applistutil_activity_app_list_dark);
//                break;
//            case 2:
//                super.setTheme(android.R.style.Theme_Black_NoTitleBar);
//                setContentView(R.layout.applistutil_activity_app_list_dark_ru);
//                break;
//            default:
        setContentView(R.layout.applistutil_activity_app_list);
//                break;
//        }

        Button closeBtn = (Button) this.findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        this.keyworkEdit = (EditText) this.findViewById(R.id.et_keyword);

        // Find button
        this.findBtn = (Button) this.findViewById(R.id.btn_search);
        findBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String keyword = keyworkEdit.getText().toString();
                keyword = keyword.trim();

                // Keyword cannot be empty
                if (lastSearchKeyword.equals("") && keyword.equals("")) {
                    Toast.makeText(BaseAppActivity.this,
                                    R.string.keyword_cannot_empty, Toast.LENGTH_SHORT)
                            .show();
                }

                searchKeyword(keyword);
            }
        });
    }

    // To find all the matched apps
    @SuppressLint("DefaultLocale")
    protected void searchKeyword(String keyword) {
        String targetKeyword = keyword.toLowerCase();
        List<AppInfo> selectedAppList = new ArrayList<AppInfo>();
        synchronized (displayAppList) {
            Iterator<AppInfo> it = displayAppList.iterator();
            while (it.hasNext()) {
                AppInfo appInfo = it.next();
                if (appInfo.appName.toLowerCase().contains(targetKeyword)) {
                    selectedAppList.add(appInfo);
                }
            }
            adapter.setAppList(selectedAppList);
        }

        // For dynamic expand list view
        // appListView.dataChanged();
        adapter.notifyDataSetChanged();

        // Hide Input Method
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(BaseAppActivity.this
                        .getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);

        // Update last search keyword
        lastSearchKeyword = keyword;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Play a trick to set keyword as empty
        keyworkEdit.setText("");

        searchThread = new AppSearchThread(searcher, this);
        searchThread.start();
    }

    protected void init(IAppSearch searcher, MyAppListAdapter adapter) {
        this.searcher = searcher;
        this.adapter = adapter;
        //
        this.searchLayout = (LinearLayout) this
                .findViewById(R.id.layout_app_searching);

        // init list view
        this.appListView = (ListView) this.findViewById(R.id.application_list);
        // try {
        // SharedPreferences sp = PreferenceManager
        // .getDefaultSharedPreferences(this);
        // String strSize = sp.getString("initAppListSize", "100");
        // int size = Integer.valueOf(strSize);
        // appListView.setInitListSize(size);
        // } catch (Exception e) {
        // appListView.setInitListSize(100);
        // }
        appListView.setAdapter(adapter);

        // Click and Long click
        appListView.setOnItemClickListener(this);
        registerForContextMenu(appListView);
    }

    protected void killUserApp() {
        final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        synchronized (this.displayAppList) {
            for (AppInfo appInfo : displayAppList) {
                if (!appInfo.isSysApp)
                    am.killBackgroundProcesses(appInfo.packagePath);
            }
        }
        refresh();
    }

    protected void killApp(ActivityManager am, String packagePath) {
        am.killBackgroundProcesses(packagePath);
    }

    protected void refresh() {
        this.searchLayout.setVisibility(View.VISIBLE);
        searchThread = new AppSearchThread(searcher, this);
        searchThread.start();
    }

    @Override
    public void setSearchResult(List<AppInfo> appList) {
        // Log.d(MainActivity.TAG, "Search result returned!");
        synchronized (this.displayAppList) {
            displayAppList.clear();
            displayAppList.addAll(appList);
        }
        adapter.setAppList(appList);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                keyworkEdit.setEnabled(true);
                findBtn.setEnabled(true);

                searchLayout.setVisibility(View.GONE);
                appListView.requestFocus();
                // appListView.dataChanged();
                adapter.notifyDataSetChanged();
            }
        });

    }

    @Override
    public void searchEnded() {
        // runOnUiThread(new Runnable() {
        // @Override
        // public void run() {
        // }
        // });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.application_list) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

            IAppCustomize appCustomer = adapter.getAppCustomize();
            List<AppInfo> appList = adapter.getAppList();
            if (appList != null) {
                AppInfo appInfo = appList.get(info.position);
                if (appInfo != null) {
                    appCustomer.appLongClicked(menu, v, menuInfo, appInfo);
                }
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                            long arg3) {
        IAppCustomize appCustomer = adapter.getAppCustomize();
        List<AppInfo> appList = adapter.getAppList();
        if (appList != null) {
            AppInfo appInfo = appList.get(position);
            if (appInfo != null) {
                appCustomer.appClicked(appInfo);
            }
        }
    }
}
