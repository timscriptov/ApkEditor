package com.gmail.heagoo.apkeditor.ce.e;

import android.util.SparseArray;
import android.util.TypedValue;

import com.gmail.heagoo.apkeditor.ce.ManifestInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

// Designed to add installLocation Attribute to AndroidManifest binary file
public class ManifestEditorNew {

    private MyInputStream is;
    private MyFileOutput out;
    private boolean bReviseDex; // User will revise the DEX or not

    // Postfix for permission name, provider authority name, etc
    private String addedPostfix;

    // Header
    private int headTag;
    private int fileSize;

    // String pool
    private ResStringChunk strChunk;
    // Resource Attribute ID table
    private ResAttrIdChunk attrIdChunk;
    // Body
    private AxmlBodyChunk body;

    // for modification
    private ManifestInfo oldInfo;
    private ManifestInfo newInfo;

    // Refactored class names
    private Map<String, String> refactoredNames = new HashMap<>();

    public ManifestEditorNew(InputStream input, String outputFile, boolean bReviseDex) throws IOException {
        this.bReviseDex = bReviseDex;

        // Prepare input and output
        is = new MyInputStream(input);
        File f = new File(outputFile);
        if (f.exists()) {
            f.delete();
        }
        RandomAccessFile outFile = new RandomAccessFile(outputFile, "rw");
        outFile.setLength(0);
        out = new MyFileOutput(outFile);

        // Parse the manifest into internal presentation
        try {
            parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static int getInt(byte[] buf, int offset) {
        return ((int) buf[offset] & 0xff)
                | (((int) buf[offset + 1] & 0xff) << 8)
                | (((int) buf[offset + 2] & 0xff) << 16)
                | (((int) buf[offset + 3] & 0xff) << 24);
    }

    protected static int getShort(byte[] buf, int offset) {
        return ((int) buf[offset] & 0xff)
                | (((int) buf[offset + 1] & 0xff) << 8);
    }

    protected static void setInt(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value & 0xff);
        buf[offset + 1] = (byte) ((value >> 8) & 0xff);
        buf[offset + 2] = (byte) ((value >> 16) & 0xff);
        buf[offset + 3] = (byte) ((value >> 24) & 0xff);
    }

    protected static void setShort(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value & 0xff);
        buf[offset + 1] = (byte) ((value >> 8) & 0xff);
    }

