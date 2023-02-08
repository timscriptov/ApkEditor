package com.gmail.heagoo.apkeditor;

public interface CodeEditInterface {

    // IN: apkFilePath
    // The modified dex file is saved into dexFilePath
    public void editDexFile(String apkFilePath, String dexFilePath) throws Exception;
}
