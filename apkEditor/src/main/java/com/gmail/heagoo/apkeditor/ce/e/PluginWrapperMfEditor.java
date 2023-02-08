package com.gmail.heagoo.apkeditor.ce.e;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Designed to add installLocation Attribute to AndroidManifest binary file
public class PluginWrapperMfEditor {

    public static int endDocTag = 0x00100101;
    public static int startTag = 0x00100102;
    public static int endTag = 0x00100103;
    public static int namespaceTag = 0x00100100;
    public static int cdataTag = 0x00100104;

    private MyInputStream is;
    private MyFileOutput out;

    // Hard coded
    private int permissionStartIdx = 29;
    private List<String> permissions;

    private ResStringChunk strChunk;
    private int fileSize;
    private int versionCode;
    private int installLocation;

    private byte[] rawChunkData;
    private int oldStrChunkSize;

    public PluginWrapperMfEditor(InputStream input, String outputFile)
            throws IOException {
        is = new MyInputStream(input);
        File f = new File(outputFile);
        if (f.exists()) {
            f.delete();
        }
        RandomAccessFile outFile = new RandomAccessFile(outputFile, "rw");
        outFile.setLength(0);
        out = new MyFileOutput(outFile);

        // Header
        int headTag = is.readInt();
        this.fileSize = is.readInt();
        out.writeInt(headTag);
        out.writeInt(fileSize);

        // String table
        this.strChunk = new ResStringChunk();
        strChunk.parse(is);
        this.oldStrChunkSize = strChunk.chunkSize;
        log("ChunkSize of Resource String: %d", strChunk.chunkSize);
        log("String Count: %d", strChunk.stringCount);
        log("String Offset: 0x%x", strChunk.stringOffset);
    }

    public static void main(String[] args) throws Exception {

        String inputFile = "D:\\temp\\AndroidManifest.xml";
        String outputFile = "D:\\temp\\AndroidManifest.new.xml";

        PluginWrapperMfEditor editor = new PluginWrapperMfEditor(
                new FileInputStream(inputFile), outputFile);

        // Replace strings
        editor.replaceString("com.example.droidpluginwrapper",
                "com.example.droidpluginwrappez");
        editor.replaceString("DroidPluginWrapper", "APP Name 3");
        editor.replaceString("1.x.y", "newversion");
        for (int i = 0; i < 9; ++i) {
            char c = (char) ('0' + i);
            editor.replaceString("com.morgoo.droidplugin_stub_P0" + c,
                    "com.morgoo.droidplugin_xxxx_P0" + c);
        }

        // Version code and install location
        editor.setVersionCode(45);
        editor.setInstallLocation(2);

        // Permissions
        List<String> permissions = new ArrayList<String>();
        permissions.add("android.permission.READ");
        permissions.add("android.permission.XYZM");
        permissions.add("android.permission.XZM");
        editor.addPermissions(permissions);

        editor.saveModification();
    }

    public void addPermissions(List<String> permissions) {
        this.permissions = permissions;

        // Get the permission start index
        permissionStartIdx = strChunk
                .getStringIndex("aaaaaa.permission.READ_EXTERNAL_STORAGE");

        Collections.sort(permissions);
        strChunk.modifyString(permissionStartIdx, permissions.get(0));

        for (int i = permissions.size() - 1; i > 0; --i) {
            strChunk.addString(permissions.get(i), permissionStartIdx + 1);
        }
    }

    public void setInstallLocation(int location) {
        this.installLocation = location;
    }

    public void setVersionCode(int verCode) {
        this.versionCode = verCode;
    }

    public void replaceString(String oldStr, String newStr) {
        for (int idx = 0; idx < strChunk.stringCount; ++idx) {
            String str = strChunk.getStringByIndex(idx);
            if (str.equals(oldStr)) {
                strChunk.modifyString(idx, newStr);
                return;
            }
        }
    }

    private void log(String fmt, Object... args) {
        // System.out.println(String.format(fmt, args));
    }

    private int changeIndex(int strIndex, int offset) {
        int skip = 0;
        List<Integer> addedPos = strChunk.getAddedStrPositions();
        for (int i = addedPos.size() - 1; i >= 0; i--) {
            if (strIndex >= addedPos.get(i)) {
                skip += 1;
            }
        }
        if (skip > 0) {
            ManifestEditorNew.setInt(rawChunkData, offset, strIndex + skip);
        }
        return strIndex + skip;
    }

