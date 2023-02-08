package com.gmail.heagoo.sqliteutil;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.CustomizedLangActivity;

import java.io.File;
import java.util.ArrayList;

/**
 * Show all the table name in the DB
 *
 * @author phe3
 */
public class SqliteTableListActivity extends CustomizedLangActivity {

    private String originDbFilePath;
    private String dbFilePath;
    private ArrayList<String> tableList;

    private boolean isRootMode;
    private int themeId = 0;

    private int textColor = 0xff333333;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Bundle bundle = getIntent().getExtras();

        // sawsem theme
//		if (bundle != null) {
//			this.themeId = bundle.getInt("themeId", 0);
//		}

//		if (themeId != 0) {
//			super.setTheme(android.R.style.Theme_Black_NoTitleBar);
        this.textColor = 0xffcccccc;
//			setContentView(themeId == 1 ?
//					R.layout.sql_activity_tablelist_dark :
//					R.layout.sql_activity_tablelist_dark_ru);
//		} else {
        setContentView(R.layout.sql_activity_tablelist);
        //	}

        Intent intent = getIntent();
        this.originDbFilePath = ActivityUtil
                .getParam(intent, "dbFilePath");
        String strRootMode = ActivityUtil.getParam(intent, "isRootMode");
        if ("false".equalsIgnoreCase(strRootMode)) {
            isRootMode = false;
        } else {
            isRootMode = true; // This is the default value
        }

        try {
            prepareAccessibleFile();
            initData();
            initView();
        } catch (Exception e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                    .show();
            this.finish();
        }
    }

    private void prepareAccessibleFile() throws Exception {
        // For non-root mode, just directly use the origin file
        if (!isRootMode) {
            this.dbFilePath = originDbFilePath;
            return;
        }

        // Following code is for root mode
        if (!SDCard.exist()) {
            throw new Exception("Can not find SD Card!");
        }
        String sdDir = SDCard.getRootDirectory();
        if (!sdDir.endsWith("/")) {
            sdDir += "/";
        }

        File f = new File(getFilesDir(), "work.db");
        this.dbFilePath = f.getPath();

//		RootCommand rc = new RootCommand();
//		boolean copyRet = rc.runRootCommand(
//				String.format("cat %s > %s", originDbFilePath, dbFilePath),
//				null, 2000);
        RootCommand rc = new RootCommand();
        String strCmd = "cp";
        File bin = new File(getFilesDir(), "mycp");
        if (bin.exists()) {
            strCmd = bin.getPath();
        }
        boolean copyRet = rc.runRootCommand(String.format(
                        //strCmd + " \"%s\" %s && chmod 666 %s",
                        strCmd + " \"%s\" %s",
                        originDbFilePath, dbFilePath),
                null, 2000);

        // Copy file failed, use the original file
        if (!copyRet) {
            dbFilePath = originDbFilePath;
        }
    }

    private void initView() {
        // Title
        TextView tv = (TextView) this.findViewById(R.id.title);
        tv.setText(getResources().getString(R.string.tables_of) + " "
                + getFileName(originDbFilePath));

        // List
        ListView tableLv = (ListView) this.findViewById(R.id.tableList);
        tableLv.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, tableList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position,
                        convertView, parent);
                textView.setTextColor(textColor);
                return textView;
            }
        });
        tableLv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                                    int position, long arg3) {
                String tableName = tableList.get(position);
                Intent intent = new Intent(SqliteTableListActivity.this,
                        SqliteTableViewActivity.class);
                ActivityUtil.attachParam(intent, "originDbFilePath",
                        originDbFilePath);
                ActivityUtil.attachParam(intent, "dbFilePath", dbFilePath);
                ActivityUtil.attachParam(intent, "tableName", tableName);
                ActivityUtil.attachParam(intent, "themeId", themeId);
                startActivity(intent);
            }
        });
    }

    private String getFileName(String filePath) {
        int pos = filePath.lastIndexOf('/');
        return filePath.substring(pos + 1);
    }

    private void initData() {
        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFilePath, null,
                SQLiteDatabase.OPEN_READONLY);

        this.tableList = new ArrayList<String>();
        Cursor c = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'", null);

        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                tableList.add(c.getString(c.getColumnIndex("name")));
                c.moveToNext();
            }
        }

        db.close();
    }

}
