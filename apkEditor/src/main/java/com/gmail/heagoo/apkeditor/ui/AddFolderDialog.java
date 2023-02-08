package com.gmail.heagoo.apkeditor.ui;

import android.app.Dialog;
import android.content.Context;
import android.text.InputFilter;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.gmail.heagoo.apkeditor.FileSelectDialog;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.InputUtil;

import java.io.File;
import java.lang.ref.WeakReference;


// Create a new directory or import a directory
public class AddFolderDialog extends Dialog implements View.OnClickListener, FileSelectDialog.IFileSelection {
    private final AddFolderCallback callback;
    private final WeakReference<Context> contextRef;
    private boolean showImportFolder;

    private View newDivider;
    private View importDivider;
    private View newFolderLayout;
    private View importFolderLayout;

    private EditText folderNameEt;
    private EditText folderPathEt;

    private boolean addFolder = true;

    ////////////////////////////////////////////////////////////////////////////////
    // Callback functions for folder selection

    public AddFolderDialog(@NonNull final Context context, AddFolderCallback callback, boolean showImportFolder) {
        super(context);

        this.contextRef = new WeakReference<>(context);
        this.callback = callback;
        this.showImportFolder = showImportFolder;
        setContentView(R.layout.dlg_add_folder);

        init();
    }

    @Override
    public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
        folderPathEt.setText(filePath);
    }

    @Override
    public boolean isInterestedFile(String filename, String extraStr) {
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public String getConfirmMessage(String filePath, String extraStr) {
        return null;
    }

    private void init() {
        TextView newTv = (TextView) findViewById(R.id.tv_new_folder);
        TextView importTv = (TextView) findViewById(R.id.tv_import_folder);
        if (!showImportFolder) {
            importTv.setVisibility(View.GONE);
        }

        this.newDivider = findViewById(R.id.divider1);
        this.importDivider = findViewById(R.id.divider2);
        this.newFolderLayout = findViewById(R.id.layout_new);
        this.importFolderLayout = findViewById(R.id.layout_import);

        folderNameEt = (EditText) findViewById(R.id.et_folder_name);
        folderPathEt = (EditText) findViewById(R.id.et_folder_path);

        InputFilter filter = InputUtil.getFileNameFilter();
        folderNameEt.setFilters(new InputFilter[]{filter});

        newTv.setOnClickListener(this);
        importTv.setOnClickListener(this);
        findViewById(R.id.btn_browse).setOnClickListener(this);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        findViewById(R.id.btn_confirm).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.tv_new_folder) {
            showNewFolderView();
        } else if (id == R.id.tv_import_folder) {
            showImportFolderView();
        } else if (id == R.id.btn_cancel) {
            this.dismiss();
        } else if (id == R.id.btn_confirm) {
            confirm();
        } else if (id == R.id.btn_browse) {
            browse();
        }
    }

    // Browse SD card to select a folder
    private void browse() {
        Context ctx = contextRef.get();
        String strTitle = ctx.getString(R.string.select_imported_folder);
        new FileSelectDialog(ctx, this,
                "", "", strTitle,
                true, false, false,
                "import_folder").show();
    }

    // Confirm to create or import a directory
    private void confirm() {
        if (addFolder) {
            String name = folderNameEt.getText().toString();
            name = name.trim();
            if ("".equals(name)) {
                Toast.makeText(contextRef.get(),
                        R.string.empty_input_tip, Toast.LENGTH_LONG).show();
            } else {
                callback.addFolder(name);
                this.dismiss();
            }
        }
        // To import a folder
        else {
            String path = folderPathEt.getText().toString();
            path = path.trim();
            if ("".equals(path)) {
                Toast.makeText(contextRef.get(),
                        R.string.empty_input_tip, Toast.LENGTH_LONG).show();
            } else if (!new File(path).exists()) {
                String fmt = contextRef.get().getString(R.string.error_path_xxx_not_exist);
                String message = String.format(fmt, path);
                Toast.makeText(contextRef.get(), message, Toast.LENGTH_LONG).show();
            } else {
                callback.importFolder(path);
                this.dismiss();
            }
        }
    }

    private void showNewFolderView() {
        addFolder = true;
        newDivider.setVisibility(View.VISIBLE);
        importDivider.setVisibility(View.INVISIBLE);
        newFolderLayout.setVisibility(View.VISIBLE);
        importFolderLayout.setVisibility(View.INVISIBLE);
    }

    private void showImportFolderView() {
        addFolder = false;
        newDivider.setVisibility(View.INVISIBLE);
        importDivider.setVisibility(View.VISIBLE);
        newFolderLayout.setVisibility(View.INVISIBLE);
        importFolderLayout.setVisibility(View.VISIBLE);
    }

    public interface AddFolderCallback {
        void addFolder(String folderName);

        void importFolder(String folderPath);
    }
}

