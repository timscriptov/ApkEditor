package com.gmail.heagoo.appdm;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.appdm.base.R;
import com.gmail.heagoo.appdm.util.ADManager;
import com.gmail.heagoo.appdm.util.FileRecord;
import com.gmail.heagoo.appdm.util.FilenameComparator;
import com.gmail.heagoo.appdm.util.InterestAdManager;
import com.gmail.heagoo.appdm.util.PaidAppsChecker;
import com.gmail.heagoo.appdm.util.SDCard;
import com.gmail.heagoo.appdm.util.SignatureInfoReader;
import com.gmail.heagoo.appdm.util.StringPair;
import com.gmail.heagoo.applistutil.AppInfo;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.CommandInterface;
import com.gmail.heagoo.common.CommandRunner;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.common.ProcessingDialog;
import com.gmail.heagoo.common.ProcessingDialog.ProcessingInterface;
import com.gmail.heagoo.common.RefInvoke;
import com.gmail.heagoo.sqliteutil.RootCommand;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PrefOverallActivity extends CustomizedLangActivity implements OnClickListener {

    protected static final int DETAIL_ACTIVITY_REQUEST_CODE = 1001;
    // Stage: -1, scanning not finished
    // 0, failed
    // 1, succeed
    int stage = -1;
    String errMsg;
    int curTabIndex = 0;
    private String packagePath;
    private ScanThread thread;
    private ListView prefListView;
    private ListView dbListView;
    private ListView appInfoListView;
    private ListView fileListView;
    private LinearLayout scanningLayout;
    private ProgressBar progressBar;
    private TextView tipTv;
    private PackageManager pm;
    private ApplicationInfo applicationInfo;
    private PackageInfo packageInfo;
    private String appName;
    private RootFileAdapter fileListAdapter;
    // File list in full path
    // private List<String> xmlFileList = new ArrayList<String>();
    // private List<String> dbFileList = new ArrayList<String>();
    private List<StringPair> xmlFilePairs = new ArrayList<StringPair>();
    private List<StringPair> dbFilePairs = new ArrayList<StringPair>();
    // Button in bottom
    private RadioButton infoBtn;
    private RadioButton prefBtn;
    private RadioButton dbBtn;
    private RadioButton fileBtn;
    private Drawable infoDrawable;
    private Drawable infoBlueDrawable;
    private Drawable prefDrawable;
    private Drawable prefBlueDrawable;
    private Drawable dbDrawable;
    private Drawable dbBlueDrawable;
    private Drawable fileDrawable;
    private Drawable fileBlueDrawable;
    private ADManager adManager;

    // Is root mode or not
    private boolean isRootMode;

    // Show backup or not
    private boolean bShowBackup;

    // For interest AD
    private InterestAdManager interestAdMgr;
    private boolean prefModified = false;
    private long prefClickTime;
    private long prefReturnTime;
    private long createTime;
    private int prefClickedNum = 0;

    private int themeId;
    private boolean isDark;

    protected static String getEditableSyntax(String fileName) {
        if (fileName.endsWith(".xml")) {
            return "xml.xml";
        } else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "html.xml";
        } else if (fileName.endsWith(".css")) {
            return "css.xml";
        } else if (fileName.endsWith(".java")) {
            return "java.xml";
        } else if (fileName.endsWith(".json")) {
            return "json.xml";
        } else if (fileName.endsWith(".txt")) {
            return "txt.xml";
        } else if (fileName.endsWith(".js")) {
            return "js.xml";
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent intent = getIntent();
        setContentView(R.layout.appdm_activity_dataoverview);

        this.packagePath = ActivityUtil.getParam(intent, "packagePath");
        this.bShowBackup = ActivityUtil.getBoolParam(intent, "backup");
        try {
            this.pm = this.getPackageManager();
            this.applicationInfo = pm.getApplicationInfo(packagePath, 0);
            this.packageInfo = pm.getPackageInfo(packagePath, 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        this.createTime = System.currentTimeMillis();
        this.isRootMode = true;
        try {
            // The target app shares the same user id with me
            if (this.packageInfo.sharedUserId != null
                    && packageInfo.sharedUserId
                    .equals(pm.getPackageInfo(getPackageName(),
                            0).sharedUserId)) {
                this.isRootMode = false;
            }
        } catch (NameNotFoundException e) {
        }

        initUI();

        this.adManager = ADManager.init(this, R.id.adViewLayout);

        this.thread = new ScanThread(this);
        thread.start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Show interest ad on some conditions
        if (interestAdMgr != null) {
            interestAdMgr.onResume();
            if (this.prefModified || prefClickedNum >= 3
                    || (prefReturnTime - prefClickTime) >= 15000
                    || (System.currentTimeMillis() - createTime) >= 45000) {
                interestAdMgr.show();
            }
        }

        adManager.resume();
    }

    @Override
    public void onPause() {
        adManager.pause();
        super.onPause();
        if (interestAdMgr != null) {
            interestAdMgr.onPause();
        }
    }

    @Override
    public void onDestroy() {
        adManager.destroy();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Returned from detail activity
        if (requestCode == DETAIL_ACTIVITY_REQUEST_CODE) {
            this.prefReturnTime = System.currentTimeMillis();
            // Log.d("DEBUG", "onActivityResult called, stayTime= "
            // + (prefReturnTime - prefClickTime) + ", resultCode=" +
            // resultCode);
            if (resultCode == 1) {
                this.prefModified = true;
            }
        }
    }

    private void initUI() {
        ImageView iv = (ImageView) this.findViewById(R.id.app_icon);
        iv.setImageDrawable(applicationInfo.loadIcon(pm));

        TextView tv = (TextView) this.findViewById(R.id.app_name);
        tv.setText(applicationInfo.loadLabel(pm));

        tv = (TextView) this.findViewById(R.id.app_pkgpath);
        tv.setText(applicationInfo.packageName);

        // Save Image
        Button backupBtn = (Button) this.findViewById(R.id.button_backup);
        if (bShowBackup) {
            backupBtn.setOnClickListener(this);
        } else {
            backupBtn.setVisibility(View.GONE);
        }

        // List view
        this.appInfoListView = (ListView) this.findViewById(R.id.appInfo_list);
        this.prefListView = (ListView) this.findViewById(R.id.preference_list);
        this.dbListView = (ListView) this.findViewById(R.id.database_list);
        this.fileListView = (ListView) this.findViewById(R.id.files_list);
        this.scanningLayout = (LinearLayout) this
                .findViewById(R.id.layout_scanning);
        this.progressBar = (ProgressBar) this.findViewById(R.id.progress_bar);
        this.tipTv = (TextView) this.findViewById(R.id.tv_tip);

        // App info
        List<BasicInfoItem> data = new ArrayList<BasicInfoItem>();
        initAppInfo(data);
        appInfoListView.setAdapter(new BasicInfoAdapter(this, data, isDark));

        // File list
        initFileListView();

        enableListSwitch();
    }

    private void initAppInfo(List<BasicInfoItem> data) {
        Resources res = getResources();

        // App name
        {
            this.appName = applicationInfo.loadLabel(pm).toString();
            data.add(new BasicInfoItem(res.getString(R.string.appdm_app_name),
                    appName));
        }

        // Package name
        {
            data.add(new BasicInfoItem(
                    res.getString(R.string.appdm_package_name),
                    this.packagePath));
        }

        // Size
        File f = new File(applicationInfo.sourceDir);
        {
            // Map<String, String> values = new HashMap<String, String>();
            // values.put("NAME", "App Size");
            String strSize = SDCard.getSizeDescription(f.length());
            data.add(new BasicInfoItem(res.getString(R.string.appdm_app_size),
                    strSize));
        }

        // Version
        String verInfo = res.getString(R.string.appdm_version_code) + ": "
                + packageInfo.versionCode + "\n"
                + res.getString(R.string.appdm_version_name) + ": "
                + packageInfo.versionName;
        data.add(new BasicInfoItem(res.getString(R.string.appdm_version),
                verInfo));

        // APK File Path
        BasicInfoItem pathItem = new BasicInfoItem(
                res.getString(R.string.appdm_apk_file_path),
                applicationInfo.sourceDir, res.getString(R.string.save),
                v -> saveTheApk());
        data.add(pathItem);

        // Apk Build Time
        try {
            ZipFile zipFile = new ZipFile(applicationInfo.sourceDir);
            ZipEntry manifestEntry = zipFile.getEntry("AndroidManifest.xml");
            ZipEntry codeEntry = zipFile.getEntry("classes.dex");
            long manifestTime = Long.MIN_VALUE;
            if (manifestEntry != null) {
                manifestTime = manifestEntry.getTime();
            }
            long codeTime = Long.MIN_VALUE;
            if (codeEntry != null) {
                codeTime = codeEntry.getTime();
            }
            long time = (codeTime < manifestTime ? manifestTime : codeTime);

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(time);
            String strTime = cal.getTime().toString();
            data.add(new BasicInfoItem(
                    res.getString(R.string.appdm_apk_build_time), strTime));

            zipFile.close();
        } catch (IOException e) {
            // e.printStackTrace();
        }

        // Install Time
        {
            Calendar cal = Calendar.getInstance();
            long time = f.lastModified();
            cal.setTimeInMillis(time);

            String strTime = cal.getTime().toString();
            data.add(new BasicInfoItem(
                    res.getString(R.string.appdm_install_time), strTime));
        }

        // Publisher
        {
            String sig = SignatureInfoReader
                    .getSignature(applicationInfo.sourceDir);
            data.add(new BasicInfoItem(res.getString(R.string.appdm_signature),
                    sig));
        }

    }

    protected void saveTheApk() {
        new ApkSaveDialog(this, applicationInfo.sourceDir, this.appName).start();
    }

    public void setScanResult(final boolean succeed) {
        synchronized (this) {
            this.stage = (succeed ? 1 : 0);
            this.errMsg = thread.getErrorMsg();
        }

        this.runOnUiThread(() -> {
            PrefOverallActivity.this.findViewById(R.id.layout_scanning)
                    .setVisibility(View.INVISIBLE);

            if (succeed) {

                // Parse preference list
                String output = thread.getPrefList();
                if (output != null) {
                    String[] lines = output.split("\n");
                    for (String line : lines) {
                        if (line.endsWith(".xml")) {
                            String filePath = line;
                            int pos = line.lastIndexOf('/');
                            String filename = line.substring(pos + 1);
                            filename = filename.substring(0,
                                    filename.length() - 4);
                            xmlFilePairs.add(
                                    new StringPair(filename, filePath));
                        }
                    }
                }

                // Parse database list
                output = thread.getDbList();
                if (output != null) {
                    String[] lines = output.split("\n");
                    for (String line : lines) {
                        if (line.endsWith(".db")) {
                            String filePath = line;
                            int pos = line.lastIndexOf('/');
                            String filename = line.substring(pos + 1);
                            filename = filename.substring(0,
                                    filename.length() - 3);
                            dbFilePairs.add(
                                    new StringPair(filename, filePath));
                        }
                    }
                }

                initPrefListView();
                initDbListView();
                updateListView();

            } else {
                Toast.makeText(PrefOverallActivity.this, errMsg,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected void updateListView() {
        switch (this.curTabIndex) {
            case 0:
                drawAppInfoListView();
                break;
            case 1:
                drawPrefListView();
                break;
            case 2:
                drawDbListView();
                break;
        }
    }

    @SuppressWarnings("deprecation")
    private void enableListSwitch() {

        Resources res = getResources();
        this.infoDrawable = res.getDrawable(R.drawable.appdm_info);
        this.infoBlueDrawable = res.getDrawable(R.drawable.appdm_info_blue);
        this.prefDrawable = res.getDrawable(R.drawable.appdm_config);
        this.prefBlueDrawable = res.getDrawable(R.drawable.appdm_config_blue);
        this.dbDrawable = res.getDrawable(R.drawable.appdm_db);
        this.dbBlueDrawable = res.getDrawable(R.drawable.appdm_db_blue);
        this.fileDrawable = res.getDrawable(R.drawable.appdm_files);
        this.fileBlueDrawable = res.getDrawable(R.drawable.appdm_files_blue);

        this.infoBtn = (RadioButton) this.findViewById(R.id.tab_appinfo);
        this.prefBtn = (RadioButton) this.findViewById(R.id.tab_preference);
        this.dbBtn = (RadioButton) this.findViewById(R.id.tab_database);
        this.fileBtn = (RadioButton) this.findViewById(R.id.tab_files);

        infoBtn.setOnClickListener(v -> {
            curTabIndex = 0;

            drawAppInfoListView();

            infoBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    infoBlueDrawable, null, null);
            prefBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    prefDrawable, null, null);
            dbBtn.setCompoundDrawablesWithIntrinsicBounds(null, dbDrawable,
                    null, null);
            fileBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    fileDrawable, null, null);
        });

        prefBtn.setOnClickListener(v -> {
            curTabIndex = 1;

            drawPrefListView();

            infoBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    infoDrawable, null, null);
            prefBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    prefBlueDrawable, null, null);
            dbBtn.setCompoundDrawablesWithIntrinsicBounds(null, dbDrawable,
                    null, null);
            fileBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    fileDrawable, null, null);
        });

        dbBtn.setOnClickListener(v -> {
            curTabIndex = 2;

            drawDbListView();

            infoBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    infoDrawable, null, null);
            prefBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    prefDrawable, null, null);
            dbBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    dbBlueDrawable, null, null);
            fileBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    fileDrawable, null, null);
        });

        fileBtn.setOnClickListener(v -> {
            curTabIndex = 3;

            drawFileListView();

            infoBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    infoDrawable, null, null);
            prefBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    prefDrawable, null, null);
            dbBtn.setCompoundDrawablesWithIntrinsicBounds(null, dbDrawable,
                    null, null);
            fileBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    fileBlueDrawable, null, null);
        });
    }

    protected void drawAppInfoListView() {
        appInfoListView.setVisibility(View.VISIBLE);
        prefListView.setVisibility(View.INVISIBLE);
        dbListView.setVisibility(View.INVISIBLE);
        fileListView.setVisibility(View.INVISIBLE);
        scanningLayout.setVisibility(View.INVISIBLE);
    }

    protected void drawPrefListView() {
        synchronized (this) {
            switch (this.stage) {
                case -1:
                    prefListView.setVisibility(View.INVISIBLE);
                    scanningLayout.setVisibility(View.VISIBLE);
                    break;
                case 0:
                    prefListView.setVisibility(View.INVISIBLE);
                    scanningLayout.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    tipTv.setText(PrefOverallActivity.this.errMsg);
                    break;
                case 1:
                    scanningLayout.setVisibility(View.INVISIBLE);
                    prefListView.setVisibility(View.VISIBLE);
                    break;
            }
        }
        appInfoListView.setVisibility(View.INVISIBLE);
        dbListView.setVisibility(View.INVISIBLE);
        fileListView.setVisibility(View.INVISIBLE);
    }

    protected void drawDbListView() {
        synchronized (this) {
            switch (this.stage) {
                case -1:
                    dbListView.setVisibility(View.INVISIBLE);
                    scanningLayout.setVisibility(View.VISIBLE);
                    break;
                case 0:
                    dbListView.setVisibility(View.INVISIBLE);
                    scanningLayout.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    tipTv.setText(PrefOverallActivity.this.errMsg);
                    break;
                case 1:
                    scanningLayout.setVisibility(View.INVISIBLE);
                    dbListView.setVisibility(View.VISIBLE);
                    break;
            }
        }
        appInfoListView.setVisibility(View.INVISIBLE);
        prefListView.setVisibility(View.INVISIBLE);
        fileListView.setVisibility(View.INVISIBLE);
    }

    protected void drawFileListView() {
        scanningLayout.setVisibility(View.INVISIBLE);

        appInfoListView.setVisibility(View.INVISIBLE);
        prefListView.setVisibility(View.INVISIBLE);
        dbListView.setVisibility(View.INVISIBLE);
        fileListView.setVisibility(View.VISIBLE);
    }

    private void initPrefListView() {
        prefListView.setAdapter(
                new NameAndPathAdapter(this, this.xmlFilePairs, isDark));
        prefListView.setOnItemClickListener((arg0, arg1, position, arg3) -> {
            PrefOverallActivity.this.prefClickTime = System
                    .currentTimeMillis();
            PrefOverallActivity.this.prefClickedNum += 1;

            Intent intent = new Intent(PrefOverallActivity.this,
                    PrefDetailActivity.class);
            ActivityUtil.attachParam(intent, "appName",
                    (String) applicationInfo.loadLabel(pm));
            ActivityUtil.attachParam(intent, "xmlFilePath",
                    xmlFilePairs.get(position).second);
            ActivityUtil.attachParam(intent, "packagePath",
                    PrefOverallActivity.this.packagePath);
            ActivityUtil.attachBoolParam(intent, "isRootMode",
                    PrefOverallActivity.this.isRootMode);
            ActivityUtil.attachParam(intent, "themeId",
                    PrefOverallActivity.this.themeId);
            startActivityForResult(intent, DETAIL_ACTIVITY_REQUEST_CODE);
        });

        // Load the interest AD
        // Load it only when preference file number >= 3
        if (!PaidAppsChecker.isPaidAppsExist(this) && this.xmlFilePairs != null
                && xmlFilePairs.size() >= 3) {
            SharedPreferences sp = this.getSharedPreferences("info", 0);
            long lastAdTime = sp.getLong("lastTime", 0);
            long curTime = System.currentTimeMillis();
            if (curTime > lastAdTime + 75000) {
                this.interestAdMgr = new InterestAdManager(this);
            }
        }
    }

    private void initDbListView() {
        dbListView.setAdapter(
                new NameAndPathAdapter(this, this.dbFilePairs, isDark));
        dbListView.setOnItemClickListener((arg0, arg1, position, arg3) -> {
            Intent intent = new Intent(PrefOverallActivity.this,
                    com.gmail.heagoo.sqliteutil.SqliteTableListActivity.class);
            ActivityUtil.attachParam(intent, "dbFilePath",
                    dbFilePairs.get(position).second);
            ActivityUtil.attachParam(intent, "isRootMode",
                    (isRootMode ? "true" : "false"));
            ActivityUtil.attachParam(intent, "themeId", themeId);
            startActivity(intent);
        });
    }

    private void initFileListView() {
        String curPath = getFilesDir().getPath();
        String packageName = getPackageName();
        int position = curPath.indexOf(packageName);
        String rootPath = curPath.substring(0, position);

        this.fileListAdapter = new RootFileAdapter(this,
                rootPath + packagePath + "/files", this.isRootMode,
                this.isDark);
        fileListView.setAdapter(fileListAdapter);
        fileListView.setOnItemClickListener((arg0, arg1, position1, arg3) -> fileItemClicked(position1));

        // Long click listener
        fileListView.setOnItemLongClickListener((parent, view, position12, id) -> {
            List<FileRecord> records = new ArrayList<FileRecord>();
            final String curDir = fileListAdapter.getData(records);
            final FileRecord rec = records.get(position12);
            if (rec.isDir) {
                return true;
            }

            parent.setOnCreateContextMenuListener(
                    new OnCreateContextMenuListener() {
                        public void onCreateContextMenu(ContextMenu menu,
                                                        View v, ContextMenuInfo menuInfo) {
                            // Open in Editor
                            MenuItem item1 = menu.add(0, Menu.FIRST, 0,
                                    R.string.appdm_open_in_editor);
                            item1.setOnMenuItemClickListener(
                                    item -> {
                                        extractAndOpenEditor(
                                                curDir + "/" + rec.fileName,
                                                null);
                                        return true;
                                    });
                        }
                    });
            return false;
        });
    }

    protected void fileItemClicked(int position) {
        List<FileRecord> records = new ArrayList<>();
        String curDir = fileListAdapter.getData(records);
        if (position < records.size()) {
            FileRecord rec = records.get(position);
            if (rec.isDir) {
                // Open the sub directory
                final String dirPath = getSubDirectory(curDir, rec.fileName);
                new ProcessingDialog(PrefOverallActivity.this,
                        new ProcessingInterface() {
                            @SuppressWarnings("unchecked")
                            @Override
                            public void process() throws Exception {
                                List<FileRecord> subFiles = fileListAdapter
                                        .listFiles(dirPath, true);
                                Collections.sort(subFiles,
                                        new FilenameComparator());
                                if (subFiles != null) {
                                    fileListAdapter.updateList(dirPath,
                                            subFiles);
                                }
                            }

                            @Override
                            public void afterProcess() {
                            }
                        }, -1).show();
            } else {
                String filepath = curDir + "/" + rec.fileName;
                String syntax = getEditableSyntax(rec.fileName);
                if (syntax != null) {
                    extractAndOpenEditor(filepath, syntax);
                } else {
                    extractAndTryToOpen(filepath);
                }
            }
        }
    }

    private String getSubDirectory(String curDir, String name) {
        if ("..".equals(name)) {
            int pos = curDir.lastIndexOf('/');
            if (pos != -1) {
                return curDir.substring(0, pos);
            } else {
                return curDir;
            }
        } else {
            return curDir + "/" + name;
        }
    }

    // Return the target file path
    // Return null if failed
    private String copyFileBySu(String filePath) {
        String postfix = null;
        int pos = filePath.lastIndexOf('.');
        if (pos != -1) {
            postfix = filePath.substring(pos);
            if (postfix.contains("/")) { // not the real postfix
                postfix = null;
            }
        }
        String tempDir = SDCard.getTempDir(this);
        String tmpFilePath = tempDir + "/_work"
                + (postfix != null ? postfix : "");

        CommandInterface rc = createCommandRunner();
        String strCmd = "cp";
        File bin = new File(getFilesDir(), "mycp");
        if (bin.exists()) {
            strCmd = bin.getPath();
        }
        boolean copyRet = rc.runCommand(
                String.format(strCmd + " \"%s\" %s", filePath, tmpFilePath),
                null, 2000, false);
        if (copyRet) {
            return tmpFilePath;
        } else {
            return null;
        }
    }

    private void extractAndOpenEditor(final String filePath,
                                      final String syntaxName) {
        new ProcessingDialog(this, new ProcessingInterface() {
            String tmpFilePath = null;

            @Override
            public void process() throws Exception {
                tmpFilePath = copyFileBySu(filePath);
            }

            @Override
            public void afterProcess() {
                if (tmpFilePath != null) {
                    // Open the editor
                    Intent intent = new Intent(PrefOverallActivity.this,
                            com.gmail.heagoo.neweditor.EditorActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("filePath", tmpFilePath);
                    bundle.putString("realFilePath", filePath);
                    if (syntaxName != null) {
                        bundle.putString("syntaxFileName", syntaxName);
                    }
                    bundle.putBoolean("isRootMode",
                            PrefOverallActivity.this.isRootMode);
                    bundle.putIntArray("resourceIds",
                            new int[]{R.string.appdm_file_too_big,
                                    R.string.appdm_file_saved,
                                    R.string.appdm_not_found});
                    intent.putExtras(bundle);
                    PrefOverallActivity.this.startActivityForResult(intent,
                            1000);
                } else {
                    Toast.makeText(PrefOverallActivity.this,
                                    "Failed to open the file.", Toast.LENGTH_SHORT)
                            .show();
                }
            }

        }, -1).show();
    }

    // Extract and try to open it with external apps
    private void extractAndTryToOpen(final String filePath) {
        new ProcessingDialog(this, new ProcessingInterface() {
            String tmpFilePath = null;

            @Override
            public void process() throws Exception {
                tmpFilePath = copyFileBySu(filePath);
            }

            @Override
            public void afterProcess() {
                if (tmpFilePath != null) {
                    com.gmail.heagoo.appdm.util.OpenFiles
                            .openFile(PrefOverallActivity.this, tmpFilePath);
                } else {
                    Toast.makeText(PrefOverallActivity.this,
                                    "Failed to open the file.", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }, -1).show();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        // As backup is hidden, not implemented yet!
        if (id == R.id.button_backup) {
            AppInfo appInfo = AppInfo.create(pm, applicationInfo);
            // BackupDialog dlg = new BackupDialog(this, appInfo);
            // dlg.show();
            RefInvoke.invokeStaticMethod("com.gmail.heagoo.appdm.free.a", "s",
                    new Class<?>[]{Activity.class, AppInfo.class},
                    new Object[]{this, appInfo});
        }
    }

    // Support root mode and non-root mode
    protected CommandInterface createCommandRunner() {
        if (this.isRootMode) {
            return new RootCommand();
        } else {
            return new CommandRunner();
        }
    }

    static class MyFilter implements FilenameFilter {
        private String type;

        public MyFilter(String type) {
            this.type = type;
        }

        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(type);
        }
    }

    static class ScanThread extends Thread {
        WeakReference<PrefOverallActivity> activityRef;
        private String packagePath;
        private String errMsg;

        // Record the output returned by ls
        private String prefOutput;
        private String dbOutput;

        public ScanThread(PrefOverallActivity activity) {
            this.packagePath = activity.packagePath;
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            boolean succeed = scanXmlFiles();
            PrefOverallActivity activity = activityRef.get();
            if (activity != null) {
                activity.setScanResult(succeed);
            }
        }

        private boolean scanXmlFiles() {
            PrefOverallActivity activity = activityRef.get();
            if (activity == null) {
                return false;
            }

            if (!SDCard.exist()) {
                this.errMsg = "Can not find SD card!";
                return false;
            }

            String curPath = activity.getFilesDir().getPath();
            String packageName = activity.getPackageName();
            int position = curPath.indexOf(packageName);
            if (position == -1) {
                this.errMsg = "Can not find data path!";
                return false;
            }

            String rootPath = curPath.substring(0, position);

            if (activity.isRootMode) {
                String prefPath = rootPath + packagePath
                        + "/shared_prefs/*.xml";
                String dbPath = rootPath + packagePath + "/databases/*.db";

                CommandInterface rc = activity.createCommandRunner();
                String strCommand = String.format("ls %s", prefPath);
                if (rc.runCommand(strCommand, null, 5000)) {
                    this.prefOutput = rc.getStdOut();
                } else {
                    errMsg = "Can not get access to read files!";
                    return false;
                }

                if (rc.runCommand(String.format("ls %s", dbPath), null, 5000)) {
                    this.dbOutput = rc.getStdOut();
                } else {
                    errMsg = "Can not get access to read files!";
                    return false;
                }
            }

            // For the non-root mode, directly list directory
            // Still DO NOT know why ls will fail
            else {
                File dir = new File(rootPath + packagePath + "/shared_prefs");
                File files[] = dir.listFiles();
                if (files != null) {
                    StringBuffer sb = new StringBuffer();
                    for (File f : files) {
                        String path = f.getAbsolutePath();
                        sb.append(path);
                        sb.append("\n");
                    }
                    this.prefOutput = sb.toString();
                }

                dir = new File(rootPath + packagePath + "/databases");
                files = dir.listFiles();
                if (files != null) {
                    StringBuffer sb = new StringBuffer();
                    for (File f : files) {
                        String path = f.getAbsolutePath();
                        sb.append(path);
                        sb.append("\n");
                    }
                    this.dbOutput = sb.toString();
                }
            }

            return true;
        }

        public String getPrefList() {
            return prefOutput;
        }

        public String getDbList() {
            return dbOutput;
        }

        public String getErrorMsg() {
            return errMsg;
        }
    }
}
