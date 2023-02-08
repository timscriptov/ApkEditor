package com.gmail.heagoo.appdm.db;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupInfo {

    public int _id;
    public String backupName;
    public String backupTime;
    public String backupPath;
    public String packageName;
    public String comment;
    public long backupSize;

    public BackupInfo() {
        this.backupTime = getCurrentDateTime();
    }

    public static String getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }
}
