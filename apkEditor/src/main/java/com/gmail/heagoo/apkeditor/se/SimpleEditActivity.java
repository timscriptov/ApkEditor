package com.gmail.heagoo.apkeditor.se;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.gmail.heagoo.apkeditor.GlobalConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apklib.AXMLParser.IReferenceDecode;
import com.gmail.heagoo.common.ActivityUtil;
import com.gmail.heagoo.common.ApkInfoParser;
import com.gmail.heagoo.common.ApkInfoParser.AppInfo;
import com.gmail.heagoo.common.CustomizedLangActivity;
import com.gmail.heagoo.common.DynamicExpandListView;
import com.gmail.heagoo.common.SDCard;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimpleEditActivity extends CustomizedLangActivity implements OnClickListener,
        IReferenceDecode, IDirChanged {
    // 3 pages support
    private static final int BLACK_COLOR = 0xff333333;
    private static final int WHITE_COLOR = 0xffffffff;
    private static final int SKY_BLUE = 0xff04aeda;
    List<View> views;
    private String apkPath;
    private AppInfo apkInfo;
    private ZipFileListAdapter filesAdapter;
    private ImageListAdapter imagesAdapter;
    private AudioListAdapter audiosAdapter;
    // Thread & handler
    private MyHandler handler;
    private MyThread thread;
    // 3 listview & other header/bottom view
    private ListView fileListView;
    private DynamicExpandListView imageListView;
    private ListView audioListView;
    private LinearLayout centerLayout;
    private int currIndex = 0;
    private int screenWidth;
    private ImageView cursorImage;
    private ViewPager viewPager;
    private View fileLayout;
    private View imageLayout;
    private View audioLayout;
    private TextView fileTitle;
    private TextView imageTitle;
    private TextView audioTitle;
    // Save/Close Button
    private Button closeSaveBtn;
    // Summary text (to show tip)
    private TextView summaryTv;
    // Working directory to store temporary files
    private String workingDir;
    // Modified or not
    private boolean isModified = false;
    // To parse all the information inside the APK
    private ZipHelper zipHelper;

    private static String getPackage(int id) {
        if (id >>> 24 == 1) {
            return "android:";
        }
        return "";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_simpleedit);

        // Full screen or not
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        this.apkPath = ActivityUtil.getParam(getIntent(), "apkPath");

        try {
            this.apkInfo = new ApkInfoParser().parse(this, apkPath);
        } catch (Exception e) {
            String msg = getResources().getString(R.string.cannot_parse_apk);
            msg += ": " + e.getMessage();
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }

        if (apkInfo != null) {
            // Start thread to parse the apk file
            this.handler = new MyHandler(this);
            this.thread = new MyThread(this);
            thread.start();

            InitCursorImage();
            initViews();
            InitViewPager();
        } else {
            this.finish();
        }
    }

    @Override
    public void onDestroy() {
        if (filesAdapter != null) {
            filesAdapter.destroy();
        }
        if (audiosAdapter != null) {
            audiosAdapter.destroy();
        }
        // imagesAdapter.destroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Log.d("DEBUG", "onActivityResult, request=" + requestCode +
        // ", result="
        // + resultCode);
        if (requestCode == 0) {
            // APK successfully modified and installed
            if (resultCode == 1000) {
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save APK path
        {
            outState.putString("apkPath", this.apkPath);
        }

        super.onSaveInstanceState(outState);
    }

    @SuppressLint("InflateParams")
    private void InitViewPager() {
        this.viewPager = (ViewPager) findViewById(R.id.pagerView);
        this.views = new ArrayList<>();

        LayoutInflater inflater = getLayoutInflater();
        this.fileLayout = inflater.inflate(R.layout.pageitem_files, null);
        this.imageLayout = inflater.inflate(R.layout.pageitem_images, null);
        this.audioLayout = inflater.inflate(R.layout.pageitem_audios, null);

        views.add(fileLayout);
        views.add(imageLayout);
        views.add(audioLayout);
        viewPager.setAdapter(new MyViewPagerAdapter(views));
        viewPager.setCurrentItem(0);
        viewPager.setOnPageChangeListener(new MyOnPageChangeListener());
    }

    private void InitCursorImage() {
        this.cursorImage = (ImageView) findViewById(R.id.cursor);
        int bmpW = BitmapFactory.decodeResource(getResources(),
                R.drawable.pager_focus).getWidth();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        this.screenWidth = dm.widthPixels;
        int offset = (screenWidth / 3 - bmpW) / 2;
        Matrix matrix = new Matrix();
        matrix.postTranslate(offset, 0);
        cursorImage.setImageMatrix(matrix);
    }

    public void dataReady(boolean succeed) {
        // Make progress bar gone
        this.findViewById(R.id.progress_bar).setVisibility(View.GONE);

        if (succeed) {
            centerLayout.setVisibility(View.VISIBLE);
            initCenterView();
        } else {
            Toast.makeText(this, thread.getErrorMessage(), Toast.LENGTH_SHORT)
                    .show();
            this.finish();
        }
    }

    private void initCenterView() {
        // Get View
        this.fileListView = (ListView) fileLayout.findViewById(R.id.files_list);
        this.imageListView = (DynamicExpandListView) imageLayout
                .findViewById(R.id.images_list);
        this.audioListView = (ListView) audioLayout
                .findViewById(R.id.audios_list);

        // Files
        this.filesAdapter = new ZipFileListAdapter(this, this, zipHelper);
        fileListView.setAdapter(filesAdapter);
        fileListView.setOnItemClickListener(filesAdapter);
        fileListView.setOnItemLongClickListener(filesAdapter);

        // Image
        this.imagesAdapter = new ImageListAdapter(imageListView, this,
                zipHelper);
        imageListView.setAdapter(imagesAdapter);
        imageListView.setOnItemClickListener(imagesAdapter);
        imageListView.setOnItemLongClickListener(imagesAdapter);

        // Audio
        audiosAdapter = new AudioListAdapter(this, zipHelper);
        audioListView.setAdapter(audiosAdapter);
        audioListView.setOnItemClickListener(audiosAdapter);
        audioListView.setOnItemLongClickListener(audiosAdapter);
    }

    private void initViews() {
        this.centerLayout = (LinearLayout) this
                .findViewById(R.id.center_layout);
        this.summaryTv = (TextView) this.findViewById(R.id.tv_summary);
        this.fileTitle = (TextView) this.findViewById(R.id.files_label);
        this.imageTitle = (TextView) this.findViewById(R.id.images_label);
        this.audioTitle = (TextView) this.findViewById(R.id.audio_label);
        this.closeSaveBtn = (Button) this.findViewById(R.id.btn_close);

        // Set center content invisible
        centerLayout.setVisibility(View.INVISIBLE);

        this.fileTitle.setOnClickListener(this);
        this.imageTitle.setOnClickListener(this);
        this.audioTitle.setOnClickListener(this);
        this.closeSaveBtn.setOnClickListener(this);

        // Basic info
        if (apkInfo != null) {
            ImageView apkIcon = (ImageView) this.findViewById(R.id.apk_icon);
            apkIcon.setImageDrawable(apkInfo.icon);

            TextView labelTV = (TextView) this.findViewById(R.id.apk_label);
            labelTV.setText(apkInfo.label);
        }
    }

    private void centerViewChanged() {
        switch (this.currIndex) {
            case 0:
                fileTitle.setTextColor(SKY_BLUE);
            {
                String strDir = filesAdapter.getCurrentDir();
                summaryTv.setText(strDir);
            }
            break;
            case 1:
                imageTitle.setTextColor(SKY_BLUE);
                if (zipHelper != null) {
                    int num = zipHelper.getImageNum();
                    String str = (String) getResources().getText(
                            R.string.image_summary);
                    String msg = String.format(str, num);
                    summaryTv.setText(msg);
                }
                break;
            case 2:
                audioTitle.setTextColor(SKY_BLUE);
                if (zipHelper != null) {
                    int num = zipHelper.getAudioNum();
                    String str = (String) getResources().getText(
                            R.string.audio_summary);
                    String msg = String.format(str, num);
                    summaryTv.setText(msg);
                }
                break;
        }
    }

    // To view/download the pro version
    protected void viewProVersion() {
        String pkgName = this.getPackageName() + ".pro";
        Uri uri = Uri.parse("market://details?id=" + pkgName);
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            this.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                    .parse("http://play.google.com/store/apps/details?id="
                            + pkgName)));
        }
    }

    private void initData() throws Exception {

        this.workingDir = SDCard.makeWorkingDir(this);

        this.zipHelper = new ZipHelper(this.apkPath);
        zipHelper.parse();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.files_label) {
            this.currIndex = 0;
            viewPager.setCurrentItem(currIndex);
        } else if (id == R.id.audio_label) {
            this.currIndex = 2;
            viewPager.setCurrentItem(currIndex);
        } else if (id == R.id.images_label) {
            this.currIndex = 1;
            viewPager.setCurrentItem(currIndex);
        } else if (id == R.id.btn_close) {
            if (this.isModified) {
                makeAPK();
                this.finish();
            } else {
                this.finish();
            }
        }
    }

    // To make the new modified APK
    private void makeAPK() {
        Map<String, String> imgReplaces = imagesAdapter.getReplaces();
        Map<String, String> fileReplaces = filesAdapter.getReplaces();
        Map<String, String> audioReplaces = audiosAdapter.getReplaces();

        Intent intent = new Intent(this, ApkCreateActivity.class);
        ActivityUtil.attachParam(intent, "apkPath", this.apkPath);
        ActivityUtil.attachParam(intent, "packageName", apkInfo.packageName);
        ActivityUtil.attachParam(intent, "imageReplaces", imgReplaces);
        if (!fileReplaces.isEmpty() || !audioReplaces.isEmpty()) {
            fileReplaces.putAll(audioReplaces);
            ActivityUtil.attachParam(intent, "otherReplaces", fileReplaces);
        }

        startActivity(intent);
    }

    @Override
    public String getResReference(int data) {
        return String.format("@%s%08X", getPackage(data), data);
    }

    @Override
    public void dirChanged(String dir) {
        this.summaryTv.setText(dir);
    }

    public void setModified() {
        if (!this.isModified) {
            this.closeSaveBtn.setText(R.string.save);
            this.isModified = true;
        }
    }

    static enum CenterView {
        FILE, IMAGE, AUDIO
    }

    private static class MyHandler extends Handler {
        WeakReference<SimpleEditActivity> activityRef;

        public MyHandler(SimpleEditActivity activity) {
            activityRef = new WeakReference<SimpleEditActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            SimpleEditActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }
            switch (msg.what) {
                case 0:
                    activity.dataReady(true);
                    break;
                case 1:
                    activity.dataReady(false);
                    break;
            }
        }
    }

    private static class MyThread extends Thread {
        String err;
        WeakReference<SimpleEditActivity> activityRef;

        public MyThread(SimpleEditActivity activity) {
            activityRef = new WeakReference<SimpleEditActivity>(activity);
        }

        @Override
        public void run() {
            SimpleEditActivity activity = activityRef.get();
            if (activity != null) {
                try {
                    activity.initData();
                    activity.handler.sendEmptyMessage(0);
                } catch (Exception e) {
                    e.printStackTrace();
                    err = e.getMessage();
                    activity.handler.sendEmptyMessage(1);
                }
            }
        }

        public String getErrorMessage() {
            return err;
        }
    }

    public class MyViewPagerAdapter extends PagerAdapter {
        private List<View> mListViews;

        public MyViewPagerAdapter(List<View> mListViews) {
            this.mListViews = mListViews;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(mListViews.get(position));
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(mListViews.get(position), 0);
            return mListViews.get(position);
        }

        @Override
        public int getCount() {
            return mListViews.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }
    }

    public class MyOnPageChangeListener implements ViewPager.OnPageChangeListener {

        int one = screenWidth / 3;
        int two = one * 2;

        public void onPageScrollStateChanged(int arg0) {

        }

        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        public void onPageSelected(int position) {
            Animation animation = new TranslateAnimation(one * currIndex, one
                    * position, 0, 0);
            currIndex = position;
            animation.setFillAfter(true);
            animation.setDuration(200);
            cursorImage.startAnimation(animation);

            // Notify to change cursor/banner
            centerViewChanged();
        }
    }
}
