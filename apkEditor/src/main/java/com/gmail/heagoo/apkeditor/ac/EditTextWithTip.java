package com.gmail.heagoo.apkeditor.ac;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

public class EditTextWithTip extends AutoCompleteTextView {

    public EditTextWithTip(Context context) {
        super(context);
    }

    public EditTextWithTip(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextWithTip(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

//    @SuppressLint("NewApi")
//    public EditTextWithTip(Context context, AttributeSet attrs,
//            int defStyleAttr, int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//    }

    @Override
    public boolean enoughToFilter() {
        return true;
    }
}
