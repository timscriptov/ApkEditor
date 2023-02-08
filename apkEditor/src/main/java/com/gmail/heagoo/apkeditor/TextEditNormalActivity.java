package com.gmail.heagoo.apkeditor;

import static java.lang.Math.abs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SlidingDrawer;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewAnimator;

import com.gmail.heagoo.apkeditor.ProcessingDialog.ProcessingInterface;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.editor.HtmlViewDialog;
import com.gmail.heagoo.common.ClipboardUtil;
import com.gmail.heagoo.common.Display;
import com.gmail.heagoo.common.ICommonCallback;
import com.gmail.heagoo.common.PathUtil;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.neweditor.ColorTheme;
import com.gmail.heagoo.neweditor.Document;
import com.gmail.heagoo.neweditor.ObEditText;
import com.gmail.heagoo.neweditor.ObScrollView;
import com.gmail.heagoo.neweditor.ScrollViewListener;
import com.gmail.heagoo.neweditor.TextSelectionListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextEditNormalActivity extends TextEditBase
        implements OnClickListener, TextSelectionListener {
    private static int AUTODELAY = 300;
    private static int LINECOUNTDELAY = 300;
    private static int TYPEDELAY = 400;
    private static int SCROLLDELAY = 100;
    public int previousLineCount;
    protected ObEditText textEditor;
    UpdateLineCount ulcTask = new UpdateLineCount();
    SyntaxHighLight shTask = new SyntaxHighLight();
    private boolean syntaxHighlighting = true;
    private boolean hlChange = false;
    private int hlEnd = -1;
    private int hlStart = -1;
    private int fontSize;
    private int highlightSize = 20;
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
    private LinearLayout editorLayout;
    private ImageView documentListButton; // Slide image
    private ScrollView scrollView; // edit scroll view
    private TextChangeListener textWatcher;
    private boolean updateLineCount = true;
    private int previousMaxDigits;

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

    // Used to dynamically change value
    private Menu optionsMenu;

    // Systax hightlight enabled or not
    private boolean enableSyntaxHighlight = true;

    //private boolean htmlInited = false;

    // Text loaded or not, set by TextLoader
    private boolean textLoaded = false;

    public TextEditNormalActivity() {
        super(false, true);
    }
    // For Debug only
    //private DebugDialog debugDialog;

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

    public int countLines(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        int lines = 1;
        int pos = 0;
        while ((pos = str.indexOf("\n", pos) + 1) != 0) {
            lines++;
        }
        return lines;
    }

    private void debug(String format, Object... args) {
        // String message = String.format(format, args);
        // Log.d("DEBUG", String.format("%f: %s",
        // System.currentTimeMillis() / 1000.0, message));
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
        if (textEditor != null) {
            String text = "" + getSpecialChars(null).charAt(idx);
            textEditor.getText().replace(
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

            updateLineCount(true);
            syntaxHighlight(-1, -1, false);
            updateSaveState();
        }

        // Show line number or not
        showHideLineNumbers();

        // Why call updateEditor??
        // new Handler().postDelayed(new Runnable() {
        // public void run() {
        // // DroidEditActivity.this.adjustMenus();
        // TextEditNormalActivity.this.updateEditor(true);
        // }
        // }, 400);
        // Replaced updateEditor with following lines, is this correct?

        debug("exit onResume");
    }

    // Show or hide line numbers
    private void showHideLineNumbers() {
        if (SettingEditorActivity.showLineNumbers(this)) {
            this.lineNumbers.setVisibility(View.VISIBLE);
            this.lineDivider.setVisibility(View.VISIBLE);
            if (textLoaded) {
                updateLineCount(false);
            }
        } else {
            this.lineNumbers.setVisibility(View.GONE);
            this.lineDivider.setVisibility(View.GONE);
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
        this.editorInnerLayout = (LinearLayout) findViewById(R.id.editorLayout);
        this.editorOuterLayout = (FrameLayout) findViewById(R.id.center_layout);
        this.lineNumbers = (EditText) findViewById(R.id.lineNumbers);
        // Remove the max characters limitation
        lineNumbers.setFilters(new InputFilter[]{});
        this.textEditor = (ObEditText) findViewById(R.id.editor);
        textEditor.setFilters(new InputFilter[]{});
        this.lineDivider = findViewById(R.id.divider);
        this.editorScrollView = (ObScrollView) findViewById(R.id.editorScrollview);
        this.editorHorizontalLayout = (HorizontalScrollView) findViewById(R.id.hScrollView);
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
        this.editorLayout = this.editorInnerLayout;
        this.documentListButton = (ImageView) findViewById(R.id.panel_button);
        this.scrollView = editorScrollView;

        // Copy button is initially disabled
        enableButton(copyButton, false);

        // Text wrap or not
        this.textEditor.setWrapped(textWrap);
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
        this.editorInnerLayout.setBackgroundColor(colorTheme.getBackgroundColor());
        this.editorOuterLayout.setBackgroundColor(colorTheme.getBackgroundColor());
        this.lineNumbers.setBackgroundColor(colorTheme.getBackgroundColor());
        this.textEditor.setBackgroundColor(colorTheme.getBackgroundColor());
        this.textEditor.setTextColor(colorTheme.getForeground());
        this.lineNumbers.setTextColor(colorTheme.getForeground());
        this.lineDivider.setBackgroundColor(colorTheme.getForeground());
        this.textEditor.setBracketSpanColor(getInverseColor(colorTheme.getBackgroundColor()));
        this.lineNumbers.setOnLongClickListener(null);
    }

    @SuppressWarnings("deprecation")
    private void setupOnclickListener() {
        this.documentListDrawer
                .setOnDrawerOpenListener(new OnDrawerOpenListener() {
                    public void onDrawerOpened() {
                        TextEditNormalActivity.this.adjustOpenedDrawer();
                    }
                });
        this.documentListDrawer
                .setOnDrawerCloseListener(new OnDrawerCloseListener() {
                    public void onDrawerClosed() {
                        TextEditNormalActivity.this.adjustClosedDrawer();
                    }
                });

        // Selection change listener
        this.textEditor.setTextSelectionListener(this);

        // Text change listener
        // Log.d("DEBUG", "will call addTextChangedListener");
        this.textWatcher = new TextChangeListener();
        this.textEditor.addTextChangedListener(textWatcher);

        // When scroll, redraw the highlighting
        this.editorScrollView.setScrollViewListener(new ScrollViewListener() {
            public void onScrollChanged(ObScrollView scrollView, int x, int y,
                                        int oldx, int oldy) {
//                if (curDocument.isBigFile()) {
//                    partialLoadDisplay();
//                }
                debug("posted scroll message to do syntax highlight");
                TextEditNormalActivity.this.syntaxHighlight(-1, -1, false);
            }
        });

        // Find and replace text key listener
        this.findText.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP
                        && keyCode == KeyEvent.KEYCODE_ENTER) {
                    TextEditNormalActivity.this.executeFindWrapAction(true);
                    return true;
                } else if (TextEditNormalActivity.this.documentListDrawer
                        .getVisibility() == View.VISIBLE
                        && event.getAction() == KeyEvent.ACTION_UP
                        && keyCode == KeyEvent.KEYCODE_BACK) {
                    TextEditNormalActivity.this.documentListDrawer.close();
                    return true;
                } else {
                    TextEditNormalActivity.this.updateReplaceState();
                    return false;
                }
            }
        });
        this.replaceText.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (TextEditNormalActivity.this.documentListDrawer
                        .getVisibility() == View.VISIBLE
                        && event.getAction() == KeyEvent.ACTION_UP
                        && keyCode == KeyEvent.KEYCODE_BACK) {
                    TextEditNormalActivity.this.documentListDrawer.close();
                    return true;
                }
                TextEditNormalActivity.this.updateReplaceState();
                return false;
            }
        });

        // When click on the selection, show keyboard (if not shown)
        this.textEditor.setOnTouchListener(new View.OnTouchListener() {
            private boolean bPossibleEvent = false;
            private float downX;
            private float downY;
            private long downTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    bPossibleEvent = false; // reset the status
                    int selectionStart = textEditor.getSelectionStart();
                    int selectionEnd = textEditor.getSelectionEnd();
                    // Some text is selected
                    if (selectionStart != selectionEnd) {
                        Layout layout = ((EditText) v).getLayout();
                        float x = event.getX() + textEditor.getScrollX();
                        float y = event.getY() + textEditor.getScrollY();
                        int line = layout.getLineForVertical((int) y);
                        int offset = layout.getOffsetForHorizontal(line, x);
                        // Click on the selection (and input method is hide), omit the event
                        if (offset >= selectionStart && offset < selectionEnd
                                && !isInputMethodShown()) {
                            bPossibleEvent = true;
                            downX = event.getX();
                            downY = event.getY();
                            downTime = System.currentTimeMillis();
                            return true;
                        }
                    }
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    // MOVE may be included in a click
                    //bPossibleEvent = false;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (bPossibleEvent) {
                        float upX = event.getX();
                        float upY = event.getY();
                        long upTime = System.currentTimeMillis();
                        if (abs(upX - downX) < 32 && abs(upY - downY) < 32 &&
                                (upTime - downTime) < 500) {
                            showInputMethod();
                            return true;
                        }
                    }
                    bPossibleEvent = false;
                }
                return false;
            }
        });

        super.setupInputMethodMonitor(textEditor);
    }

    @Override
    public void selectionChanged(int selStart, int selEnd) {
        updateCopyState(selStart, selEnd);
        if (TextEditNormalActivity.this.documentListDrawer.isOpened()) {
            debug("updateReplaceState starts");
            TextEditNormalActivity.this.updateReplaceState();
            debug("updateReplaceState ends");
        }
    }

    private int getInverseColor(int background) {
        return Color.argb(0x80, 255 - Color.red(background),
                255 - Color.green(background), 255 - Color.blue(background));
    }

    protected void updateLineCount(boolean withDelay) {
        // Scroll to the start line
        if (curStartLine > 0) {
            String[] lines = textEditor.getText().toString().split("\n");
            int charIndex = 0;
            int line = (curStartLine > lines.length) ? lines.length : curStartLine;
            for (int i = 0; i < line - 1; ++i) {
                charIndex += lines[i].length() + 1;
            }
            textEditor.setSelection(charIndex);
            textEditor.requestFocus();

            // Hide the input method
            hideInputMethod();

            // Can NOT delete it, as this function is repeatly called
            curStartLine = -1;
        }

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
        if (!(this.editorHorizontalLayout == null
                || String.valueOf(this.previousLineCount).length() == String
                .valueOf(this.textEditor.getLineCount()).length())) {
            this.editorHorizontalLayout.requestLayout();
        }
        this.previousLineCount = this.textEditor.getLineCount();
    }

    private void hideInputMethod() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showInputMethod() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, 0);
        }
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

    private void changeLineNumbers(boolean updateLineCount, int nd) {
        if (!updateLineCount || nd != this.previousMaxDigits) {
            this.updateLineCount = updateLineCount;
            LinearLayout.LayoutParams lp1 = (LinearLayout.LayoutParams) this.lineNumbers
                    .getLayoutParams();
            LinearLayout.LayoutParams lp2 = (LinearLayout.LayoutParams) this.lineDivider
                    .getLayoutParams();
            if (updateLineCount) {
                if (SettingEditorActivity.showLineNumbers(this)) {
                    this.lineNumbers.setVisibility(View.VISIBLE);
                    this.lineDivider.setVisibility(View.VISIBLE);
                }
                String digits = "";
                for (int i = 0; i < nd; i++) {
//                    digits = new StringBuilder(String.valueOf(digits))
//                            .append("9").toString();
                    digits += "9";
                }
                this.previousMaxDigits = nd;
                lp1.width = ((int) this.textEditor.getPaint()
                        .measureText(digits))
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
                if (optionsMenu != null) {
                    optionsMenu.findItem(R.id.action_save).setEnabled(true);
                }
            } else {
                enableButton(saveFileButton, false);
                if (optionsMenu != null) {
                    optionsMenu.findItem(R.id.action_save).setEnabled(false);
                }
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

    protected synchronized void updateUndoRedoState() {
        // Document document = curDocument;
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
        int start = textEditor.getSelectionStart();
        int end = textEditor.getSelectionEnd();
        String selected = textEditor.getText().toString().substring(start, end);
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
        textEditor.getText().replace(
                textEditor.getSelectionStart(), textEditor.getSelectionEnd(), text);
        textEditor.requestFocus();
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
            Matcher matcher = Pattern
                    .compile(this.findText.getText().toString(), flags)
                    .matcher(curDocument.getText());

            // Find all the occurrence
            List<Location> occurences = new ArrayList<>();
            int startIdx = 0;
            while (matcher.find(startIdx)) {
                int start = matcher.start();
                int end = matcher.end();
                occurences.add(new Location(start, end));
                startIdx = end;
            }

            // Do the replacement
            if (!occurences.isEmpty()) {
                String replacing = this.replaceText.getText().toString();
                int delta = 0;
                for (int i = 0; i < occurences.size(); ++i) {
                    Location loc = occurences.get(i);
                    this.textEditor.getEditableText().replace(loc.start + delta,
                            loc.end + delta, replacing);
                    delta += replacing.length() - (loc.end - loc.start);
                }
                Toast.makeText(getApplicationContext(),
                        String.format(getString(R.string.replace_all_ret),
                                occurences.size()),
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
            textEditor.getText().insert(textEditor.getSelectionStart(), "\n");
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
            String[] lines = textEditor.getText().toString().split("\n");
            int charIndex = 0;
            lineNO = (lineNO > lines.length) ? lines.length : lineNO;
            for (int i = 0; i < lineNO - 1; ++i) {
                charIndex += lines[i].length() + 1;
            }
            lineNumEdit.setText("");
            textEditor.setSelection(charIndex);
            textEditor.requestFocus();
        }
    }

    // Save the document
    private void executeSaveAction(final ICommonCallback callback) {
        ProcessingDialog dlg = new ProcessingDialog(this,
                new ProcessingInterface() {
                    @Override
                    public void process() throws Exception {
                        curDocument.save(TextEditNormalActivity.this, false);
                        curDocument.setChanged(false);
                    }

                    @Override
                    public void afterProcess() {
                        TextEditNormalActivity.this.updateSaveState();
                        TextEditNormalActivity.this.modifySaved = true;
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
//        boolean hideDocumentList;
//        final SharedPreferences prefs = PreferenceManager
//                .getDefaultSharedPreferences(getApplicationContext());
//        if (VERSION.SDK_INT < 11
//                || !prefs.getBoolean("hideDocumentList", false)) {
//            hideDocumentList = false;
//        } else {
//            hideDocumentList = true;
//        }

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
            String selection = this.textEditor.getText().subSequence(start, end)
                    .toString();
            if (!selection.contains("\n")) {
                this.findText.setText(selection);
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
            Toast.makeText(getApplicationContext(),
                    getString(R.string.not_found), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean executeFindAction() {
        int flags = Pattern.MULTILINE;
        if (this.toggleIgnoreCase.isChecked()) {
            flags = Pattern.MULTILINE | Pattern.CASE_INSENSITIVE;
        }
        if (!(this.toggleRegularExpression == null
                || this.toggleRegularExpression.isChecked())) {
            flags |= Pattern.LITERAL;
        }
        try {
            Matcher matcher = Pattern
                    .compile(this.findText.getText().toString(), flags)
                    .matcher(curDocument.getText());
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
        FrameLayout.LayoutParams lp1 = (FrameLayout.LayoutParams) this.editorLayout
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
        FrameLayout.LayoutParams lp1 = (FrameLayout.LayoutParams) this.editorLayout
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
        int width;
        if (openedDrawer) {
            int editorLayoutWidth = this.editorInnerLayout.getWidth();
            int lineNumberWidth = this.lineNumbers.getWidth();
            int dividerWidth = this.lineDivider.getWidth();
            int docListBtnWidth = this.documentListButton.getWidth();
            width = editorLayoutWidth - lineNumberWidth - dividerWidth - docListBtnWidth;
            this.textEditor.setMaxWidth(width);
            // this.textEditor.setMaxWidth(((this.editorInnerLayout.getWidth() -
            // this.lineNumbers.getWidth()) - this.lineDivider.getWidth()) -
            // this.documentListButton.getWidth());
        } else {
            width = ((this.editorInnerLayout.getWidth()
                    - this.lineNumbers.getWidth())
                    - this.lineDivider.getWidth())
                    - this.documentListDrawer.getWidth();
            this.textEditor.setMaxWidth(width);
        }
    }

    @Override
    public void insertString(String str) {
        int index = this.textEditor.getSelectionStart();
        Editable edit = this.textEditor.getEditableText();
        if (index < 0 || index >= edit.length()) {
            edit.append(str);
        } else {
            edit.insert(index, str);
        }
    }

    @Override
    public void onBackPressed() {
        // If doc is not changed, direct finish it
        if (curDocument != null && !curDocument.changed()) {
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
                                        TextEditNormalActivity.this.finish();
                                    }
                                });
                            }
                        })
                .setNegativeButton(R.string.donot_save,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                TextEditNormalActivity.this.finish();
                            }
                        })
                .setNeutralButton(android.R.string.cancel, null).show();
    }

    @Override
    public void operateLines(int from, int to) {
        boolean bModified = false;

        if (linesOP == MoreEditorOptionAdapter.CMD_DELETE_LINES) {
            String[] lines = textEditor.getText().toString().split("\n");
            if (from <= 0) {
                from = 1;
            }
            int end = (to <= lines.length ? to : lines.length);
            if (from <= end) {
                int startIndex = 0;
                for (int i = 1; i < from; i++) {
                    startIndex += lines[i - 1].length() + 1;
                }
                int endIndex = startIndex;
                for (int i = from; i <= end; i++) {
                    endIndex += lines[i - 1].length() + 1;
                }

                // delete the lines
                textEditor.getText().replace(startIndex, endIndex, "");
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
                StringBuilder sb = new StringBuilder();
                int startIndex = 0;
                for (int i = 1; i < from; i++) {
                    startIndex += lines[i - 1].length() + 1;
                }
                int endIndex = startIndex;
                for (int i = from; i <= end; i++) {
                    endIndex += lines[i - 1].length() + 1;
                }

                // Already commented, then uncomment it
                boolean isUnComment = allStartsWith(lines, from, end, "#");
                if (isUnComment) {
                    for (int i = from; i <= end; i++) {
                        int pos = lines[i - 1].indexOf('#');
                        if (pos > 0) {
                            sb.append(lines[i - 1].substring(0, pos));
                        }
                        sb.append(lines[i - 1].substring(pos + 1));
                        sb.append("\n");
                    }
                } else {
                    for (int i = from; i <= end; i++) {
                        sb.append("#");
                        sb.append(lines[i - 1]);
                        sb.append("\n");
                    }
                }

                // Replace with commented or uncommented lines
                textEditor.getText().replace(startIndex, endIndex, sb.toString());
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

    class UpdateLineCount extends TimerTask {
        UpdateLineCount() {
        }

        public void run() {
            debug("updateLineCount starts");
            try {
                if (!SettingEditorActivity.showLineNumbers((TextEditNormalActivity.this))) {
                    TextEditNormalActivity.this.previousLineCount = 0;
                    return;
                }
                if (TextEditNormalActivity.this.textWrap) {
                    updateWrap();
                } else {
                    updateNoWrap();
                }
            } catch (Exception ignored) {
            }
            TextEditNormalActivity.this.previousLineCount = TextEditNormalActivity.this.textEditor
                    .getLineCount();
            debug("updateLineCount ends");
        }

        private void updateWrap() {
            //long startTime = System.currentTimeMillis();
            String[] lines =
                    TextEditNormalActivity.this.textEditor.getText().toString().split("\n");
            TextEditNormalActivity.this.changeLineNumbers(
                    TextEditNormalActivity.this.updateLineCount,
                    getNumberDigits(lines.length + 1));
            int offset = 0;
            StringBuilder sb = new StringBuilder();
            int expected = 0;
            int i = 0;
            while (i < lines.length) {
                int expected2;
                while (true) {
                    expected2 = expected + 1;
                    if (expected >= TextEditNormalActivity.this.textEditor
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
            //long endTime = System.currentTimeMillis();

            TextEditNormalActivity.this.lineNumbers.setText(sb.toString());
        }

        private int getNumberDigits(int lines) {
            int nd = 1;
            while (lines >= 10) {
                lines /= 10;
                nd++;
            }
            return Math.max(2, nd);
        }

        private void updateNoWrap() {
            int lineCount = TextEditNormalActivity.this.textEditor.getLineCount();
            if (lineCount == 0) {
                lineCount = 1;
            }

            TextEditNormalActivity.this.changeLineNumbers(
                    TextEditNormalActivity.this.updateLineCount,
                    getNumberDigits(lineCount));

            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= lineCount; i++) {
                sb.append(i);
                sb.append('\n');
            }

            TextEditNormalActivity.this.lineNumbers.setText(sb.toString());
            TextEditNormalActivity.this.lineNumbers.requestLayout();
        }
    }

//    protected void updateEditor(boolean setSelection) {
//        // Log.d("DEBUG", "updateEditor called");
//        final Document document = curDocument;
//        // updateSpellCheckerState();
//        this.autoTextChange = true;
//        String strContent = document.getText();
//        this.textEditor.setText(strContent);
//        this.autoTextChange = false;
//        if (setSelection) {
//            int ss = document.getSelectionStart();
//            int se = document.getSelectionEnd();
//            Handler handler = new Handler();
//            this.textEditor.setSelectionNoHack(ss, se);
//            handler.postDelayed(new Runnable() {
//                public void run() {
//                    TextEditNormalActivity.this.scrollView.scrollTo(
//                            document.getScrollPositionX(),
//                            document.getScrollPositionY());
//                }
//            }, 400);
//        }
//        updateLineCount(true);
//        updateUndoRedoState();
//        updateSaveState();
//        // updateFooter();
//        // this.documentList.setItemChecked(this.currentDocument, true);
//        // if (VERSION.SDK_INT < 11 || getActionBar() == null) {
//        // updateTabs();
//        // } else if (getActionBar().getNavigationMode() == 1) {
//        // getActionBar().setSelectedNavigationItem(this.currentDocument);
//        // this.navigationBarAdapter.notifyDataSetChanged();
//        // }
//    }

    class SyntaxHighLight extends TimerTask {
        SyntaxHighLight() {
        }

        public void run() {
            debug("SyntaxHighLight starts");
            if (!enableSyntaxHighlight)
                return;
            final Document document = TextEditNormalActivity.this.curDocument;
            final Rect r = new Rect();
            if (TextEditNormalActivity.this.textEditor.getLocalVisibleRect(r)) {
                doHighlighting(document, r);
            } else {
                highlightingOnLayout(document, r);
            }
            TextEditNormalActivity.this.hlStart = -1;
            TextEditNormalActivity.this.hlEnd = -1;
            TextEditNormalActivity.this.hlChange = false;
            debug("SyntaxHighLight ends");
        }

        private void highlightingOnLayout(final Document document,
                                          final Rect r) {
            if (!enableSyntaxHighlight)
                return;
            document.syntaxHighlight(TextEditNormalActivity.this.textEditor,
                    TextEditNormalActivity.this.hlStart, TextEditNormalActivity.this.hlEnd, -1,
                    -1, TextEditNormalActivity.this.hlChange,
                    TextEditNormalActivity.this.getApplicationContext());
            TextEditNormalActivity.this.textEditor.getViewTreeObserver()
                    .addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                        @SuppressWarnings("deprecation")
                        public void onGlobalLayout() {
                            TextEditNormalActivity.this.textEditor.getViewTreeObserver()
                                    .removeGlobalOnLayoutListener(this);
                            if (TextEditNormalActivity.this.textEditor
                                    .getLocalVisibleRect(r)) {
                                doHighlighting(document, r);
                            }
                        }
                    });
        }

        private void doHighlighting(Document document, Rect r) {
            if (!enableSyntaxHighlight)
                return;
            try {
                int lineHeight = TextEditNormalActivity.this.textEditor.getLineHeight();
                int lstart = r.top / lineHeight;
//                int lend = (r.top + TextEditNormalActivity.this.getWindowManager()
//                        .getDefaultDisplay().getHeight()) / lineHeight;
                int lend = r.bottom / lineHeight;
                if (TextEditNormalActivity.this.textWrap) {
                    int rlstart = 0;
                    int rlend = 0;
                    String[] lines = TextEditNormalActivity.this.textEditor.getText()
                            .toString().split("\\n");
                    int offset = 0;
                    for (int i = 0; i < lines.length; i++) {
                        int line = TextEditNormalActivity.this.textEditor.getLineNumber(offset);
                        offset += lines[i].length() + 1;
                        if (line <= lstart) {
                            rlstart = i;
                        }
                        if (line <= lend) {
                            rlend = i;
                        }
                        // New added
                        else {
                            break;
                        }
                    }
                    lstart = rlstart;
                    lend = rlend;
                }
                lstart -= TextEditNormalActivity.this.highlightSize;
                lend += TextEditNormalActivity.this.highlightSize;
                // timer.lastTime("Prepare Time");
                long startTime = System.currentTimeMillis();
                document.syntaxHighlight(TextEditNormalActivity.this.textEditor,
                        /* TestActivity.this.currentTheme, */
                        TextEditNormalActivity.this.hlStart, TextEditNormalActivity.this.hlEnd,
                        lstart, lend, TextEditNormalActivity.this.hlChange,
                        TextEditNormalActivity.this.getApplicationContext());
                long endTime = System.currentTimeMillis();
                if (endTime - startTime > 2000) {
                    TextEditNormalActivity.this.enableSyntaxHighlight = false;
                }
                // timer.lastTime("Highlight Time");
            } catch (Exception ignored) {
            }
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
            Document doc = new Document(TextEditNormalActivity.this, new File(curFilePath),
                    syntaxFileName);
            try {
                // Consider load the file from temporary file
                if (unsavedFilePath != null) {
                    doc.load(TextEditNormalActivity.this, unsavedFilePath, R.string.error_file_too_big);
                    doc.setChanged(true);
                    boolean ret = new File(unsavedFilePath).delete();
                    unsavedFilePath = null;
                } else {
                    doc.load(TextEditNormalActivity.this, curFilePath, R.string.error_file_too_big);
                }

                TextEditNormalActivity.this.curDocument = doc;
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

                textEditor.removeTextChangedListener(textWatcher);
                String strContent = curDocument.getText();
                textEditor.setText(strContent);
                int tmpLine = textEditor.getLineNumber(801640 - 1);
                textEditor.addTextChangedListener(textWatcher);

                // Set max digits for left line number view
                int lines = curDocument.getText().split("\n").length + 1;
                int digits = ulcTask.getNumberDigits(lines);
                changeLineNumbers(true, digits);

                updateLineCount(true);
                syntaxHighlight(-1, -1, false);
                updateSaveState();
            } else {
                Toast.makeText(TextEditNormalActivity.this, "Failed to open " + curFilePath,
                        Toast.LENGTH_LONG).show();
            }

            // Set max width for title, so that method menu can be fully shown
            filenameTv.setMaxWidth(Display.getWidth(TextEditNormalActivity.this) -
                    previousMenu.getWidth() - nextMenu.getWidth() - methodMenu.getWidth() -
                    (int) (16 * getResources().getDisplayMetrics().density));
        }
    }

    // Editor text change listener
    private class TextChangeListener implements TextWatcher {
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            Document document = TextEditNormalActivity.this.curDocument;
            if (document == null) {
                return;
            }
            boolean wasChanged = document.changed();
            if (!TextEditNormalActivity.this.autoTextChange) {
                document.textChanged(s, start, before, count);
            }
            TextEditNormalActivity.this.syntaxHighlight(
                    Math.min(TextEditNormalActivity.this.hlStart, start),
                    Math.max(before, count) + start, true);
            TextEditNormalActivity.this.updateUndoRedoState();
            TextEditNormalActivity.this.updateLineCount(true);
            if (!wasChanged && document.changed()) {
                TextEditNormalActivity.this.updateSaveState();
                // TestActivity.this.notifyDocumentListChanged();
            }
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void afterTextChanged(Editable e) {
        }
    }
}
