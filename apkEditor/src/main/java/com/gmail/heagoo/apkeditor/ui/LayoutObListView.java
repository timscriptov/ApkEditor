package com.gmail.heagoo.apkeditor.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

import androidx.annotation.RequiresApi;

import com.gmail.heagoo.neweditor.InputMethodWatcher;

public class LayoutObListView extends ListView implements InputMethodWatcher {
    private boolean insideChildLayout = false;
    private boolean inputMethodVisible;

    // Record current selection item
    private int selPosition = -1;
    private int selStart;
    private int selEnd;

    public LayoutObListView(Context context) {
        super(context);
    }

    public LayoutObListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LayoutObListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(21)
    public LayoutObListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void layoutChildren() {
        insideChildLayout = true;
        super.layoutChildren();
        insideChildLayout = false;
    }

    public boolean isInsideChildLayout() {
        return insideChildLayout;
    }

    public boolean isInputMethodVisible() {
        return inputMethodVisible;
    }

    @Override
    public void setInputMethodVisible(boolean visible) {
        this.inputMethodVisible = visible;
    }

    // To remember last selection
    public void setCurrentSelection(int position, int selStart, int selEnd) {
        // If the position is not visible, omit the incorrect selection
        if (position < this.getFirstVisiblePosition()) {
            return;
        }
        if (position > this.getLastVisiblePosition()) {
            return;
        }

        this.selPosition = position;
        this.selStart = selStart;
        this.selEnd = selEnd;

        //Log.e("DEBUG", "setCurrentSelection: position=" + position + ", selStart=" + selStart + ", selEnd=" + selEnd);
    }

    // When update the content of the list (like delete some lines), should reset the selection
    public void resetSelection() {
        this.selPosition = 0;
        this.selStart = 0;
        this.selEnd = 0;
    }

    public int getSelPosition() {
        return selPosition;
    }

    public int getSelStart() {
        return selStart;
    }

    public int getSelEnd() {
        return selEnd;
    }
}
