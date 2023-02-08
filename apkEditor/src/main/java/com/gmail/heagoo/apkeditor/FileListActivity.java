package com.gmail.heagoo.apkeditor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.util.LruCache;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.gmail.heagoo.apkeditor.EditModeDialog.IEditModeSelected;
import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.se.SimpleEditActivity;
import com.gmail.heagoo.apkeditor.util.DebugDialog;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.ApkInfoParser;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.common.Display;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.common.TextFileReader;
import com.gmail.heagoo.folderlist.FolderListWrapper;
import com.gmail.heagoo.folderlist.IListEventListener;
import com.gmail.heagoo.folderlist.IListItemProducer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FileListActivity extends CustomizedLangActivity implements IListEventListener,
        IListItemProducer, IEditModeSelected, OnClickListener {

    private static final String LAST_DIR_KEY = "apkDirectory";

    private TextView pathTV;
    private ListView listView;

    private FolderListWrapper foderWrapper;

    private ImageView sdCardImg;
    private View switchView;
    private boolean showingExtSdCard = false;

    private boolean isDarkTheme;

    private ApkParseThread parseThread = null;

    // Popup window for storage selection
    private PopupWindow popupWindow = null;

    // Image cache
    private LruCache<String, ApkInfoParser.AppInfo> apkIconCache =
            new LruCache<String, ApkInfoParser.AppInfo>(64);
//            {
//                @Override
//                protected void entryRemoved(boolean evicted, String key,
//                                            ApkInfoParser.AppInfo oldValue,
//                                            ApkInfoParser.AppInfo newValue) {
//                    //oldValue.recycle();
//                    if (oldValue.icon instanceof BitmapDrawable) {
//                        BitmapDrawable bitmapDrawable = (BitmapDrawable) oldValue.icon;
//                        Bitmap bitmap = bitmapDrawable.getBitmap();
//                        bitmap.recycle();
//                    }
//                }
//            }
    private int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;
    private android.os.Handler handler = new android.os.Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    foderWrapper.getAdapter().notifyDataSetChanged();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.activity_listfile);

        // Parse Thread
        if (parseThread == null) {
            parseThread = new ApkParseThread();
            parseThread.start();
        }

        initWithPermChecking();
    }

    @Override
    public void onDestroy() {
        parseThread.stopParse();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            init();
        }
    }

    private void initWithPermChecking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        } else {
            init();
        }
    }

    private void init() {
        String rootPath = "/";
        String curDir = getLastDirectory();

        this.pathTV = (TextView) this.findViewById(R.id.dirPath);
        pathTV.setText(curDir);

        this.listView = (ListView) this.findViewById(R.id.file_list);
        this.foderWrapper = new FolderListWrapper(this, listView, curDir,
                rootPath, this, this);

        // extsdcard
        this.sdCardImg = (ImageView) this.findViewById(R.id.imageView_extsdcard);
        this.switchView = this.findViewById(R.id.menu_switch_card);
        switchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    switchSdCard();
                } catch (Exception ignored) {
                }
            }
        });
        switchView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showStorageList(v);
                return true;
            }
        });

        // Search apk files
        Button searchBtn = (Button) this.findViewById(R.id.search_button);
        searchBtn.setOnClickListener(this);
    }

    // Show "SD Card", "Ext SD Card", "App Files"
    private void showStorageList(View parent) {
        if (popupWindow == null) {
            LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.popup_list, null);
            ListView lv_group = (ListView) view.findViewById(R.id.lvGroup);

            // Menu String
            ArrayList<String> groups = new ArrayList<>();
            Resources res = getResources();
            groups.add(res.getString(R.string.sd_card));
            groups.add(res.getString(R.string.ext_sd_card));
            groups.add(res.getString(R.string.app_files));

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, groups);
            lv_group.setAdapter(adapter);

            // Create a popup window
            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int width = Display.getWidth(this) * 2 / 5;
            int height = view.getMeasuredHeight();
            if (height == 0) {
                height = Display.dip2px(this, 150);
            } else {
                height = height * 3 + Display.dip2px(this, 2);
            }

            popupWindow = new PopupWindow(view, width, height);

            lv_group.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> adapterView, View view,
                                        int position, long id) {
                    switch (position) {
                        case 0: // SD card
                            openSdCard();
                            break;
                        case 1: // Ext SD Card
                            openExtSdCard();
                            break;
                        case 2: // App Files
                            openAppFiles();
                            break;
                    }

                    if (popupWindow != null) {
                        popupWindow.dismiss();
                    }
                }
            });
        }

        // Focus and allow disappear touch outside
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);

        // To make it can disappear when press return button
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        // Display position
        int xPos = windowManager.getDefaultDisplay().getWidth() / 2 - popupWindow.getWidth() / 2;

        popupWindow.showAsDropDown(parent, xPos, 0);
    }

    private void switchSdCard() {
        boolean switched = false;

        if (showingExtSdCard) {
            openSdCard();
            int id = isDarkTheme ? R.drawable.extsdcard_white : R.drawable.extsdcard;
            sdCardImg.setImageResource(id);
            switched = true;
        } else {
            if (openExtSdCard()) {
                int id = isDarkTheme ? R.drawable.sdcard_white : R.drawable.sdcard;
                sdCardImg.setImageResource(id);
                switched = true;
            }
        }

        if (switched) {
            showingExtSdCard = !showingExtSdCard;
        }
    }

    private boolean openExtSdCard() {
        String path = SDCard.getExternalStoragePath(this);
        if (path != null && !path.equals("")) {
            if (foderWrapper != null) {
                foderWrapper.openDirectory(path);
                return true;
            } else {
                return false;
            }
        } else {
            Toast.makeText(this, R.string.cannot_find_ext_sdcard,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean openAppFiles() {
        File f = this.getFilesDir();
        if (foderWrapper != null) {
            foderWrapper.openDirectory(f.getPath());
            return true;
        } else {
            return false;
        }
    }

    private void showDebugDialog() {
        DebugDialog dialog = new DebugDialog(this);
        dialog.show();

        TextFileReader reader = null;
        try {
            reader = new TextFileReader("/proc/mounts");
            dialog.addLog(reader.getContents());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openSdCard() {
        String path = SDCard.getRootDirectory();
        if (path != null && !path.equals("")) {
            if (foderWrapper != null) {
                foderWrapper.openDirectory(path);
            }
        }
    }

    @Override
    public void dirChanged(String newDir) {
        if (pathTV != null) {
            pathTV.setText(newDir);
        }
    }

    @Override
    public void fileRenamed(String dirPath, String oldName, String newName) {
    }

    @Override
    public void fileDeleted(String dirPath, String fileName) {
    }

    @Override
    public void itemLongClicked(ContextMenu menu, View v,
                                ContextMenuInfo menuInfo) {
    }

    @Override
    public boolean fileClicked(String filePath) {
        if (filePath.endsWith(".apk")) {
            // Save the directory
            String directory = filePath.substring(0, filePath.lastIndexOf('/'));
            setLastDirectory(directory);

            if (BuildConfig.PARSER_ONLY) {
                UserAppActivity.startFullEditActivity(this, filePath);
            } else if (BuildConfig.LIMIT_NEW_VERSION && !MainActivity.upgradedFromOldVersion(this)) {
                startFullEditActivity(filePath);
            } else {
                EditModeDialog dlg = new EditModeDialog(this, this, filePath);
                dlg.show();
            }

            return true;
        }
        return false;
    }

    private void startFullEditActivity(String filePath) {
        UserAppActivity.startFullEditActivity(this, filePath);
    }

    private String getLastDirectory() {
        String rootDir = SDCard.getRootDirectory();
        SharedPreferences sp = this.getSharedPreferences("config", 0);
        return sp.getString(LAST_DIR_KEY, rootDir);
    }

    private void setLastDirectory(String directory) {
        SharedPreferences sp = this.getSharedPreferences("config", 0);
        Editor editor = sp.edit();
        editor.putString(LAST_DIR_KEY, directory);
        editor.commit();
    }

    // Called from the non-UI thread
    private void updateList() {
        handler.removeMessages(0);
        handler.sendEmptyMessageDelayed(0, 300);
    }

    @Override
    public Drawable getFileIcon(String dirPath,
                                com.gmail.heagoo.folderlist.FileRecord record) {
        if (record == null) {
            return null;
        }
        // APK file
        if (!record.isDir && record.fileName.endsWith(".apk")) {
            String path = dirPath + "/" + record.fileName;
            ApkInfoParser.AppInfo info = this.apkIconCache.get(path);
            if (info != null) {
                return info.icon;
            }

            parseThread.addApk(path);

            return getResources().getDrawable(R.drawable.apk_icon);
        }
        return null;
    }

    @Override
    public String getDetail1(String dirPath,
                             com.gmail.heagoo.folderlist.FileRecord record) {
        if (!record.isDir && record.fileName.endsWith(".apk")) {
            String path = dirPath + "/" + record.fileName;
            ApkInfoParser.AppInfo info = this.apkIconCache.get(path);
            if (info != null) {
                return info.label;
            } else {
                return "";
            }
        }
        return null;
    }

    // Simple edit or full edit clicked
    @Override
    public void editModeSelected(int mode, String extraStr) {
        String filePath = extraStr;

        Intent intent = null;
        switch (mode) {
            case EditModeDialog.SIMPLE_EDIT:
                intent = new Intent(this, SimpleEditActivity.class);
                break;
            case EditModeDialog.FULL_EDIT:
                startFullEditActivity(filePath);
                return;
            case EditModeDialog.COMMON_EDIT:
                intent = new Intent(this, CommonEditActivity.class);
                break;
            case EditModeDialog.XML_FILE_EDIT:
                intent = new Intent(this, AxmlEditActivity.class);
                break;
        }

        if (intent != null) {
            ActivityUtil.attachParam(intent, "apkPath", filePath);
            startActivity(intent);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.search_button) {
            if (foderWrapper != null) {
                EditText et = (EditText) this.findViewById(R.id.keyword_edit);
                String keyword = et.getText().toString();
                String currentFolder = this.foderWrapper.getAdapter().getData(null);
                Intent intent = new Intent(this, ApkSearchActivity.class);
                ActivityUtil.attachParam(intent, "Keyword", keyword);
                ActivityUtil.attachParam(intent, "Path", currentFolder);
                this.startActivity(intent);
            }
        }
    }

    class ApkParseThread extends Thread {
        private ApkInfoParser apkParser = null;
        private boolean bStop = false;
        private List<String> apkList = new LinkedList<>();

        public void addApk(String path) {
            synchronized (apkList) {
                apkList.add(path);
                apkList.notify();
            }
        }

        public void stopParse() {
            bStop = true;
            synchronized (apkList) {
                apkList.notify();
            }
        }

        @Override
        public void run() {
            apkParser = new ApkInfoParser();

            while (!bStop) {
                String path = null;
                synchronized (apkList) {
                    if (apkList.isEmpty()) {
                        try {
                            apkList.wait();
                        } catch (InterruptedException e) {
                        }
                    }

                    if (!apkList.isEmpty()) {
                        path = apkList.remove(0);
                    } else {
                        path = null;
                    }
                }

                if (path == null) {
                    continue;
                }

                ApkInfoParser.AppInfo info = null;
                try {
                    info = apkParser.parse(FileListActivity.this, path);
                } catch (Throwable ignored) {
                }
                if (info != null) {
                    apkIconCache.put(path, info);
                    updateList();
                }
            } // end while
        } // end run()
    }
}
