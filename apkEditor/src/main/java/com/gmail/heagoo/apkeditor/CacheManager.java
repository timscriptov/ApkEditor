package com.gmail.heagoo.apkeditor;

import brut.androlib.res.data.ResPackage;

public class CacheManager {

    private static CacheManager inst = new CacheManager();

    private ResPackage frameworkPackage;

    private CacheManager() {

    }

    public static CacheManager instance() {
        return inst;
    }

    public ResPackage getFrameworkPackage() {
        return frameworkPackage;
    }

    public void setFrameworkPackage(ResPackage rp) {
        this.frameworkPackage = rp;
    }
}
