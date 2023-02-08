package com.gmail.heagoo.apkeditor;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.EditModeDialog.IEditModeSelected;
import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.se.SimpleEditActivity;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.common.Display;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.RefInvoke;
import com.gmail.heagoo.common.SDCard;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class UserAppActivity extends CustomizedLangActivity implements OnItemClickListener,
        OnClickListener, IEditModeSelected {

    private final List<AppInfo> displayApps = new ArrayList<>();
    private final int APPINFO_ID = Menu.FIRST + 1;
    private final int BACKUP_ID = Menu.FIRST + 2;
    private final int LAUNCH_ID = Menu.FIRST + 3;
    // appTypeIndex = 0, show user apps; appTypeIndex = 1, show system apps
    private int appTypeIndex = 0;
    private int appOrderIndex = 0; // 0 means by name
    private ListView appListView;
    private MyHandler handler = new MyHandler(this);
    private List<AppInfo> allApps = new ArrayList<>();
    private PopupWindow popupWindow;
    private GroupAdapter popListAdapter;
    private EditText keywordEdit;
    private Button searchBtn;
    private ProgressBar progressBar;

    public static boolean startFullEditActivity(final Context ctx, final String filePath) {
        String mode = SettingActivity.getDecodeMode(ctx);

        // to be dynamically selected
        if ("2".equals(mode)) {
            new DecodeModeDialog(ctx, (mode1, extraStr) -> startFullEditActivity(ctx, filePath, "" + mode1), filePath).show();
            return false;
        } else {
            startFullEditActivity(ctx, filePath, mode);
            return true;
        }
    }

    // mode = "0" means full editing
    private static void startFullEditActivity(Context ctx, String filePath, String mode) {
        Intent intent = new Intent(ctx, ApkInfoExActivity.class);
        ActivityUtil.attachParam(intent, "apkPath", filePath);
        boolean fullDecoding = "0".equals(mode);
        ActivityUtil.attachBoolParam(intent, "isFullDecoding", fullDecoding);
        ctx.startActivity(intent);
    }

    public static boolean openApp(Context context, String packageName) {
        PackageManager manager = context.getPackageManager();
        try {
            Intent i = manager.getLaunchIntentForPackage(packageName);
            if (i == null) {
                return false;
                //throw new ActivityNotFoundException();
            }
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            context.startActivity(i);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        int layoutId;
        // sawsem theme
//        switch (GlobalConfig.instance(this).getThemeId()) {
//            case GlobalConfig.THEME_DARK_DEFAULT:
//                super.setTheme(android.R.style.Theme_Black_NoTitleBar);
//                layoutId = R.layout.activity_applist_dark;
//                break;
//            case GlobalConfig.THEME_DARK_RUSSIAN:
//                super.setTheme(android.R.style.Theme_Black_NoTitleBar);
//                layoutId = R.layout.activity_applist_dark_ru;
//                break;
//            default:
        layoutId = R.layout.activity_applist;
//                break;
//        }
        setContentView(layoutId);

        // Get default app order
        String strOrder = SettingActivity.getListOrder(this);
        this.appOrderIndex = getOrderIndex(strOrder);

        initUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        reScanAppList();
    }

    public void showAppList(List<AppInfo> apps) {
        this.progressBar.setVisibility(View.GONE);
        this.appListView.setVisibility(View.VISIBLE);

        AppListAdapter adapter = (AppListAdapter) appListView.getAdapter();
        String[] orders = getResources().getStringArray(R.array.order_value);
        String strOrder = (appOrderIndex < orders.length) ? orders[appOrderIndex] : "";
        adapter.setAppList(apps, strOrder);
        adapter.notifyDataSetChanged();

        synchronized (this.displayApps) {
            this.displayApps.clear();
            this.displayApps.addAll(apps);
        }

        // Enable the search
        this.keywordEdit.setEnabled(true);
        this.searchBtn.setEnabled(true);
    }

    // Get app order index from the string
    private int getOrderIndex(String strOrder) {
        String[] orders = getResources().getStringArray(R.array.order_value);
        for (int i = 0; i < orders.length; ++i) {
            if (strOrder.equals(orders[i])) {
                return i;
            }
        }
        return 0;
    }

    protected void initAppList() {
        AppListAdapter adapter = new AppListAdapter(this);
        adapter.setAppList(displayApps, SettingActivity.getListOrder(this));
        appListView.setAdapter(adapter);
        appListView.setOnItemClickListener(this);
        this.registerForContextMenu(appListView);
    }

    private void initUI() {
        progressBar = (ProgressBar) this.findViewById(R.id.progress_bar);

        TextView tv = (TextView) this.findViewById(R.id.apptype);
        tv.setText(R.string.select_apk_from_app);

        View moreMenu = this.findViewById(R.id.menu_more);
        moreMenu.setOnClickListener(this);

        appListView = (ListView) this.findViewById(R.id.application_list);
        initAppList();

        this.keywordEdit = (EditText) this.findViewById(R.id.et_keyword);
        this.searchBtn = (Button) this.findViewById(R.id.btn_search);
        searchBtn.setOnClickListener(this);
        this.findViewById(R.id.btn_close).setOnClickListener(this);
    }

    private void reScanAppList(int typeIndex, int orderIndex) {
        this.appTypeIndex = typeIndex;
        this.appOrderIndex = orderIndex;
        reScanAppList();
    }

    private void reScanAppList() {
        new Thread() {
            @Override
            public void run() {
                // To get the data
                List<AppInfo> appList = new ArrayList<>();
                getAppList(appList);

                // To refresh UI
                handler.setAppList(appList);
                handler.sendEmptyMessage(0);
            }
        }.start();
    }

    protected void getAppList(List<AppInfo> appList) {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> appInfoList = pm.getInstalledApplications(0);

        // Show user apps
        if (appTypeIndex == 0) {
            for (ApplicationInfo ai : appInfoList) {
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    // When do some recording, to enable following line
                    //if (ai.enabled)
                    appList.add(AppInfo.create(pm, ai));
                }
            }
        }
        // Show system apps
        else {
            for (ApplicationInfo ai : appInfoList) {
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    appList.add(AppInfo.create(pm, ai));
                }
            }
        }
    }

    private AppInfo getAppInfo(int position) {
        AppInfo info = null;
        synchronized (displayApps) {
            try {
                info = displayApps.get(position);
            } catch (Throwable ignored) {
            }
        }
        return info;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        AppInfo info = getAppInfo(position);
        if (info != null) {
            appClicked(info);
        }
    }

    public void appClicked(AppInfo appInfo) {
        //Intent intent = new Intent(UserAppActivity.this, ApkInfoActivity.class);
        PackageManager pm = getPackageManager();
        ApplicationInfo moreInfo;
        try {
            moreInfo = pm.getApplicationInfo(appInfo.packagePath, 0);
            String _apkPath = moreInfo.sourceDir;

            if (BuildConfig.PARSER_ONLY) {
                UserAppActivity.startFullEditActivity(this, _apkPath);
            } else if (BuildConfig.LIMIT_NEW_VERSION && !MainActivity.upgradedFromOldVersion(this)) {
                startFullEditActivity(this, _apkPath);
            } else {
                EditModeDialog dlg = new EditModeDialog(this, this, _apkPath, moreInfo.packageName);
                dlg.show();
            }

        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.menu_more) {
            popListAdapter(v);
        } else if (id == R.id.btn_close) {
            this.finish();
        } else if (id == R.id.btn_search) {
            searchApp();
        }
    }

    @SuppressLint("DefaultLocale")
    private void searchApp() {
        String keyword = keywordEdit.getText().toString();
        keyword = keyword.toLowerCase();

        List<AppInfo> matchedApps = new ArrayList<>();
        for (AppInfo appInfo : this.allApps) {
            if (appInfo.appName.toLowerCase().contains(keyword)) {
                matchedApps.add(appInfo);
            }
        }

        showAppList(matchedApps);
    }

    @SuppressLint("InflateParams")
    private void popListAdapter(View parent) {
        if (popupWindow == null) {
            LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.popup_list, null);
            ListView lv_group = (ListView) view.findViewById(R.id.lvGroup);

            // Menu String
            ArrayList<String> groups = new ArrayList<>();
            Resources res = getResources();
            groups.add(res.getString(R.string.user_apps));
            groups.add(res.getString(R.string.system_apps));
            String[] strOrders = res.getStringArray(R.array.order_value);
            for (String strOrder : strOrders) {
                groups.add(strOrder);
            }

            this.popListAdapter = new GroupAdapter(this, groups);
            lv_group.setAdapter(popListAdapter);
            // Create a popup window
            int height = Display.dip2px(this, 203); // 4 * 50dip content + 3 * 1dip divider
            int width = Display.getWidth(this) / 2;
            popupWindow = new PopupWindow(view, width, height);

            lv_group.setOnItemClickListener((adapterView, view1, position, id) -> {
                appListView.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                switch (position) {
                    case 0: // User apps
                        reScanAppList(0, appOrderIndex);
                        break;
                    case 1: // System apps
                        reScanAppList(1, appOrderIndex);
                        break;
                    case 2: // Sort by app name
                        reScanAppList(appTypeIndex, 0);
                        break;
                    case 3: // Sort by install time
                        reScanAppList(appTypeIndex, 1);
                        break;
                }

                if (popupWindow != null) {
                    popupWindow.dismiss();
                }
            });
        }

        // Update checked items
        List<Integer> checkedIndice = new ArrayList<>();
        checkedIndice.add(appTypeIndex);
        checkedIndice.add(2 + appOrderIndex);
        popListAdapter.setCheckIndice(checkedIndice);

        // Focus and allow disappear touch outside
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);

        // To make it can disappear when press return button
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        // Display position
        int xPos = windowManager.getDefaultDisplay().getWidth() / 2 - popupWindow.getWidth() / 2;

        popupWindow.showAsDropDown(parent, xPos, 0);
    }

    // Simple edit or full edit clicked
    @Override
    public void editModeSelected(int mode, String extraStr) {
        String filePath = extraStr;

        Intent intent = null;
        switch (mode) {
            case EditModeDialog.SIMPLE_EDIT:
                intent = new Intent(this, SimpleEditActivity.class);
                break;
            case EditModeDialog.FULL_EDIT:
                if (startFullEditActivity(this, filePath)) {
                    this.finish();
                }
                return;
            case EditModeDialog.COMMON_EDIT:
                intent = new Intent(this, CommonEditActivity.class);
                break;
            case EditModeDialog.XML_FILE_EDIT:
                intent = new Intent(this, AxmlEditActivity.class);
                break;
            case EditModeDialog.DATA_EDIT:
                try {
                    String pkgName = extraStr;
                    RefInvoke.invokeStaticMethod(
                            "com.gmail.heagoo.apkeditor.pro.appdm", "de",
                            new Class<?>[]{Context.class, String.class},
                            new Object[]{this, pkgName});
                } catch (Exception ignored) {
                }
                break;
        }

        if (intent != null) {
            ActivityUtil.attachParam(intent, "apkPath", filePath);
            startActivity(intent);

            this.finish();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        if (info == null) {
            return;
        }

        AppInfo appInfo = getAppInfo(info.position);
        if (appInfo == null) {
            return;
        }

        menu.setHeaderTitle(appInfo.appName);

        menu.add(0, APPINFO_ID, 0, R.string.app_info);
        menu.add(0, BACKUP_ID, 0, R.string.backup);
        menu.add(0, LAUNCH_ID, 0, R.string.launch);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;

        switch (item.getItemId()) {
            case APPINFO_ID:
                showAppInfo(position);
                return true;
            case BACKUP_ID:
                backupApp(position);
                return true;
            case LAUNCH_ID:
                launchApp(position);
                return true;
        }

        return (super.onOptionsItemSelected(item));
    }

    // App Detail/information
    private void showAppInfo(int position) {
        try {
            AppInfo info = getAppInfo(position);
            if (info == null) {
                return;
            }

            String packageName = info.packagePath;

            try {
                // Open the specific App Info page:
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            } catch (ActivityNotFoundException ignored) {
                // Open the generic Apps page:
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                startActivity(intent);
            }
        } catch (Exception ignored) {
        }
    }

    // Copy apk of the target app to sd card
    private void backupApp(int position) {
        try {
            AppInfo info = getAppInfo(position);
            if (info == null) {
                return;
            }

            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(info.packagePath, 0);
            final String appName = info.appName;
            final String apkPath = appInfo.publicSourceDir;

            ProcessingDialog dlg = new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
                private String outPath;
                private boolean succeed = false;

                @Override
                public void process() throws Exception {
                    outPath = SDCard.makeBackupDir(UserAppActivity.this) + appName + ".apk";

                    FileInputStream in = null;
                    FileOutputStream out = null;
                    try {
                        in = new FileInputStream(apkPath);
                        out = new FileOutputStream(outPath);
                        IOUtils.copy(in, out);
                        succeed = true;
                    } finally {
                        IOUtils.closeQuietly(in);
                        IOUtils.closeQuietly(out);
                    }
                }

                @Override
                public void afterProcess() {
                    if (succeed) {
                        String format = getString(R.string.apk_saved_tip);
                        String message = String.format(format, outPath);
                        Toast.makeText(UserAppActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                }
            }, -1);

            dlg.show();
        } catch (Exception ignored) {
        }
    }

    private void launchApp(int position) {
        try {
            AppInfo info = getAppInfo(position);
            if (info == null) {
                return;
            }

            if (!openApp(this, info.packagePath)) {
                String message = String.format(getString(R.string.cannot_launch), info.appName);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ignored) {
        }
    }

    // Async handler
    private static class MyHandler extends Handler {
        private WeakReference<UserAppActivity> activityRef;

        private List<AppInfo> appList;

        public MyHandler(UserAppActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        public void setAppList(List<AppInfo> appList) {
            this.appList = appList;
        }

        public void handleMessage(Message msg) {
            UserAppActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }
            switch (msg.what) {
                // Refresh apps list
                case 0:
                    activity.allApps.clear();
                    activity.allApps.addAll(appList);
                    activity.showAppList(appList);
                    break;
            }
        }
    }
}
