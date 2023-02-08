package com.gmail.heagoo.apkeditor.util;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.gmail.heagoo.common.Display;

import java.lang.ref.WeakReference;

public class AndroidBug5497Workaround {


    private WeakReference<Activity> actRef;
    private int screenHeight;
    private int screenWidth;
    private View mChildOfContent;
    private int usableHeightPrevious;
    private FrameLayout.LayoutParams frameLayoutParams;
    private AndroidBug5497Workaround(Activity activity) {
        actRef = new WeakReference<>(activity);
        screenHeight = Display.getHeight(activity);
        screenWidth = Display.getWidth(activity);

        FrameLayout content = (FrameLayout) activity.findViewById(android.R.id.content);
        mChildOfContent = content.getChildAt(0);
        mChildOfContent.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                //possiblyResizeChildOfContent();
                switchScreenMode();
            }
        });
        frameLayoutParams = (FrameLayout.LayoutParams) mChildOfContent.getLayoutParams();
    }

    public static void assistActivity(Activity activity) {
        new AndroidBug5497Workaround(activity);
    }

    private void switchScreenMode() {
        int usableHeightNow = computeUsableHeight();
        if (usableHeightNow == usableHeightPrevious) {
            return;
        }

        int orientation = actRef.get().getResources().getConfiguration().orientation;
        int totalHeight = (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ?
                screenHeight : screenWidth);
        if (usableHeightNow >= totalHeight * 85 / 100) {
            //Log.e("DEBUG", "Turn on full screen");
            // Turn on full screen
            Window w = actRef.get().getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            w.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        } else {
            //Log.e("DEBUG", "Turn off full screen, usableHeightNow=" + usableHeightNow + ", screenHeight=" + screenHeight + ", screenWidth=" + screenWidth);
            // Turn off full screen mode
            Window w = actRef.get().getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            w.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        usableHeightPrevious = usableHeightNow;
    }

    private void possiblyResizeChildOfContent() {
        int usableHeightNow = computeUsableHeight();
        if (usableHeightNow != usableHeightPrevious) {
            int usableHeightSansKeyboard = mChildOfContent.getRootView().getHeight();
            int heightDifference = usableHeightSansKeyboard - usableHeightNow;
            if (heightDifference > (usableHeightSansKeyboard / 4)) {
                // keyboard probably just became visible
                frameLayoutParams.height = usableHeightSansKeyboard - heightDifference;
            } else {
                // keyboard probably just became hidden
                frameLayoutParams.height = usableHeightSansKeyboard;
            }
            mChildOfContent.requestLayout();
            usableHeightPrevious = usableHeightNow;
        }
    }

    private int computeUsableHeight() {
        Rect r = new Rect();
        mChildOfContent.getWindowVisibleDisplayFrame(r);
        return (r.bottom - r.top);
    }
}