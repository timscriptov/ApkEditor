package com.gmail.heagoo.apkeditor;

import android.util.Log;


public class LOGGER {

    private static final String tag = "DEBUG";
    private static final String timeTag = "TIME";

    public static void warning(String str) {
        //Log.w(tag, str);
    }

    public static void info(String str) {
        //Log.i(tag, str);
    }

    public static void info(String str, boolean bForce) {
        if (bForce) {
            Log.i(tag, str);
        }
    }

//	public static void time(String str) {
//		Log.i(timeTag, str);
//	}
//	
//	public static void debug(String str) {
//		Log.d(tag, str);
//	}
}
