package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class GlobalConfig {

    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK_DEFAULT = 1;
    public static final int THEME_DARK_RUSSIAN = 2;
    private static GlobalConfig config;
    private int themeId;
    private boolean isFullScreen;

    private GlobalConfig(Context ctx) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(ctx);
        try {
            String str = sp.getString("AppTheme", "0");
            int id = Integer.valueOf(str);
            if (id >= 0 && id < 3) {
                themeId = id;
            } else {
                themeId = 0;
            }
        } catch (Exception e) {
            this.themeId = 0;
        }

        this.isFullScreen = sp.getBoolean("FullScreen", false);
    }

    public static GlobalConfig instance(Context ctx) {
        if (config == null) {
            config = new GlobalConfig(ctx);
        }

        return config;
    }

    public boolean isDarkTheme() {
        return themeId != 0;
    }

    public int getThemeId() {
        return themeId;
    }

    public boolean isFullScreen() {
        return isFullScreen;
    }

    public void updateThemeId(int themeId) {
        this.themeId = themeId;
    }

    public void setFullScreen(Context ctx, boolean isFullScreen) {
        if (isFullScreen != this.isFullScreen) {
            this.isFullScreen = isFullScreen;
            SharedPreferences sp = PreferenceManager
                    .getDefaultSharedPreferences(ctx);
            Editor editor = sp.edit();
            editor.putBoolean("FullScreen", isFullScreen);
            editor.commit();
        }
    }
}
