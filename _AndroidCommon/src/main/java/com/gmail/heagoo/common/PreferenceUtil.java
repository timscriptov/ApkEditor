package com.gmail.heagoo.common;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceUtil {
    public static boolean getBoolean(Context ctx, String key, boolean defValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean value = sp.getBoolean(key, defValue);
        return value;
    }

    public static void setBoolean(Context ctx, String key, boolean value) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }
}
