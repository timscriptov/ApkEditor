package com.gmail.heagoo.apkeditor.se;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
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
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.common.ZipUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AudioListAdapter extends BaseAdapter implements OnClickListener,
        OnCompletionListener, OnItemClickListener, OnItemLongClickListener,
        IFileSelection {

    private Activity ctx;
    private ZipHelper zipHelper;
    private List<String> audioPathList;

    // Record all the extracted audios
    private Set<String> extractedAudios = new HashSet<String>();

    // Record all the replaces
    private Map<String, String> replaces = new HashMap<String, String>();

    // Support music playing
    private String workingDir;
    private MediaPlayer mediaPlayer;
    private int playingPosition = -1;
    private String playingEntry;

    private int layoutId;

    public AudioListAdapter(Activity ctx, ZipHelper zipHelper) {
        this.ctx = ctx;
        this.zipHelper = zipHelper;
        this.audioPathList = zipHelper.audioPathList;
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
    }

    @Override
    public int getCount() {
        return audioPathList.size();
    }

    @Override
    public Object getItem(int position) {
        return audioPathList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String audioPath = audioPathList.get(position);
        ViewHolder viewHolder = null;
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

        int pos = audioPath.lastIndexOf('/');
        String audioName = audioPath.substring(pos + 1);
        String folder = audioPath.substring(0, pos + 1);

        // Image on click listener
        viewHolder.icon.setId(position);
        viewHolder.icon.setOnClickListener(this);
        viewHolder.editMenu.setId(audioPathList.size() + position);
        viewHolder.editMenu.setOnClickListener(this);
        viewHolder.saveMenu.setId(2 * audioPathList.size() + position);
        viewHolder.saveMenu.setOnClickListener(this);

        if (position != playingPosition) {
            viewHolder.icon.setImageResource(R.drawable.play);
        } else {
            viewHolder.icon.setImageResource(R.drawable.pause);
        }
        viewHolder.filename.setText(audioName);
        viewHolder.desc1.setText(folder);

        return convertView;
    }

    private String getNameByPath(String path) {
        int pos = path.lastIndexOf('/');
        String name = path.substring(pos + 1);
        return name;
    }

    protected void playPauseAudio(int position) throws Exception {
        String entryName = audioPathList.get(position);

        if (this.mediaPlayer == null) {
            this.workingDir = SDCard.makeWorkingDir(ctx);
            this.mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(this);
        } else {
            mediaPlayer.reset();
        }

        // Upzip
        if (!extractedAudios.contains(entryName)) {
            String name = getNameByPath(entryName);
            ZipUtil.unzipFileTo(zipHelper.getFilePath(), entryName, workingDir + name);
            extractedAudios.add(entryName);
        }

        // To play the audio
        if (!entryName.equals(this.playingEntry)) {
            String replacing = this.replaces.get(entryName);
            if (replacing != null) {
                mediaPlayer.setDataSource(replacing);
            } else {
                String name = getNameByPath(entryName);
                mediaPlayer.setDataSource(workingDir + name);
            }
            mediaPlayer.prepare();
            mediaPlayer.start();
            this.playingEntry = entryName;
            this.playingPosition = position;
        }
        // To stop the playing
        else {
            mediaPlayer.stop();
            this.playingEntry = null;
            this.playingPosition = -1;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        // Click on the play button
        if (id < audioPathList.size()) {
            try {
                playPauseAudio(id);
                this.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Edit/replace
        else if (id < 2 * audioPathList.size()) {
            showReplaceDlg(id - audioPathList.size());
        }

        // Extract/save
        else if (id < 3 * audioPathList.size()) {
            extractFile(id - 2 * audioPathList.size());
        }
    }

    public void destroy() {
        if (this.mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        playingEntry = null;
        playingPosition = -1;
        this.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                            long arg3) {
        try {
            playPauseAudio(position);
            this.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                // Replace
                MenuItem item2 = menu.add(0, Menu.FIRST + 1, 0,
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

    // position is the clicked item index
    private void showReplaceDlg(int position) {
        String entryPath = audioPathList.get(position);
//		FileReplaceDialog dlg = new FileReplaceDialog(ctx, entryPath, this);
//		dlg.show();

        FileSelectDialog replaceDlg = new FileSelectDialog(
                ctx, this, "", entryPath, ctx.getString(R.string.select_file_replace));
        replaceDlg.show();
    }

    // Add an image file replace
    @Override
    public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
        String entryPath = extraStr;
        this.replaces.put(entryPath, filePath);

        // Notify that something modified
        ((SimpleEditActivity) ctx).setModified();
    }

    @Override
    public String getConfirmMessage(String filePath, String extraStr) {
        return null;
    }

    @Override
    public boolean isInterestedFile(String filename, String extraStr) {
        return ZipHelper.isAudio(filename);
    }

    // Extract audio file to SD card
    private void extractFile(int position) {
        String entryName = audioPathList.get(position);
        String zipPath = zipHelper.getFilePath();
        ZipFileListAdapter.extractFromZip(ctx, zipPath, entryName);
    }

    // Return entry name to file path
    public Map<String, String> getReplaces() {
        return this.replaces;
    }
}
