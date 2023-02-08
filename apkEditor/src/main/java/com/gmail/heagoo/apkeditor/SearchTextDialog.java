package com.gmail.heagoo.apkeditor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.ProcessingDialog.ProcessingInterface;
import com.gmail.heagoo.apkeditor.ac.AutoCompleteAdapter;
import com.gmail.heagoo.apkeditor.ac.EditTextWithTip;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.ActivityUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

class MatchedLineItem {
    int lineIndex;
    int matchedPosition;
    String lineContent;

    public MatchedLineItem(int lineIndex, int position, String line) {
        this.lineIndex = lineIndex;
        this.matchedPosition = position;
        this.lineContent = line;
    }
}

class TxtSearchResult {
    String filePath;
    String keyword;
    List<MatchedLineItem> matchList;
}

// /////////////////////////////////////////////////////////////////////

public class SearchTextDialog extends Dialog
        implements OnGroupClickListener, OnChildClickListener, OnClickListener, AdapterView.OnItemLongClickListener {

    private TextView titleTv;
    private EditTextWithTip etReplaceAll;
    private ExpandableListView listView;
    private MatchedTextListAdapter listAdapter;
    private LinearLayout searchingLayout;

    private WeakReference<ApkInfoActivity> activityRef;
    private String searchFolder;
    private List<String> filenameList;
    private String keyword;
    private boolean caseSensitive;

    // Record matched files
    private ArrayList<String> matchedFiles = new ArrayList<>();

    // Replace string
    private AutoCompleteAdapter adapter;

    public SearchTextDialog(ApkInfoActivity activity, String searchFolder,
                            List<String> filenameList,
                            String keyword, boolean caseSensitive) {
        super(activity);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Full screen
        if (GlobalConfig.instance(activity).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        this.activityRef = new WeakReference<>(activity);
        this.searchFolder = searchFolder;
        this.filenameList = filenameList;
        this.keyword = keyword;
        this.caseSensitive = caseSensitive;
        if (!this.searchFolder.endsWith("/")) {
            searchFolder += "/";
        }
        // this.searchFolder = "/storage/sdcard1/";
        // this.keyword = "android";

        init(activity);
    }

    private void init(Activity activity) {
        int resId;
        // sawsem theme
//        switch (GlobalConfig.instance(activity).getThemeId()) {
//            case GlobalConfig.THEME_DARK_DEFAULT:
//                resId = R.layout.dialog_txt_searchresult_dark;
//                break;
//            case GlobalConfig.THEME_DARK_RUSSIAN:
//                resId = R.layout.dialog_txt_searchresult_dark_ru;
//                break;
//            default:
        resId = R.layout.dialog_txt_searchresult;
//                break;
//        }

        View view = LayoutInflater.from(activity).inflate(resId, null);

        this.titleTv = (TextView) view.findViewById(R.id.title);
        this.etReplaceAll = (EditTextWithTip) view.findViewById(R.id.et_replaceall);
        this.listView = (ExpandableListView) view.findViewById(R.id.lv_matchedfiles);
        this.searchingLayout = (LinearLayout) view.findViewById(R.id.searching_layout);
        listView.setVisibility(View.INVISIBLE);

        // Start searching task
        new AsyncFolderSearchTask(searchFolder, filenameList, keyword,
                caseSensitive).execute();

        // Replace all
        view.findViewById(R.id.btn_replaceall).setOnClickListener(this);
        this.adapter = new AutoCompleteAdapter(
                activity.getApplicationContext(), "search_replace_with");
        EditTextWithTip etReplaceAll = (EditTextWithTip) view.findViewById(R.id.et_replaceall);
        etReplaceAll.setAdapter(adapter);

        this.setContentView(view);
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v,
                                int groupPosition, long id) {
        boolean expanded = parent.isGroupExpanded(groupPosition);
        if (!expanded) {
            // Search it only when never searched before
            if (!listAdapter.groupChildExist(groupPosition)) {
                String filePath = (String) listAdapter.getGroup(groupPosition);
                String keyword = listAdapter.getKeyword();
                new AsyncFileSearchTask(filePath, keyword, groupPosition)
                        .execute();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v,
                                int groupPos, int childPos, long id) {
        ArrayList<String> filePathList = listAdapter.getFileList();

        MatchedLineItem item = (MatchedLineItem) listAdapter.getChild(groupPos, childPos);
        if (item == null) {
            return false;
        }

        Intent intent;

        // If too many files, only edit current file
        if (filePathList.size() > 100) {
            String filePath = filePathList.get(groupPos);
            ApkInfoActivity activity = activityRef.get();
            intent = TextEditor.getEditorIntent(activity, filePath, activity.getApkPath());
            ActivityUtil.attachParam(intent, "startLine", "" + item.lineIndex);
        } else {
            ApkInfoActivity activity = activityRef.get();
            intent = TextEditor.getEditorIntent(activity, filePathList, groupPos, activity.getApkPath());
            ActivityUtil.attachParam(intent, "fileList", filePathList);
            ActivityUtil.attachParam(intent, "curFileIndex", groupPos);

            ArrayList<Integer> startLineList = new ArrayList<>(filePathList.size());
            for (int i = 0; i < groupPos; ++i) {
                startLineList.add(-1);
            }
            startLineList.add(item.lineIndex);
            for (int i = groupPos + 1; i < filePathList.size(); ++i) {
                startLineList.add(-1);
            }
            ActivityUtil.attachParam2(intent, "startLineList", startLineList);
        }

        ActivityUtil.attachParam(intent, "searchString", keyword);

        activityRef.get().startActivityForResult(intent, 0);

        return false;
    }

    private void showMatchedFiles() {
        // Set title
        String format = activityRef.get().getString(R.string.str_files_found);
        String text = String.format(format, matchedFiles.size(), keyword);
        titleTv.setText(text);

        this.listAdapter = new MatchedTextListAdapter(activityRef,
                listView, searchFolder, matchedFiles, keyword);
        listView.setAdapter(listAdapter);
        listView.setOnGroupClickListener(this);
        listView.setOnChildClickListener(this);
        listView.setOnItemLongClickListener(this);

        // Switch to list view
        listView.setVisibility(View.VISIBLE);
        searchingLayout.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_replaceall) {
            showConfirmDialog();
        }
    }

    private void showConfirmDialog() {

        final String strReplace = etReplaceAll.getText().toString();

        AlertDialog.Builder comfirmDlg = new AlertDialog.Builder(activityRef.get());
        String msg = String.format(
                activityRef.get().getString(R.string.sure_to_replace_all),
                this.keyword, strReplace);
        comfirmDlg.setMessage(msg);

        comfirmDlg.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        if (!"".equals(strReplace.trim())) {
                            adapter.addInputHistory(strReplace);
                        }
                        doReplaceAll(strReplace);
                    }
                });

        comfirmDlg.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        // Canceled.
                    }
                });

        comfirmDlg.show();
    }

    protected void doReplaceAll(final String strReplace) {
        new ProcessingDialog(activityRef.get(), new ProcessingInterface() {
            int failedNum = 0;
            String failMessage = "";

            @Override
            public void process() throws Exception {
                for (String f : matchedFiles) {
                    try {
                        listAdapter.replaceWith(f, strReplace);
                        addModification(f);
                    } catch (Exception e) {
                        failMessage += "\n" + String.format(
                                activityRef.get().getString(R.string.failed_to_modify), f);
                        failedNum += 1;
                    }
                }
            }

            @Override
            public void afterProcess() {
                for (int i = 0; i < listAdapter.getGroupCount(); ++i) {
                    listView.collapseGroup(i);
                    listAdapter.removeSearchResult(i);
                }

                String msg = activityRef.get().getString(R.string.str_num_modified_file);
                msg = String.format(msg, matchedFiles.size() - failedNum);

                if (failedNum > 0) {
                    msg += failMessage;
                    Toast.makeText(activityRef.get(), msg, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activityRef.get(), msg, Toast.LENGTH_SHORT).show();
                }
            }

        }, -1);
    }

    // Mark the file as modified
    public void addModification(String filePath) {
        activityRef.get().dealWithModifiedFile(filePath, null);
    }

    // Long click on list item
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
                                   int position, long id) {
        int itemType = ExpandableListView.getPackedPositionType(id);
        if (itemType != ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            return true;
        }

        final int groupIdx = ExpandableListView.getPackedPositionGroup(id);
        parent.setOnCreateContextMenuListener(
                new View.OnCreateContextMenuListener() {
                    public void onCreateContextMenu(ContextMenu menu, View v,
                                                    ContextMenu.ContextMenuInfo menuInfo) {

                        // Delete
                        MenuItem item1 = menu.add(0, Menu.FIRST, 0,
                                R.string.delete);
                        item1.setOnMenuItemClickListener(
                                new MenuItem.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(
                                            MenuItem item) {
                                        deleteItem(groupIdx);
                                        return true;
                                    }
                                });
                        // Extract
                        MenuItem item2 = menu.add(0, Menu.FIRST + 1, 0,
                                R.string.extract);
                        item2.setOnMenuItemClickListener(
                                new MenuItem.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(
                                            MenuItem item) {
                                        extractItem(groupIdx);
                                        return true;
                                    }
                                });
                        // Replace the file
                        MenuItem item3 = menu.add(0, Menu.FIRST + 2, 0,
                                R.string.replace);
                        MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                replaceItem(groupIdx);
                                return true;
                            }
                        };
                        item3.setOnMenuItemClickListener(listener);
                    }
                });

        return false;
    }

    private void deleteItem(int position) {
        ResListAdapter resManager = activityRef.get().getResListAdapter();

        // Use ResListAdapter to delete it
        String filepath = this.matchedFiles.get(position);
        int pos = filepath.lastIndexOf('/');
        String dirPath = (pos != -1) ? filepath.substring(0, pos) : "";
        String fileName = filepath.substring(pos + 1);
        resManager.deleteFile(dirPath, fileName, false);

        // Update UI
        listAdapter.removeItem(position);
    }

    private void extractItem(int position) {
        if (position < matchedFiles.size()) {
            String filepath = matchedFiles.get(position);
            activityRef.get().extractFileOrDir(filepath);
        }
    }

    private void replaceItem(final int position) {
        if (position < matchedFiles.size()) {
            String filepath = matchedFiles.get(position);
            activityRef.get().replaceFile(filepath,
                    new SomethingChangedListener() {
                        @Override
                        public void somethingChanged() {
                            listView.collapseGroup(position);
                            listAdapter.removeSearchResult(position);
                        }
                    });
        }
    }

    // Search all the files inside the folder
    private class AsyncFolderSearchTask
            extends AsyncTask<Object, Void, List<String>> {

        private String baseFolder;
        private List<String> filenameList;
        private String keyword;
        private String lcKeyword; // lower case
        private boolean caseSensitive;

        @SuppressLint("DefaultLocale")
        public AsyncFolderSearchTask(String folderPath,
                                     List<String> filenameList, String keyword,
                                     boolean caseSensitive) {
            this.baseFolder = folderPath;
            this.filenameList = filenameList;
            this.keyword = keyword;
            this.lcKeyword = keyword.toLowerCase();
            this.caseSensitive = caseSensitive;
        }

        // Check the file whether contains the keyword
        @SuppressLint("DefaultLocale")
        private boolean fileContainsKeyword(File file) {
            boolean ret = false;

            BufferedReader br = null;
            try {
                br = new BufferedReader(
                        new InputStreamReader(new FileInputStream(file)));

                // Search the keyword line by line
                String line = br.readLine();
                if (caseSensitive) {
                    while (line != null) {
                        if (line.contains(keyword)) {
                            ret = true;
                            break;
                        }
                        line = br.readLine();
                    }
                } else {
                    while (line != null) {
                        if (line.toLowerCase().contains(lcKeyword)) {
                            ret = true;
                            break;
                        }
                        line = br.readLine();
                    }
                }

            } catch (Exception e) {
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                    }
                }
            }

            return ret;
        }

        private void searchFolder(File folderFile) {
            File[] files = folderFile.listFiles();
            if (files != null)
                for (File f : files) {
                    if (f.isDirectory()) {
                        searchFolder(f);
                    } else if (isTxtFile(f)) {
                        if (fileContainsKeyword(f)) {
                            SearchTextDialog.this.matchedFiles.add(f.getPath());
                        }
                    }
                }

        }

        private boolean isTxtFile(File f) {
            String name = f.getName();
            return name.endsWith(".xml") || name.endsWith(".smali")
                    || name.endsWith(".txt");
        }

        @Override
        protected List<String> doInBackground(Object... params) {
            File root = new File(baseFolder);
            for (String filename : filenameList) {
                File f = new File(root, filename);
                if (!f.exists()) { // Not exist
                    continue;
                }
                if (f.isDirectory()) {
                    searchFolder(f);
                } else if (isTxtFile(f)) { // Regular text file
                    if (fileContainsKeyword(f)) {
                        SearchTextDialog.this.matchedFiles.add(f.getPath());
                    }
                }
            }

            return matchedFiles;
        }

        @Override
        protected void onPostExecute(List<String> result) {
            showMatchedFiles();
        }
    }

    // Search the keyword asynchronously in one file
    private class AsyncFileSearchTask
            extends AsyncTask<Object, Void, TxtSearchResult> {

        private String filePath;
        private String keyword;
        private int groupPosition;

        public AsyncFileSearchTask(String filePath, String keyword,
                                   int groupPosition) {
            this.filePath = filePath;
            this.keyword = keyword;
            this.groupPosition = groupPosition;
        }

        @SuppressLint("DefaultLocale")
        @Override
        protected TxtSearchResult doInBackground(Object... params) {
            TxtSearchResult result = new TxtSearchResult();
            result.filePath = filePath;
            result.keyword = keyword;
            String lcKeyword = keyword.toLowerCase();

            BufferedReader br = null;
            try {
                List<MatchedLineItem> matchedItems = new ArrayList<MatchedLineItem>();
                br = new BufferedReader(
                        new InputStreamReader(new FileInputStream(filePath)));

                // Search the keyword line by line
                int lineIndex = 1;
                String line = br.readLine();
                while (line != null) {
                    int position = -1;
                    if (caseSensitive) {
                        position = line.indexOf(keyword);
                    } else {
                        position = line.toLowerCase().indexOf(lcKeyword);
                    }
                    if (position != -1) {
                        matchedItems.add(
                                new MatchedLineItem(lineIndex, position, line));
                    }
                    lineIndex += 1;
                    line = br.readLine();
                }

                result.matchList = matchedItems;
            } catch (Exception e) {
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                    }
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(TxtSearchResult result) {
            // unfold the list
            if (result.matchList != null) {
                listAdapter.addSearchResult(result.filePath, result.matchList);
            }
            listView.expandGroup(groupPosition);
        }
    }
}
