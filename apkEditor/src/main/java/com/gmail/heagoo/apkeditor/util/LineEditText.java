package com.gmail.heagoo.apkeditor.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.util.AttributeSet;
import android.widget.EditText;

public class LineEditText extends EditText {
    private Rect mRect;
    private Paint mPaint;

    private float scale;
    private int left;
    private int right;
    private int bottom;
    private int top;

    public LineEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.scale = context.getResources().getDisplayMetrics().density;
        this.left = this.getPaddingLeft();
        this.right = this.getPaddingRight();
        this.bottom = this.getPaddingBottom();
        this.top = this.getPaddingTop();

        mRect = new Rect();
        mPaint = new Paint();
        // define the style of line
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        // define the color of line
        mPaint.setColor(0xff787878);
        mPaint.setTextSize(dip2px(context, 14));

        this.left = (int) getWidth(4);
        this.setPadding((int) (left + 2 * scale), top, right, bottom);
    }

    public int dip2px(Context context, float dpValue) {
        return (int) (dpValue * scale + 0.5f);
    }

    private float getWidth(int digitNum) {
        String testedStr = "445";
        if (digitNum >= 4) {
            testedStr = "4455";
        }
        return mPaint.measureText(testedStr);
    }

    @SuppressLint("DefaultLocale")
    private String getLineDescription(int index) {
        if (index < 1000) {
            return String.format("%03d", index);
        } else {
            return String.format("%4d", index);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // int height = getHeight();
        int lHeight = getLineHeight();
        // the number of line
        // int count = height / lHeight;
        // if (getLineCount() > count) {
        // // for long text with scrolling
        // count = getLineCount();
        // }
        int count = getLineCount();
        Rect r = mRect;
        Paint paint = mPaint;

        String content = this.getText().toString();
        int contentLen = content.length();
        Layout layout = this.getLayout();

        // draw line numbers
        int lastLineIndex = -1;
        int lineIndex = 1;
        int baseline = 0;
        for (int i = 0; i < count; i++) {
            int endPos = layout.getLineEnd(i);

            if (lineIndex != lastLineIndex) {
                String strLine = getLineDescription(lineIndex);
                baseline = getLineBounds(i, r);
                canvas.drawText(strLine, r.left - this.getPaddingLeft(),
                        baseline + 1, paint);
                lastLineIndex = lineIndex;
            }

            // Current line ends with enter '\n'
            if (contentLen >= endPos && content.charAt(endPos - 1) == '\n') {
                lineIndex += 1;
            }
        }
        canvas.drawLine(left, top + lHeight / 3, left, baseline + 1, paint);

        super.onDraw(canvas);
    }
}
