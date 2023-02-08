package com.gmail.heagoo.apkeditor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.XmlLineDialog.IXmlLineChanged;
import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

interface IManifestChangeCallback {
    // Try to delete the section
    // Return null if succeed, otherwise return the fail reason
    String tryToDeleteSection(LineRecord lineRec);

    // Set the new modified content
    void manifestChanged(String newContent);
}

class LineRecord {
    int lineIndex;
    String lineData;
    int indent;
    // Section start and end line index
    int sectionStart;
    int sectionEnd;

    boolean collapsed = false;
    boolean deleted = false;
    // Section Tag
    private String tag;

    LineRecord(int index, String content, String hanging) {
        this.lineIndex = index;
        this.lineData = content;

        // Here, hanging is "\t"
        while (lineData.startsWith(hanging)) {
            this.indent++;
            lineData = lineData.substring(hanging.length());
        }

        while (lineData.startsWith("    ")) {
            this.indent++;
            lineData = lineData.substring(4);
        }

        lineData = lineData.trim();

        sectionStart = -1;
        sectionEnd = -1;
    }

    // Return attr of android:name
    public String getName() {
        String name = null;

        String data = this.lineData;
        int startPos = data.indexOf("android:name=\"");
        if (startPos != -1) {
            startPos += 14;
            int endPos = data.indexOf("\"", startPos);
            if (endPos != -1) {
                name = data.substring(startPos, endPos);
            } else {
                name = data.substring(startPos);
            }
        }
        return name;
    }

    String getSectionTag() {
        if (this.tag != null) {
            return tag;
        }

        String data = this.lineData;
        int startPos;
        if (this.lineIndex != this.sectionStart) {
            startPos = data.indexOf("</");
            if (startPos != -1) {
                startPos += 2;
            }
        } else {
            startPos = data.indexOf("<");
            if (startPos != -1) {
                startPos += 1;
            }
        }
        if (startPos == -1) {
            return null;
        }

        int endPos = data.indexOf(" ");
        if (endPos == -1) {
            if (this.sectionStart == this.sectionEnd) {
                endPos = data.indexOf("/>");
                if (endPos != -1) {
                    endPos -= 1;
                }
            } else {
                endPos = data.indexOf(">");
            }
        }

        String tag;
        if (endPos != -1) {
            tag = data.substring(startPos, endPos);
        } else {
            tag = data.substring(startPos);
        }
        tag = tag.trim();

        this.tag = tag;
        return tag;
    }

}

