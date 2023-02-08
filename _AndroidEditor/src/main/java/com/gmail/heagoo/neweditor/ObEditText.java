package com.gmail.heagoo.neweditor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.text.DynamicLayout;
import android.text.style.BackgroundColorSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.lang.ref.WeakReference;

// ObservableEditText
public class ObEditText extends EditText implements InputMethodWatcher {
    // private static char[] brackets = new char[]{'{', '[', '(', '}', ']',
    // ')'};
    private boolean arrowKeyPressed;
    private BackgroundColorSpan closeBracketSpan = new BackgroundColorSpan(-256);
    private boolean editable = true;
    private int lastColumn = -1;
    private int lastLine = -1;
    private int lastPosition;
    private Rect mBounds = new Rect();
    private int mNumberCols;
    private boolean mNumberColsChanged;
    private int mNumberLines;
    private boolean mWrapped;
    private BackgroundColorSpan openBracketSpan = new BackgroundColorSpan(-256);
    private boolean positionHack = false;
    private int secLastPosition;
    private WeakReference<TextSelectionListener> textSelectionListener = null;

    // Is the input method visible or not
    private boolean bInputMethodVisible;

    public ObEditText(Context context) {
        super(context);
    }

    public ObEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ObEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPositionHack(boolean positionHack) {
        this.positionHack = positionHack;
    }

    public void setBracketSpanColor(int color) {
        this.openBracketSpan = new BackgroundColorSpan(color);
        this.closeBracketSpan = new BackgroundColorSpan(color);
    }

    public void setTextSelectionListener(TextSelectionListener textSelectionListener) {
        this.textSelectionListener = new WeakReference<>(textSelectionListener);
    }

    protected void onSelectionChanged(int selStart, int selEnd) {
        //Log.d("DEBUG", "onSelectionChanged" + selStart + "," + selEnd);
        super.onSelectionChanged(selStart, selEnd);
        if (this.textSelectionListener != null) {
            TextSelectionListener listener = this.textSelectionListener.get();
            if (listener != null) {
                listener.selectionChanged(selStart, selEnd);
            }
        }
        //Log.d("DEBUG", "onSelectionChanged end");
    }

//	protected void onSelectionChanged(int selStart, int selEnd) {
//		Log.d("DEBUG", "onSelectionChanged" + selStart + "," + selEnd);
//
//		try {
//			if (this.positionHack && selStart == this.secLastPosition
//					&& selStart == selEnd) {
//				selStart = this.lastPosition;
//				setSelection(selStart);
//			} else if (selStart == selEnd) {
//				int currentLine = getLineNumber(selStart);
//				int lineStart = getLineStart(currentLine);
//				int currentColumn = selStart - lineStart;
//				if (this.arrowKeyPressed
//						&& Math.abs(currentLine - this.lastLine) == 1) {
//					int lineEnd = getLineEnd(currentLine);
//					if (selStart < this.lastColumn + lineStart) {
//						selStart = Math.min(this.lastColumn + lineStart,
//								lineEnd);
//					}
//					setSelection(selStart);
//				} else {
//					this.lastColumn = currentColumn;
//				}
//				this.lastLine = currentLine;
//				selEnd = selStart;
//			} else {
//				this.lastLine = -1;
//				this.lastColumn = -1;
//			}
//			super.onSelectionChanged(selStart, selEnd);
//			if (this.textSelectionListener != null) {
//				this.textSelectionListener.selectionChanged(selStart, selEnd);
//			}
//			this.secLastPosition = this.lastPosition;
//			this.lastPosition = selStart;
//			if (selStart == selEnd) {
//				checkMatchingBracket(selStart);
//			}
//		} catch (Exception e) {
//		}
//		Log.d("DEBUG", "onSelectionChanged end");
//	}

    protected void onTextChanged(CharSequence text, int start,
                                 int lengthBefore, int lengthAfter) {
        //Log.d("DEBUG", "onTextChanged start");
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (getSelectionStart() == getSelectionEnd()) {
            checkMatchingBracket(getSelectionStart());
        }
        //Log.d("DEBUG", "onTextChanged end");
    }

