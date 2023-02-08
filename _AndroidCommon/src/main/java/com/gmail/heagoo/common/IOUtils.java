package com.gmail.heagoo.common;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.ZipFile;

public class IOUtils {

    public static void copy(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = new byte[4096];
        int count = 0;
        while ((count = in.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }
    }

    // Copy all files inside srcDir to targetDir
    public static void copy(File targetDir, File srcDir) {
        File[] files = srcDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    FileInputStream fis = null;
                    FileOutputStream fos = null;
                    try {
                        fis = new FileInputStream(file);
                        fos = new FileOutputStream(new File(targetDir, file.getName()));
                        IOUtils.copy(fis, fos);
                    } catch (Exception ignored) {
                    } finally {
                        IOUtils.closeQuietly(fis);
                        IOUtils.closeQuietly(fos);
                    }
                } else if (file.isDirectory()) {
                    File subDir = new File(targetDir, file.getName());
                    subDir.mkdir();
                    copy(subDir, file);
                }
            }
        }
    }

    public static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(in, output);
        return output.toByteArray();
    }

    public static void writeZero(OutputStream out, int size) throws IOException {
        int blocks = size / 1024;
        int remain = size % 1024;

        byte[] buffer = new byte[1024];
        for (int i = 0; i < 1024; i++) {
            buffer[i] = 0;
        }

        for (int i = 0; i < blocks; i++) {
            out.write(buffer);
        }

        if (remain > 0) {
            out.write(buffer, 0, remain);
        }
    }

    public static void readFully(InputStream is, byte[] buf) throws IOException {
        int read = 0;
        while (read < buf.length) {
            int ret = is.read(buf, read, buf.length - read);
            if (ret != -1) {
                read += ret;
            } else {
                break;
            }
        }
    }

    public static boolean writeObjectToFile(String filePath, Object obj) {
        File file = new File(filePath);
        ObjectOutputStream objOut = null;
        try {
            objOut = new ObjectOutputStream(new FileOutputStream(file));
            objOut.writeObject(obj);
            objOut.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(objOut);
        }
        return false;
    }

    public static Object readObjectFromFile(String filePath) {
        if (filePath == null) {
            return null;
        }

        Object result = null;
        File file = new File(filePath);
        ObjectInputStream objIn = null;
        try {
            objIn = new ObjectInputStream(new FileInputStream(file));
            result = objIn.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuietly(objIn);
        }
        return result;
    }

    public static String readString(InputStream input) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(input));
        String line = br.readLine();
        while (line != null) {
            sb.append(line);
            sb.append("\n");
            line = br.readLine();
        }
        return sb.toString();

    }

    public static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static void closeQuietly(ZipFile zfile) {
        if (zfile != null) {
            try {
                zfile.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static void writeToFile(String targetFile, String content) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(targetFile);
            fos.write(content.getBytes());
        } finally {
            closeQuietly(fos);
        }
    }
}
