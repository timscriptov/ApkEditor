package com.gmail.heagoo.apkeditor;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.Display;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// For the expandable list view
class ModificationCategory {
    public String name;
    // For string and manifest: if all 3 numbers are -1, means modified
    public int addedFiles;
    public int removedFiles;
    public int modifiedFiles;

    public ModificationCategory(String _name, int add, int removed,
                                int modified) {
        this.name = _name;
        this.addedFiles = add;
        this.removedFiles = removed;
        this.modifiedFiles = modified;
    }

    public int getItems() {
        // Special cases
        if (addedFiles < 0 && removedFiles < 0 && modifiedFiles < 0) {
            return 1;
        }
        if (addedFiles == 0 && removedFiles == 0 && modifiedFiles == 0) {
            return 1;
        }

        int items = 0;
        if (this.addedFiles > 0) {
            items += 1;
        }
        if (this.removedFiles > 0) {
            items += 1;
        }
        if (this.modifiedFiles > 0) {
            items += 1;
        }
        return items;
    }

    public String getItemDescription(Context ctx, int index) {
        // Special cases
        if (addedFiles < 0 && removedFiles < 0 && modifiedFiles < 0) {
            return ctx.getString(R.string.str_modified);
        }
        if (addedFiles == 0 && removedFiles == 0 && modifiedFiles == 0) {
            return ctx.getString(R.string.str_not_modified);
        }

        int curIndex = -1;
        if (this.addedFiles > 0) {
            curIndex += 1;
        }
        if (curIndex == index) {
            String fmt = ctx.getString(R.string.str_num_added_file);
            return String.format(fmt, addedFiles);
        }
        if (this.removedFiles > 0) {
            curIndex += 1;
        }
        if (curIndex == index) {
            String fmt = ctx.getString(R.string.str_num_removed_file);
            return String.format(fmt, removedFiles);
        }
        if (this.modifiedFiles > 0) {
            curIndex += 1;
        }
        if (curIndex == index) {
            String fmt = ctx.getString(R.string.str_num_modified_file);
            return String.format(fmt, modifiedFiles);
        }
        return null;
    }
}

class ModificationAdapter extends BaseExpandableListAdapter {

    public Context ctx;
    public List<ModificationCategory> modificationList;

    public ModificationAdapter(Context ctx,
                               List<ModificationCategory> modList) {
        this.ctx = ctx;
        this.modificationList = modList;
    }

    @Override
    public int getGroupCount() {
        return modificationList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return modificationList.get(groupPosition).getItems();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return modificationList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return modificationList.get(groupPosition).getItemDescription(ctx,
                childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return (groupPosition << 16) | childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String text = this.modificationList.get(groupPosition).name;

        if (convertView == null) {
            convertView = new TextView(ctx);
        }

        int left = Display.dip2px(ctx, 32);
        ((TextView) convertView).setPadding(left, left / 8, 0, 0);
        ((TextView) convertView).setTypeface(null, Typeface.BOLD);
        ((TextView) convertView).setText(text);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        String text = modificationList.get(groupPosition)
                .getItemDescription(ctx, childPosition);

        if (convertView == null) {
            convertView = new TextView(ctx);
        }

        int left = Display.dip2px(ctx, 48);
        ((TextView) convertView).setPadding(left, 0, 0, 0);
        ((TextView) convertView).setText(text);

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }
}

public class RebuildConfirmDialog {

    private Context ctx;

    // Checkbox in UI
    private CheckBox dexCb;
    private CheckBox resCb;
    private CheckBox signCb;

    private boolean stringModified;
    private boolean manifestModified;
    private boolean resFileModified;

    private int resAddedFiles = 0;
    private int resRemovedFiles = 0;
    private int resModifiedFiles = 0;

    private int otherAddedFiles = 0;
    private int otherRemovedFiles = 0;
    private int otherModifiedFiles = 0;

    private Map<String, Integer> smaliAddedFiles;
    private Map<String, Integer> smaliRemovedFiles;
    private Map<String, Integer> smaliModifiedFiles;
    private Set<String> smaliFolders;

    public RebuildConfirmDialog(Context ctx, boolean stringModified,
                                boolean manifestModified, Map<String, String> added,
                                Map<String, String> replaced, Set<String> deleted) {
        this.ctx = ctx;
        this.stringModified = stringModified;
        this.manifestModified = manifestModified;
        initData(added, replaced, deleted);
    }

