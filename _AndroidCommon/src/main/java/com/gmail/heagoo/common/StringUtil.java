package com.gmail.heagoo.common;

public class StringUtil {

    public static String join(String join, String[] strAry) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strAry.length; i++) {
            if (i == (strAry.length - 1)) {
                sb.append(strAry[i]);
            } else {
                sb.append(strAry[i]).append(join);
            }
        }

        return sb.toString();
    }
}
