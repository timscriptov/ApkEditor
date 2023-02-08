package com.gmail.heagoo.neweditor;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

// ObservableScrollView
public class ObScrollView extends ScrollView {
    private ScrollViewListener scrollViewListener = null;

    public ObScrollView(Context context) {
        super(context);
    }

    public ObScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ObScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setScrollViewListener(ScrollViewListener scrollViewListener) {
        this.scrollViewListener = scrollViewListener;
    }

    protected void onScrollChanged(int x, int y, int oldx, int oldy) {
        super.onScrollChanged(x, y, oldx, oldy);
        if (this.scrollViewListener != null) {
            this.scrollViewListener.onScrollChanged(this, x, y, oldx, oldy);
        }
    }
}