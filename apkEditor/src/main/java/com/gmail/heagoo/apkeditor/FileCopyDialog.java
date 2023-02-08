package com.gmail.heagoo.apkeditor;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.FileUtil;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.common.ZipUtil;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Used for extract function
// It can copy from srcPath to dstPath
public class FileCopyDialog extends Dialog implements
        android.view.View.OnClickListener {

    // Source and target
    private List<CopySource> copySources;
    private String apkPath;
    private String targetFolder; // In general, not end with "/"

    // Used to get entry name from file path
    private String decodeRootPath;

    // Get real entry in apk by entry name
    private Map<String, String> entryMapping;

    private MyHandler handler = new MyHandler(this);

    private View view;

    private String succeedStr;
    private String failedStr;

    // File will automatically renamed(0) or overwrite
    private int fileRenameOption;

    // The real target path of the copied file/dir
    private String savedFilePath;

    // This constructor means may copy from file, and may copy from zip
    public FileCopyDialog(Context context, String filePath,
                          String targetFolder, String apkPath, String decodeRootPath,
                          Map<String, String> entryMapping, int unused) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        CopySource source = new CopySource();
        source.path = filePath;
        source.isInApk = false;
        source.isDir = new File(filePath).isDirectory();

        this.copySources = new ArrayList<>();
        this.copySources.add(source);
        this.apkPath = apkPath;
        this.decodeRootPath = decodeRootPath;
        this.entryMapping = entryMapping;

        if (targetFolder != null) {
            this.targetFolder = targetFolder;
        } else {
            this.targetFolder = SDCard.getRootDirectory() + "/ApkEditor";
        }

        init(context);
    }

    public FileCopyDialog(Context context, String apkPath,
                          String decodeRootPath, Map<String, String> fileEntry2ZipEntry,
                          List<CopySource> copySources, String targetFolder) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        this.apkPath = apkPath;
        this.decodeRootPath = decodeRootPath;
        this.entryMapping = fileEntry2ZipEntry;
        this.copySources = copySources;
        this.targetFolder = targetFolder;

        init(context.getApplicationContext());
    }

    public static String getName(String path) {
        int pos = path.lastIndexOf('/');
        return path.substring(pos + 1);
    }

    // Get the target saving file/dir which not exist, by adding (1), (2), etc
    public static File getTargetNonExistFile(String path, boolean isDir) {
        int index = 1;
        String folder = null;
        String name = null;
        String fileType = null;
        if (!isDir) {
            int slashPos = path.lastIndexOf('/');
            folder = path.substring(0, slashPos + 1);
            String filename = path.substring(slashPos + 1);

            name = filename;
            fileType = "";
            int dotPos = filename.lastIndexOf('.');
            if (dotPos != -1) {
                name = filename.substring(0, dotPos);
                fileType = filename.substring(dotPos);
            }
        }

        while (true) {
            String testingPath = isDir ? path + "(" + index + ")" :
                    folder + name + "(" + index + ")" + fileType;
            File node = new File(testingPath);
            if (!node.exists()) {
                return node;
            }
            index += 1;
        }
    }

    private void init(Context context) {
        this.fileRenameOption = SettingActivity.getFileRenameOption(context);

        Resources res = context.getResources();
        this.succeedStr = res.getString(R.string.save_succeed_1);
        this.failedStr = res.getString(R.string.failed_1);

        int layoutId = R.layout.dlg_extractres;
        // sawsem theme
//        switch (GlobalConfig.instance(context).getThemeId()) {
//            case GlobalConfig.THEME_DARK_DEFAULT:
//                layoutId = R.layout.dlg_extractres_dark;
//                break;
//            case GlobalConfig.THEME_DARK_RUSSIAN:
//                layoutId = R.layout.dlg_extractres_dark_ru;
//                break;
//        }
        this.view = LayoutInflater.from(context).inflate(layoutId, null);
        setContentView(view);
        // getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button closeBtn = (Button) view.findViewById(R.id.close_button);
        closeBtn.setOnClickListener(this);
    }

    // File copy succeed
    public void succeed() {
        TextView resultTv = (TextView) view.findViewById(R.id.result_tv);

        // When just copy one file, show the full path
        if (copySources.size() == 1) {
            resultTv.setText(String.format(succeedStr, savedFilePath));
        } else {
            resultTv.setText(String.format(succeedStr, targetFolder));
        }

        view.findViewById(R.id.layout_done).setVisibility(View.VISIBLE);
    }

    // File copy failed
    public void failed(String msg) {
        TextView resultTv = (TextView) view.findViewById(R.id.result_tv);
        resultTv.setText(String.format(failedStr, msg));

        view.findViewById(R.id.layout_done).setVisibility(View.VISIBLE);
    }

    // Start file copy thread and show the dialog
    public void show() {
        new Thread() {
            @Override
            public void run() {
                try {
                    for (CopySource source : copySources) {
                        // The file/directory already decoded
                        if (!source.isInApk) {
                            copyFiles(source);
                        } else {
                            extractFiles(source);
                        }
                    }
                    handler.sendEmptyMessage(0);
                } catch (Exception e) {
                    handler.setErrorMessage(e.getMessage());
                    handler.sendEmptyMessage(1);
                }
            }
        }.start();
        super.show();
    }

    // To extract file/directory from the apk/zip file
    protected void extractFiles(CopySource source) throws Exception {
        String targetPath = targetFolder + "/" + getName(source.path);
        boolean bExist = new File(targetPath).exists();
        if (bExist && fileRenameOption == SettingActivity.EXTRACT_AUTORENAME) {
            targetPath = getTargetNonExistPath(targetPath, source.isDir);
        }

        if (source.isDir) {
            ZipUtil.unzipDirectory(apkPath, source.path, targetPath);
        } else {
            ZipUtil.unzipFileTo(apkPath, source.path, targetPath);
        }

        this.savedFilePath = targetPath;
    }

    // Just copy the file/directory, as it is already decoded
    protected void copyFiles(CopySource source) throws Exception {
        String targetPath = targetFolder + "/" + getName(source.path);
        File srcFile = new File(source.path);
        // Copy directory
        if (srcFile.isDirectory()) {
            File outputDirFile = new File(targetPath);
            if (outputDirFile.exists()) {
                if (fileRenameOption == SettingActivity.EXTRACT_AUTORENAME) {
                    outputDirFile = createDirByAddSuffix(targetPath);
                }
            } else {
                outputDirFile.mkdirs();
            }
            copyDirectory(srcFile, outputDirFile);
            this.savedFilePath = outputDirFile.getPath();
        }
        // Copy file: just copy 1 file
        else {
            File dst = new File(targetPath);
            if (dst.exists()) {
                if (fileRenameOption == SettingActivity.EXTRACT_AUTORENAME) {
                    dst = getTargetNonExistFile(targetPath, false);
                }
            }
            doFileCopy(new File(source.path), dst);
            this.savedFilePath = dst.getPath();
        }
    }

    // Get the target saving path which not exist, by adding (1), (2), etc
    private String getTargetNonExistPath(String path, boolean isDir) {
        return getTargetNonExistFile(path, isDir).getPath();
    }

    // Create a new directory by adding (1) (2), ...
    private File createDirByAddSuffix(String path) {
        File f = getTargetNonExistFile(path, true);
        f.mkdirs();
        return f;
    }

    // Copy all content in the directory
    // Assume the dstDir already exists
    private void copyDirectory(File srcDir, File dstDir) throws Exception {
        File[] files = srcDir.listFiles();
        if (files != null)
            for (File f : files) {
                if (f.isFile()) {
                    File t = new File(dstDir, f.getName());
                    doFileCopy(f, t);
                } else {
                    File dir = new File(dstDir, f.getName());
                    dir.mkdir();
                    copyDirectory(f, dir);
                }
            }
    }

    // This is a customized copy
    // Note: if it is png/jpg file, will extract from zip
    private void doFileCopy(File from, File to) throws Exception {
        String filename = from.getName();
        // It is the common image
        if (this.apkPath != null
                && (filename.endsWith(".jpg") || (filename.endsWith(".png")
                && !filename.endsWith(".9.png")))) {
            String entryName = from.getPath().substring(
                    decodeRootPath.length() + 1);
            String realEntry = entryMapping.get(entryName);
            if (realEntry != null) {
                entryName = realEntry;
            }
            ZipUtil.unzipFileTo(this.apkPath, entryName, to.getPath());
        } else {
            FileUtil.copyFile(from, to);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.close_button) {
            this.dismiss();
        }
    }

    public static class CopySource {
        public String path; // File path or entry path
        public boolean isDir;
        public boolean isInApk;
    }

    private static class MyHandler extends Handler {
        private WeakReference<FileCopyDialog> dlgRef;
        private String errMsg;

        public MyHandler(FileCopyDialog dlg) {
            this.dlgRef = new WeakReference<>(dlg);
        }

        public void setErrorMessage(String msg) {
            this.errMsg = msg;
        }

        @Override
        public void handleMessage(Message msg) {
            FileCopyDialog dlg = dlgRef.get();
            if (dlg == null) {
                return;
            }
            switch (msg.what) {
                case 0:
                    dlg.succeed();
                    break;
                case 1:
                    dlg.failed(errMsg);
                    break;
            }
        }
    }
}
