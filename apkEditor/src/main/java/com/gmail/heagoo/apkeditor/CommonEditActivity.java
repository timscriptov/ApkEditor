package com.gmail.heagoo.apkeditor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.ce.ApkParser;
import com.gmail.heagoo.apkeditor.ce.DexDecode;
import com.gmail.heagoo.apkeditor.ce.DexEncode;
import com.gmail.heagoo.apkeditor.ce.DexPatch_CheatPkgName;
import com.gmail.heagoo.apkeditor.ce.IApkMaking;
import com.gmail.heagoo.apkeditor.ce.ManifestInfo;
import com.gmail.heagoo.apkeditor.ce.ManifestParser;
import com.gmail.heagoo.apkeditor.ce.ReplaceLauncherIcon;
import com.gmail.heagoo.apkeditor.ce.e.ManifestEditorNew;
import com.gmail.heagoo.apkeditor.ce.e.PluginWrapperExtra;
import com.gmail.heagoo.apkeditor.ce.e.PluginWrapperMfEditor;
import com.gmail.heagoo.apkeditor.ce.e.RefactorDex;
import com.gmail.heagoo.apkeditor.ce.e.RefactorLayout;
import com.gmail.heagoo.apkeditor.se.ApkCreateActivity;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.ApkInfoParser;
import com.gmail.heagoo.common.ApkInfoParser.AppInfo;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.RandomUtil;
import com.gmail.heagoo.common.SDCard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CommonEditActivity extends CustomizedLangActivity implements OnClickListener,
        IFileSelection, TextWatcher, OnItemSelectedListener {

    private String apkPath;
    private AppInfo apkInfo;

    private ImageView apkIcon;
    private ImageView launcherIcon;
    private TextView apkLabel;
    private TextView minSdkLabel;
    private TextView targetSdkLabel;
    private TextView maxSdkLabel;
    private EditText appNameEt;
    private EditText pkgNameEt;
    private EditText verCodeEt;
    private EditText verNameEt;
    private EditText minSdkEt;
    private EditText targetSdkEt;
    private EditText maxSdkEt;
    private CheckBox renameResCb;
    private CheckBox renameDexCb;
    private View renameExtraLayout;

    private ManifestParser manifestParser;
    private ManifestInfo manifestInfo;
    private MyHandler handler = new MyHandler(this);

    private Spinner installLocSpinner;
    private Spinner renameMethodSpinner;

    // The modified manifest
    private String newManifestFile;

    // Class name which need to be refactored
    private Map<String, String> refactored;
    private String newPackageName;
    private String newAppName;
    private String newVerName;
    private int newVerCode;
    private int newInstallLocation;

    // png file path for apk launcher icon
    private String launcherIconPath;

    // Extra string replaces is for provider authority change
    private Map<String, String> extraStrReplaces;

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0
                || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        // Dark theme or not
        // sawsem theme
//        switch (GlobalConfig.instance(this).getThemeId()) {
//            case GlobalConfig.THEME_DARK_DEFAULT:
//                super.setTheme(android.R.style.Theme_Black_NoTitleBar);
//                setContentView(R.layout.activity_commonedit_dark);
//                break;
//            case GlobalConfig.THEME_DARK_RUSSIAN:
//                super.setTheme(android.R.style.Theme_Black_NoTitleBar);
//                setContentView(R.layout.activity_commonedit_dark_ru);
//                break;
//            default:
        setContentView(R.layout.activity_commonedit);
//                break;
//        }

        // Full screen or not
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        this.apkPath = ActivityUtil.getParam(getIntent(), "apkPath");

        // Create a thread to parse APK Information
        new Thread() {
            @Override
            public void run() {
                try {
                    apkInfo = new ApkInfoParser().parse(CommonEditActivity.this, apkPath);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    ZipFile zipFile = new ZipFile(apkPath);
                    ZipEntry entry = zipFile.getEntry("AndroidManifest.xml");
                    InputStream is = zipFile.getInputStream(entry);
                    manifestParser = new ManifestParser(is);
                    manifestParser.parse();
                    is.close();
                    zipFile.close();

                    ManifestInfo mfInfo = manifestParser.getManifestInfo();
                    if (apkInfo == null) {
                        apkInfo = new AppInfo();
                        ApkParser parser = new ApkParser(CommonEditActivity.this, apkPath);
                        parser.parse(mfInfo.appNameId, mfInfo.launcherId);
                        apkInfo.label = parser.getAppName();
                        Bitmap bitmap = parser.getAppIcon();
                        if (bitmap != null) {
                            apkInfo.icon = new BitmapDrawable(bitmap);
                        }
                    }

                    handler.sendEmptyMessage(0);
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.setErrorMessage(e.getMessage());
                    handler.sendEmptyMessage(1);
                }
            }
        }.start();

        initView();
    }

    // When error message = NULL, means succeed
    public void parseFinished(String errMessage) {
        if (errMessage == null) {
            this.manifestInfo = manifestParser.getManifestInfo();
            manifestInfo.appName = apkInfo.label;

            apkIcon.setImageDrawable(apkInfo.icon);
            apkLabel.setText(apkInfo.label);

            appNameEt.setText(apkInfo.label);
            pkgNameEt.setText(manifestInfo.packageName);
            if (manifestInfo.verNameIdx == -1) {
                verNameEt.setVisibility(View.GONE);
                findViewById(R.id.tv_vername).setVisibility(View.GONE);
            } else {
                verNameEt.setText(manifestInfo.versionName);
            }
            verCodeEt.setText(String.valueOf(manifestInfo.versionCode));

            int location = manifestInfo.installLocation;
            if (location >= -1 && location < 3) {
                installLocSpinner.setSelection(location + 1);
            }

            // Launcher Icon
            this.launcherIcon.setImageDrawable(apkInfo.icon);
            if (manifestInfo.launcherId != -1) {
                this.launcherIcon.setOnClickListener(this);
            }

            // SDK version
            updateSdkVersion(minSdkLabel, minSdkEt, manifestInfo.minSdkVersion);
            updateSdkVersion(targetSdkLabel, targetSdkEt, manifestInfo.targetSdkVersion);
            updateSdkVersion(maxSdkLabel, maxSdkEt, manifestInfo.maxSdkVersion);
        } else {
            String msg = getResources().getString(R.string.cannot_parse_apk);
            msg += ": " + errMessage;
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            this.finish();
        }
    }

    private void updateSdkVersion(TextView label, EditText editText, int sdkVersion) {
        if (sdkVersion != -1) {
            editText.setText(String.valueOf(sdkVersion));
            label.setVisibility(View.VISIBLE);
            editText.setVisibility(View.VISIBLE);
        } else {
            label.setVisibility(View.GONE);
            editText.setVisibility(View.GONE);
        }
    }

    private void initView() {
        this.apkIcon = (ImageView) this.findViewById(R.id.apk_icon);
        this.launcherIcon = (ImageView) this.findViewById(R.id.launcher_icon);
        this.apkLabel = (TextView) this.findViewById(R.id.apk_label);
        this.minSdkLabel = (TextView) this.findViewById(R.id.tv_minSdkVersion);
        this.targetSdkLabel = (TextView) this.findViewById(R.id.tv_targetSdkVersion);
        this.maxSdkLabel = (TextView) this.findViewById(R.id.tv_maxSdkVersion);

        this.appNameEt = (EditText) this.findViewById(R.id.et_appname);
        this.pkgNameEt = (EditText) this.findViewById(R.id.et_pkgname);
        this.verCodeEt = (EditText) this.findViewById(R.id.et_vercode);
        this.verNameEt = (EditText) this.findViewById(R.id.et_vername);
        this.minSdkEt = (EditText) this.findViewById(R.id.et_minSdkVersion);
        this.targetSdkEt = (EditText) this.findViewById(R.id.et_targetSdkVersion);
        this.maxSdkEt = (EditText) this.findViewById(R.id.et_maxSdkVersion);
        this.renameResCb = (CheckBox) this.findViewById(R.id.cb_rename_resource);
        this.renameDexCb = (CheckBox) this.findViewById(R.id.cb_rename_dex);

        this.renameExtraLayout = this.findViewById(R.id.layout_rename_extra);
        this.renameMethodSpinner = (Spinner) this
                .findViewById(R.id.rename_method_spinner);
        if (!this.getPackageName().endsWith("pro")) {
            this.pkgNameEt.setEnabled(false);
            renameMethodSpinner.setEnabled(false);
            this.findViewById(R.id.cb_rename_resource).setEnabled(false);
        } else {
            this.pkgNameEt.addTextChangedListener(this);
        }

        // Install Location Spinner
        this.installLocSpinner = (Spinner) this
                .findViewById(R.id.location_spinner);
        Resources res = getResources();
        String[] strLocations = new String[4];
        strLocations[0] = res.getString(R.string.location_0);
        strLocations[1] = res.getString(R.string.location_1);
        strLocations[2] = res.getString(R.string.location_2);
        strLocations[3] = res.getString(R.string.location_3);
        ArrayAdapter<String> installLocAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, strLocations);
        installLocAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        installLocSpinner.setAdapter(installLocAdapter);

        // Package Rename Spinner
        String[] strRenameMds = new String[2];
        strRenameMds[0] = res.getString(R.string.pkg_rename_direct);
        strRenameMds[1] = res.getString(R.string.pkg_rename_as_plugin);
        ArrayAdapter<String> renameMethodAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, strRenameMds);
        renameMethodAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        renameMethodSpinner.setAdapter(renameMethodAdapter);
        renameMethodSpinner.setOnItemSelectedListener(this);

        // Button
        this.findViewById(R.id.btn_close).setOnClickListener(this);
        this.findViewById(R.id.btn_save).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            this.finish();
        } else if (id == R.id.btn_save) {
            saveChanges();
        } else if (id == R.id.launcher_icon) {
            FileSelectDialog dlg = new FileSelectDialog(
                    this, this, ".png", "launcher.png", null);
            dlg.show();
        }
    }

    private void extractAssets(String assetName, String filePath) throws IOException {
        InputStream input = getAssets().open(assetName);
        FileOutputStream output = new FileOutputStream(filePath);
        IOUtils.copy(input, output);
        input.close();
        output.close();
    }

    // Use DroidPlugin
    private void enhancedPackageRename() {
        File fileDir = getFilesDir();
        String rootDirectory = fileDir.getAbsolutePath();
        String tmplApkPath = rootDirectory + "/bin/_wrapper.apk";

        // In case first time run
        File binDir = new File(rootDirectory + "/bin");
        if (!binDir.exists()) {
            if (!binDir.mkdirs()) {
                Toast.makeText(this, "Cannot create bin directory", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // authorityName
        String authString = RandomUtil.getRandomString(4);

        try {
            this.newManifestFile = SDCard.makeWorkingDir(this) + ".xml";
            extractWrapperIfNeeded(tmplApkPath);

            ZipFile zipFile = new ZipFile(tmplApkPath);
            ZipEntry entry = zipFile.getEntry("AndroidManifest.xml");
            InputStream is = zipFile.getInputStream(entry);

            PluginWrapperMfEditor editor = new PluginWrapperMfEditor(is,
                    this.newManifestFile);
            // Replace strings
            editor.replaceString("com.example.droidpluginwrapper",
                    this.newPackageName);
            editor.replaceString("DroidPluginWrapper", this.newAppName);
            editor.replaceString("1.x.y", this.newVerName);
            for (int i = 0; i < 9; ++i) {
                char c = (char) ('0' + i);
                editor.replaceString("com.morgoo.droidplugin_stub_P0" + c,
                        "com.morgoo.droidplugin_" + authString + "_P0" + c);
            }

            // Version code and install location
            editor.setVersionCode(this.newVerCode);
            editor.setInstallLocation(
                    this.newInstallLocation < 0 ? 0 : this.newInstallLocation);

            // Permissions
            if (!this.manifestInfo.permissions.isEmpty()) {
                List<String> permissions = new ArrayList<>();
                for (String permName : this.manifestInfo.permissions) {
                    permissions.add(permName);
                }
                editor.addPermissions(permissions);
            }

            editor.saveModification();

            is.close();
            zipFile.close();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return;
        }

        Map<String, String> fileReplaces = new HashMap<>();
        fileReplaces.put("AndroidManifest.xml", this.newManifestFile);
        fileReplaces.put("assets/1.apk", this.apkPath);
        if (this.launcherIconPath != null) {
            fileReplaces.put("res/drawable/ic_launcher.png", launcherIconPath);
        } else {
            String path = saveLauncher();
            if (path != null) {
                fileReplaces.put("res/drawable/ic_launcher.png", path);
            }
        }

        Intent intent = new Intent(this, ApkCreateActivity.class);
        ActivityUtil.attachParam(intent, "apkPath", tmplApkPath);
        ActivityUtil.attachParam(intent, "packageName", manifestInfo.packageName);
        ActivityUtil.attachParam(intent, "otherReplaces", fileReplaces);
        ArrayList<IApkMaking> extraTasks = new ArrayList<>();
        extraTasks.add(new PluginWrapperExtra(authString));
        intent.putExtra("interfaces", extraTasks);
        startActivity(intent);
    }

    private void extractWrapperIfNeeded(String tmplApkPath) throws IOException {
        String curVersion = null;
        try {
            PackageInfo pInfo = getPackageManager()
                    .getPackageInfo(getPackageName(), 0);
            curVersion = pInfo.versionName;
        } catch (NameNotFoundException ignored) {
        }

        SharedPreferences sp = getSharedPreferences("info", 0);
        String lastVersion = sp.getString("wrapper_ver", "");

        // File not exist or not the latest version
        if (!new File(tmplApkPath).exists()
                || !lastVersion.equals(curVersion)) {
            try {
                extractAssets("_wrapper", tmplApkPath);
                com.gmail.heagoo.common.FileEncrypter.encrypt(tmplApkPath);

                Editor editor = sp.edit();
                editor.putString("wrapper_ver", curVersion);
                editor.apply();
            } catch (Exception ignored) {
            }
        }
    }

    private String saveLauncher() {
        try {
            if (this.apkInfo.icon != null) {
                String file = SDCard.makeWorkingDir(this) + "_launcher";
                Bitmap bm = drawableToBitmap(this.apkInfo.icon);
                FileOutputStream outStream = new FileOutputStream(file);
                bm.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                outStream.flush();
                outStream.close();
                return file;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void saveChanges() {
        // Collect info
        String strCode = this.verCodeEt.getText().toString();
        this.newVerCode = 0;
        try {
            newVerCode = Integer.valueOf(strCode);
        } catch (Exception e) {
            Toast.makeText(this, R.string.invalid_ver_code, Toast.LENGTH_LONG).show();
            return;
        }

        this.newAppName = this.appNameEt.getText().toString();
        this.newPackageName = this.pkgNameEt.getText().toString();
        this.newPackageName = newPackageName.trim();
        this.newVerName = this.verNameEt.getText().toString();

        int minSdkVersion = -1;
        int targetSdkVersion = -1;
        int maxSdkVersion = -1;
        try {
            String str = minSdkEt.getText().toString();
            if (!"".equals(str)) {
                minSdkVersion = Integer.valueOf(str);
            }
            str = targetSdkEt.getText().toString();
            if (!"".equals(str)) {
                targetSdkVersion = Integer.valueOf(str);
            }
            str = maxSdkEt.getText().toString();
            if (!"".equals(str)) {
                maxSdkVersion = Integer.valueOf(str);
            }
        } catch (Exception ignored) {
        }

        if (!checkPackageName()) {
            return;
        }

        int selection = this.installLocSpinner.getSelectedItemPosition();
        this.newInstallLocation = selection - 1;

        // Detect changed or not
        boolean notModified = (this.launcherIconPath == null
                && manifestInfo.appName.equals(newAppName)
                && manifestInfo.packageName.equals(newPackageName)
                && (manifestInfo.versionCode == newVerCode)
                && (manifestInfo.installLocation == newInstallLocation));
        // As the version name can be a reference to string resources
        // So need to do a check
        if (manifestInfo.verNameIdx != -1) {
            notModified = (notModified
                    && manifestInfo.versionName.equals(newVerName));
        }
        notModified = notModified && (manifestInfo.minSdkVersion == minSdkVersion);
        notModified = notModified && (manifestInfo.targetSdkVersion == targetSdkVersion);
        notModified = notModified && (manifestInfo.maxSdkVersion == maxSdkVersion);
        if (notModified) {
            Toast.makeText(this, R.string.no_change_detected, Toast.LENGTH_LONG).show();
            return;
        }

        // Use DroidPlugin to rename the package
        int renameMethodIdx = renameMethodSpinner.getSelectedItemPosition();
        if (!manifestInfo.packageName.equals(newPackageName) && renameMethodIdx == 1) {
            enhancedPackageRename();
            return;
        }

        // Rename the package inside the DEX
        boolean refactorDex = (renameDexCb.isChecked() &&
                !this.manifestInfo.packageName.equals(this.newPackageName));

        try {
            this.newManifestFile = SDCard.makeWorkingDir(this) + ".xml";
            ZipFile zipFile = new ZipFile(apkPath);
            ZipEntry entry = zipFile.getEntry("AndroidManifest.xml");
            InputStream is = zipFile.getInputStream(entry);

            ManifestInfo newInfo = new ManifestInfo();
            newInfo.appName = newAppName;
            newInfo.packageName = newPackageName;
            newInfo.versionCode = newVerCode;
            newInfo.versionName = newVerName;
            newInfo.installLocation = newInstallLocation;
            newInfo.minSdkVersion = minSdkVersion;
            newInfo.targetSdkVersion = targetSdkVersion;
            newInfo.maxSdkVersion = maxSdkVersion;

            ManifestEditorNew editor = new ManifestEditorNew(is, newManifestFile, refactorDex);
            editor.modify(manifestInfo, newInfo);
            editor.save();
            this.refactored = editor.getRefactoredNames();
            this.extraStrReplaces = new HashMap<>();

            is.close();
            zipFile.close();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return;
        }

        makeAPK(false, refactorDex);
    }

    private boolean checkPackageName() {
        String errMessage = "Invalid Package Name";
        if (this.newPackageName.equals("")) {
            Toast.makeText(this, errMessage, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (this.newPackageName.length() >= 128) {
            Toast.makeText(this, "Package name is too long", Toast.LENGTH_SHORT)
                    .show();
            return false;
        }
        for (int i = 0; i < newPackageName.length(); i++) {
            int v = newPackageName.charAt(i);
            if (v > 127) {
                Toast.makeText(this, errMessage, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    // To make the new modified APK
    private void makeAPK(boolean bCheatPkgName, boolean refactorDex) {
        Map<String, String> fileReplaces = new HashMap<>();
        fileReplaces.put("AndroidManifest.xml", this.newManifestFile);

        Intent intent = new Intent(this, ApkCreateActivity.class);
        ActivityUtil.attachParam(intent, "apkPath", this.apkPath);
        ActivityUtil.attachParam(intent, "packageName", manifestInfo.packageName);
        ActivityUtil.attachParam(intent, "otherReplaces", fileReplaces);

        // Need to modify resources.arsc when package name is changed
        if (!this.manifestInfo.packageName.equals(this.newPackageName)) {
            if (renameResCb.isChecked()) {
                ActivityUtil.attachParam(intent, "newPackageNameInArsc", this.newPackageName);
            }
        }

        // When app name is changed and it is saved in resource, will modify it
        if (manifestInfo.appNameIdx < 0
                && !this.newAppName.equals(this.manifestInfo.appName)) {
            ActivityUtil.attachParam(intent, "oldAppNameInArsc",
                    this.manifestInfo.appName);
            ActivityUtil.attachParam(intent, "newAppNameInArsc", newAppName);
        }

        // Need to modify dex file
        if (!this.refactored.isEmpty()) {
            Map<String, String> replaces = new HashMap<>();
            for (Map.Entry<String, String> entry : refactored.entrySet()) {
                String key = entry.getKey();
                key = key.replaceAll("\\.", "/");
                key = "L" + key;
                String value = entry.getValue();
                value = value.replaceAll("\\.", "/");
                value = "L" + value;
                replaces.put(key, value);
            }
            ActivityUtil.attachParam(intent, "classRenames", replaces);
        }

        // Collect extra tasks
        ArrayList<IApkMaking> extraTasks = new ArrayList<>();

        // Replace the launcher icon
        if (manifestInfo.launcherId != -1 && launcherIconPath != null) {
            ReplaceLauncherIcon task = new ReplaceLauncherIcon(
                    manifestInfo.launcherId, this.launcherIconPath);
            extraTasks.add(task);
        }

        // Refactor the package name in DEX file;
        // Refactor package name in layout files
        if (refactorDex) {
            RefactorDex task = new RefactorDex(manifestInfo.packageName, newPackageName);
            extraTasks.add(task);
            RefactorLayout t = new RefactorLayout(manifestInfo.packageName, newPackageName);
            extraTasks.add(t);
        }

        // Whatsapp (such revise not work any more)
        String oldPackageName = this.manifestInfo.packageName;
        // if (oldPackageName.equals("com.whatsapp")
        // && !oldPackageName.equals(this.newPackageName)) {
        // extraTasks.add(new DexDecode());
        // extraTasks.add(new DexPatch_Whatsapp(this.newPackageName,
        // this.extraStrReplaces));
        // extraTasks.add(new DexEncode());
        // }
        // BBM
//        if (!bCheatPkgName && oldPackageName.equals("com.bbm")
//                && !oldPackageName.equals(this.newPackageName)) {
//            extraTasks.add(new DexDecode());
//            extraTasks.add(new DexPatch_BBM(this.newPackageName, this.extraStrReplaces));
//            extraTasks.add(new DexEncode());
//        }
        // Cheat the package name
        if (bCheatPkgName && !oldPackageName.equals(this.newPackageName)) {
            extraTasks.add(new DexDecode());
            extraTasks.add(new DexPatch_CheatPkgName(
                    manifestInfo.applicationCls, oldPackageName));
            extraTasks.add(new DexEncode());
        }

        if (!extraTasks.isEmpty()) {
            intent.putExtra("interfaces", extraTasks);
        }

        startActivity(intent);
    }

    @Override
    public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
        this.launcherIconPath = filePath;
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(this.launcherIconPath);
            this.launcherIcon.setImageBitmap(bitmap);
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean isInterestedFile(String filename, String extraStr) {
        return filename.endsWith(".png");
    }

    @Override
    public String getConfirmMessage(String filePath, String extraStr) {
        return null;
    }

    /***
     * Package name text watcher
     ***/
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        String newPkgName = s.toString();
        // Changed back to origin package name
        if (manifestInfo != null && newPkgName.equals(manifestInfo.packageName)) {
            renameExtraLayout.setVisibility(View.GONE);
        } else {
            renameExtraLayout.setVisibility(View.VISIBLE);
        }
    }

    /***
     * Package Rename Method select listener
     ***/
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position,
                               long id) {
        // This is "Make it as a plugin"
        if (position == 1) {
            renameResCb.setVisibility(View.GONE);
            renameDexCb.setVisibility(View.GONE);
        } else {
            renameResCb.setVisibility(View.VISIBLE);
            renameDexCb.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private static class MyHandler extends Handler {
        private WeakReference<CommonEditActivity> actRef;
        private String errMessage;

        MyHandler(CommonEditActivity act) {
            this.actRef = new WeakReference<>(act);
        }

        void setErrorMessage(String errMessage) {
            this.errMessage = errMessage;
        }

        @Override
        public void handleMessage(Message msg) {
            CommonEditActivity act = actRef.get();
            if (act == null) {
                return;
            }

            switch (msg.what) {
                case 0: // parse succeed
                    act.parseFinished(null);
                    break;
                case 1: // parse failed
                    act.parseFinished(errMessage);
                    break;
            }
        }
    }
}
