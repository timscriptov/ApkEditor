package com.gmail.heagoo.sqliteutil.util;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;

import com.gmail.heagoo.sqliteutil.CustomScrollView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class PaddingTable implements OnClickListener, OnTouchListener {

    private Context ctx;
    private TableLayout tableView;

    private ArrayList<String> columnNames;
    private List<ArrayList<String>> tableData;

    // View and its height
    private TableRow headerRow;
    private CustomScrollView scrollView;
    private int scrollViewHeight;
    private int scrollViewWidth;
    private int headerRowHeight;
    private int headerRowWidth;
    private int seperateLineHeight;
    private ImageView paddingImage;

    // Callback function
    private ITableRowClicked rowClickInterface;

    private LayoutParams tableLayoutParam;
    private android.widget.TableRow.LayoutParams rowLayoutParam;
    private TableRow[] tableRows;
    private View[] seperateLines;
    private boolean bShowWholeTable;

    private int bgColor = 0xffffffff;
    private int textColor = 0xff333333;
    private int hoverColor = 0xffe9f2fc;
    private int seperateColor = 0xffcccccc;
    private int headerTextColor = 0xffffffff;
    private int headerBgColor = 0xff7FAF7F;

    public PaddingTable(Context ctx, CustomScrollView scrollView,
                        TableLayout tableView, ITableRowClicked rowClickInterface) {
        this(ctx, scrollView, tableView, rowClickInterface, false);
    }

    public PaddingTable(Context ctx, CustomScrollView scrollView,
                        TableLayout tableView, ITableRowClicked rowClickInterface,
                        boolean isDark) {
        this.ctx = ctx;
        this.scrollView = scrollView;
        this.tableView = tableView;
        this.rowClickInterface = rowClickInterface;
        if (isDark) {
            this.bgColor = 0xff333333;
            this.textColor = 0xffcccccc;
            this.hoverColor = 0xff000000;
            this.seperateColor = 0xff808080;
            this.headerTextColor = 0xffffffff;
            this.headerBgColor = 0xff7FAF7F;
        }
    }

    public void setTableHeaderNames(ArrayList<String> columnNames) {
        this.columnNames = columnNames;
        // tableView.setColumnStretchable(columnNames.size() - 1, true);
    }

    public void setTableData(List<ArrayList<String>> tableData) {
        this.tableData = tableData;
    }

    // Prepare table rows
    public void prepareTable() {
        this.tableLayoutParam = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        this.rowLayoutParam = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT);
        rowLayoutParam.setMargins(8, 0, 8, 0);

        // Create table rows
        int rowNum = tableData.size();
        int colNum = columnNames.size();
        this.tableRows = new TableRow[rowNum];
        TextView textTvs[] = new TextView[colNum];
        this.seperateLines = new View[rowNum];

        for (int i = 0; i < rowNum; i++) {
            List<String> rowData = tableData.get(i);

            tableRows[i] = new TableRow(ctx);
            // row.setLayoutParams(rowParam);
            tableRows[i].setId(i);

            for (int j = 0; j < colNum; j++) {
                textTvs[j] = new TextView(ctx);
                textTvs[j].setTextColor(this.textColor);
                textTvs[j].setText(rowData.get(j));
            }
            for (int j = 0; j < colNum; j++) {
                tableRows[i].addView(textTvs[j], j, rowLayoutParam);
            }

            // Seperate line
            seperateLines[i] = new View(ctx);
            seperateLines[i].setBackgroundColor(seperateColor);
        }

    }

    // private long lastTime = 0;
    // private void debugTime(String info) {
    // long curTime = System.currentTimeMillis();
    // if (lastTime != 0) {
    // Log.d("DEBUG", info + ": " + (curTime - lastTime));
    // }
    // lastTime = curTime;
    // }

    public void showSearchResult(List<ArrayList<String>> data) {
        int rowNum = data.size();
        int colNum = columnNames.size();
        for (int i = 0; i < rowNum; i++) {
            ArrayList<String> rowData = data.get(i);
            for (int col = 0; col < colNum; col++) {
                TextView tv = (TextView) tableRows[i].getChildAt(col);
                tv.setText(rowData.get(col));
            }
            tableRows[i].setVisibility(View.VISIBLE);
            seperateLines[i].setVisibility(View.VISIBLE);
        }

        // Make other rows invisible
        for (int i = rowNum; i < this.tableData.size(); i++) {
            tableRows[i].setVisibility(View.GONE);
            seperateLines[i].setVisibility(View.GONE);
        }

        this.bShowWholeTable = false;
    }

    public void drawTable() {
        // debugTime("start");
        tableView.removeAllViews();
        this.bShowWholeTable = true;

        // Add header
        this.headerRow = new TableRow(ctx);
        for (int j = 0; j < columnNames.size(); j++) {
            TextView textTv = new TextView(ctx);
            textTv.setTextColor(headerTextColor);
            textTv.setText(columnNames.get(j));
            headerRow.addView(textTv, rowLayoutParam);
        }

        // Add a fake column
        // this.paddingImage = new ImageView(ctx);
        // paddingImage.setImageResource(R.drawable.headerbg);
        // headerRow.addView(paddingImage, new TableRow.LayoutParams(
        // TableRow.LayoutParams.WRAP_CONTENT,
        // TableRow.LayoutParams.WRAP_CONTENT, 1.0f));

        headerRow.setBackgroundColor(headerBgColor);
        tableView.addView(headerRow, tableLayoutParam);
        // debugTime("PaddingTable, add header");

        // Add data rows
        TableRow.LayoutParams rowParam = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT);
        TableRow.LayoutParams lineLayout = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT, 1);
        for (int i = 0; i < tableData.size(); i++) {
            tableView.addView(tableRows[i], 2 * i + 1, rowParam);
            tableRows[i].setOnClickListener(this);
            tableRows[i].setOnTouchListener(this);
            tableView.addView(seperateLines[i], 2 * i + 2, lineLayout);
        }
        // debugTime("add rows to table");

        // To compute size dynamically
        setupSizeObserver();
    }

    private void setupSizeObserver() {

        // set up an observer that will be called once the listView's layout is
        // ready
        if (scrollView == null) {
            return;
        }
        // android.view.ViewTreeObserver observer = scrollView
        // .getViewTreeObserver();
        // if (observer.isAlive()) {
        // observer.addOnGlobalLayoutListener(new
        // android.view.ViewTreeObserver.OnGlobalLayoutListener() {
        //
        // @Override
        // public void onGlobalLayout() {
        // View targetView = scrollView;
        // if (targetView != null) {
        // scrollViewHeight = targetView.getMeasuredHeight();
        // scrollViewWidth = targetView.getMeasuredWidth();
        // // don't need the listener any more
        // targetView.getViewTreeObserver()
        // .removeGlobalOnLayoutListener(this);
        // }
        //
        // targetView = headerRow;
        // if (targetView != null) {
        // headerRowHeight = targetView.getMeasuredHeight();
        // headerRowWidth = targetView.getMeasuredWidth();
        // }
        //
        // targetView = seperateLine;
        // if (targetView != null) {
        // seperateLineHeight = targetView.getMeasuredHeight();
        // }
        //
        // new MyHandler(PaddingTable.this).sendEmptyMessage(0);
        // }
        // });
        // }
    }

    // Padding the table to fulfill the screen
    private void padding() {
        // Cannot get the row height
        if (headerRowHeight == 0) {
            headerRowHeight = 32;
        }

        if (headerRowWidth < scrollViewWidth) {
            paddingImage.getLayoutParams().width = scrollViewWidth
                    - headerRowWidth;
        }

        // compute how many rows remaining
        int rowNum = tableData.size();
        int remainHeight = scrollViewHeight - headerRowHeight * (rowNum + 1)
                - seperateLineHeight * rowNum;
        int paddingRows = remainHeight / headerRowHeight;

        for (int i = 0; i < paddingRows; i++) {
            TableRow row = new TableRow(ctx);
            row.addView(new TextView(ctx));

            tableView.addView(row);
        }
    }

    // Click on some item
    @Override
    public void onClick(View v) {
        int index = v.getId();
        if (rowClickInterface != null) {
            rowClickInterface.tableRowClicked(index, bShowWholeTable);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            v.setBackgroundColor(this.hoverColor);
        } else if (action == MotionEvent.ACTION_UP) {
            v.setBackgroundColor(this.bgColor);
            v.performClick();
        } else if (((action & MotionEvent.ACTION_UP) != 0)
                || ((action & MotionEvent.ACTION_OUTSIDE) != 0)) {
            v.setBackgroundColor(this.bgColor);
        }

        return true;
    }

    public static interface ITableRowClicked {
        public void tableRowClicked(int index, boolean bShowWholeTable);
    }

    // To handle padding message
    private static class MyHandler extends Handler {
        private WeakReference<PaddingTable> ref;

        public MyHandler(PaddingTable table) {
            this.ref = new WeakReference<PaddingTable>(table);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    PaddingTable table = ref.get();
                    if (table != null) {
                        table.padding();
                    }
                    break;
            }
        }
    }
}
