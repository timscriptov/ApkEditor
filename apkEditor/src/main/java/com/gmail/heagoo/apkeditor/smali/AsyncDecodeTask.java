package com.gmail.heagoo.apkeditor.smali;

import android.content.Context;
import android.os.AsyncTask;

import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.RefInvoke;
import com.gmail.heagoo.common.SDCard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AsyncDecodeTask extends AsyncTask<Void, Integer, Boolean> {

    private Context context;
    private IDecodeTaskCallback callback;
    private String decodeRootPath;
    private String apkPath;
    // Record all dex file paths which are extracted from apk file
    private List<String> dexFileList = new ArrayList<String>();
    private String strError;
    private String strWarning;
    public AsyncDecodeTask(Context context, String apkPath,
                           String decodeRootPath, IDecodeTaskCallback callback) {
        this.context = context;
        this.callback = callback;
        this.apkPath = apkPath;
        this.decodeRootPath = decodeRootPath;
    }

    @Override
    protected void onPreExecute() {
        if (callback != null) {
            callback.dexDecodingStarted();
        }
    }

    private void prepareDexFiles() throws Exception {
        String tmpDirectory = SDCard.makeDir(context, "tmp");

        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(apkPath);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".dex") && !name.contains("/")) {
                    unzipDex2File(zipFile, entry, tmpDirectory + name);
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private void prepareMainDex() throws Exception {
        String tmpDirectory = SDCard.makeDir(context, "tmp");

        ZipFile zipFile = null;
        try {
            String name = "classes.dex";
            zipFile = new ZipFile(apkPath);
            ZipEntry entry = zipFile.getEntry(name);
            unzipDex2File(zipFile, entry, tmpDirectory + name);
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private void unzipDex2File(ZipFile zipFile, ZipEntry entry, String filePath)
            throws IOException {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = zipFile.getInputStream(entry);
            out = new FileOutputStream(filePath);
            IOUtils.copy(in, out);
            this.dexFileList.add(filePath);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            doAllJobs();
            return true;
        } catch (Exception e) {
            this.strError = e.getMessage();
            return false;
        }
    }

    public void doAllJobs() throws Exception {
        prepareDexFiles();
        decodeDexFiles();
        removeDexFiles();
    }

    // Only decode the classes.dex
    public void decodeMainDex() throws Exception {
        prepareMainDex();
        decodeDexFiles();
        removeDexFiles();
    }

    private void removeDexFiles() {
        for (String filePath : this.dexFileList) {
            File f = new File(filePath);
            f.delete();
        }
    }

    private void decodeDexFiles() {
        for (String dexFilePath : this.dexFileList) {
            Object decoder = RefInvoke.createInstance(
                    "com.gmail.heagoo.apkeditor.pro.DexDecoder",
                    new Class<?>[]{String.class},
                    new Object[]{dexFilePath});
            String directory = this.decodeRootPath + "/smali";
            // Not the default dex file
            if (!dexFilePath.endsWith("/classes.dex")) {
                int position = dexFilePath.lastIndexOf("/");
                String dexName = dexFilePath.substring(position + 1,
                        dexFilePath.length() - 4);
                directory = this.decodeRootPath + "/smali_" + dexName;
            }
            createDirectoryIfNotExist(directory);
            RefInvoke.invokeMethod("com.gmail.heagoo.apkeditor.pro.DexDecoder",
                    "dex2smali", decoder, new Class<?>[]{String.class},
                    new Object[]{directory});

            if (this.strWarning == null) {
                this.strWarning = (String) RefInvoke.invokeMethod(
                        "com.gmail.heagoo.apkeditor.pro.DexDecoder",
                        "getWarning", decoder, null, null);
            }
        }
    }

    private void createDirectoryIfNotExist(String directory) {
        File dir = new File(directory);
        if (dir.exists()) {
            dir.mkdir();
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (callback != null) {
            if (result) {
                callback.dexDecodingFinished(true, null, this.strWarning);
            } else {
                callback.dexDecodingFinished(false, this.strError, null);
            }
        }
    }

    public static interface IDecodeTaskCallback {
        public void dexDecodingStarted();

        public void dexDecodingFinished(boolean result, String strError,
                                        String strWarning);
    }

}
