package com.gmail.heagoo.apkeditor;


import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.gmail.heagoo.SelectionChangedListener;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.ui.LayoutObListView;
import com.gmail.heagoo.neweditor.Document;

import java.util.Arrays;
import java.util.List;

public class TextEditBigListHelper {

    private static final int MSG_SELECTION = 1000;
    private LayoutObListView textList;
    private TextEditBigListAdapter adapter;
    private int inputType;
    private int backgroundColor;
    private int textColor;
    private int bracketSpanColor;
    // Font size
    private int complexUnitSp;
    private float fontSize;
    private MyHandler myHandler = new MyHandler();

    public TextEditBigListHelper(Context ctx, LayoutObListView textList) {
        this.textList = textList;
        this.adapter = new TextEditBigListAdapter(ctx, textList);
        textList.setAdapter(adapter);
        textList.setDivider(null);
        textList.setItemsCanFocus(true);
    }

    ;

    public void setInputType(int inputType) {
        this.inputType = inputType;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
        textList.setBackgroundColor(backgroundColor);
    }

    public void setTextColor(int textColor) {
        adapter.setTextColor(textColor);
    }

    public void setBracketSpanColor(int bracketSpanColor) {
        this.bracketSpanColor = bracketSpanColor;
    }

    public void requestFocus() {
    }

    public void selectItem(int itemPos) {
        textList.setSelection(itemPos);
    }

    public void setSelection(int itemPosition, int selStart, int selEnd) {
        int firstVisible = textList.getFirstVisiblePosition();
        int lastVisible = textList.getLastVisiblePosition();

        // The position is invisible, first scroll to that position
        if (itemPosition < firstVisible || itemPosition > lastVisible) {
            selectItem(itemPosition);
            textList.requestFocus();
            myHandler.setDelayedSelection(itemPosition, selStart, selEnd);
        }
        // Directly set selection
        else {
            EditText et = getEditTextByPosition(itemPosition);
            if (et != null) {
                // Need to request focus then set selection
                et.requestFocus();
                et.setSelection(selStart, selEnd);
            }
        }
    }

    // Which line/item is selected
    public int getSelectionItemIndex() {
        int position = textList.getSelectedItemPosition();
        if (position == AdapterView.INVALID_POSITION) {
            position = textList.getSelPosition();
        }
        return position;
    }

    // Selection start position inside the line
    public int getSelectionStart() {
        int position = getSelectionItemIndex();
        EditText textEt = getEditTextByPosition(position);
        if (textEt != null) {
            return textEt.getSelectionStart();
        }
        return 0;
    }

    public int getSelectionEnd() {
        int position = getSelectionItemIndex();
        EditText textEt = getEditTextByPosition(position);
        if (textEt != null) {
            return textEt.getSelectionEnd();
        }
        return 0;
    }

    public EditText getEditTextByPosition(int pos) {
        if (pos < 0) {
            return null;
        }

        final int firstListItemPosition = textList.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + textList.getChildCount() - 1;

        View view;
        if (pos < firstListItemPosition || pos > lastListItemPosition) {
            view = textList.getAdapter().getView(pos, null, textList);
        } else {
            final int childIndex = pos - firstListItemPosition;
            view = textList.getChildAt(childIndex);
        }

        if (view != null) {
            return (EditText) view.findViewById(R.id.text);
        }

        return null;
    }

    public CharSequence getText() {
        return adapter.getText();
    }

    public void setText(String text) {
        String[] lines = text.split("\\r?\\n");
        adapter.setText(Arrays.asList(lines));
        adapter.notifyDataSetChanged();
    }

    // Directly set each lines
    public void setText(List<String> lines) {
        adapter.setText(lines);
        adapter.notifyDataSetChanged();
    }

    public List<String> getTextLines() {
        return adapter.getTextLines();
    }

    public void replaceSelection(String text) {
        int itemIndex = getSelectionItemIndex();
        if (itemIndex >= 0) {
            int start = getSelectionStart();
            int end = getSelectionEnd();
            if (start != end) {
                EditText et = getEditTextByPosition(itemIndex);
                if (et != null) {
                    et.getEditableText().replace(start, end, text);
                }
            }
        }
    }

    public void setTextSize(int complexUnitSp, float fontSize) {
        adapter.setTextSize(complexUnitSp, fontSize);
        adapter.notifyDataSetChanged();
    }

    public Editable getEditableText() {
        int position = getSelectionItemIndex();
        EditText textEt = getEditTextByPosition(position);
        if (textEt != null) {
            return textEt.getEditableText();
        }
        return null;
    }

    public void setMaxWidth(int maxWidth) {
        textList.setLayoutParams(
                new FrameLayout.LayoutParams(maxWidth,
                        FrameLayout.LayoutParams.MATCH_PARENT));
    }

    public void insert(String str) {
        int position = getSelectionItemIndex();
        EditText textEt = getEditTextByPosition(position);
        if (textEt != null) {
            textEt.getText().insert(textEt.getSelectionStart(), str);
        }
    }

    public void requestLayout() {
        textList.requestLayout();
    }

    public ViewGroup.LayoutParams getLayoutParams() {
        return textList.getLayoutParams();
    }

    public void setDocument(Document document) {
        adapter.setDocument(document);
    }

    public void showLineNumber(boolean bShow) {
        adapter.setShowLineNumbers(bShow);
        adapter.notifyDataSetChanged();
    }

    public void setOnTextChangeListener(TextWatcher watcher) {
        adapter.setTextChangeListener(watcher);
    }

    public void refresh() {
        adapter.notifyDataSetChanged();
    }

//    public String getSelectedString() {
//        int position = getSelectionItemIndex();
//        EditText textEt = getEditTextByPosition(position);
//        if (textEt != null) {
//            return textEt.getText().subSequence(
//                    textEt.getSelectionStart(), textEt.getSelectionEnd()).toString();
//        }
//        return "";
//    }

    public TextEditBigListAdapter getAdapter() {
        return adapter;
    }

    public void setTextSelectionListener(SelectionChangedListener listener) {
        adapter.setSelectionChangedListener(listener);
    }

    class MyHandler extends Handler {
        private int itemPosition;
        private int selStart;
        private int selEnd;

        public void setDelayedSelection(int lineIdx, int start, int end) {
            this.itemPosition = lineIdx;
            this.selStart = start;
            this.selEnd = end;
            sendEmptyMessageDelayed(MSG_SELECTION, 100);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SELECTION:
                    EditText et = getEditTextByPosition(itemPosition);
                    if (et != null) {
                        // Need to request focus then set selection
                        et.requestFocus();
                        et.setSelection(selStart, selEnd);
                    }
                    break;
            }
        }
    }
}
