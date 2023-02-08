package com.gmail.heagoo.folderlist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.FileUtil;
import com.gmail.heagoo.common.InputUtil;
import com.gmail.heagoo.folderlist.util.OpenFiles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FolderListWrapper implements OnItemClickListener,
        OnItemLongClickListener {

    private Context ctx;
    private ListView listView;
    private String rootPath;
    private String curPath;

    private FolderListAdapter adapter;
    private IListEventListener listener;

//	public FolderListWrapper(Context ctx, ListView listView, String path,
//			IListEventListener listener, IListItemProducer producer) {
//		this(ctx, listView, path, path, listener, producer);
//	}

    public FolderListWrapper(Context ctx, ListView listView, String curPath,
                             String rootPath, IListEventListener listener,
                             IListItemProducer producer) {
        this.ctx = ctx;
        this.listView = listView;
        this.curPath = curPath;
        this.rootPath = rootPath;
        this.listener = listener;

        init(producer);
    }

    public FolderListAdapter getAdapter() {
        return adapter;
    }

    private void init(IListItemProducer producer) {
        this.adapter = new FolderListAdapter(ctx, rootPath, curPath, producer);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                            long arg3) {
        List<FileRecord> fileList = new ArrayList<FileRecord>();
        String oldDir = adapter.getData(fileList);
        FileRecord rec = fileList.get(position);
        if (rec == null) {
            return;
        }

        if (rec.isDir) {
            String targetPath = null;
            if (rec.fileName.equals("..")) {
                int pos = oldDir.lastIndexOf('/');
                targetPath = oldDir.substring(0, pos);
            } else {
                targetPath = oldDir + "/" + rec.fileName;
            }
            adapter.openDirectory(targetPath);
        } else {
            String filePath = oldDir + "/" + rec.fileName;
            // When listener not deal with the opening, we will do it
            if (!listener.fileClicked(filePath)) {
                OpenFiles.openFile(ctx, filePath);
            }
        }

        String newDir = adapter.getData(null);
        if (!newDir.equals(oldDir)) {
            listener.dirChanged(newDir);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
                                   final int position, long id) {
        // The first item is always the parent folder
        if (position == 0)
            return true;
        parent.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            public void onCreateContextMenu(ContextMenu menu, View v,
                                            ContextMenuInfo menuInfo) {
                // Delete
                MenuItem item1 = menu.add(0, Menu.FIRST, 0, R.string.delete);
                item1.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        deleteFile(position);
                        return true;
                    }
                });

                // Rename
                MenuItem item2 = menu.add(0, Menu.FIRST + 1, 0, R.string.rename);
                item2.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        showRenameDlg(position);
                        return true;
                    }
                });

                // New File
                MenuItem item3 = menu.add(0, Menu.FIRST + 2, 0, R.string.new_file);
                item3.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        createFile();
                        return true;
                    }
                });

                if (listener != null) {
                    listener.itemLongClicked(menu, v, menuInfo);
                }
            }
        });
        return false;
    }

    private void showRenameDlg(int position) {
        AlertDialog.Builder renameDlg = new AlertDialog.Builder(ctx);

        renameDlg.setTitle(R.string.rename);
        renameDlg.setMessage(R.string.pls_input_filename);

        // Set an EditText view to get user input
        final EditText input = new EditText(ctx);
        List<FileRecord> records = new ArrayList<FileRecord>();
        final String dirPath = adapter.getData(records);
        FileRecord fr = records.get(position);
        if (fr == null) {
            return;
        }
        final String fileName = fr.fileName;
        input.setText(fileName);
        renameDlg.setView(input);

        renameDlg.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String newName = input.getText().toString();
                        boolean ret = doRename(dirPath, fileName, newName);
                        if (ret) {
                            adapter.fileRenamed(dirPath, fileName, newName);
                            listener.fileRenamed(dirPath, fileName, newName);
                        }
                    }
                });

        renameDlg.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

        renameDlg.show();

    }

    protected boolean doRename(String dirPath, String fileName, String newName) {
        boolean ret = false;
        File newFile = new File(dirPath + "/" + newName);
        if (newFile.exists()) {
            String tip = ctx.getResources().getString(
                    R.string.file_already_exist);
            String msg = String.format(tip, newName);
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
        } else {
            ret = new File(dirPath + "/" + fileName).renameTo(newFile);
            String strRename = ctx.getResources().getString(R.string.rename);
            String strResult = ctx.getResources().getString(
                    ret ? R.string.succeed : R.string.failed);

            Toast.makeText(ctx, strRename + " " + strResult, Toast.LENGTH_SHORT)
                    .show();
        }
        return ret;
    }

    private void createFile() {
        final String dirPath = adapter.getData(null);

        AlertDialog.Builder inputDlg = new AlertDialog.Builder(ctx);
        inputDlg.setTitle(R.string.new_file);
        inputDlg.setMessage(R.string.pls_input_filename);

        // Set an EditText view to get user input
        final EditText input = new EditText(ctx);
        InputFilter filter = InputUtil.getFileNameFilter();
        input.setFilters(new InputFilter[]{filter});
        inputDlg.setView(input);

        inputDlg.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        String name = input.getText().toString();
                        name = name.trim();
                        if ("".equals(name)) {
                            Toast.makeText(ctx,
                                            R.string.empty_input_tip, Toast.LENGTH_LONG)
                                    .show();
                        } else {
                            boolean succeed = false;
                            String errMessage = null;

                            // Try to create a new file in current directory
                            File dir = new File(dirPath);
                            File newFile = new File(dir, name);
                            try {
                                succeed = newFile.createNewFile();
                                if (succeed) {
                                    // Update list view
                                    adapter.openDirectory(dirPath);
                                } else {
                                    errMessage = ctx.getString(R.string.failed_create_file);
                                }
                            } catch (IOException e) {
                                String fmt = ctx.getString(R.string.general_error);
                                errMessage = String.format(fmt, e.getMessage());
                            }
                            if (!succeed) {
                                Toast.makeText(ctx, errMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });

        inputDlg.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        // Canceled.
                    }
                });

        inputDlg.show();
    }

    private void deleteFile(int position) {
        List<FileRecord> records = new ArrayList<FileRecord>();
        final String dirPath = adapter.getData(records);
        FileRecord fr = records.get(position);
        if (fr == null) {
            return;
        }

        String fileName = fr.fileName;
        String path = dirPath + "/" + fr.fileName;
        boolean ret = deleteFile(path);
        if (ret) {
            adapter.fileDeleted(dirPath, fileName);
            listener.fileDeleted(dirPath, fileName);
        }
    }

    private boolean deleteFile(String path) {
        boolean ret = false;
        File file = new File(path);

        if (file.exists()) {
            if (file.isFile()) {
                ret = file.delete();
            } else {
                try {
                    FileUtil.deleteAll(file);
                    ret = true;
                } catch (IOException e) {
                }
                // Do not use following code, as it makes file list hang
//                String deleteCmd = "rm -rf " + path;
//                Runtime runtime = Runtime.getRuntime();
//                try {
//                    runtime.exec(deleteCmd);
//                    ret = true;
//                } catch (IOException e) {
//                }
            }
        }

        return ret;
    }

    public void openDirectory(String dir) {
        String oldDir = adapter.getData(null);

        // It may fail, so we need to check after the call
        adapter.openDirectory(dir);

        String newDir = adapter.getData(null);

        if (!newDir.equals(oldDir)) {
            listener.dirChanged(newDir);
        }
    }
}