    public int parseNextTag(MyInputStream is) throws IOException {
        int chunkTag = is.readInt();
        int chunkSize = is.readInt();

        this.rawChunkData = new byte[chunkSize];
        ManifestEditorNew.setInt(rawChunkData, 0, chunkTag);
        ManifestEditorNew.setInt(rawChunkData, 4, chunkSize);
        if (chunkSize > 8) {
            is.readFully(rawChunkData, 8, chunkSize - 8);
        }

        if (chunkTag == startTag) { // XML START TAG
            // int lineNo = AddAttribute.getInt(rawChunkData, 2 * 4);
            // int tag3 = AddAttribute.getInt(rawChunkData, 3 * 4);
            int nameNsSi = ManifestEditorNew.getInt(rawChunkData, 4 * 4);
            nameNsSi = changeIndex(nameNsSi, 4 * 4);
            int nameSi = ManifestEditorNew.getInt(rawChunkData, 5 * 4);
            nameSi = changeIndex(nameSi, 5 * 4);
            // Expected to be 14001400
            // int tag6 = AddAttribute.getInt(rawChunkData, 6 * 4);
            // Number of Attributes to follow
            int attrNum = ManifestEditorNew.getInt(rawChunkData, 7 * 4);
            // Expected to be 00000000
            // int tag8 = AddAttribute.getInt(rawChunkData, 8 * 4);

            // manifest
            if (strChunk.getStringByIndex(nameSi).equals("manifest")) {
                manipulateManifest(attrNum);
            } else {
                // Change the string index if needed
                for (int ii = 0; ii < attrNum; ii++) {
                    int attrNsIdx = ManifestEditorNew.getInt(rawChunkData,
                            36 + ii * 20);
                    changeIndex(attrNsIdx, 36 + ii * 20);
                    int attrNameIdx = ManifestEditorNew.getInt(rawChunkData,
                            36 + ii * 20 + 4);
                    attrNameIdx = changeIndex(attrNameIdx, 36 + ii * 20 + 4);
                    int attrValIdx = ManifestEditorNew.getInt(rawChunkData,
                            36 + ii * 20 + 8);
                    attrValIdx = changeIndex(attrValIdx, 36 + ii * 20 + 8);
                    int type = ManifestEditorNew.getInt(rawChunkData,
                            36 + ii * 20 + 12) >> 16;
                    type = ((type & 0xff00) >> 8) | ((type & 0x00ff) << 8);
                    int attrValIdx2 = ManifestEditorNew.getInt(rawChunkData,
                            36 + ii * 20 + 16);
                    if (isTypeRefToStrTable(type)) {
                        attrValIdx2 = changeIndex(attrValIdx2,
                                36 + ii * 20 + 16);
                    }
                }
            }

        } else if (chunkTag == endTag) { // XML END TAG
            int nameNsSi = ManifestEditorNew.getInt(rawChunkData, 4 * 4);
            changeIndex(nameNsSi, 4 * 4);
            int nameSi = ManifestEditorNew.getInt(rawChunkData, 5 * 4);
            changeIndex(nameSi, 5 * 4);

        } else if (chunkTag == endDocTag) { // END OF XML DOC TAG
            int prefix = ManifestEditorNew.getInt(rawChunkData, 4 * 4);
            changeIndex(prefix, 4 * 4);
            int uri = ManifestEditorNew.getInt(rawChunkData, 5 * 4);
            changeIndex(uri, 5 * 4);

        } else if (chunkTag == namespaceTag) {
            int prefix = ManifestEditorNew.getInt(rawChunkData, 4 * 4);
            changeIndex(prefix, 4 * 4);
            int uri = ManifestEditorNew.getInt(rawChunkData, 5 * 4);
            changeIndex(uri, 5 * 4);

        } else if (chunkTag == cdataTag) {

        }

        return chunkTag;
    }