    protected static void setByte(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value & 0xff);
    }

    protected static void log(String format, Object... arguments) {
        // String msg = String.format(format, arguments);
        // Log.d("DEBUG", msg);
        // System.out.println(msg);
    }

    // Called when dex is refactored
    private static String getNewComponentName(String oldPkgName,
                                              String newPkgName,
                                              String nameInManifest) {
        if (nameInManifest.startsWith(oldPkgName + ".")) {
            return newPkgName + nameInManifest.substring(oldPkgName.length());
        }
        // Keep unchanged
        return nameInManifest;
    }

    private static String getFullComponentName(String packageName, String name) {
        String newName = name;
        if (name.startsWith(".")) {
            newName = packageName + name;
        } else if (!name.contains(".")) {
            newName = packageName + "." + name;
        }
        return newName;
    }

    public void parse() throws Exception {
        // Header
        this.headTag = is.readInt();
        this.fileSize = is.readInt();

        // String table
        this.strChunk = new ResStringChunk();
        strChunk.parse(is);

        // Resource Attribute ID table
        this.attrIdChunk = new ResAttrIdChunk();
        attrIdChunk.parse(is);

        // Body
        body = new AxmlBodyChunk(strChunk, attrIdChunk);
        body.parse(is);
    }

    public void modify(ManifestInfo oldVal, ManifestInfo newVal) throws Exception {
        this.oldInfo = oldVal;
        this.newInfo = newVal;

        boolean bAddInstallLocation = (oldInfo.installLocation == -1 &&
                newInfo.installLocation != -1);
        boolean bModifyVersionName = (oldInfo.verNameIdx != -1 &&
                newInfo.versionName != null &&
                !newInfo.versionName.equals(oldInfo.versionName));
        boolean bModifyPkgName = (newInfo.packageName != null &&
                !newInfo.packageName.equals(oldInfo.packageName));
        boolean bModifyAppNameStr = (oldInfo.appNameIdx >= 0 &&
                newInfo.appName != null && !newInfo.appName.equals(oldInfo.appName));

        // String
        modifyString(bModifyPkgName, bModifyVersionName, bModifyAppNameStr, bReviseDex);
        addString(bModifyPkgName, bAddInstallLocation);

        // Attribute id
        if (bAddInstallLocation) {
            attrIdChunk.addAttributeId(0x010102b7, 0);
        }

        // Axml body
        body.updateStringIndex();
        if (bAddInstallLocation) { // add installLocation
            body.addAttribute("manifest", 0, -1, 0x10000008, newInfo.installLocation);
        } else if (newInfo.installLocation != -1) { // modify installLocation
            body.modifyAttribute("manifest", "installLocation",
                    new IModifyTagAttribute() {
                        @Override
                        public void modify(Tag.TagAttr attr) {
                            setInt(attr.getRawData(), attr.getOffset() + 16,
                                    newInfo.installLocation);
                        }
                    });
        }
        if (bModifyPkgName && !oldInfo.providerInfoList.isEmpty()) { // authorities
            body.modifyAttribute("manifest/application/provider", "authorities",
                    // Revise the authorities
                    new IModifyTagAttribute() {
                        @Override
                        public void modify(Tag.TagAttr attr) {
                            int valueType = attr.getValueType();
                            if (valueType == TypedValue.TYPE_STRING) {
                                int[] mapping = strChunk.getStringIndexMapping();
                                int origin = attr.getValueData();
                                if (origin >= 0 && origin < mapping.length) {
                                    int newVal = mapping[origin] + 1;
                                    setInt(attr.getRawData(), attr.getOffset() + 8, newVal);
                                    setInt(attr.getRawData(), attr.getOffset() + 16, newVal);
                                }
                            }
                        }
                    });
        }
        if (newInfo.versionCode != oldInfo.versionCode) { // modify version code
            body.modifyAttribute("manifest", "versionCode",
                    new IModifyTagAttribute() {
                        @Override
                        public void modify(Tag.TagAttr attr) {
                            setInt(attr.getRawData(), attr.getOffset() + 16,
                                    newInfo.versionCode);
                        }
                    });
        }
        // SDK versions
        if (oldInfo.minSdkVersion != -1 &&
                newInfo.minSdkVersion != oldInfo.minSdkVersion) {
            body.modifyAttribute("manifest/uses-sdk", "minSdkVersion",
                    new IModifyTagAttribute() {
                        @Override
                        public void modify(Tag.TagAttr attr) {
                            setInt(attr.getRawData(), attr.getOffset() + 16,
                                    newInfo.minSdkVersion);
                        }
                    });
        }
        if (oldInfo.targetSdkVersion != -1 &&
                newInfo.targetSdkVersion != oldInfo.targetSdkVersion) {
            body.modifyAttribute("manifest/uses-sdk", "targetSdkVersion",
                    new IModifyTagAttribute() {
                        @Override
                        public void modify(Tag.TagAttr attr) {
                            setInt(attr.getRawData(), attr.getOffset() + 16,
                                    newInfo.targetSdkVersion);
                        }
                    });
        }
        if (oldInfo.maxSdkVersion != -1 &&
                newInfo.maxSdkVersion != oldInfo.maxSdkVersion) {
            body.modifyAttribute("manifest/uses-sdk", "minSdkVersion",
                    new IModifyTagAttribute() {
                        @Override
                        public void modify(Tag.TagAttr attr) {
                            setInt(attr.getRawData(), attr.getOffset() + 16,
                                    newInfo.maxSdkVersion);
                        }
                    });
        }
    }

    private void modifyString(boolean bModifyPkgName, boolean bModifyVersionName,
                              boolean bModifyAppNameStr, boolean bReviseDex) {
        // Record all the revised indice
        Set<Integer> revisedIndice = new HashSet<>();

        // Generate addedPostfix
        // example: permission.wec.READ -> permission.wec.READbw
        if (bModifyPkgName) {
            Random r = new Random(System.currentTimeMillis());
            char rand1 = (char) ('a' + r.nextInt(26));
            char rand2 = (char) ('a' + r.nextInt(26));
            this.addedPostfix = "" + rand1 + rand2;
        }

        // When DEX also revised, modify all the string started with the package name
        if (bModifyPkgName && this.bReviseDex) {
            for (int i = 0; i < strChunk.getStringCount(); ++i) {
                String oldStr = strChunk.getStringByIndex(i);
                if (oldStr.startsWith(oldInfo.packageName)) {
                    strChunk.modifyString(i, newInfo.packageName +
                            oldStr.substring(oldInfo.packageName.length()));
                    revisedIndice.add(i);
                }
            }
        }

        // When modifying package name, also modify self defined permissions
        if (bModifyPkgName) {
            for (Map.Entry<Integer, String> entry : oldInfo.permissionIdx2Name.entrySet()) {
                int index = entry.getKey();
                if (!revisedIndice.contains(index)) {
                    String oldVal = entry.getValue();
                    String newVal = oldVal + addedPostfix;
                    strChunk.modifyString(index, newVal);
                }
            }
        }

        // Modify version name
        int verNameIdx = oldInfo.verNameIdx;
        if (bModifyVersionName && verNameIdx != -1) {
            strChunk.modifyString(verNameIdx, newInfo.versionName);
        }

        // Modify package name
        int pkgNameIdx = oldInfo.pkgNameIdx;
        if (bModifyPkgName && pkgNameIdx != -1 && !revisedIndice.contains(pkgNameIdx)) {
            strChunk.modifyString(pkgNameIdx, newInfo.packageName);
        }

        // When DEX revised, change the component name according to the new package name
        if (bModifyPkgName) {
            // Also modify component names (when needed)
            if (oldInfo.compNameIdxs != null && !oldInfo.compNameIdxs.isEmpty()) {
                Collections.sort(oldInfo.compNameIdxs);
                int lastIndex = -1;
                for (int idx : oldInfo.compNameIdxs) {
                    // Not the repeated index
                    if (idx != lastIndex) {
                        if (revisedIndice.contains(idx)) {
                            continue;
                        }
                        String name = strChunk.getStringByIndex(idx);
                        String newName = (bReviseDex ?
                                getNewComponentName(oldInfo.packageName, newInfo.packageName, name) :
                                getFullComponentName(oldInfo.packageName, name));
                        if (!newName.equals(name)) {
                            strChunk.modifyString(idx, newName);
                        }
                    }
                    lastIndex = idx;
                }
            }
            // Also modify targetActitity for activity-alias
            if (!oldInfo.targetActivityIdxs.isEmpty()) {
                for (int idx : oldInfo.targetActivityIdxs.values()) {
                    if (revisedIndice.contains(idx)) {
                        continue;
                    }
                    String name = strChunk.getStringByIndex(idx);
                    String newName = (bReviseDex ?
                            getNewComponentName(oldInfo.packageName, newInfo.packageName, name) :
                            getFullComponentName(oldInfo.packageName, name));
                    if (!newName.equals(name)) {
                        strChunk.modifyString(idx, newName);
                    }
                }
            }
        }

        // Directly modify app name
        if (bModifyAppNameStr) {
            strChunk.modifyString(oldInfo.appNameIdx, newInfo.appName);
        }
    }

    // Refactor the service/receiver/provider name
    private String refactorProviderName(String name) {
        Random r = new Random();
        int len = name.length();
        StringBuffer sb = new StringBuffer(len + 2);
        if (len > 2 && name.charAt(len - 2) != '.') {
            sb.append(name.substring(0, len - 2));
            sb.append((char) (name.charAt(len - 2) + r.nextInt(5)));
            sb.append((char) (name.charAt(len - 1) + r.nextInt(5)));
        } else {
            sb.append(name.substring(0, len - 1));
            sb.append((char) (name.charAt(len - 1) + r.nextInt(5)));
        }
        String newName = sb.toString();

        // Record the refactored names
        this.refactoredNames.put(name, newName);

        return newName;
    }

    private void addString(boolean bModifyPkgName, boolean bAddInstallLocation) {
        // Add the new authority string (as it is not changed in modifyString)
        // Add the tail first (make sure the index not changed)
        if (bModifyPkgName) {
            List<Integer> authorityIdxList = new ArrayList<>();
            SparseArray<String> idx2Str = new SparseArray<>();
            for (ManifestInfo.ProviderInfo info : oldInfo.providerInfoList) {
                if (!authorityIdxList.contains(info.authorityIdx)) {
                    authorityIdxList.add(info.authorityIdx);
                    idx2Str.put(info.authorityIdx, info.authority);
                }
            }
            Collections.sort(authorityIdxList);
            for (int i = authorityIdxList.size() - 1; i >= 0; --i) {
                int idx = authorityIdxList.get(i);
                String originVal = idx2Str.get(idx);
                // Replace old package name with new package name for authorities
                if (originVal.startsWith(oldInfo.packageName)) {
                    strChunk.addString(newInfo.packageName +
                            originVal.substring(oldInfo.packageName.length()), idx + 1);
                } else {
                    strChunk.addString(originVal + addedPostfix, idx + 1);
                }
            }
        }

        if (bAddInstallLocation) {
            strChunk.addString("installLocation", 0);
        }
    }

    public void save() throws IOException {
        try {
            out.writeInt(headTag);
            out.writeInt(fileSize);

            // String
            int oldSize = strChunk.chunkSize;
            strChunk.dump(out);

            // Attr
            attrIdChunk.dump(out);

            // Body
            body.dump(out);

            // Revise the file size
            log("Wrote: 0x%x", out.getWriteLength());
            out.writeInt(4, out.getWriteLength());
        } finally {
            closeQuietly(out);
        }
    }

    private void closeQuietly(MyFileOutput out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
            }
        }
    }

    public Map<String, String> getRefactoredNames() {
        return this.refactoredNames;
    }
}
