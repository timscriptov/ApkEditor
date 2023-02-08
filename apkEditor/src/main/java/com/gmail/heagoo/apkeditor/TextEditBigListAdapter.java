package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.gmail.heagoo.SelectionChangedListener;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.ui.EditTextRememberCursor;
import com.gmail.heagoo.apkeditor.ui.LayoutObListView;
import com.gmail.heagoo.neweditor.Document;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


public class TextEditBigListAdapter extends BaseAdapter {
    private Context ctx;
    private List<String> lines;
    private WeakReference<LayoutObListView> listRef;

    // Font appearance
    private int complexUnitSp;
    private float fontSize;
    private int textColor = -1; // foreground color
    private boolean bShowLineNumbers = true; // show line # or not

    // For text highlight
    private Document document;

    // To track the text change, so that can update save button
    private TextWatcher textChangeListener;

    // Text selection change listener
    private WeakReference<SelectionChangedListener> selectionListenerRef;

    // Deal with messages
    private MyHandler handler = new MyHandler();

    public TextEditBigListAdapter(Context ctx, LayoutObListView list) {
        this.ctx = ctx;
        this.lines = new ArrayList<>();
        this.listRef = new WeakReference<>(list);
    }

    public void setSelectionChangedListener(SelectionChangedListener selectionChangedListener) {
        this.selectionListenerRef = new WeakReference<>(selectionChangedListener);
    }

    ;

    @Override
    public int getCount() {
        return lines.size();
    }

    @Override
    public Object getItem(int position) {
        return lines.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private void reclaimFocus(View v, long timestamp) {
        if (timestamp == -1)
            return;
        if ((System.currentTimeMillis() - timestamp) < 250)
            v.requestFocus();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(ctx).inflate(R.layout.item_lined_text, null);

            viewHolder = new ViewHolder();
            viewHolder.lineTv = (TextView) convertView.findViewById(R.id.line_num);
            viewHolder.et = (EditTextRememberCursor) convertView.findViewById(R.id.text);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Remove old watcher
        Object tag = viewHolder.et.getTag();
        if (tag != null) {
            viewHolder.et.removeTextChangedListener((CustomTextWatcher) tag);
        }

        // Add new watcher
        CustomTextWatcher watcher = new CustomTextWatcher(position);
        viewHolder.et.addTextChangedListener(watcher);
        viewHolder.et.setTag(watcher);

        viewHolder.et.setTextSelectionListener(selectionListenerRef);

        // Set content
        String strNum = getNumString(position + 1);
        if (bShowLineNumbers) {
            viewHolder.lineTv.setVisibility(View.VISIBLE);
            viewHolder.lineTv.setText(strNum);
            viewHolder.lineTv.setTextColor(textColor);
        } else {
            viewHolder.lineTv.setVisibility(View.GONE);
        }
        viewHolder.et.setTextColor(textColor);
        viewHolder.et.setText(lines.get(position));
        if (fontSize > 0) {
            viewHolder.lineTv.setTextSize(complexUnitSp, fontSize);
            viewHolder.et.setTextSize(complexUnitSp, fontSize);
        }
        if (document != null) {
            document.syntaxHighlight(viewHolder.et);
        }

        // Use following code to restore the cursor position
        // Note: when delete some lines, the relative position is changed, cannot directly restore
        viewHolder.et.setTag(R.id.text, position);
        viewHolder.et.setParent(ctx, listRef);
        viewHolder.et.selectionClickable();

        LayoutObListView lv = listRef.get();
        if (lv != null) {
            if (lv.getSelPosition() == position) {
                //Log.e("DEBUG", "restore selection for position=" + position + "," + lv.getSelStart() + "," + lv.getSelEnd());
                viewHolder.et.setSelection(lv.getSelStart(), lv.getSelEnd());
                viewHolder.et.requestFocus();
                handler.setTargetView(viewHolder.et);
                handler.removeMessages(0);
                handler.sendEmptyMessageDelayed(0, 100);
            }
        }

        return convertView;
    }

    private int getNumberDigits(int lines) {
        int nd = 1;
        while (lines >= 10) {
            lines /= 10;
            nd++;
        }
        return nd;
    }

    private String getPaddingString(int digits) {
        switch (digits) {
            case 1:
                return "0";
            case 2:
                return "00";
            case 3:
                return "000";
            case 4:
                return "0000";
            case 5:
                return "00000";
            case 6:
                return "000000";
            default:
                return "0000000";
        }
    }

    private String getNumString(int num) {
        int digits = getNumberDigits(lines.size());
        int cur = getNumberDigits(num);
        if (cur < digits) {
            return getPaddingString(digits - cur) + num;
        } else {
            return "" + num;
        }
    }

    public void setTextSize(int complexUnitSp, float fontSize) {
        this.complexUnitSp = complexUnitSp;
        this.fontSize = fontSize;
    }

    // Text in all lines
    public CharSequence getText() {
        StringBuilder sb = new StringBuilder();
        if (lines.size() > 0) {
            sb.append(lines.get(0));
            for (int i = 1; i < lines.size(); ++i) {
                sb.append("\n");
                sb.append(lines.get(i));
            }
        }
        return sb.toString();
    }

    // Set the new data
    public void setText(List<String> text) {
        lines.clear();
        for (String line : text) {
            lines.add(line);
        }

        // Reset the selection
        listRef.get().resetSelection();
    }

    public List<String> getTextLines() {
        return lines;
    }

    // Document is used to do syntex highlight
    public void setDocument(Document document) {
        this.document = document;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void setShowLineNumbers(boolean bShow) {
        this.bShowLineNumbers = bShow;
    }

    public void setTextChangeListener(TextWatcher textChangeListener) {
        this.textChangeListener = textChangeListener;
    }

    static class SelectionRecord {
        long time = 0;
        int position = -1;
        int selStart = 0;
        int selEnd = 0;
    }

    static class ViewHolder {
        TextView lineTv;
        EditTextRememberCursor et;
    }

    class MyHandler extends Handler {
        View target;

        public void setTargetView(EditText target) {
            this.target = target;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    target.requestFocus();
                    break;
                default:
                    break;
            }
        }
    }

    private class CustomTextWatcher implements TextWatcher {
        private int position;
        private boolean isSetInitialText;

        public CustomTextWatcher(int position) {
            this.position = position;
            isSetInitialText = true;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        public void afterTextChanged(Editable s) {
            if (isSetInitialText) {
                isSetInitialText = false;
            } else {
                if (position < lines.size()) {
                    lines.set(position, s.toString());
                }
                // Notify text change listener
                if (textChangeListener != null) {
                    textChangeListener.afterTextChanged(s);
                }
            }
        }
    }
}
