package com.gmail.heagoo.apkeditor.patch;

public abstract class PathFilter {

    // Return next entry path
    public abstract String getNextEntry();

    // Is the target path or not
    public abstract boolean isTarget(String entryPath);

    // Inside smali directory or not
    public abstract boolean isSmaliNeeded();

    // Wildchar or not
    public abstract boolean isWildMatch();
}
