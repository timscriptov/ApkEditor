package com.gmail.heagoo.apkeditor.se;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.GlobalConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.folderlist.FileRecord;
import com.gmail.heagoo.folderlist.FilenameComparator;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileListAdapter extends BaseAdapter {


    List<FileRecord> fileList = new ArrayList<FileRecord>();
    private Context ctx;
    private String rootPath;
    private String curPath;
    private String strResolution;
    private FilenameFilter filter;

    private int themeId;

    public FileListAdapter(Context ctx, String folderPath) {
        this(ctx, folderPath, null);
    }

    public FileListAdapter(Context ctx, String rootPath, FilenameFilter filter) {
        this.ctx = ctx;
        this.rootPath = rootPath;
        this.filter = filter;
        this.strResolution = ctx.getResources().getString(R.string.size);
        this.themeId = GlobalConfig.instance(ctx).getThemeId();

        initListData(rootPath);
    }

    public FileListAdapter(Context ctx, String rootPath, String initPath,
                           FilenameFilter filter) {
        this.ctx = ctx;
        this.rootPath = rootPath;
        this.filter = filter;
        this.strResolution = ctx.getResources().getString(R.string.size);
        this.themeId = GlobalConfig.instance(ctx).getThemeId();

        initListData(initPath);
    }

    public FileRecord getItemData(int position) {
        synchronized (fileList) {
            return fileList.get(position);
        }
    }

    // Directly call this function may cause data not synchronized
    @SuppressWarnings("unused")
    private String getCurrentDirectory() {
        return curPath;
    }

    // Directly call this function may cause data not synchronized
    @SuppressWarnings("unused")
    private List<FileRecord> getFileList() {
        synchronized (fileList) {
            List<FileRecord> clone = new ArrayList<FileRecord>();
            clone.addAll(fileList);
            return clone;
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

    @SuppressWarnings("unchecked")
    private void initListData(String path) {
        synchronized (fileList) {
            boolean isRootDir = path.equals(rootPath);
            File dir = new File(path);
            File[] subFiles;
            if (filter != null) {
                subFiles = dir.listFiles(filter);
            } else {
                subFiles = dir.listFiles();
            }
            if (subFiles != null) {
                fileList.clear();

                for (File f : subFiles) {
                    String fileName = f.getName();
                    // In root directory, not show values related dir
                    // if (isRootDir
                    // && (fileName.equals("values") || fileName
                    // .startsWith("values-"))) {
                    // } else
                    {
                        FileRecord fr = new FileRecord();
                        fr.fileName = fileName;
                        fr.isDir = f.isDirectory();
                        fileList.add(fr);
                    }
                }

                Collections.sort(fileList, new FilenameComparator());

                // In root directory, will not show parent folder
                if (!isRootDir) {
                    FileRecord fr = new FileRecord();
                    fr.fileName = "..";
                    fr.isDir = true;
                    fileList.add(0, fr);
                }

                curPath = path;
            }
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

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FileRecord rec = fileList.get(position);
        ViewHolder viewHolder = null;
        // sawsem theme
        if (convertView == null) {
            int resId = R.layout.item_file;
//			switch (themeId) {
//				case GlobalConfig.THEME_DARK_DEFAULT:
//					resId = R.layout.item_file_dark;
//					break;
//				case GlobalConfig.THEME_DARK_RUSSIAN:
//					resId = R.layout.item_file_dark_ru;
//					break;
//			}
            convertView = LayoutInflater.from(ctx).inflate(resId, null);

            viewHolder = new ViewHolder();
            viewHolder.icon = (ImageView) convertView
                    .findViewById(R.id.file_icon);
            viewHolder.filename = (TextView) convertView
                    .findViewById(R.id.filename);
            viewHolder.desc1 = (TextView) convertView
                    .findViewById(R.id.detail1);

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
            if (isImageFile(rec.fileName)) {
                // Get thumbnail for the image
                com.gmail.heagoo.common.ImageZoomer zoomer = new com.gmail.heagoo.common.ImageZoomer();
                Bitmap bitmap = zoomer.getImageThumbnail(curPath + "/"
                        + rec.fileName, 32, 32);
                viewHolder.icon.setImageBitmap(bitmap);
                detailInfo = strResolution + ": " + zoomer.getOriginWidth()
                        + " X " + zoomer.getOriginHeight();
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

        // Click listener
        // setupClickListener(convertView);

        return convertView;
    }

    private boolean isImageFile(String fileName) {
        if (fileName.endsWith(".bmp") || fileName.endsWith(".png")
                || fileName.endsWith(".jpg")) {
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

    public void updateData(String path, List<FileRecord> newList) {
        // Only update then directory is same
        synchronized (fileList) {
            if (path.equals(curPath)) {
                fileList.clear();
                fileList.addAll(newList);
            }
            this.notifyDataSetChanged();
        }
    }

    public void fileRenamed(String dirPath, String fileName, String newName) {
        synchronized (fileList) {
            for (FileRecord rec : fileList) {
                if (rec.fileName.equals(fileName)) {
                    rec.fileName = newName;
                    break;
                }
            }
            this.notifyDataSetChanged();
        }
    }

    public void fileDeleted(String dirPath, String fileName) {
        synchronized (fileList) {
            for (int i = 0; i < fileList.size(); i++) {
                FileRecord rec = fileList.get(i);
                if (rec.fileName.equals(fileName)) {
                    fileList.remove(i);
                    break;
                }
            }
            this.notifyDataSetChanged();
        }
    }

    private static class ViewHolder {
        ImageView icon;
        TextView filename;
        TextView desc1;
    }

}