    private void checkMatchingBracket(int pos) {
        // getText().removeSpan(this.openBracketSpan);
        // getText().removeSpan(this.closeBracketSpan);
        // if (pos > 0 && pos <= getText().length()) {
        // char c1 = getText().charAt(pos - 1);
        // for (int i = 0; i < brackets.length; i++) {
        // if (brackets[i] == c1) {
        // char c2 = brackets[(i + 3) % 6];
        // boolean open = false;
        // if (i <= 2) {
        // open = true;
        // }
        // int k;
        // if (open) {
        // int nob = 1;
        // for (k = pos; k < getText().length(); k++) {
        // if (getText().charAt(k) == c2) {
        // nob--;
        // }
        // if (getText().charAt(k) == c1) {
        // nob++;
        // }
        // if (nob == 0) {
        // showBracket(pos - 1, k);
        // break;
        // }
        // }
        // } else {
        // int ncb = 1;
        // for (k = pos - 2; k >= 0; k--) {
        // if (getText().charAt(k) == c2) {
        // ncb--;
        // }
        // if (getText().charAt(k) == c1) {
        // ncb++;
        // }
        // if (ncb == 0) {
        // showBracket(k, pos - 1);
        // break;
        // }
        // }
        // }
        // }
        // }
        // }
    }

    private void showBracket(int i, int j) {
        getText().setSpan(this.openBracketSpan, i, i + 1, 33);
        getText().setSpan(this.closeBracketSpan, j, j + 1, 33);
    }

    public void setSelectionNoHack(int selectionStart, int selectionEnd) {
        boolean oldPositionHack = this.positionHack;
        this.positionHack = false;
        try {
            setSelection(selectionStart, selectionEnd);
        } catch (Exception e) {
        }
        this.positionHack = oldPositionHack;
    }

    public int getLineNumber(int offset) {
        if (getLayout() != null) {
            return ((DynamicLayout) getLayout()).getLineForOffset(offset);
        }
        return 0;
    }

    private int getLineEnd(int line) {
        return ((DynamicLayout) getLayout()).getLineEnd(line) - 1;
    }

    private int getLineStart(int line) {
        return ((DynamicLayout) getLayout()).getLineStart(line);
    }

    public void setWidth(int pixels) {
        if (pixels != 0) {
            super.setWidth(pixels);
        }
    }

    public boolean onCheckIsTextEditor() {
        return isEditable();
    }

    public boolean isEditable() {
        return this.editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void setArrowKeyPressed(boolean arrowKeyPressed) {
        this.arrowKeyPressed = arrowKeyPressed;
    }

    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        //Log.d("DEBUG", "onCreateInputConnection called");
        if (!PreferenceManager.getDefaultSharedPreferences(getContext())
                .getBoolean("rawKeys", false)) {
            return super.onCreateInputConnection(outAttrs);
        }
        BaseInputConnection fic = new BaseInputConnection(this, false) {
            public boolean deleteSurroundingText(int beforeLength,
                                                 int afterLength) {
                if (beforeLength != 1 || afterLength != 0) {
                    return super.deleteSurroundingText(beforeLength,
                            afterLength);
                }
                if (super.sendKeyEvent(new KeyEvent(0, 67))
                        && super.sendKeyEvent(new KeyEvent(1, 67))) {
                    return true;
                }
                return false;
            }
        };
        outAttrs.actionLabel = null;
        outAttrs.inputType = 0;
        outAttrs.imeOptions = 1342177280;
        return fic;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //Log.d("DEBUG", "onMeasure called");
        if (this.mWrapped) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        float width = getPaint().measureText("X");
        int height = getLineHeight();
        updateFastLineCount();
        setMeasuredDimension(
                Math.max(((int) (((float) (String.valueOf(this.mNumberLines)
                                .length() + this.mNumberCols)) * width)) + 10,
                        MeasureSpec.getSize(widthMeasureSpec)), Math.max(
                        (this.mNumberLines * height) + 10,
                        MeasureSpec.getSize(heightMeasureSpec)));
        if (this.mNumberColsChanged) {
            requestReflow();
        }
    }

    public void updateFastLineCount() {
        boolean z = true;
        String[] lines = getText().toString().split("\\n");
        int oldNumberCols = this.mNumberCols;
        this.mNumberCols = 0;
        this.mNumberLines = lines.length;
        for (String string : lines) {
            this.mNumberCols = Math.max(this.mNumberCols, string.length());
        }
        if (this.mNumberLines == 0) {
            this.mNumberLines = 1;
        }
        if (oldNumberCols == this.mNumberCols) {
            z = false;
        }
        this.mNumberColsChanged = z;
    }

    @SuppressLint("WrongCall")
    public final void fastMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setWrapped(boolean wrapped) {
        this.mWrapped = wrapped;
    }


    public void requestReflow() {
        setPaintFlags(getPaintFlags() + 1);
        setPaintFlags(getPaintFlags() - 1);
    }

    @Override
    public void setInputMethodVisible(boolean visible) {
        this.bInputMethodVisible = visible;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (bInputMethodVisible) {
                // Hide keyboard
                InputMethodManager mgr = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.hideSoftInputFromWindow(this.getWindowToken(), 0);

                // Set as consumed
                return true;
            }
        }
        return false;
    }
}