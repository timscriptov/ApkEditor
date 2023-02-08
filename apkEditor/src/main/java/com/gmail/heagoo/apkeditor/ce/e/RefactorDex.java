package com.gmail.heagoo.apkeditor.ce.e;


import android.content.Context;

import com.gmail.heagoo.apkeditor.ce.IApkMaking;
import com.gmail.heagoo.apkeditor.ce.IDescriptionUpdate;
import com.gmail.heagoo.apkeditor.dex.DexStringEditor;
import com.gmail.heagoo.common.RandomUtil;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.common.ZipUtil;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class RefactorDex implements IApkMaking, Serializable {
    private String oldPath;
    private String newPath;

    public RefactorDex(String oldPath, String newPath) {
        this.oldPath = oldPath;
        this.newPath = newPath;
    }

    @Override
    public void prepareReplaces(Context ctx, String apkFilePath,
                                Map<String, String> allReplaces,
                                IDescriptionUpdate updater) throws Exception {
        List<String> entryLsit = ZipUtil.listFiles(apkFilePath, "");
        for (String entry : entryLsit) {
            if (entry.endsWith(".dex")) {
                String savePath = SDCard.makeWorkingDir(ctx) + RandomUtil.getRandomString(6) + ".dex";
                DexStringEditor editor = new DexStringEditor(apkFilePath, entry);
                if (editor.refactorPackageName(oldPath, newPath, savePath)) {
                    allReplaces.put(entry, savePath);
                }
            }
        }
    }
}
