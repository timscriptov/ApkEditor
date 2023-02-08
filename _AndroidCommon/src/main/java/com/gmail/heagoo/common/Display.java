package com.gmail.heagoo.common;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;

public class Display {

    public static int screenWidth = 0;
    public static int screenHeight = 0;

    public static int getWidth(Activity activity) {
        if (screenWidth <= 0) {
            init(activity);
        }

        return screenWidth;
    }

    public static int getHeight(Activity activity) {
        if (screenHeight <= 0) {
            init(activity);
        }

        return screenHeight;
    }

    private static void init(Activity activity) {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
    }

    public static int dip2px(Context context, float dipValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static int getHeightDip(Activity activity) {
        if (screenHeight <= 0) {
            init(activity);
        }
        float scale = activity.getResources().getDisplayMetrics().density;
        return (int) (screenHeight / scale);
    }

    public static int getWidthDip(Activity activity) {
        if (screenWidth <= 0) {
            init(activity);
        }
        float scale = activity.getResources().getDisplayMetrics().density;
        return (int) (screenWidth / scale);
    }
}
