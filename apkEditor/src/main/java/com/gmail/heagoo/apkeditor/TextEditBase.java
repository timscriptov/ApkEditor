package com.gmail.heagoo.apkeditor;

import static com.gmail.heagoo.apkeditor.MoreEditorOptionAdapter.CMD_CODE_SNIPPET;
import static com.gmail.heagoo.apkeditor.MoreEditorOptionAdapter.CMD_COLORPAD;
import static com.gmail.heagoo.apkeditor.MoreEditorOptionAdapter.CMD_COMMENT_LINES;
import static com.gmail.heagoo.apkeditor.MoreEditorOptionAdapter.CMD_DELETE_LINES;
import static com.gmail.heagoo.apkeditor.MoreEditorOptionAdapter.CMD_HELP;
import static com.gmail.heagoo.apkeditor.MoreEditorOptionAdapter.CMD_HTML;
import static com.gmail.heagoo.apkeditor.MoreEditorOptionAdapter.CMD_SETTINGS;
import static com.gmail.heagoo.apkeditor.MoreEditorOptionAdapter.CMD_TO_JAVA;
import static com.gmail.heagoo.apkeditor.TextEditBase.isSmali;
import static com.gmail.heagoo.apkeditor.TextEditBase.isXml;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.common.colormixer.ColorMixer;
import com.common.colormixer.ColorMixerDialog;
import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.editor.HtmlViewDialog;
import com.gmail.heagoo.apkeditor.inf.IJavaExtractor;
import com.gmail.heagoo.apkeditor.util.AndroidBug5497Workaround;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.ClipboardUtil;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.common.Display;
import com.gmail.heagoo.common.Pair;
import com.gmail.heagoo.common.RefInvoke;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.neweditor.Document;
import com.gmail.heagoo.neweditor.InputMethodWatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Adapter for the more options
class MoreEditorOptionAdapter extends BaseAdapter {
    static final int CMD_HTML = 0;
    static final int CMD_COLORPAD = 1;
    static final int CMD_DELETE_LINES = 2;
    static final int CMD_SETTINGS = 3;
    static final int CMD_HELP = 4;
    static final int CMD_COMMENT_LINES = 5;
    static final int CMD_CODE_SNIPPET = 6;
    static final int CMD_TO_JAVA = 7;

    private Context ctx;

    // Commands recorded for all the position
    private List<Integer> commands = new ArrayList<>();

    // Pair contains image resource id and string id
    private List<Pair<Integer, Integer>> optionResIds = new ArrayList<>();

    MoreEditorOptionAdapter(Context ctx, String filePath) {
        this.ctx = ctx;

        if (isSmali(filePath) || isXml(filePath)) {
            optionResIds.add(new Pair<>(-1, R.string.html));
            commands.add(CMD_HTML);
        }

        optionResIds.add(new Pair<>(R.drawable.ic_colorpad, R.string.colorpad));
        commands.add(CMD_COLORPAD);

        if (isSmali(filePath)) { // Code snippet
            optionResIds.add(new Pair<>(-1, R.string.code_snippet));
            commands.add(CMD_CODE_SNIPPET);
        }

        optionResIds.add(new Pair<>(-1, R.string.delete_lines));
        commands.add(CMD_DELETE_LINES);

        if (isSmali(filePath)) { // Comment lines & to java code
            optionResIds.add(new Pair<>(-1, R.string.comment_lines));
            commands.add(CMD_COMMENT_LINES);

            if (BuildConfig.IS_PRO) {
                optionResIds.add(new Pair<>(R.drawable.ic_java, R.string.java_code));
                commands.add(CMD_TO_JAVA);
            }
        }

        optionResIds.add(new Pair<>(R.drawable.ic_setting, R.string.settings));
        commands.add(CMD_SETTINGS);

        optionResIds.add(new Pair<>(-1, R.string.help));
        commands.add(CMD_HELP);
    }

    // Get option number
    public int getOptions() {
        return optionResIds.size();
    }

    public int getCommandByPosition(int position) {
        if (position < commands.size()) {
            return commands.get(position);
        }
        return -1;
    }

