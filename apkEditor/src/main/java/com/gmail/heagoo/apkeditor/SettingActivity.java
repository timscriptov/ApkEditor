package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.ProcessingDialog.ProcessingInterface;
import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.CommandRunner;
import com.gmail.heagoo.common.RandomUtil;
import com.gmail.heagoo.common.SDCard;

import java.io.File;

public class SettingActivity extends PreferenceActivity
        implements OnPreferenceChangeListener, OnPreferenceClickListener {

    public static final String STR_PUBLICKEYPATH = "PublicKeyPath";
    public static final String STR_PRIVATEKEYPATH = "PrivateKeyPath";
    public static final int EXTRACT_AUTORENAME = 0; // automatically add number suffix
    public static final int EXTRACT_OVERWRITE = 1;
    private static final String appOrderKey = "AppListOrder";
    private static final String appIconKey = "MyIcon";
    private static final String compressLevelKey = "CompressionLevel";
    private static final String signWithKey = "SignApkWith";
    private static final String apkNameKey = "OutputApkName";
    private static final String cleanKey = "CleanGarbage";
    private static final String dex2smaliEnabledKey = "SmaliEditingEnabled";
    private static final String confirmEnabledKey = "RebuildConfirmation";
    private static final String fileRenameKey = "FileRenameOption";
    private static final String decodeDirKey = "DecodeDirectory";
    private static final String decodeModeKey = "DecodeMode";

    private static boolean isFreeVersion() {
        return !BuildConfig.IS_PRO;
    }

    public static String getListOrder(Context ctx) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(ctx);
        String order = sp.getString(appOrderKey, "");
        String[] orders = ctx.getResources().getStringArray(R.array.order_value);
        for (String str : orders) {
            if (order.equals(str)) {
                return order;
            }
        }
        // order value is not any valid order
        return orders[0];
    }

    public static int getCompressionLevel(Context ctx) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(ctx);

        String strLevel = sp.getString(compressLevelKey, "9");
        int level = 9;
        try {
            level = Integer.valueOf(strLevel);
        } catch (Exception ignored) {
        }

        if (level < 0) {
            level = 0;
        } else if (level > 9) {
            level = 9;
        }

        return level;
    }

    public static String getSignKeyName(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getString(signWithKey, "testkey");
    }

    public static int getOutputApkRule(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String strNameRule = sp.getString(apkNameKey, "1");

        int ret = 1;
        try {
            ret = Integer.valueOf(strNameRule);
        } catch (Exception ignored) {
        }
        return ret;
    }

    public static boolean isDex2smaliEnabled(Context ctx) {
        if (!isFreeVersion()) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
            return sp.getBoolean(dex2smaliEnabledKey, true);
        }
        return false;
    }

    public static boolean isRebuildConfirmEnabled(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getBoolean(confirmEnabledKey, false);
    }

    // 0: Auto (by add a number suffix)
    // 1: overwrite
    public static int getFileRenameOption(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String str = sp.getString(fileRenameKey, "1");
        try {
            return Integer.valueOf(str);
        } catch (Exception ignored) {
            return -1;
        }
    }

    // Can write to the directory or not
    private static boolean dirCanWrite(String dir) {
        File f = new File(dir);
        if (f.exists() && f.isDirectory()) {
            String rand = RandomUtil.getRandomString(8);
            File tryF = new File(f, rand);
            boolean ret = tryF.mkdir();
            if (ret) {
                tryF.delete();
            }
            return ret;
        }
        return false;
    }

    public static String getDecodeDirectory(Context ctx) {
        if (BuildConfig.PARSER_ONLY) {
            return SDCard.getRootDirectory() + "/ApkParser";
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String str = sp.getString(decodeDirKey, null);

        if (str != null) {
            if (str.endsWith("/")) {
                str = str.substring(0, str.length() - 1);
            }
            if (dirCanWrite(str)) {
                return str;
            }
        }

        return null;
    }

    public static String getDecodeMode(Context ctx) {
        // "0" means Full Decoding
        if (BuildConfig.PARSER_ONLY) {
            return "0";
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String value = sp.getString(decodeModeKey, "2");
        if (!value.equals("0") && !value.equals("1") && !value.equals("2")) {
            value = "2";
        }
        return value;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // sawsem theme

//        boolean isDarkTheme = GlobalConfig.instance(this).isDarkTheme();
//        if (isDarkTheme) {
//            this.setTheme(R.style.titlebar_dark);
//            requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
//        } else {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
//        }

        super.onCreate(savedInstanceState);

        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
//// sawsem theme
//        if (isDarkTheme) {
//            this.addPreferencesFromResource(R.xml.settings_dark);
//        } else {
        this.addPreferencesFromResource(R.xml.settings);
//        }

//        if (isDarkTheme) {
//            getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
//                    R.layout.titlebar_dark);
//        }

        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
// sawsem theme
//        ImageView iv = (ImageView) this.findViewById(R.id.title_icon);
//        if (iv != null) {
//            int resId = (Integer) RefInvoke.invokeStaticMethod(
//                    "com.gmail.heagoo.seticon.SetIcon", "getSelectedIcon",
//                    new Class[]{Activity.class}, new Object[]{this});
//            iv.setImageResource(resId);
//        }
    }

    @SuppressWarnings("deprecation")
    private void initData() {
        PreferenceManager manager = getPreferenceManager();

        // Decode mode
        {
            ListPreference modePref = (ListPreference) manager.findPreference(decodeModeKey);

            String value = getDecodeMode(this);
            modePref.setSummary(getDecodeModeSummary(value));

            modePref.setOnPreferenceChangeListener(this);
        }

        // App list order
        {
            ListPreference orderPref = (ListPreference) manager.findPreference(appOrderKey);

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            String defaultOrder = getResources().getStringArray(R.array.order_value)[0];
            String order = sp.getString(appOrderKey, defaultOrder);

            orderPref.setValue(order);
            orderPref.setSummary(order);

            orderPref.setOnPreferenceChangeListener(this);
        }
        // My Icon
        {
            // ListPreference iconPref = (ListPreference) manager
            // .findPreference(appIconKey);
            //
            // SharedPreferences sp = PreferenceManager
            // .getDefaultSharedPreferences(this);
            // String defaultIcon = getResources().getStringArray(
            // R.array.appicon_value)[0];
            // String iconName = sp.getString(appIconKey, defaultIcon);
            //
            // iconPref.setValue(iconName);
            // iconPref.setSummary(iconName);
            //
            // iconPref.setOnPreferenceChangeListener(this);
        }
        // Compression Level
        {
            ListPreference compressLevelPref = (ListPreference) manager
                    .findPreference(compressLevelKey);
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            String strLevel = sp.getString(compressLevelKey, "9");
            compressLevelPref.setValue(strLevel);
            compressLevelPref.setOnPreferenceChangeListener(this);
        }

        // Sign APK with ...
        {
            ListPreference signPref = (ListPreference) manager.findPreference(signWithKey);
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            String strKeyname = sp.getString(signWithKey, "testkey");
            signPref.setValue(strKeyname);
            signPref.setOnPreferenceChangeListener(this);
        }

        // Output APK Name
        {
            ListPreference signPref = (ListPreference) manager.findPreference(apkNameKey);
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            String strNameIdx = sp.getString(apkNameKey, "1");
            signPref.setValue(strNameIdx);
            signPref.setOnPreferenceChangeListener(this);
        }

        // Full screen
        {
            CheckBoxPreference checkBox = (CheckBoxPreference) findPreference("FullScreen");
            boolean b = GlobalConfig.instance(SettingActivity.this).isFullScreen();
            checkBox.setChecked(b);
            checkBox.setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference arg0,
                                                          Object newValue) {
                            GlobalConfig.instance(SettingActivity.this)
                                    .setFullScreen(SettingActivity.this,
                                            (Boolean) newValue);
                            return true;
                        }
                    });
        }

        // App Theme
        {
            ListPreference appThemeList = (ListPreference) findPreference("AppTheme");
            int id = GlobalConfig.instance(SettingActivity.this).getThemeId();
            String[] values = getResources().getStringArray(R.array.theme_value);
            appThemeList.setValue(values[id]);
            appThemeList.setOnPreferenceChangeListener(this);
        }

        // Language
        {
            String key = "Language";
            ListPreference languageList = (ListPreference) findPreference(key);
            String language =
                    PreferenceManager.getDefaultSharedPreferences(this).getString(key, "");
            languageList.setValue(language);
            languageList.setOnPreferenceChangeListener(this);
        }

        // Smali editing
        CheckBoxPreference smaliPref = (CheckBoxPreference) findPreference(
                dex2smaliEnabledKey);
        if (isFreeVersion()) {
            PreferenceCategory category = (PreferenceCategory) findPreference(
                    "ApkBuilding");
            category.removePreference(smaliPref);
        } else {
            boolean b = isDex2smaliEnabled(this);
            smaliPref.setChecked(b);
            smaliPref.setSummary(b ? R.string.smali_edit_summary
                    : R.string.smali_edit_disabled_summary);
            smaliPref.setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference arg0,
                                                          Object newValue) {
                            CheckBoxPreference cb = (CheckBoxPreference) arg0;
                            boolean val = (Boolean) newValue;
                            cb.setChecked(val);
                            cb.setSummary(val ? R.string.smali_edit_summary
                                    : R.string.smali_edit_disabled_summary);
                            return true;
                        }
                    });
        }

        // Rebuild confirmation
        {
            CheckBoxPreference confirmPref = (CheckBoxPreference) findPreference(
                    confirmEnabledKey);
            boolean enabled = isRebuildConfirmEnabled(this);
            confirmPref.setChecked(enabled);
            confirmPref.setSummary(enabled ? R.string.rebuild_confirm_e_summary
                    : R.string.rebuild_confirm_d_summary);
            confirmPref.setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference arg0,
                                                          Object newValue) {
                            CheckBoxPreference cb = (CheckBoxPreference) arg0;
                            boolean val = (Boolean) newValue;
                            cb.setChecked(val);
                            cb.setSummary(val
                                    ? R.string.rebuild_confirm_e_summary
                                    : R.string.rebuild_confirm_d_summary);
                            return true;
                        }
                    });
        }

        // Clean Garbage
        {
            Preference p = findPreference(cleanKey);
            p.setOnPreferenceClickListener(this);
        }

        // Automatically rename or overwrite when extract files
        // The main purpose for this is to set the default value
        {
            ListPreference renamePref = (ListPreference) manager.findPreference(fileRenameKey);
            int option = getFileRenameOption(this);
            renamePref.setValue("" + option);
        }

        // Decode directory
        if (!BuildConfig.IS_PRO) {
            PreferenceScreen root = (PreferenceScreen) manager.findPreference("ROOT");
            Preference pref = manager.findPreference("DecodeCategory");
            root.removePreference(pref);
        } else {
            EditTextPreference dirPref = (EditTextPreference) manager.findPreference(decodeDirKey);
            dirPref.setOnPreferenceChangeListener(this);
            String decodeDir = getDecodeDirectory(this);
            if (decodeDir != null) {
                dirPref.setSummary(decodeDir);
            }
        }

        // Remove APK Building related settings and launcher icon setting
        if (BuildConfig.PARSER_ONLY) {
            PreferenceScreen root = (PreferenceScreen) manager.findPreference("ROOT");
            Preference pref = manager.findPreference("ApkBuilding");
            root.removePreference(pref);

            Preference iconPref = manager.findPreference(appIconKey);
            PreferenceCategory category = (PreferenceCategory) findPreference("Appearance");
            category.removePreference(iconPref);
        }
    }

    // Get decode mode summary according to the value
    private int getDecodeModeSummary(String value) {
        if ("0".equals(value)) {
            return R.string.summary_decode_all_files;
        } else if ("1".equals(value)) {
            return R.string.summary_decode_partial_files;
        } else {
            return R.string.summary_decide_at_decoding;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        // app list orders
        if (appOrderKey.equals(key)) {
            String orderValue = (String) newValue;
            Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putString(appOrderKey, orderValue);
            editor.apply();

            preference.setSummary(orderValue);
        }

        // App theme
        else if ("AppTheme".equals(key)) {
            try {
                int themeId = Integer.valueOf((String) newValue);
                GlobalConfig.instance(this).updateThemeId(themeId);
            } catch (Exception ignored) {
            }
        }

        // Decode directory
        else if (decodeDirKey.equals(key)) {
            boolean isValidValue = false;
            String dir = (String) newValue;

            // Not empty
            if (dir != null && !"".equals(dir.trim())) {
                if (dirCanWrite(dir)) {
                    isValidValue = true;
                    preference.setSummary(dir);
                } else {
                    String msg = String.format(getString(R.string.invalid_directory), dir);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
            }

            if (!isValidValue) {
                preference.setSummary(R.string.decode_dir_summary_may_change);
            }
        }
        // decode mode
        else if (decodeModeKey.equals(key)) {
            String value = (String) newValue;
            preference.setSummary(getDecodeModeSummary(value));
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String clickedKey = preference.getKey();
        final String path = this.getFilesDir().getAbsolutePath();
        if (cleanKey.equals(clickedKey)) {
            ProcessingDialog dlg = new ProcessingDialog(this,
                    new ProcessingInterface() {
                        @Override
                        public void process() throws Exception {
                            cleanSdcard();
                            CommandRunner cr = new CommandRunner();
                            cr.runCommand("rm -rf " + path + "/decoded\n"
                                    + "rm -rf " + path + "/temp", null, 8000);
                        }

                        @Override
                        public void afterProcess() {
                        }

                    }, R.string.temp_file_cleaned);
            dlg.show();
        }
        return false;
    }

    // Clean the ApkEditor folder except backups
    protected void cleanSdcard() {
        File f = new File(SDCard.getRootDirectory() + "/ApkEditor");
        if (!f.exists() || !f.isDirectory()) {
            return;
        }

        String[] keepingFolders = {"backups", ".projects"};
        File[] subfiles = f.listFiles();
        if (subfiles != null)
            for (File subfile : subfiles) {
                if (subfile.isDirectory()) {
                    boolean bKeep = false;
                    String folder = subfile.getName();
                    for (String keepDir : keepingFolders) {
                        if (folder.equals(keepDir)) {
                            bKeep = true;
                            break;
                        }
                    }
                    if (!bKeep) {
                        cleanDirectory(subfile);
                        subfile.delete();
                    }
                } else {
                    subfile.delete();
                }
            }
    }

    protected void cleanDirectory(File dir) {
        File[] subfiles = dir.listFiles();
        if (subfiles != null)
            for (File f : subfiles) {
                if (f.isDirectory()) {
                    cleanDirectory(f);
                    f.delete();
                } else {
                    f.delete();
                }
            }
    }
}
