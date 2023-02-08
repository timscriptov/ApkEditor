package com.gmail.heagoo.apkeditor.patch;

import com.gmail.heagoo.apkeditor.ApkInfoActivity;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public interface IBeforeAddFile {

    // Return true means already consumed
    public boolean consumeAddedFile(ApkInfoActivity activity, ZipFile zfile,
                                    ZipEntry entry) throws Exception;
}
