package com.gmail.heagoo.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class FileUtil {

    private static final char[] notAllowedChars = new char[]{'\"', '/', '\\', ':', '*', '?', '<', '>', '|'};

    public static void writeToFile(String fileName, byte[] data) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileName);
            fos.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static void writeToFile(String fileName, List<String> lines) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(fileName));
            for (String line : lines) {
                bw.write(line);
                bw.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static void copyFile(String srcFilePath, String dstFilePath)
            throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(srcFilePath);
            fos = new FileOutputStream(dstFilePath);
            IOUtils.copy(fis, fos);
        } finally {
            if (fis != null) {
                fis.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    public static void copyFile(File srcFile, File dstFile)
            throws IOException {
        BufferedInputStream fis = null;
        BufferedOutputStream fos = null;
        try {
            fis = new BufferedInputStream(new FileInputStream(srcFile));
            fos = new BufferedOutputStream(new FileOutputStream(dstFile));
            IOUtils.copy(fis, fos);
        } finally {
            if (fis != null) {
                fis.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    public static void deleteAll(File f) throws IOException {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File c : files) {
                    deleteAll(c);
                }
            }
        }
//		if (!f.delete())
//			throw new FileNotFoundException("Failed to delete file: " + f);
        f.delete();
    }

    public static void copyAll(File srcFolder, File dstFolder) throws IOException {
        if (dstFolder.exists()) {
            dstFolder.mkdirs();
        }

        File[] files = srcFolder.listFiles();
        if (files != null)
            for (File f : files) {
                if (f.isFile()) { // Copy a single file
                    copyFile(f, new File(dstFolder, f.getName()));
                } else { // Copy the sub folder
                    copyAll(f, new File(dstFolder, f.getName()));
                }
            }
    }

    private static boolean isNotAllowed(char c) {
        for (char nc : notAllowedChars) {
            if (c == nc) {
                return true;
            }
        }
        return false;
    }

    public static String reviseFileName(String filename) {
        if (filename == null) {
            return "";
        }

        boolean modified = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filename.length(); ++i) {
            char c = filename.charAt(i);
            if (isNotAllowed(c)) {
                sb.append('_');
                modified = true;
            } else {
                sb.append(c);
            }
        }

        if (modified) {
            return sb.toString();
        } else {
            return filename;
        }
    }

    public static long recursiveModifiedTime(File[] files) {
        long modified = 0;
        for (int i = 0; i < files.length; i++) {
            long submodified = recursiveModifiedTime(files[i]);
            if (submodified > modified) {
                modified = submodified;
            }
        }
        return modified;
    }

    public static long recursiveModifiedTime(File file) {
        long modified = file.lastModified();
        if (file.isDirectory()) {
            File[] subfiles = file.listFiles();
            for (int i = 0; i < subfiles.length; i++) {
                long submodified = recursiveModifiedTime(subfiles[i]);
                if (submodified > modified) {
                    modified = submodified;
                }
            }
        }
        return modified;
    }
}
