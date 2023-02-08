package com.gmail.heagoo.sqliteutil;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TableLayout;

import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.sqliteutil.util.HexUtil;
import com.gmail.heagoo.sqliteutil.util.PaddingTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Show all the records in a table
 *
 * @author phe3
 */
public class SqliteTableViewActivity extends CustomizedLangActivity implements
        PaddingTable.ITableRowClicked {

    // Show 30 record in each page
    private final int pageSize = 30;
    private String originDbFilePath;
    private String dbFilePath;
    private String tableName;
    private ArrayList<String> columnNames;
    private ArrayList<String> columnTypes;
    private ArrayList<String> columnIsPKs;
    private List<ArrayList<String>> tableData;
    private TableLayout tableView;
    private PaddingTable table; // Supporter
    // Parameters about table record
    private int tableOffset = 0;
    private int tableSize;
    private Button preBtn;
    private Button nextBtn;

    private int themeId;

    protected static boolean isDateType(String typeName) {
        return typeName.equalsIgnoreCase("DATE")
                || typeName.equalsIgnoreCase("DATETIME");
    }

    protected static boolean isDoubleType(String typeName) {
        return typeName.equalsIgnoreCase("DOUBLE")
                || typeName.equalsIgnoreCase("DOUBLE PRECISION");
    }

    protected static boolean isFloatType(String typeName) {
        return typeName.equalsIgnoreCase("REAL")
                || typeName.equalsIgnoreCase("FLOAT");
    }

    protected static boolean isIntType(String typeName) {
        return typeName.equalsIgnoreCase("INTEGER")
                || typeName.equalsIgnoreCase("LONG")
                || typeName.equalsIgnoreCase("TINYINT")
                || typeName.equalsIgnoreCase("SMALLINT")
                || typeName.equalsIgnoreCase("MEDIUMINT")
                || typeName.equalsIgnoreCase("BIGINT")
                || typeName.equalsIgnoreCase("UNSIGNED BIG INT")
                || typeName.startsWith("INT") || typeName.startsWith("BOOL");
    }

    protected static boolean isBoolType(String typeName) {
        return typeName.startsWith("BOOL");
    }

    protected static boolean isBlobType(String typeName) {
        return typeName.startsWith("BLOB");
    }

    protected static boolean isStringType(String typeName) {
        return typeName.equalsIgnoreCase("TEXT")
                || typeName.equalsIgnoreCase("NCHAR")
                || typeName.equalsIgnoreCase("CLOB")
                || typeName.endsWith("VARCHAR")
                || typeName.endsWith("CHARACTER")
                || typeName.startsWith("NUMERIC")
                || typeName.startsWith("DECIMAL");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // sawsem theme
//		this.themeId = ActivityUtil.getIntParam(getIntent(), "themeId");
//		switch (themeId) {
//			case 1:
//				super.setTheme(android.R.style.Theme_Black_NoTitleBar);
//				setContentView(R.layout.sql_activity_tableview_dark);
//				break;
//			case 2:
//				super.setTheme(android.R.style.Theme_Black_NoTitleBar);
//				setContentView(R.layout.sql_activity_tableview_dark_ru);
//				break;
//			default:
        setContentView(R.layout.sql_activity_tableview);
//				break;
//		}

        this.originDbFilePath = ActivityUtil.getParam(getIntent(),
                "originDbFilePath");
        this.dbFilePath = ActivityUtil.getParam(getIntent(), "dbFilePath");
        this.tableName = ActivityUtil.getParam(getIntent(), "tableName");

        initTableData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        initTableView();

        this.preBtn = (Button) this.findViewById(R.id.button_prepage);
        this.nextBtn = (Button) this.findViewById(R.id.button_nextpage);
        if (tableSize < pageSize) { // One Page is enough
            nextBtn.setVisibility(View.GONE);
        } else {
            preBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    prePageClicked();
                }
            });
            nextBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    nextPageClicked();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Table record modified in the SqliteRowViewActivity
        if (requestCode == 0 && resultCode == 1) {
//			Log.d("DEBUG", "table should be updated!");
            queryTableData();
            // updateTableView();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
//		Log.d("DEBUG",
//				String.format("tableWidth=%d, tableHeight=%d",
//						tableView.getWidth(), tableView.getHeight()));
        return super.onKeyDown(keyCode, event);
    }

    private void prePageClicked() {
        this.tableOffset -= pageSize;
        if (tableOffset < 0) {
            tableOffset = 0;
        }

        queryTableData();

        if (tableOffset > 0) {
            preBtn.setVisibility(View.VISIBLE);
        } else {
            preBtn.setVisibility(View.GONE);
        }
        if (tableOffset + tableData.size() < tableSize) {
            nextBtn.setVisibility(View.VISIBLE);
        } else {
            nextBtn.setVisibility(View.GONE);
        }

        updateTableView();
    }

    private void nextPageClicked() {
        this.tableOffset += pageSize;

        queryTableData();

        // Update Button
        if (tableOffset > 0) {
            preBtn.setVisibility(View.VISIBLE);
        } else {
            preBtn.setVisibility(View.GONE);
        }
        if (tableOffset + tableData.size() < tableSize) {
            nextBtn.setVisibility(View.VISIBLE);
        } else {
            nextBtn.setVisibility(View.GONE);
        }

        updateTableView();
    }

    private void initTableData() {
        this.tableOffset = 0;

        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFilePath, null,
                SQLiteDatabase.OPEN_READONLY);
        initColumnInfo(db);
        getTableSize(db);
        tableData = query(db, tableOffset, pageSize);
        db.close();
    }

    private void initTableView() {

//		CustomScrollView scrollView = (CustomScrollView) this
//				.findViewById(R.id.scrollView);
        this.tableView = (TableLayout) this.findViewById(R.id.valueTable);

//		this.table = new PaddingTable(this, scrollView, tableView, this);
        this.table = new PaddingTable(this, null, tableView, this, themeId != 0);
        table.setTableHeaderNames(columnNames);
        table.setTableData(tableData);
        table.prepareTable();
        table.drawTable();
    }

    private void updateTableView() {
        table.setTableData(tableData);
        table.prepareTable();
        table.drawTable();
    }

    @Override
    public void tableRowClicked(int index, boolean bWholeTable) {
        Intent intent = new Intent(this, SqliteRowViewActivity.class);
        ActivityUtil.attachParam(intent, "originDbFilePath", originDbFilePath);
        ActivityUtil.attachParam(intent, "dbFilePath", dbFilePath);
        ActivityUtil.attachParam(intent, "tableName", tableName);
        ActivityUtil.attachParam(intent, "columnNames", columnNames);
        ActivityUtil.attachParam(intent, "columnTypes", columnTypes);
        ActivityUtil.attachParam(intent, "columnIsPKs", columnIsPKs);
        ActivityUtil.attachParam(intent, "rowData", tableData.get(index));
        ActivityUtil.attachParam(intent, "themeId", this.themeId);
        this.startActivityForResult(intent, 0);
    }

    private List<ArrayList<String>> query(SQLiteDatabase db, int offset,
                                          int limit) {
        ArrayList<ArrayList<String>> recordList = new ArrayList<ArrayList<String>>();
        Cursor c = queryTheCursor(db, offset, limit);
        while (c.moveToNext()) {
            ArrayList<String> record = new ArrayList<String>();
            for (int i = 0; i < columnNames.size(); i++) {
                record.add(getValue(c, i));
            }
            recordList.add(record);
        }
        c.close();
        return recordList;
    }

    private String getValue(Cursor c, int i) {
        try {
            String typeName = columnTypes.get(i);
            if (isStringType(typeName)) {
                return c.getString(i);
            }
            if (isIntType(typeName)) {
                return "" + c.getLong(i);
            }
            if (isDateType(typeName)) {
                return c.getString(i);
            }
            if (isFloatType(typeName)) {
                return "" + c.getFloat(i);
            }
            if (isDoubleType(typeName)) {
                return "" + c.getDouble(i);
            }
            if (isBlobType(typeName)) {
                byte[] data = c.getBlob(i);
                if (data.length > 64) {
                    return "(Too big, first 64 byte): \n"
                            + HexUtil.bytesToHexString(data, 0, 64);
                } else {
                    return HexUtil.bytesToHexString(data, 0, data.length);
                }
            }

            // Suppose it to be String
            try {
                return c.getString(i);
            } catch (Exception e) {
                return "(un-supported type)";
            }
        } catch (Exception e) {
            return "(error to parse)";
        }
    }

    /**
     * query all records, return cursor
     *
     * @return Cursor
     */
    private Cursor queryTheCursor(SQLiteDatabase db, int offset, int limit) {
        Cursor c = db.rawQuery("SELECT * FROM " + tableName + " limit " + limit
                + " offset " + offset, null);
        return c;
    }

    protected void queryTableData() {
        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFilePath, null,
                SQLiteDatabase.OPEN_READONLY);
        tableData = query(db, tableOffset, pageSize);
        db.close();
    }

    private void initColumnInfo(SQLiteDatabase db) {
        if (columnNames == null) {
            this.columnNames = new ArrayList<String>();
            this.columnTypes = new ArrayList<String>();
            this.columnIsPKs = new ArrayList<String>();

            Cursor c = db
                    .rawQuery("PRAGMA table_info(" + tableName + ")", null);
            if (c.moveToFirst()) {
                int pkIdx = c.getColumnIndex("pk");
                do {
                    String name = c.getString(1);
                    String type = c.getString(2);
                    int isPK = c.getInt(pkIdx);
                    columnNames.add(name);
                    if (type != null) {
                        type = type.toUpperCase();
                    }
                    // Log.d("DEBUG", "name = " + name + ", type = " + type);
                    columnTypes.add(type);
                    columnIsPKs.add("" + isPK);
                } while (c.moveToNext());
            }
            c.close();
        }
    }

    private void getTableSize(SQLiteDatabase db) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null);
        if (c.moveToFirst()) {
            this.tableSize = c.getInt(0);
        }
        c.close();
    }
}
