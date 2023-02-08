package com.gmail.heagoo.appdm;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.appdm.base.R;
import com.gmail.heagoo.appdm.util.FileRecord;
import com.gmail.heagoo.appdm.util.FilenameComparator;
import com.gmail.heagoo.common.CommandInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RootFileAdapter extends BaseAdapter {

    private WeakReference<PrefOverallActivity> activityRef;
    private String rootDir;
    private String curDir;
    private boolean isRootMode;
    private boolean isDark;

    private List<FileRecord> fileList = new ArrayList<FileRecord>();

    private String strFileSize;

    public RootFileAdapter(PrefOverallActivity activity, String rootDir,
                           boolean rootMode, boolean isDark) {
        this.activityRef = new WeakReference<PrefOverallActivity>(activity);
        this.rootDir = rootDir;
        this.isRootMode = rootMode;
        this.isDark = isDark;
        this.curDir = rootDir;

        this.strFileSize = activity.getString(R.string.appdm_file_size) + " ";

        new FileListThread(curDir).start();
    }

    private void showMessage_nonUiThread(final String msg) {
        activityRef.get().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activityRef.get(), msg, Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    protected List<FileRecord> listFiles(String dirPath) {
        return listFiles(dirPath, false);
    }

    protected List<FileRecord> listFiles(String dirPath, boolean bManyFiles) {
        if (isRootMode) {
            CommandInterface rc = activityRef.get().createCommandRunner();
            String strCommand = String.format("ls -l %s", dirPath);
            boolean readWhileExec = bManyFiles;
            if (rc.runCommand(strCommand, null, 5000, readWhileExec)) {
                String output = rc.getStdOut();
                if (output != null) {
                    return parseLsOutput(output);
                }
            }

            String errMsg = "Read error, please try again.";
            showMessage_nonUiThread(errMsg);
            return null;
        }
        // Directly list files by java interfaces
        else {
            List<FileRecord> subFiles = new ArrayList<FileRecord>();

            File dir = new File(dirPath);
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    FileRecord rec = new FileRecord();
                    rec.fileName = f.getName();
                    rec.isDir = f.isDirectory();
                    rec.size = (int) f.length();
                    subFiles.add(rec);
                }
            }

            return subFiles;
        }
    }

    // Update the list
    // can be called from non-ui thread
    public void updateList(final String dirPath,
                           final List<FileRecord> subFiles) {
        Activity activity = activityRef.get();
        if (activity == null) {
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                synchronized (fileList) {
                    curDir = dirPath;
                    fileList.clear();
                    if (!curDir.equals(rootDir)) {
                        fileList.add(makeParentFileRec());
                    }
                    fileList.addAll(subFiles);
                }
                notifyDataSetChanged();
            }
        });
    }

    protected FileRecord makeParentFileRec() {
        FileRecord rec = new FileRecord();
        rec.fileName = "..";
        rec.isDir = true;
        return rec;
    }

    // Parse "ls -l" outputs to file records
    private List<FileRecord> parseLsOutput(String output) {
        List<FileRecord> result = new ArrayList<FileRecord>();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new StringReader(output));
            String line = br.readLine();
            while (line != null) {
                String segs[] = line.split("\\s+");
                if (segs.length >= 5) {
                    FileRecord rec = null;
                    char c = segs[0].charAt(0);
                    if (c == '-') { // normal file
                        rec = new FileRecord();
                        rec.isDir = false;
                        try {
                            rec.size = Integer.valueOf(segs[3]);
                        } catch (Throwable t) {
                        }
                    } else if (c == 'd') { // directory
                        rec = new FileRecord();
                        rec.isDir = true;
                        rec.size = 0;
                    }

                    if (rec != null) {
                        rec.fileName = segs[segs.length - 1];
                        result.add(rec);
                    }
                }
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }

        return result;
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final FileRecord record = (FileRecord) getItem(position);
        if (record == null) {
            return null;
        }

        ViewHolder viewHolder = null;
        // sawsem theme
        if (convertView == null) {
            convertView = LayoutInflater.from(activityRef.get()).inflate((R.layout.appdm_item_file),
                    null);

            viewHolder = new ViewHolder();
            viewHolder.icon = (ImageView) convertView
                    .findViewById(R.id.file_icon);
            viewHolder.title = (TextView) convertView
                    .findViewById(R.id.filename);
            viewHolder.subTitle = (TextView) convertView
                    .findViewById(R.id.detail1);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (record.isDir) {
            viewHolder.icon.setImageResource(R.drawable.folderutil_folder);
            viewHolder.subTitle.setVisibility(View.GONE);
        } else {
            viewHolder.icon.setImageResource(R.drawable.folderutil_file);
            viewHolder.subTitle.setText(strFileSize + record.size);
            viewHolder.subTitle.setVisibility(View.VISIBLE);
        }
        viewHolder.title.setText(record.fileName);

        return convertView;
    }

    public String getData(List<FileRecord> records) {
        synchronized (fileList) {
            if (records != null) {
                records.addAll(fileList);
            }
            return curDir;
        }
    }

    static class ViewHolder {
        public ImageView icon;
        public TextView title;
        public TextView subTitle;
    }

    class FileListThread extends Thread {
        private String dirPath;

        public FileListThread(String dirPath) {
            this.dirPath = dirPath;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            List<FileRecord> subFiles = listFiles(dirPath);
            if (subFiles != null) {
                Collections.sort(subFiles, new FilenameComparator());
                updateList(dirPath, subFiles);
            }
        }
    }
}
