package com.gmail.heagoo.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

    public static void unzip(String zipFile, String zipDir) throws IOException {

        ZipFile zfile = null;
        InputStream is = null;
        OutputStream os = null;

        try {
            zfile = new ZipFile(zipFile);
            Enumeration<?> zList = zfile.entries();
            ZipEntry ze = null;
            byte[] buf = new byte[4096];
            while (zList.hasMoreElements()) {
                ze = (ZipEntry) zList.nextElement();
                if (ze.isDirectory()) {
                    String absPath = zipDir;
                    if (!zipDir.endsWith("/")) {
                        absPath += "/";
                        absPath += ze.getName();
                    }
                    File f = new File(absPath);
                    f.mkdirs();
                    // Log.d("DEBUG", "dir=" + zipDir + ze.getName() + ", ret="
                    // + ret);
                    continue;
                }
                // Log.d("DEBUG", "file=" + ze.getName());
                os = new BufferedOutputStream(new FileOutputStream(getFile(
                        zipDir, ze.getName())));
                is = new BufferedInputStream(zfile.getInputStream(ze));
                int readLen = 0;
                while ((readLen = is.read(buf, 0, 4096)) != -1) {
                    os.write(buf, 0, readLen);
                }
                closeQuietly(is);
                closeQuietly(os);
            }
            zfile.close();
        } finally {
            closeQuietly(is);
            closeQuietly(os);
            try {
                if (null != zfile) {
                    zfile.close();
                }
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    // Extract entryPath to destination path
    // entryPath looks like "assets"
    // If 2 files under "assets" -- "assets/a", "assets/b", then a and b will be
    // copied to dstPath -- dstPath + "/a", dstPath + "/b"
    public static void unzipDirectory(String zipFilePath, String entryPath,
                                      String dstPath) throws IOException {

        ZipFile zfile = null;
        InputStream is = null;
        OutputStream os = null;

        if (!entryPath.endsWith("/")) {
            entryPath = entryPath + "/";
        }

        try {
            zfile = new ZipFile(zipFilePath);
            Enumeration<?> zList = zfile.entries();
            ZipEntry ze = null;
            byte[] buf = new byte[4096];
            while (zList.hasMoreElements()) {
                ze = (ZipEntry) zList.nextElement();
                if (!ze.getName().startsWith(entryPath)) {
                    continue;
                }

                String relativePath = ze.getName()
                        .substring(entryPath.length());
                if (ze.isDirectory()) {
                    File f = new File(dstPath + "/" + relativePath);
                    f.mkdirs();
                    continue;
                }

                os = new BufferedOutputStream(new FileOutputStream(getFile(
                        dstPath, relativePath)));
                is = new BufferedInputStream(zfile.getInputStream(ze));
                int readLen = 0;
                while ((readLen = is.read(buf, 0, 4096)) != -1) {
                    os.write(buf, 0, readLen);
                }
                closeQuietly(is);
                closeQuietly(os);
                is = null;
                os = null;
            }
        } finally {
            closeQuietly(is);
            closeQuietly(os);
            closeQuietly(zfile);
        }
    }

    public static void unzipNoThrow(String zipFile, String zipDir) {

        ZipFile zfile = null;
        InputStream is = null;
        OutputStream os = null;

        try {
            zfile = new ZipFile(zipFile);
            Enumeration<?> zList = zfile.entries();
            ZipEntry ze = null;
            byte[] buf = new byte[4096];
            while (zList.hasMoreElements()) {
                ze = (ZipEntry) zList.nextElement();
                if (ze.isDirectory()) {
                    String absPath = zipDir;
                    if (!zipDir.endsWith("/")) {
                        absPath += "/";
                        absPath += ze.getName();
                    }
                    File f = new File(absPath);
                    f.mkdirs();
                    // Log.d("DEBUG", "dir=" + zipDir + ze.getName() + ", ret="
                    // + ret);
                    continue;
                }
                // Log.d("DEBUG", "file=" + ze.getName());
                os = new BufferedOutputStream(new FileOutputStream(getFile(
                        zipDir, ze.getName())));
                is = new BufferedInputStream(zfile.getInputStream(ze));
                int readLen = 0;
                while ((readLen = is.read(buf, 0, 4096)) != -1) {
                    os.write(buf, 0, readLen);
                }
                closeQuietly(is);
                closeQuietly(os);
            }
            zfile.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(is);
            closeQuietly(os);
            try {
                if (null != zfile) {
                    zfile.close();
                }
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    // Just unzip one file to the target directory
    public static void unzipFileTo(String zipFilePath, String entryName,
                                   String targetPath) throws Exception {
        ZipFile zfile = null;
        InputStream is = null;
        OutputStream os = null;

        try {
            zfile = new ZipFile(zipFilePath);
            ZipEntry ze = zfile.getEntry(entryName);
            byte[] buf = new byte[4096];

            os = new BufferedOutputStream(new FileOutputStream(targetPath));
            is = new BufferedInputStream(zfile.getInputStream(ze));
            int readLen = 0;
            while ((readLen = is.read(buf, 0, 4096)) != -1) {
                os.write(buf, 0, readLen);
            }
        } finally {
            closeQuietly(is);
            closeQuietly(os);
            closeQuietly(zfile);
        }
    }

    public static void zipDir(String dirName, String nameZipFile)
            throws IOException {
        ZipOutputStream zip = null;
        FileOutputStream fW = null;
        fW = new FileOutputStream(nameZipFile);
        zip = new ZipOutputStream(fW);
        addFolderToZip("", dirName, zip);
        zip.close();
        fW.close();
    }

    private static void addFolderToZip(String path, String srcFolder,
                                       ZipOutputStream zip) throws IOException {
        File folder = new File(srcFolder);
        String[] subFiles = folder.list();
        if (subFiles == null || subFiles.length == 0) {
            addFileToZip(path, srcFolder, zip, true);
        } else {
            for (String fileName : subFiles) {
                if (path.equals("")) {
                    addFileToZip(folder.getName(), srcFolder + "/" + fileName,
                            zip, false);
                } else {
                    addFileToZip(path + "/" + folder.getName(), srcFolder + "/"
                            + fileName, zip, false);
                }
            }
        }
    }

    private static void addFileToZip(String path, String srcFile,
                                     ZipOutputStream zip, boolean flag) throws IOException {
        File folder = new File(srcFile);
        if (flag) {
            zip.putNextEntry(new ZipEntry(path + "/" + folder.getName() + "/"));
        } else {
            if (folder.isDirectory()) {
                addFolderToZip(path, srcFile, zip);
            } else {
                byte[] buf = new byte[4096];
                int len;
                FileInputStream in = new FileInputStream(srcFile);
                zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
                while ((len = in.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }
                in.close();
            }
        }
    }

    private static void closeQuietly(InputStream input) {
        if (input != null) {
            try {
                input.close();
            } catch (Throwable e) {
                // Ignore
            }
        }
    }

    private static void closeQuietly(OutputStream output) {
        if (output != null) {
            try {
                output.close();
            } catch (Throwable e) {
                // Ignore
            }
        }
    }

    private static void closeQuietly(ZipFile zfile) {
        if (zfile != null) {
            try {
                zfile.close();
            } catch (Throwable ignored) {
                // Ignore
            }
        }
    }

    private static File getFile(String baseDir, String relativePath) {
        String[] dirs = relativePath.split("/");
        File ret = new File(baseDir);
        if (!ret.exists()) {
            ret.mkdirs();
        }

        if (dirs.length > 1) {
            for (int i = 0; i < dirs.length - 1; i++) {
                ret = new File(ret, dirs[i]);
            }
            if (!ret.exists())
                ret.mkdirs();
        }

        ret = new File(ret, dirs[dirs.length - 1]);
        return ret;
    }

    private static int getSlashNum(String str) {
        int slashNum = 0;
        int startOff = 0;
        while ((startOff = str.indexOf('/', startOff)) != -1) {
            slashNum += 1;
            startOff += 1;
        }
        return slashNum;
    }

    // prefix like "res/", means to list all the entries under res directory
    // prefix like "res/" will return "res/a.png", but will not return "res/raw/a.png"
    public static List<String> listFiles(String zipPath, String prefix) {
        List<String> result = new ArrayList<String>();
        int slashNum = getSlashNum(prefix);

        ZipFile zfile = null;
        try {
            zfile = new ZipFile(zipPath);
            Enumeration<?> zList = zfile.entries();
            ZipEntry ze;
            while (zList.hasMoreElements()) {
                ze = (ZipEntry) zList.nextElement();
                if (!ze.isDirectory()) {
                    String name = ze.getName();
                    if (name.startsWith(prefix) && getSlashNum(name) == slashNum) {
                        result.add(name);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(zfile);
        }

        return result;
    }
}
