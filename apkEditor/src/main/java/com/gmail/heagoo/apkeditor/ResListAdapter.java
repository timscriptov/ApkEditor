package com.gmail.heagoo.apkeditor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.ProcessingDialog.ProcessingInterface;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.se.ZipImageZoomer;
import com.gmail.heagoo.common.FileUtil;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.PathUtil;
import com.gmail.heagoo.common.RandomUtil;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.folderlist.FileRecord;
import com.gmail.heagoo.folderlist.FilenameComparator;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResListAdapter extends BaseAdapter implements
        OnCheckedChangeListener {

    List<FileRecord> fileList = new ArrayList<FileRecord>();
    private WeakReference<Context> ctxRef;
    private WeakReference<ResSelectionChangeListener> listenerRef;
    private int resourceId;
    private String apkPath;
    private String rootPath;
    private String curPath;
    private String strResolution;
    private FilenameFilter filter;
    // use this record to get the real entry inside the apk/zip
    private Map<String, String> fileEntry2ZipEntry;

    // Record all the checked items
    // When it is not empty, it means in selection mode
    private Set<Integer> checkedItems = new HashSet<Integer>();
    private LruCache<String, BitmapInfo> bitmapCache = new LruCache<String, BitmapInfo>(
            64) {
        protected void entryRemoved(boolean evicted, String key,
                                    BitmapInfo oldValue, BitmapInfo newValue) {
            if (oldValue.bitmap != null) {
                oldValue.bitmap.recycle();
            }
        }
    };
    // To support in zip exploring for folders like assets
    private ZipNode rootNode = new ZipNode();
    private ZipImageZoomer zipImageZoomer;
    private Map<String, String> allFileReplaced = new HashMap<>();
    private Map<String, String> allFileAdded = new HashMap<String, String>();
    // All the deleted entries
    private Set<String> allFileDeleted = new HashSet<String>();
    public ResListAdapter(Context ctx, String apkPath, String curPath,
                          String rootPath, FilenameFilter filter) {
        this(ctx, apkPath, curPath, rootPath, null, filter, null);
    }
    public ResListAdapter(Context ctx, String apkPath, String curPath,
                          String rootPath, Map<String, String> fileEntry2ZipEntry,
                          FilenameFilter filter, ResSelectionChangeListener listener) {
        this.ctxRef = new WeakReference<>(ctx);
        if (listener != null) {
            this.listenerRef = new WeakReference<>(listener);
        }

        this.apkPath = apkPath;
        this.rootPath = rootPath;
        this.curPath = curPath;
        this.fileEntry2ZipEntry = fileEntry2ZipEntry;
        this.filter = filter;
        this.strResolution = ctx.getResources().getString(R.string.resolution);

        // Decide resource id
        // sawsem theme
        resourceId = (listener != null ? R.layout.item_file_selectable : R.layout.item_file);
//        switch (GlobalConfig.instance(ctx).getThemeId()) {
//            case GlobalConfig.THEME_DARK_DEFAULT:
//                resourceId = (listener != null ? R.layout.item_file_selectable_dark
//                        : R.layout.item_file_dark);
//                break;
//            case GlobalConfig.THEME_DARK_RUSSIAN:
//                resourceId = (listener != null ? R.layout.item_file_selectable_dark_ru
//                        : R.layout.item_file_dark_ru);
//                break;
//        }

        if (apkPath != null) {
            initZipData(apkPath);
        }

        initListData(curPath);
    }

    // Refresh the list
    public void refresh() {
        openDirectory(curPath);
    }

    private void initZipData(String apkFilePath) {
        try {
            ZipFile zipFile = new ZipFile(apkFilePath);
            this.zipImageZoomer = new ZipImageZoomer(zipFile);

            Enumeration<? extends ZipEntry> entryEnum = zipFile.entries();

            ZipEntry entry = null;
            while (entryEnum.hasMoreElements()) {
                entry = entryEnum.nextElement();
                String entryName = entry.getName();
                if (entryName.startsWith("res/") || entryName.startsWith("r/")) {
                    continue;
                }
                if (entryName.startsWith("META-INF/")) {
                    continue;
                }

                String paths[] = entryName.split("/");
                if (paths.length == 1) {
                    if (entryName.equals("AndroidManifest.xml")) {
                        continue;
                    }
                    if (entryName.endsWith(".dex")
                            || entryName.endsWith(".arsc")) {
                        continue;
                    }
                }
                rootNode.addChildByPath(paths, true);
            }

            // zipFile.close();
        } catch (Exception e) {
        }
    }

    // Return directory and sub file records
    public String getData(List<FileRecord> records) {
        synchronized (fileList) {
            if (records != null) {
                records.addAll(fileList);
            }
            return curPath;
        }
    }

    // Directly call this function may cause data not synchronized
    // @SuppressWarnings("unused")
    // private String getCurrentDirectory() {
    // return curPath;
    // }

    // Directly call this function may cause data not synchronized
    // @SuppressWarnings("unused")
    // private List<FileRecord> getFileList() {
    // synchronized (fileList) {
    // List<FileRecord> clone = new ArrayList<FileRecord>();
    // clone.addAll(fileList);
    // return clone;
    // }
    // }

    private void initListData(String path) {
        synchronized (fileList) {

            File dir = new File(path);

            // The file is not extracted (get sub files from zip)
            if (!dir.exists()) {
                listSubNodes(path);
            } else {
                listSubFiles(path);
            }
        }
    }

    // List sub nodes in zip file
    @SuppressWarnings("unchecked")
    private void listSubNodes(String path) {
        String relativePath = path.substring(rootPath.length() + 1);
        String[] paths = relativePath.split("/");

        ZipNode parent = this.rootNode.findFolderByPath(paths);
        if (parent != null) {
            fileList.clear();

            List<ZipNode> children = parent.getChildren();
            if (children != null) {
                for (ZipNode node : children) {
                    FileRecord fr = new FileRecord();
                    fr.fileName = node.name;
                    fr.isDir = !node.isFile;
                    fr.isInZip = true;
                    fileList.add(fr);
                }
                Collections.sort(fileList, new FilenameComparator());
            }

            // This path always not in root dir, so add ".."
            FileRecord fr = new FileRecord();
            fr.fileName = "..";
            fr.isDir = true;
            fileList.add(0, fr);

            curPath = path;
        }
    }

    @SuppressWarnings("unchecked")
    private void listSubFiles(String path) {
        File dir = new File(path);
        boolean isRootDir = path.equals(rootPath);
        File[] subFiles = null;
        if (isRootDir && filter != null) {
            subFiles = dir.listFiles(filter);
        } else {
            subFiles = dir.listFiles();
        }
        if (subFiles != null) {
            fileList.clear();

            for (File f : subFiles) {
                FileRecord fr = new FileRecord();
                fr.fileName = f.getName();
                fr.isDir = f.isDirectory();
                fileList.add(fr);
            }

            // Don't know why it may throw IllegalArgumentException
            try {
                Collections.sort(fileList, new FilenameComparator());
            } catch (Exception ignored) {
            }

            // In root directory, will not show parent folder
            if (!isRootDir) {
                FileRecord fr = new FileRecord();
                fr.fileName = "..";
                fr.isDir = true;
                fileList.add(0, fr);
            } else {
                // Also list root entry in zip file
                List<FileRecord> extraRecords = new ArrayList<FileRecord>();
                List<ZipNode> children = rootNode.children;
                if (children != null && !children.isEmpty()) {
                    for (ZipNode node : children) {
                        FileRecord fr = new FileRecord();
                        fr.fileName = node.name;
                        fr.isDir = !node.isFile;
                        fr.isInZip = true;
                        extraRecords.add(fr);
                    }
                    Collections.sort(extraRecords, new FilenameComparator());
                    fileList.addAll(extraRecords);
                }
            }

            curPath = path;
        }
        // Special case: in the parent path of SD card (like /storage/emulated/0)
        // As on some phones, we cannot access the directory like /storage/emulated
        else if (PathUtil.isParentFolderOf(path, SDCard.getRootDirectory())) {
            fileList.clear();

            SDCard.getRootDirectory();
            FileRecord fr = new FileRecord();
            fr.fileName = PathUtil.getSubFolder(path, SDCard.getRootDirectory());
            fr.isDir = true;
            fileList.add(fr);

            // In root directory, will not show parent folder
            if (!path.equals(rootPath)) {
                fr = new FileRecord();
                fr.fileName = "..";
                fr.isDir = true;
                fr.totalSize = -1;
                fileList.add(0, fr);
            }

            curPath = path;
        }
    }

    @Override
    public int getCount() {
        return fileList.size();
    }

    @Override
    public Object getItem(int position) {
        return fileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // Get image thumbnail by file path
    public ImageThumbnailInfo getImageInfo(String filepath) {
        String entryName = filepath.substring(rootPath.length() + 1);
        return getImageInfo(entryName, false);
    }

    // Get image thumbnail by entry name
    private ImageThumbnailInfo getImageInfo(String entryName, boolean bInZip) {
        Bitmap bitmap = null;
        String detailInfo = null;

        // Get bitmap info from replaced file
        BitmapInfo bmpInfo = getModifiedImageInfo(entryName);
        if (bmpInfo != null) {
            bitmap = bmpInfo.bitmap;
            detailInfo = strResolution + ": " + bmpInfo.width + " X "
                    + bmpInfo.height;
        }
        // The file is not replaced and no apk provided
        else if (this.apkPath == null) {
            com.gmail.heagoo.common.ImageZoomer zoomer = new com.gmail.heagoo.common.ImageZoomer();
            bitmap = zoomer.getImageThumbnail(rootPath + "/" + entryName, 32,
                    32);
            detailInfo = strResolution + ": " + zoomer.getOriginWidth() + " X "
                    + zoomer.getOriginHeight();
        }
        // The file is not replaced (look into file system and apk file)
        else {
            // image (.9.png) is in file
            if (!bInZip && entryName.endsWith(".9.png")) {
                com.gmail.heagoo.common.ImageZoomer zoomer = new com.gmail.heagoo.common.ImageZoomer();
                bitmap = zoomer.getImageThumbnail(rootPath + "/" + entryName,
                        32, 32);
                detailInfo = strResolution + ": " + zoomer.getOriginWidth()
                        + " X " + zoomer.getOriginHeight();
            }
            // Get image from zip
            // Note: image on file system is dummy
            else {
                if (fileEntry2ZipEntry != null) {
                    String t = fileEntry2ZipEntry.get(entryName);
                    if (t != null) {
                        entryName = t;
                    }
                }

                bmpInfo = getImageInfoFromZip(entryName);
                if (bmpInfo != null) {
                    bitmap = bmpInfo.bitmap;
                    detailInfo = strResolution + ": " + bmpInfo.width + " X "
                            + bmpInfo.height;
                }
            }
        }

        return new ImageThumbnailInfo(bitmap, detailInfo);
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FileRecord rec = fileList.get(position);
        ViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(ctxRef.get()).inflate(resourceId,
                    null);

            viewHolder = new ViewHolder();
            viewHolder.icon = (ImageView) convertView
                    .findViewById(R.id.file_icon);
            viewHolder.filename = (TextView) convertView
                    .findViewById(R.id.filename);
            viewHolder.desc1 = (TextView) convertView
                    .findViewById(R.id.detail1);
            viewHolder.checkbox = (CheckBox) convertView
                    .findViewById(R.id.checkBox);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String detailInfo = null;
        viewHolder.filename.setText(rec.fileName);
        if (rec.fileName.equals("..")) {
            viewHolder.icon.setImageResource(R.drawable.folderutil_up);
        } else if (rec.isDir) {
            viewHolder.icon.setImageResource(R.drawable.folderutil_folder);
        } else {
            if (rec.fileName.endsWith(".xml")) {
                viewHolder.icon.setImageResource(R.drawable.util_xml_file);
            }
            if (isImageFile(rec.fileName)) {
                String entryName = getEntryName(curPath, rec.fileName);
                ImageThumbnailInfo info = getImageInfo(entryName, rec.isInZip);
                viewHolder.icon.setImageBitmap(info.thumbnail);
                detailInfo = info.detailInfo;
            } else if (rec.fileName.endsWith(".xml")) {
                viewHolder.icon.setImageResource(R.drawable.util_xml_file);
            } else {
                viewHolder.icon.setImageResource(R.drawable.folderutil_file);
            }
        }

        if (detailInfo != null) {
            viewHolder.desc1.setText(detailInfo);
            viewHolder.desc1.setVisibility(View.VISIBLE);
        } else {
            viewHolder.desc1.setVisibility(View.GONE);
        }

        // checkbox
        if (viewHolder.checkbox != null) {
            if (position == 0 && rec.fileName.equals("..")) {
                viewHolder.checkbox.setVisibility(View.INVISIBLE);
            } else {
                viewHolder.checkbox.setVisibility(View.VISIBLE);
                viewHolder.checkbox.setId(position);
                viewHolder.checkbox.setOnCheckedChangeListener(this);
                viewHolder.checkbox.setChecked(this.checkedItems
                        .contains(Integer.valueOf(position)));
            }
        }

        return convertView;
    }

    // Get entry name by current directory and file name
    private String getEntryName(String curPath, String fileName) {
        String entryName = null;
        if (curPath.equals(rootPath)) {
            entryName = fileName;
        } else {
            int position = rootPath.endsWith("/") ? rootPath.length()
                    : (rootPath.length() + 1);
            entryName = curPath.substring(position) + "/" + fileName;
        }
        return entryName;
    }

    // Get the image info from modified records (added or replaced)
    private BitmapInfo getModifiedImageInfo(String entryName) {
        String path = this.allFileAdded.get(entryName);
        if (path == null) {
            path = this.allFileReplaced.get(entryName);
        }

        if (path != null) {
            com.gmail.heagoo.common.ImageZoomer zoomer = new com.gmail.heagoo.common.ImageZoomer();
            Bitmap bitmap = zoomer.getImageThumbnail(path, 32, 32);
            if (bitmap != null) {
                return new BitmapInfo(bitmap, zoomer.getOriginWidth(),
                        zoomer.getOriginHeight());
            }
        }

        return null;
    }

    private BitmapInfo getImageInfoFromZip(String entryName) {
        // Get thumbnail, width and height
        BitmapInfo bmpInfo = null;

        bmpInfo = bitmapCache.get(entryName);

        if (bmpInfo == null) {
            Bitmap bitmap = zipImageZoomer.getImageThumbnail(entryName, 32, 32);
            if (bitmap != null) {
                int width = zipImageZoomer.getOriginWidth();
                int height = zipImageZoomer.getOriginHeight();
                bmpInfo = new BitmapInfo(bitmap, width, height);
            }
            // Save to cache
            if (bmpInfo != null) {
                bitmapCache.put(entryName, bmpInfo);
            }
        }

        return bmpInfo;
    }

    public boolean isImageFile(String fileName) {
        if (fileName.endsWith(".png") || fileName.endsWith(".jpg")) {
            return true;
        }
        return false;
    }

    public void openDirectory(String targetPath) {
        // Target path is not correct
        if (rootPath.startsWith(targetPath) && !targetPath.equals(rootPath)) {
            return;
        }
        initListData(targetPath);
        this.notifyDataSetChanged();
    }

    // Removed several list items
    protected void listItemsDeleted(List<Integer> positions) {
        Iterator<FileRecord> it = fileList.iterator();
        int index = 0;
        while (it.hasNext()) {
            it.next();
            if (positions.contains(index)) {
                it.remove();
            }
            index += 1;
        }

        // As it is called in selection mode, need to uncheck
        this.checkedItems.clear();

        this.notifyDataSetChanged();

        // Sometimes, it will not trigger onCheckedChanged
        if (listenerRef.get() != null) {
            listenerRef.get().selectionChanged(checkedItems);
        }
    }

    // Add a list item
    public void listItemAdded(String dirPath, FileRecord rec) {
        synchronized (fileList) {
            if (this.curPath.equals(dirPath)) {
                fileList.add(rec);
                this.notifyDataSetChanged();
            }
        }
    }

    // Add a file (either in the decoded directory or inside the apk file)
    // Called in UI thread
    public void addFile(String targetPath, String filePath) {
        int pos = targetPath.lastIndexOf("/");
        String filename = targetPath.substring(pos + 1);
        String dirPath = targetPath.substring(0, pos);

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
            if (fis != null) {
                FileRecord rec = addFile(targetPath, fis);
                if (rec != null) {
                    listItemAdded(dirPath, rec);
                    String msg = String.format(
                            ctxRef.get().getString(R.string.file_added),
                            filename);
                    Toast.makeText(ctxRef.get(), msg, Toast.LENGTH_SHORT)
                            .show();
                }
            }
        } catch (Exception e) {
            String msg = String.format(ctxRef.get()
                    .getString(R.string.failed_1), e.getMessage());
            Toast.makeText(ctxRef.get(), msg, Toast.LENGTH_LONG).show();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }
    }

    // This function can be called in non-UI thread
    public FileRecord addFile(String targetPath, InputStream input)
            throws Exception {

        int position = targetPath.lastIndexOf('/');
        String dirPath = targetPath.substring(0, position);
        String filename = targetPath.substring(position + 1);
        String entryName;
        if (dirPath.equals(rootPath)) { // special case
            entryName = filename;
        } else {
            entryName = targetPath.substring(rootPath.length() + 1);
        }

        // Special case: in the root dir to check allowed or not
        if (dirPath.equals(rootPath) && this.apkPath != null) {
            checkCanPutInRootPath(filename);
        }

        boolean bInZip;
        File dir = new File(dirPath);
        // Copy to a decoded folder
        if (insideResOrSmali(dirPath) || (dir.exists() && !dirPath.equals(rootPath))) {
            if (!dir.exists()) {
                dir.mkdirs();
            }
            bInZip = false;
            if (new File(targetPath).exists()) {
                throwExistException(entryName);
            } else {
                FileOutputStream out = new FileOutputStream(targetPath);
                IOUtils.copy(input, out);
                out.close();
            }
        } else { // copy to a folder inside apk (like assets)
            bInZip = true;
            String[] paths = entryName.split("/");
            ZipNode zipNode = rootNode.findNodeByPath(paths);
            if (zipNode != null) {
                throwExistException(entryName);
            } else {
                // Copy to the working path
                targetPath = SDCard.makeWorkingDir(ctxRef.get())
                        + RandomUtil.getRandomString(8);
                FileOutputStream out = new FileOutputStream(targetPath);
                IOUtils.copy(input, out);
                out.close();

                rootNode.addChildByPath(paths, true);
            }
        }

        // Record and update the list
        recordFileAdd(entryName, targetPath);

        FileRecord rec = new FileRecord(filename, false, bInZip);
        return rec;
    }

    private boolean insideResOrSmali(String dirPath) {
        String resDir = rootPath + "/res";
        if (dirPath.equals(resDir)) {
            return true;
        }
        if (dirPath.startsWith(resDir + "/")) {
            return true;
        }

        String smaliDir = rootPath + "/smali";
        if (dirPath.equals(smaliDir)) {
            return true;
        }
        if (dirPath.startsWith(smaliDir + "/")) {
            return true;
        }
        if (dirPath.startsWith(smaliDir + "_")) {
            return true;
        }
        return false;
    }

    // Check this filename can put in the root dir of the apk file or not
    private void checkCanPutInRootPath(String filename) throws Exception {
        if (filename.equals("META-INF")) {
            throwExistException(filename);
        }
        ZipFile zfile = new ZipFile(this.apkPath);
        ZipEntry entry = zfile.getEntry(filename);
        if (entry != null) {
            zfile.close();
            throwExistException(filename);
        } else {
            zfile.close();
        }
    }

    // Add an empty folder
    public void addFolder(String dirPath, String folderName) {
        try {
            addFolderReportError(dirPath, folderName, true);
        } catch (Exception e) {
            Toast.makeText(ctxRef.get(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void addFolderReportError(String dirPath, String folderName,
                                     boolean bUiThread) throws Exception {
        // Special case: in the root dir to check allowed or not
        if (dirPath.equals(rootPath) && this.apkPath != null) {
            checkCanPutInRootPath(folderName);
        }

        File dir = new File(dirPath);
        // Add the folder into decode path
        if (dir.exists() && !dirPath.equals(rootPath)) {
            File f = new File(dirPath, folderName);
            if (f.exists()) {
                throwExistException(folderName);
            }

            boolean ret = f.mkdir();
            if (bUiThread) {
                if (!ret && ctxRef.get() != null) {
                    Toast.makeText(ctxRef.get(), R.string.failed,
                            Toast.LENGTH_LONG).show();
                } else { // call openDirectory to update the list
                    openDirectory(dirPath);
                }
            }
        }
        // Add the folder in some place of the apk file
        else {
            String entryName = null;
            if (dirPath.equals(rootPath)) { // special case
                entryName = folderName;
            } else {
                entryName = dirPath.substring(rootPath.length() + 1) + "/"
                        + folderName;
            }
            String[] paths = entryName.split("/");
            ZipNode node = rootNode.findNodeByPath(paths);
            if (node != null) {
                throwExistException(folderName);
            } else {
                rootNode.addChildByPath(paths, false);
                if (bUiThread) {
                    openDirectory(dirPath); // update the list view
                }
            }
        }
    }

    private void throwExistException(String filename) throws Exception {
        throw new Exception(String.format(
                ctxRef.get().getString(R.string.file_already_exist), filename));
    }

    // For editable files, call this function when it is modified
    public void fileModified(String entryName, String newFilePath) {
        this.recordFileReplace(entryName, newFilePath);
    }

    // A file is selected to replace an existing file (not just
    // image, can be image, smali, and others)
    public void replaceFile(String decodedPath, String newPath) {
        String entryPath = decodedPath.substring(rootPath.length() + 1);

        // Do replace in file system
        if (new File(decodedPath).exists()) {
            FileInputStream in = null;
            FileOutputStream out = null;
            try {
                // Copy files
                in = new FileInputStream(newPath);
                out = new FileOutputStream(decodedPath);
                IOUtils.copy(in, out);

                // Record replace information and show
                recordFileReplace(entryPath, decodedPath);
                Toast.makeText(ctxRef.get(), R.string.file_replaced,
                        Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeQuietly(in);
                closeQuietly(out);
            }
        }
        // Do replace inside the apk
        else {
            try {
                // Copy file to working directory
                String targetPath = SDCard.makeWorkingDir(ctxRef.get())
                        + RandomUtil.getRandomString(8);
                FileUtil.copyFile(newPath, targetPath);

                // Record replacement and show toast
                recordFileReplace(entryPath, targetPath);
                Toast.makeText(ctxRef.get(), R.string.file_replaced,
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.notifyDataSetChanged();
    }

    // Replace the whole folder
    // Copy all the contents in newPath to decodedPath
    // The folder name will keep unchanged
    public void replaceFolder(final String decodedPath, final String newPath) {
        ProcessingInterface processing = new ProcessingInterface() {
            @Override
            public void process() throws Exception {
                File dstFolder = new File(decodedPath);
                if (dstFolder.exists()) { // copy to decoded path (file system)
                    List<String> deletedFiles = doDeleteDirectory(dstFolder);
                    if (deletedFiles != null) {
                        for (String delPath : deletedFiles) {
                            String entry = delPath
                                    .substring(rootPath.length() + 1);
                            recordFileDelete(entry);
                        }
                    }
                    File srcFolder = new File(newPath);
                    Map<String, String> added = copyAllFiles(srcFolder,
                            dstFolder,
                            decodedPath.substring(rootPath.length() + 1));
                    for (Map.Entry<String, String> entry : added.entrySet()) {
                        recordFileAdd(entry.getKey(), entry.getValue());
                    }
                } else { // copy to apk/zip node
                    String entry = decodedPath.substring(rootPath.length() + 1);
                    List<String> delEntries = rootNode.deleteByPath(entry
                            .split("/"));
                    if (delEntries != null) {
                        for (String e : delEntries) {
                            recordFileDelete(e);
                        }
                    }
                    // Copy to the working directory (not decoded path)
                    String targetFolder = SDCard.makeWorkingDir(ctxRef.get())
                            + RandomUtil.getRandomString(6);
                    Map<String, String> added = copyAllFiles(new File(newPath),
                            new File(targetFolder), entry);
                    // Record and update the zip nodes
                    for (Map.Entry<String, String> e : added.entrySet()) {
                        rootNode.addChildByPath(e.getKey().split("/"), true);
                        recordFileAdd(e.getKey(), e.getValue());
                    }
                }
            }

            @Override
            public void afterProcess() {
            }
        };
        new ProcessingDialog((Activity) ctxRef.get(), processing,
                R.string.folder_replaced).show();
    }

    // Copy all files from srcFolder to dstFolder
    // Return all the added entries
    // Note: it will NOT record the added entries
    private Map<String, String> copyAllFiles(File srcFolder, File dstFolder,
                                             String entryName) throws IOException {
        if (!dstFolder.exists()) {
            dstFolder.mkdirs();
        }

        Map<String, String> addedEntries = new HashMap<String, String>();

        File[] files = srcFolder.listFiles();
        if (files != null)
            for (File f : files) {
                String name = f.getName();
                if (f.isFile()) { // Copy a single file
                    File dstFile = new File(dstFolder, f.getName());
                    FileUtil.copyFile(f, dstFile);
                    String filepath = dstFile.getPath();
                    addedEntries.put(entryName + "/" + name, filepath);
                } else { // Copy the sub folder
                    Map<String, String> added = copyAllFiles(f, new File(dstFolder,
                            f.getName()), entryName + "/" + name);
                    addedEntries.putAll(added);
                }
            }

        return addedEntries;
    }

    protected void dumpChangedFiles() {
        // Log.d("DEBUG", "Added Entry: ");
        // for (Map.Entry<String, String> entry : allFileAdded.entrySet()) {
        // Log.d("DEBUG", "\t" + entry.getKey() + " --> " + entry.getValue());
        // }
        //
        // Log.d("DEBUG", "Replaced Entry: ");
        // for (Map.Entry<String, String> entry : allFileReplaced.entrySet()) {
        // Log.d("DEBUG", "\t" + entry.getKey() + " --> " + entry.getValue());
        // }
        //
        // Log.d("DEBUG", "Deleted Entry: ");
        // for (String entry : allFileDeleted) {
        // Log.d("DEBUG", "\t" + entry);
        // }
    }

    private void recordFileAdd(String entryName, String filepath) {
        if (this.allFileDeleted.contains(entryName)) {
            this.allFileDeleted.remove(entryName);
            this.allFileReplaced.put(entryName, filepath);
        } else if (this.allFileReplaced.containsKey(entryName)) {
            Log.d("DEBUG", "error: " + entryName + " already exist?");
        } else {
            this.allFileAdded.put(entryName, filepath);
        }
    }

    private void recordFileDelete(String entryName) {
        // The file is new added
        if (this.allFileAdded.containsKey(entryName)) {
            this.allFileAdded.remove(entryName);
        }
        // The file is modified
        else if (this.allFileReplaced.containsKey(entryName)) {
            this.allFileReplaced.remove(entryName);
            this.allFileDeleted.add(entryName);
        }
        // no operation before
        else {
            this.allFileDeleted.add(entryName);
        }
    }

    private void recordFileReplace(String entryName, String filepath) {
        if (this.allFileAdded.containsKey(entryName)) {
            this.allFileAdded.put(entryName, filepath);
        } else if (this.allFileDeleted.contains(entryName)) {
            Log.d("DEBUG", "error: " + entryName + " is already deleted?");
        } else {
            this.allFileReplaced.put(entryName, filepath);
        }
    }

    private void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }

    // Recursively delete a directory
    // Return all deleted files
    private List<String> doDeleteDirectory(File dir) {
        List<String> deletedFiles = new ArrayList<String>();
        File[] subFiles = dir.listFiles();
        if (subFiles != null) {
            for (File subF : subFiles) {
                if (subF.isFile()) {
                    subF.delete();
                    deletedFiles.add(subF.getPath());
                } else {
                    deletedFiles.addAll(doDeleteDirectory(subF));
                }
            }
        }
        dir.delete();
        return deletedFiles;
    }

    // Delete a file or directory in resource directory
    // Return all deleted files
    private List<String> doDeleteFileOrDir(String path) {
        File file = new File(path);

        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
                List<String> deletedFiles = new ArrayList<String>();
                deletedFiles.add(path);
                return deletedFiles;
            } else {
                return doDeleteDirectory(file);
            }
        }

        return null;
    }

    // Delete a file or directory in a directory
    // The item to be deleted can in file system or zip file
    protected void deleteFile(List<Integer> positions) {
        List<FileRecord> records = new ArrayList<FileRecord>();
        String dirPath = getData(records);

        List<Integer> deletedPositions = new ArrayList<Integer>();
        for (int position : positions) {
            FileRecord fileRec = records.get(position);
            if (fileRec == null) {
                continue;
            }
            // Do NOT check the return value
            // if (deleteFile(dirPath, fileRec)) {
            // deletedPositions.add(position);
            // }
            deleteFile(dirPath, fileRec);
            deletedPositions.add(position);
        }

        dumpChangedFiles();

        listItemsDeleted(deletedPositions);
    }

    private boolean deleteFile(String dirPath, FileRecord fileRec) {
        return deleteFile(dirPath, fileRec.fileName, fileRec.isInZip);
    }

    // Can be called in non-UI thread
    public boolean deleteFile(String dirPath, String fileName, boolean bInZip) {
        boolean ret = false;
        List<String> deletedEntries = null;

        if (bInZip) {
            String entryPath = null;
            if (dirPath.equals(rootPath)) {
                entryPath = fileName;
            } else {
                entryPath = dirPath.substring(rootPath.length() + 1) + "/"
                        + fileName;
            }
            // Log.d("DEBUG", "delete path = " + entryPath);
            deletedEntries = rootNode.deleteByPath(entryPath.split("/"));
            ret = true;
        } else {
            String path = dirPath + "/" + fileName;
            List<String> deletedFiles = doDeleteFileOrDir(path);
            if (deletedFiles != null) {
                deletedEntries = new ArrayList<String>();
                for (String delPath : deletedFiles) {
                    deletedEntries
                            .add(delPath.substring(rootPath.length() + 1));
                }
                // for (String entry : deletedEntries) {
                // Log.d("DEBUG", "File: " + entry);
                // }
            }
            ret = (deletedFiles != null);
        }

        // Record deleted entries
        if (deletedEntries != null) {
            for (String delEntry : deletedEntries) {
                recordFileDelete(delEntry);
            }
        }

        return ret;
    }

    // Get the replacing file path by entry name
    // Also look into the added entry
    public String getReplacedFilePath(String entryName) {
        String filepath = allFileReplaced.get(entryName);
        if (filepath == null) {
            filepath = allFileAdded.get(entryName);
        }

        return filepath;
    }

    public Map<String, String> getAddedFiles() {
        return this.allFileAdded;
    }

    public Map<String, String> getReplacedFiles() {
        return this.allFileReplaced;
    }

    public Set<String> getDeletedFiles() {
        return this.allFileDeleted;
    }

    // Recover the previous modification
    public void setModification(Map<String, String> res_addedFiles,
                                Set<String> res_deletedFiles, Map<String, String> res_replacedFiles) {
        if (res_addedFiles != null) {
            this.allFileAdded = res_addedFiles;
        }
        if (res_deletedFiles != null) {
            this.allFileDeleted = res_deletedFiles;
        }
        if (res_replacedFiles != null) {
            this.allFileReplaced = res_replacedFiles;
        }
    }

    // Checkbox selection changed
    @Override
    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        // id is the position
        int id = view.getId();
        if (isChecked) {
            this.checkedItems.add(id);
        } else {
            this.checkedItems.remove(id);
        }

        if (this.listenerRef != null) {
            listenerRef.get().selectionChanged(checkedItems);
        }

        // Log.d("DEBUG", "checked items: " + this.checkedItems.size());
        // for (int position : checkedItems) {
        // Log.d("DEBUG", "\t" + position);
        // }
    }

    public Set<Integer> getCheckedItems() {
        return this.checkedItems;
    }

    public void reverseCheckStatus(int position) {
        // Omit the first parent item
        if (position == 0) {
            if (!fileList.isEmpty() && "..".equals(fileList.get(0).fileName)) {
                return;
            }
        }

        if (this.checkedItems.contains(position)) {
            this.checkedItems.remove(position);
        } else {
            this.checkedItems.add(position);
        }
        this.notifyDataSetChanged();
    }

    // Select all the items, or un-select all the items
    public void checkAllItems(boolean checkAll) {
        if (checkAll) {
            FileRecord rec = fileList.get(0);
            if (!rec.fileName.equals("..")) { // Not for parent folder
                this.checkedItems.add(0);
            }
            for (int i = 1; i < fileList.size(); ++i) {
                this.checkedItems.add(i);
            }
        } else {
            this.checkedItems.clear();
            if (listenerRef.get() != null) {
                listenerRef.get().selectionChanged(checkedItems);
            }
        }
        this.notifyDataSetChanged();
    }

    // Check the folder exist or not
    // Note: path is the full path like /data/0/com.xx/...
    public boolean isFolderExist(String path) {
        File f = new File(path);
        if (f.exists()) {
            return true;
        }

        // Special root path
        if (this.rootPath.equals(path)) {
            return true;
        }

        // Exist in the node records
        String relativePath = path.substring(rootPath.length() + 1);
        ZipNode node = rootNode.findFolderByPath(relativePath.split("/"));
        if (node != null) {
            return true;
        }

        return false;
    }

    public boolean isInRootDir() {
        return this.curPath.equals(this.rootPath);
    }

    public void gotoRootDir() {
        openDirectory(this.rootPath);
    }

    // Rename the file paths as the decoded folder rename
    public void renamePathAsFolderRename(String fromPath, String toDir) {
        if (this.allFileAdded != null) {
            for (Map.Entry<String, String> entry : this.allFileAdded.entrySet()) {
                if (entry.getValue().startsWith(fromPath)) {
                    entry.setValue(toDir + entry.getValue().substring(fromPath.length()));
                }
            }
        }
        if (this.allFileReplaced != null) {
            for (Map.Entry<String, String> entry : this.allFileReplaced.entrySet()) {
                if (entry.getValue().startsWith(fromPath)) {
                    entry.setValue(toDir + entry.getValue().substring(fromPath.length()));
                }
            }
        }
    }

    // Image cache
    static class BitmapInfo {
        Bitmap bitmap;
        int width;
        int height;
        public BitmapInfo(Bitmap bitmap2, int width2, int height2) {
            this.bitmap = bitmap2;
            this.width = width2;
            this.height = height2;
        }
    }

    // To emulate the folder tree in zip file
    public static class ZipNode {
        List<ZipNode> children;
        String name;
        boolean isFile;

        public ZipNode() {
            name = "";
            isFile = false;
        }

        public ZipNode(String name, boolean isFile) {
            this.name = name;
            this.isFile = isFile;
        }

        public void addChildByPath(String[] paths, boolean isFile) {
            ZipNode parentNode = this;
            for (int i = 0; i < paths.length - 1; i++) {
                ZipNode childNode = parentNode.findFolder(paths[i], true);
                if (childNode == null) {
                    childNode = new ZipNode(paths[i], false);
                    parentNode.addChild(childNode);
                }
                parentNode = childNode;
            }
            // The last one can be file or directory
            ZipNode leaf = new ZipNode(paths[paths.length - 1], isFile);
            parentNode.addChild(leaf);
        }

        public ZipNode findFolderByPath(String[] paths) {
            ZipNode parentNode = this;
            for (int i = 0; i < paths.length; i++) {
                ZipNode childNode = parentNode.findFolder(paths[i], false);
                if (childNode == null) {
                    return null;
                }
                parentNode = childNode;
            }
            return parentNode;
        }

        public ZipNode findNodeByPath(String[] paths) {
            ZipNode parentNode = this;
            for (int i = 0; i < paths.length - 1; i++) {
                ZipNode childNode = parentNode.findFolder(paths[i], false);
                if (childNode == null) {
                    return null;
                }
                parentNode = childNode;
            }
            return parentNode.findChildByName(paths[paths.length - 1]);
        }

        private void addChild(ZipNode childNode) {
            if (children == null) {
                children = new ArrayList<ZipNode>();
            }
            children.add(childNode);
        }

        private ZipNode findFolder(String name, boolean bCreate) {
            if (children == null) {
                if (bCreate) {
                    children = new ArrayList<ZipNode>();
                } else {
                    return null;
                }
            }
            for (ZipNode node : children) {
                if (!node.isFile && name.equals(node.name)) {
                    return node;
                }
            }
            return null;
        }

        // Find a child node by name
        private ZipNode findChildByName(String name) {
            if (children != null) {
                for (ZipNode node : children) {
                    if (name.equals(node.name)) {
                        return node;
                    }
                }
            }
            return null;
        }

        public List<ZipNode> getChildren() {
            return children;
        }

        // Delete the node by path
        // Return all the deleted file entries
        public List<String> deleteByPath(String[] paths) {
            ZipNode parentNode = null;
            if (paths.length > 1) {
                String[] newPaths = new String[paths.length - 1];
                for (int i = 0; i < paths.length - 1; i++) {
                    newPaths[i] = paths[i];
                }
                parentNode = findFolderByPath(newPaths);
            } else {
                parentNode = this;
            }
            if (parentNode != null) {
                ZipNode target = parentNode
                        .findChildByName(paths[paths.length - 1]);
                if (target != null) {
                    if (target.isFile) {
                        parentNode.deleteChild(target);
                        List<String> deletedEntries = new ArrayList<String>();
                        String delPath = com.gmail.heagoo.common.StringUtil
                                .join("/", paths);
                        deletedEntries.add(delPath);
                        return deletedEntries;
                    } else {
                        List<String> deletedEntries = target.enumFiles();
                        if (deletedEntries != null) {
                            String delRootPath = com.gmail.heagoo.common.StringUtil
                                    .join("/", paths);
                            for (int i = 0; i < deletedEntries.size(); i++) {
                                deletedEntries.set(i, delRootPath + "/"
                                        + deletedEntries.get(i));
                            }
                            parentNode.deleteChild(target);
                            return deletedEntries;
                        }
                        parentNode.deleteChild(target);
                    }
                }
            }
            return null;
        }

        // Delete a child
        private boolean deleteChild(ZipNode _child) {
            if (children != null) {
                return children.remove(_child);
            }
            return false;
        }

        // Enum all the sub files recursively
        private List<String> enumFiles() {
            if (children != null) {
                List<String> entries = new ArrayList<String>();
                for (ZipNode child : children) {
                    if (child.isFile) {
                        entries.add(child.name);
                    } else {
                        List<String> subEntries = child.enumFiles();
                        if (subEntries != null) {
                            for (String subEntry : subEntries) {
                                entries.add(child.name + "/" + subEntry);
                            }
                        }
                    }
                }
                return entries;
            }

            return null;
        }
    }

    private static class ViewHolder {
        ImageView icon;
        TextView filename;
        TextView desc1;
        CheckBox checkbox;
    }
}
