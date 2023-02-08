package com.gmail.heagoo.apkeditor.patch;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PathFilter_Wildcard extends PathFilter {

    private IPatchContext ctx;
    private String wildPathStr;
    private String regexPath;
    private String decodedRootPath;

    private boolean initialized = false;

    // Each element in folderList is the relative path
    private List<String> folderList = new LinkedList<>();
    private List<String> fileList = new ArrayList<>();
    private int fileCursor = 0; // Index inside fileList

    public PathFilter_Wildcard(IPatchContext ctx, String pathStr) {
        this.ctx = ctx;
        this.wildPathStr = pathStr;
        this.regexPath = "^" + pathStr.replace("*", ".*") + "$";

        this.decodedRootPath = ctx.getDecodeRootPath();
    }

    private void init() {
        File rootFile = new File(decodedRootPath);
        File[] subFiles = rootFile.listFiles();
        if (subFiles != null) {
            for (File f : subFiles) {
                if (f.isDirectory()) {
                    folderList.add(f.getName());
                } else {
                    String relativePath = f.getName();
                    if (isTarget(relativePath)) {
                        fileList.add(relativePath);
                    }
                }
            }
        }

        initialized = true;
    }

    @Override
    public String getNextEntry() {
        if (!initialized) {
            init();
        }

        if (fileCursor < fileList.size()) {
            String path = fileList.get(fileCursor);
            fileCursor += 1;
            return path;
        } else if (!folderList.isEmpty()) {
            // Clear the used records
            fileCursor = 0;
            fileList.clear();
            // Enumerate files from folders
            while (!folderList.isEmpty()) {
                String path = folderList.remove(0);
                File folder = new File(this.decodedRootPath + "/" + path);
                File[] files = folder.listFiles();
                if (files != null) {
                    for (File f : files) {
                        String relativePath = path + "/" + f.getName();
                        if (f.isDirectory()) {
                            folderList.add(relativePath);
                        } else if (isTarget(relativePath)) {
                            fileList.add(relativePath);
                        }
                    }
                }
                // If found, break the loop
                if (!fileList.isEmpty()) {
                    break;
                }
            } // end while

            if (!fileList.isEmpty()) {
                fileCursor = 1;
                return fileList.get(0);
            }
        }

        return null;
    }

    @Override
    public boolean isTarget(String entryPath) {
        if (entryPath.matches(regexPath)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isSmaliNeeded() {
        return wildPathStr.startsWith("smali") || wildPathStr.endsWith(".smali");
    }

    @Override
    public boolean isWildMatch() {
        return true;
    }
}
