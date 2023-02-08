package com.gmail.heagoo.apklib.sign;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class UTF8Writer {

    public static void write(OutputStream os, String content)
            throws UnsupportedEncodingException, IOException {
        os.write(content.getBytes("utf-8"));
    }

    public static boolean writeFile(String filePath, String content) {
        boolean ret = false;

        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            write(fos, content);
            ret = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    public static void main(String[] args) {
        writeFile("D:\\workspace-android\\GiftApp\\assets\\greeting", "Happy Birthday!");
    }
}
