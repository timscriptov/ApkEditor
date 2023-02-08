package com.gmail.heagoo.appdm;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.appdm.base.R;
import com.gmail.heagoo.appdm.util.ADManager;
import com.gmail.heagoo.appdm.util.FileCopyUtil;
import com.gmail.heagoo.appdm.util.SDCard;
import com.gmail.heagoo.appdm.util.XmlUtils;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.CommandInterface;
import com.gmail.heagoo.common.CommandRunner;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.sqliteutil.RootCommand;
import com.gmail.heagoo.sqliteutil.util.PaddingTable;
import com.gmail.heagoo.sqliteutil.util.PaddingTable.ITableRowClicked;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

public class PrefDetailActivity extends CustomizedLangActivity implements ITableRowClicked,
        OnClickListener {

    // File path of the xml in /data/data/..
    protected String filePath;
    // Temporary file path
    protected String tmpFilePath;
    private String appName;
    private ParseThread thread;

    private ScrollView scrollView;
    private ProgressBar progressBar;
    private TableLayout tableView;
    private LinkedHashMap<String, Object> keyValues;
    private LinkedHashMap<String, Object> searchedKeyValues; // Result of search
    private ArrayList<ArrayList<String>> data; // arranged table data, contain
    // all the data
    private ADManager adManager;
    // private Object interestAd;

    // Is root mode or not
    private boolean isRootMode;
    // Is google version or not
    private boolean isGoogleVersion;

    // Table View Wrapper
    private PaddingTable paddingTable;

    private MyHandler handler = new MyHandler(this);

    // Search button
    private Button searchBtn;

    private int themeId;
    private boolean isDark;

    // Support root mode and non-root mode
    private static CommandInterface createCommandRunner(boolean isRootMode) {
        if (isRootMode) {
            return new RootCommand();
        } else {
            return new CommandRunner();
        }
    }

    ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

//		this.themeId = ActivityUtil.getIntParam(this.getIntent(), "themeId");
//		this.isDark = (themeId != 0);
//		switch (themeId) {
//			case 1:
//				super.setTheme(android.R.style.Theme_Black_NoTitleBar);
//				setContentView(R.layout.appdm_activity_prefdetail_dark);
//				break;
//			case 2:
//				super.setTheme(android.R.style.Theme_Black_NoTitleBar);
//				setContentView(R.layout.appdm_activity_prefdetail_dark_ru);
//				break;
//			default:
        setContentView(R.layout.appdm_activity_prefdetail);
//				break;
//		}

        // Check is google version or not
        try {
            ApplicationInfo appInfo = this.getPackageManager()
                    .getApplicationInfo(getPackageName(),
                            PackageManager.GET_META_DATA);
            String channel = appInfo.metaData
                    .getString("com.gmail.heagoo.publish_channel");
            // Log.d("DEBUG", " channel == " + channel);
            this.isGoogleVersion = "gplay".equals(channel);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.isRootMode = ActivityUtil.getBoolParam(this.getIntent(),
                "isRootMode");
        this.appName = ActivityUtil.getParam(this.getIntent(), "appName");
        this.filePath = ActivityUtil.getParam(this.getIntent(), "xmlFilePath");

        initUI();

        this.adManager = ADManager.init(this, R.id.adViewLayout);

        refresh();
        // Log.d("DEBUG", "PrefDetailActivity.create called.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        adManager.resume();
    }

    @Override
    public void onPause() {
        adManager.pause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        adManager.destroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1000: // EditorActivity
                if (resultCode != 0) { // modified
                    this.setResult(1);
                    this.refresh();
                }
                break;
        }
    }

    public void refresh() {
        this.thread = new ParseThread(this);
        thread.start();
    }

    private void initUI() {
        this.scrollView = (ScrollView) this.findViewById(R.id.scroll_view);
        this.progressBar = (ProgressBar) this.findViewById(R.id.progress_bar);
        this.tableView = (TableLayout) findViewById(R.id.valueTable);

        // Text View
        TextView tv = (TextView) this.findViewById(R.id.tv_appname);
        tv.setText(appName);
        tv = (TextView) this.findViewById(R.id.tv_prefname);
        tv.setText(getShortPath(filePath));

        // Close Button
        this.findViewById(R.id.button_close).setOnClickListener(this);

        // Search button
        this.searchBtn = (Button) this.findViewById(R.id.button_search);
        searchBtn.setVisibility(View.GONE);
        searchBtn.setOnClickListener(this);

        // raw file button
        this.findViewById(R.id.btn_raw_file).setOnClickListener(this);
    }

    private CharSequence getShortPath(String path) {
        int pos = path.lastIndexOf("/");
        String filename = path.substring(pos + 1);
        // Delete ".xml"
        return filename.substring(0, filename.length() - 4);
    }

    public void parseFinished(String errMsg) {
        if (errMsg == null) {
            handler.sendEmptyMessage(0);
        } else {
            handler.setErrorMessage(errMsg);
            handler.sendEmptyMessage(1);
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

    private void prepareTable(LinkedHashMap<String, Object> keyValues) {
        if (keyValues == null) {
            return;
        }
        // debugTime("start");
        this.keyValues = keyValues;
        this.data = new ArrayList<ArrayList<String>>();
        for (String key : keyValues.keySet()) {
            ArrayList<String> rowData = new ArrayList<String>();
            Object obj = keyValues.get(key);
            String value = "";
            if (obj != null) {
                value = obj.toString();
            }
            rowData.add(key);
            rowData.add(value);
            data.add(rowData);
        }

        // No custom scroll view any more, always NULL
        if (this.paddingTable == null) {
            this.paddingTable = new PaddingTable(this, null, tableView, this, this.isDark);
        }
        ArrayList<String> header = new ArrayList<String>();
        header.add("Key");
        header.add("Value");
        paddingTable.setTableHeaderNames(header);
        paddingTable.setTableData(data);
        paddingTable.prepareTable();

    }

    private void showTable() {
        paddingTable.drawTable();
        // Show the result
        scrollView.setVisibility(View.VISIBLE);
        searchBtn.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void tableRowClicked(int position, boolean bWholeTable) {
        // Now make it always editable
        if (!bWholeTable) {
            Dialog dialog = new KeyValueDialog(this, searchedKeyValues,
                    position, true, this.themeId);
            dialog.show();
        } else {
            Dialog dialog = new KeyValueDialog(this, keyValues, position, true,
                    this.themeId);
            dialog.show();
        }
    }

    // private void showKeyValueDialog(String key, Object value) {
    // LayoutInflater inflater = getLayoutInflater();
    // View layout = inflater.inflate(R.layout.dialog_keyvalue, null);
    // EditText keyEdit = (EditText) layout.findViewById(R.id.et_key);
    // keyEdit.setText(key);
    // EditText valueEdit = (EditText) layout.findViewById(R.id.et_valuey);
    // valueEdit.setText(value.toString());
    //
    // Dialog dialog = new Dialog(this);
    // dialog.setContentView(layout);
    // dialog.getWindow().setBackgroundDrawableResource(
    // android.R.color.transparent);
    // // layout.setDialog(dialog);
    // dialog.show();
    //
    // // new AlertDialog.Builder(this)
    // // .setView(layout)
    // // .setPositiveButton(R.string.save,
    // // new DialogInterface.OnClickListener() {
    // // @Override
    // // public void onClick(DialogInterface dialog,
    // // int which) {
    // // showNotSupportTip();
    // // }
    // // }).setNegativeButton(R.string.cancel, null).show();
    // }

    // Save a key value record to xmls
    public void saveValue(String key, Object newValue) throws Exception {
        keyValues.put(key, newValue);

        if (isRootMode) {
            saveValue_root();
        } else {
            saveValue_nonroot();
        }

        // Set as modified
        this.setResult(1);
    }

    private void saveValue_root() throws Exception {
        FileOutputStream out = new FileOutputStream(tmpFilePath);
        XmlUtils.writeMapXml(keyValues, out);
        out.close();

        FileCopyUtil.copyBack(this, tmpFilePath, filePath, isRootMode);
    }

    // For this mode, directly write to the file
    private void saveValue_nonroot() throws Exception {
        FileOutputStream out = new FileOutputStream(filePath);
        XmlUtils.writeMapXml(keyValues, out);
        out.close();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.button_close) {
            this.finish();
        } else if (id == R.id.button_search) {
            doSearch();
        } else if (id == R.id.btn_raw_file) {
            // Open the editor
            Intent intent = new Intent(this,
                    com.gmail.heagoo.neweditor.EditorActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("filePath", this.tmpFilePath);
            bundle.putString("realFilePath", this.filePath);
            bundle.putBoolean("isRootMode", this.isRootMode);
            bundle.putIntArray("resourceIds", new int[]{
                    R.string.appdm_file_too_big, R.string.appdm_file_saved,
                    R.string.appdm_not_found});
            intent.putExtras(bundle);
            this.startActivityForResult(intent, 1000);
        }
    }

    private void doSearch() {
        EditText et = (EditText) this.findViewById(R.id.tv_keyword);
        String keyword = et.getText().toString();
        keyword = keyword.trim();
        if (keyword.equals("")) {
            // Not correct
            // paddingTable.drawTable();
            // return;
            Toast.makeText(this, R.string.warning_keyword_empty,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // result is ued for dialog value show
        // tableResult is used for table view
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        List<ArrayList<String>> tableResult = new ArrayList<ArrayList<String>>();

        for (Entry<String, Object> entry : this.keyValues.entrySet()) {
            boolean matched = false;

            // Check the key
            String key = entry.getKey();
            Object obj = entry.getValue();

            String val = key.toLowerCase();
            if (val.contains(keyword)) {
                matched = true;
            }

            // Check the value
            if (!matched) {
                if (obj != null) {
                    val = obj.toString().toLowerCase();
                    if (val.contains(keyword)) {
                        matched = true;
                    }
                }
            }

            if (matched) {
                result.put(key, obj);

                ArrayList<String> rowData = new ArrayList<String>();
                rowData.add(key);
                if (obj != null) {
                    rowData.add(obj.toString());
                } else {
                    rowData.add("");
                }
                tableResult.add(rowData);
            }
        }

        // Show the result
        if (!result.isEmpty()) {
            this.searchedKeyValues = result;
            paddingTable.showSearchResult(tableResult);
        } else {
            Toast.makeText(this, "No record found.", Toast.LENGTH_SHORT).show();
        }
    }

    private static class MyHandler extends Handler {
        WeakReference<PrefDetailActivity> ref;
        private String errMsg;

        public MyHandler(PrefDetailActivity act) {
            this.ref = new WeakReference<PrefDetailActivity>(act);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // Succeed
                case 0:
                    PrefDetailActivity act = ref.get();
                    if (act != null) {
                        act.showTable();
                    }
                    break;
                case 1: // failed
                    Toast.makeText(ref.get(), "Error: " + errMsg, Toast.LENGTH_LONG)
                            .show();
                    break;
            }
        }

        public void setErrorMessage(String errMsg) {
            this.errMsg = errMsg;
        }
    }

//	private String getFileName() {		
//		int pos = filePath.lastIndexOf('/');
//		if (pos != -1) {
//			return filePath.substring(pos + 1);
//		}
//		return filePath;
//	}

    static class ParseThread extends Thread {

        private WeakReference<PrefDetailActivity> activityRef;

        public ParseThread(PrefDetailActivity prefDetailActivity) {
            this.activityRef = new WeakReference<PrefDetailActivity>(
                    prefDetailActivity);
        }

        @Override
        public void run() {
            String extraInfo = null;
            PrefDetailActivity activity = activityRef.get();
            if (activity != null) {
                try {
                    if (!SDCard.exist()) {
                        throw new Exception("Can not find SD Card!");
                    }
                    String workingDir = SDCard.getTempDir(activity);

                    File dir = new File(workingDir);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    HashMap<String, Object> result = null;
                    if (activity.isRootMode) {
                        File f = new File(activity.getFilesDir(), "work.xml");
                        String tmpFilePath = f.getPath();
                        activity.tmpFilePath = tmpFilePath;
                        CommandInterface rc = createCommandRunner(activity.isRootMode);
                        String strCmd = "cp";
                        File bin = new File(activity.getFilesDir(), "mycp");
                        if (bin.exists()) {
                            strCmd = bin.getPath();
                        }
                        boolean copyRet = rc.runCommand(String.format(
                                        //strCmd + " \"%s\" %s && chmod 666 %s",
                                        strCmd + " \"%s\" %s",
                                        activity.filePath, tmpFilePath, tmpFilePath),
                                null, 2000);
                        extraInfo = rc.getStdError();
                        // Copy file failed, try the original file
                        if (!copyRet) {
                            tmpFilePath = activity.filePath;
                        }
                        FileInputStream in = new FileInputStream(tmpFilePath);
                        result = XmlUtils.readMapXml(in);
                        in.close();
                    }
                    // For the non-root mode, directly read it
                    else {
                        FileInputStream in = new FileInputStream(
                                activity.filePath);
                        result = XmlUtils.readMapXml(in);
                        in.close();
                    }

                    // FileOutputStream out = new FileOutputStream(dstPath);
                    // XmlUtils.writeMapXml(result, out);

                    LinkedHashMap<String, Object> ret = new LinkedHashMap<String, Object>();
                    for (Entry<String, Object> entry : result.entrySet()) {
                        ret.put(entry.getKey(), entry.getValue());
                    }

                    activity.prepareTable(ret);
                    activity.parseFinished(null);
                } catch (Exception e) {
                    String errMsg = e.getMessage() + ": " + extraInfo;
                    activity.parseFinished(errMsg);
                }
            }
        }
    }
}