    @Override
    public int getCount() {
        return optionResIds.size();
    }

    @Override
    public Object getItem(int position) {
        return optionResIds.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(ctx).inflate(R.layout.item_more_option, null);
            holder = new ViewHolder();
            holder.image = (ImageView) convertView.findViewById(R.id.menu_icon);
            holder.title = (TextView) convertView.findViewById(R.id.menu_title);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Pair<Integer, Integer> data = optionResIds.get(position);
        if (data.first > 0) {
            holder.image.setImageResource(data.first);
        } else {
            holder.image.setImageBitmap(null);
        }
        holder.title.setText(data.second);

        return convertView;
    }

    private static class ViewHolder {
        public ImageView image;
        public TextView title;
    }
}

public abstract class TextEditBase extends CustomizedLangActivity implements ColorMixer.OnColorChangedListener, SmaliMethodWindowHelper.ISmaliMethodClicked, LinesOpDialogHelper.ILinesOperation {
    protected String searchString;
    protected boolean batchMode;
    protected String displayName;
    protected List<String> fileList;
    protected List<Integer> startLineList;
    protected List<String> syntaxFileList;
    protected List<String> extraStringList;
    protected int curFileIndex = 0;
    // Lines are wrapped or not
    protected boolean textWrap = true;
    // Used to record the save state when re-create the activity
    protected boolean modifySaved = false;
    // When rotate and doc changed, save the changed content to unsavedFilePath
    protected String unsavedFilePath;
    // Record all the modified files
    protected ArrayList<String> modifiedFiles = new ArrayList<>();
    // Remember lines operation (delete or comment)
    protected int linesOP;
    // private int currentDocument = 0;
    // private ArrayList<Document> openDocuments;
    protected Document curDocument;
    // For big file or not
    private boolean isForBigFile;
    // Fix the Android bug or not
    private boolean fixBug;
    private String apkPath;
    // File path current editing
    private String curFilePath;
    // Use to check if the input method is shown
    private boolean bInputMethodShown;
    // For popup window
    private SmaliMethodWindowHelper popupWindowHelper = new SmaliMethodWindowHelper(this);
    // For bottom menu
    private String specialStrings[] = {
            ",.?!:;~-_=\"'/@*+()<>{}[]%&$|\\#^",
            "<>\":=/@+.-?#_()[]{}\\;!$%^&*|~',",
            "(){};.=\"'|&![]@<>+-*/?:,_\\^%#~$",
    };
    private int specialStrIndex = -1;
    private View specialCharLayout;
    private LinearLayout menuLayout;
    // To show HTML dialog
    private HtmlViewDialog htmlDialog;

    public TextEditBase(boolean isForBigFile, boolean fixBug) {
        this.isForBigFile = isForBigFile;
        this.fixBug = fixBug;
    }

    public static boolean isSmali(String filePath) {
        return filePath != null && filePath.endsWith(".smali");
    }

    public static boolean isJava(String filePath) {
        return filePath != null && filePath.endsWith(".java");
    }

    public static boolean isXml(String filePath) {
        return filePath.endsWith(".xml");
    }

    // Utility function
    // Check all lines betweeen [start, end] starts with strStart or not
    protected static boolean allStartsWith(String[] lines, int start, int end, String strStart) {
        boolean allMatch = true;
        for (int i = start; i <= end; i++) {
            if (!lines[i - 1].trim().startsWith(strStart)) {
                allMatch = false;
                break;
            }
        }
        return allMatch;
    }

    @SuppressLint("NewApi")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        // Seems there are some problems in full screen?
        boolean isFullScreen = GlobalConfig.instance(this).isFullScreen();
        if (isFullScreen) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        // Need double check this line?
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        int themeId = GlobalConfig.instance(this).getThemeId();
        // sawsem theme
//        if (themeId != GlobalConfig.THEME_LIGHT) {
//            setTheme(R.style.AppDarkTheme);
//        }

