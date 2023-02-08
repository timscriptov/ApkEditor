package com.gmail.heagoo.apkeditor.se;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.FileSelectDialog;
import com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection;
import com.gmail.heagoo.apkeditor.GlobalConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.DynamicExpandListView;
import com.gmail.heagoo.common.ImageZoomer;
import com.gmail.heagoo.imageviewlib.ViewZipImageActivity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

public class ImageListAdapter extends BaseAdapter implements
        OnItemClickListener, OnItemLongClickListener, IFileSelection,
        OnClickListener {

    private WeakReference<DynamicExpandListView> viewRef;
    private Activity ctx;

    private List<String> drawableFileList;
    private HashMap<String, DrawableEntry> drawableEntries;

    private ZipHelper zipHelper;
    private ZipImageZoomer zipImageZoomer;
    private ZipFile zfile;

    // All replaces (entry path -> file path)
    private Map<String, String> replaces = new HashMap<>();
    private LruCache<String, BitmapRec> imageBitmaps = new LruCache<String, BitmapRec>(
            32) {
        protected void entryRemoved(boolean evicted, String key,
                                    BitmapRec oldValue, BitmapRec newValue) {
            if (oldValue.showingBitmap != null) {
                oldValue.showingBitmap.recycle();
            }
        }
    };
    private int layoutId;

    public ImageListAdapter(DynamicExpandListView listView, Activity ctx,
                            ZipHelper zipHelper) {
        this.viewRef = new WeakReference<>(listView);
        this.ctx = ctx;
        this.zipHelper = zipHelper;

        // Get list information from ZipHelper
        this.drawableFileList = zipHelper.drawableNameList;
        this.drawableEntries = zipHelper.drawableEntries;
// sawsem theme
        this.layoutId = R.layout.item_zipfile;
//		switch (GlobalConfig.instance(ctx).getThemeId()) {
//			case GlobalConfig.THEME_DARK_DEFAULT:
//				this.layoutId = R.layout.item_zipfile_dark;
//				break;
//			case GlobalConfig.THEME_DARK_RUSSIAN:
//				this.layoutId = R.layout.item_zipfile_dark_ru;
//				break;
//		}

        try {
            this.zfile = new ZipFile(zipHelper.getFilePath());
            this.zipImageZoomer = new ZipImageZoomer(zfile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Cannot close?
        }
    }

    @Override
    protected void finalize() throws Throwable {
        closeZipFile();
        super.finalize();
    }

    // Close the zip file
    private void closeZipFile() {
        if (zfile != null) {
            try {
                zfile.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public int getCount() {
        return drawableEntries.size();
    }

    @Override
    public Object getItem(int position) {
        return drawableEntries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String entryName = drawableFileList.get(position);
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

        String detailInfo;
        viewHolder.filename.setText(entryName);

        // Set up image click listener
        viewHolder.editMenu.setId(position);
        viewHolder.editMenu.setOnClickListener(this);
        viewHolder.saveMenu.setId(drawableFileList.size() + position);
        viewHolder.saveMenu.setOnClickListener(this);

        // Get thumbnail for the image
        Bitmap bitmap;
        DrawableEntry entry = drawableEntries.get(entryName);

        BitmapRec buf = imageBitmaps.get(entryName);
        if (buf != null) {
            bitmap = buf.showingBitmap;
        } else {
            if (entry.replaceFile == null) {
                bitmap = zipImageZoomer.getImageThumbnail(
                        entry.bestQualifier + "/" + entryName, 32, 32);
            } else {
                ImageZoomer zoomer = new ImageZoomer();
                bitmap = zoomer.getImageThumbnail(entry.replaceFile, 32, 32);
            }
            // Save to cache
            buf = new BitmapRec();
            buf.showingBitmap = bitmap;
            imageBitmaps.put(entryName, buf);
        }
        viewHolder.icon.setImageBitmap(bitmap);
        detailInfo = entry.getAllPaths();
        viewHolder.desc1.setText(detailInfo);

        return convertView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View arg1, int position, long arg3) {
        viewImageFile(position);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View arg1,
                                   final int position, long arg3) {
        parent.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            public void onCreateContextMenu(ContextMenu menu, View v,
                                            ContextMenuInfo menuInfo) {
                // Extract
                MenuItem item1 = menu.add(0, Menu.FIRST, 0, R.string.extract);
                item1.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        extractFile(position);
                        return true;
                    }
                });
                // View
                MenuItem item3 = menu.add(0, Menu.FIRST + 1, 0, R.string.view);
                item3.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        viewImageFile(position);
                        return true;
                    }
                });
                // Replace
                MenuItem item2 = menu.add(0, Menu.FIRST + 2, 0,
                        R.string.replace);
                item2.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        showReplaceDlg(position);
                        return true;
                    }
                });
            }
        });
        return false;
    }

    // To view the image
    private void viewImageFile(int position) {
        String filename = drawableFileList.get(position);
        DrawableEntry entry = drawableEntries.get(filename);
        Intent intent = new Intent(ctx, ViewZipImageActivity.class);
        ActivityUtil.attachParam(intent, "fullScreen", GlobalConfig.instance(ctx).isFullScreen());
        if (entry.replaceFile == null) {
            ActivityUtil.attachParam(intent, "zipFilePath", zipHelper.getFilePath());
            ActivityUtil.attachParam(intent, "entryName", entry.bestQualifier + "/" + filename);
        } else {
            ActivityUtil.attachParam(intent, "imageFilePath", entry.replaceFile);
        }
        ctx.startActivity(intent);
    }

    //
    private void extractFile(int position) {
        String fileName = drawableFileList.get(position);
        DrawableEntry dEntry = drawableEntries.get(fileName);
        String entryName = dEntry.bestQualifier + "/" + fileName;
        String zipPath = zipHelper.getFilePath();
        ZipFileListAdapter.extractFromZip(ctx, zipPath, entryName);
    }

    // position is the clicked item index
    private void showReplaceDlg(int position) {
        String filename = drawableFileList.get(position);
//		FileReplaceDialog dlg = new FileReplaceDialog(ctx, filename, this);
//		dlg.show();
        FileSelectDialog replaceDlg = new FileSelectDialog(
                ctx, this, null, filename, ctx.getString(R.string.select_file_replace));
        replaceDlg.show();
    }

    // Add an image file replace
    @Override
    public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
        String filename = extraStr;
        DrawableEntry entry = drawableEntries.get(filename);
        entry.replaceFile = filePath;

        // Record the modifications
        for (String q : entry.qualifierList) {
            String path = q + "/" + filename;
            this.replaces.put(path, filePath);
        }

        // Remove the old cache
        this.imageBitmaps.remove(filename);

        // Update list view
        DynamicExpandListView lv = viewRef.get();
        if (lv != null) {
            lv.dataChanged();
        }

        // Notify that something modified
        ((SimpleEditActivity) ctx).setModified();
    }

    @Override
    public String getConfirmMessage(String filePath, String extraStr) {
        return null;
    }

    @Override
    public boolean isInterestedFile(String filename, String extraStr) {
        return ZipFileListAdapter.isImageFile(filename);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        // Click on edit image
        if (id < this.drawableFileList.size()) {
            showReplaceDlg(id);
        }
        // Click on the save image
        else if (id < this.drawableFileList.size() * 2) {
            extractFile(id - drawableFileList.size());
        }
    }

    public Map<String, String> getReplaces() {
        return this.replaces;
    }

    static class BitmapRec {
        Bitmap showingBitmap;
        int originWidth;
        int originHeight;
    }

}
