package com.gmail.heagoo.folderlist;

public class FileRecord {

    public String fileName;
    public boolean isDir;
    public long totalSize;
    public boolean isInZip = false;

    public FileRecord() {

    }

    public FileRecord(String fileName, boolean isDir) {
        this(fileName, isDir, false);
    }

    public FileRecord(String fileName, boolean isDir, long size) {
        this(fileName, isDir, false, size);
    }

    public FileRecord(String fileName, boolean isDir, boolean isInZip) {
        this(fileName, isDir, isInZip, 0);
    }

    public FileRecord(String fileName, boolean isDir, boolean isInZip, long size) {
        this.fileName = fileName;
        this.isDir = isDir;
        this.isInZip = isInZip;
        this.totalSize = size;
    }
}
