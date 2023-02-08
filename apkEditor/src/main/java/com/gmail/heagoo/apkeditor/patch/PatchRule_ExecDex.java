package com.gmail.heagoo.apkeditor.patch;

import android.content.Context;

import com.gmail.heagoo.apkeditor.ApkInfoActivity;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.SDCard;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;


class PatchRule_ExecDex extends PatchRule {

    private static final String strEnd = "[/EXECUTE_DEX]";
    private static final String SCRIPT = "SCRIPT:";
    private static final String INTERFACE_VERSION = "INTERFACE_VERSION:";
    private static final String SMALI_NEEDED = "SMALI_NEEDED:";
    private static final String MAIN_CLASS = "MAIN_CLASS:";
    private static final String ENTRANCE = "ENTRANCE:";
    private static final String PARAM = "PARAM:";

    private String scriptName;
    private String mainClass;
    private String entranceFunc;
    private String param;
    private boolean smaliNeeded = false;
    private int ifVersion = 1;

    private List<String> keywords;

    PatchRule_ExecDex() {
        keywords = new ArrayList<>();
        keywords.add(strEnd);
        keywords.add(SCRIPT);
        keywords.add(INTERFACE_VERSION);
        keywords.add(SMALI_NEEDED);
        keywords.add(MAIN_CLASS);
        keywords.add(ENTRANCE);
        keywords.add(PARAM);
    }

    @Override
    public void parseFrom(LinedReader br, IPatchContext logger) throws IOException {
        super.startLine = br.getCurrentLine();

        String line = br.readLine();
        while (line != null) {
            line = line.trim();
            if (strEnd.equals(line)) {
                break;
            }
            if (super.parseAsKeyword(line, br)) {
                line = br.readLine();
                continue;
            } else if (SCRIPT.equals(line)) {
                String next = br.readLine();
                this.scriptName = next.trim();
            } else if (MAIN_CLASS.equals(line)) {
                String next = br.readLine();
                this.mainClass = next.trim();
            } else if (ENTRANCE.equals(line)) {
                String next = br.readLine();
                this.entranceFunc = next.trim();
            } else if (PARAM.equals(line)) {
                List<String> lines = new ArrayList<>();
                line = readMultiLines(br, lines, true, keywords);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < lines.size(); ++i) {
                    sb.append(lines.get(i));
                    if (i != lines.size() - 1) {
                        sb.append('\n');
                    }
                }
                this.param = sb.toString();
                continue;
            } else if (SMALI_NEEDED.equals(line)) {
                String next = br.readLine();
                this.smaliNeeded = Boolean.valueOf(next.trim());
            } else if (INTERFACE_VERSION.equals(line)) {
                String next = br.readLine();
                this.ifVersion = Integer.valueOf(next.trim());
            } else {
                logger.error(R.string.patch_error_cannot_parse, br.getCurrentLine(), line);
            }
            line = br.readLine();
        }
    }

    @Override
    public String executeRule(ApkInfoActivity activity, ZipFile patchZip, IPatchContext logger) {
        if (ifVersion != 1) {
            logger.error(R.string.general_error, "Unsupported interface version: " + ifVersion);
            return null;
        }

        // Internal storage where the DexClassLoader writes the optimized dex file to.
        final File optimizedDexOutputPath = activity.getDir("outdex", Context.MODE_PRIVATE);

        ZipEntry ze = patchZip.getEntry(scriptName);
        if (ze == null) {
            logger.error(R.string.general_error, "Cannot find '" + scriptName + "' inside the patch.");
            return null;
        }

        // Extract to sdcard
        InputStream is = null;
        OutputStream os = null;
        String dexPath = null;
        try {
            dexPath = SDCard.makeWorkingDir(activity) + "script.dex";
            os = new BufferedOutputStream(new FileOutputStream(dexPath));
            is = new BufferedInputStream(patchZip.getInputStream(ze));
            IOUtils.copy(is, os);
        } catch (Exception e) {
            logger.error(R.string.general_error, "Cannot extract '" + scriptName + "' to SD card.");
            return null;
        } finally {
            closeQuietly(is);
            closeQuietly(os);
        }

        // Initialize the class loader with the secondary dex file.
        DexClassLoader cl = new DexClassLoader(dexPath,
                optimizedDexOutputPath.getAbsolutePath(),
                null,
                activity.getClassLoader());

        try {
            // Load the library class from the class loader.
            Class<?> entryClass = cl.loadClass(mainClass);

            // Cast the return object to the library interface so that the
            // caller can directly invoke methods in the interface.
            // Alternatively, the caller can invoke methods through reflection,
            // which is more verbose and slow.
            Object obj = entryClass.newInstance();
            Method method = entryClass.getMethod(entranceFunc,
                    String.class, String.class, String.class, String.class);

            // apkPath, patchPath, decodePath, param
            String apkPath = activity.getApkPath();
            String patchPath = patchZip.getName();
            String decodedRootPath = activity.getDecodeRootPath();
            method.invoke(obj, apkPath, patchPath, decodedRootPath, this.param);

        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                InvocationTargetException ite = (InvocationTargetException) e;
                Throwable t = ite.getTargetException();
                if (t != null) {
                    logger.error(R.string.general_error, getStackTrace(t));
                } else {
                    logger.error(R.string.general_error, getStackTrace(e));
                }
            } else {
                logger.error(R.string.general_error, getStackTrace(e));
            }
        }

        return null;
    }

    private String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString(); // stack trace as a string
    }

    @Override
    public boolean isValid(IPatchContext logger) {
        if (this.scriptName == null) {
            logger.error(R.string.patch_error_no_script_name);
            return false;
        }

        if (this.mainClass == null) {
            logger.error(R.string.patch_error_no_main_class);
            return false;
        }

        if (this.entranceFunc == null) {
            logger.error(R.string.patch_error_no_entrance_func);
            return false;
        }

        return true;
    }

    @Override
    public boolean isSmaliNeeded() {
        return smaliNeeded;
    }
}