    private void manipulateManifest(int attrNum) {
        // 0th word: StringIndex of Attribute Name's Namespace, or FFFFFFFF
        // 1st word: StringIndex of Attribute Name
        // 2nd word: StringIndex of Attribute Value, or FFFFFFF if
        // ResourceId
        // used
        // 3rd word: Flags? (This is type)
        // 4th word: str ind of attr value again, or ResourceId of value
        for (int ii = 0; ii < attrNum; ii++) {
            int attrNsIdx = ManifestEditorNew.getInt(rawChunkData, 36 + ii * 20);
            changeIndex(attrNsIdx, 36 + ii * 20);
            int attrNameIdx = ManifestEditorNew.getInt(rawChunkData,
                    36 + ii * 20 + 4);
            attrNameIdx = changeIndex(attrNameIdx, 36 + ii * 20 + 4);
            int attrValIdx = ManifestEditorNew.getInt(rawChunkData,
                    36 + ii * 20 + 8);
            attrValIdx = changeIndex(attrValIdx, 36 + ii * 20 + 8);
            int type = ManifestEditorNew.getInt(rawChunkData,
                    36 + ii * 20 + 12) >> 16;
            type = ((type & 0xff00) >> 8) | ((type & 0x00ff) << 8);
            int attrValIdx2 = ManifestEditorNew.getInt(rawChunkData,
                    36 + ii * 20 + 16);
            if (isTypeRefToStrTable(type)) {
                attrValIdx2 = changeIndex(attrValIdx2, 36 + ii * 20 + 16);
            }

            // To modify it
            String name = strChunk.getStringByIndex(attrNameIdx);
            if ("versionCode".equals(name)) {
                if (attrValIdx2 != -1) { // This value is the version code
                    ManifestEditorNew.setInt(rawChunkData, 36 + ii * 20 + 16,
                            this.versionCode);
                } else if (attrValIdx != -1) {
                    ManifestEditorNew.setInt(rawChunkData, 36 + ii * 20 + 8,
                            this.versionCode);
                }
            } else if ("installLocation".equals(name)) {
                if (attrValIdx2 != -1) { // This value is the location
                    ManifestEditorNew.setInt(rawChunkData, 36 + ii * 20 + 16,
                            this.installLocation);
                } else if (attrValIdx != -1) {
                    ManifestEditorNew.setInt(rawChunkData, 36 + ii * 20 + 8,
                            this.installLocation);
                }
            }
        }
    }

    private boolean isTypeRefToStrTable(int type) {
        return type == 3;
    }

    public void saveModification() throws Exception {

        // Resource Attribute ID table
        ResAttrIdChunk attrIdChunk = new ResAttrIdChunk();
        attrIdChunk.parse(is);

        strChunk.dump(out);
        fileSize += strChunk.chunkSize - this.oldStrChunkSize;
        attrIdChunk.dump(out);

        int usePermissionIdx = strChunk.getStringIndex("uses-permission");
        byte[] usePermissionStart = new byte[56];
        byte[] usePermissionEnd = new byte[24];

        // Body
        int tag = 0;
        do {
            boolean bShouldAddPermission = false;
            tag = parseNextTag(is);

            // Record the use-permission tag
            if (tag == AxmlManipulateBodyChunk.startTag) { // XML START TAG
                int nameSi = ManifestEditorNew.getInt(rawChunkData, 5 * 4);
                if (nameSi == usePermissionIdx) {
                    System.arraycopy(rawChunkData, 0, usePermissionStart, 0,
                            56);
                }
            } else if (tag == AxmlManipulateBodyChunk.endTag) {
                int nameSi = ManifestEditorNew.getInt(rawChunkData, 5 * 4);
                if (nameSi == usePermissionIdx) {
                    System.arraycopy(rawChunkData, 0, usePermissionEnd, 0, 24);
                    bShouldAddPermission = true;
                }
            }

            out.writeBytes(rawChunkData);

            // Write the added permission
            if (bShouldAddPermission && this.permissions != null) {
                for (int i = 1; i < this.permissions.size(); ++i) {
                    ManifestEditorNew.setInt(usePermissionStart, 44,
                            this.permissionStartIdx + i);
                    ManifestEditorNew.setInt(usePermissionStart, 52,
                            this.permissionStartIdx + i);
                    out.writeBytes(usePermissionStart);
                    fileSize += usePermissionStart.length;
                    out.writeBytes(usePermissionEnd);
                    fileSize += usePermissionEnd.length;
                }
            }
        } while (tag != AxmlManipulateBodyChunk.endDocTag);

        // Revise the file size
        out.writeInt(4, fileSize);

        out.close();
    }

}
