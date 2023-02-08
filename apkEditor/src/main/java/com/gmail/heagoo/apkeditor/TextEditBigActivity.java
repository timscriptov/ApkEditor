package com.gmail.heagoo.apkeditor;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SlidingDrawer;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewAnimator;

import com.gmail.heagoo.SelectionChangedListener;
import com.gmail.heagoo.apkeditor.ProcessingDialog.ProcessingInterface;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.editor.HtmlViewDialog;
import com.gmail.heagoo.apkeditor.ui.LayoutObListView;
import com.gmail.heagoo.common.ClipboardUtil;
import com.gmail.heagoo.common.Display;
import com.gmail.heagoo.common.FileUtil;
import com.gmail.heagoo.common.ICommonCallback;
import com.gmail.heagoo.common.PathUtil;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.neweditor.ColorTheme;
import com.gmail.heagoo.neweditor.Document;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TextEditBigActivity extends TextEditBase
        implements OnClickListener,
        TextWatcher, SelectionChangedListener {

    private int fontSize;

    private FrameLayout editorOuterLayout;
    private ViewAnimator docFindAnim;
    private SlidingDrawer documentListDrawer;
    private EditText findText;
    private EditText replaceText;
    private ImageButton findButton;
    private ImageButton replaceButton;
    private ImageButton replaceAllButton;
    private ImageButton goButton;
    private EditText lineNumEdit;
    private ImageView openFindButton;
    private ImageView saveFileButton;
    private ImageView copyButton;
    private ImageView pasteButton;
    private ImageView moreButton;
    private ToggleButton toggleIgnoreCase;
    private ToggleButton toggleRegularExpression;
    //    private LinearLayout editorLayout;
    private ImageView documentListButton; // Slide image

    private boolean wasOpenedDrawer = false; // search panel opened
    //private boolean lastRedoState = true;
    //private boolean lastSaveState = true; // Latest changed saved or not
    private boolean saveBtnEnabled = false; // Save btn is enabled or not
    // changed state
    private boolean lastUndoState = true;
    // On the html view or not
    //private boolean onHtmlPage = false;

    // ///////////////////////////////// Not directly for editor
    // ////////////////////////////////////////
    private TextView filenameTv;
    //private File htmlFile;
    private LinearLayout editorView;
    private HtmlViewDialog htmlDialog;
    //private WebView webView;
    //private LinearLayout htmlLayout;
    private View previousMenu;
    private View nextMenu;
    private View methodMenu;


    private int curStartLine; // start line in current file

    // Text loaded or not, set by TextLoader
    private boolean textLoaded = false;

    // Text list view
    private TextEditBigListHelper textEditor;

    // Record selected string in text editor
    private String strSelected = "";

    public TextEditBigActivity() {
        super(true, false);
    }

    public static boolean isValuesXml(String filePath) {
        String[] folders = filePath.split("/");
        if (folders.length > 2) {
            String folder = folders[folders.length - 2];
            if (folder.startsWith("values")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void debug(String format, Object... args) {
        // String message = String.format(format, args);
        // Log.d("DEBUG", String.format("%f: %s",
        // System.currentTimeMillis() / 1000.0, message));
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        curDocument.setChanged(true);
        TextEditBigActivity.this.updateSaveState();
    }

    @Override
    public void textSelected(int lineIndex, int start, int end, String strSelected) {
        this.strSelected = strSelected;

        updateCopyState(start, end);
        if (documentListDrawer.isOpened()) {
            updateReplaceState();
        }
    }

    private void updateLineCount() {
        if (curStartLine > 0) {
            textEditor.selectItem(curStartLine - 1);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initMainView();

        initEditView();

        setupOnclickListener();

        new TextLoader().execute();
    }

    @Override
    protected void specialCharClicked(int idx) {
        if (textEditor == null) {
            return;
        }
        Editable editable = textEditor.getEditableText();
        if (editable != null) {
            String text = "" + getSpecialChars(null).charAt(idx);
            editable.replace(
                    textEditor.getSelectionStart(), textEditor.getSelectionEnd(), text);
            textEditor.requestFocus();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("curFileIndex", curFileIndex);
        outState.putBoolean("modifySaved", modifySaved);
        if (curDocument != null && curDocument.changed()) {
            outState.putBoolean("docChanged", true);
            String unsavedFilePath;
            try {
                unsavedFilePath = SDCard.makeWorkingDir(this) +
                        PathUtil.getNameFromPath(getCurrentFilePath()) + ".tmp";
            } catch (Exception ignored) {
                unsavedFilePath = getCurrentFilePath() + ".tmp";
            }
            try {
                curDocument.save(unsavedFilePath, this);
                outState.putString("unsavedFilePath", unsavedFilePath);
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        debug("enter onResume");

        // Log.d("DEBUG", "onResume() called.");

        // To check font size changed or not
        int fontSize = SettingEditorActivity.getFontSize(this);
        if (this.fontSize != fontSize) {
            this.fontSize = fontSize;
            changeFontSize(fontSize);
            updateSaveState();
        }

        // Show line number or not
        showHideLineNumbers();
    }

    // Show or hide line numbers
    private void showHideLineNumbers() {
        if (SettingEditorActivity.showLineNumbers(this)) {
            textEditor.showLineNumber(true);
        } else {
            textEditor.showLineNumber(false);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    private void initMainView() {
        // File name title
        this.filenameTv = (TextView) this.findViewById(R.id.filename);
        this.previousMenu = this.findViewById(R.id.menu_previous);
        this.nextMenu = this.findViewById(R.id.menu_next);
        this.methodMenu = this.findViewById(R.id.menu_methods);
        this.previousMenu.setOnClickListener(this);
        this.nextMenu.setOnClickListener(this);
        this.methodMenu.setOnClickListener(this);
    }

    @SuppressWarnings("deprecation")
    private void initEditView() {
        this.editorOuterLayout = (FrameLayout) findViewById(R.id.center_layout);
        this.textEditor = new TextEditBigListHelper(this,
                (LayoutObListView) findViewById(R.id.text_list));
        this.docFindAnim = (ViewAnimator) findViewById(R.id.searchAnimator);
        this.documentListDrawer = (SlidingDrawer) findViewById(R.id.sliding_drawer);
        this.findText = (EditText) findViewById(R.id.findEdit);
        this.replaceText = (EditText) findViewById(R.id.replaceEdit);
        this.findButton = (ImageButton) findViewById(R.id.findBtn);
        this.replaceButton = (ImageButton) findViewById(R.id.replaceBtn);
        this.replaceAllButton = (ImageButton) findViewById(R.id.replaceAllBtn);
        this.openFindButton = (ImageView) findViewById(R.id.openFindBtn);
        this.toggleIgnoreCase = (ToggleButton) findViewById(R.id.checkBoxIgnoreCase);
        this.toggleRegularExpression = (ToggleButton) findViewById(R.id.checkBoxRegexp);
        this.saveFileButton = (ImageView) findViewById(R.id.saveBtn);
        this.copyButton = (ImageView) findViewById(R.id.copyBtn);
        this.pasteButton = (ImageView) findViewById(R.id.pasteBtn);
        this.moreButton = (ImageView) findViewById(R.id.moreBtn);
        this.goButton = (ImageButton) findViewById(R.id.goBtn);
        this.lineNumEdit = (EditText) findViewById(R.id.lineNumEdit);
        this.documentListButton = (ImageView) findViewById(R.id.panel_button);

        // Copy button is initially disabled
        enableButton(copyButton, false);

        // No suggestions:721041
        //this.textEditor.setInputType(721041);
        this.textEditor.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                InputType.TYPE_CLASS_TEXT);
        // font size
        this.fontSize = SettingEditorActivity.getFontSize(this);
        changeFontSize(fontSize);

        // On click listener
        this.openFindButton.setOnClickListener(this);
        this.findButton.setOnClickListener(this);
        this.replaceButton.setOnClickListener(this);
        this.replaceAllButton.setOnClickListener(this);
        this.saveFileButton.setOnClickListener(this);
        this.goButton.setOnClickListener(this);
        this.copyButton.setOnClickListener(this);
        this.pasteButton.setOnClickListener(this);
        this.moreButton.setOnClickListener(this);

        // At the beginning, disable the save & replace button
        this.saveFileButton.getDrawable().setAlpha(80);
        this.saveFileButton.setClickable(false);
        this.replaceButton.setEnabled(false);
        this.replaceAllButton.setEnabled(false);

        ColorTheme colorTheme = new ColorTheme(this);
        this.editorOuterLayout.setBackgroundColor(colorTheme.getBackgroundColor());
        this.textEditor.setBackgroundColor(colorTheme.getBackgroundColor());
        this.textEditor.setTextColor(colorTheme.getForeground());
        this.textEditor.setBracketSpanColor(getInverseColor(colorTheme.getBackgroundColor()));

        this.textEditor.setOnTextChangeListener(this);
    }

    @SuppressWarnings("deprecation")
    private void setupOnclickListener() {
        this.documentListDrawer
                .setOnDrawerOpenListener(new OnDrawerOpenListener() {
                    public void onDrawerOpened() {
                        TextEditBigActivity.this.adjustOpenedDrawer();
                    }
                });
        this.documentListDrawer
                .setOnDrawerCloseListener(new OnDrawerCloseListener() {
                    public void onDrawerClosed() {
                        TextEditBigActivity.this.adjustClosedDrawer();
                    }
                });

        // Selection change listener
        this.textEditor.setTextSelectionListener(this);

        // Find and replace text key listener
        this.findText.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP
                        && keyCode == KeyEvent.KEYCODE_ENTER) {
                    TextEditBigActivity.this.executeFindWrapAction(true);
                    return true;
                } else if (TextEditBigActivity.this.documentListDrawer
                        .getVisibility() == View.VISIBLE
                        && event.getAction() == KeyEvent.ACTION_UP
                        && keyCode == KeyEvent.KEYCODE_BACK) {
                    TextEditBigActivity.this.documentListDrawer.close();
                    return true;
                } else {
                    TextEditBigActivity.this.updateReplaceState();
                    return false;
                }
            }
        });
        this.replaceText.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (TextEditBigActivity.this.documentListDrawer
                        .getVisibility() == View.VISIBLE
                        && event.getAction() == KeyEvent.ACTION_UP
                        && keyCode == KeyEvent.KEYCODE_BACK) {
                    TextEditBigActivity.this.documentListDrawer.close();
                    return true;
                }
                TextEditBigActivity.this.updateReplaceState();
                return false;
            }
        });

        super.setupInputMethodMonitor((LayoutObListView) findViewById(R.id.text_list));
    }

    private int getInverseColor(int background) {
        return Color.argb(0x80, 255 - Color.red(background),
                255 - Color.green(background), 255 - Color.blue(background));
    }

    private void enableButton(ImageView button, boolean enable) {
        if (enable) {
            button.getDrawable().setAlpha(255);
            button.setClickable(true);
        } else {
            button.getDrawable().setAlpha(80);
            button.setClickable(false);
        }
    }

    protected synchronized void updateSaveState() {
        Document document = curDocument;
        if (document == null) {
            return;
        }
        boolean docChanged = document.changed();
        // changed -> not changed, not changed -> changed
        if (saveBtnEnabled != docChanged) {
            if (docChanged) {
                enableButton(saveFileButton, true);
            } else {
                enableButton(saveFileButton, false);
            }
            this.saveFileButton.invalidate();
            this.saveBtnEnabled = docChanged;
        }
    }

    // Update the state of the copy button
    private void updateCopyState(int selStart, int selEnd) {
        if (selStart == selEnd) {
            enableButton(copyButton, false);
        } else {
            enableButton(copyButton, true);
        }
    }

    // Check the file whether modified before reloading
    // To load another text file
    private void doReloadWithCheck(final int fileIdxOffset) {
        Document document = curDocument;
        boolean docChanged = document.changed();

        // If doc is not changed, direct load it
        if (!docChanged) {
            curFileIndex += fileIdxOffset;
            new TextLoader().execute();
            return;
        }

        new AlertDialog.Builder(this).setMessage(R.string.save_changes_tip)
                .setPositiveButton(R.string.save,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                executeSaveAction(new ICommonCallback() {
                                    @Override
                                    public void doCallback() {
                                        curFileIndex += fileIdxOffset;
                                        new TextLoader().execute();
                                    }
                                });
                            }
                        })
                .setNegativeButton(R.string.donot_save,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                curFileIndex += fileIdxOffset;
                                new TextLoader().execute();
                            }
                        })
                .setNeutralButton(android.R.string.cancel, null).show();
    }

    private void doCopySelectedText() {
        String selected = strSelected;
        if (selected != null && !selected.equals("")) {
            ClipboardUtil.copyToClipboard(this, selected);
            if (selected.contains("\n")) {
                Toast.makeText(this, R.string.selected_str_copied, Toast.LENGTH_SHORT).show();
            } else {
                String message = String.format(getString(R.string.copied_to_clipboard), selected);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void doPaste() {
        String text = ClipboardUtil.getText(this);
        if (text == null) {
            Toast.makeText(this, R.string.clipboard_no_text, Toast.LENGTH_SHORT).show();
            return;
        }
        Editable editable = textEditor.getEditableText();
        if (editable != null) {
            editable.replace(
                    textEditor.getSelectionStart(), textEditor.getSelectionEnd(), text);
            textEditor.requestFocus();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        // To edit next file
        if (id == R.id.menu_next) {
            doReloadWithCheck(1);
        }
        // To edit previous file
        else if (id == R.id.menu_previous) {
            doReloadWithCheck(-1);
        }
        // Pop up more options
        else if (id == R.id.moreBtn) {
            showMoreOptions(v);
        }

        // Open the search panel
        else if (id == R.id.openFindBtn) {
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
        // Replace All
        else if (id == R.id.replaceAllBtn) {
            executeReplaceAll();
        }
        // Go to the line number, or just type enter key
        else if (id == R.id.goBtn) {
            gotoButtonClicked();
        }
        // Save the document
        else if (id == R.id.saveBtn) {
            executeSaveAction(null);
        }
        // Copy selected string to clipboard
        else if (id == R.id.copyBtn) {
            doCopySelectedText();
        }
        // Paste something
        else if (id == R.id.pasteBtn) {
            doPaste();
        }
        // Smali methods
        else if (id == R.id.menu_methods) {
            showPopWindow(v);
        }
    }

    // Replace all the occurrence
    private void executeReplaceAll() {
        int flags = Pattern.MULTILINE;
        if (this.toggleIgnoreCase.isChecked()) {
            flags = Pattern.MULTILINE | Pattern.CASE_INSENSITIVE;
        }
        if (!(this.toggleRegularExpression == null
                || this.toggleRegularExpression.isChecked())) {
            flags |= Pattern.LITERAL;
        }
        try {
            int totalReplaces = 0;
            String replacing = this.replaceText.getText().toString();
            List<String> lines = textEditor.getTextLines();
            Pattern pattern = Pattern
                    .compile(this.findText.getText().toString(), flags);

            // Store all the occurrence
            List<Location> occurences = new ArrayList<>();

            for (int i = 0; i < lines.size(); ++i) {
                String lineText = lines.get(i);
                Matcher matcher = pattern.matcher(lineText);

                // Get all occurrences for this line
                int startIdx = 0;
                while (matcher.find(startIdx)) {
                    int start = matcher.start();
                    int end = matcher.end();
                    occurences.add(new Location(start, end));
                    startIdx = end;
                }

                // Do the replacement
                if (!occurences.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    int lastPos = 0;
                    for (Location loc : occurences) {
                        sb.append(lineText.substring(lastPos, loc.start));
                        sb.append(replacing);
                        lastPos = loc.end;
                    }
                    sb.append(lineText.substring(lastPos));
                    lines.set(i, sb.toString());

                    // Update total replaces
                    totalReplaces += occurences.size();

                    occurences.clear();
                }
            }

            // Update the text list
            textEditor.refresh();

            // Show replace result tip
            if (totalReplaces > 0) {
                Toast.makeText(getApplicationContext(),
                        String.format(getString(R.string.replace_all_ret), totalReplaces),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.not_found), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ignored) {
        }
    }

    // Go to specified line or input an enter key
    private void gotoButtonClicked() {
        String str = lineNumEdit.getText().toString();
        str = str.trim();
        if (str.equals("")) {
            textEditor.insert("\n");
            textEditor.requestFocus();
            return;
        }

        // Go to the line index
        int lineNO;
        try {
            lineNO = Integer.valueOf(str);
        } catch (Exception e) {
            return;
        }

        gotoLine(lineNO);
    }

    // Go to the target line (lineNO starts at 1)
    @Override
    public void gotoLine(int lineNO) {
        // Scroll to the start line
        if (lineNO > 0) {
            lineNumEdit.setText("");
            textEditor.selectItem(lineNO - 1);
            textEditor.requestFocus();
        }
    }

    // Save the document
    private void executeSaveAction(final ICommonCallback callback) {
        ProcessingDialog dlg = new ProcessingDialog(this,
                new ProcessingInterface() {
                    @Override
                    public void process() throws Exception {
                        // As the content in document is not correct, do not use it
                        //curDocument.save(TextEditBigActivity.this, false);
                        FileUtil.writeToFile(getCurrentFilePath(), textEditor.getTextLines());
                        curDocument.setChanged(false);
                    }

                    @Override
                    public void afterProcess() {
                        TextEditBigActivity.this.updateSaveState();
                        TextEditBigActivity.this.modifySaved = true;
                        setResult();
                        if (callback != null) {
                            callback.doCallback();
                        }
                    }

                }, R.string.file_saved);
        dlg.show();
    }

    // Open the search panel
    private void openFindDialog() {

        // The search panel is not opened yet
        if (this.documentListDrawer.getVisibility() == View.GONE) {
            if (!this.wasOpenedDrawer) {
                this.documentListDrawer.animateOpen();
            }
            this.documentListDrawer.setVisibility(View.VISIBLE);
        } else {
            if (!this.wasOpenedDrawer) {
                this.documentListDrawer.close();
            }
            this.documentListDrawer.setVisibility(View.GONE);

            this.textEditor.requestFocus();
        }
        String selectedStr = strSelected;
        if (selectedStr != null && !"".equals(selectedStr)) {
            if (!selectedStr.contains("\n")) {
                this.findText.setText(selectedStr);
            }
        }
        // Set as user searched keyword
        else if (searchString != null && "".equals(this.findText.getText().toString())) {
            this.findText.setText(searchString);
        }
        this.findText.requestFocus();
    }

    private boolean executeFindWrapAction(boolean toast) {
        if (this.findText.getText().toString().equals("")) {
            return false;
        }

        int lineIndex = textEditor.getSelectionItemIndex();
        int cursorPos = textEditor.getSelectionEnd();
        if (executeFindAction(lineIndex, cursorPos)) {
            this.textEditor.requestFocus();
            return true;
        } else {
            // Try to find from the beginning
            if (executeFindAction(0, 0)) {
                this.textEditor.requestFocus();
                return true;
            }
            if (!toast) {
                return false;
            }
            Toast.makeText(getApplicationContext(),
                    getString(R.string.not_found), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    // fromLine: from which to do the search
    // fromPosition: position inside the line
    private boolean executeFindAction(int fromLine, int fromPosition) {
        if (fromLine < 0) {
            fromLine = 0;
        }

        int flags = Pattern.MULTILINE;
        if (this.toggleIgnoreCase.isChecked()) {
            flags = Pattern.MULTILINE | Pattern.CASE_INSENSITIVE;
        }
        if (!(this.toggleRegularExpression == null
                || this.toggleRegularExpression.isChecked())) {
            flags |= Pattern.LITERAL;
        }
        try {
            List<String> allLines = textEditor.getTextLines();
            String curLine = allLines.get(fromLine);

            // Check current line match or not
            Pattern pattern = Pattern.compile(this.findText.getText().toString(), flags);
            Matcher matcher = pattern.matcher(curLine);
            if (matcher.find(fromPosition)) {
                setSelection(fromLine, matcher.start(), matcher.end());
                return true;
            }

            // Check following lines match or not
            while (++fromLine < allLines.size()) {
                matcher = pattern.matcher(allLines.get(fromLine));
                if (matcher.find()) {
                    setSelection(fromLine, matcher.start(), matcher.end());
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void setSelection(int lineIndex, int start, int end) {
        this.textEditor.setSelection(lineIndex, start, end);
    }

    private void adjustOpenedDrawer() {
        LinearLayout.LayoutParams lp1 = (LinearLayout.LayoutParams) this.textEditor
                .getLayoutParams();
        int width = this.documentListDrawer.getWidth();
        lp1.setMargins(0, 0, width, lp1.bottomMargin);
        this.textEditor.requestLayout();
        this.documentListButton.setImageResource(R.drawable.edit_slide_right);
    }

    private void adjustClosedDrawer() {
        this.documentListDrawer.setVisibility(View.GONE);
        LinearLayout.LayoutParams lp1 = (LinearLayout.LayoutParams)
                this.textEditor.getLayoutParams();
        lp1.setMargins(0, 0, 0, lp1.bottomMargin);
        this.textEditor.requestLayout();
        this.documentListButton.setImageResource(R.drawable.edit_slide_left);
        this.docFindAnim.setDisplayedChild(0);
    }

    private void updateReplaceState() {
        try {
            String selected = strSelected;
            if (selected != null && !"".equals(selected)) {
                Pattern pattern;
                String toFind = this.findText.getText().toString();
                if (this.toggleIgnoreCase.isChecked()) {
                    pattern = Pattern.compile(toFind,
                            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
                } else {
                    pattern = Pattern.compile(toFind);
                }
                if (pattern.matcher(selected).matches()) {
                    this.replaceButton.setEnabled(true);
                    this.replaceAllButton.setEnabled(true);
                    return;
                }
            }
        } catch (Exception ignored) {
        }
        this.replaceButton.setEnabled(false);
        this.replaceAllButton.setEnabled(false);
    }

    private void executeReplaceAction() {
        this.textEditor.replaceSelection(
                this.replaceText.getText().toString());
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
        this.textEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) fontSize);
    }

    @Override
    public void onBackPressed() {
        Document document = curDocument;
        boolean docChanged = document.changed();

        // If doc is not changed, direct finish it
        if (!docChanged) {
            this.finish();
            return;
        }

        new AlertDialog.Builder(this).setMessage(R.string.save_changes_tip)
                .setPositiveButton(R.string.save,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                executeSaveAction(new ICommonCallback() {
                                    @Override
                                    public void doCallback() {
                                        TextEditBigActivity.this.finish();
                                    }
                                });
                            }
                        })
                .setNegativeButton(R.string.donot_save,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                TextEditBigActivity.this.finish();
                            }
                        })
                .setNeutralButton(android.R.string.cancel, null).show();
    }

    @Override
    public void insertString(String str) {
        int index = this.textEditor.getSelectionStart();
        Editable edit = this.textEditor.getEditableText();
        if (edit == null) {
            return;
        }

        if (index < 0 || index >= edit.length()) {
            edit.append(str);
        } else {
            edit.insert(index, str);
        }
    }
//
//    private void adjustWrappedTextEditor(boolean openedDrawer) {
//        int width;
//        if (openedDrawer) {
//            int editorLayoutWidth = this.editorOuterLayout.getWidth();
//            int drawerWidth = this.documentListDrawer.getWidth();
//            width = editorLayoutWidth - drawerWidth;
//            this.textEditor.setMaxWidth(width);
//        } else {
//            width = this.editorOuterLayout.getWidth();
//            this.textEditor.setMaxWidth(width);
//        }
//    }

    @Override
    public void operateLines(int from, int to) {
        boolean bModified = false;

        // Delete lines
        if (linesOP == MoreEditorOptionAdapter.CMD_DELETE_LINES) {
            String[] lines = textEditor.getText().toString().split("\n");
            if (from <= 0) {
                from = 1;
            }
            int end = (to <= lines.length ? to : lines.length);
            if (from <= end) {
                List<String> newLines = new ArrayList<>();
                for (int i = 1; i < from; i++) {
                    newLines.add(lines[i - 1]);
                }
                for (int i = end + 1; i <= lines.length; i++) {
                    newLines.add(lines[i - 1]);
                }
                textEditor.setText(newLines);
                bModified = true;

                // Show tip
                int deleted = end - from + 1;
                String fmt = getString(R.string.n_lines_deleted);
                Toast.makeText(this, String.format(fmt, deleted), Toast.LENGTH_LONG).show();
            }
        }
        // Comment lines
        else if (linesOP == MoreEditorOptionAdapter.CMD_COMMENT_LINES) {
            String[] lines = textEditor.getText().toString().split("\n");
            if (from <= 0) {
                from = 1;
            }
            int end = (to <= lines.length ? to : lines.length);
            if (from <= end) {
                List<String> newLines = new ArrayList<>();
                // Before comment
                for (int i = 1; i < from; i++) {
                    newLines.add(lines[i - 1]);
                }
                // Already commented
                boolean isUnComment = allStartsWith(lines, from, end, "#");
                if (isUnComment) {
                    for (int i = from; i <= end; i++) {
                        int pos = lines[i - 1].indexOf('#');
                        if (pos > 0) {
                            newLines.add(lines[i - 1].substring(0, pos) + lines[i - 1].substring(pos + 1));
                        } else {
                            newLines.add(lines[i - 1].substring(pos + 1));
                        }
                    }
                } else {
                    for (int i = from; i <= end; i++) {
                        newLines.add("#" + lines[i - 1]);
                    }
                }
                // After the comment part
                for (int i = end + 1; i <= lines.length; i++) {
                    newLines.add(lines[i - 1]);
                }
                textEditor.setText(newLines);
                bModified = true;

                // Show tip
                int modified = end - from + 1;
                String fmt;
                if (isUnComment) {
                    fmt = getString(R.string.n_lines_uncommented);
                } else {
                    fmt = getString(R.string.n_lines_commented);
                }
                Toast.makeText(this, String.format(fmt, modified), Toast.LENGTH_LONG).show();
            }
        }
        if (bModified) {
            curDocument.setChanged(true);
            updateSaveState();
        }
    }

    private static final class Location {
        public final int start;
        public final int end;

        private Location(int _start, int _end) {
            this.start = _start;
            this.end = _end;
        }
    }

    // Load text from file and show it in EditText
    private class TextLoader extends AsyncTask<Void, Integer, Boolean> {
        private String syntaxFileName;
        private boolean loadSucceed;
        private boolean htmlAvailable;

        private String curFilePath;

        @Override
        protected void onPreExecute() {
            curFilePath = fileList.get(curFileIndex);
            curStartLine = startLineList.get(curFileIndex);
            syntaxFileName = syntaxFileList.get(curFileIndex);

            // Notify super class
            setCurrentFilePath(curFilePath);

            // Update title
            filenameTv.setText(getDisplayName(curFilePath));

            // Update method menu
            methodMenu.setVisibility((isSmali(curFilePath) || isJava(curFilePath)) ? View.VISIBLE : View.GONE);

            // Update previous/next file menu
            if (curFileIndex >= fileList.size() - 1) {
                nextMenu.setVisibility(View.INVISIBLE);
            } else {
                nextMenu.setVisibility(View.VISIBLE);
            }
            if (curFileIndex == 0) {
                previousMenu.setVisibility(View.INVISIBLE);
            } else {
                previousMenu.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Document doc = new Document(TextEditBigActivity.this, new File(curFilePath),
                    syntaxFileName);
            try {
                // Consider load the file from temporary file
                if (unsavedFilePath != null) {
                    doc.load(TextEditBigActivity.this, unsavedFilePath, R.string.error_file_too_big);
                    doc.setChanged(true);
                    boolean ret = new File(unsavedFilePath).delete();
                    unsavedFilePath = null;
                } else {
                    doc.load(TextEditBigActivity.this, curFilePath, R.string.error_file_too_big);
                }

                TextEditBigActivity.this.curDocument = doc;
                loadSucceed = true;
            } catch (IOException e) {
                loadSucceed = false;
            }

            // Initialize html file
            htmlAvailable = (isXml(curFilePath) || isSmali(curFilePath));

            return null;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (loadSucceed) {
                textLoaded = true;

                textEditor.setDocument(curDocument);

                String strContent = curDocument.getText();
                textEditor.setText(strContent);

                updateLineCount();
                updateSaveState();
            } else {
                Toast.makeText(TextEditBigActivity.this, "Failed to open " + curFilePath,
                        Toast.LENGTH_LONG).show();
            }

            // Set max width for title, so that method menu can be fully shown
            filenameTv.setMaxWidth(Display.getWidth(TextEditBigActivity.this) -
                    previousMenu.getWidth() - nextMenu.getWidth() - methodMenu.getWidth() -
                    (int) (16 * getResources().getDisplayMetrics().density));
        }
    }
}
