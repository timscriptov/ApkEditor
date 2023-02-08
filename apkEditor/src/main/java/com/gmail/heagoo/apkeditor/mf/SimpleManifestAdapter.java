package com.gmail.heagoo.apkeditor.mf;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.base.R;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

interface ISectionDeleteCallback {
    public boolean sectionDeleted(LineRecord lineRec);

    public String getUndeletableReason();
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

    public LineRecord(int index, String content, String hanging) {
        this.lineIndex = index;
        this.lineData = content;

        while (lineData.startsWith(hanging)) {
            this.indent++;
            lineData = lineData.substring(hanging.length());
        }

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

    public String getSectionTag() {
        if (this.tag != null) {
            return tag;
        }

        String data = this.lineData;
        int startPos = -1;
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
            startPos = 1;
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

        String tag = null;
        if (endPos != -1) {
            tag = data.substring(startPos, endPos);
        } else {
            tag = data.substring(startPos);
        }
        tag = tag.trim();

        this.tag = tag;
        return tag;
    }

    public void dump() {
//		Log.d("DEBUG", String.format("%d:%s (%d, %d)", lineIndex,
//				getSectionTag(), sectionStart, sectionEnd));
    }
}

public class SimpleManifestAdapter extends BaseAdapter implements
        ISectionDeleteCallback {

    private static final String hanging = "\t";
    private List<LineRecord> allXmlLines;
    private List<LineRecord> xmlLines;
    private Activity ctx;
    private ISectionDeleteCallback callback;

    public SimpleManifestAdapter(Activity ctx, String xmlPath,
                                 ISectionDeleteCallback callback) {
        this.ctx = ctx;
        this.callback = callback;
        allXmlLines = new ArrayList<LineRecord>();
        xmlLines = new ArrayList<LineRecord>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(xmlPath));
            int index = 0;
            String data = null;
            while ((data = br.readLine()) != null) {
                LineRecord rec = new LineRecord(index++, data, hanging);
                allXmlLines.add(rec);
                xmlLines.add(rec);
            }
            br.close();
        } catch (Exception e) {
        }

        // Scan to initialize the line record
        initXmlLines();
    }

    private void initXmlLines() {
        try {
            Stack<LineRecord> stack = new Stack<LineRecord>();
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
                //lr.dump();
            }
        } catch (EmptyStackException e) {
            Toast.makeText(ctx, "Manifest parse error.", Toast.LENGTH_SHORT).show();
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
        LineRecord rec = xmlLines.get(position);
        ViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(ctx).inflate(
                    R.layout.item_manifestline, null);

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
        setContentClickable(viewHolder.lineData, rec);
        if (rec.indent > 0) {
            viewHolder.collapseImage.setVisibility(View.VISIBLE);
            viewHolder.collapseImage.setImageBitmap(getImage(rec));
            setCollapsable(viewHolder.collapseImage, rec);
        } else {
            viewHolder.collapseImage.setVisibility(View.GONE);
        }

        return convertView;
    }

    private void setContentClickable(TextView lineData, final LineRecord lineRec) {
        lineData.setClickable(true);
        lineData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//				// Create and show the dialog.
//				ManifestFragment fragment = ManifestFragment.makeFragment(
//						lineRec, ManifestListAdapter.this);
//				// FragmentManager fragmentMgr = fragment.getFragmentManager();
//				// FragmentTransaction ft = fragmentMgr.beginTransaction();
//				// fragment.show(ft, "dialog");
//				Dialog dlg = fragment.createDialog(ctx);
//				dlg.show();
            }
        });

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

    protected void collapseOrExpand(LineRecord lineRec) {
        synchronized (this) {
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
                i = rec.sectionEnd;
            }
        }
    }

    private Bitmap getImage(LineRecord lineRec) {
        int indent = lineRec.indent;
        final int height = 48;
        int width = 48 * indent;
        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);

        if (lineRec.sectionEnd != lineRec.lineIndex) {
            Bitmap arrow = null;
            if (!lineRec.collapsed) {
                arrow = BitmapFactory.decodeResource(ctx.getResources(),
                        R.drawable.arrow_down);
            } else {
                arrow = BitmapFactory.decodeResource(ctx.getResources(),
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
    public boolean sectionDeleted(LineRecord lineRec) {

        if (!isSectionDeletable(lineRec)) {
            return false;
        }

        // Notify the upper callback
        if (!callback.sectionDeleted(lineRec)) {
            return false;
        }

        // To hide some lines
        int startLine = lineRec.sectionStart;
        int endLine = lineRec.sectionEnd;

        synchronized (this) {
            for (int i = 0; i < allXmlLines.size(); i++) {
                LineRecord rec = allXmlLines.get(i);
                if (rec.lineIndex >= startLine && rec.lineIndex <= endLine) {
                    rec.deleted = true;
                }
            }

            updateDisplayLineData();
        }

        this.notifyDataSetChanged();

        return true;
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
    public String getUndeletableReason() {
        return null;
    }

    private static class ViewHolder {

        public TextView lineData;
        public ImageView collapseImage;

    }
}
