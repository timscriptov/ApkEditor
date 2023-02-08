package com.gmail.heagoo.apkeditor.util;


import android.content.Context;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.patch.ResourceItem;
import com.gmail.heagoo.common.RandomUtil;
import com.gmail.heagoo.common.TextFileReader;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// /data/user/0/com.gmail.heagoo.apkeditor.pro/files/decoded/res/anim/aRA.xml: error: File is case-insensitive equivalent to: /data/user/0/com.gmail.heagoo.apkeditor.pro/files/decoded/res/anim/aRa.xml
public class FixInvalidEquivalent extends FixInvalid {
    private final static String err_eqivalent = "^(.+): error: File is case-insensitive equivalent to: (.+)";

    private Map<String, String> equivalents = new HashMap<>();

    // Record all resource names in lower case grouped by type
    private Map<String, Set<String>> resourceNames = new HashMap<>();

    // Record all the renamed resource names grouped by type
    private Map<String, List<ResourceRename>> renamedResources = new HashMap<>();

    public FixInvalidEquivalent(String decodeRootPath, String message) {
        super(decodeRootPath, message);
        try {
            parseErrorMessage();
        } catch (IOException e) {
        }
    }

    private void parseErrorMessage() throws IOException {
        BufferedReader br = null;
        try {
            Pattern pattern = Pattern.compile(err_eqivalent);

            br = new BufferedReader(new StringReader(this.errMessage));
            String line = br.readLine();
            while (line != null) {
                Matcher m = pattern.matcher(line);
                if (m.find()) {
                    String name1 = m.group(1);
                    String name2 = m.group(2);
                    equivalents.put(name1, name2);
                }
                line = br.readLine();
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    @Override
    public boolean isErrorFixable() {
        return !equivalents.isEmpty();
    }

    @Override
    public void fixErrors() {
        // public.xml
        modifyPublicXml();

        // Rename equivalent file names
        for (Map.Entry<String, List<ResourceRename>> entry : renamedResources.entrySet()) {
            String type = entry.getKey();
            modifyDeclaration(type, entry.getValue());
        }

        // Modify other xml files for reference
        if (!renamedResources.isEmpty()) {
            List<ResourceRename> allRenames = new ArrayList<>();
            for (List<ResourceRename> rr : renamedResources.values()) {
                allRenames.addAll(rr);
            }
            modifyReferences(allRenames);
        }
    }

    private void modifyPublicXml() {
        String path = decodeRootPath + "res/values/public.xml";
        try {
            TextFileReader reader = new TextFileReader(path);
            List<String> lines = reader.getLines();

            for (int i = 0; i < lines.size(); ++i) {
                String line = lines.get(i);
                ResourceItem item = ResourceItem.parseFrom(line);
                if (item == null) {
                    continue;
                }
                String resType = item.type;
                String resName = item.name;
                String lcName = resName.toLowerCase();
                // When cannot record it, means already exist
                if (!recordResourceName(resType, lcName)) {
                    String newName = resName + "_" + RandomUtil.getRandomString(6);
                    recordRenameRecord(resType, resName, newName);

                    // Change the item
                    item.name = newName;
                    lines.set(i, item.toString());
                }
            }

            // Write back
            BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(path));
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                sb.append(line);
                sb.append("\n");
            }
            fos.write(sb.toString().getBytes());
            fos.close();

            modifiedFileSet.add(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Add a rename record
    private void recordRenameRecord(String resType, String resName, String newName) {
        List<ResourceRename> renameRec = renamedResources.get(resType);
        if (renameRec == null) {
            renameRec = new ArrayList<>();
            renamedResources.put(resType, renameRec);
        }

        ResourceRename rec = new ResourceRename(resType, resName, newName);
        renameRec.add(rec);
    }

    // Add a resource name record
    private boolean recordResourceName(String resType, String lcName) {
        Set<String> nameSet = resourceNames.get(resType);
        if (nameSet == null) {
            nameSet = new HashSet<>();
            resourceNames.put(resType, nameSet);
        }
        if (!nameSet.contains(lcName)) {
            nameSet.add(lcName);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean succeeded() {
        return renamedFileNum > 0 || !modifiedFileSet.isEmpty();
    }

    @Override
    public String getMofifyMessage(Context ctx) {
        // Show toast and then rebuild
        StringBuilder sb = new StringBuilder();
        if (renamedFileNum > 0) {
            sb.append(
                    String.format(ctx.getString(R.string.str_num_renamed_file),
                            renamedFileNum));
            sb.append("\n");
        }
        if (!modifiedFileSet.isEmpty()) {
            sb.append(
                    String.format(ctx.getString(R.string.str_num_modified_file),
                            modifiedFileSet.size()));
            sb.append("\n");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    @Override
    public Map<String, Map<String, String>> getAxmlModifications() {
        return null;
    }
}
