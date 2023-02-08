package com.gmail.heagoo.apkeditor.se;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.FileCopyDialog;
import com.gmail.heagoo.apkeditor.FileSelectDialog;
import com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection;
import com.gmail.heagoo.apkeditor.GlobalConfig;
import com.gmail.heagoo.apkeditor.ProcessingDialog;
import com.gmail.heagoo.apkeditor.TextEditor;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apklib.AXMLPrinter;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.ImageZoomer;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.imageviewlib.ViewZipImageActivity;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipFileListAdapter extends BaseAdapter implements
        OnItemClickListener, OnItemLongClickListener, OnClickListener,
        IFileSelection, ProcessingDialog.ProcessingInterface {

    // Use to do the sorting
    Comparator<FileInfo> comparator = new Comparator<FileInfo>() {
        @Override
        public int compare(FileInfo fi1, FileInfo fi2) {
            if (fi1.isDir) {
                if (fi2.isDir) {
                    return fi1.filename.compareTo(fi2.filename);
                } else {
                    return -1;
                }
            } else {
                if (fi2.isDir) {
                    return 1;
                } else {
                    return fi1.filename.compareTo(fi2.filename);
                }
            }
        }
    };
    private Activity ctx;
    private IDirChanged dirChangeIf;
    private String curDir;
    private List<FileInfo> curFileList;
    private Map<String, List<FileInfo>> dir2Files;
    // Record all the replaces (entry name -> file path)
    private Map<String, String> fileReplaces = new HashMap<>();
    // For AXML editing
    private boolean xmlEditMode = false;
    // Image cache
    private LruCache<String, Bitmap> imageBitmaps = new LruCache<String, Bitmap>(
            32) {
        protected void entryRemoved(boolean evicted, String key,
                                    Bitmap oldValue, Bitmap newValue) {
            oldValue.recycle();
        }
    };
    // Help to resolve the image
    private ZipHelper zipHelper;
    private ZipFile zfile;
    private ZipImageZoomer zipImageZoomer;

    private int layoutId;

    /* interface implementation for ProcessingDialog */
    private boolean convertSucceed;
    private String clickedEntryPath;
    private String decodedXmlPath;

    public ZipFileListAdapter(Activity ctx, IDirChanged dirChangeIf,
                              ZipHelper zipHelper, boolean xmlEditMode) {
        this(ctx, dirChangeIf, zipHelper);
        this.xmlEditMode = xmlEditMode;
    }

    public ZipFileListAdapter(Activity ctx, IDirChanged dirChangeIf,
                              ZipHelper zipHelper) {
        this.ctx = ctx;
        this.curDir = "/";
        this.dirChangeIf = dirChangeIf;
        this.dir2Files = zipHelper.dir2Files;
        this.zipHelper = zipHelper;

        // Sort the root dir
        curFileList = dir2Files.get(curDir);
        Collections.sort(curFileList, comparator);

        this.layoutId = R.layout.item_zipfile;
        // sawsem theme
//        switch (GlobalConfig.instance(ctx).getThemeId()) {
//            case GlobalConfig.THEME_DARK_DEFAULT:
//                this.layoutId = R.layout.item_zipfile_dark;
//                break;
//            case GlobalConfig.THEME_DARK_RUSSIAN:
//                this.layoutId = R.layout.item_zipfile_dark_ru;
//                break;
//        }

        try {
            this.zfile = new ZipFile(zipHelper.getFilePath());
            this.zipImageZoomer = new ZipImageZoomer(zfile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }

    public static boolean isImageFile(String filename) {
        return filename.endsWith(".png") || filename.endsWith(".jpg")
                || filename.endsWith(".jpeg") || filename.endsWith(".gif");
    }

    public static boolean isXmlFIle(String filename) {
        return filename.endsWith(".xml");
    }

    // Extract file from a path, not a entry
    public static void extractFromZip(Activity ctx, String zipFilePath, String entryName) {
        List<FileCopyDialog.CopySource> sources = new ArrayList<>();
        FileCopyDialog.CopySource s = new FileCopyDialog.CopySource();
        s.isInApk = true;
        s.isDir = false;
        s.path = entryName;
        sources.add(s);
        extractFromZip(ctx, zipFilePath, sources);
    }

    private static void extractFromZip(final Activity ctx, final String zipFilePath,
                                       final List<FileCopyDialog.CopySource> sources) {
        // Select a target folder to extract
        String dlgTitle = ctx.getString(R.string.select_folder);
        IFileSelection callback = new IFileSelection() {
            private WeakReference<Activity> ctxRef = new WeakReference<>(ctx);

            @Override
            // filePath is the target directory
            // extraStr is the source file/directory
            public void fileSelectedInDialog(
                    String filePath, String extraStr, boolean openFile) {
                FileCopyDialog dlg = new FileCopyDialog(
                        ctxRef.get(), zipFilePath, null, null, sources, filePath);
                dlg.show();
            }

            @Override
            public boolean isInterestedFile(String filename, String extraStr) {
                return true;
            }

            @Override
            public String getConfirmMessage(String filePath, String extraStr) {
                return null;
            }
        };

        FileSelectDialog dlg = new FileSelectDialog(ctx, callback, null, null,
                dlgTitle, true, false, false, null);
        dlg.show();
    }

    @Override
    public void process() throws Exception {
        InputStream input;
        FileOutputStream output;

        convertSucceed = false;

        if (zfile != null) {
            // The entry is already replaced
            String replaceFile = fileReplaces.get(clickedEntryPath);
            if (replaceFile != null) {
                input = new FileInputStream(replaceFile);
            } else {
                ZipEntry entry = zfile.getEntry(clickedEntryPath);
                input = zfile.getInputStream(entry);
            }

            decodedXmlPath = SDCard.makeWorkingDir(ctx)
                    + clickedEntryPath.replace('/', '_');
            output = new FileOutputStream(decodedXmlPath);

            AXMLPrinter printer = new AXMLPrinter();
            if (input != null && output != null) {
                convertSucceed = printer.convert(input, output);
            }

            // Clean up
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }
    }

    @Override
    public void afterProcess() {
        if (convertSucceed) {
            String apkPath = (zipHelper != null ? zipHelper.getFilePath() : null);
            Intent intent = TextEditor.getEditorIntent(ctx, decodedXmlPath, apkPath);
            ActivityUtil.attachParam(intent, "displayFileName", clickedEntryPath);
            ActivityUtil.attachParam(intent, "extraString", clickedEntryPath);
            ctx.startActivityForResult(intent, 0);
        } else {
            String fmt = ctx.getString(R.string.failed_to_parse_xml);
            String message = String.format(fmt, clickedEntryPath);
            Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public int getCount() {
        if (isRootDirectory()) {
            return curFileList.size();
        } else if (curFileList != null) {
            return curFileList.size() + 1;
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        if (isRootDirectory()) {
            return curFileList.get(position);
        } else if (curFileList != null) {
            return curFileList.get(position - 1);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (curFileList == null) {
            return null;
        }

        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(ctx).inflate(layoutId, null);

            viewHolder = new ViewHolder();
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.file_icon);
            viewHolder.filename = (TextView) convertView.findViewById(R.id.filename);
            viewHolder.desc1 = (TextView) convertView.findViewById(R.id.detail1);
            viewHolder.editMenu = convertView.findViewById(R.id.menu_edit);
            viewHolder.saveMenu = convertView.findViewById(R.id.menu_save);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        int index = (isRootDirectory() ? position : position - 1);
        if (index >= 0) {
            FileInfo fi = curFileList.get(index);
            viewHolder.filename.setText(fi.filename);

            boolean replaceable;
            // Directory
            if (fi.isDir) {
                viewHolder.icon.setImageResource(R.drawable.folderutil_folder);
                replaceable = false;
            }
            // For the binary, not allow to replace (also allow replace now)
            else if (isBinaryFile(fi.filename)) {
                viewHolder.icon.setImageResource(R.drawable.folderutil_file);
                replaceable = true;
            }
            // For the image, show icon
            else if (isImageFile(fi.filename)) {
                String entryPath = curDir.substring(1) + fi.filename;
                viewHolder.icon.setImageBitmap(getImageIcon(entryPath));
                replaceable = true;
            }
            // Other files
            else {
                viewHolder.icon.setImageResource(R.drawable.folderutil_file);
                replaceable = true;
            }

            // For AXML editing mode, does not show image buttons
            if (replaceable && !xmlEditMode) {
                viewHolder.editMenu.setVisibility(View.VISIBLE);
                viewHolder.editMenu.setId(index);
                viewHolder.editMenu.setOnClickListener(this);
                viewHolder.saveMenu.setVisibility(View.VISIBLE);
                viewHolder.saveMenu.setId(index + curFileList.size());
                viewHolder.saveMenu.setOnClickListener(this);
            } else {
                viewHolder.editMenu.setVisibility(View.GONE);
                viewHolder.saveMenu.setVisibility(View.GONE);
            }
        }
        // Show parent item
        else {
            viewHolder.filename.setText("..");
            viewHolder.icon.setImageResource(R.drawable.folderutil_up);
            viewHolder.editMenu.setVisibility(View.GONE);
            viewHolder.saveMenu.setVisibility(View.GONE);
        }

        // No description
        viewHolder.desc1.setVisibility(View.GONE);

        return convertView;
    }

    private boolean isBinaryFile(String filename) {
        // seems arsc has some other usage, so we deleted it from here
        return filename.endsWith(".xml") || filename.endsWith(".dex")
                || filename.endsWith(".MF")
                || filename.endsWith(".SF") || filename.endsWith(".RSA")
                || filename.endsWith(".so");
    }

    private Bitmap getImageIcon(String entryName) {
        // Get thumbnail for the image
        Bitmap bitmap;

        bitmap = imageBitmaps.get(entryName);

        if (bitmap == null) {
            String replaceFile = fileReplaces.get(entryName);
            // Get from APK/ZIP file
            if (replaceFile == null) {
                bitmap = zipImageZoomer.getImageThumbnail(entryName, 32, 32);
            }
            // Get from replace file
            else {
                ImageZoomer zoomer = new ImageZoomer();
                bitmap = zoomer.getImageThumbnail(replaceFile, 32, 32);
            }
            // Save to cache
            if (bitmap != null) {
                imageBitmaps.put(entryName, bitmap);
            }
        }

        return bitmap;
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                            long arg3) {

        int index = -1;
        boolean dirChanged = false;

        if (!isRootDirectory()) {
            // Click on the parent item
            if (position == 0) {
                curDir = getParentPath(curDir);
                this.curFileList = dir2Files.get(curDir);
                dirChanged = true;
            } else {
                index = position - 1;
            }
        } else {
            index = position;
        }

        if (index != -1) {
            FileInfo fi = curFileList.get(index);

            // Open the directory
            if (fi.isDir) {
                this.curDir += fi.filename + "/";
                this.curFileList = dir2Files.get(curDir);
                dirChanged = true;
            }

            // Click on the image file
            else if (isImageFile(fi.filename)) {
                viewImageFile(curDir.substring(1) + fi.filename);
            }
            // To edit AXML file
            else if (xmlEditMode && isXmlFIle(fi.filename)) {
                editAXML(curDir.substring(1) + fi.filename);
            }
        }

        if (dirChanged) {
            // Notify that the dir changed
            dirChangeIf.dirChanged(curDir);

            this.notifyDataSetChanged();
        }
    }

    // To view the image
    private void viewImageFile(String entryPath) {
        String replaceFile = fileReplaces.get(entryPath);
        Intent intent = new Intent(ctx, ViewZipImageActivity.class);
        ActivityUtil.attachParam(intent, "fullScreen", GlobalConfig.instance(ctx).isFullScreen());
        if (replaceFile == null) {
            ActivityUtil.attachParam(intent, "zipFilePath", zipHelper.getFilePath());
            ActivityUtil.attachParam(intent, "entryName", entryPath);
        } else {
            ActivityUtil.attachParam(intent, "imageFilePath", replaceFile);
        }
        ctx.startActivity(intent);
    }

    private void editAXML(String entryPath) {
        this.clickedEntryPath = entryPath;
        ProcessingDialog dlg = new ProcessingDialog(ctx, this, -1);
        dlg.show();
    }

    private boolean isRootDirectory() {
        return "/".equals(curDir);
    }

    private String getParentPath(String path) {
        int pos = path.lastIndexOf('/', path.length() - 2);
        return path.substring(0, pos + 1);
    }

    public String getCurrentDir() {
        return curDir;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                   int position, long arg3) {
        // TODO Auto-generated method stub
        return false;
    }

    public Map<String, String> getReplaces() {
        return this.fileReplaces;
    }

    public void addReplace(String entryName, String filePath) {
        fileReplaces.put(entryName, filePath);
    }

    public void destroy() {
        try {
            if (zfile != null) {
                zfile.close();
            }
        } catch (IOException e) {
        }
    }

    // Click on the edit/save image
    @Override
    public void onClick(View v) {
        int id = v.getId();

        // Edit image
        if (id < curFileList.size()) {
            showReplaceDlg(id);
        }

        // Save image
        else if (id < 2 * curFileList.size()) {
            extractFile(id - curFileList.size());
        }
    }

    private void extractFile(int index) {
        if (index >= curFileList.size()) {
            return;
        }

        FileInfo fi = curFileList.get(index);
        String entryName = curDir.substring(1) + fi.filename;
        String zipPath = zipHelper.getFilePath();

        extractFromZip(ctx, zipPath, entryName);
    }

    // position is the clicked item index
    private void showReplaceDlg(int index) {
        if (index < curFileList.size()) {
            FileInfo fi = curFileList.get(index);
            String entryPath = curDir.substring(1) + fi.filename;
//			FileReplaceDialog dlg = new FileReplaceDialog(ctx, entryPath, this);
//			dlg.show();
            FileSelectDialog replaceDlg = new FileSelectDialog(
                    ctx, this, null, entryPath, ctx.getString(R.string.select_file_replace));
            replaceDlg.show();
        }
    }

    // Add an image file replace
    @Override
    public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
        // Record the modification
        String entryPath = extraStr;
        this.fileReplaces.put(entryPath, filePath);

        // Remove the old cache
        this.imageBitmaps.remove(entryPath);

        // Update the list view
        this.notifyDataSetChanged();

        // Notify that something modified
        ((SimpleEditActivity) ctx).setModified();
    }

    @Override
    public String getConfirmMessage(String filePath, String extraStr) {
        return null;
    }

    @Override
    public boolean isInterestedFile(String filename, String extraStr) {
        int slashPos = extraStr.lastIndexOf('/');
        int dotPos = extraStr.indexOf('.', slashPos + 1);
        if (dotPos != -1) {
            return filename.endsWith(extraStr.substring(dotPos));
        } else {
            return true;
        }
    }

    static class FileInfo {
        String filename;
        boolean isDir;

        public FileInfo(String name, boolean isDir) {
            this.filename = name;
            this.isDir = isDir;
        }
    }
}
