package com.gmail.heagoo.apkeditor;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.ac.AutoCompleteAdapter;
import com.gmail.heagoo.apkeditor.ac.EditTextWithTip;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.ActivityUtil;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MatchedTextListAdapter extends BaseExpandableListAdapter
        implements OnClickListener {

    // File path -> matched lines
    private final Map<String, List<MatchedLineItem>> matchedContents;
    // Do not show path prefix in the group label
    private String pathPrefix;
    private String keyword;
    private ArrayList<String> filePathList;
    private WeakReference<ApkInfoActivity> activityRef;
    private WeakReference<ExpandableListView> listviewRef;
    private boolean[] replaceClicked;
    private boolean[] editClicked;
    private int themeId;
    private boolean isDark;

    // To decide how many chars to cut
    private int lineTotalWidth = 0;
    private int keywordWidth;
    private int titleWidth = 0;

    // For replace function
    private boolean notShowReplaceDlg = false;
    private String strReplace = null;

    MatchedTextListAdapter(WeakReference<ApkInfoActivity> activityRef,
                           ExpandableListView listView, String pathPrefix,
                           List<String> fileList, String keyword) {
        this.activityRef = activityRef;
        this.listviewRef = new WeakReference<>(listView);
        this.pathPrefix = pathPrefix + "/";
        this.keyword = keyword;
        this.themeId = GlobalConfig.instance(activityRef.get()).getThemeId();
        this.isDark = GlobalConfig.instance(activityRef.get()).isDarkTheme();

        filePathList = new ArrayList<>();
        matchedContents = new HashMap<>();

        for (String filePath : fileList) {
            filePathList.add(filePath);
        }

        this.replaceClicked = new boolean[fileList.size()];
        this.editClicked = new boolean[fileList.size()];
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        String filePath = filePathList.get(groupPosition);
        List<MatchedLineItem> item = matchedContents.get(filePath);
        if (item != null && childPosition < item.size()) {
            return item.get(childPosition);
        } else {
            return null;
        }
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return groupPosition * 65536 + childPosition;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        MatchedLineItem matchedLine = (MatchedLineItem) getChild(groupPosition,
                childPosition);

        ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) activityRef.get()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            int resId = R.layout.item_matchedline;
            // sawsem theme
//            switch (themeId) {
//                case GlobalConfig.THEME_DARK_DEFAULT:
//                    resId = R.layout.item_matchedline_dark;
//                    break;
//                case GlobalConfig.THEME_DARK_RUSSIAN:
//                    resId = R.layout.item_matchedline_dark_ru;
//                    break;
//            }
            convertView = layoutInflater.inflate(resId, null);
            viewHolder = new ViewHolder();
            viewHolder.matchedLine = (TextView) convertView
                    .findViewById(R.id.tv_line);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String lineRec;
        if (matchedLine != null) {
            Paint paint = new Paint();
            paint.setTextSize(viewHolder.matchedLine.getTextSize());

            if (this.lineTotalWidth == 0) {
                this.lineTotalWidth = viewHolder.matchedLine.getWidth();
                this.keywordWidth = (int) paint.measureText(keyword);
            }

            String header = "" + matchedLine.lineIndex + ": ";
            int headerWidth = (int) paint.measureText(header);
            int requiredWidth = (int) paint
                    .measureText(header + matchedLine.lineContent);

            int cutChars = 0;
            if (lineTotalWidth < requiredWidth) {
                if (matchedLine.matchedPosition > 0) {
                    int actualWidth = (int) paint
                            .measureText(matchedLine.lineContent.substring(0,
                                    matchedLine.matchedPosition));
                    int idealWidth = (lineTotalWidth - headerWidth
                            - keywordWidth) / 2;
                    if (actualWidth > idealWidth) {
                        cutChars = matchedLine.matchedPosition
                                - (matchedLine.matchedPosition * idealWidth
                                / actualWidth - 2);
                    }

                    if (cutChars > matchedLine.matchedPosition) {
                        cutChars = matchedLine.matchedPosition;
                    }
                }
            }

            int highlightStart = header.length() + matchedLine.matchedPosition;
            if (cutChars > 0) {
                lineRec = header + "..."
                        + matchedLine.lineContent.substring(cutChars);
                highlightStart -= cutChars - 3;
            } else {
                lineRec = header + matchedLine.lineContent;
            }

            SpannableString sp = new SpannableString(lineRec);
            sp.setSpan(new ForegroundColorSpan(Color.RED), highlightStart,
                    highlightStart + keyword.length(),
                    Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            viewHolder.matchedLine.setText(sp);
        }

        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        String filePath = filePathList.get(groupPosition);
        List<MatchedLineItem> matched = matchedContents.get(filePath);
        if (matched != null) {
            return matched.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getGroup(int groupPosition) {
        return filePathList.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return filePathList.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String filePath = this.filePathList.get(groupPosition);
        String groupLabel = filePath.substring(pathPrefix.length());

        GroupViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) activityRef.get()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            // sawsem theme
            int resId = R.layout.item_matchedfile;
//            switch (themeId) {
//                case GlobalConfig.THEME_DARK_DEFAULT:
//                    resId = R.layout.item_matchedfile_dark;
//                    break;
//                case GlobalConfig.THEME_DARK_RUSSIAN:
//                    resId = R.layout.item_matchedfile_dark_ru;
//                    break;
//            }
            convertView = layoutInflater.inflate(resId, null);
            viewHolder = new GroupViewHolder();
            viewHolder.groupLabel = (TextView) convertView
                    .findViewById(R.id.tv_filepath);
            viewHolder.editMenu = convertView.findViewById(R.id.menu_edit);
            viewHolder.replaceMenu = convertView
                    .findViewById(R.id.menu_replace);
            viewHolder.editImage = (ImageView) convertView
                    .findViewById(R.id.image_edit);
            viewHolder.replaceImage = (ImageView) convertView
                    .findViewById(R.id.image_replace);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (GroupViewHolder) convertView.getTag();
        }

        int editResId = this.editClicked[groupPosition] ? R.drawable.pencil_blue
                : (this.isDark ? R.drawable.pencil_white : R.drawable.pencil);
        viewHolder.editImage.setImageResource(editResId);

        int replaceId = this.replaceClicked[groupPosition]
                ? R.drawable.ic_replace_blue
                : (this.isDark ? R.drawable.ic_replace_white
                : R.drawable.ic_replace);
        viewHolder.replaceImage.setImageResource(replaceId);

        TextView groupTextView = viewHolder.groupLabel;
        groupTextView.setTypeface(null, Typeface.BOLD);
        groupTextView.setText(groupLabel);
        viewHolder.editMenu.setTag(groupPosition);
        viewHolder.editMenu.setOnClickListener(this);
        viewHolder.replaceMenu.setTag(groupPosition);
        viewHolder.replaceMenu.setOnClickListener(this);
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public ArrayList<String> getFileList() {
        return filePathList;
    }

    public String getKeyword() {
        return this.keyword;
    }

    // Add the search result so that we can unfold the group
    void addSearchResult(String filePath,
                         List<MatchedLineItem> matchList) {
        synchronized (matchedContents) {
            matchedContents.put(filePath, matchList);
        }
    }

    public boolean groupChildExist(int groupPosition) {
        String filePath = this.filePathList.get(groupPosition);
        if (filePath != null) {
            synchronized (matchedContents) {
                return this.matchedContents.containsKey(filePath);
            }
        }
        return false;
    }

    public void removeSearchResult(int groupPosition) {
        String filePath = this.filePathList.get(groupPosition);
        if (filePath != null) {
            synchronized (matchedContents) {
                this.matchedContents.remove(filePath);
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.menu_edit) {
            Integer index = (Integer) v.getTag();

            if (index < this.filePathList.size()) {
                if (!this.editClicked[index]) {
                    this.editClicked[index] = true;
                    this.notifyDataSetChanged();
                }

                Intent intent;

                // Allow multiple files editing
                if (filePathList.size() <= 100) {
                    ApkInfoActivity activity = activityRef.get();
                    intent = TextEditor.getEditorIntent(activityRef.get(), filePathList, index, activity.getApkPath());
                } else {
                    String filePath = filePathList.get(index);
                    ApkInfoActivity activity = activityRef.get();
                    intent = TextEditor.getEditorIntent(activity, filePath, activity.getApkPath());
                }

                ActivityUtil.attachParam(intent, "searchString", keyword);

                activityRef.get().startActivityForResult(intent, 0);
            }
        } else if (id == R.id.menu_replace) {
            Integer index = (Integer) v.getTag();

            if (!this.replaceClicked[index]) {
                this.replaceClicked[index] = true;
                this.notifyDataSetChanged();
            }

            if (index < this.filePathList.size()) {
                if (this.notShowReplaceDlg) {
                    replace(index);
                } else {
                    showReplaceDialog(index);
                }
            }
        }
    }

    private void showReplaceDialog(final int index) {
        AlertDialog.Builder inputDlg = new AlertDialog.Builder(
                activityRef.get());
        inputDlg.setTitle(R.string.replace);
        String msg = String.format(
                activityRef.get().getString(R.string.str_replace_with),
                keyword);
        inputDlg.setMessage(msg);

        // Set an EditText view to get user input
        final AutoCompleteAdapter adapter = new AutoCompleteAdapter(
                activityRef.get().getApplicationContext(), "search_replace_with");

        LinearLayout layout = new LinearLayout(activityRef.get());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));

        final EditTextWithTip input = new EditTextWithTip(
                activityRef.get().getApplicationContext());
        input.setAdapter(adapter);
        layout.addView(input, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));

        final CheckBox notshowCb = new CheckBox(activityRef.get());
        notshowCb.setText(R.string.label_replace_with_same_setting);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 8, 0, 20);
        notshowCb.setLayoutParams(params);
        layout.addView(notshowCb);

        inputDlg.setView(layout);

        inputDlg.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        MatchedTextListAdapter.this.strReplace = input.getText()
                                .toString();
                        // Not show the dialog again next time
                        if (notshowCb.isChecked()) {
                            MatchedTextListAdapter.this.notShowReplaceDlg = true;
                        }
                        // Record the input history
                        if (!"".equals(strReplace.trim())) {
                            adapter.addInputHistory(strReplace);
                        }
                        replace(index);
                    }
                });

        inputDlg.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        // Canceled.
                    }
                });

        inputDlg.show();
    }

    // Replace the matched string with user input string
    private void replace(int index) {
        String filePath = filePathList.get(index);
        try {
            replaceWith(filePath, strReplace);
            // Mark the modification
            activityRef.get().dealWithModifiedFile(filePath, null);
            // Collapse the group
            listviewRef.get().collapseGroup(index);
            // Remove the child, so next time expand can make it
            // search again
            removeSearchResult(index);
            // Show messages
            String msg = String.format(activityRef.get().getString(R.string.str_replaced), keyword);
            Toast.makeText(activityRef.get(), msg, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(activityRef.get(), e.getMessage(), Toast.LENGTH_LONG)
                    .show();
        }
    }

    public void replaceWith(String filePath, String strReplace)
            throws IOException {
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(filePath, "rw");
            long size = file.length();
            byte[] buffer = new byte[(int) size];
            int offset = 0;
            int read;
            while ((read = file.read(buffer, offset,
                    buffer.length - offset)) > 0) {
                offset += read;
            }
            String content = new String(buffer, "UTF-8");
            content = content.replace(this.keyword, strReplace);

            // write
            file.setLength(0);
            file.write(content.getBytes());
        } finally {
            closeQuietly(file);
        }
    }

    private void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void removeItem(int position) {
        if (position < filePathList.size()) {
            String path = filePathList.remove(position);
            this.matchedContents.remove(path);
            this.notifyDataSetChanged();
        }
    }

    private static class ViewHolder {
        TextView matchedLine;
    }

    private static class GroupViewHolder {
        TextView groupLabel;
        View replaceMenu;
        View editMenu;
        ImageView replaceImage;
        ImageView editImage;
    }
}