        if (isForBigFile) {
            this.setContentView(R.layout.activity_editor_bigfile);
        } else {
            this.textWrap = SettingEditorActivity.isLineWrap(this);
            if (this.textWrap) {
                this.setContentView(
                        R.layout.activity_editor_wrap);
            } else {
                this.setContentView(
                        R.layout.activity_editor);
            }
        }

        // Bug fix: http://blog.csdn.net/huangxiaoguo1/article/details/53081229?locationNum=3&fps=1
        if (fixBug) {
            if (isFullScreen) {
                AndroidBug5497Workaround.assistActivity(this);
            }
        }

        // Get data from intent
        Intent intent = getIntent();
        String filePath = ActivityUtil.getParam(intent, "xmlPath");
        this.apkPath = ActivityUtil.getParam(intent, "apkPath");
        this.searchString = ActivityUtil.getParam(intent, "searchString");
        try {
            // Open a single file
            if (filePath != null) {
                this.batchMode = false;

                int startLine = -1;
                String strLine = ActivityUtil.getParam(intent, "startLine");
                if (strLine != null) {
                    startLine = Integer.valueOf(strLine);
                }
                String syntaxFileName = ActivityUtil.getParam(intent, "syntaxFileName");
                String extraString = ActivityUtil.getParam(intent, "extraString");
                this.displayName = ActivityUtil.getParam(intent, "displayFileName");

                // Put values to list container
                this.fileList = new ArrayList<>(1);
                this.fileList.add(filePath);
                this.startLineList = new ArrayList<>();
                this.startLineList.add(startLine);
                this.syntaxFileList = new ArrayList<>(1);
                this.syntaxFileList.add(syntaxFileName);
                this.extraStringList = new ArrayList<>(1);
                this.extraStringList.add(extraString);
            }
            // Open multiple files
            else {
                this.batchMode = true;
                this.fileList = ActivityUtil.getStringArray(intent, "fileList");
                this.curFileIndex = ActivityUtil.getIntParam(intent, "curFileIndex");
                this.startLineList = ActivityUtil.getIntArray(intent, "startLineList");
                this.syntaxFileList = ActivityUtil.getStringArray(intent, "syntaxFileList");
                this.extraStringList = ActivityUtil.getStringArray(intent, "extraStringList");

                // Set the default value if not passed to us
                if (startLineList == null) {
                    startLineList = new ArrayList<>(fileList.size());
                    for (int i = 0; i < fileList.size(); ++i) {
                        startLineList.add(-1);
                    }
                }
                if (syntaxFileList == null) {
                    syntaxFileList = new ArrayList<>(fileList.size());
                    for (int i = 0; i < fileList.size(); ++i) {
                        syntaxFileList.add(null);
                    }
                }
                if (extraStringList == null) {
                    extraStringList = new ArrayList<>(fileList.size());
                    for (int i = 0; i < fileList.size(); ++i) {
                        extraStringList.add(null);
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.failed, Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }

        // Activity re-created (for example, rotate the screen)
        if (savedInstanceState != null) {
            this.curFileIndex = savedInstanceState.getInt("curFileIndex");
            this.modifySaved = savedInstanceState.getBoolean("modifySaved", false);
            if (modifySaved) {
                setResult();
            }
            boolean changed = savedInstanceState.getBoolean("docChanged", false);
            if (changed) {
                this.unsavedFilePath = savedInstanceState.getString("unsavedFilePath");
            }
        }

        initBottomMenu();
    }

    protected String getCurrentFilePath() {
        return curFilePath;
    }

    protected void setCurrentFilePath(String filePath) {
        this.curFilePath = filePath;

        // When current path changed, then re-initialize the special layout
        updateSpecialCharButton(filePath);
    }

    private void initBottomMenu() {
        this.specialCharLayout = findViewById(R.id.special_char_layout);
        this.menuLayout = (LinearLayout) findViewById(R.id.menu_layout);

        View switchView = findViewById(R.id.switch_view);
        if (SettingEditorActivity.symbolInputEnabled(this)) {
            switchView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (menuLayout.getVisibility() == View.VISIBLE) {
                        specialCharLayout.setVisibility(View.VISIBLE);
                        menuLayout.setVisibility(View.INVISIBLE);
                    } else {
                        menuLayout.setVisibility(View.VISIBLE);
                        specialCharLayout.setVisibility(View.INVISIBLE);
                    }
                }
            });
        } else {
            switchView.setVisibility(View.GONE);
            findViewById(R.id.separate_view).setVisibility(View.GONE);
        }
    }

    private void updateSpecialCharButton(String filePath) {
        int idx = getSpecialStringIndex(filePath);
        if (idx == this.specialStrIndex) { // File type keeps the same
            return;
        } else {
            this.specialStrIndex = idx;
        }

        LinearLayout parentView = (LinearLayout) findViewById(R.id.special_char_container);
        parentView.removeAllViews();

        int screenWidth = Display.getWidth(this);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                screenWidth / 11, ViewGroup.LayoutParams.MATCH_PARENT);

        String str = this.specialStrings[this.specialStrIndex];
        for (int i = 0; i < str.length(); ++i) {
            TextView tv = new TextView(this);

            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            tv.setBackgroundResource(R.drawable.selector_iv_button);
            tv.setText("" + str.charAt(i));
            tv.setTextColor(0xffffffff);
            tv.setGravity(Gravity.CENTER);
            tv.setTag(i);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int idx = (Integer) v.getTag();
                    specialCharClicked(idx);
                }
            });

            parentView.addView(tv, param);
        }
    }

    // If input method visible, then should special char, otherwise show menu
    private void updateBottomMenu(boolean inputMethodVisible) {
        if (SettingEditorActivity.symbolInputEnabled(this)) {
            if (inputMethodVisible) {
                specialCharLayout.setVisibility(View.VISIBLE);
                menuLayout.setVisibility(View.INVISIBLE);
            } else {
                menuLayout.setVisibility(View.VISIBLE);
                specialCharLayout.setVisibility(View.INVISIBLE);
            }
        }
    }

    // .xml -> 1, .java-> 2, default -> 0
    private int getSpecialStringIndex(String filePath) {
        if (filePath == null) {
            return specialStrIndex;
        }

        int index;
        if (filePath.endsWith(".xml")) {
            index = 1;
        } else if (filePath.endsWith(".java")) {
            index = 2;
        } else {
            index = 0;
        }
        return index;
    }

    // For special char button
    protected String getSpecialChars(String filePath) {
        if (filePath == null) {
            if (specialStrIndex >= 0) {
                return specialStrings[specialStrIndex];
            } else {
                return "";
            }
        }

        return specialStrings[getSpecialStringIndex(filePath)];
    }

    protected abstract void specialCharClicked(int idx);

    // Make parent activity aware the modification
    protected void setResult() {
        // Set modified flag as the result
        Intent intent = new Intent();
        if (!batchMode) {
            intent.putExtra("xmlPath", curFilePath);
            intent.putExtra("extraString", extraStringList.get(0));
        } else {
            if (!modifiedFiles.contains(curFilePath)) {
                modifiedFiles.add(curFilePath);
            }
            intent.putStringArrayListExtra("modifiedFiles", modifiedFiles);
        }
        setResult(1, intent);
    }

    protected int getNumberDigits(int lines) {
        int nd = 1;
        while (lines >= 10) {
            lines /= 10;
            nd++;
        }
        return Math.max(2, nd);
    }

    protected void setupInputMethodMonitor(final InputMethodWatcher watcher) {
        final View contentView = this.findViewById(android.R.id.content);
        contentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                Rect r = new Rect();
                contentView.getWindowVisibleDisplayFrame(r);
                int screenHeight = contentView.getRootView().getHeight();

                // r.bottom is the position above soft keypad or device button.
                // if keypad is shown, the r.bottom is smaller than that before.
                int keypadHeight = screenHeight - r.bottom;

                // 0.15 ratio is perhaps enough to determine keypad height.
                bInputMethodShown = (keypadHeight > screenHeight * 0.15);

                watcher.setInputMethodVisible(bInputMethodShown);

                updateBottomMenu(bInputMethodShown);
            }
        });
    }

    protected boolean isInputMethodShown() {
        return bInputMethodShown;
    }

    protected void showMoreOptions(View parent) {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View view = layoutInflater.inflate(R.layout.popup_list, null);
        ListView lv_group = (ListView) view.findViewById(R.id.lvGroup);

        final MoreEditorOptionAdapter adapter = new MoreEditorOptionAdapter(
                getApplicationContext(), curFilePath);
        lv_group.setAdapter(adapter);

        // Create a popup window
        int optionNum = adapter.getOptions();
        int height = Display.dip2px(this, 50 * optionNum + (optionNum - 1)); // n * 50dip content + (n-1) * 1dip divider
        int width = Display.getWidth(this) / 2;
        final PopupWindow popupWindow = new PopupWindow(view, width, height);

        lv_group.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                                    int position, long id) {
                int cmd = adapter.getCommandByPosition(position);
                switch (cmd) {
                    case CMD_HTML:
                        showHtmlDialog();
                        break;
                    case CMD_SETTINGS:
                        Intent intent = new Intent(TextEditBase.this, SettingEditorActivity.class);
                        startActivity(intent);
                        break;
                    case CMD_COLORPAD:
                        new ColorMixerDialog(TextEditBase.this, 0xffffffff, TextEditBase.this).show();
                        break;
                    case CMD_CODE_SNIPPET:
                        new SmaliCodeDialog(TextEditBase.this, curFilePath).show();
                        break;
                    case CMD_DELETE_LINES:
                        TextEditBase.this.linesOP = CMD_DELETE_LINES;
                        new LinesOpDialogHelper().showDialog(TextEditBase.this, R.string.delete_lines, TextEditBase.this);
                        break;
                    case CMD_TO_JAVA:
                        showJavaCodeWithTip();
                        break;
                    case CMD_COMMENT_LINES:
                        TextEditBase.this.linesOP = CMD_COMMENT_LINES;
                        new LinesOpDialogHelper().showDialog(TextEditBase.this, R.string.comment_lines, TextEditBase.this);
                        break;
                    case CMD_HELP:
                        Intent it = new Intent(TextEditBase.this, EditorHelpActivity.class);
                        startActivity(it);
                        break;
                }

                if (popupWindow != null) {
                    popupWindow.dismiss();
                }
            }
        });


        // Focus and allow disappear touch outside
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);

        // To make it can disappear when press return button
        popupWindow.setBackgroundDrawable(new BitmapDrawable());


        // Display position
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        int xPos = windowManager.getDefaultDisplay().getWidth() / 2
                - popupWindow.getWidth() / 2;

        popupWindow.showAsDropDown(parent, xPos, 0);
    }

    private void showHtmlDialog() {
        if (htmlDialog == null) {
            htmlDialog = new HtmlViewDialog(this);
        }
        htmlDialog.show(curFilePath, getDisplayName(curFilePath));
    }

    private void showJavaCodeWithTip() {
        final String key = "java_edit_tip_shown";
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean shown = sp.getBoolean(key, false);

        if (!shown) {
            AlertDialog.Builder tipDlg = new AlertDialog.Builder(this)
                    .setTitle(R.string.please_note)
                    .setMessage(R.string.java_code_edit_tip)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showJavaCode();
                        }
                    });
            tipDlg.show();
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(key, true);
            editor.commit();
        } else {
            showJavaCode();
        }
    }

    // Smali -> Java
    private void showJavaCode() {
        if (apkPath == null) {
            Toast.makeText(this, "Internal error: cannot find apk path to decode java code, please contact the author.", Toast.LENGTH_LONG).show();
            return;
        }

        final String workingDirectory;
        try {
            workingDirectory = SDCard.makeWorkingDir(this);
        } catch (Exception ignored) {
            Toast.makeText(this, "Cannot make working directory.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] dexAndClass = getDexAndClassName();
        final String dexName = dexAndClass[0];
        final String className = dexAndClass[1];
        if (className == null) {
            Toast.makeText(this, "Internal error: Cannot get class name, please contact the author.", Toast.LENGTH_LONG).show();
            return;
        }

        new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
            boolean succeed = false;
            String errMessage;

            @Override
            public void process() throws Exception {
                IJavaExtractor extractor = (IJavaExtractor)
                        RefInvoke.createInstance("com.gmail.heagoo.apkeditor.pro.JavaExtractor",
                                new Class[]{String.class, String.class, String.class, String.class},
                                new Object[]{apkPath, dexName, className, workingDirectory});

                if (extractor != null) {
                    succeed = extractor.extract();
                    if (!succeed) {
                        errMessage = extractor.getErrorMessage();
                    }
                }
            }

            @Override
            public void afterProcess() {
                if (succeed) {
                    String relativePath = className.substring(1);
                    String path = workingDirectory + relativePath + ".java";

                    boolean fileExist = new File(path).exists();
                    if (!fileExist) {
                        do {
                            // Try to remove string after $
                            int position = relativePath.lastIndexOf('$');
                            if (position != -1) {
                                relativePath = relativePath.substring(0, position);
                                path = workingDirectory + relativePath + ".java";
                                fileExist = new File(path).exists();
                                if (fileExist) {
                                    break;
                                }
                            }

                            // Try to get the file in defpackage folder
                            path = workingDirectory + "defpackage/" + relativePath + ".java";
                            fileExist = new File(path).exists();
                            if (fileExist) {
                                break;
                            }
                        } while (false);
                    }

                    if (!fileExist) {
                        Toast.makeText(TextEditBase.this,
                                R.string.cannot_find_java_file,
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    Intent intent = TextEditor.getEditorIntent(TextEditBase.this, path, null);
                    startActivity(intent);
                } else {
                    Toast.makeText(TextEditBase.this, errMessage, Toast.LENGTH_LONG).show();
                }
            }
        }, -1).show();
    }

    // Get dex and class name from the file path
    // Return something like: classes2.dex, Lcom.gmail.heagoo.MainActivity
    private String[] getDexAndClassName() {
        String dexName = "classes.dex";
        String[] dirs = curFilePath.split("/");

        int i = 0;
        for (; i < dirs.length; i++) {
            if ("smali".equals(dirs[i])) {
                break;
            }
            if (dirs[i].startsWith("smali_")) {
                dexName = dirs[i].substring(6) + ".dex";
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append('L');
        for (i = i + 1; i < dirs.length; i++) {
            String name = dirs[i];
            if (i == dirs.length - 1) {
                if (name.length() > 6 && name.endsWith(".smali")) {
                    name = name.substring(0, name.length() - 6);
                    sb.append(name);
                }
            } else {
                sb.append(name);
                sb.append('/');
            }
        }

        if (sb.length() == 0) {
            return null;
        }

        return new String[]{dexName, sb.toString()};
    }

    public String getDisplayName(String filePath) {
        // For batch mode, always show file name
        if (!batchMode && this.displayName != null) {
            return this.displayName;
        }

        int pos = filePath.lastIndexOf('/');
        if (pos != -1) {
            return filePath.substring(pos + 1);
        }
        return filePath;
    }

    public abstract void insertString(String str);

    // For colorpad callback
    @Override
    public void onColorChange(int argb) {
        String strColor = String.format("#%08x", argb);
        ClipboardUtil.copyToClipboard(this, strColor);
        String message = String.format(getString(R.string.copied_to_clipboard), strColor);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void operateLines(int from, int to) {
        Toast.makeText(this, "Not implemented.", Toast.LENGTH_SHORT).show();
    }

    // If popup window not exist, create it in async task
    // otherwise directly show it
    protected void showPopWindow(View parent) {
        // The popup window exist and is for current file
        if (popupWindowHelper != null && curFilePath.equals(popupWindowHelper.getFile())) {
            popupWindowHelper.doPopWindowShow(parent);
        } else {
            popupWindowHelper.asyncShowPopup(
                    this, curFilePath, curDocument.getText(), parent);
        }
    }
}
