package com.gmail.heagoo.neweditor;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.HorizontalScrollView;

public class ObHorizontalScrollView extends HorizontalScrollView {
    boolean mFastDirty = true;

    public ObHorizontalScrollView(Context context) {
        super(context);
    }

    public ObHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ObHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        ObEditText child2 = (ObEditText) child;
        child2.fastMeasure(getChildMeasureSpec(parentWidthMeasureSpec, (lp.leftMargin + lp.rightMargin) + widthUsed, lp.width), getChildMeasureSpec(parentHeightMeasureSpec, (lp.topMargin + lp.bottomMargin) + heightUsed, lp.height));
    }
}