    private void initData(Map<String, String> added,
                          Map<String, String> replaced, Set<String> deleted) {
        this.resFileModified = false;

        this.smaliFolders = new HashSet<String>();
        this.smaliAddedFiles = new HashMap<String, Integer>();
        this.smaliRemovedFiles = new HashMap<String, Integer>();
        this.smaliModifiedFiles = new HashMap<String, Integer>();

        // Enumerate added files
        String smaliFoder;
        for (Map.Entry<String, String> entry : added.entrySet()) {
            String entryName = entry.getKey();
            // Resource file
            if (entryName.startsWith("res/")) {
                if (!resFileModified
                        && !ApkInfoActivity.isCommonImage(entryName)) {
                    resFileModified = true;
                }
                resAddedFiles += 1;
            }
            // Smali or other files
            else if ((smaliFoder = ApkInfoActivity.dealWithSmaliFile(entryName,
                    smaliFolders)) != null) {
                Integer v = smaliAddedFiles.get(smaliFoder);
                if (v == null) {
                    smaliAddedFiles.put(smaliFoder, 1);
                } else {
                    smaliAddedFiles.put(smaliFoder, v + 1);
                }
            } else {
                otherAddedFiles += 1;
            }
        }

        // Enumerate modified files
        for (Map.Entry<String, String> entry : replaced.entrySet()) {
            String entryName = entry.getKey();
            // Resource file
            if (entryName.startsWith("res/")) {
                if (!resFileModified
                        && !ApkInfoActivity.isCommonImage(entryName)) {
                    resFileModified = true;
                }
                resModifiedFiles += 1;
            }
            // Smali or other files
            else if ((smaliFoder = ApkInfoActivity.dealWithSmaliFile(entryName,
                    smaliFolders)) != null) {
                Integer v = smaliModifiedFiles.get(smaliFoder);
                if (v == null) {
                    smaliModifiedFiles.put(smaliFoder, 1);
                } else {
                    smaliModifiedFiles.put(smaliFoder, v + 1);
                }
            } else {
                otherModifiedFiles += 1;
            }
        }

        // Enumerate deleted files
        for (String entryName : deleted) {
            // Resource file
            if (entryName.startsWith("res/")) {
                if (!resFileModified
                        && !ApkInfoActivity.isCommonImage(entryName)) {
                    resFileModified = true;
                }
                resRemovedFiles += 1;
            }
            // Smali or other files
            else if ((smaliFoder = ApkInfoActivity.dealWithSmaliFile(entryName,
                    smaliFolders)) != null) {
                Integer v = smaliRemovedFiles.get(smaliFoder);
                if (v == null) {
                    smaliRemovedFiles.put(smaliFoder, 1);
                } else {
                    smaliRemovedFiles.put(smaliFoder, v + 1);
                }
            } else {
                otherRemovedFiles += 1;
            }
        }
    }

    private List<ModificationCategory> getModifications() {
        List<ModificationCategory> modList = new ArrayList<ModificationCategory>();
        // String
        if (this.stringModified) {
            modList.add(new ModificationCategory(ctx.getString(R.string.string),
                    -1, -1, -1));
        } else {
            modList.add(new ModificationCategory(ctx.getString(R.string.string),
                    0, 0, 0));
        }

        // resource
        modList.add(new ModificationCategory(ctx.getString(R.string.resource),
                resAddedFiles, resRemovedFiles, resModifiedFiles));

        if (this.manifestModified) { // Manifest
            modList.add(new ModificationCategory(
                    ctx.getString(R.string.manifest), -1, -1, -1));
        } else {
            modList.add(new ModificationCategory(
                    ctx.getString(R.string.manifest), 0, 0, 0));
        }

        // smali
        for (String smaliFolder : smaliFolders) {
            Integer add = smaliAddedFiles.get(smaliFolder);
            Integer remove = smaliRemovedFiles.get(smaliFolder);
            Integer modify = smaliModifiedFiles.get(smaliFolder);
            modList.add(new ModificationCategory(smaliFolder,
                    (add == null ? 0 : add), (remove == null ? 0 : remove),
                    (modify == null ? 0 : modify)));
        }

        // other files
        if (otherAddedFiles > 0 || otherRemovedFiles > 0
                || otherModifiedFiles > 0) {
            modList.add(new ModificationCategory(ctx.getString(R.string.others),
                    otherAddedFiles, otherRemovedFiles, otherModifiedFiles));
        }

        return modList;
    }

    @SuppressLint("InflateParams")
    public void show() {
        AlertDialog.Builder confirmDlg = new AlertDialog.Builder(ctx);
        confirmDlg.setTitle(R.string.rebuild_the_apk);

        // Initialize the expandable list view
        View layout = LayoutInflater.from(ctx)
                .inflate(R.layout.dlg_rebuild_confirm, null);
        ExpandableListView list = (ExpandableListView) layout
                .findViewById(R.id.modificationList);
        List<ModificationCategory> modList = getModifications();
        list.setAdapter(new ModificationAdapter(ctx, modList));
        for (int i = 0; i < modList.size(); ++i) {
            list.expandGroup(i);
        }
        this.dexCb = (CheckBox) layout.findViewById(R.id.cb_rebuild_dex);
        this.resCb = (CheckBox) layout.findViewById(R.id.cb_rebuild_res);
        this.signCb = (CheckBox) layout.findViewById(R.id.cb_resign);
        this.dexCb.setChecked(!this.smaliFolders.isEmpty());
        this.resCb.setChecked(this.stringModified || this.manifestModified
                || this.resFileModified);
        this.dexCb.setEnabled(false);
        this.resCb.setEnabled(false);
        this.signCb.setChecked(true); // default is checked

        confirmDlg.setView(layout);
        confirmDlg.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        boolean bSign = signCb.isChecked();
                        // Ugly hardcode
                        ((ApkInfoActivity) ctx).build(bSign);
                    }
                });

        confirmDlg.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        // Canceled.
                    }
                });

        confirmDlg.show();
    }
}
