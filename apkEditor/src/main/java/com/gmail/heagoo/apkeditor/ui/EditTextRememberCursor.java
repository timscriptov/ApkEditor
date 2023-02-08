package com.gmail.heagoo.apkeditor.ui;

import static java.lang.Math.abs;

import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.appcompat.widget.AppCompatEditText;

import com.gmail.heagoo.SelectionChangedListener;
import com.gmail.heagoo.apkeditor.base.R;

import java.lang.ref.WeakReference;


public class EditTextRememberCursor extends AppCompatEditText {
    private int selStart;
    private int selEnd;

    private WeakReference<Context> ctxRef;
    private WeakReference<LayoutObListView> parentRef;
    private WeakReference<SelectionChangedListener> textSelectionListener;

    public EditTextRememberCursor(Context context) {
        super(context);
    }

    public EditTextRememberCursor(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextRememberCursor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setParent(Context ctx, WeakReference<LayoutObListView> parentRef) {
        this.ctxRef = new WeakReference<>(ctx);
        this.parentRef = parentRef;
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (selStart == selEnd) {
            // When inside the layout, as list view will request focus, it will cause edit text reset the selection
            if (parentRef != null && parentRef.get() != null) {
                if (parentRef.get().isInsideChildLayout()) {
                    return;
                }
            }
        }

        this.selStart = selStart;
        this.selEnd = selEnd;
        Integer position = (Integer) this.getTag(R.id.text);
        if (position != null) {
            // Notify the listener
            if (this.textSelectionListener != null) {
                SelectionChangedListener listener = textSelectionListener.get();
                if (listener != null) {
                    String strSeletected = "";
                    if (selStart < selEnd) {
                        String content = getText().toString();
                        try {
                            strSeletected = content.substring(selStart, selEnd);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    listener.textSelected(position, selStart, selEnd, strSeletected);
                }
            }

            LayoutObListView list = parentRef.get();
            if (list != null) {
                list.setCurrentSelection(position.intValue(), selStart, selEnd);
            }
        }
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            LayoutObListView parent = parentRef.get();
            if (parent != null && parent.isInputMethodVisible()) {
                // Hide keyboard
                InputMethodManager mgr = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.hideSoftInputFromWindow(this.getWindowToken(), 0);

                // Set as consumed
                return true;
            }
        }

        return false;
    }

    public void selectionClickable() {
        setOnTouchListener(new View.OnTouchListener() {
            private boolean bPossibleEvent = false;
            private float downX;
            private float downY;
            private long downTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    bPossibleEvent = false; // reset the status
                    int selectionStart = selStart;
                    int selectionEnd = selEnd;

                    // Some text is selected
                    if (selectionStart != selectionEnd) {
                        Layout layout = ((EditText) v).getLayout();
                        float x = event.getX() + getScrollX();
                        float y = event.getY() + getScrollY();
                        int line = layout.getLineForVertical((int) y);
                        int offset = layout.getOffsetForHorizontal(line, x);

                        // Click on the selection (and input method is hide), omit the event
                        boolean inputMethodShown = false;
                        LayoutObListView parent = parentRef.get();
                        if (parent != null && parent.isInputMethodVisible()) {
                            inputMethodShown = true;
                        }
                        if (offset >= selectionStart && offset < selectionEnd
                                && !inputMethodShown) {
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
    }

    private void showInputMethod() {
        InputMethodManager imm = (InputMethodManager) ctxRef.get().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(this, 0);
    }

    public void setTextSelectionListener(WeakReference<SelectionChangedListener> textSelectionListener) {
        this.textSelectionListener = textSelectionListener;
    }
}
