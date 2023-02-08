package com.gmail.heagoo.folderlist.util;

public class StorageUtil {

    private static long oneK = 1024l;
    private static long oneM = 1024l * 1024;
    private static long oneG = 1024l * 1024 * 1024;

    public static String getSizeSescription(long size) {
        String str = null;
        // < 1M
        if (size < oneM) {
            if (size < 1024) {
                str = "" + size + " B";
            } else {
                str = String.format("%.2f K", 1.0f * size / oneK);
            }
        } else if (size < oneG) {
            str = String.format("%.2f M", 1.0f * size / oneM);
        } else {
            str = String.format("%.2f G", 1.0f * size / oneG);
        }

        return str;
    }
}
