package com.gmail.heagoo.apkeditor;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.AsyncTask;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.base.R;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// This activity is called from ApkInfoActivity::searchInResourceFiles
public class SearchFilenameDialog extends Dialog implements
        android.view.View.OnClickListener, ResSelectionChangeListener,
        OnItemClickListener, OnItemLongClickListener {

    // Record modified files
    private final Set<String> modifiedFiles = new HashSet<>();
    WeakReference<ApkInfoActivity> activityRef;
    private TextView titleTv;
    private View selectionHeaderView;
    private TextView selectionTipTv;
    private ListView listView;
    private MatchedFilenameAdapter listAdapter;
    private LinearLayout searchingLayout;
    private Button closeBtn;
    private Button deleteBtn;
    private View doneMenu;
    private View selectMenu;
    private String searchFolder;
    private List<String> filenameList;
    private String keyword;
    private boolean caseSensitive;
    // Record matched files
    private ArrayList<String> matchedFiles = new ArrayList<String>();

    public SearchFilenameDialog(ApkInfoActivity activity, String searchFolder,
                                List<String> filenameList, String keyword, boolean caseSensitive) {
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

        init(activity);
    }

    private void init(ApkInfoActivity activity) {
// sawsem theme
        int resId = R.layout.dlg_filename_searchret;
//        switch (GlobalConfig.instance(activity).getThemeId()) {
//            case GlobalConfig.THEME_DARK_DEFAULT:
//                resId = R.layout.dlg_filename_searchret_dark;
//                break;
//            case GlobalConfig.THEME_DARK_RUSSIAN:
//                resId = R.layout.dlg_filename_searchret_dark_ru;
//                break;
//        }
        View view = LayoutInflater.from(activity).inflate(resId, null);

        this.titleTv = (TextView) view.findViewById(R.id.title);
        this.selectionHeaderView = view.findViewById(R.id.res_header_selection);
        this.selectionTipTv = (TextView) view.findViewById(R.id.selection_tip);
        this.listView = (ListView) view.findViewById(R.id.file_list);
        this.searchingLayout = (LinearLayout) view
                .findViewById(R.id.searching_layout);

        this.doneMenu = view.findViewById(R.id.menu_done);
        this.selectMenu = view.findViewById(R.id.menu_select);
        this.doneMenu.setOnClickListener(this);
        this.selectMenu.setOnClickListener(this);

        this.closeBtn = (Button) view.findViewById(R.id.btn_close);
        this.deleteBtn = (Button) view.findViewById(R.id.btn_delete);
        this.closeBtn.setOnClickListener(this);
        this.deleteBtn.setOnClickListener(this);

        listView.setVisibility(View.INVISIBLE);

        // Start searching task
        new AsyncFolderSearchTask(searchFolder, filenameList, keyword).execute();

        this.setContentView(view);
    }

    // @Override
    // protected void onActivityResult(int requestCode, int resultCode, Intent
    // data) {
    // switch (requestCode) {
    //
    // case 2: // Open manifest in a new window, manifest search
    // if (resultCode != 0) {
    // String xmlPath = data.getStringExtra("xmlPath");
    // this.modifiedFiles.add(xmlPath);
    //
    // setResult();
    // }
    // break;
    // }
    // }

    // Transfer modified files to parent activity
    // private void setResult() {
    // Intent intent = new Intent();
    // StringBuffer sb = new StringBuffer();
    // for (String path : modifiedFiles) {
    // sb.append(path);
    // sb.append("\n");
    // }
    // sb.deleteCharAt(sb.length() - 1);
    // intent.putExtra("ModifiedFiles", sb.toString());
    //
    // this.setResult(1, intent);
    // }

    private void showMatchedFiles() {
        // Set title
        String format = activityRef.get().getString(R.string.str_files_found);
        String text = String.format(format, matchedFiles.size(), keyword);
        titleTv.setText(text);

        this.listAdapter = new MatchedFilenameAdapter(activityRef.get(), this,
                searchFolder, matchedFiles);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);

        // Switch to list view
        listView.setVisibility(View.VISIBLE);
        searchingLayout.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            this.dismiss();
        } else if (id == R.id.btn_delete) {
            deleteSelectedFiles();
        } else if (id == R.id.menu_done) {
            listAdapter.selectNone();
            this.showNonSelectView();
        } else if (id == R.id.menu_select) {
            if (listAdapter.isAllSelected()) {
                listAdapter.selectNone();
                this.showNonSelectView();
            } else {
                listAdapter.selectAll();
            }
        }
    }

    private void deleteSelectedFiles() {
        List<Integer> selected = listAdapter.getSeletedItems();
        deleteFilesByIndex(selected);
    }

    private void deleteFilesByIndex(List<Integer> indexes) {
        ResListAdapter resManager = activityRef.get().getResListAdapter();

        // Use ResListAdapter to delete it
        for (int index : indexes) {
            String filepath = this.matchedFiles.get(index);
            int pos = filepath.lastIndexOf('/');
            String dirPath = (pos != -1) ? filepath.substring(0, pos) : "";
            String fileName = filepath.substring(pos + 1);
            resManager.deleteFile(dirPath, fileName, false);
        }

        // Collect file list which not deleted
        ArrayList<String> fileList = new ArrayList<String>();
        for (int i = 0; i < matchedFiles.size(); ++i) {
            if (!indexes.contains(i)) {
                fileList.add(matchedFiles.get(i));
            }
        }

        // Update matched files
        this.matchedFiles = fileList;
        listAdapter.resetFileList(this.matchedFiles, indexes);

        if (listAdapter.isNonSelected()) {
            showNonSelectView();
        }
    }

    private void showNonSelectView() {
        titleTv.setVisibility(View.VISIBLE);
        selectionHeaderView.setVisibility(View.GONE);
        deleteBtn.setVisibility(View.GONE);

        // As file count may change, need to update the title
        String format = activityRef.get().getString(R.string.str_files_found);
        String text = String.format(format, matchedFiles.size(), keyword);
        titleTv.setText(text);
    }

    @Override
    public void selectionChanged(Set<Integer> selected) {
        // No selection at all
        if (selected.isEmpty()) {
            showNonSelectView();
        } else {
            String text = String.format(
                    activityRef.get().getString(R.string.num_items_selected),
                    selected.size());
            selectionTipTv.setText(text);
            titleTv.setVisibility(View.GONE);
            selectionHeaderView.setVisibility(View.VISIBLE);
            deleteBtn.setVisibility(View.VISIBLE);
        }
    }

    // Click on list item
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        if (position >= matchedFiles.size()) {
            return;
        }

        String filepath = matchedFiles.get(position);
        int pos = filepath.lastIndexOf("/");
        if (pos != -1) {
            String directory = filepath.substring(0, pos);
            String fileName = filepath.substring(pos + 1);
            activityRef.get().openFile(directory, fileName, false);
        }
    }

    private void deleteItem(int position) {
        List<Integer> indexes = new ArrayList<Integer>();
        indexes.add(position);
        this.deleteFilesByIndex(indexes);
    }

    private void extractItem(int position) {
        if (position < matchedFiles.size()) {
            String filepath = matchedFiles.get(position);
            activityRef.get().extractFileOrDir(filepath);
        }
    }

    private void replaceItem(int position) {
        if (position < matchedFiles.size()) {
            String filepath = matchedFiles.get(position);
            activityRef.get().replaceFile(filepath,
                    new SomethingChangedListener() {
                        @Override
                        public void somethingChanged() {
                            listAdapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    // Long click on list item
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
                                   final int position, long id) {
        parent.setOnCreateContextMenuListener(
                new OnCreateContextMenuListener() {
                    public void onCreateContextMenu(ContextMenu menu, View v,
                                                    ContextMenuInfo menuInfo) {

                        // Delete
                        MenuItem item1 = menu.add(0, Menu.FIRST, 0,
                                R.string.delete);
                        item1.setOnMenuItemClickListener(
                                new OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(
                                            MenuItem item) {
                                        deleteItem(position);
                                        return true;
                                    }
                                });
                        // Extract
                        MenuItem item2 = menu.add(0, Menu.FIRST + 1, 0,
                                R.string.extract);
                        item2.setOnMenuItemClickListener(
                                new OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(
                                            MenuItem item) {
                                        extractItem(position);
                                        return true;
                                    }
                                });
                        // Replace the file
                        MenuItem item3 = menu.add(0, Menu.FIRST + 2, 0,
                                R.string.replace);
                        OnMenuItemClickListener listener = new OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                replaceItem(position);
                                return true;
                            }
                        };
                        item3.setOnMenuItemClickListener(listener);
                    }
                });

        return false;
    }

    // Search all the files inside the folder
    private class AsyncFolderSearchTask
            extends AsyncTask<Object, Void, List<String>> {

        private String baseFolder;
        private List<String> filenameList;
        private String keyword;
        private String lowerCaseKeyword;

        @SuppressLint("DefaultLocale")
        public AsyncFolderSearchTask(String folderPath,
                                     List<String> filenameList, String keyword) {
            this.baseFolder = folderPath;
            this.filenameList = filenameList;
            this.keyword = keyword;
            this.lowerCaseKeyword = keyword.toLowerCase();
        }

        // Check the file whether contains the keyword
        private boolean fileMatches(String filename) {
            boolean bContain = false;
            if (caseSensitive) {
                bContain = filename.contains(keyword);
            } else {
                bContain = filename.toLowerCase()
                        .contains(lowerCaseKeyword);
            }
            return bContain;
        }

        private void searchFolder(File folderFile) {
            File[] files = folderFile.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        searchFolder(f);
                    } else {
                        if (fileMatches(f.getName())) {
                            SearchFilenameDialog.this.matchedFiles
                                    .add(f.getPath());
                        }
                    }
                }
            }
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
                } else { // Regular file
                    if (fileMatches(filename)) {
                        SearchFilenameDialog.this.matchedFiles.add(f.getPath());
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
}
