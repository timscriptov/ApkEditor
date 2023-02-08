package com.gmail.heagoo.apkeditor.dex;

/**
 * Created by phe3 on 4/18/2017.
 */

public class MyBuffer {
    public byte[] buf;
    public int offset;
    public int len;

    public MyBuffer() {
    }

    public MyBuffer(byte[] buf, int off, int len) {
        this.buf = buf;
        this.offset = off;
        this.len = len;
    }
}
