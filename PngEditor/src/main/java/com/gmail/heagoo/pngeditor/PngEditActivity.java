package com.gmail.heagoo.pngeditor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.gmail.heagoo.common.Display;
import com.gmail.heagoo.common.ImageTool;
import com.gmail.heagoo.pngeditor.editor.RemoveBackground;
import com.gmail.heagoo.pngeditor.editor.Resize;
import com.gmail.heagoo.pngeditor.editor.Transparency;
import com.polites.android.GestureImageView;
import com.polites.android.GestureImageViewListener;

public class PngEditActivity extends AppCompatActivity implements
        View.OnClickListener, SeekBar.OnSeekBarChangeListener, GestureImageViewListener {

    private int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;

    private String filepath;
    private String filename;

    // Views in action bar
    private TextView scaleTv;
    private ImageView saveImage;
    private View normalButtons;
    private View editButtons;

    // Center image view
    private GestureImageView gestureView;

    // Bottom views
    private View toolsLayout;
    private View removeBgLayout;
    private View resizeLayout;
    private View transparencyLayout;
    private TextView toleranceTv;
    private SeekBar toleranceSeekbar;
    private TextView transparencyTv;
    private SeekBar transparencySeekbar;
    private TextView widthValueTv;
    private TextView heightValueTv;

    // Bitmap and modified bitmap
    private Bitmap imageBitmap;
    private Bitmap modifiedBitmap;
    private boolean imageModified = false;

    // Editor to edit the image
    private ImageEditor editor;

    // Popup window
    private PopupWindow scaleOptionWindow;
    private PopupWindow bgColorWindow;

    ////////////////////////////////////////////////////////////////////////////////
    // For seek bar
    public static void setMargins(View v, int l, int t, int r, int b) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(l, t, r, b);
            v.requestLayout();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void permissionCheck() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pngeditor_activity);

        initData();

        // Initialize View
        initView();

        permissionCheck();

        // Load image
        new ImageLoadTask().execute();
    }

    private void initData() {
        Intent intent = getIntent();
        filepath = intent.getStringExtra("filePath");
        //filepath = "/sdcard/ApkEditor/Images/search2.png";
        int pos = filepath.lastIndexOf('/');
        filename = filepath.substring(pos + 1);
    }

    private void initView() {
        // Action bar
        final ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(
                R.layout.pngeditor_actionbar, null);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.pngeditor_arrowleft);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(actionBarLayout);
        }

        // Views in action bar
        TextView filenameTv = (TextView) findViewById(R.id.tv_filename);
        filenameTv.setText(filename);
        this.scaleTv = (TextView) findViewById(R.id.tv_scale);
        scaleTv.setPaintFlags(scaleTv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        scaleTv.setOnClickListener(this);
        this.saveImage = (ImageView) findViewById(R.id.btn_save);
        this.normalButtons = findViewById(R.id.normal_action_layout);
        this.editButtons = findViewById(R.id.edit_action_layout);
        findViewById(R.id.btn_save).setOnClickListener(this);
        findViewById(R.id.btn_confirm).setOnClickListener(this);
        findViewById(R.id.btn_cancel).setOnClickListener(this);

        //// Views in tools layout
        this.toolsLayout = findViewById(R.id.tools_layout);
        this.removeBgLayout = findViewById(R.id.remove_bg_layout);
        this.resizeLayout = findViewById(R.id.resize_layout);
        this.transparencyLayout = findViewById(R.id.transparency_layout);

        this.toleranceTv = (TextView) findViewById(R.id.tv_tolerance);
        this.toleranceSeekbar = (SeekBar) findViewById(R.id.seekbar_tolerance);
        toleranceSeekbar.setOnSeekBarChangeListener(this);
        this.transparencyTv = (TextView) findViewById(R.id.tv_transparency);
        this.transparencySeekbar = (SeekBar) findViewById(R.id.seekbar_transparency);
        transparencySeekbar.setOnSeekBarChangeListener(this);

        // Resize related
        this.widthValueTv = (TextView) findViewById(R.id.tv_width_value);
        this.heightValueTv = (TextView) findViewById(R.id.tv_height_value);
        widthValueTv.setPaintFlags(widthValueTv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        heightValueTv.setPaintFlags(heightValueTv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        findViewById(R.id.width_labelvalue).setOnClickListener(this);
        findViewById(R.id.height_labelvalue).setOnClickListener(this);

        findViewById(R.id.btn_remove_bg).setOnClickListener(this);
        findViewById(R.id.btn_remove_it).setOnClickListener(this);

        findViewById(R.id.btn_resize).setOnClickListener(this);
        findViewById(R.id.btn_do_resize).setOnClickListener(this);

        findViewById(R.id.btn_transparency).setOnClickListener(this);
        findViewById(R.id.btn_transparency_preview).setOnClickListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_background) {
            showBgColorWindow();
            return true;
        } else if (id == R.id.action_help) {
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pngeditor_main, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (inEditingMode()) {
            closeEditorWithTip();
        } else {
            if (imageModified) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setMessage(R.string.image_save_tip)
                        .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                saveTheImage();
                                PngEditActivity.this.finish();
                            }
                        })
                        .setNegativeButton(R.string.discard, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                PngEditActivity.this.finish();
                            }
                        })
                        .setNeutralButton(android.R.string.cancel, null);
                builder.show();
            } else {
                //NavUtils.navigateUpFromSameTask(this);
                this.finish();
            }
        }
    }

    private boolean inEditingMode() {
        return toolsLayout.getVisibility() != View.VISIBLE;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        // Open edit layout: remove background
        if (id == R.id.btn_remove_bg) {
            switchToEditLayout(new RemoveBackground(), removeBgLayout);
        }
        // Remove the background
        else if (id == R.id.btn_remove_it) {
            int tolerance = toleranceSeekbar.getProgress();
            editor.setParam("tolerance", tolerance);
            this.modifiedBitmap = editor.edit(imageBitmap);
            if (gestureView != null && modifiedBitmap != null) {
                gestureView.setImageBitmap(modifiedBitmap);
            }
        }
        // Confirm current edit
        else if (id == R.id.btn_confirm) {
            if (editor.isModified()) {
                this.imageBitmap = modifiedBitmap;
                this.imageModified = true;
                switchToNormalLayout();
            } else {
                Toast.makeText(this, R.string.no_change, Toast.LENGTH_SHORT).show();
            }
        }
        // Save the modified image
        else if (id == R.id.btn_save) {
            if (imageModified) {
                saveTheImage();
            } else {
                Toast.makeText(this, R.string.no_change, Toast.LENGTH_SHORT).show();
            }
        }
        // Cancel the modification
        else if (id == R.id.btn_cancel) {
            closeEditorWithTip();
        }
        // Switch to resize layout
        else if (id == R.id.btn_resize) {
            switchToEditLayout(new Resize(), resizeLayout);
        }
        // Resize the image
        else if (id == R.id.btn_do_resize) {
            doResize();
        }
        // Switch to transparency layout
        else if (id == R.id.btn_transparency) {
            switchToEditLayout(new Transparency(), transparencyLayout);
        }
        // Transparency preview
        else if (id == R.id.btn_transparency_preview) {
            int transparency = transparencySeekbar.getProgress() * 255 / transparencySeekbar.getMax();
            editor.setParam("transparency", transparency);
            this.modifiedBitmap = editor.edit(imageBitmap);
            if (gestureView != null && modifiedBitmap != null) {
                gestureView.setImageBitmap(modifiedBitmap);
            }
        }
        // Set Resized width and height
        else if (id == R.id.width_labelvalue) {
            showWidthHeightInputDialog(0);
        } else if (id == R.id.height_labelvalue) {
            showWidthHeightInputDialog(1);
        }
        // Show scale options (popup window)
        else if (id == R.id.tv_scale) {
            showScaleOptionPopupWindow(view);
        }
    }

    // Close current editor, if modified, show tip
    private void closeEditorWithTip() {
        if (editor == null) {
            return;
        }
        if (editor.isModified()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setMessage(R.string.image_modified_tip)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Revert to original bitmap
                            gestureView.setImageBitmap(imageBitmap);
                            switchToNormalLayout();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null);
            builder.show();
        } else {
            switchToNormalLayout();
        }
    }

    private void saveTheImage() {
        if (ImageTool.saveAsPng(imageBitmap, filepath)) {
            Toast.makeText(this, R.string.image_saved, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.image_save_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void showScaleOptionPopupWindow(View parent) {
        if (this.scaleOptionWindow == null) {
            View view = View.inflate(this, R.layout.pngeditor_scale_options, null);
            View.OnClickListener scaleBtnListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    float scale = 1;
                    int id = view.getId();
                    if (id == R.id.btn_scale_fit) {
                        scale = gestureView.getFitScale();
                    } else if (id == R.id.btn_scale_100) {
                        scale = 1;
                    } else if (id == R.id.btn_scale_200) {
                        scale = 2;
                    } else if (id == R.id.btn_scale_400) {
                        scale = 4;
                    }
                    gestureView.setScale(scale);
                    gestureView.redraw();
                    onScale(scale);
                    scaleOptionWindow.dismiss();
                }
            };
            view.findViewById(R.id.btn_scale_fit).setOnClickListener(scaleBtnListener);
            view.findViewById(R.id.btn_scale_100).setOnClickListener(scaleBtnListener);
            view.findViewById(R.id.btn_scale_200).setOnClickListener(scaleBtnListener);
            view.findViewById(R.id.btn_scale_400).setOnClickListener(scaleBtnListener);
            int height = findViewById(R.id.btn_remove_bg).getHeight();
            scaleOptionWindow = new PopupWindow(view, Display.getWidth(this), height);
        }
        scaleOptionWindow.setFocusable(true);
        scaleOptionWindow.setOutsideTouchable(true);
        scaleOptionWindow.showAsDropDown(parent);
    }

    private void showBgColorWindow() {
        if (this.bgColorWindow == null) {
            View view = View.inflate(this, R.layout.pngeditor_bgcolor, null);
            View.OnClickListener btnListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    bgColorWindow.dismiss();
                }
            };
            view.findViewById(R.id.btn_close).setOnClickListener(btnListener);
            SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekbar_bgcolor);
            seekBar.setOnSeekBarChangeListener(PngEditActivity.this);
            view.measure(0, 0);
            int height = view.getMeasuredHeight();
            bgColorWindow = new PopupWindow(view, Display.getWidth(this), height);
        }
        bgColorWindow.setFocusable(true);
        bgColorWindow.setOutsideTouchable(true);
        bgColorWindow.showAsDropDown(scaleTv); // Borrow scale text view as parent
    }

    // To input the resized width and height
    private void showWidthHeightInputDialog(int index) {
        View view = View.inflate(this, R.layout.pngeditor_dlg_size_input, null);
        final EditText widthEt = (EditText) view.findViewById(R.id.et_width);
        final EditText heightEt = (EditText) view.findViewById(R.id.et_height);
        widthEt.setText(widthValueTv.getText());
        heightEt.setText(heightValueTv.getText());
        switch (index) {
            case 0:
                widthEt.requestFocus();
                break;
            case 1:
                heightEt.requestFocus();
                break;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.input_new_size)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Check the input
                        String strWidth = widthEt.getText().toString();
                        String strHeight = heightEt.getText().toString();
                        boolean isValid = false;
                        try {
                            int width = Integer.valueOf(strWidth);
                            int height = Integer.valueOf(strHeight);
                            isValid = (width > 0 && height > 0 && width < 32768 && height < 32768);
                        } catch (Exception ignored) {
                        }
                        if (isValid) {
                            widthValueTv.setText(strWidth);
                            heightValueTv.setText(strHeight);
                        } else {
                            Toast.makeText(PngEditActivity.this,
                                    R.string.invalid_input,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    // Check the input and then resize the image
    private void doResize() {
        String strWidth = widthValueTv.getText().toString();
        String strHeight = heightValueTv.getText().toString();
        CheckBox cb = (CheckBox) findViewById(R.id.cb_without_zoom);
        boolean withoutZoom = cb.isChecked();

        // Get width
        int width;
        try {
            width = Integer.valueOf(strWidth);
        } catch (Exception ignored) {
            return;
        }

        // Get height
        int height;
        try {
            height = Integer.valueOf(strHeight);
        } catch (Exception ignored) {
            return;
        }

        editor.setParam("width", width);
        editor.setParam("height", height);
        editor.setParam("zooming", !withoutZoom);

        this.modifiedBitmap = editor.edit(imageBitmap);
        if (gestureView != null && modifiedBitmap != null) {
            gestureView.setImageBitmap(modifiedBitmap);
        }
    }

    private void switchToEditLayout(ImageEditor editor, View editLayout) {
        this.editor = editor;

        editLayout.setVisibility(View.VISIBLE);
        toolsLayout.setVisibility(View.INVISIBLE);

        editButtons.setVisibility(View.VISIBLE);
        normalButtons.setVisibility(View.INVISIBLE);

        // Set current image height and width
        widthValueTv.setText(String.valueOf(imageBitmap.getWidth()));
        heightValueTv.setText(String.valueOf(imageBitmap.getHeight()));
    }

    private void switchToNormalLayout() {
        this.editor = null;

        // TODO: check edited or not
        removeBgLayout.setVisibility(View.INVISIBLE);
        resizeLayout.setVisibility(View.INVISIBLE);
        transparencyLayout.setVisibility(View.INVISIBLE);
        toolsLayout.setVisibility(View.VISIBLE);

        editButtons.setVisibility(View.INVISIBLE);
        normalButtons.setVisibility(View.VISIBLE);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int id = seekBar.getId();

        int leftPad = seekBar.getPaddingLeft();
        int rightPad = seekBar.getPaddingRight();
        int width = seekBar.getWidth() - leftPad - rightPad;
        int thumbPos = leftPad + width * seekBar.getProgress() / seekBar.getMax();
        if (id == R.id.seekbar_tolerance) {
            toleranceTv.setText(String.valueOf(progress));
            toleranceTv.measure(0, 0);
            setMargins(toleranceTv, thumbPos - toleranceTv.getMeasuredWidth() / 2, 0, 0, 0);
        } else if (id == R.id.seekbar_transparency) {
            transparencyTv.setText(String.valueOf(progress));
            transparencyTv.measure(0, 0);
            setMargins(transparencyTv, thumbPos - transparencyTv.getMeasuredWidth() / 2, 0, 0, 0);
        } else if (id == R.id.seekbar_bgcolor) {
            if (progress < 0) progress = 0;
            if (progress > 255) progress = 255;
            progress = 255 - progress;
            int color = 0xff000000 | (progress << 16) | (progress << 8) | progress;
            findViewById(R.id.overall_layout).setBackgroundColor(color);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    ////////////////////////////////////////////////////////////////////////////////
    // Gesture image view listener

    @Override
    public void onTouch(float x, float y) {

    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onScale(float scale) {
        scaleTv.setText(String.format("%d%%", (int) (scale * 100)));
    }

    @Override
    public void onPosition(float x, float y) {

    }

    ////////////////////////////////////////////////////////////////////////////////


    class ImageLoadTask extends AsyncTask<Void, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            imageBitmap = BitmapFactory.decodeFile(filepath);
            return imageBitmap != null;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                gestureView = new GestureImageView(PngEditActivity.this, imageBitmap);
                gestureView.setGestureImageViewListener(PngEditActivity.this);

                gestureView.setLayoutParams(params);
                gestureView.setStartingScale(1);

                LinearLayout layout = (LinearLayout) findViewById(R.id.image_layout);

                layout.addView(gestureView);

                // Set scale text
                onScale(gestureView.getScale());
            } else {
                String message = String.format(getString(R.string.cannot_open_file), filepath);
                Toast.makeText(PngEditActivity.this, message, Toast.LENGTH_LONG).show();
                PngEditActivity.this.finish();
            }
        }
    }
}
