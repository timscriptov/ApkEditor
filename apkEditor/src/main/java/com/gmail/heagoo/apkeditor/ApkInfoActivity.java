package com.gmail.heagoo.apkeditor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection;
import com.gmail.heagoo.apkeditor.ac.AutoCompleteAdapter;
import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.smali.AsyncDecodeTask;
import com.gmail.heagoo.apkeditor.smali.AsyncDecodeTask.IDecodeTaskCallback;
import com.gmail.heagoo.apkeditor.translate.PossibleLanguages;
import com.gmail.heagoo.apkeditor.translate.TranslateItem;
import com.gmail.heagoo.apkeditor.ui.AddFolderDialog;
import com.gmail.heagoo.apkeditor.util.AndroidBug5497Workaround;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.ApkInfoParser;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.common.FileUtil;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.MD5;
import com.gmail.heagoo.common.PathUtil;
import com.gmail.heagoo.common.PreferenceUtil;
import com.gmail.heagoo.common.RandomUtil;
import com.gmail.heagoo.common.RefInvoke;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.common.ServiceUtil;
import com.gmail.heagoo.common.TextFileReader;
import com.gmail.heagoo.common.ZipUtil;
import com.gmail.heagoo.folderlist.FileRecord;
import com.gmail.heagoo.folderlist.util.OpenFiles;
import com.gmail.heagoo.httpserver.HttpServiceManager;
import com.gmail.heagoo.pngeditor.PngEditActivity;

import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import brut.androlib.res.data.ResConfigFlags;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResSpec;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.data.value.ResReferenceValue;
import brut.androlib.res.data.value.ResScalarValue;
import brut.androlib.res.data.value.ResValue;
import brut.androlib.res.xml.ResXmlEncoders;
import brut.util.Duo;
import brut.util.KXmlSerializer;
import brut.util.LanguageMapping;
import common.types.ActivityState_V1;
import common.types.ProjectInfo_V1;
import common.types.StringItem;

