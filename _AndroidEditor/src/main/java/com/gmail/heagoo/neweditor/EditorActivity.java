package com.gmail.heagoo.neweditor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewAnimator;

import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.common.ProcessingDialog;
import com.gmail.heagoo.common.RefInvoke;

import java.io.File;
import java.io.IOException;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditorActivity extends CustomizedLangActivity implements OnClickListener {
    private static int AUTODELAY = 300;
    private static int LINECOUNTDELAY = 300;
    private static int TYPEDELAY = 400;
    private static int SCROLLDELAY = 100;
    public int previousLineCount;
    protected ObEditText textEditor;
    // Used to record the save state when re-create the activity
    protected boolean modifySaved = false;
    UpdateLineCount ulcTask = new UpdateLineCount();
    SyntaxHighLight shTask = new SyntaxHighLight();
    private boolean syntaxHighlighting = true;
    private boolean hlChange = false;
    private int hlEnd = -1;
    private int hlStart = -1;
    private boolean textWrap = true;
    private int fontSize;
    private int highlightSize = 50;
    private Handler mHandler = new Handler();
    private boolean autoTextChange;
    private LinearLayout editorInnerLayout;
    private FrameLayout editorOuterLayout;
    private EditText lineNumbers;
    private HorizontalScrollView editorHorizontalLayout;
    private View lineDivider;
    private ObScrollView editorScrollView;
    private ViewAnimator docFindAnim;
    private SlidingDrawer documentListDrawer;
    private EditText findText;
    private EditText replaceText;
    private ImageButton findButton;
    private ImageButton replaceButton;
    private ImageView openFindButton;
    private ImageView saveFileButton;
    private ImageView configButton;
    private ToggleButton toggleIgnoreCase;
    private ToggleButton toggleRegularExpression;
    private LinearLayout editorLayout;
    private ImageView documentListButton; // Slide image
    private ScrollView scrollView; // edit scroll view
    // private int currentDocument = 0;
    // private ArrayList<Document> openDocuments;
    private String filePath;
    private String realFilePath; // when not null, need to copy back to real path
    private String syntaxFileName;
    private boolean isRootMode;
    private Document curDocument;
    private boolean updateLineCount = true;
    private int previousMaxDigits;
    private boolean wasOpenedDrawer = false; // search panel opened
    private boolean lastRedoState = true;
    private boolean lastSaveState = true; // Latest changed saved or not
    private boolean lastChangedState = false; // Like a cache for document
    // changed state
    private boolean lastUndoState = true;
    // ///////////////////////////////// Not directly for editor
    // ////////////////////////////////////////
    private LinearLayout editorView;
    // Resource ID
    private int resId_tooBig;
    private int resId_fileSaved;
    private int resId_notFound;

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        this.textWrap = true;
        if (this.textWrap) {
            this.setContentView(R.layout.editorutil_main);
        } else {
            this.setContentView(R.layout.editorutil_main);
        }

        try {
            Bundle bundle = getIntent().getExtras();
            this.filePath = bundle.getString("filePath");
            this.realFilePath = bundle.getString("realFilePath");
            this.syntaxFileName = bundle.getString(
                    "syntaxFileName");
            this.isRootMode = bundle.getBoolean("isRootMode");
            int[] resIds = bundle.getIntArray("resourceIds");
            this.resId_tooBig = resIds[0];
            this.resId_fileSaved = resIds[1];
            this.resId_notFound = resIds[2];
        } catch (Exception e) {
            Toast.makeText(this, "failed", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }

        // Log.d("DEBUG", "onCreate() called");
        // Activity re-created (for example, rotate the screen)
        if (savedInstanceState != null) {
            this.modifySaved = savedInstanceState.getBoolean("modifySaved",
                    false);
            if (modifySaved) {
                setResult();
            }
        }

        initData();
        // Log.d("DEBUG", "initData() finished");
        initMainView();
        // Log.d("DEBUG", "initMainView() finished");
        initEditView();
        // Log.d("DEBUG", "initEditView() finished");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("modifySaved", modifySaved);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Log.d("DEBUG", "onStart() finished");
    }

    // @Override
    // public void onResume() {
    // super.onResume();
    //
    // }

    @Override
    protected void onResume() {
        super.onResume();

        setupOnclickListener();

        // Log.d("DEBUG", "onResume() called.");

        // To check font size changed or not
        int fontSize = 12; // SettingEditorActivity.getFontSize(this);
        if (this.fontSize != fontSize) {
            this.fontSize = fontSize;
            changeFontSize(fontSize);
        }
        int lines = this.curDocument.getText().split("\n").length + 1;
        int digits = this.ulcTask.getNumberDigits(lines);
        changeLineNumbers(true, digits);

        // Why call updateEditor??
        new Handler().postDelayed(() -> {
            // DroidEditActivity.this.adjustMenus();
            EditorActivity.this.updateEditor(true);
        }, 400);
    }

    private boolean isXml() {
        return filePath.endsWith(".xml");
    }

    private boolean isSmali() {
        return filePath.endsWith(".smali");
    }

    private boolean isValuesXml() {
        String[] folders = filePath.split("/");
        if (folders.length > 2) {
            String folder = folders[folders.length - 2];
            if (folder.startsWith("values")) {
                return true;
            }
        }
        return false;
    }

    private String getFileName() {
        if (this.realFilePath != null) {
            int pos = realFilePath.lastIndexOf('/');
            if (pos != -1) {
                return realFilePath.substring(pos + 1);
            }
        }

        int pos = filePath.lastIndexOf('/');
        if (pos != -1) {
            return filePath.substring(pos + 1);
        }
        return filePath;
    }

    private void initData() {
        this.curDocument = new Document(this, new File(filePath), syntaxFileName);
        try {
            this.curDocument.load(this, filePath, resId_tooBig);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to open " + filePath,
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void initMainView() {
        // File name title
        TextView filenameTv = (TextView) this.findViewById(R.id.filename);
        filenameTv.setText(getFileName());

        this.editorView = (LinearLayout) this.findViewById(R.id.editorView);
    }

    @SuppressWarnings("deprecation")
    private void initEditView() {
        this.editorInnerLayout = (LinearLayout) findViewById(R.id.editorLayout);
        this.editorOuterLayout = (FrameLayout) findViewById(R.id.center_layout);
        this.lineNumbers = (EditText) findViewById(R.id.lineNumbers);
        this.textEditor = (ObEditText) findViewById(R.id.editor);
        this.lineDivider = findViewById(R.id.divider);
        this.editorScrollView = (com.gmail.heagoo.neweditor.ObScrollView) findViewById(R.id.editorScrollview);
        // this.editorHorizontalLayout = (HorizontalScrollView)
        // findViewById(R.id.hScrollView);
        this.docFindAnim = (ViewAnimator) findViewById(R.id.searchAnimator);
        this.documentListDrawer = (SlidingDrawer) findViewById(R.id.sliding_drawer);
        this.findText = (EditText) findViewById(R.id.findEdit);
        this.replaceText = (EditText) findViewById(R.id.replaceEdit);
        this.findButton = (ImageButton) findViewById(R.id.findBtn);
        this.replaceButton = (ImageButton) findViewById(R.id.replaceBtn);
        this.openFindButton = (ImageView) findViewById(R.id.openFindBtn);
        this.toggleIgnoreCase = (ToggleButton) findViewById(R.id.checkBoxIgnoreCase);
        this.toggleRegularExpression = (ToggleButton) findViewById(R.id.checkBoxRegexp);
        this.saveFileButton = (ImageView) findViewById(R.id.saveBtn);
        this.configButton = (ImageView) findViewById(R.id.configBtn);
        this.editorLayout = this.editorInnerLayout;
        this.documentListButton = (ImageView) findViewById(R.id.panel_button);
        this.replaceButton = (ImageButton) findViewById(R.id.replaceBtn);
        this.scrollView = editorScrollView;

        // Text wrap or not
        this.textEditor.setWrapped(textWrap);
        // No suggestions:721041
        this.textEditor.setInputType(721041);
        // font size
        this.fontSize = 12;// SettingEditorActivity.getFontSize(this);
        changeFontSize(fontSize);

        // Initial content
        String strContent = this.curDocument.getText();
        if (strContent != null) {
            textEditor.setText(strContent);
            syntaxHighlight(0, strContent.length(), true);
        }

        // On click listener
        this.openFindButton.setOnClickListener(this);
        this.findButton.setOnClickListener(this);
        this.replaceButton.setOnClickListener(this);
        this.saveFileButton.setOnClickListener(this);
        this.configButton.setOnClickListener(this);

        // At the beginning, disable the save & replace button
        this.saveFileButton.getDrawable().setAlpha(80);
        this.saveFileButton.setClickable(false);
        this.replaceButton.setEnabled(false);

        ColorTheme colorTheme = new ColorTheme(this);
        this.editorInnerLayout.setBackgroundColor(colorTheme
                .getBackgroundColor());
        this.editorOuterLayout.setBackgroundColor(colorTheme
                .getBackgroundColor());
        this.lineNumbers.setBackgroundColor(colorTheme.getBackgroundColor());
        this.textEditor.setBackgroundColor(colorTheme.getBackgroundColor());
        this.textEditor.setTextColor(colorTheme.getForeground());
        this.lineNumbers.setTextColor(colorTheme.getForeground());
        this.lineDivider.setBackgroundColor(colorTheme.getForeground());
        this.textEditor.setBracketSpanColor(getInverseColor(colorTheme
                .getBackgroundColor()));
        this.lineNumbers.setOnLongClickListener(null);
    }

    @SuppressWarnings("deprecation")
    private void setupOnclickListener() {
        this.documentListDrawer
                .setOnDrawerOpenListener(EditorActivity.this::adjustOpenedDrawer);
        this.documentListDrawer
                .setOnDrawerCloseListener(EditorActivity.this::adjustClosedDrawer);

        // Selection change listener
        this.textEditor.setTextSelectionListener((selStart, selEnd) -> {
            if (EditorActivity.this.documentListDrawer.isOpened()) {
                EditorActivity.this.updateReplaceState();
            }
        });

        // Text change listener
        // Log.d("DEBUG", "will call addTextChangedListener");
        this.textEditor.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                // Log.d("DEBUG", "Text change detected, start=" + start +
                // ", before=" + before + ", count=" + count);
                Document document = EditorActivity.this.getCurrentDocument();
                boolean wasChanged = document.changed();
                if (!EditorActivity.this.autoTextChange) {
                    document.textChanged(s, start, before, count);
                }
                EditorActivity.this.syntaxHighlight(
                        Math.min(EditorActivity.this.hlStart, start),
                        Math.max(before, count) + start, true);
                EditorActivity.this.updateUndoRedoState();
                EditorActivity.this.updateLineCount(true);
                if (!wasChanged && document.changed()) {
                    EditorActivity.this.updateSaveState();
                    // TestActivity.this.notifyDocumentListChanged();
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            public void afterTextChanged(Editable e) {
            }
        });

        // When scroll, redraw the highlighting
        this.editorScrollView.setScrollViewListener((scrollView, x, y, oldx, oldy) -> EditorActivity.this.syntaxHighlight(-1, -1, false));

        // Find and replace text key listener
        this.findText.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP
                        && keyCode == KeyEvent.KEYCODE_ENTER) {
                    EditorActivity.this.executeFindWrapAction(true);
                    return true;
                } else if (EditorActivity.this.documentListDrawer
                        .getVisibility() == View.VISIBLE
                        && event.getAction() == KeyEvent.ACTION_UP
                        && keyCode == KeyEvent.KEYCODE_BACK) {
                    EditorActivity.this.documentListDrawer.close();
                    return true;
                } else {
                    EditorActivity.this.updateReplaceState();
                    return false;
                }
            }
        });
        this.replaceText.setOnKeyListener((v, keyCode, event) -> {
            if (EditorActivity.this.documentListDrawer.getVisibility() == View.VISIBLE
                    && event.getAction() == KeyEvent.ACTION_UP
                    && keyCode == KeyEvent.KEYCODE_BACK) {
                EditorActivity.this.documentListDrawer.close();
                return true;
            }
            EditorActivity.this.updateReplaceState();
            return false;
        });

    }

    private int getInverseColor(int background) {
        return Color.argb(0x80, 255 - Color.red(background),
                255 - Color.green(background), 255 - Color.blue(background));
    }

    protected void updateLineCount(boolean withDelay) {
        if (this.updateLineCount) {
            this.mHandler.removeCallbacks(this.ulcTask);
            if (withDelay) {
                this.mHandler.postDelayed(this.ulcTask, (long) LINECOUNTDELAY);
                return;
            } else {
                this.mHandler.postDelayed(this.ulcTask, 0);
                return;
            }
        }
        if (!(this.editorHorizontalLayout == null || String.valueOf(
                this.previousLineCount).length() == String.valueOf(
                this.textEditor.getLineCount()).length())) {
            this.editorHorizontalLayout.requestLayout();
        }
        this.previousLineCount = this.textEditor.getLineCount();
    }

    protected void syntaxHighlight(int start, int end, boolean change) {
        if (this.syntaxHighlighting) {
            if (this.hlStart == -1 || start < this.hlStart) {
                this.hlStart = start;
            }
            if (this.hlEnd == -1 || end > this.hlEnd) {
                this.hlEnd = end;
            }
            this.hlChange = change;
            this.mHandler.removeCallbacks(this.shTask);
            if (this.autoTextChange) {
                this.mHandler.postDelayed(this.shTask, (long) AUTODELAY);
            } else if (change) {
                this.mHandler.postDelayed(this.shTask, (long) TYPEDELAY);
            } else {
                this.mHandler.postDelayed(this.shTask, (long) SCROLLDELAY);
            }
        }
    }

    private Document getCurrentDocument() {
        return curDocument;
        // while (this.currentDocument >= this.openDocuments.size()) {
        // this.currentDocument--;
        // }
        // return (Document) this.openDocuments.get(this.currentDocument);
    }

    private void changeLineNumbers(boolean updateLineCount, int nd) {
        if (!updateLineCount || nd != this.previousMaxDigits) {
            this.updateLineCount = updateLineCount;
            LinearLayout.LayoutParams lp1 = (LinearLayout.LayoutParams) this.lineNumbers
                    .getLayoutParams();
            LinearLayout.LayoutParams lp2 = (LinearLayout.LayoutParams) this.lineDivider
                    .getLayoutParams();
            if (updateLineCount) {
                this.lineNumbers.setVisibility(View.VISIBLE);
                this.lineDivider.setVisibility(View.VISIBLE);
                String digits = "";
                for (int i = 0; i < nd; i++) {
                    digits = new StringBuilder(String.valueOf(digits)).append(
                            "9").toString();
                }
                this.previousMaxDigits = nd;
                lp1.width = ((int) this.textEditor.getPaint().measureText(
                        digits))
                        + ((int) TypedValue.applyDimension(1, 6.0f,
                        getResources().getDisplayMetrics()));
                lp2.width = 1;
            } else {
                this.lineNumbers.setVisibility(View.GONE);
                this.lineDivider.setVisibility(View.GONE);
                lp1.width = 0;
                lp2.width = 0;
                this.previousMaxDigits = 0;
            }
            this.lineNumbers.requestLayout();
            this.lineDivider.requestLayout();
        }
    }

    protected synchronized void updateSaveState() {
        Document document = getCurrentDocument();
        boolean docChanged = document.changed();
        // changed -> not changed, not changed -> changed
        if (lastChangedState != docChanged) {
            if (docChanged) {
                this.saveFileButton.getDrawable().setAlpha(255);
                saveFileButton.setClickable(true);
            } else {
                this.saveFileButton.getDrawable().setAlpha(80);
                saveFileButton.setClickable(false);
            }
            this.saveFileButton.invalidate();
            this.lastChangedState = docChanged;
        }
    }

    protected synchronized void updateUndoRedoState() {
        // Document document = getCurrentDocument();
        // boolean newUndoState = document.canUndo();
        // if (newUndoState != this.lastUndoState) {
        // if (VERSION.SDK_INT < 11 || getActionBar() == null ||
        // !getActionBar().isShowing()) {
        // if (document.canUndo()) {
        // this.undoButton.getDrawable().setAlpha(255);
        // } else {
        // this.undoButton.getDrawable().setAlpha(80);
        // }
        // this.undoButton.invalidate();
        // } else {
        // invalidateOptionsMenu();
        // }
        // this.lastUndoState = newUndoState;
        // }
        // boolean newRedoState = document.canRedo();
        // if (newRedoState != this.lastRedoState) {
        // if (VERSION.SDK_INT < 11 || getActionBar() == null ||
        // !getActionBar().isShowing()) {
        // if (document.canRedo()) {
        // this.redoButton.getDrawable().setAlpha(255);
        // } else {
        // this.redoButton.getDrawable().setAlpha(80);
        // }
        // this.redoButton.invalidate();
        // } else {
        // invalidateOptionsMenu();
        // }
        // this.lastRedoState = newRedoState;
        // }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        // Open the search panel
        if (id == R.id.openFindBtn) {
            openFindDialog();
        }
        // Do search
        else if (id == R.id.findBtn) {
            executeFindWrapAction(true);
        }
        // Do replace
        else if (id == R.id.replaceBtn) {
            executeReplaceAction();
        }
        // Save the document
        else if (id == R.id.saveBtn) {
            executeSaveAction();
        }
        // Editor setting
        else if (id == R.id.configBtn) {
            // Intent intent = new Intent(this, SettingEditorActivity.class);
            // startActivity(intent);
        }
    }

    // Save the document
    private void executeSaveAction() {
        ProcessingDialog dlg = new ProcessingDialog(this,
                new ProcessingDialog.ProcessingInterface() {
                    @Override
                    public void process() throws Exception {
                        curDocument.save(EditorActivity.this, false);
                        curDocument.setChanged(false);
                        if (realFilePath != null) {
                            copyBack2RealPath();
                        }
                        // Notify modified
                        setResult(1);
                    }

                    @Override
                    public void afterProcess() {
                        EditorActivity.this.updateSaveState();
                        EditorActivity.this.modifySaved = true;
                        setResult();
                    }

                }, resId_fileSaved);
        dlg.show();
    }

    // Work in ROOT mode, copy back to real path when saved
    protected void copyBack2RealPath() {
        RefInvoke.invokeStaticMethod(
                "com.gmail.heagoo.appdm.util.FileCopyUtil", "copyBack",
                new Class<?>[]{Context.class, String.class, String.class, boolean.class},
                new Object[]{this, filePath, realFilePath, isRootMode});
    }

    // Make parent activity aware the modification
    protected void setResult() {
        // Set modified flag as the result
        Intent intent = new Intent();
        // if (filePath.endsWith(".xml")) {
        intent.putExtra("xmlPath", filePath);
        // }
        EditorActivity.this.setResult(1, intent);
    }

    // Open the search panel
    private void openFindDialog() {
        boolean hideDocumentList;
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        if (VERSION.SDK_INT < 11
                || !prefs.getBoolean("hideDocumentList", false)) {
            hideDocumentList = false;
        } else {
            hideDocumentList = true;
        }

        // The search panel is not opened yet
        if (this.documentListDrawer.getVisibility() == View.GONE) {
            if (!this.wasOpenedDrawer) {
                this.documentListDrawer.animateOpen();
            }
            this.documentListDrawer.setVisibility(View.VISIBLE);
            // if (!prefs.getBoolean("find_tip_read", false)) {
            // new MessageBarController(findViewById(R.id.undobar), new
            // MessageListener() {
            // public void messageRead() {
            // prefs.edit().putBoolean("find_tip_read", true).commit();
            // }
            // }).showUndoBar(false, getString(R.string.find_dialog_tip));
            // }
        } else {
            if (!this.wasOpenedDrawer) {
                this.documentListDrawer.close();
            }
            this.documentListDrawer.setVisibility(View.GONE);

            this.textEditor.requestFocus();
        }
        int start = Math.min(this.textEditor.getSelectionStart(),
                this.textEditor.getSelectionEnd());
        int end = Math.max(this.textEditor.getSelectionStart(),
                this.textEditor.getSelectionEnd());
        if (start < end) {
            String selection = this.textEditor.getText()
                    .subSequence(start, end).toString();
            if (!selection.contains("\n")) {
                this.findText.setText(selection);
            }
        }
        this.findText.requestFocus();
    }

    private boolean executeFindWrapAction(boolean toast) {
        if (this.findText.getText().toString().equals("")) {
            return false;
        }
        if (executeFindAction()) {
            this.textEditor.requestFocus();
            return true;
        } else {
            int selStart = this.textEditor.getSelectionStart();
            int selEnd = this.textEditor.getSelectionEnd();
            this.textEditor.setSelection(0);
            if (executeFindAction()) {
                this.textEditor.requestFocus();
                return true;
            }
            this.textEditor.setSelection(selStart, selEnd);
            if (!toast) {
                return false;
            }
            Toast.makeText(getApplicationContext(), getString(resId_notFound),
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean executeFindAction() {
        int flags = Pattern.MULTILINE;
        if (this.toggleIgnoreCase.isChecked()) {
            flags = Pattern.MULTILINE | Pattern.CASE_INSENSITIVE;
        }
        if (!(this.toggleRegularExpression == null || this.toggleRegularExpression
                .isChecked())) {
            flags |= Pattern.LITERAL;
        }
        try {
            Matcher matcher = Pattern.compile(
                    this.findText.getText().toString(), flags).matcher(
                    getCurrentDocument().getText().toString());
            if (matcher.find(this.textEditor.getSelectionEnd())) {
                this.textEditor.setSelection(matcher.start(), matcher.end());
                return true;
            }
            this.textEditor.setSelection(this.textEditor.getSelectionEnd());
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void adjustOpenedDrawer() {
        android.widget.FrameLayout.LayoutParams lp1 = (android.widget.FrameLayout.LayoutParams) this.editorLayout
                .getLayoutParams();
        int width = this.documentListDrawer.getWidth();
        lp1.setMargins(0, 0, width, lp1.bottomMargin);
        this.editorLayout.requestLayout();
        // try {
        // if (this.extraKeys && this.keyboard.getParent() ==
        // this.keyboardLayout) {
        // this.keyboardLayout.removeView(this.keyboard);
        // }
        // } catch (Exception e) {
        // }
        if (this.textWrap) {
            adjustWrappedTextEditor(true);
            updateLineCount(false);
        }
        this.documentListButton.setImageResource(R.drawable.edit_slide_right);
    }

    private void adjustClosedDrawer() {
        // boolean hideDocumentList = true;
        // SharedPreferences prefs =
        // PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        // if (!(VERSION.SDK_INT >= 11 && prefs.getBoolean("actionBar", true) &&
        // prefs.getBoolean("hideDocumentList", true))) {
        // hideDocumentList = false;
        // }
        // if (hideDocumentList) {
        // this.documentListDrawer.setVisibility(8);
        // }
        // int width = this.documentListButton.getWidth();
        // if (hideDocumentList) {
        // width = 0;
        // }
        this.documentListDrawer.setVisibility(View.GONE);
        android.widget.FrameLayout.LayoutParams lp1 = (android.widget.FrameLayout.LayoutParams) this.editorLayout
                .getLayoutParams();
        lp1.setMargins(0, 0, 0, lp1.bottomMargin);
        this.editorLayout.requestLayout();
        if (this.textWrap) {
            adjustWrappedTextEditor(false);
            updateLineCount(false);
        }
        this.documentListButton.setImageResource(R.drawable.edit_slide_left);
        this.docFindAnim.setDisplayedChild(0);
        // try {
        // if (this.extraKeys && this.keyboard.getParent() == null) {
        // this.keyboardLayout.addView(this.keyboard);
        // }
        // } catch (Exception e) {
        // }
    }

    private void updateReplaceState() {
        try {
            int start = Math.min(this.textEditor.getSelectionStart(),
                    this.textEditor.getSelectionEnd());
            int end = Math.max(this.textEditor.getSelectionStart(),
                    this.textEditor.getSelectionEnd());
            if (start != end) {
                Pattern pattern;
                String selected = this.textEditor.getText()
                        .subSequence(start, end).toString();
                String toFind = this.findText.getText().toString();
                if (this.toggleIgnoreCase.isChecked()) {
                    pattern = Pattern.compile(toFind,
                            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
                } else {
                    pattern = Pattern.compile(toFind);
                }
                if (pattern.matcher(selected).matches()) {
                    this.replaceButton.setEnabled(true);
                    return;
                }
            }
        } catch (Exception e) {
        }
        this.replaceButton.setEnabled(false);
    }

    private void executeReplaceAction() {
        this.textEditor.getEditableText().replace(
                this.textEditor.getSelectionStart(),
                this.textEditor.getSelectionEnd(), this.replaceText.getText());
        executeFindWrapAction(true);
    }

    // Change the font size
    private void changeFontSize(int fontSize) {
        if (fontSize > 40) {
            fontSize = 40;
        }
        if (fontSize < 4) {
            fontSize = 4;
        }
        this.textEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                (float) fontSize);
        this.lineNumbers.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                (float) fontSize);
        this.previousMaxDigits = 0;
    }

    private void adjustWrappedTextEditor(boolean openedDrawer) {
        if (openedDrawer) {
            int editorLayoutWidth = this.editorInnerLayout.getWidth();
            int lineNumberWidth = this.lineNumbers.getWidth();
            int dividerWidth = this.lineDivider.getWidth();
            int docListBtnWidth = this.documentListButton.getWidth();
            this.textEditor.setMaxWidth(editorLayoutWidth - lineNumberWidth
                    - dividerWidth - docListBtnWidth);
        } else {
            this.textEditor
                    .setMaxWidth(((this.editorInnerLayout.getWidth() - this.lineNumbers
                            .getWidth()) - this.lineDivider.getWidth())
                            - this.documentListDrawer.getWidth());
        }
    }

    protected void updateEditor(boolean setSelection) {
        // Log.d("DEBUG", "updateEditor called");
        final Document document = getCurrentDocument();
        // updateSpellCheckerState();
        this.autoTextChange = true;
        this.textEditor.setText(document.getText());
        this.autoTextChange = false;
        if (setSelection) {
            int ss = document.getSelectionStart();
            int se = document.getSelectionEnd();
            Handler handler = new Handler();
            this.textEditor.setSelectionNoHack(ss, se);
            handler.postDelayed(new Runnable() {
                public void run() {
                    EditorActivity.this.scrollView.scrollTo(
                            document.getScrollPositionX(),
                            document.getScrollPositionY());
                }
            }, 400);
        }
        updateLineCount(true);
        updateUndoRedoState();
        updateSaveState();
        // updateFooter();
        // this.documentList.setItemChecked(this.currentDocument, true);
        // if (VERSION.SDK_INT < 11 || getActionBar() == null) {
        // updateTabs();
        // } else if (getActionBar().getNavigationMode() == 1) {
        // getActionBar().setSelectedNavigationItem(this.currentDocument);
        // this.navigationBarAdapter.notifyDataSetChanged();
        // }
    }

    class UpdateLineCount extends TimerTask {
        UpdateLineCount() {
        }

        public void run() {
            try {
                if (EditorActivity.this.textWrap) {
                    updateWrap();
                } else {
                    updateNoWrap();
                }
            } catch (Exception e) {
            }
            EditorActivity.this.previousLineCount = EditorActivity.this.textEditor
                    .getLineCount();
        }

        private void updateWrap() {
            String[] lines = new StringBuilder(
                    String.valueOf(EditorActivity.this.textEditor.getText()
                            .toString())).append("\nEND").toString()
                    .split("\n");
            EditorActivity.this.changeLineNumbers(
                    EditorActivity.this.updateLineCount,
                    getNumberDigits(lines.length));
            int offset = 0;
            StringBuilder sb = new StringBuilder();
            int expected = 0;
            int i = 0;
            while (i < lines.length - 1) {
                int expected2;
                while (true) {
                    expected2 = expected + 1;
                    if (expected >= EditorActivity.this.textEditor
                            .getLineNumber(offset)) {
                        break;
                    }
                    sb.append('\n');
                    expected = expected2;
                }
                sb.append(i + 1);
                sb.append('\n');
                offset += lines[i].length() + 1;
                i++;
                expected = expected2;
            }
            EditorActivity.this.lineNumbers.setText(sb.toString());
        }

        protected int getNumberDigits(int lines) {
            int nd = 1;
            while (lines >= 10) {
                lines /= 10;
                nd++;
            }
            return Math.max(2, nd);
        }

        private void updateNoWrap() {
            int lineCount = EditorActivity.this.textEditor.getLineCount();
            if (lineCount == 0) {
                lineCount = 1;
            }

            EditorActivity.this.changeLineNumbers(
                    EditorActivity.this.updateLineCount,
                    getNumberDigits(lineCount));

            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= lineCount; i++) {
                sb.append(i);
                sb.append('\n');
            }

            EditorActivity.this.lineNumbers.setText(sb.toString());
            EditorActivity.this.lineNumbers.requestLayout();
        }
    }

    class SyntaxHighLight extends TimerTask {
        SyntaxHighLight() {
        }

        public void run() {
            final Document document = EditorActivity.this.getCurrentDocument();
            final Rect r = new Rect();
            if (EditorActivity.this.textEditor.getLocalVisibleRect(r)) {
                doHighlighting(document, r);
            } else {
                highlightingOnLayout(document, r);
            }
            EditorActivity.this.hlStart = -1;
            EditorActivity.this.hlEnd = -1;
            EditorActivity.this.hlChange = false;
        }

        private void highlightingOnLayout(final Document document, final Rect r) {
            document.syntaxHighlight(EditorActivity.this.textEditor,
                    EditorActivity.this.hlStart, EditorActivity.this.hlEnd, -1,
                    -1, EditorActivity.this.hlChange,
                    EditorActivity.this.getApplicationContext());
            EditorActivity.this.textEditor.getViewTreeObserver()
                    .addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                        @SuppressWarnings("deprecation")
                        public void onGlobalLayout() {
                            EditorActivity.this.textEditor
                                    .getViewTreeObserver()
                                    .removeGlobalOnLayoutListener(this);
                            if (EditorActivity.this.textEditor
                                    .getLocalVisibleRect(r)) {
                                doHighlighting(document, r);
                            }
                        }
                    });
        }

        private void doHighlighting(Document document, Rect r) {
            try {
                int lstart = r.top
                        / EditorActivity.this.textEditor.getLineHeight();
                int lend = (r.top + EditorActivity.this.getWindowManager()
                        .getDefaultDisplay().getHeight())
                        / EditorActivity.this.textEditor.getLineHeight();
                if (EditorActivity.this.textWrap) {
                    int rlstart = 0;
                    int rlend = 0;
                    String[] lines = EditorActivity.this.textEditor.getText()
                            .toString().split("\\n");
                    int offset = 0;
                    for (int i = 0; i < lines.length; i++) {
                        int line = EditorActivity.this.textEditor
                                .getLineNumber(offset);
                        offset += lines[i].length() + 1;
                        if (line <= lstart) {
                            rlstart = i;
                        }
                        if (line <= lend) {
                            rlend = i;
                        }
                    }
                    lstart = rlstart;
                    lend = rlend;
                }
                lstart -= EditorActivity.this.highlightSize;
                lend += EditorActivity.this.highlightSize;
                document.syntaxHighlight(EditorActivity.this.textEditor,
                        /* TestActivity.this.currentTheme, */
                        EditorActivity.this.hlStart, EditorActivity.this.hlEnd, lstart,
                        lend, EditorActivity.this.hlChange,
                        EditorActivity.this.getApplicationContext());
            } catch (Exception e) {
            }
        }
    }
}
