package com.gmail.heagoo.apkeditor;

/**
 * Created by phe3 on 1/31/2017.
 */

public class SmaliMethodInfo {
    public String methodDesc;
    public int lineIndex; // index start at 0

    public SmaliMethodInfo(int lineIndex, String method) {
        this.lineIndex = lineIndex;
        this.methodDesc = method;
    }
}
