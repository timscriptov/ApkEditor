package com.gmail.heagoo.apkeditor;

import com.gmail.heagoo.apkeditor.ce.IApkMaking;
import com.gmail.heagoo.common.ITaskCallback;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ComposeThread extends Thread {
    public abstract void stopRunning();

    public abstract void setExtraMaker(IApkMaking extraMaker);

    public abstract void setModification(boolean strModified, boolean manifestModified,
                                         boolean resFileModified, List<String> modifiedSmaliFolders,
                                         Map<String, String> addedFiles, Map<String, String> replacedFiles,
                                         Set<String> deletedFiles, Map<String, String> fileEntry2ZipEntry,
                                         boolean bSignApk);

    public abstract void setTaskCallback(ITaskCallback callback);
}
