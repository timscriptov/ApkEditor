package com.gmail.heagoo.appdm.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "data.db";
    private static final int DATABASE_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS data_backups"
                + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, backup_name VARCHAR, "
                + "backup_time DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + "backup_path VARCHAR, package_name VARCHAR, "
                + "comment VARCHAR, size LONG)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // db.execSQL("ALTER TABLE revised_app ADD COLUMN other STRING");
    }
}