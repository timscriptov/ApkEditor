package com.gmail.heagoo.folderlist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.GlobalConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.PathUtil;
import com.gmail.heagoo.common.SDCard;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FolderListAdapter extends BaseAdapter {

    List<FileRecord> fileList = new ArrayList<FileRecord>();
    private Context ctx;
    private String rootPath;
    private String curPath;
    private IListItemProducer producer;
    private int themeId;

    public FolderListAdapter(Context ctx, String rootPath, String curPath, IListItemProducer producer) {
        this.ctx = ctx;
        this.rootPath = rootPath;
        this.curPath = curPath;
        this.producer = producer;
        this.themeId = GlobalConfig.instance(ctx).getThemeId();

        initListData(curPath);
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
            File dir = new File(path);
            if (!dir.exists()) {
                path = this.rootPath;
                dir = new File(path);
            }
            File[] subFiles = dir.listFiles();
            if (subFiles != null) {
                fileList.clear();

                for (File f : subFiles) {
                    FileRecord fr = new FileRecord();
                    fr.fileName = f.getName();
                    fr.isDir = f.isDirectory();
                    if (!fr.isDir) {
                        fr.totalSize = f.length();
                    } else {
                        fr.totalSize = -1;
                    }
                    fileList.add(fr);
                }

                Collections.sort(fileList, new FilenameComparator());

                // In root directory, will not show parent folder
                if (!path.equals(rootPath)) {
                    FileRecord fr = new FileRecord();
                    fr.fileName = "..";
                    fr.isDir = true;
                    fr.totalSize = -1;
                    fileList.add(0, fr);
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
                fr.totalSize = -1;
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

        viewHolder.filename.setText(rec.fileName);
        if (rec.fileName.equals("..")) {
            viewHolder.icon.setImageResource(R.drawable.folderutil_up);
        } else if (rec.isDir) {
            viewHolder.icon.setImageResource(R.drawable.folderutil_folder);
        } else {
            Drawable icon = producer.getFileIcon(curPath, rec);
            if (icon == null) {
                // Use the default icon
                viewHolder.icon.setImageResource(R.drawable.folderutil_file);
            } else {
                viewHolder.icon.setImageDrawable(icon);
            }
        }

        String detailInfo = producer.getDetail1(curPath, rec);
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

    public void openDirectory(String targetPath) {
        // Target path is not correct
        if (rootPath.startsWith(targetPath) && !targetPath.equals(rootPath)) {
            return;
        }
        initListData(targetPath);
        this.notifyDataSetChanged();
    }

    public void fileRenamed(String dirPath, String fileName, String newName) {
        synchronized (fileList) {
            for (FileRecord rec : fileList) {
                if (rec.fileName.equals(fileName)) {
                    rec.fileName = newName;
                    break;
                }
            }
        }
        this.notifyDataSetChanged();
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
        }
        this.notifyDataSetChanged();
    }

    private static class ViewHolder {
        ImageView icon;
        TextView filename;
        TextView desc1;
    }

}
