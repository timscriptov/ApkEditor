package com.gmail.heagoo.neweditor;

import android.content.Context;
import android.text.Spannable;
import android.text.style.CharacterStyle;
import android.widget.EditText;

import com.gmail.heagoo.neweditor.TokenMarker.LineContext;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

public class Document implements Serializable {
    public static final String LINE_SEPARATOR_UNIX = "\n";
    public static final String LINE_SEPARATOR_WINDOWS = "\r\n";

    private static int undoLevel = 64;
    protected boolean changed = false;
    protected String text = null;
    private transient ArrayList<LineContext> baseToken;
    private boolean clearNext;
    private int lastSHLineEnd;
    private int lastSHLineStart;
    private File mFile;
    private Vector<Change> redo = new Vector<Change>();
    private int scrollPositionX = 0;
    private int scrollPositionY = 0;
    private int selectionEnd;
    private int selectionStart;
    private Vector<Change> undo = new Vector<Change>();

    private transient TokenMarker mTokenMarker;
    private ColorTheme colorTheme;

    public Document(Context context, File file, String syntaxFileName) {
        this.mFile = file;
        this.colorTheme = new ColorTheme(context);

        String syntaxName = syntaxFileName;
        if (syntaxName == null) {
            syntaxName = getSyntaxName(file.getName());
        }

        XmlSyntaxParser xmh = new XmlSyntaxParser();
        try {
            XMLReader parser = SAXParserFactory.newInstance().newSAXParser()
                    .getXMLReader();
            // mode.setTokenMarker(xmh.getTokenMarker());
            try {
                try {
                    InputSource isrc = new InputSource(new BufferedInputStream(
                            context.getAssets().open("syntax/" + syntaxName)));
                    parser.setContentHandler(xmh);
                    parser.setDTDHandler(xmh);
                    parser.setEntityResolver(xmh);
                    parser.setErrorHandler(xmh);
                    parser.parse(isrc);
                    this.mTokenMarker = xmh.getTokenMarker();
                } catch (Throwable th) {
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (SAXException saxe) {
            saxe.printStackTrace();
        } catch (ParserConfigurationException e2) {
            e2.printStackTrace();
        }

    }

    public static void setUndoLevel(int undoLevel) {
        undoLevel = undoLevel;
    }

    private String getSyntaxName(String fileName) {
        int pos = fileName.lastIndexOf('.');
        if (pos != -1) {
            String suffix = fileName.substring(pos + 1);
            if (suffix.equals("htm")) {
                suffix = "html";
            }
            return suffix + ".xml";
        }
        return "txt.xml";
    }

    public void setSelection(int selectionStart, int selectionEnd) {
        this.selectionStart = selectionStart;
        this.selectionEnd = selectionEnd;
    }

    public int getSelectionStart() {
        return this.selectionStart;
    }

    public int getSelectionEnd() {
        return this.selectionEnd;
    }

    // public void load(Context context, boolean rootMode) throws Exception {
    // load(context, this.mFile.getAbsolutePath());
    // }

    public void textChanged(CharSequence s, int start, int before, int count) {
        try {
            String oldText = this.text;
            this.text = s.toString();
            Change change = new Change(start, oldText.substring(start, start
                    + before), this.text.substring(start, start + count));
            if (this.undo.size() <= 0
                    || !((Change) this.undo.lastElement()).compatible(change)) {
                this.undo.addElement(change);
            } else {
                ((Change) this.undo.lastElement()).add(change);
            }
            while (undoLevel != 0 && this.undo.size() > undoLevel) {
                this.undo.remove(0);
            }
            this.redo.clear();
            this.changed = true;
        } catch (Exception e) {
        }
    }

//    public boolean isBigFile() {
//        return text != null && text.length() > 128 * 1024;
//    }

    // stringId = R.string.error_file_too_big
    public void load(Context context, String path, int stringId)
            throws IOException {
        File f = new File(path);
        FileInputStream fis = new FileInputStream(f);

        long size = f.length();
        if (size > 4 * 1024 * 1024) {
            throw new IOException(context.getString(stringId));
        }
        byte[] bts = new byte[((int) size)];
        int offset = 0;
        while (offset < bts.length) {
            int numRead = fis.read(bts, offset, bts.length - offset);
            if (numRead < 0) {
                break;
            }
            offset += numRead;
        }
        this.text = new String(bts, Encoding.DEFAULT_CHARSET_NAME);
        //this.text = this.text.replaceAll("\\r\\n", LINE_SEPARATOR_UNIX);
        //this.text = this.text.replaceAll("\\r", LINE_SEPARATOR_UNIX);
        this.changed = false;
        fis.close();
    }

    private boolean isLetter(int i) {
        char c = this.text.charAt(i);
        return Character.isLetter(c)
                || ((c == '\'' || c == '-') && surroundedByLetters(i));
    }

    private boolean surroundedByLetters(int i) {
        if (i <= 0 || i >= this.text.length() - 1
                || !Character.isLetter(this.text.charAt(i - 1))
                || !Character.isLetter(this.text.charAt(i + 1))) {
            return false;
        }
        return true;
    }

//    public void setText(String text) {
//        this.text = text;
//    }

    public String getText() {
        return this.text;
    }

    public String toString() {
        if (this.changed) {
            return getName() + " *";
        }
        return getName();
    }

    public String getName() {
        if (this.mFile != null) {
            return this.mFile.getName();
        }
        return "untitled";
    }

    public String getPath() {
        if (this.mFile == null) {
            return null;
        }
        return this.mFile.getPath();
    }

    public Change undo() {
        int selection = 0;
        Change change = null;
        if (!this.undo.isEmpty()) {
            change = (Change) this.undo.lastElement();
            this.undo.removeElement(change);
            String textBefore = this.text.substring(0, change.getStart());
            this.text = new StringBuilder(String.valueOf(textBefore))
                    .append(change.getOldText())
                    .append(this.text.substring(change.getStart()
                            + change.getNewText().length())).toString();
            this.redo.insertElementAt(change, 0);
            this.changed = true;
            selection = change.getStart() + change.getOldText().length();
        }
        this.selectionStart = selection;
        this.selectionEnd = selection;
        return change;
    }

    public Change redo() {
        int selection = 0;
        Change change = null;
        if (!this.redo.isEmpty()) {
            change = (Change) this.redo.firstElement();
            this.redo.removeElement(change);
            String textBefore = this.text.substring(0, change.getStart());
            this.text = new StringBuilder(String.valueOf(textBefore))
                    .append(change.getNewText())
                    .append(this.text.substring(change.getStart()
                            + change.getOldText().length())).toString();
            this.undo.addElement(change);
            this.changed = true;
            selection = change.getStart() + change.getNewText().length();
        }
        this.selectionEnd = selection;
        this.selectionStart = selection;
        return change;
    }

    public boolean canUndo() {
        return this.undo.size() > 0;
    }

    public boolean changed() {
        return this.changed;
    }

    public boolean canRedo() {
        return this.redo.size() > 0;
    }

    public void save(Context context, boolean rootMode) throws Exception {
        save(this.mFile.getAbsolutePath(), context);
    }

    public void save(String path, Context context) throws IOException {
        FileOutputStream fos = new FileOutputStream(new File(path));

        OutputStreamWriter osw = new OutputStreamWriter(fos,
                Encoding.DEFAULT_CHARSET_NAME);
        BufferedWriter bw = new BufferedWriter(osw);
        bw.write(this.text);
        bw.flush();
        osw.close();
        fos.close();
    }

    public void syntaxHighlight(EditText textEditor, /* Theme theme, */
                                int start, int end, int lstart, int lend, boolean change,
                                Context context) {
        try {
            if (this.mTokenMarker == null) {
                if (this.clearNext) {
                    clearSyntaxHighlighting(textEditor);
                }
                this.clearNext = false;
                return;
            }
            Spannable spannable = textEditor.getText();
            String strContent = spannable.toString();
            String[] lines = strContent.split("\\n");
            Segment segment = new Segment();
            int pos = 0;
            LineContext lastTokenType = null;
            int l = 0;
            while (l < lines.length) {
                LineContext initialTokenType = lastTokenType;
                if (l >= this.lastSHLineStart && l <= this.lastSHLineEnd
                        && (l < lstart || l > lend)) {
                    clearSyntaxHighlighting(textEditor.getText(), pos,
                            lines[l].length() + pos);
                }
                if ((!change || start > lines[l].length() + pos || end < pos)
                        && ((change || l < lstart || l > lend) && (getBaseToken(l) == lastTokenType
                        || l < lstart || l > lend))) {
                    lastTokenType = getBaseToken(l + 1);
                } else {
                    clearSyntaxHighlighting(spannable, pos, lines[l].length() + pos);
                    try {
                        DefaultTokenHandler dth = new DefaultTokenHandler();
                        segment.array = lines[l].toCharArray();
                        segment.offset = 0;
                        segment.count = lines[l].length();
                        lastTokenType = this.mTokenMarker.markTokens(
                                lastTokenType, dth, segment);
                        Token token = dth.getTokens();
                        token = TokenMerger.merge(token);
                        while (token != null) {
                            if (l >= lstart && l <= lend) {
                                // token.id = 0 means NULL
                                if (token.length > 0 && token.id != 0) {
                                    int _start = pos + token.offset;
                                    int _end = token.length
                                            + (token.offset + pos);
                                    setSpan(spannable,
                                            new SyntaxHighlightSpan(colorTheme
                                                    .getForegroundColor(token)),
                                            _start, _end, 33);
                                }
                            }
                            token = token.next;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    setBaseToken(l, initialTokenType);
                }
                pos += lines[l].length() + 1;
                l++;
            }
            this.lastSHLineStart = lstart;
            this.lastSHLineEnd = lend;
        } catch (Error e2) {
            e2.printStackTrace();
        }
    }

    public void syntaxHighlight(EditText textEditor) {
        try {
            if (this.mTokenMarker == null) {
                if (this.clearNext) {
                    clearSyntaxHighlighting(textEditor);
                }
                this.clearNext = false;
                return;
            }
            Spannable spannable = textEditor.getText();
            String strContent = spannable.toString();
            String[] lines = strContent.split("\\n");
            Segment segment = new Segment();
            int pos = 0;
            LineContext lastTokenType = null;
            int l = 0;
            while (l < lines.length) {
                LineContext initialTokenType = lastTokenType;

                clearSyntaxHighlighting(spannable, pos, lines[l].length() + pos);
                try {
                    DefaultTokenHandler dth = new DefaultTokenHandler();
                    segment.array = lines[l].toCharArray();
                    segment.offset = 0;
                    segment.count = lines[l].length();
                    lastTokenType = this.mTokenMarker.markTokens(
                            lastTokenType, dth, segment);
                    Token token = dth.getTokens();
                    token = TokenMerger.merge(token);
                    while (token != null) {
                        // token.id = 0 means NULL
                        if (token.length > 0 && token.id != 0) {
                            int _start = pos + token.offset;
                            int _end = token.length
                                    + (token.offset + pos);
                            setSpan(spannable,
                                    new SyntaxHighlightSpan(colorTheme
                                            .getForegroundColor(token)),
                                    _start, _end, 33);
                        }

                        token = token.next;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                setBaseToken(l, initialTokenType);

                pos += lines[l].length() + 1;
                l++;
            }
        } catch (Error e2) {
            e2.printStackTrace();
        }
    }

    private void setBaseToken(int line, LineContext initialTokenType) {
        if (line >= 0) {
            if (this.baseToken == null) {
                this.baseToken = new ArrayList();
            }
            while (line >= this.baseToken.size()) {
                this.baseToken.add(null);
            }
            this.baseToken.set(line, initialTokenType);
        }
    }

    private LineContext getBaseToken(int line) {
        if (this.baseToken == null) {
            this.baseToken = new ArrayList();
        }
        if (line >= this.baseToken.size()) {
            return null;
        }
        return (LineContext) this.baseToken.get(line);
    }

    private void setSpan(Spannable spannable, CharacterStyle style, int i,
                         int j, int flags) {
        spannable.setSpan(style, i, j, flags);
    }

    public void clearSyntaxHighlighting(EditText textEditor) {
        clearSyntaxHighlighting(textEditor.getText(), 0, this.text.length());
        // clearBaseTokens();
    }

    public void clearSyntaxHighlighting(Spannable spannable, int start, int end) {
        Object[] toRemoveSpans = spannable.getSpans(start, end,
                SyntaxHighlightSpan.class);
        for (Object removeSpan : toRemoveSpans) {
            spannable.removeSpan(removeSpan);
        }
    }

    public String getExtension() {
        if (this.mFile == null || this.mFile.getName() == null
                || !this.mFile.getName().contains(".")) {
            return null;
        }
        return this.mFile.getName().substring(
                this.mFile.getName().lastIndexOf(46) + 1);
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public String getNameNoExtension() {
        String name = getName();
        if (name.contains(".")) {
            return name.substring(0, name.lastIndexOf(46));
        }
        return name;
    }

    public String getNumberWords() {
        return getNumberWords(0, this.text.length());
    }

    public String getNumberWords(int start, int end) {
        if (start > end) {
            int temp = start;
            start = end;
            end = temp;
        }
        if (start == end) {
            start = 0;
            end = this.text.length();
        }
        int words = 0;
        int lines = 1;
        boolean wasLastWhiteSpace = true;
        int i = Math.max(0, start);
        while (i < Math.min(this.text.length(), end)) {
            if (wasLastWhiteSpace && isLetter(i)) {
                words++;
            }
            if (isLetter(i)) {
                wasLastWhiteSpace = false;
            } else {
                wasLastWhiteSpace = true;
            }
            if (this.text.charAt(i) == '\n') {
                lines++;
            }
            i++;
        }
        return new StringBuilder(String.valueOf(lines)).append(" / ")
                .append(words).append(" / ").append(end - start).toString();
    }

    public void setScrollPosition(int x, int y) {
        this.scrollPositionX = x;
        this.scrollPositionY = y;
    }

    public int getScrollPositionX() {
        return this.scrollPositionX;
    }

    public int getScrollPositionY() {
        return this.scrollPositionY;
    }

    public void setFile(File file) {
        this.mFile = file;
    }
}