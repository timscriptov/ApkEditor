package com.gmail.heagoo.apkeditor.patch;

public class PathFilter_ExactEntry extends PathFilter {

    private String entryName;

    private int cursor = 0;

    public PathFilter_ExactEntry(IPatchContext ctx, String pathStr) {
        this.entryName = pathStr;
    }

    @Override
    public String getNextEntry() {
        if (cursor == 0) {
            cursor += 1;
            return entryName;
        }
        return null;
    }

    @Override
    public boolean isTarget(String entry) {
        return this.entryName.equals(entry);
    }

    @Override
    public boolean isSmaliNeeded() {
        if (entryName != null) {
            int pos = entryName.indexOf('/');
            if (pos != -1) {
                String firstDir = entryName.substring(0, pos);
                if ("smali".equals(firstDir) || firstDir.startsWith("smali_")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isWildMatch() {
        return false;
    }
}
