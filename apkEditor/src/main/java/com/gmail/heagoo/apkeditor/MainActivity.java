package com.gmail.heagoo.apkeditor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.prj.ProjectListActivity;
import com.gmail.heagoo.apkeditor.prj.ProjectListActivity2;
import com.gmail.heagoo.apkeditor.util.OnlineMessage;
import com.gmail.heagoo.common.FileUtil;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.httpserver.HttpServiceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * For apktool, look into:
 * brut.apktool\apktool-lib\src\main\java\brut\androlib\ApkDecoder.java:decode()
 * <p>
 * And then look into: brut.androlib.res.AndrolibResources
 */
public class MainActivity extends AppCompatActivity implements
        AdapterView.OnItemClickListener, ProcessingDialog.ProcessingInterface {
    private static int g_vcRet;
    static {
        System.loadLibrary("syscheck");
    }

    // Used to show a dialog
    private OnlineMessage prompter;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;

    public static native int isX86();
    public static native void it(Object ctx, String pkgName, String dataDir, String apkPath);
    public static native void mg(String res, String orig,
                                 String replaces, int len2, String mapping, int len1);
    public static native void md(String target, String source, String added,
                                 int len1, String removed, int len2, String replaced, int len3);
    public static native int vc(Object ctx, int seed);

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    public static boolean upgradedFromOldVersion(Context ctx) {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setupLanguage();
        setContentView(R.layout.activity_main);

        initUI();

        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo moreInfo = pm.getApplicationInfo(this.getPackageName(), 0);
            String apkPath = moreInfo.sourceDir;
            it(this.getApplicationContext(),
                    this.getPackageName(), this.getFilesDir().getPath(), apkPath);
        } catch (Exception ignored) {
        }

        // checkVersion();
        // isX86();

        // As pro has no network access, cannot get online message
        if (!BuildConfig.IS_PRO) {
            this.prompter = new OnlineMessage(this);
        }

        // Show license dialog
        if (BuildConfig.SHOW_AGREEMENT) {
            if (!AppAgreementDialog.appLicenseAccepted(this)) {
                new AppAgreementDialog(this).show();
            } else {
                initFileWithPermissionCheck();
            }
        } else {
            initFileWithPermissionCheck();
        }
    }

    private void setupLanguage() {
        String languageToLoad =
                PreferenceManager.getDefaultSharedPreferences(this).getString("Language", "");
        if (!languageToLoad.equals("")) {
            Locale locale = new Locale(languageToLoad);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocale(locale);
            } else {
                config.locale = locale;
            }
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());
        }
    }

    public void onResume() {
        if (!BuildConfig.IS_PRO) {
            prompter.showMessageDialog();
        }
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void checkVersion() {
        String pkgName = this.getPackageName();
        // pro version
        if (pkgName.charAt(pkgName.length() - 1) == 'o') {
            String installer = this.getPackageManager().getInstallerPackageName(pkgName);
            if (installer == null || !installer.endsWith(".vending")) {
                Toast.makeText(this, "Please Install it from Google Play!",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupSlidingMenu() {
        // enabling action bar app icon and behaving it as toggle button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.slider_list);

        MenuListAdapter adapter = new MenuListAdapter(
                this, GlobalConfig.instance(this).isDarkTheme());
        mDrawerList.setAdapter(adapter);
        mDrawerList.setOnItemClickListener(this);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.app_name, // nav drawer open - description for accessibility
                R.string.app_name // nav drawer close - description for accessibility
        );
        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    private void initUI() {
        if (BuildConfig.PARSER_ONLY) {
            ImageView imageView = (ImageView) findViewById(R.id.logo);
            imageView.setImageResource(R.drawable.parser_logo);
        }

        // Left sliding menu
        setupSlidingMenu();

        // Select apk from folder
        TextView openApkBtn = (TextView) this.findViewById(R.id.tv_select_apkfile);
        openApkBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FileListActivity.class);
            startActivity(intent);
        });

        // Select apk from app
        TextView openAppBtn = (TextView) this.findViewById(R.id.tv_select_appfile);
        if (BuildConfig.DISPLAY_APP) {
            openAppBtn.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, UserAppActivity.class);
                startActivity(intent);
            });
        } else {
            openAppBtn.setText(R.string.settings);
            openAppBtn.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
            });
        }

        // Exit
        TextView exitButton = (TextView) this.findViewById(R.id.tv_exit);
        exitButton.setOnClickListener(v -> {
            if (System.currentTimeMillis() < 3600 * 1000) {
                test();
            }
            new ProcessingDialog(MainActivity.this, MainActivity.this, -1).show();
        });

        // Help
        // For APK Parser, use it as 'project'
        TextView helpButton = (TextView) this.findViewById(R.id.tv_help);
        if (BuildConfig.PARSER_ONLY) {
            helpButton.setText(R.string.projects);
            helpButton.setOnClickListener(v -> {
                Class<?> cls = (BuildConfig.PARSER_ONLY ? ProjectListActivity2.class : ProjectListActivity.class);
                Intent helpIntent = new Intent(MainActivity.this, cls);
                startActivity(helpIntent);
            });
        } else {
            helpButton.setOnClickListener(v -> {
                Intent helpIntent = new Intent(MainActivity.this, HelpActivity.class);
                startActivity(helpIntent);
            });
        }
    }

    private void test() {
        g_vcRet = vc(this.getApplicationContext(), 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // toggle nav drawer on selecting action bar app icon/title
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingActivity.class);
            startActivity(i);
            return true;
        } else if (id == R.id.action_about) {
            new AboutDialog(this).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /***
     * Called when invalidateOptionsMenu() is triggered
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // if nav drawer is opened, hide the action items
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.action_settings).setVisible(!drawerOpen);
        menu.findItem(R.id.action_about).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        this.finish();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initFile();
        }
    }

    public void initFileWithPermissionCheck() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        } else {
            initFile();
        }
    }

    private void initFile() {
        // For free version, do not need to create these files
        if (!BuildConfig.IS_PRO) {
            return;
        }

        // If need to limit the new version, does not need to create such files
        if (BuildConfig.LIMIT_NEW_VERSION) {
            return;
        }

        File f = new File(this.getFilesDir(), "work.xml");
        if (!f.exists()) {
            try {
                f.createNewFile();
                f.setWritable(true);
            } catch (IOException ignored) {
            }
        }

        f = new File(this.getFilesDir(), "work.db");
        if (!f.exists()) {
            try {
                f.createNewFile();
                f.setWritable(true);
            } catch (IOException ignored) {
            }
        }

        // Copy mycp
        try {
            File bin = new File(this.getFilesDir(), "mycp");
            if (!bin.exists() && (Build.VERSION.SDK_INT >= 20)) {
                InputStream input = getAssets().open("mycp");
                FileOutputStream output = new FileOutputStream(bin);
                IOUtils.copy(input, output);
                input.close();
                output.close();
                bin.setExecutable(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        switch ((int) id) {
            case MenuListAdapter.ITEM_PROJECT: // projects
            {
                Class<?> cls = (BuildConfig.PARSER_ONLY ? ProjectListActivity2.class : ProjectListActivity.class);
                Intent intent = new Intent(this, cls);
                startActivity(intent);
                break;
            }
            case MenuListAdapter.ITEM_DONATE: // patch/Donate
            {
                Intent intent = new Intent(this, DonateActivity.class);
                startActivity(intent);
                break;
            }
            case MenuListAdapter.ITEM_SETTING: // settings
            {
                Intent intent = new Intent(this, SettingActivity.class);
                startActivity(intent);
                break;
            }
            case MenuListAdapter.ITEM_IMG_DOWNLOADER: {
                Intent intent = new Intent(this, ImageDownloadActivity.class);
                startActivity(intent);
                break;
            }
            case MenuListAdapter.ITEM_ABOUT: // about
                new AboutDialog(this).show();
                break;
        }
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    // Deal with exit
    @Override
    public void process() throws Exception {
        try {
            Intent intent = new Intent(MainActivity.this, ApkComposeService.class);
            stopService(intent);
            HttpServiceManager.instance().stopWebService(this);

            File fileDir = MainActivity.this.getFilesDir();
            String rootDirectory = fileDir.getAbsolutePath();
            String decodeRootPath = rootDirectory + "/decoded";
            FileUtil.deleteAll(new File(decodeRootPath));
        } catch (Throwable ignored) {
        }
    }

    // Deal with exit
    @Override
    public void afterProcess() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
