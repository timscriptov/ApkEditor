package com.gmail.heagoo.apkeditor.ce.e;

import java.io.IOException;
import java.io.InputStream;

public class MyInputStream {
    int curOffset = 0;
    InputStream is;

    public MyInputStream(InputStream is) {
        this.is = is;
    }

    public void readFully(byte[] buf) throws IOException {
        int read = 0;

        while (read < buf.length) {
            int ret = is.read(buf, read, buf.length - read);
            if (ret == -1) {
                break;
            }
            if (ret > 0) {
                curOffset += ret;
                read += ret;
            }
        }
    }

    public void readFully(byte[] buf, int off, int len) throws IOException {
        int read = 0;

        while (read < len) {
            int ret = is.read(buf, off + read, len - read);
            if (ret == -1) {
                break;
            }
            if (ret > 0) {
                curOffset += ret;
                read += ret;
            }
        }
    }

    public int readBytes(byte[] buf, int off, int len) throws IOException {
        int ret = is.read(buf, off, len);
        if (ret > 0) {
            curOffset += ret;
        }
        return ret;
    }

    public int readShort() throws IOException {
        int low = is.read();
        int high = is.read();
        curOffset += 2;
        return (low & 0xff) | ((high & 0xff) << 8);
    }

    public int readInt() throws IOException {
        byte[] arr = new byte[4];
        is.read(arr);
        curOffset += 4;
        return ManifestEditorNew.getInt(arr, 0);
    }

    public void readIntArray(int[] arr) throws IOException {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = readInt();
        }
    }

    public void skip(int n) throws IOException {
        if (n <= 0) {
            return;
        }

        is.skip(n);
        curOffset += n;
        ManifestEditorNew
                .log("########## skip detected (should not happen) ##########");
    }

    public int getPosition() {
        return curOffset;
    }

    public int readByte() throws IOException {
        int ret = is.read();
        curOffset += 1;
        return ret;
    }
}