class ManifestListAdapter extends BaseAdapter implements
        IManifestChangeCallback, IXmlLineChanged, OnItemClickListener,
        OnItemLongClickListener {

    private static final String hanging = "\t";
    private final List<LineRecord> xmlLines;
    private String xmlPath;
    private List<LineRecord> allXmlLines;
    private WeakReference<Activity> activityRef;
    private IManifestChangeCallback callback;
    private int layoutId;

    ManifestListAdapter(Activity activity, String xmlPath,
                        IManifestChangeCallback callback) {
        this.activityRef = new WeakReference<>(activity);
        this.callback = callback;
        this.xmlPath = xmlPath;
        allXmlLines = new ArrayList<>();
        xmlLines = new ArrayList<>();

        this.layoutId = R.layout.item_manifestline;
        // sawsem theme
//        switch (GlobalConfig.instance(activity).getThemeId()) {
//            case GlobalConfig.THEME_DARK_DEFAULT:
//                layoutId = R.layout.item_manifestline_dark;
//                break;
//            case GlobalConfig.THEME_DARK_RUSSIAN:
//                layoutId = R.layout.item_manifestline_dark_ru;
//                break;
//        }

        initData();
    }

    public static void showPromoteDialog(final Context ctx) {
        new AlertDialog.Builder(ctx)
                .setTitle(R.string.not_available)
                .setMessage(R.string.promote_msg)
                .setPositiveButton(R.string.view_pro_version,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialoginterface, int i) {
                                viewProVersion(ctx);
                            }
                        }).setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // To view/download the pro version
    private static void viewProVersion(Context ctx) {
        String pkgName = ctx.getPackageName() + ".pro";
        Uri uri = Uri.parse("market://details?id=" + pkgName);
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            ctx.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                    .parse("http://play.google.com/store/apps/details?id="
                            + pkgName)));
        }
    }

    private void initData() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(xmlPath));
            int index = 0;
            String data;
            while ((data = br.readLine()) != null) {
                LineRecord rec = new LineRecord(index++, data, hanging);
                allXmlLines.add(rec);
                xmlLines.add(rec);
            }
            br.close();
        } catch (Exception ignored) {
        }

        // Scan to initialize the line record
        initXmlLines();

    }

    private void initXmlLines() {
        try {
            Stack<LineRecord> stack = new Stack<>();
            for (int i = 0; i < xmlLines.size(); i++) {
                LineRecord lr = xmlLines.get(i);
                if (lr.indent <= 0) {
                    continue;
                }
                // Self ended
                if (lr.lineData.endsWith("/>")) {
                    lr.sectionStart = lr.lineIndex;
                    lr.sectionEnd = lr.lineIndex;
                }
                // Section end
                else if (lr.lineData.startsWith("</")) {
                    LineRecord startLine = stack.pop();
                    startLine.sectionEnd = lr.lineIndex;
                    lr.sectionStart = startLine.lineIndex;
                    lr.sectionEnd = lr.lineIndex;
                } else {
                    lr.sectionStart = lr.lineIndex;
                    stack.push(lr);
                }
                // lr.dump();
            }
        } catch (EmptyStackException e) {
            // Toast.makeText(ctx, "Manifest parse error.",
            // Toast.LENGTH_SHORT).show();
            for (int i = 0; i < xmlLines.size(); i++) {
                LineRecord lr = xmlLines.get(i);
                lr.indent = 0;
            }
        }
    }

    @Override
    public int getCount() {
        return xmlLines.size();
    }

    @Override
    public Object getItem(int arg0) {
        return xmlLines.get(arg0);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LineRecord rec;
        synchronized (xmlLines) {
            rec = xmlLines.get(position);
        }
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(activityRef.get()).inflate(layoutId, null);

            viewHolder = new ViewHolder();
            viewHolder.collapseImage = (ImageView) convertView
                    .findViewById(R.id.collapse_icon);
            viewHolder.lineData = (TextView) convertView
                    .findViewById(R.id.line_data);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.lineData.setText(rec.lineData);
        // setContentClickable(viewHolder.lineData, rec);
        if (rec.indent > 0) {
            viewHolder.collapseImage.setVisibility(View.VISIBLE);
            viewHolder.collapseImage.setImageBitmap(getImage(rec));
            setCollapsable(viewHolder.collapseImage, rec);
        } else {
            viewHolder.collapseImage.setVisibility(View.GONE);
        }

        return convertView;
    }

    private void setCollapsable(ImageView collapseImage,
                                final LineRecord lineRec) {
        collapseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collapseOrExpand(lineRec);
            }
        });
    }

    private void collapseOrExpand(LineRecord lineRec) {
        synchronized (xmlLines) {
            lineRec.collapsed = !lineRec.collapsed;
            updateDisplayLineData();
        }

        this.notifyDataSetChanged();
    }

    // Update xmlLinew from allXmlLines according to collapse and deleted
    // attribute
    private void updateDisplayLineData() {
        xmlLines.clear();
        for (int i = 0; i < allXmlLines.size(); i++) {
            LineRecord rec = allXmlLines.get(i);

            if (rec.deleted) {
                continue;
            }

            xmlLines.add(rec);

            // For the collapsed section, skip some lines
            if (rec.collapsed) {
                if (rec.sectionEnd > i) {
                    i = rec.sectionEnd;
                }
            }
        }

    }

    private Bitmap getImage(LineRecord lineRec) {
        int indent = lineRec.indent;
        final int height = 48;
        int width = 48 * indent;
        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);

        if (lineRec.sectionEnd != lineRec.lineIndex) {
            Bitmap arrow;
            if (!lineRec.collapsed) {
                arrow = BitmapFactory.decodeResource(activityRef.get().getResources(),
                        R.drawable.arrow_down);
            } else {
                arrow = BitmapFactory.decodeResource(activityRef.get().getResources(),
                        R.drawable.arrow_right);
            }

            // Draw the arrow
            Canvas c = new Canvas(b);
            Paint paint = new Paint();
            // paint.setColor(Color.WHITE);
            c.drawBitmap(arrow, width - 40, 8, paint);
        }

        return b;
    }

    @Override
    public String tryToDeleteSection(LineRecord lineRec) {

        if (!isSectionDeletable(lineRec)) {
            return activityRef.get().getResources().getString(R.string.section_undeletable);
        }

        if (!isProVersion()) {
            showPromoteDialog(activityRef.get());
            return "";
        }

        // To hide some lines
        int startLine = lineRec.sectionStart;
        int endLine = lineRec.sectionEnd;

        StringBuilder contentBuffer = new StringBuilder();
        synchronized (xmlLines) {
            for (int i = 0; i < allXmlLines.size(); i++) {
                LineRecord rec = allXmlLines.get(i);
                if (rec.lineIndex >= startLine && rec.lineIndex <= endLine) {
                    rec.deleted = true;
                }
                if (!rec.deleted) {
                    contentBuffer.append(rec.lineData);
                    contentBuffer.append('\n');
                }
            }

            updateDisplayLineData();
        }

        this.notifyDataSetChanged();

        // Further callback to save the content
        if (callback != null) {
            callback.manifestChanged(contentBuffer.toString());
        }

        return null;
    }

    private boolean isProVersion() {
        return BuildConfig.IS_PRO;
    }

    // Check whether the section can be deleted
    private boolean isSectionDeletable(LineRecord lineRec) {
        String tag = lineRec.getSectionTag();
        if ("manifest".equals(tag) || "application".equals(tag)) {
            return false;
        } else if ("activity".equals(tag) || "intent-filter".equals(tag)) {
            return !(containMainAction(lineRec));
        } else if ("action".equals(tag)) {
            return !lineRec.lineData.contains("android.intent.action.MAIN");
        } else if ("category".equals(tag)) {
            boolean bContain = lineRec.lineData
                    .contains("android.intent.category.LAUNCHER");
            return !bContain;
        }

        return true;
    }

    // Check whether contain main action inside the section represented by
    // lineRec
    private boolean containMainAction(LineRecord lineRec) {
        for (int i = lineRec.sectionStart; i < lineRec.sectionEnd; i++) {
            LineRecord rec = allXmlLines.get(i);
            // This is the main activity
            if ("action".equals(rec.getSectionTag())
                    && rec.lineData.contains("android.intent.action.MAIN")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void xmlLineChanged(int lineIndex, String newLine) {
        if (!isProVersion()) {
            showPromoteDialog(activityRef.get());
            return;
        }

        StringBuilder contentBuffer = new StringBuilder();

        synchronized (this) {
            for (LineRecord rec : allXmlLines) {
                if (rec.lineIndex == lineIndex) {
                    rec.lineData = newLine;
                }
                if (!rec.deleted) {
                    contentBuffer.append(rec.lineData);
                    contentBuffer.append('\n');
                }
            }
            for (LineRecord rec : xmlLines) {
                if (rec.lineIndex == lineIndex) {
                    rec.lineData = newLine;
                    break;
                }
            }
        }
        this.notifyDataSetChanged();

        // Further callback to save
        if (callback != null) {
            callback.manifestChanged(contentBuffer.toString());
        }
    }

    @Override
    public void manifestChanged(String newContent) {
        if (!isProVersion()) {
            showPromoteDialog(activityRef.get());
            return;
        }

        if (callback != null) {
            callback.manifestChanged(newContent);
        }
    }

    void reload() {
        synchronized (xmlLines) {
            allXmlLines.clear();
            xmlLines.clear();
            initData();
        }
        this.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                            long arg3) {
        LineRecord lineRec = null;
        try {
            lineRec = xmlLines.get(position);
        } catch (Exception ignored) {
        }

        if (lineRec != null) {
            XmlLineDialog dlg = new XmlLineDialog(
                    activityRef.get(),
                    ManifestListAdapter.this, lineRec.lineIndex, lineRec.lineData);
            dlg.show();
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                   int position, long arg3) {
        LineRecord lineRec = null;
        try {
            lineRec = xmlLines.get(position);
        } catch (Exception ignored) {
        }

        // Create and show the dialog.
        ManifestLongClickDlg dlg = new ManifestLongClickDlg(
                activityRef.get(), xmlPath, lineRec, ManifestListAdapter.this);
        dlg.show();
        return true;
    }

    private static class ViewHolder {
        TextView lineData;
        ImageView collapseImage;
    }
}
