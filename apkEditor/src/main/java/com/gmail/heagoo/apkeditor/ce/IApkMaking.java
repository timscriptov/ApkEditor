package com.gmail.heagoo.apkeditor.ce;

import android.content.Context;

import java.util.Map;

public interface IApkMaking {

    public void prepareReplaces(Context ctx, String apkFilePath,
                                Map<String, String> allReplaces, IDescriptionUpdate updater)
            throws Exception;
}