public class ApkInfoActivity extends CustomizedLangActivity
        implements OnItemClickListener, OnItemLongClickListener,
        IManifestChangeCallback, OnClickListener, IDecodeTaskCallback,
        OnLongClickListener, ApkParseConsumer, ResSelectionChangeListener, AddFolderDialog.AddFolderCallback {
    // To edit/view a file before replacing
    public static final int RC_OPEN_BEFORE_REPLACE = 1001;
    // To edit/view a file in external app
    public static final int RC_OPEN_EXTERNAL = 1002;
    public static final int RC_REQUEST_PERMISSION = 1003;
    // Define the request code
    private static final int RC_FILE_EDITOR = 0;
    private static final int RC_COMPOSE = 1;
    private static final int RC_SEARCH_MF = 2; // search manifest
    private static final int RC_COLOR_EDITOR = 3;
    private static final int RC_TRANSLATE = 1000;
    private static final String TMP_EDITOR_FILE = "APKEDITOR.xcrhfvke";
    protected String apkPath;
    protected String decodeRootPath; // not ends with "/"
    protected ResNavigationMgr navigationMgr;
    protected ImageView searchOptionImage;
    protected ImageView searchOptionCase;
    // Current state
    protected boolean searchTextContent = true;
    protected boolean searchResSensitive = true;
    // Record all the file entry to zip entry
    // As images are dummy, we need this info to show original image
    protected Map<String, String> fileEntry2ZipEntry;
    // Theme control
    protected int themeId;
    // Decode all files or not (all files means files include assets, libs, and unknown files)
    protected boolean isFullDecoding;
    ApkInfoParser.AppInfo apkInfo;
    // Is it OK to change to String type
    HashMap<String, ArrayList<StringItem>> allStringValues;
    Map<String, Map<String, String>> changedStringValues;
    ResListAdapter resListAdapter;
    private ImageView apkIcon;
    private TextView apkLabel;
    private TextView apkPkgPath;
    private RadioButton stringRadio;
    private RadioButton resRadio;
    private RadioButton manifestRadio;
    private Drawable textIcon;
    //HashMap<ResConfigFlags, ArrayList<StringItem>> allStringValues;
    //Map<ResConfigFlags, Map<String, String>> changedStringValues;
    //private ResConfigFlags curConfig = null; // config flag for string resource
    //private ArrayList<ResConfigFlags> langConfigList;
    private Drawable textIconGrey;
    private Drawable resIcon;
    private Drawable resIconGrey;
    private Drawable manifestIcon;
    private Drawable manifestIconGrey;
    private LinearLayout stringLayout;
    private ListView stringList;
    private StringListAdapter stringListAdapter;
    private String curConfig = null; // config flag for string resource
    private ArrayList<String> langConfigList;
    private LinearLayout resourceLayout;
    private ListView resourceList;
    private Stack<Duo<Integer, Integer>> resListPosition = new Stack<>();
    private View resSearchLayout;
    private View resMenuLayout;
    private HorizontalScrollView resNaviScrollView;
    private LinearLayout resNaviHeader;
    private View resSelectHeader;
    private TextView resSelectTip;
    private LinearLayout manifestLayout;
    private ListView manifestList;
    private ManifestListAdapter mfListAdapter;
    private LinearLayout loadingLayout;
    private View webserverMenu;
    private View rotateMenu;
    private View patchMenu;
    private Button saveBtn;
    // APK parser
    private ApkParseThread parseThread;
    // Modified String/Manifest or not
    private boolean stringModified = false;

    // For auto translating support
    // private static final String TRANSLATE_DLG_CLASS =
    // "com.gmail.heagoo.apkeditor.translate.TranslateDialog";
    // protected Object translageDlg;
    private boolean manifestModified = false;
    private boolean stringParsed = false;
    private boolean resourceParsed = false;
    private int curSelectedRadio = 0; // 0 - String, 1 - Resource, 2 - Manifest
    // For DEX decoding
    private View dexDecodeLayout;
    private ImageView dex2smaliImage;
    private ProgressBar decodeProgressBar;
    private View decodeResultLayout;
    private TextView decodeResultTitle;
    private TextView decodeResultDetail;
    private boolean dexDecoded = false;
    // Following 5 records are collected/renewed each time click at save button
    private HashMap<String, String> addedFiles;
    private HashMap<String, String> replacedFiles;
    private ArrayList<String> deletedFiles;
    private ArrayList<String> smaliFolders;
    private boolean resFileModified;
    // It may fail when first time to prepare the string, so record it
    // This is because the string may refer to the value of Android system
    private boolean bStringPrepared = false;
    // How many times the rotate button is clicked
    // Used to determine landscape or portrait
    private int rotateClickedTimes = 0;
    // ONLY used for data recovering (the state in resource list adapter)
    private String resCurrentDir; // when rotate the screen, will save and recover from it
    private Map<String, String> res_addedFiles;
    private Map<String, String> res_replacedFiles;
    private Set<String> res_deletedFiles;
    // Auto complete adapters
    private AutoCompleteAdapter strKeywordAdapter;
    private AutoCompleteAdapter resKeywordAdapter;
    private AutoCompleteAdapter mfKeywordAdapter;
    // Used to notify dex decoded watcher
    private IGeneralCallback dexDecodedCallback;
    // Params saved before activity launch
    private String savedParam_filePath;
    private String savedParam_extraStr;
    private SomethingChangedListener savedParam_listener;
    // When projectName != null, means opened from a project
    private String projectName;
    // Recorded before open a file in external editor
    private String filePathForExternal;
    private String entryNameForExternal;
    private long modifiedTimeBeforeOpen;

    // prjDirectory not ends with '/'
    public static ProjectInfo_V1 loadProject(String prjDirectory) {
        String versionPath = prjDirectory + "/.prj_version";
        String infoPath = prjDirectory + "/ae.prj";
        File versionFile = new File(versionPath);
        File prjInfoFile = new File(infoPath);
        if (versionFile.exists() && prjInfoFile.exists()) {
            try {
                TextFileReader reader = new TextFileReader(versionPath);
                int version = Integer.valueOf(reader.getContents());
                switch (version) {
                    case 1:
                        return (ProjectInfo_V1) IOUtils.readObjectFromFile(infoPath);
                    default:
                        break;
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
        return null;
    }

    // Return the smali root directory by smali entry
    // Return like "smali" "smali_class2"
    private static String getSmaliRootDir(String fileEntry) {
        int pos = fileEntry.indexOf('/');
        String folderName = null;
        if (pos != -1) {
            folderName = fileEntry.substring(0, pos);
        }
        return folderName;
    }

    // entryName is like: smali
    public static String dealWithSmaliFile(String entryName,
                                           Set<String> modifiedDexNames) {
        if ((entryName.startsWith("smali/") || entryName.startsWith("smali_"))
                && entryName.endsWith(".smali")) {
            String smaliDir = getSmaliRootDir(entryName);
            modifiedDexNames.add(smaliDir);
            return smaliDir;
        }
        return null;
    }

    // Check is the common image or not
    // For the common image, we donot need to rebuild the res
    public static boolean isCommonImage(String entryName) {
        return entryName.endsWith(".jpg") || ((entryName.endsWith(".png")
                && !entryName.endsWith(".9.png")));
    }

    // filename does not contain ".apk"
    public static String createOutputPath(String inputPath, String outputDir, String filename) {
        if (outputDir.endsWith("/")) {
            outputDir = outputDir.substring(0, outputDir.length() - 1);
        }

        String targetApkPath = outputDir + "/" + filename + ".apk";

        int position = inputPath.lastIndexOf('/');
        String inputDir = inputPath.substring(0, position);
        String inputName = inputPath.substring(position + 1);
        // Need to revise the output file name
        // signed -> signed2, signed2 -> signed3
        if (inputDir.equals(outputDir) && inputName.startsWith(filename)) {
            String strEnd = inputName.substring(filename.length());
            if (".apk".equals(strEnd)) {
                targetApkPath = outputDir + "/" + filename + "2.apk";
            } else if (strEnd.matches("[1-9][0-9]*\\.apk")) {
                String strNum = strEnd.substring(0, strEnd.length() - 4);
                try {
                    int idx = Integer.valueOf(strNum);
                    idx += 1;
                    targetApkPath = outputDir + "/" + filename + idx + ".apk";
                } catch (Exception ignored) {
                }
            }
        }

        return targetApkPath;
    }

    public static boolean generalTranslatePluginExist(Context ctx) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(null,
                "application/com.gmail.heagoo.apkeditor-translate");
        PackageManager manager = ctx.getPackageManager();
        List<ResolveInfo> infos = manager.queryIntentActivities(intent, 0);
        return (infos.size() > 0);
    }

    public static ResConfigFlags getBestConfigFlags(Set<ResConfigFlags> configs) {
        Set<String> qualifiers = new HashSet<>();
        for (ResConfigFlags configFlags : configs) {
            qualifiers.add(configFlags.getQualifiers());
        }
        String matches = getBestConfig(qualifiers);
        for (ResConfigFlags configFlags : configs) {
            if (configFlags.getQualifiers().equals(matches)) {
                return configFlags;
            }
        }
        return null;
    }

    // Get best configuration according to current locale
    public static String getBestConfig(Set<String> configs) {
        String bestConfig = null;

        Locale locale = Locale.getDefault();
        String realLang = "-" + locale.getLanguage();
        String realCountry = locale.getCountry();
        String realQualifier = realLang + "-r" + realCountry;

        LOGGER.info("*****realQualifier=" + realQualifier);

//        for (ResConfigFlags cf : configs) {
//            String quaifier = cf.getQualifiers();
        for (String quaifier : configs) {
            if (realQualifier.equals(quaifier)) {
                bestConfig = quaifier;
                break;
            }

            // Country is not set and lang is the same
            if (realLang.equals(quaifier)) {
                bestConfig = quaifier;
            }

            // Default
            else if (quaifier.equals("") && bestConfig == null) {
                bestConfig = quaifier;
            }
        }

        return bestConfig;
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_apkinfo);

        // Full screen or not
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            AndroidBug5497Workaround.assistActivity(this);
        }

        // If projectName is not null, means recover from a project
        this.projectName = ActivityUtil.getParam(getIntent(), "projectName");

        ProjectInfo_V1 prjInfo = null;
        if (projectName != null) {
            try {
                if (BuildConfig.PARSER_ONLY) {
                    this.apkPath = "";
                    this.decodeRootPath = SDCard.getRootDirectory() + "/ApkParser" + "/" + projectName;
                } else {
                    String prjRoot = SDCard.makeDir(this, ".projects");
                    prjInfo = loadProject(prjRoot + projectName);
                    this.apkPath = prjInfo.apkPath;
                    this.decodeRootPath = prjInfo.decodeRootPath;
                }
            } catch (Exception e) {
                Toast.makeText(this, R.string.cannot_load_project_info, Toast.LENGTH_LONG).show();
                this.finish();
                return;
            }
        } else {
            // Get apk path from URI (open from third-party file explorer)
            Intent intent = getIntent();
            Uri uri = intent.getData();
            if (uri != null) {
                // Request for permission
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            RC_REQUEST_PERMISSION);
                }
                // Initialize the library
                try {
                    PackageManager pm = getPackageManager();
                    ApplicationInfo moreInfo = pm.getApplicationInfo(this.getPackageName(), 0);
                    String apkPath = moreInfo.sourceDir;
                    // SaWSeM
                    MainActivity.it(this, this.getPackageName(), this.getFilesDir().getPath(), apkPath);
                } catch (Exception ignored) {
                }
                this.apkPath = com.gmail.heagoo.common.UriUtil.getAbsolutePath(this, uri);
            }
            // Get it from Intent
            if (this.apkPath == null) {
                this.apkPath = ActivityUtil.getParam(getIntent(), "apkPath");
            }
            this.decodeRootPath = ActivityUtil.getParam(getIntent(), "decodeRootPath");
            if (this.decodeRootPath == null) {
                String decodeDir = SettingActivity.getDecodeDirectory(this);
                if (decodeDir != null) {
                    this.decodeRootPath = decodeDir + "/decoded";
                } else {
                    File fileDir = this.getFilesDir();
                    String rootDirectory = fileDir.getAbsolutePath();
                    this.decodeRootPath = rootDirectory + "/decoded";
                }
            }
        }

        // Parse information from apk
        try {
            if (apkPath != null && !"".equals(apkPath)) {
                this.apkInfo = new ApkInfoParser().parse(this, apkPath);
            }
        } catch (Exception ignored) {
        }

        this.stringListAdapter = new StringListAdapter(this);
        initView();

        // If saved instance != null, do not need to parse any more
        if (savedInstanceState != null) {
            recoverFromBundle(savedInstanceState);
        } else if (projectName != null) {
            // For APK Parser
            if (BuildConfig.PARSER_ONLY) {
                loadParserProject();
            } else if (prjInfo != null && prjInfo.state != null) {
                recoverFromProject(prjInfo);
            } else {
                Toast.makeText(this, R.string.cannot_load_project_info, Toast.LENGTH_LONG).show();
                this.finish();
                return;
            }
        } else {
            this.isFullDecoding = ActivityUtil.getBoolParam(getIntent(), "isFullDecoding");
            this.parseThread = new ApkParseThread(this, this, apkPath, decodeRootPath, isFullDecoding);
            parseThread.start();
        }
    }

    // save to file, also include version file
    private boolean storeProject(String prjDirectory, ProjectInfo_V1 prjInfo) {
        String versionPath = prjDirectory + "/.prj_version";
        String infoPath = prjDirectory + "/ae.prj";
        try {
            IOUtils.writeToFile(versionPath, "1");
            return IOUtils.writeObjectToFile(infoPath, prjInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Always recover from a bundle
    private void recoverFromProject(ProjectInfo_V1 prjInfo) {
        Bundle bundle = new Bundle();
        prjInfo.state.toBundle(bundle);
        recoverFromBundle(bundle);
    }

    // Async recovering
    private void recoverFromBundle(final Bundle savedInstanceState) {
        new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
            @Override
            public void process() {
                recoverData(savedInstanceState);
            }

            @Override
            public void afterProcess() {
                recoverView();
            }
        }, -1).show();
    }

    private ActivityState_V1 saveCurrentState(String workingPath) {
        return saveCurrentState(workingPath, false, null);
    }

    // workingPath: path where to save object files, ends with "/"
    // saveAll: save big files like all strings
    // projectDir: should move temp files to this project directory; null means do nothing
    private ActivityState_V1 saveCurrentState(
            String workingPath, boolean saveAll, File projectDir) {
        ActivityState_V1 state = new ActivityState_V1();

        // Save changed string values
        Map<String, Map<String, String>>
                changedValues = stringListAdapter.getChangedValues();
        if (changedValues != null && !changedValues.isEmpty()) {
            this.stringModified = true;
            String path = workingPath + "changedStringValues";
            IOUtils.writeObjectToFile(path, changedValues);
            state.putString("changedStringValues_file", path);
        }

        // Save Resource file state (for all files decoding, do not need to save resource state)
        if (!isFullDecoding && resListAdapter != null) {
            String curDir = resListAdapter.getData(null);
            state.putString("res_current_dir", curDir);
            Map<String, String> added = resListAdapter.getAddedFiles();
            Map<String, String> replaced = resListAdapter.getReplacedFiles();
            Set<String> deleted = resListAdapter.getDeletedFiles();
            if (projectDir != null) {
                moveTempFile2ProjectDir(added, replaced, projectDir);
            }
            if (added != null && !added.isEmpty()) {
                String path = workingPath + "res_added";
                IOUtils.writeObjectToFile(path, added);
                state.putString("res_added_file", path);
            }
            if (replaced != null && !replaced.isEmpty()) {
                String path = workingPath + "res_replaced";
                IOUtils.writeObjectToFile(path, replaced);
                state.putString("res_replaced_file", path);
            }
            if (deleted != null && !deleted.isEmpty()) {
                String path = workingPath + "res_deleted";
                IOUtils.writeObjectToFile(path, deleted);
                state.putString("res_deleted_file", path);
            }
        }

        // Do not do it any more as consumes too much time (in case of rotation)
        String path = workingPath + "allStringValues";
        if (saveAll) {
            IOUtils.writeObjectToFile(path, allStringValues);
        }
        state.putString("allStringValues_file", path);

        path = workingPath + "fileEntry2ZipEntry";
        if (saveAll && !isFullDecoding) { // when all files decoded, do not need to save the mapping
            IOUtils.writeObjectToFile(path, fileEntry2ZipEntry);
        }
        state.putString("fileEntry2ZipEntry_file", path);

        state.putString("curConfig", curConfig.toString());
        state.putSerializable("langConfigList", langConfigList);
        state.putBoolean("stringModified", stringModified);
        state.putBoolean("manifestModified", manifestModified);
        state.putBoolean("stringParsed", stringParsed);
        state.putBoolean("resourceParsed", resourceParsed);
        state.putBoolean("bStringPrepared", bStringPrepared);
        state.putBoolean("searchTextContent", searchTextContent);
        state.putBoolean("searchResSensitive", searchResSensitive);
        state.putInt("curSelectedRadio", curSelectedRadio);
        state.putInt("rotateClickedTimes", rotateClickedTimes);

        state.putBoolean("dex2smaliClicked", dexDecoded);

        state.putString("savedParam_extraStr", savedParam_extraStr);
        state.putString("savedParam_filePath", savedParam_filePath);

        state.putBoolean("isFullDecoding", isFullDecoding);

        return state;
    }

    // Move temporary files to project folder, so that it will always there even with cleanup
    private void moveTempFile2ProjectDir(Map<String, String> added,
                                         Map<String, String> replaced, File projectDir) {
        String tmpFolder;
        try {
            tmpFolder = SDCard.makeWorkingDir(this);
        } catch (Exception e) {
            return;
        }

        if (added != null && !added.isEmpty()) {
            for (Map.Entry<String, String> entry : added.entrySet()) {
                String path = entry.getValue();
                if (path != null && path.startsWith(tmpFolder)) {
                    File oldFile = new File(path);
                    File newFile = new File(projectDir, oldFile.getName());
                    if (oldFile.renameTo(newFile)) {
                        entry.setValue(newFile.getPath());
                    }
                }
            }
        }
        if (replaced != null && !replaced.isEmpty()) {
            for (Map.Entry<String, String> entry : replaced.entrySet()) {
                String path = entry.getValue();
                if (path != null && path.startsWith(tmpFolder)) {
                    File oldFile = new File(path);
                    File newFile = new File(projectDir, oldFile.getName());
                    if (oldFile.renameTo(newFile)) {
                        entry.setValue(newFile.getPath());
                    }
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        try {
            String workingPath;
            if (projectName != null) {
                String prjRoot = SDCard.makeDir(this, ".projects");
                workingPath = prjRoot + projectName + "/";
            } else {
                workingPath = SDCard.makeWorkingDir(this);
            }

            ActivityState_V1 state = saveCurrentState(workingPath);
            state.toBundle(savedInstanceState);
        } catch (Exception ignored) {
        }

        super.onSaveInstanceState(savedInstanceState);
    }

    private void loadParserProject() {
        this.allStringValues = new HashMap<>();
        this.changedStringValues = null;
        this.fileEntry2ZipEntry = null;

        this.curConfig = null;
        this.langConfigList = null;
        this.stringModified = false;
        this.manifestModified = false;
        this.stringParsed = true;
        this.resourceParsed = true;
        this.bStringPrepared = true;
        this.curSelectedRadio = 1;
        this.rotateClickedTimes = 0;

        this.dexDecoded = false;
        this.isFullDecoding = true;

        recoverView();
    }

    private void recoverData(Bundle savedInstanceState) {

        String path = savedInstanceState.getString("allStringValues_file");
        this.allStringValues = (HashMap) IOUtils.readObjectFromFile(path);

        path = savedInstanceState.getString("changedStringValues_file");
        if (path != null) {
            this.changedStringValues = (HashMap) IOUtils.readObjectFromFile(path);
            mergeStrings(allStringValues, changedStringValues);
        }

        path = savedInstanceState.getString("fileEntry2ZipEntry_file");
        this.fileEntry2ZipEntry = (HashMap) IOUtils.readObjectFromFile(path);

        this.curConfig = savedInstanceState.getString("curConfig");
        this.langConfigList = (ArrayList) savedInstanceState
                .getSerializable("langConfigList");
        this.stringModified = savedInstanceState.getBoolean("stringModified");
        this.manifestModified = savedInstanceState
                .getBoolean("manifestModified");
        this.stringParsed = savedInstanceState.getBoolean("stringParsed");
        this.resourceParsed = savedInstanceState.getBoolean("resourceParsed");
        this.bStringPrepared = savedInstanceState.getBoolean("bStringPrepared");
        this.searchTextContent = savedInstanceState
                .getBoolean("searchTextContent");
        this.searchResSensitive = savedInstanceState
                .getBoolean("searchResSensitive");
        this.curSelectedRadio = savedInstanceState.getInt("curSelectedRadio");
        this.rotateClickedTimes = savedInstanceState
                .getInt("rotateClickedTimes");

        // Recover state of ResListAdapter
        this.resCurrentDir = savedInstanceState.getString("res_current_dir");
        String filePath = savedInstanceState.getString("res_added_file");
        if (filePath != null) {
            this.res_addedFiles = (Map) IOUtils.readObjectFromFile(filePath);
        }
        filePath = savedInstanceState.getString("res_replaced_file");
        if (filePath != null) {
            this.res_replacedFiles = (Map) IOUtils.readObjectFromFile(filePath);
        }
        filePath = savedInstanceState.getString("res_deleted_file");
        if (filePath != null) {
            this.res_deletedFiles = (Set) IOUtils.readObjectFromFile(filePath);
        }

        this.dexDecoded = savedInstanceState.getBoolean("dex2smaliClicked");

        savedParam_extraStr = savedInstanceState.getString("savedParam_extraStr");
        savedParam_filePath = savedInstanceState.getString("savedParam_filePath");

        isFullDecoding = savedInstanceState.getBoolean("isFullDecoding");
    }

    // Merge changed string values to allStringValues
    private void mergeStrings(Map<String, ArrayList<StringItem>> allStringValues,
                              Map<String, Map<String, String>> changedStringValues) {
        Set<Entry<String, Map<String, String>>> entries = changedStringValues.entrySet();
        for (Entry<String, Map<String, String>> entry : entries) {
            String cfgFlags = entry.getKey();
            Map<String, String> values = entry.getValue();
            for (Entry<String, String> keyValue : values.entrySet()) {
                ArrayList<StringItem> merged = allStringValues.get(cfgFlags);
                // find the key in merged record
                for (StringItem rec : merged) {
                    if (keyValue.getKey().equals(rec.name)) {
                        rec.value = keyValue.getValue();
                        break;
                    }
                }
            }
        }
    }

    private void recoverView() {
        showStringList();
        setupClickListener();
        showDecodedFileList();

        // DEX decoded or not
        if (this.dexDecoded) {
            dexDecodeLayout.setVisibility(View.GONE);
        }

        // Search text or not
        updateSearchOption();

        webserverMenu.setVisibility(View.VISIBLE);
        rotateMenu.setVisibility(View.VISIBLE);
        if (isPro()) {
            patchMenu.setVisibility(View.VISIBLE);
        } else {
            patchMenu.setVisibility(View.GONE);
        }
        if (!BuildConfig.PARSER_ONLY) {
            saveBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        HttpServiceManager.instance().unbindService(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        boolean dealed = false;

        // In resource page
        if (this.curSelectedRadio == 1) {
            if (resListAdapter == null) {
                dealed = false;
            } else if (!resListAdapter.getCheckedItems().isEmpty()) {
                resListAdapter.checkAllItems(false);
                dealed = true;
            } else if (!resListAdapter.isInRootDir()) {
//                resListAdapter.gotoRootDir();
//                navigationMgr.gotoHome();
//                resListPosition.clear();
                String oldDir = resListAdapter.getData(null);
                int idx = oldDir.lastIndexOf('/');
                String targetPath = oldDir.substring(0, idx);

                resListAdapter.openDirectory(targetPath);
                navigationMgr.gotoDirectory(targetPath);

                // Recover from the old position
                try {
                    Duo<Integer, Integer> pos = resListPosition.pop();
                    resourceList.setSelectionFromTop(pos.m1, pos.m2);
                } catch (Exception e) {
                }

                dealed = true;
            }
        }

        if (dealed) {
            return;
        }

        AlertDialog.Builder dlg = new AlertDialog.Builder(this)
                .setMessage(R.string.sure_to_exit_editing)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    if (parseThread != null && parseThread.isAlive()) {
                        parseThread.stopParse();
                        parseThread = null;
                    }
                    ApkInfoActivity.this.finish();
                })
                .setNegativeButton(R.string.no, null);
        // Not to exit the editor, when for APK Parser
        if (BuildConfig.PARSER_ONLY) {
            dlg.setMessage(R.string.sure_to_exit);
        }

        // Opened from a project
        // Do not show save button for project, any more
        if (projectName != null) {
//            dlg.setNeutralButton(R.string.save, new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    saveProject();
//                }
//            });
        }
        // Could save as project after parsing done
        else if (parseThread == null || !parseThread.isAlive()) {
//            String dirPath = SDCard.getRootDirectory() + "/ApkParser/";
//            String message = String.format(getString(R.string.sure_to_exit_tip), dirPath);
//            dlg.setMessage(message);
            dlg.setMessage(R.string.sure_to_exit);

            dlg.setNeutralButton(R.string.save_as_project, (dialog, which) -> saveAsProject());
        }
        dlg.show();
    }

    // Is already a project, save current status
    private void saveProject() {
        new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
            String prjRoot;
            ProjectInfo_V1 prjInfo;

            @Override
            public void process() {
                try {
                    prjRoot = SDCard.makeDir(ApkInfoActivity.this, ".projects");
                    prjInfo = loadProject(prjRoot + projectName);
                    ActivityState_V1 state = saveCurrentState(prjRoot + projectName + "/", true,
                            new File(prjRoot + projectName));
                    prjInfo.state = state;
                } catch (Exception e) {
                    Toast.makeText(ApkInfoActivity.this,
                            String.format(getString(R.string.general_error), e.getMessage()),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void afterProcess() {
                if (prjInfo == null) {
                    return;
                }

                boolean ret = storeProject(prjRoot + projectName, prjInfo);
                if (!ret) {
                    Toast.makeText(ApkInfoActivity.this,
                            R.string.cannot_save_project,
                            Toast.LENGTH_LONG).show();
                } else {
                    ApkInfoActivity.this.finish();
                }
            }
        }, -1).show();
    }

    // Save current decoding as project
    private void saveAsProject() {
        new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
            ActivityState_V1 state;
            File targetDir;
            File projectDir;

            String errorMessage = null;

            @Override
            public void process() {
                // Save to project files
                String projectName = "UNKNOWN";
                if (apkInfo != null) {
                    projectName = apkInfo.label;
                }

                String parentFolder;
                try {
                    parentFolder = SDCard.makeDir(ApkInfoActivity.this, ".projects");
                } catch (Exception e) {
                    errorMessage = String.format(getString(R.string.general_error), e.getMessage());
                    return;
                }

                // Locate the project folder
                String projectFolder = parentFolder + projectName;
                projectDir = new File(projectFolder);
                if (projectDir.exists()) {
                    projectDir = FileCopyDialog.getTargetNonExistFile(projectFolder, true);
                }
                boolean ret = projectDir.mkdirs();
                if (!ret) {
                    errorMessage = getString(R.string.cannot_save_project);
                    return;
                }

                // Rename decoded folder (as currently always in a fixed directory)
                String targetFolder = PathUtil.replaceNameWith(decodeRootPath, projectName);
                targetDir = new File(targetFolder);
                if (targetDir.exists()) {
                    targetDir = FileCopyDialog.getTargetNonExistFile(targetFolder, true);
                }
                ret = new File(decodeRootPath).renameTo(targetDir);
                if (!ret) {
                    errorMessage = getString(R.string.cannot_rename_decode_folder);
                    return;
                }

                // For APK Parser, do not save the state
                if (!BuildConfig.PARSER_ONLY) {
                    // Rename the file path as "decoded" changed to project name
                    resListAdapter.renamePathAsFolderRename(decodeRootPath + "/", targetDir.getPath() + "/");

                    state = saveCurrentState(projectDir.getPath() + "/", true, projectDir);
                    if (state == null) {
                        errorMessage = "Cannot save project state.";
                    }
                }
            }

            @Override
            public void afterProcess() {
                if (errorMessage != null) {
                    Toast.makeText(ApkInfoActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    return;
                }

                // For APK Parser, do not save other information
                if (BuildConfig.PARSER_ONLY) {
                    ApkInfoActivity.this.finish();
                } else {
                    // Save project info
                    ProjectInfo_V1 prjInfo = new ProjectInfo_V1();
                    prjInfo.state = state;
                    prjInfo.apkPath = ApkInfoActivity.this.apkPath;
                    prjInfo.decodeRootPath = targetDir.getPath();
                    boolean ret = storeProject(projectDir.getPath(), prjInfo);
                    if (!ret) {
                        Toast.makeText(ApkInfoActivity.this, R.string.cannot_save_project, Toast.LENGTH_LONG).show();
                    } else {
                        ApkInfoActivity.this.finish();
                    }
                }
            }
        }, -1).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        Log.d("DEBUG", "onActivityResult, requestCode=" + requestCode +
//                ", resultCode=" + resultCode);
        switch (requestCode) {
            case RC_FILE_EDITOR: // TextEditNormalActivity
            case RC_COLOR_EDITOR: // ColorXmlActivity
                if (resultCode != 0) {
                    ArrayList<String> files = data.getStringArrayListExtra("modifiedFiles");
                    if (files != null) { // in batch mode
                        for (String file : files) {
                            dealWithModifiedFile(file, null);
                        }
                        break;
                    }

                    String filePath = data.getStringExtra("xmlPath");
                    String entryName = data.getStringExtra("extraString");

                    fileModifiedIncludeTempPath(filePath, entryName);
                }
                break;
            case RC_COMPOSE: // ApkComposeActivity
                // Exit when succeed
                if (resultCode == ApkComposeActivity.SUCCEED) {
                    this.finish();
                }
                break;
            case RC_SEARCH_MF: // Open manifest in a new window, manifest search
                if (resultCode != 0) {
                    setManifestModified(true);
                }
                break;
            // No activity any more
//        case 249: // Return from SearchTextActivity
//        case 250: // Return from SearchFilenameActivity
//            if (resultCode != 0) {
//                String strBuffer = data.getStringExtra("ModifiedFiles");
//                String files[] = strBuffer.split("\n");
//                for (String filePath : files) {
//                    this.dealWithModifiedFile(filePath, null);
//                }
//            }
//            break;
            case RC_TRANSLATE: // Return from auto translate activity
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    String strQualifier = bundle.getString("targetLanguageCode");
                    String path = bundle.getString("translatedList_file");
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    List<TranslateItem> items = (List) IOUtils.readObjectFromFile(path);
                    if (items != null && !items.isEmpty()) {
                        List<StringItem> valueList = new ArrayList<>();
                        for (TranslateItem item : items) {
                            StringItem si = new StringItem(
                                    item.name, item.translatedValue);
                            valueList.add(si);
                        }
                        try {
                            this.saveTranslatedLanguage(strQualifier, valueList);
                            String strFormat = getString(R.string.save_succeed_tip);
                            String msg = String.format(strFormat, valueList.size());
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
                break;
            // Return from external file editing activity (when replace a file)
            case RC_OPEN_BEFORE_REPLACE:
                resListAdapter.replaceFile(savedParam_extraStr, savedParam_filePath);
                // Ugly code for Manifest
                if ((decodeRootPath + "/AnfroidManifest.xml").equals(savedParam_extraStr)) {
                    setManifestModified(true);
                }
                if (savedParam_listener != null) {
                    savedParam_listener.somethingChanged();
                }
                break;
            case RC_OPEN_EXTERNAL:
                long mt = new File(filePathForExternal).lastModified();
                if (mt > modifiedTimeBeforeOpen) {
                    fileModifiedIncludeTempPath(filePathForExternal, entryNameForExternal);
                }
                break;
            default:
                break;
        }
    }

    private void fileModifiedIncludeTempPath(String filePath, String entryName) {
        // To get the string removed last type
        String pathRemovedType;
        String names[] = filePath.split("/");
        int pos = names[names.length - 1].lastIndexOf('.');
        if (pos != -1) {
            int len = names[names.length - 1].length() - pos;
            pathRemovedType = filePath.substring(0, filePath.length() - len);
        } else {
            pathRemovedType = filePath;
        }

        // It is a temporary file
        // Rename it to other names, as it will be overwritten
        if (pathRemovedType != null && pathRemovedType.endsWith("/" + TMP_EDITOR_FILE)) {
            File oldFile = new File(filePath);
            String newPath = pathRemovedType.substring(0,
                    pathRemovedType.length() - TMP_EDITOR_FILE.length())
                    + RandomUtil.getRandomString(8);
            File newFile = new File(newPath);
            if (oldFile.renameTo(newFile)) {
                filePath = newPath;
            } else {
                Log.w("DEBUG", "file rename error.");
            }
        }
        dealWithModifiedFile(filePath, entryName);
    }

    // Called in onActivityResult
    protected void dealWithModifiedFile(String filePath, String entryName) {
        if (entryName == null) {
            entryName = filePath.substring(this.decodeRootPath.length() + 1);
        }

        resListAdapter.fileModified(entryName, filePath);
    }

    // The manifest has been changed
    // bInUiThread: the invoke thread is in UI thread or not
    public void setManifestModified(boolean bInUiThread) {
        this.manifestModified = true;
        if (bInUiThread) {
            mfListAdapter.reload();
        } else {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mfListAdapter.reload();
                }
            });
        }
    }

    private void initView() {
        Resources res = getResources();
        this.textIcon = res.getDrawable(R.drawable.icon_text);
        this.textIconGrey = res.getDrawable(R.drawable.icon_text_grey);
        this.resIcon = res.getDrawable(R.drawable.icon_folder);
        this.resIconGrey = res.getDrawable(R.drawable.icon_folder_grey);
        this.manifestIcon = res.getDrawable(R.drawable.icon_manifest);
        this.manifestIconGrey = res.getDrawable(R.drawable.icon_manifest_grey);

        this.apkIcon = (ImageView) this.findViewById(R.id.app_icon);
        this.apkLabel = (TextView) this.findViewById(R.id.app_name);
        this.apkPkgPath = (TextView) this.findViewById(R.id.app_pkgpath);
        this.stringRadio = (RadioButton) this.findViewById(R.id.tab_string);
        this.resRadio = (RadioButton) this.findViewById(R.id.tab_resource);
        this.manifestRadio = (RadioButton) this.findViewById(R.id.tab_manifest);
        if (isAmazonVersion()) {
            manifestRadio.setVisibility(View.GONE);
        }
        //  For APK parser, manifest view is not visiblie, and for project, it does not contain string information
        if (BuildConfig.PARSER_ONLY) {
            manifestRadio.setVisibility(View.GONE);
            if (projectName != null) {
                stringRadio.setVisibility(View.GONE);
                resRadio.setVisibility(View.GONE);
            }
        }

        // Check package name
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
                String clsName = decrypt(
                        "4\u003b1\u0027\u003a\u003c1\u007b6\u003a\u003b\u00210\u003b\u0021\u007b\u0016\u003a\u003b\u00210\u002d\u0021");
                String name = "get" + decrypt("\u000546\u003e420\u001b480");
                String pkgName = (String) RefInvoke.invokeMethod(clsName, name,
                        ApkInfoActivity.this, null, null);
                String md5 = MD5.stringMD5(pkgName);
                // APK Editor, APK Editor Pro, APK Parser
                if (!"9bde489fb18d43e8729607528f1e9d52".equals(md5)
                        && !"02bd0fffef96bcd282ba1ba6edfe7beb".equals(md5)
                        && !"d5513dd1c5e8f14e78e94fa3f5da7e62".equals(md5)) {
                    killMyself(pkgName);
                }
            }

            // Only kill me when install time longer than 3 days
            private void killMyself(String pkgName) {
                try {
                    PackageManager pm = ApkInfoActivity.this
                            .getPackageManager();
                    PackageInfo packageInfo = pm.getPackageInfo(pkgName, 0);
                    long lastUpdateTime = packageInfo.lastUpdateTime;
                    if (System.currentTimeMillis() - lastUpdateTime < 3 * 24 * 3600L * 1000) {
                        return;
                    }
                } catch (NameNotFoundException ignored) {
                }

                String clsName = decrypt(
                        "4\u003b1\u0027\u003a\u003c1\u007b\u003a\u0026\u007b\u0005\u0027\u003a60\u0026\u0026");
                int mypid = (Integer) RefInvoke.invokeStaticMethod(clsName,
                        decrypt("8\u002c\u0005\u003c1"), null, null);
                RefInvoke.invokeStaticMethod(clsName,
                        decrypt("\u003e\u003c99\u0005\u0027\u003a60\u0026\u0026"),
                        new Class<?>[]{int.class}, new Object[]{mypid});
            }

            private String decrypt(String str) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < str.length(); i++) {
                    sb.append((char) (str.charAt(i) ^ 0x55));
                }
                return sb.toString();
            }
        }.start();

        // Show not support
        if (BuildConfig.IS_PRO) {
            this.findViewById(R.id.tv_not_support).setVisibility(View.GONE);
            this.findViewById(R.id.layout_search_mf).setVisibility(View.VISIBLE);
            setupMfSearch();
        } else {
            this.findViewById(R.id.tv_not_support).setVisibility(View.VISIBLE);
            this.findViewById(R.id.layout_search_mf).setVisibility(View.GONE);
        }

        this.stringLayout = (LinearLayout) this
                .findViewById(R.id.string_layout);
        this.stringList = (ListView) this.findViewById(R.id.string_list);
        this.resourceLayout = (LinearLayout) this
                .findViewById(R.id.resource_layout);
        this.resourceList = (ListView) this.findViewById(R.id.resource_list);
        this.findViewById(R.id.menu_search_res).setOnClickListener(this);
        this.resSearchLayout = findViewById(R.id.res_search_layout);
        this.resMenuLayout = findViewById(R.id.res_menu_layout);
        this.resNaviScrollView = (HorizontalScrollView) findViewById(
                R.id.res_navi_scrollView);
        this.resNaviHeader = (LinearLayout) findViewById(
                R.id.res_header_navigation);
        this.resSelectHeader = findViewById(R.id.res_header_selection);
        this.navigationMgr = new ResNavigationMgr(this, this.decodeRootPath,
                resNaviHeader, resNaviScrollView);
        this.resSelectTip = (TextView) findViewById(R.id.selection_tip);

        this.manifestLayout = (LinearLayout) this
                .findViewById(R.id.manifest_layout);
        this.manifestList = (ListView) this.findViewById(R.id.manifest_list);
        this.loadingLayout = (LinearLayout) this
                .findViewById(R.id.layout_loading);

        if (apkInfo != null) {
            apkIcon.setImageDrawable(apkInfo.icon);
            apkLabel.setText(apkInfo.label);
            apkPkgPath.setText(apkInfo.packageName);
        } else {
            if (projectName != null) {
                apkIcon.setImageResource(R.drawable.apk_icon);
                apkLabel.setText(projectName);
            } else {
                apkIcon.setImageResource(R.drawable.parse_error);
                apkLabel.setText("UNKNOWN");
            }
            apkPkgPath.setVisibility(View.GONE);
        }

        stringList.setAdapter(stringListAdapter);
        stringList.setOnItemClickListener(stringListAdapter);

        // DEX/Smali decoding
        this.dexDecodeLayout = findViewById(R.id.dex_decode_layout);
        if (SettingActivity.isDex2smaliEnabled(this)) {
            this.dex2smaliImage = (ImageView) findViewById(
                    R.id.imageview_dex2smali);
            this.dex2smaliImage.setOnClickListener(this);
            this.dex2smaliImage.setOnLongClickListener(this);
            this.decodeProgressBar = (ProgressBar) findViewById(
                    R.id.progressbar_dex2smali);
            this.decodeResultLayout = findViewById(R.id.decode_result_layout);
            this.decodeResultTitle = (TextView) findViewById(
                    R.id.decode_result_title);
            this.decodeResultDetail = (TextView) findViewById(
                    R.id.decode_result_detail);
            findViewById(R.id.down_arrow_container).setOnClickListener(this);
        } else {
            this.dexDecodeLayout.setVisibility(View.GONE);
        }

        // File search option
        this.searchOptionImage = (ImageView) findViewById(
                R.id.imageview_text_check);
        this.searchOptionCase = (ImageView) findViewById(
                R.id.imageview_insensitive_check);
        updateSearchOption();

        // keyword auto complete
        AutoCompleteTextView editView = (AutoCompleteTextView) findViewById(
                R.id.keyword_edit);
        this.strKeywordAdapter = new AutoCompleteAdapter(
                getApplicationContext(), "string_keywords");
        editView.setAdapter(strKeywordAdapter);
        editView = (AutoCompleteTextView) findViewById(R.id.et_res_keyword);
        this.resKeywordAdapter = new AutoCompleteAdapter(
                getApplicationContext(), "res_keywords");
        editView.setAdapter(resKeywordAdapter);
        editView = (AutoCompleteTextView) findViewById(R.id.mf_keyword);
        this.mfKeywordAdapter = new AutoCompleteAdapter(
                getApplicationContext(), "mf_keywords");
        editView.setAdapter(mfKeywordAdapter);
    }

    private void updateSearchOption() {
        int txtId = -1;
        int caseId = -1;
        if (GlobalConfig.instance(this).isDarkTheme()) {
            txtId = searchTextContent ? R.drawable.searchtxt_checked_white
                    : R.drawable.searchtxt_unchecked_white;
            caseId = searchResSensitive ? R.drawable.ic_case_sensitive_white
                    : R.drawable.ic_case_insensitive_white;
        } else {
            txtId = searchTextContent ? R.drawable.searchtxt_checked
                    : R.drawable.searchtxt_unchecked;
            caseId = searchResSensitive ? R.drawable.ic_case_sensitive
                    : R.drawable.ic_case_insensitive;
        }
        searchOptionImage.setImageResource(txtId);
        searchOptionCase.setImageResource(caseId);

    }

    private boolean isAmazonVersion() {
        // Check is google version or not
        try {
            ApplicationInfo appInfo = this.getPackageManager()
                    .getApplicationInfo(getPackageName(),
                            PackageManager.GET_META_DATA);
            String channel = appInfo.metaData
                    .getString("com.gmail.heagoo.publish_channel");
            return "amazon".equals(channel);
        } catch (Exception e) {
            // e.printStackTrace();
        }

        return false;
    }

    // Setup the manifest search button
    private void setupMfSearch() {
        Button btn = (Button) this.findViewById(R.id.btn_search_mf);
        btn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        // Search a keyword in all strings
        if (id == R.id.search_button) {
            EditText keywordEt = (EditText) this
                    .findViewById(R.id.keyword_edit);
            String keyword = keywordEt.getText().toString();
            keyword = keyword.trim();
            if (!keyword.equals("")) {
                searchStringByKeyword(keyword);
                this.strKeywordAdapter.addInputHistory(keyword);
            } else {
                // Reset the string list
                updateStringList();
            }

            // Hide Input Method
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(
                    ApkInfoActivity.this.getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
        // Search keyword in AndroidManifest.xml
        else if (id == R.id.btn_search_mf) {
            EditText et = (EditText) this.findViewById(R.id.mf_keyword);
            String keyword = et.getText().toString();
            keyword = keyword.trim();
            if (keyword.equals("")) {
                Toast.makeText(this, R.string.empty_input_tip,
                        Toast.LENGTH_SHORT).show();
            } else {
                this.mfKeywordAdapter.addInputHistory(keyword);
                ArrayList<Integer> lines = new ArrayList<>();
                ArrayList<String> lineContents = new ArrayList<>();
                searchManifest(keyword, lines, lineContents);
                if (lines.isEmpty()) {
                    Toast.makeText(this, R.string.notfound_in_manifest,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(this, MfSearchRetActivity.class);
                    Bundle bundle = new Bundle();
                    String xmlPath = decodeRootPath + "/AndroidManifest.xml";
                    bundle.putString("xmlPath", xmlPath);
                    bundle.putIntegerArrayList("lineIndexs", lines);
                    bundle.putStringArrayList("lineContents", lineContents);
                    intent.putExtras(bundle);
                    startActivityForResult(intent, RC_SEARCH_MF);
                }
            }
        }

        // Search in resource
        else if (id == R.id.menu_search_res) {
            EditText et = (EditText) this.findViewById(R.id.et_res_keyword);
            String keyword = et.getText().toString();
            keyword = keyword.trim();
            if (keyword.equals("")) {
                Toast.makeText(this, R.string.empty_input_tip,
                        Toast.LENGTH_SHORT).show();
            } else if (resListAdapter != null) {
                // Collect all the file names
                ArrayList<String> filenameList = new ArrayList<>();
                ArrayList<FileRecord> records = new ArrayList<>();
                String curFolder = resListAdapter.getData(records);
                if (!"..".equals(records.get(0).fileName)) {
                    filenameList.add(records.get(0).fileName);
                }
                for (int i = 1; i < records.size(); ++i) {
                    filenameList.add(records.get(i).fileName);
                }
                boolean bSearchFilename = !this.searchTextContent;
                searchInResourceFiles(keyword, curFolder, filenameList,
                        bSearchFilename, this.searchResSensitive);
            }
        }

        // Image of dex2smali
        else if (id == R.id.imageview_dex2smali) {
            boolean showed = PreferenceUtil.getBoolean(this, "smali_license_showed", false);
            if (!showed) {
                new SmaliNoticeDialog(this).show();
            }
            decodeDex(null);
        }

        // Hide the smali decoding result
        else if (id == R.id.down_arrow_container) {
            this.dexDecodeLayout.setVisibility(View.GONE);
        }

        // Rotate the view
        else if (id == R.id.menu_rotate) {
            this.rotateClickedTimes += 1;
            if ((this.rotateClickedTimes % 2) == 1) {
                this.setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                this.setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
        }

        // Start web server
        else if (id == R.id.menu_webserver) {
            HttpServiceManager.instance().startWebService(this, decodeRootPath);
        }

        // Apply a patch
        else if (id == R.id.menu_apply_patch) {
            new PatchDialog(this).show();
        }

        // Auto translate
        else if (id == R.id.translate) {
            startNewTranslation();
        }
    }

    // Do resource search
    // Search in all the files/folder (specified by fileList)
    protected void searchInResourceFiles(String keyword, String directory,
                                         ArrayList<String> filenameList, boolean bSearchFilename,
                                         boolean caseSensitive) {

        if (this.resKeywordAdapter != null) {
            resKeywordAdapter.addInputHistory(keyword);
        }

        if (bSearchFilename) {
            SearchFilenameDialog dlg = new SearchFilenameDialog(this, directory,
                    filenameList, keyword, caseSensitive);
            dlg.show();
        } else {
            new SearchTextDialog(
                    this, directory, filenameList, keyword, caseSensitive).show();
        }
    }

    // Return line NO and line content
    private void searchManifest(String keyword, List<Integer> lineIndexs,
                                List<String> lineContents) {
        String filePath = decodeRootPath + "/AndroidManifest.xml";
        try {
            FileInputStream fis = new FileInputStream(filePath);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            String line = br.readLine();
            int index = 1;
            while (line != null) {
                if (line.contains(keyword)) {
                    lineIndexs.add(index);
                    lineContents.add(line);
                }
                line = br.readLine();
                index += 1;
            }

            br.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void setupClickListener() {
        stringRadio.setOnClickListener(v -> stringRadioClicked());
        resRadio.setOnClickListener(v -> resRadioClicked());
        manifestRadio.setOnClickListener(v -> manifestRadioClicked());

        this.saveBtn = (Button) this.findViewById(R.id.save_button);
        if (BuildConfig.PARSER_ONLY) {
            this.saveBtn.setVisibility(View.GONE);
        } else {
            saveBtn.setOnClickListener(v -> composeApkFile());
        }

        this.webserverMenu = this.findViewById(R.id.menu_webserver);
        this.rotateMenu = this.findViewById(R.id.menu_rotate);
        webserverMenu.setOnClickListener(this);
        rotateMenu.setOnClickListener(this);
        this.patchMenu = this.findViewById(R.id.menu_apply_patch);
        if (this.isPro()) {
            patchMenu.setOnClickListener(this);
        } else {
            patchMenu.setVisibility(View.GONE);
        }
    }

    private boolean isPro() {
        return BuildConfig.IS_PRO;
    }

    private void collectAndSaveChangedString() {
        // Collect changes
        Map<String, Map<String, String>>
                changedValues = stringListAdapter.getChangedValues();
        if (changedValues != null && !changedValues.isEmpty()) {
            this.stringModified = true;
            try {
                saveStringValues(changedValues);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void composeApkFile() {
        if (BuildConfig.LIMIT_NEW_VERSION && !MainActivity.upgradedFromOldVersion(this)) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle(R.string.please_note);
            alert.setMessage(R.string.build_not_support_tip);
            alert.show();
            return;
        }

        collectAndSaveChangedString();

        if (SettingActivity.isRebuildConfirmEnabled(this)) {
            Map<String, String> added = resListAdapter.getAddedFiles();
            Map<String, String> replaced = resListAdapter.getReplacedFiles();
            Set<String> deleted = resListAdapter.getDeletedFiles();
            new RebuildConfirmDialog(this, stringModified, manifestModified,
                    added, replaced, deleted).show();
        } else {
            build(true);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Add or import a folder, dialog callback functions

    private void saveStringValues(Map<String, Map<String, String>> allChangedValues)
            throws Exception {
        Set<Entry<String, Map<String, String>>> entries = allChangedValues.entrySet();
        for (Entry<String, Map<String, String>> entry : entries) {
//            ResConfigFlags cfgFlags = entry.getKey();
//            String qualifier = cfgFlags.getQualifiers();
            String qualifier = entry.getKey();
            // The value in allStringValues is already changed
            //saveStringResource(qualifier, allStringValues.get(qualifier));
            modifyStringResource(qualifier, entry.getValue());
        }
    }

    protected void build(final boolean bSign) {
        this.resFileModified = false;
        Set<String> modifiedDex = new HashSet<>();

        long resModifyTime = FileUtil.recursiveModifiedTime(new File(decodeRootPath + "/res"));
        Log.d("DEBUG", "resModifyTime=" + resModifyTime);
        long manifestModifyTime = FileUtil.recursiveModifiedTime(new File(decodeRootPath + "/AndroidManifest.xml"));
        Log.d("DEBUG", "manifestTime=" + manifestModifyTime);

        // Collect modified files
        Map<String, String> added = resListAdapter.getAddedFiles();
        Map<String, String> replaced = resListAdapter.getReplacedFiles();
        Set<String> deleted = resListAdapter.getDeletedFiles();

        this.addedFiles = new HashMap<>();
        this.replacedFiles = new HashMap<>();
        this.deletedFiles = new ArrayList<>();

        // Enumerate added files
        for (Entry<String, String> entry : added.entrySet()) {
            String entryName = entry.getKey();
            // Resource file
            if (entryName.startsWith("res/")) {
                if (!resFileModified && !isCommonImage(entryName)) {
                    resFileModified = true;
                }
                addedFiles.put(entry.getKey(), entry.getValue());
            }
            // General other files
            else if (dealWithSmaliFile(entryName, modifiedDex) == null) {
                addedFiles.put(entry.getKey(), entry.getValue());
            }
        }

        // Enumerate modified files
        for (Entry<String, String> entry : replaced.entrySet()) {
            String entryName = entry.getKey();
            // Resource file
            if (entryName.startsWith("res/")) {
                if (!resFileModified && !isCommonImage(entryName)) {
                    resFileModified = true;
                }
                replacedFiles.put(entry.getKey(), entry.getValue());
            }
            // General other files
            else if (dealWithSmaliFile(entryName, modifiedDex) == null) {
                replacedFiles.put(entry.getKey(), entry.getValue());
            }
        }

        // Enumerate deleted files
        for (String entryName : deleted) {
            // Resource file
            if (entryName.startsWith("res/")) {
                if (!resFileModified && !isCommonImage(entryName)) {
                    resFileModified = true;
                }
                deletedFiles.add(entryName);
            }
            // General other files
            else if (dealWithSmaliFile(entryName, modifiedDex) == null) {
                deletedFiles.add(entryName);
            }
        }

        // Collect modified smali folders
        this.smaliFolders = new ArrayList<>();
        for (String folder : modifiedDex) {
            smaliFolders.add(folder);
        }

        // Need to rebuild the resource, check known issues first
        if (stringModified || manifestModified || resFileModified) {
            String errMsg = getKnownResourceError();
            if (errMsg != null) {
                AlertDialog.Builder dlg = new AlertDialog.Builder(this);
                dlg.setTitle(R.string.warning);
                dlg.setMessage(errMsg + "\nAre you sure to continue?");
                dlg.setPositiveButton(R.string.yes,
                        (dialog, which) -> launchBuildActivityAndService(bSign));
                dlg.setNegativeButton(android.R.string.cancel, null);
                dlg.show();
                return;
            }
        }

        launchBuildActivityAndService(bSign);
    }

    ////////////////////////////////////////////////////////////////////////////////

    private String getKnownResourceError() {
        // Check AndroidManifest: <provider android:authorities="@string" not
        // allowed
        // Removed at 20170303
        String filepath = this.decodeRootPath + "/AndroidManifest.xml";
        BufferedReader br = null;
        try {
            br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filepath)));
            int lineIndex = 1;
            String line = br.readLine();
            while (line != null) {
                if (line.trim().startsWith("<provider")) {
                    if (line.contains("android:authorities=\"@string")) {
                        return "AndroidManifest.xml(Line " + lineIndex
                                + "): The provider authorities reference to a @string value. It may not work.";
                    }
                }
                line = br.readLine();
                lineIndex += 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    private void showCannotStartBuildDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage(R.string.build_in_progress_tip)
                .setTitle(R.string.please_note)
                .setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    @Override
    public void addFolder(String folderName) {
        final String dirPath = resListAdapter.getData(null);
        resListAdapter.addFolder(dirPath, folderName);
    }

    @Override
    public void importFolder(final String folderPath) {
        final String dirPath = resListAdapter.getData(null);
        new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
            private String errorMessage = null;

            @Override
            public void process() {
                File srcDir = new File(folderPath);
                String folderName = srcDir.getName();
                File curDir = new File(dirPath);
                File targetDir = new File(curDir, folderName);
                if (targetDir.exists()) {
                    String fmt = getString(R.string.file_already_exist);
                    this.errorMessage = String.format(fmt, folderName);
                } else {
                    targetDir.mkdir();
                    IOUtils.copy(targetDir, srcDir);
                }
            }

            @Override
            public void afterProcess() {
                if (errorMessage != null) {
                    Toast.makeText(ApkInfoActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                } else {
                    resListAdapter.refresh();
                }
            }
        }, -1).show();
    }

    // First check if the build is still ongoing
    private void launchBuildActivityAndService(boolean bSign) {
        // If the service is running, then check the build still ongoing or not
        if (ServiceUtil.isMyServiceRunning(this, ApkComposeService.class)) {
            Intent intent = new Intent(this, ApkComposeService.class);
            bindService(intent, new MyServiceConnection(bSign), Context.BIND_AUTO_CREATE);
            return;
        }

        launchWithoutCheck(bSign);
    }

    private void launchWithoutCheck(boolean bSign) {
        // int pos = apkPath.lastIndexOf('/');
        // String apkName = apkPath.substring(pos + 1);
        String outputDir = SDCard.getRootDirectory() + "/ApkEditor";
        File outputDirFile = new File(outputDir);
        if (!outputDirFile.exists()) {
            outputDirFile.mkdir();
        }

        int outputApkRule = SettingActivity.getOutputApkRule(this);
        String filename;
        switch (outputApkRule) {
            case 0:
                filename = (bSign && BuildConfig.WITH_SIGN) ? apkInfo.packageName + "_signed" :
                        apkInfo.packageName + "_unsigned";
                break;
            case 2:
                filename = (bSign && BuildConfig.WITH_SIGN) ? apkInfo.label + "_signed" :
                        apkInfo.label + "_unsigned";
                break;
            default:
                filename = (bSign && BuildConfig.WITH_SIGN) ? "gen_signed" : "gen_unsigned";
                break;
        }
        filename = FileUtil.reviseFileName(filename);

        String targetApkPath = createOutputPath(apkPath, outputDir, filename);

        Intent intent = new Intent(this, ApkComposeService.class);
        ActivityUtil.attachParam(intent, "decodeRootPath", decodeRootPath);
        // For full decoding, do NOT pass apkPath, so the builder will know that situation
        if (!this.isFullDecoding) {
            ActivityUtil.attachParam(intent, "srcApkPath", apkPath);
        }
        ActivityUtil.attachParam(intent, "targetApkPath", targetApkPath);
        ActivityUtil.attachParam(intent, "stringModified",
                stringModified ? "true" : "false");
        ActivityUtil.attachParam(intent, "manifestModified",
                manifestModified ? "true" : "false");
        ActivityUtil.attachParam(intent, "resFileModified",
                resFileModified ? "true" : "false");
        ActivityUtil.attachParam(intent, "modifiedSmaliFolders", smaliFolders);
        ActivityUtil.attachParam(intent, "addedFiles", addedFiles);
        ActivityUtil.attachParam(intent, "deletedFiles", deletedFiles);
        ActivityUtil.attachParam(intent, "replacedFiles", replacedFiles);
        ActivityUtil.attachBoolParam(intent, "signAPK", bSign);

        // It is too big to pass it to another activity
        // So we save it to file
        String mapFileName = serialize2File(fileEntry2ZipEntry);
        ActivityUtil.attachParam(intent, "fileEntry2ZipEntry", mapFileName);

        startService(intent);

        // Start activity (note: this is activity, not service)
        {
            Intent it = new Intent(this, ApkComposeActivity.class);
            startActivityForResult(it, RC_COMPOSE);
        }
    }

    // Write the map structure to file
    // Return the file name
    private String serialize2File(Map<String, String> fileEntry2ZipEntry2) {
        try {
            String filepath = SDCard.makeWorkingDir(this)
                    + RandomUtil.getRandomString(8);
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(filepath));
            for (Entry<String, String> entry : fileEntry2ZipEntry2
                    .entrySet()) {
                bos.write(entry.getKey().getBytes());
                bos.write('\n');
                bos.write(entry.getValue().getBytes());
                bos.write('\n');
            }
            bos.close();
            return filepath;
        } catch (Exception ignored) {
        }
        return null;
    }

    // Update the view in the center of the screen
    private void updateCenterView() {

        manifestRadio.setCompoundDrawablesWithIntrinsicBounds(null,
                manifestIconGrey, null, null);
        resRadio.setCompoundDrawablesWithIntrinsicBounds(null, resIconGrey,
                null, null);
        stringRadio.setCompoundDrawablesWithIntrinsicBounds(null, textIconGrey,
                null, null);

        loadingLayout.setVisibility(View.INVISIBLE);
        manifestLayout.setVisibility(View.INVISIBLE);
        stringLayout.setVisibility(View.INVISIBLE);
        resourceLayout.setVisibility(View.INVISIBLE);

        switch (curSelectedRadio) {
            case 0:
                stringRadio.setCompoundDrawablesWithIntrinsicBounds(null, textIcon,
                        null, null);
                if (this.stringParsed) {
                    stringLayout.setVisibility(View.VISIBLE);
                } else {
                    loadingLayout.setVisibility(View.VISIBLE);
                }
                break;
            case 1:
                resRadio.setCompoundDrawablesWithIntrinsicBounds(null, resIcon,
                        null, null);
                if (this.resourceParsed) {
                    resourceLayout.setVisibility(View.VISIBLE);
                } else {
                    loadingLayout.setVisibility(View.VISIBLE);
                }
                break;
            case 2:
                manifestRadio.setCompoundDrawablesWithIntrinsicBounds(null,
                        manifestIcon, null, null);
                if (this.resourceParsed) {
                    manifestLayout.setVisibility(View.VISIBLE);
                } else {
                    loadingLayout.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    protected void manifestRadioClicked() {
        if (this.curSelectedRadio != 2) {
            this.curSelectedRadio = 2;
            updateCenterView();
        }
    }

    protected void resRadioClicked() {
        // Already in current page
        if (this.curSelectedRadio == 1) {
            return;
        }

        this.curSelectedRadio = 1;
        updateCenterView();

        if (!dexDecoded) {
            String key = "HideSmaliMsgShown";
            SharedPreferences sp = PreferenceManager
                    .getDefaultSharedPreferences(this);
            int showTimes = sp.getInt(key, 0);
            if (showTimes < 1) {
                if (SettingActivity.isDex2smaliEnabled(this)) {
                    Toast.makeText(this, R.string.hide_smali_tip,
                            Toast.LENGTH_LONG).show();
                    Editor editor = sp.edit();
                    editor.putInt(key, showTimes + 1);
                    editor.apply();
                }
            }
        }
    }

    protected void stringRadioClicked() {
        if (this.curSelectedRadio != 0) {
            this.curSelectedRadio = 0;
            updateCenterView();
        }
    }

    // Called when finished resource table decoding
    public void resTableDecoded(boolean ret) {
        if (ret) {
            this.bStringPrepared = prepareStringList();
            if (bStringPrepared) {
                this.runOnUiThread(() -> {
                    showStringList();
                    setupClickListener();
                });
            }
        } else {
            // parseThread
        }
    }

    public void resourceDecoded(final Map<String, String> fileEntry2ZipEntry) {
        this.fileEntry2ZipEntry = fileEntry2ZipEntry;

        // Save the big structure to file
        // So that we do not need to save it every time in onSaveInstanceState
        new Thread() {
            @Override
            public void run() {
                try {
                    String workingPath = SDCard
                            .makeWorkingDir(ApkInfoActivity.this);
                    String path = workingPath + "allStringValues";
                    IOUtils.writeObjectToFile(path, allStringValues);
                    path = workingPath + "fileEntry2ZipEntry";
                    IOUtils.writeObjectToFile(path, fileEntry2ZipEntry);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        // To check whether need to re-show the string list
        final boolean stringShowNeeded = !this.bStringPrepared;
        if (!this.bStringPrepared) {
            this.bStringPrepared = prepareStringList();
        }

        this.runOnUiThread(() -> {
            if (stringShowNeeded) {
                showStringList();
                setupClickListener();
            }
            showDecodedFileList();
            webserverMenu.setVisibility(View.VISIBLE);
            rotateMenu.setVisibility(View.VISIBLE);
            if (isPro()) {
                patchMenu.setVisibility(View.VISIBLE);
            } else {
                patchMenu.setVisibility(View.GONE);
            }
            if (!BuildConfig.PARSER_ONLY) {
                saveBtn.setVisibility(View.VISIBLE);
            }
        });
    }

    public void decodeDex(IGeneralCallback dexDecodedCallback) {
        this.dexDecodedCallback = dexDecodedCallback;
        new AsyncDecodeTask(this, this.apkPath, decodeRootPath, this).execute();
        this.dexDecoded = true;
    }

    public void decodeFailed(final String errMessage) {
        this.runOnUiThread(() -> Toast.makeText(ApkInfoActivity.this, errMessage,
                Toast.LENGTH_LONG).show());
    }

    // Collect all the string information
    // It may consume a lot of time, as it may need to decode android system
    // package
    private boolean prepareStringList() {
        allStringValues = new HashMap<>();
        // Set<ResConfigFlags> allConfigs = new HashSet<ResConfigFlags>();

        ResPackage pkg = parseThread.getApkPackage();
        if (pkg == null) {
            return false;
        }
        List<ResResSpec> specList = pkg.listResSpecs();
        for (ResResSpec spec : specList) {

            // String type
            if ("string".equals(spec.getType().getName())) {
                String name = spec.getName();
                String value;

                Map<ResConfigFlags, ResResource> resources = spec.getAllResources();
                for (Entry<ResConfigFlags, ResResource> entry : resources.entrySet()) {
                    String styledValue = null;
                    ResConfigFlags configFlag = entry.getKey();
                    ResResource resResource = entry.getValue();
                    ResValue resVal = resResource.getValue();
                    // String like @string/xxx
                    if (resVal instanceof ResReferenceValue) {
                        value = resVal.toString();
                    } else if (resVal instanceof ResScalarValue) {
                        ResScalarValue scalarVal = ((ResScalarValue) resVal);
                        if (scalarVal.isStyled()) {
                            styledValue = scalarVal.getHtmlValue();
                        }
                        value = scalarVal.getRawValue();
                        if (value == null) {
                            value = resVal.toString();
                        }
                    } else {
                        value = resVal.toString();
                    }
                    StringItem item = new StringItem(name, value, styledValue);
                    String qualifier = configFlag.getQualifiers();
                    ArrayList<StringItem> stringValueList = allStringValues.get(qualifier);
                    if (stringValueList == null) {
                        stringValueList = new ArrayList<>();
                        allStringValues.put(qualifier, stringValueList);
                    }
                    stringValueList.add(item);
                }
            }
        }
        return true;
    }

    private void showStringList() {
        // Set the changed values to make it recover the previous state (for rotation)
        if (stringListAdapter != null && changedStringValues != null) {
            stringListAdapter.setChangedValues(changedStringValues);
        }

        // Setup listener for adding a button
        initAddLanguageBtn();
        initTranslateBtn();

        // Update string list
        // Do not need to update as initSpinner will setup the string list?
        // updateStringList();

        // Spinner
        initSpinner();

        // Searcher
        Button searchBtn = (Button) this.findViewById(R.id.search_button);
        searchBtn.setOnClickListener(this);

        this.stringParsed = true;

        updateCenterView();
    }

    private void initAddLanguageBtn() {
        ImageView iv = (ImageView) this.findViewById(R.id.add_language);
        iv.setOnClickListener(v -> {
            LanguageSelectDialog dlg = new LanguageSelectDialog(
                    ApkInfoActivity.this, null, null);
            dlg.show();
        });
    }

    // Set the click listener for translate button
    private void initTranslateBtn() {
        ImageView iv = (ImageView) this.findViewById(R.id.translate);

        if (generalTranslatePluginExist(this)) {
            iv.setOnClickListener(this);
        } else if (BuildConfig.IS_PRO
                && proTranslatePluginExist()) {
            iv.setOnClickListener(this);
        } else {
            iv.setOnClickListener(v -> new AboutPluginDialog(ApkInfoActivity.this).show());
        }
    }

    private boolean proTranslatePluginExist() {
        try {
            this.getPackageManager().getApplicationInfo("apkeditor.translate", 0);
            return true;
        } catch (NameNotFoundException ignored) {
        }
        return false;
    }

    // Create a ResConfigFlags
    // Please note qualifier always starts with '-'
    public ResConfigFlags createConfigFlags(String qualifier) {
        int pos = qualifier.indexOf("-r", 1);
        if (pos != -1 && pos + 3 < qualifier.length()) {
            return new ResConfigFlags(qualifier.charAt(1), qualifier.charAt(2),
                    qualifier.charAt(pos + 2), qualifier.charAt(pos + 3));
        } else {
            return new ResConfigFlags(qualifier.charAt(1), qualifier.charAt(2));
        }
    }

    // Start a new translation, it may be called by the translation dialog
    // Show the language selection dialog
    public void startNewTranslation() {
        LanguageSelectDialog dlg = new LanguageSelectDialog(
                ApkInfoActivity.this, PossibleLanguages.languages,
                PossibleLanguages.codes);
        dlg.setTitle(R.string.select_target_lang);
        dlg.show();
    }

    // Save Translated strings
    public void saveTranslatedLanguage(String qualifier,
                                       List<StringItem> stringValues) throws Exception {
        this.saveStringResource(qualifier, stringValues);
        this.stringModified = true;

        // add to the map structure so that the UI can see it
        String targetConfig = null;
        for (Entry<String, ArrayList<StringItem>> entry : allStringValues.entrySet()) {
            String q = entry.getKey();
            //String q = configFlag.getQualifiers();
            if (q.equals(qualifier)) {
                targetConfig = q;
                ArrayList<StringItem> stringList = entry.getValue();
                stringList.clear();
                stringList.addAll(stringValues);
            }
        }

        // The language does not exist at all
        if (targetConfig == null) {
            targetConfig = qualifier;
            ArrayList<StringItem> stringList = new ArrayList<>();
            stringList.addAll(stringValues);
            allStringValues.put(targetConfig, stringList);
        }

        resetSpinner(); // Reset the spinner
    }

    // Show the auto translation dialog (When language selection is completed)
    public void translateLanguage(String strQualifier) {
        // List<TranslateItem> translateList = prepareTranslateItems("-zh-rCN");
        List<TranslateItem> translatedList = new ArrayList<>();
        List<TranslateItem> untranslatedList = new ArrayList<>();
        prepareTranslateItems(strQualifier, translatedList, untranslatedList);

        try {
            Intent intent;
            if (generalTranslatePluginExist(this)) {
                intent = new Intent("android.intent.action.VIEW");
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setDataAndType(null,
                        "application/com.gmail.heagoo.apkeditor-translate");
            } else {
                ComponentName componetName = new ComponentName(
                        "apkeditor.translate",
                        "apkeditor.translate.TranslateActivity");
                intent = new Intent();
                intent.setComponent(componetName);
            }

            Bundle bundle = new Bundle();
            bundle.putBoolean("isFullScreen",
                    GlobalConfig.instance(this).isFullScreen());
            bundle.putBoolean("isDark",
                    GlobalConfig.instance(this).isDarkTheme());
            {
                String translatedFile = SDCard.makeWorkingDir(this)
                        + "translated";
                IOUtils.writeObjectToFile(translatedFile, translatedList);
                bundle.putString("translatedList_file", translatedFile);
            }
            {
                String untranslatedFile = SDCard.makeWorkingDir(this)
                        + "untranslatedList";
                IOUtils.writeObjectToFile(untranslatedFile, untranslatedList);
                bundle.putString("untranslatedList_file", untranslatedFile);
            }
            bundle.putString("targetLanguageCode", strQualifier);
            intent.putExtras(bundle);

            startActivityForResult(intent, RC_TRANSLATE);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void prepareTranslateItems(String qualifier,
                                       List<TranslateItem> translatedList,
                                       List<TranslateItem> untranslatedList) {

        // We always assume translate from the default string
        ArrayList<StringItem> defaultStrings = null;
        ArrayList<StringItem> translatedStrings = null;
        for (Entry<String, ArrayList<StringItem>> entry : allStringValues.entrySet()) {
//            ResConfigFlags configFlag = entry.getKey();
//            String q = configFlag.getQualifiers();
            String q = entry.getKey();
            if (q.equals(qualifier)) {
                translatedStrings = entry.getValue();
            } else if ("".equals(q)) {
                defaultStrings = entry.getValue();
            }
        }

        // Error, cannot find the source string to translate
        if (defaultStrings == null) {
            if (translatedStrings != null) {
                for (StringItem kv : translatedStrings) {
                    TranslateItem item = new TranslateItem(kv.name, "", kv.value);
                    translatedList.add(item);
                }
            }
            return;
        }

        // Arrange into hash maps
        Map<String, String> translated = new HashMap<>();
        Map<String, String> defaultStrs = new HashMap<>();
        if (translatedStrings != null) {
            for (StringItem p : translatedStrings) {
                translated.put(p.name, p.value);
            }
        }
        for (StringItem p : defaultStrings) {
            defaultStrs.put(p.name, p.value);
        }

        // Add the translated item
        if (translatedStrings != null) {
            for (StringItem p : translatedStrings) {
                String name = p.name;
                String originValue = defaultStrs.get(name);
                String translatedVal = p.value;
                TranslateItem item = new TranslateItem(name, originValue,
                        translatedVal);
                translatedList.add(item);
            }
        }

        // Add the un-translated item
        for (StringItem p : defaultStrings) {
            String name = p.name;
            String translatedVal = translated.get(name);
            if (translatedVal == null) {
                String originValue = p.value;
                TranslateItem item = new TranslateItem(name, originValue, null);
                untranslatedList.add(item);
            }
        }
    }

    // Add string values for a language
    public String addLanguageRetError(String qualifier) {
        Resources res = getResources();
        if (qualifier.length() < 3) {
            return res.getString(R.string.invalid_lang_code);
        }

        if (!resourceParsed) {
            return res.getString(R.string.wait_for_decoding);
        }

        // Collect which strings are already translated, which are not
        List<TranslateItem> translatedItems = new ArrayList<>();
        List<TranslateItem> untranslatedItems = new ArrayList<>();
        prepareTranslateItems(qualifier, translatedItems, untranslatedItems);

        // All strings are already in that language
        if (untranslatedItems.isEmpty()) {
            return res.getString(R.string.lang_exist);
        }

        // Put the new value to the map structure
        boolean bExisting = false; // This language previously exist or not
        String addedConfigFlag = null;
        ArrayList<StringItem> untranslatedList = new ArrayList<>();
        for (TranslateItem item : untranslatedItems) {
            untranslatedList.add(new StringItem(item.name, item.originValue));
        }
        // Find the existing config flags
        for (Entry<String, ArrayList<StringItem>> entry : allStringValues.entrySet()) {
//            ResConfigFlags configFlag = entry.getKey();
//            String q = configFlag.getQualifiers();
            String q = entry.getKey();
            if (q.equals(qualifier)) {
                addedConfigFlag = q;
                bExisting = true;
                entry.getValue().addAll(untranslatedList);
                break;
            }
        }
        // The config flags does not exist
        if (addedConfigFlag == null) {
//            addedConfigFlag = createConfigFlags(qualifier);
            addedConfigFlag = qualifier;
            allStringValues.put(addedConfigFlag, untranslatedList);
        }

        // Save added language to resource file
        try {
            if (!bExisting) { // Just need to save the untranslated list
                saveStringResource(qualifier, untranslatedList);
            } else {
                // Need to save both translated and untranslated
                List<StringItem> valueList = new ArrayList<>();
                for (TranslateItem item : translatedItems) {
                    valueList.add(new StringItem(item.name, item.translatedValue));
                }
                valueList.addAll(untranslatedList);
                saveStringResource(qualifier, valueList);
            }
            this.stringModified = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Update the display
        this.curConfig = addedConfigFlag;
        // Log.d("DEBUG", "curConfig.qualifier=" + curConfig.getQualifiers());
        resetSpinner(); // Reset the spinner
        // updateStringList(); // Not needed as it will be updated when reset
        // spinner

        return null;
    }

    // Save strings to file like values-zh-rCN/strings.xml
    private void saveStringResource(String qualifier,
                                    List<StringItem> valueList) throws Exception {
        String fileName = "strings.xml";

        // XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        // XmlSerializer xmlSerializer = factory.newSerializer();
        XmlSerializer xmlSerializer = new KXmlSerializer();

        String dirPath = this.decodeRootPath + "/res/values" + qualifier;
        File dirFile = new File(dirPath);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }

        File file = new File(dirPath + "/" + fileName);
        FileOutputStream fos = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(fos);
        xmlSerializer.setOutput(writer);
        // xmlSerializer.startDocument("utf-8", false);
        writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        writer.write("<resources>\n");

        // LOGGER.info(path);
        for (StringItem v : valueList) {
            // Log.d("DEBUG", "dirPath="+ dirPath + ", name=" + v.m1 +
            // ", value=" + v.m2);
            xmlSerializer.startTag(null, "string");
            xmlSerializer.attribute(null, "name", v.name);

            if (ResXmlEncoders.hasMultipleNonPositionalSubstitutions(v.value)) {
                xmlSerializer.attribute(null, "formatted", "false");
            }

            String txt;
            // Special case: reference, no encode needed
            if (v.value.startsWith("@string/")
                    || v.value.startsWith("@android:string/")) {
                txt = v.value;
            } else {
                if (v.styledValue == null) {
                    String escaped = ResXmlEncoders.escapeXmlChars(v.value);
                    txt = ResXmlEncoders.encodeAsXmlValue(escaped);
                } else {
                    txt = ResXmlEncoders.encodeAsXmlValue(v.styledValue);
                }
            }
            xmlSerializer.ignorableWhitespace(txt);
            // xmlSerializer.text(txt);
            xmlSerializer.endTag(null, "string");
            xmlSerializer.flush();
            writer.write("\n");
        }

        writer.write("</resources>\n");
        writer.close();
        fos.close();
    }

    // Modify the string resource
    // modifiedValues only contain modified values
    private void modifyStringResource(String qualifier,
                                      Map<String, String> modifiedValues) throws Exception {
        String fileName = "strings.xml";

        String dirPath = this.decodeRootPath + "/res/values" + qualifier;
        File dirFile = new File(dirPath);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }

        String filePath = dirPath + "/" + fileName;
        List<String> lines = new TextFileReader(filePath).getLines();
        List<String> newLines = new ArrayList<>();

        // lastStringLine is used to record the position where the new added string should append to
        int lastStringLine = -1;
        final String targetStr = "<string name=\"";
        Set<String> savedStrings = new HashSet<>();
        for (int i = 0; i < lines.size(); ++i) { // Check line by line
            String line = lines.get(i);
            int pos = line.indexOf(targetStr);
            if (pos == -1) {
                newLines.add(line);
                continue;
            }
            int start = pos + targetStr.length();
            int end = line.indexOf('\"', start);
            if (end == -1) {
                newLines.add(line);
                continue;
            }
            String name = line.substring(start, end);
            String newValue = modifiedValues.get(name);
            if (newValue != null) {
                newLines.add(StringItem.toString(name, newValue, null));
                savedStrings.add(name);
                // The string occupies several lines
                if (!line.contains("</string>")) {
                    while (++i < lines.size()) {
                        line = lines.get(i);
                        if (line.contains("</string>")) {
                            break;
                        }
                    }
                }
            } else {
                newLines.add(line);
            }
            lastStringLine = newLines.size() - 1;
        }

        // Add the unsaved string
        for (Map.Entry<String, String> entry : modifiedValues.entrySet()) {
            // The modified value is not saved, the add it
            String name = entry.getKey();
            if (!savedStrings.contains(name)) {
                String newValue = entry.getValue();
                lines.add(++lastStringLine, StringItem.toString(name, newValue, null));
            }
        }

        // Write back to file
        FileUtil.writeToFile(filePath, newLines);
    }

    @SuppressLint("DefaultLocale")
    protected void searchStringByKeyword(String keyword) {
        String lcKeyword = keyword.toLowerCase();
        ArrayList<StringItem> values = allStringValues.get(curConfig);
        if (values != null) {
            ArrayList<StringItem> selected = new ArrayList<>();
            for (StringItem pair : values) {
                if (pair.value.toLowerCase().contains(lcKeyword)) {
                    selected.add(pair);
                }
            }
            updateStringList(selected);
        }
    }

    private void updateStringList() {
        if (this.curConfig == null) {
            curConfig = getBestConfig(allStringValues.keySet());
        }
        LOGGER.info("********BEST*********" + curConfig);
        ArrayList<StringItem> values = allStringValues.get(curConfig);
        updateStringList(values);
    }

    private void updateStringList(ArrayList<StringItem> values) {
        // Log.d("DEBUG", "updateStringList called!");
        stringListAdapter.updateData(curConfig, values);
    }

    private void initSpinner() {
        // Should not appear, but ...
        if (allStringValues == null) {
            return;
        }
        if (this.curConfig == null) {
            curConfig = getBestConfig(allStringValues.keySet());
        }

        this.langConfigList = new ArrayList<>();
        String languages[] = new String[allStringValues.size()];
        List<String> langList = new ArrayList<>();
        Map<String, String> _m = new HashMap<>(); // Language -> qualifier
        for (String qualifier : allStringValues.keySet()) {
            String strLang = LanguageMapping.getLanguage(qualifier);
            langList.add(strLang);
            _m.put(strLang, qualifier);
            // Log.d("DEBUG", "qualifier=" + qualifier + ", " + curConfig);
        }

        // Sort the language
        Collections.sort(langList);
        int selected = 0;
        int index = 0;
        for (String strLang : langList) {
            String configFlag = _m.get(strLang);
            languages[index] = strLang;
            langConfigList.add(configFlag);
            if (configFlag.equals(curConfig)) {
                selected = index;
            }
            index++;
        }

        // Initialize spinner by setting adapter
        Spinner spinner = (Spinner) findViewById(R.id.language_spinner);
        if (spinner == null) {
            return;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Event listener
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int position, long arg3) {
                ApkInfoActivity.this.curConfig = langConfigList.get(position);
                updateStringList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        // Log.d("DEBUG", "spinner, selected=" + selected);
        spinner.setSelection(selected);
    }

    private void resetSpinner() {
        initSpinner();
    }

    // Not only the resource files, but may also include smali files
    private void showDecodedFileList() {
        String curDir;
        if (resCurrentDir != null && resCurrentDir.startsWith(decodeRootPath)) {
            curDir = resCurrentDir;
        } else {
            // Now always show the root path
            //curDir = BuildConfig.IS_PRO ? decodeRootPath : decodeRootPath + "/res";
            curDir = decodeRootPath;
        }
        // For full decoding, do not need apk path (otherwise may cause 2 assets directory)
        String path = (isFullDecoding ? null : this.apkPath);
        resListAdapter = new ResListAdapter(this, path, curDir,
                decodeRootPath, this.fileEntry2ZipEntry, (dir, filename) -> {
                    // Do not filter AndroidManifest.xml any more
                    return true;
    //                File f = new File(dir, filename);
    //                if (f.isDirectory()) {
    //                    return true;
    //                } else {
    //                    return !(decodeRootPath.equals(dir.getAbsolutePath())
    //                            && filename.equals("AndroidManifest.xml"));
    //                }
                }, this);
        resListAdapter.setModification(this.res_addedFiles,
                this.res_deletedFiles, this.res_replacedFiles);
        this.resourceList.setAdapter(resListAdapter);
        this.resourceList.setOnItemClickListener(this);
        this.resourceList.setOnItemLongClickListener(this);

        mfListAdapter = new ManifestListAdapter(this,
                decodeRootPath + "/AndroidManifest.xml", this);
        this.manifestList.setAdapter(mfListAdapter);
        this.manifestList.setOnItemClickListener(mfListAdapter);
        this.manifestList.setOnItemLongClickListener(mfListAdapter);

        // Recover the navigation bar
        if (navigationMgr != null && resCurrentDir != null) {
            navigationMgr.gotoDirectory(resCurrentDir);
        }

        // Set Flag
        this.resourceParsed = true;

        updateCenterView();
    }

    // Resource directory is changed (notified by navigation manager)
    public void resDirectoryChanged(String newDir, int upLevel) {
        this.resListAdapter.openDirectory(newDir);
        if (upLevel > 0) {
            try {
                Duo<Integer, Integer> pos = null;
                for (int i = 0; i < upLevel; ++i) {
                    pos = resListPosition.pop();
                }
                if (pos != null) {
                    resourceList.setSelectionFromTop(pos.m1, pos.m2);
                }
            } catch (Exception ignored) {
            }
        }
    }

    // The selection mode of the resource list changed
    @Override
    public void selectionChanged(Set<Integer> selected) {
        boolean selectionMode = !selected.isEmpty();
        if (selectionMode) {
            this.resSearchLayout.setVisibility(View.GONE);
            this.resNaviHeader.setVisibility(View.GONE);
            this.resMenuLayout.setVisibility(View.VISIBLE);
            this.resSelectHeader.setVisibility(View.VISIBLE);
            String text = String.format(getString(R.string.num_items_selected),
                    selected.size());
            this.resSelectTip.setText(text);
        } else {
            this.resMenuLayout.setVisibility(View.GONE);
            this.resSelectHeader.setVisibility(View.GONE);
            this.resSearchLayout.setVisibility(View.VISIBLE);
            this.resNaviHeader.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        List<FileRecord> fileList = new ArrayList<>();
        String oldDir = resListAdapter.getData(fileList);
        FileRecord rec = fileList.get(position);
        if (rec == null) {
            return;
        }

        // In selection mode
        if (!resListAdapter.getCheckedItems().isEmpty()) {
            resListAdapter.reverseCheckStatus(position);
            return;
        }

        if (rec.isDir) {
            boolean isUpToParent = false;
            String targetPath;
            if (rec.fileName.equals("..")) {
                isUpToParent = true;
                int pos = oldDir.lastIndexOf('/');
                targetPath = oldDir.substring(0, pos);
            } else {
                // save index and top position
                int index = resourceList.getFirstVisiblePosition();
                View v = resourceList.getChildAt(0);
                int top = (v == null) ? 0
                        : (v.getTop() - resourceList.getPaddingTop());
                resListPosition.push(new Duo<>(index, top));

                targetPath = oldDir + "/" + rec.fileName;
            }
            resListAdapter.openDirectory(targetPath);
            navigationMgr.gotoDirectory(targetPath);
            if (isUpToParent) { // Recover from the old position
                try {
                    Duo<Integer, Integer> pos = resListPosition.pop();
                    resourceList.setSelectionFromTop(pos.m1, pos.m2);
                } catch (Exception ignored) {
                }
            } else {
                resourceList.setSelectionAfterHeaderView();
            }

            // After open the folder, check if it exist
            // String curFolder = resListAdapter.getData(null);
            // if (new File(curFolder).exists()) {
            // this.resSearchLayout.setVisibility(View.VISIBLE);
            // } else {
            // this.resSearchLayout.setVisibility(View.GONE);
            // }
        }
        // Try to open the file
        else {
            openFile(oldDir, rec.fileName, rec.isInZip);
        }
    }

    // Open files like *.xml, *.txt, etc
    private void openEditableFile(String directory, String fileName,
                                  boolean bInZip, String entryName, String syntaxFileName) {

        String filePath = resListAdapter.getReplacedFilePath(entryName);
        if (filePath != null) {
            // replaced entry
        } else if (bInZip) {
            // Extract to temp path
            filePath = extractFileFromZip(entryName);
            if (filePath == null) {
                String errMeesage = String.format(getString(R.string.cannot_open_xxx), entryName);
                Toast.makeText(this, errMeesage, Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            filePath = directory + "/" + fileName;
        }
        openFileEditor(filePath, syntaxFileName, fileName, entryName);
    }

    protected void openFile(String directory, String fileName, boolean bInZip) {
        // Get entry name
        String entryName;
        if (directory.equals(this.decodeRootPath)) {
            entryName = fileName;
        } else {
            entryName = directory.substring(decodeRootPath.length() + 1) + "/"
                    + fileName;
        }

        String syntaxFileName = getEditableSyntax(fileName);
        if (syntaxFileName != null) {
            openEditableFile(directory, fileName, bInZip, entryName, syntaxFileName);
        } else {
            String filePath = resListAdapter.getReplacedFilePath(entryName);
            if (filePath != null) {
                // replaced entry
            } else if (bInZip) {
                // Extract to temp path
                filePath = extractFileFromZip(entryName);
            } else if (resListAdapter.isImageFile(fileName)) {
                // Image file also need to extract
                filePath = extractFileFromZip(entryName);
            } else {
                // Need to copy to sd card?
                filePath = directory + "/" + fileName;
            }

            // Avoid null path, don't know when it is null, but seems it could be happened
            if (filePath != null) {
                this.filePathForExternal = filePath;
                this.entryNameForExternal = entryName;
                File f = new File(filePath);
                this.modifiedTimeBeforeOpen = f.lastModified();
                if (fileName.endsWith(".png")) {
                    Intent intent = new Intent(this, PngEditActivity.class);
                    ActivityUtil.attachParam(intent, "filePath", filePath);
                    startActivityForResult(intent, RC_OPEN_EXTERNAL);
                } else {
                    OpenFiles.openFile(this, filePath, RC_OPEN_EXTERNAL);
                }
            }
        }
    }

    // Get the syntax file name for the file when editing
    private String getEditableSyntax(String fileName) {
        if (fileName.endsWith(".xml")) {
            return "xml.xml";
        } else if (fileName.endsWith(".smali")) {
            return "smali.xml";
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

    protected void replaceFile(String replacedPath, String replacingPath) {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            // Copy files
            in = new FileInputStream(replacingPath);
            out = new FileOutputStream(replacedPath);
            IOUtils.copy(in, out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    private void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }

    private boolean isImageFile(String fileName) {
        if (fileName.endsWith(".png") || fileName.endsWith(".jpg")
                || fileName.endsWith(".bmp")) {
            return true;
        }
        return false;
    }

    // filePath: file to be edited
    // syntaxFileName: like html.xml, smali.xml
    // displayFileName: name to be shown in title
    // entryName: the editing entry in apk file (here as extraString)
    // Note: entryName can NOT be null
    private void openFileEditor(String filePath, String syntaxFileName,
                                String displayFileName, String entryName) {
        // Open the color editor
        if ("res/values/colors.xml".equals(entryName)) {
            Intent intent = new Intent(this, ColorXmlActivity.class);
            ActivityUtil.attachParam(intent, "xmlPath", filePath);
            startActivityForResult(intent, RC_COLOR_EDITOR);
            return;
        }

        Intent intent = TextEditor.getEditorIntent(this, filePath, this.apkPath);
        ActivityUtil.attachParam(intent, "syntaxFileName", syntaxFileName);
        if (displayFileName != null) {
            ActivityUtil.attachParam(intent, "displayFileName", displayFileName);
        }
        ActivityUtil.attachParam(intent, "extraString", entryName);
        startActivityForResult(intent, RC_FILE_EDITOR);
    }

    // Long click the resource list item
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View arg1,
                                   final int position, long id) {
        // The first item is always the parent folder
        final boolean isFirstItem = (position == 0);

        parent.setOnCreateContextMenuListener(
                (menu, v, menuInfo) -> {
                    // Check the item is directory or not
                    List<FileRecord> records = new ArrayList<>();
                    String curPath = resListAdapter.getData(records);
                    boolean isDir = records.get(position).isDir;

                    // Delete
                    if (!isFirstItem) {
                        MenuItem item1 = menu.add(0, Menu.FIRST, 0,
                                R.string.delete);
                        item1.setOnMenuItemClickListener(
                                item -> {
                                    List<Integer> positions = new ArrayList<>(1);
                                    positions.add(position);
                                    resListAdapter
                                            .deleteFile(positions);
                                    resListAdapter.dumpChangedFiles();
                                    return true;
                                });
                    }
                    // Extract (res directory also allow to extract)
                    if (!isFirstItem || curPath.equals(decodeRootPath)) {
                        MenuItem item2 = menu.add(0, Menu.FIRST + 1, 0,
                                R.string.extract);
                        item2.setOnMenuItemClickListener(
                                item -> {
                                    List<Integer> positions = new ArrayList<>(1);
                                    positions.add(position);
                                    extractFileOrDir(positions);
                                    return true;
                                });
                    }
                    // Replace the file/folder
                    if (!isFirstItem || curPath.equals(decodeRootPath)) {
                        MenuItem item3 = menu.add(0, Menu.FIRST + 2, 0,
                                R.string.replace);
                        OnMenuItemClickListener listener;
                        if (isDir) {
                            listener = item -> {
                                replaceFolder(position);
                                resListAdapter.dumpChangedFiles();
                                return true;
                            };
                        } else {
                            listener = item -> {
                                replaceFile(position);
                                resListAdapter.dumpChangedFiles();
                                return true;
                            };
                        }
                        item3.setOnMenuItemClickListener(listener);
                    }

                    // Add a file (always allow to add a file)
                    {
                        MenuItem item4 = menu.add(0, Menu.FIRST + 3, 0,
                                R.string.add_a_file);
                        item4.setOnMenuItemClickListener(
                                item -> {
                                    addFile(position);
                                    // resListAdapter.dumpChangedFiles();
                                    return true;
                                });
                    }

                    // Add a folder (always allow to add a folder)
                    {
                        MenuItem item5 = menu.add(0, Menu.FIRST + 4, 0,
                                R.string.new_folder);
                        item5.setOnMenuItemClickListener(
                                item -> {
                                    createFolder(position);
                                    return true;
                                });
                    }
                });
        return false;
    }

    private List<FileCopyDialog.CopySource> getCopySources(List<Integer> positions) {
        List<FileCopyDialog.CopySource> result = new ArrayList<>();

        List<FileRecord> records = new ArrayList<>();
        String dirPath = resListAdapter.getData(records);

        for (int position : positions) {
            if (position >= records.size()) {
                continue;
            }

            FileRecord fileRec = records.get(position);

            FileCopyDialog.CopySource src = new FileCopyDialog.CopySource();
            if (fileRec.isInZip) {
                src.path = (dirPath + "/" + fileRec.fileName)
                        .substring(decodeRootPath.length() + 1);
            } else {
                src.path = dirPath + "/" + fileRec.fileName;
            }
            src.isInApk = fileRec.isInZip;
            src.isDir = fileRec.isDir;

            result.add(src);
        }

        return result;
    }

    // Extract file/directory to sd card
    protected void extractFileOrDir(List<Integer> positions) {
        List<FileCopyDialog.CopySource> sources = getCopySources(positions);
        extractFileOrDir_internal(sources);
    }

    // Extract file from a path, not a entry
    protected void extractFileOrDir(String filepath) {
        List<FileCopyDialog.CopySource> sources = new ArrayList<>();
        FileCopyDialog.CopySource s = new FileCopyDialog.CopySource();
        s.isInApk = false;
        s.isDir = false;
        s.path = filepath;
        sources.add(s);
        extractFileOrDir_internal(sources);
    }

    private void extractFileOrDir_internal(
            final List<FileCopyDialog.CopySource> sources) {
        // Select a target folder to extract
        String dlgTitle = this.getString(R.string.select_folder);
        IFileSelection callback = new IFileSelection() {
            @Override
            // filePath is the target directory
            // extraStr is the source file/directory
            public void fileSelectedInDialog(
                    String filePath, String extraStr, boolean openFile) {
                FileCopyDialog dlg = new FileCopyDialog(ApkInfoActivity.this,
                        apkPath, decodeRootPath, fileEntry2ZipEntry, sources, filePath);
                dlg.show();
            }

            @Override
            public boolean isInterestedFile(String filename, String extraStr) {
                return true;
            }

            @Override
            public String getConfirmMessage(String filePath, String extraStr) {
                return null;
            }
        };

        FileSelectDialog dlg = new FileSelectDialog(this, callback, null, null,
                dlgTitle, true, false, false, null);
        dlg.show();
    }

    // Extract a file from apk to a temporary path
    private String extractFileFromZip(String entryName) {
        // Get file type
        String filename;
        int pos = entryName.lastIndexOf("/");
        if (pos != -1) {
            filename = entryName.substring(pos + 1);
        } else {
            filename = entryName;
        }
        String fileType = "";
        pos = filename.lastIndexOf(".");
        if (pos != -1) {
            fileType = filename.substring(pos);
        }

        String dstPath;
        try {
            String zipEntry = this.fileEntry2ZipEntry.get(entryName);
            dstPath = SDCard.makeWorkingDir(this) + TMP_EDITOR_FILE + fileType;
//            String message = String.format("Extract %s(REAL:%s) to %s",
//                    entryName, zipEntry, dstPath);
//            Log.d("DEBUG", message);
            if (zipEntry != null) {
                entryName = zipEntry;
            }
            ZipUtil.unzipFileTo(this.apkPath, entryName, dstPath);
            return dstPath;
        } catch (Exception e1) {
            return null;
        }
    }

    // To replace a resource file by showing the a file select dlg
    protected void replaceFile(int position) {
        List<FileRecord> records = new ArrayList<>();
        String dirPath = resListAdapter.getData(records);
        FileRecord rec = records.get(position);
        String filepath = dirPath + "/" + rec.fileName;
        replaceFile(filepath, (SomethingChangedListener) null);
    }

    // replacedPath, also called as decodedPath, will be replaced
    protected void replaceFile(String replacedPath,
                               final SomethingChangedListener listener) {
        String suffix = null;
        int slashPosition = replacedPath.lastIndexOf('/');
        String fileName = replacedPath.substring(slashPosition + 1);
        int dotPosition = fileName.lastIndexOf('.');
        if (dotPosition != -1) {
            suffix = fileName.substring(dotPosition);
        }
        final String fileTypeStr = suffix;

        FileSelectDialog dlg = new FileSelectDialog(this,
                new IFileSelection() {
                    @Override
                    public void fileSelectedInDialog(
                            String filePath, String extraStr, boolean openFile) {
                        if (openFile) {
                            // Save the param before launch a new activity
                            saveParams(filePath, extraStr, listener);
                            OpenFiles.openFile(
                                    ApkInfoActivity.this, filePath, RC_OPEN_BEFORE_REPLACE);
                        } else {
                            resListAdapter.replaceFile(extraStr, filePath);
                            if (listener != null) {
                                listener.somethingChanged();
                            }
                        }
                    }

                    @Override
                    public boolean isInterestedFile(String filename, String extraStr) {
                        if (fileTypeStr != null) {
                            return filename.endsWith(fileTypeStr);
                        }
                        return true;
                    }

                    @Override
                    public String getConfirmMessage(String filePath, String extraStr) {
                        return null;
                    }
                }, suffix, replacedPath, null, false, false, true, null);
        dlg.show();
    }

    // Replace a folder
    protected void replaceFolder(int position) {
        String dlgTitle = this.getString(R.string.select_folder_replace);

        List<FileRecord> records = new ArrayList<>();
        String dirPath = resListAdapter.getData(records);
        FileRecord rec = records.get(position);

        IFileSelection callback = new IFileSelection() {
            @Override
            public void fileSelectedInDialog(
                    String filePath, String extraStr, boolean openFile) {
                String decodedPath = extraStr;
                String workingDir = SDCard.getRootDirectory() + "/ApkEditor/tmp";
                // Selected path contains working dir
                if (workingDir.startsWith(filePath)) {
                    Toast.makeText(ApkInfoActivity.this,
                                    R.string.select_folder_err2, Toast.LENGTH_LONG)
                            .show();
                }
                // Selected path inside working dir
                else if (filePath.startsWith(workingDir)) {
                    Toast.makeText(ApkInfoActivity.this,
                                    R.string.select_folder_err1, Toast.LENGTH_LONG)
                            .show();
                } else {
                    resListAdapter.replaceFolder(decodedPath, filePath);
                }
            }

            @Override
            public boolean isInterestedFile(String filename, String extraStr) {
                return true;
            }

            @Override
            public String getConfirmMessage(String filePath, String extraStr) {
                String replaced = extraStr
                        .substring(decodeRootPath.length() + 1);
                String message = getString(R.string.folder_replace_tip);
                return String.format(message, replaced, filePath);
            }
        };
        FileSelectDialog dlg = new FileSelectDialog(this, callback, null,
                dirPath + "/" + rec.fileName, dlgTitle, true, true, false, null);
        dlg.show();
    }

    // To add a file in current directory
    protected void addFile(int position) {
        String dirPath = resListAdapter.getData(null);
        FileSelectDialog dlg = new FileSelectDialog(this,
                new IFileSelection() {
                    @Override
                    public void fileSelectedInDialog(
                            String filePath, String extraStr, boolean openFile) {
                        String dirPath = extraStr;
                        String name = filePath
                                .substring(filePath.lastIndexOf("/") + 1);
                        resListAdapter.addFile(dirPath + "/" + name, filePath);
                    }

                    @Override
                    public boolean isInterestedFile(String filename, String extraStr) {
                        return true;
                    }

                    @Override
                    public String getConfirmMessage(String filePath,
                                                    String extraStr) {
                        return null;
                    }
                }, null, dirPath, ApkInfoActivity.this.getString(R.string.add_a_file));

        dlg.show();
    }

    // To create a folder in current directory
    protected void createFolder(int position) {
        boolean showImportFolder = this.isFullDecoding;
        new AddFolderDialog(this, this, showImportFolder).show();
    }

    @Override
    public String tryToDeleteSection(LineRecord lineRec) {
        return null;
    }

    @Override
    public void manifestChanged(String newContent) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(decodeRootPath + "/AndroidManifest.xml");
            fos.write(newContent.getBytes());
            this.manifestModified = true;
        } catch (IOException ignored) {

        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    protected void fakeShow() {
        String errMessage = "Manifest editing is disabled (seems not a genuine version)";
        Toast.makeText(this, errMessage, Toast.LENGTH_LONG).show();
    }

    // To show the dex decoding progress bar
    @Override
    public void dexDecodingStarted() {
        this.dex2smaliImage.setVisibility(View.INVISIBLE);
        this.decodeResultLayout.setVisibility(View.INVISIBLE);
        this.decodeProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void dexDecodingFinished(boolean ret, String strError,
                                    String strWarning) {
        // Notify the dex decoded watcher
        if (this.dexDecodedCallback != null) {
            dexDecodedCallback.callbackFunc();
            dexDecodedCallback = null;
        }

        boolean showResult = true;
        if (ret) {
            if (strWarning != null) {
                this.decodeResultTitle.setText(R.string.succeed_with_warning);
                String content = this.getString(R.string.warning) + ": " + strWarning;
                this.decodeResultDetail.setText(content);
                this.decodeResultDetail.setVisibility(View.VISIBLE);
            } else {
                showResult = false;
                this.decodeResultTitle.setText(R.string.succeed);
                this.decodeResultDetail.setVisibility(View.GONE);
            }
        } else {
            this.decodeResultTitle.setText(R.string.failed);
            this.decodeResultDetail.setVisibility(View.VISIBLE);
            if (strError != null) {
                this.decodeResultDetail.setText(strError);
            } else {
                this.decodeResultDetail.setText(R.string.unknown_error);
            }
        }

        // Switch the layout (decoding panel)
        this.dex2smaliImage.setVisibility(View.INVISIBLE);
        this.decodeProgressBar.setVisibility(View.INVISIBLE);
        if (showResult) {
            this.decodeResultLayout.setVisibility(View.VISIBLE);
        } else {
            this.dexDecodeLayout.setVisibility(View.GONE);
            Toast.makeText(this, R.string.dex_decode_succeed, Toast.LENGTH_LONG).show();
        }

        // Update the file list if in the root decoded folder
        String curFolder = this.resListAdapter.getData(null);
        if (curFolder.endsWith("/decoded")) {
            this.resListAdapter.openDirectory(curFolder);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        int id = v.getId();
        if (id == R.id.imageview_dex2smali) {
            this.dexDecodeLayout.setVisibility(View.GONE);
            return true;
        }
        return false;
    }

    public String getDecodeRootPath() {
        return this.decodeRootPath;
    }

    public ResListAdapter getResListAdapter() {
        return this.resListAdapter;
    }

    public boolean isDexDecoded() {
        return this.dexDecoded;
    }

    public String getApkPath() {
        return this.apkPath;
    }

    public ApkInfoParser.AppInfo getApkInfo() {
        return this.apkInfo;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    // Save params before launching the new Activity for file view/editing
    // In the scenario of file replacing
    public void saveParams(String filePath, String extraStr, SomethingChangedListener listener) {
        this.savedParam_filePath = filePath;
        this.savedParam_extraStr = extraStr;
        ApkInfoActivity.this.savedParam_listener = listener;
    }

    class MyServiceConnection implements ServiceConnection {
        private boolean bSign;

        public MyServiceConnection(boolean bSign) {
            this.bSign = bSign;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ApkComposeService.ComposeServiceBinder binder = (ApkComposeService.ComposeServiceBinder) service;
            if (binder.isRunning()) {
                showCannotStartBuildDialog();
            } else {
                launchWithoutCheck(bSign);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }
}
