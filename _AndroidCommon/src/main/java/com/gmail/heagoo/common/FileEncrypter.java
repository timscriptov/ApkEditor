package com.gmail.heagoo.common;

import java.io.IOException;
import java.io.RandomAccessFile;

public class FileEncrypter {

    public static void encrypt(String filepath) throws IOException {
        byte[] buf = new byte[4096];
        int offset = 0;
        int len;

        RandomAccessFile file = null;

        try {
            file = new RandomAccessFile(filepath, "rw");
            while ((len = file.read(buf)) != -1) {
                for (int i = 0; i < len; ++i) {
                    buf[i] ^= 0x55;
                }
                file.seek(offset);
                file.write(buf, 0, len);
                offset += len;
            }
        } finally {
            if (file != null) {
                file.close();
            }
        }

        file.close();
    }

    public static void decrypt(String filepath) throws IOException {
        encrypt(filepath);
    }
}
