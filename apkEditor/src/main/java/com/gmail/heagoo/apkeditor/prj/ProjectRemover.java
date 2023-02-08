package com.gmail.heagoo.apkeditor.prj;

import android.content.Context;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.ProcessingDialog;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.FileUtil;
import com.gmail.heagoo.common.SDCard;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

// Help to remove a project
class ProjectRemover implements ProcessingDialog.ProcessingInterface {
    private WeakReference<ProjectListActivity> actRef;
    private ProjectListAdapter.ItemInfo itemInfo;
    private boolean result;
    private String errMessage;

    ProjectRemover(ProjectListActivity activity, ProjectListAdapter.ItemInfo itemInfo) {
        this.actRef = new WeakReference<>(activity);
        this.itemInfo = itemInfo;
        this.result = false;
    }

    @Override
    public void process() throws Exception {

        // Remove the decoded directory
        try {
            FileUtil.deleteAll(new File(itemInfo.decodeDirectory));
        } catch (IOException ignored) {
        }

        // Remove the project index files
        try {
            String projectFolder = SDCard.makeDir(actRef.get(), ".projects");
            FileUtil.deleteAll(new File(projectFolder + itemInfo.name));
            this.result = true;
        } catch (Exception e) {
            this.errMessage = e.getMessage();
        }
    }

    @Override
    public void afterProcess() {
        if (this.result) {
            Context ctx = actRef.get();
            Toast.makeText(ctx,
                    String.format(ctx.getString(R.string.project_removed), itemInfo.name),
                    Toast.LENGTH_LONG).show();
            actRef.get().updateProjectList();
        } else if (errMessage != null) {
            Toast.makeText(actRef.get(),
                    String.format(actRef.get().getString(R.string.general_error), errMessage),
                    Toast.LENGTH_LONG).show();
        }
    }
}
