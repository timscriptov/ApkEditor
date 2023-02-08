package com.gmail.heagoo.apkeditor.ce;

import android.content.Context;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.smali.ISmaliAssembleCallback;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

// Currently, we ONLY encode classes.dex
public class DexEncode implements IApkMaking, Serializable,
        ISmaliAssembleCallback {

    private static final long serialVersionUID = 4379505259983741615L;
    private Map<String, String> allReplaces;
    private IDescriptionUpdate updater;
    private int lastShownNum = -10000;

    private String strDexEncode;

    @Override
    public void prepareReplaces(Context ctx, String apkFilePath,
                                Map<String, String> allReplaces, IDescriptionUpdate updater)
            throws Exception {
        this.allReplaces = allReplaces;
        this.updater = updater;
        this.strDexEncode = ctx.getString(R.string.encode_dex_file);

        File fileDir = ctx.getFilesDir();
        String rootDirectory = fileDir.getAbsolutePath();
        String smaliFilePath = rootDirectory + "/decoded/smali";

        assembleSmali(smaliFilePath, rootDirectory + "/.dex");
    }

    // Assemble smali to DEX
    private void assembleSmali(String smaliFilePath, String dexFilePath)
            throws Exception {

        this.allReplaces.put("classes.dex", dexFilePath);

        // Invoke DexEncoder.smali2Dex
        try {
            Class<?> obj_class = Class
                    .forName("com.gmail.heagoo.apkeditor.pro.DexEncoder");
            Method method = obj_class.getMethod("smali2Dex", new Class<?>[]{
                    String.class, String.class, ISmaliAssembleCallback.class});
            method.invoke(null,
                    new Object[]{smaliFilePath, dexFilePath, this});
        } catch (InvocationTargetException e) {
            throw new Exception(e.getTargetException().getMessage());
        } catch (Throwable e) {
            throw new Exception(e.getMessage());
        }

        if (updater != null) {
            updater.updateDescription("");
        }
    }

    // @Override
    // public String getDescription() {
    // return "Encode DEX File";
    // }

    @Override
    public void updateAssembledFiles(int assembledFiles, int totalFiles) {
        if ((assembledFiles - lastShownNum) >= 100
                || assembledFiles == totalFiles) {
            if (updater != null) {
                updater.updateDescription(strDexEncode + ": " + assembledFiles
                        + "/" + totalFiles);
                lastShownNum = assembledFiles;
            }
        }
    }
}
