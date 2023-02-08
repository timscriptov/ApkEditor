package com.gmail.heagoo.apkeditor.ce.e;

import android.content.Context;

import com.gmail.heagoo.apkeditor.ce.IApkMaking;
import com.gmail.heagoo.apkeditor.ce.IDescriptionUpdate;
import com.gmail.heagoo.common.SDCard;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class RefactorLayout implements IApkMaking, Serializable {
    private String oldStarts;
    private String newStarts;

    public RefactorLayout(String oldName, String newName) {
        this.oldStarts = oldName;
        this.newStarts = newName;
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

    @Override
    public void prepareReplaces(Context ctx, String apkFilePath, Map<String, String> allReplaces, IDescriptionUpdate updater) throws Exception {
        ZipFile zfile = null;
        String outFolder = SDCard.makeWorkingDir(ctx);

        try {
            zfile = new ZipFile(apkFilePath);
            Enumeration<?> zList = zfile.entries();
            ZipEntry ze;
            while (zList.hasMoreElements()) {
                ze = (ZipEntry) zList.nextElement();
                if (!ze.isDirectory()) {
                    String name = ze.getName();
                    if (name.endsWith(".xml") && name.startsWith("res/layout")) {
                        InputStream input = zfile.getInputStream(ze);
                        AxmlStringEditor editor = new AxmlStringEditor(input, outFolder);
                        String newFile = editor.modifyStringStartWith(oldStarts, newStarts);
                        if (newFile != null) {
                            //Log.e("DEBUG", name + " edited.");
                            allReplaces.put(name, newFile);
                        }
                        input.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(zfile);
        }
    }
}
