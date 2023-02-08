package com.gmail.heagoo.sqliteutil;

import android.os.Environment;

public class SDCard {

    public static boolean exist() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    public static String getRootDirectory() {
        return Environment.getExternalStorageDirectory().getPath();
    }
}
