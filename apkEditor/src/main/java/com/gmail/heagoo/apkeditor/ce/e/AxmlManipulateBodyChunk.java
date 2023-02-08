package com.gmail.heagoo.apkeditor.ce.e;

import android.util.TypedValue;

import com.gmail.heagoo.apkeditor.ce.ManifestInfo;
import com.gmail.heagoo.apkeditor.ce.ManifestParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AxmlManipulateBodyChunk {

    public static int endDocTag = 0x00100101;
    public static int startTag = 0x00100102;
    public static int endTag = 0x00100103;
    public static int namespaceTag = 0x00100100;
    public static int cdataTag = 0x00100104;
    // index in the string table ("android" & "manifest")
    ResStringChunk stringChunk;
    ResAttrIdChunk attrIdChunk;
    private byte[] rawChunkData;
    private List<Integer> addedStrPositions;
    private int stringCount; // stringCount after modification
    private int androidIndex;
    private int manifestIndex;
    private int applicationIndex;
    private int activityIndex;
    private int activityAliasIndex;
    private int providerIndex = -1;

    private ManifestInfo oldInfo;
    private ManifestInfo newInfo;
    private ArrayList<Integer> launcherActNameIdxs;

    private boolean bModifyPkgName;

    // addedAttrPosition = the position of added attribute name in string table
    public AxmlManipulateBodyChunk(ResStringChunk strChunk, ResAttrIdChunk attrIdChunk,
                                   ManifestInfo oldInfo, ManifestInfo newInfo) {
        this.oldInfo = oldInfo;
        this.newInfo = newInfo;
        this.stringCount = strChunk.stringCount;
        this.addedStrPositions = strChunk.getAddedStrPositions();
        this.stringChunk = strChunk;
        this.attrIdChunk = attrIdChunk;

        this.bModifyPkgName = (newInfo.packageName != null && !newInfo.packageName
                .equals(oldInfo.packageName));

        for (int i = 0; i < stringCount; i++) {
            if ("android".equals(strChunk.stringValues[i])) {
                androidIndex = i;
            } else if ("manifest".equals(strChunk.stringValues[i])) {
                manifestIndex = i;
            } else if ("application".equals(strChunk.stringValues[i])) {
                applicationIndex = i;
            } else if ("activity".equals(strChunk.stringValues[i])) {
                activityIndex = i;
            } else if ("activity-alias".equals(strChunk.stringValues[i])) {
                activityAliasIndex = i;
            } else if ("provider".equals(strChunk.stringValues[i])) {
                providerIndex = i;
            }
        }

        // Revise the launcher activity name index
        this.launcherActNameIdxs = new ArrayList<Integer>();
        for (int idx : oldInfo.launcherActIdxs) {
            int newIndex = this.getRevisedIndex(idx);
            launcherActNameIdxs.add(newIndex);
        }
    }

    public byte[] getRawChunkData() {
        return rawChunkData;
    }

    public int parseNext(MyInputStream is) throws IOException {
        int chunkTag = is.readInt();
        int chunkSize = is.readInt();

        rawChunkData = new byte[chunkSize];
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

            ManifestEditorNew.log("Manifest?: (%d)%s", nameSi,
                    stringChunk.getStringByIndex(nameSi));

            // manifest
            if (nameSi == manifestIndex) {
                manipulateManifest(attrNum);
            }
            // Do not change the label inside application and activity any more
            // // application
            // else if (nameSi == applicationIndex) {
            // manipulateApplication(attrNum);
            // }
            // // activity[-alias]
            // else if (nameSi == activityIndex || nameSi == activityAliasIndex)
            // {
            // manipulateActivity(attrNum);
            // }
            // provider
            else if (nameSi == providerIndex) {
                manipulateProvider(attrNum);
            }
            // For other tag, we also need to change index
            else {
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

    // Change the authorities string to the new index, if its name==authorities,
    // as in such case we added a new string as the authorities name
    private void manipulateProvider(int attrNum) {
        int authorityIndex = -1;
        int nameIndex = -1;
        int authorityAttrIdx = -1;
        for (int ii = 0; ii < attrNum; ii++) {
            int attrNsIdx = ManifestEditorNew.getInt(rawChunkData, 36 + ii * 20);
            changeIndex(attrNsIdx, 36 + ii * 20);
            int attrNameIdx = ManifestEditorNew.getInt(rawChunkData,
                    36 + ii * 20 + 4);
            attrNameIdx = changeIndex(attrNameIdx, 36 + ii * 20 + 4);
            int attrValIdx = ManifestEditorNew.getInt(rawChunkData,
                    36 + ii * 20 + 8);
            attrValIdx = changeIndex(attrValIdx, 36 + ii * 20 + 8);
            int type = ManifestEditorNew.getInt(rawChunkData, 36 + ii * 20 + 12) >> 16;
            type = ((type & 0xff00) >> 8) | ((type & 0x00ff) << 8);
            int attrValIdx2 = ManifestEditorNew.getInt(rawChunkData,
                    36 + ii * 20 + 16);
            if (isTypeRefToStrTable(type) && attrValIdx2 >= 0) {
                attrValIdx2 = changeIndex(attrValIdx2, 36 + ii * 20 + 16);
            }

            String attrName = getAttributeName(attrNameIdx);
            if ("authorities".equals(attrName)) {
                authorityAttrIdx = ii;
                authorityIndex = (attrValIdx2 >= 0 ? attrValIdx2 : attrValIdx);
            } else if ("name".equals(attrName)) {
                nameIndex = (attrValIdx2 >= 0 ? attrValIdx2 : attrValIdx);
            }

//            String msg = String
//                    .format("provider, attrNameIdx=%d, name=%s, attrValIdx=%d, attrValIdx2=%d, value=%s",
//                            attrNameIdx,
//                            attrName,
//                            attrValIdx,
//                            attrValIdx2,
//                            stringChunk
//                                    .getStringByIndex(attrValIdx2 >= 0 ? attrValIdx2
//                                            : attrValIdx));
//            Log.d("DEBUG", msg);
        }

        // Change the authorities index (+1 as the new name inserted after it)
        if (this.bModifyPkgName && authorityIndex == nameIndex) {
            ManifestEditorNew.setInt(rawChunkData, 36 + authorityAttrIdx * 20 + 8,
                    authorityIndex + 1);
            ManifestEditorNew.setInt(rawChunkData,
                    36 + authorityAttrIdx * 20 + 16, authorityIndex + 1);
        }
    }

    private String getAttributeName(int attrNameIdx) {
        String name = stringChunk.getStringByIndex(attrNameIdx);
        if (name == null || name.equals("")) {
            if (attrNameIdx < attrIdChunk.getCount()) {
                name = ManifestParser
                        .getNameFromAttr(attrIdChunk.attrIdArray[attrNameIdx]);
            }
        }
        return name;
    }

    // Change the label of launcher activity
    @SuppressWarnings("unused")
    private void manipulateActivity(int attrNum) {
        boolean modifyAppName = (oldInfo.appNameIdx < 0
                && newInfo.appName != null && !newInfo.appName
                .equals(oldInfo.appName));

        boolean isLauncherActivity = false;
        int labelAttrIndex = -1;
        for (int ii = 0; ii < attrNum; ii++) {
            int attrNsIdx = ManifestEditorNew.getInt(rawChunkData, 36 + ii * 20);
            changeIndex(attrNsIdx, 36 + ii * 20);
            int attrNameIdx = ManifestEditorNew.getInt(rawChunkData,
                    36 + ii * 20 + 4);
            attrNameIdx = changeIndex(attrNameIdx, 36 + ii * 20 + 4);
            int attrValIdx = ManifestEditorNew.getInt(rawChunkData,
                    36 + ii * 20 + 8);
            attrValIdx = changeIndex(attrValIdx, 36 + ii * 20 + 8);
            int type = ManifestEditorNew.getInt(rawChunkData, 36 + ii * 20 + 12) >> 16;
            type = ((type & 0xff00) >> 8) | ((type & 0x00ff) << 8);
            int attrValIdx2 = ManifestEditorNew.getInt(rawChunkData,
                    36 + ii * 20 + 16);
            if (isTypeRefToStrTable(type)) {
                attrValIdx2 = changeIndex(attrValIdx2, 36 + ii * 20 + 16);
            }

            // To modify the label
            if (!modifyAppName) {
                continue;
            }

            String name = getAttributeName(attrNameIdx);
            if ("name".equals(name)) {
                int idx = (attrValIdx2 >= 0 ? attrValIdx2 : attrValIdx);
                // ManifestEditorNew.log("activity name idx = %d", idx);
                if (this.launcherActNameIdxs.contains(idx)) {
                    isLauncherActivity = true;
                }
            } else if ("label".equals(name)) {
                labelAttrIndex = ii;
            }
        }

        // ManifestEditorNew.log("isLauncherActivity = " + isLauncherActivity
        // + ", labelAttrIndex=" + labelAttrIndex);

        // This activity is the target to modify
        if (isLauncherActivity && labelAttrIndex != -1) {
            // Get the index of new app name
            int valIndex = -1;
            for (int i = 0; i < stringChunk.stringCount; i++) {
                String str = stringChunk.getStringByIndex(i);
                if (str.equals(newInfo.appName)) {
                    valIndex = i;
                    break;
                }
            }

            if (valIndex != -1) {
                ManifestEditorNew.setInt(rawChunkData,
                        36 + labelAttrIndex * 20 + 8, valIndex);
                ManifestEditorNew.setInt(rawChunkData,
                        36 + labelAttrIndex * 20 + 12, 0x03000008);
                ManifestEditorNew.setInt(rawChunkData,
                        36 + labelAttrIndex * 20 + 16, valIndex);
            }
        }
    }

    private void addAnAttribute(int attrNum, int nameIdx, int valIdx1,
                                int type, int valIdx2) {
        int chunkSize = rawChunkData.length;
        byte[] newRawBuf = new byte[rawChunkData.length + 20];
        System.arraycopy(rawChunkData, 0, newRawBuf, 0, 36);
        System.arraycopy(rawChunkData, 36, newRawBuf, 36 + 20, chunkSize - 36);

        // New attr (5 bytes)
        int newAttrOffset = 36;
        ManifestEditorNew.setInt(newRawBuf, newAttrOffset, androidIndex);
        ManifestEditorNew.setInt(newRawBuf, newAttrOffset + 4, nameIdx); // index(installLocation)
        // = 0
        ManifestEditorNew.setInt(newRawBuf, newAttrOffset + 8, valIdx1);
        ManifestEditorNew.setInt(newRawBuf, newAttrOffset + 12, type);
        ManifestEditorNew.setInt(newRawBuf, newAttrOffset + 16, valIdx2);

        // Chunk size & attr count
        chunkSize += 20;
        ManifestEditorNew.setInt(newRawBuf, 4, chunkSize); // Chunk
        // Size
        attrNum += 1;
        ManifestEditorNew.setInt(newRawBuf, 7 * 4, attrNum); // Attr
        // count

        rawChunkData = newRawBuf;
    }

    @SuppressWarnings("unused")
    private void manipulateApplication(int attrNum) {

        for (int ii = 0; ii < attrNum; ii++) {
            int attrNsIdx = ManifestEditorNew.getInt(rawChunkData, 36 + ii * 20);
            changeIndex(attrNsIdx, 36 + ii * 20);
            int attrNameIdx = ManifestEditorNew.getInt(rawChunkData,
                    36 + ii * 20 + 4);
            attrNameIdx = changeIndex(attrNameIdx, 36 + ii * 20 + 4);
            int attrValIdx = ManifestEditorNew.getInt(rawChunkData,
                    36 + ii * 20 + 8);
            attrValIdx = changeIndex(attrValIdx, 36 + ii * 20 + 8);
            int type = ManifestEditorNew.getInt(rawChunkData, 36 + ii * 20 + 12) >> 16;
            type = ((type & 0xff00) >> 8) | ((type & 0x00ff) << 8);
            int attrValIdx2 = ManifestEditorNew.getInt(rawChunkData,
                    36 + ii * 20 + 16);
            if (isTypeRefToStrTable(type)) {
                attrValIdx2 = changeIndex(attrValIdx2, 36 + ii * 20 + 16);
            }

            // We do not change the label any more,
            // as we modify it in resources.arsc now
            // // To modify the label
            // if (!modifyAppName) {
            // continue;
            // }
            //
            // String name = stringChunk.getStringByIndex(attrNameIdx);
            // if ("label".equals(name)) {
            // int valIndex = -1;
            // for (int i = 0; i < stringChunk.stringCount; i++) {
            // String str = stringChunk.getStringByIndex(i);
            // if (str.equals(newInfo.appName)) {
            // valIndex = i;
            // break;
            // }
            // }
            //
            // ManifestEditorNew.log("app name index =  " + valIndex);
            //
            // if (valIndex != -1) {
            // ManifestEditorNew.setInt(rawChunkData, 36 + ii * 20 + 8,
            // valIndex);
            // ManifestEditorNew.setInt(rawChunkData, 36 + ii * 20 + 12,
            // 0x03000008);
            // ManifestEditorNew.setInt(rawChunkData, 36 + ii * 20 + 16,
            // valIndex);
            // }
            // }
        }
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
            int type = ManifestEditorNew.getInt(rawChunkData, 36 + ii * 20 + 12) >> 16;
            type = ((type & 0xff00) >> 8) | ((type & 0x00ff) << 8);
            int attrValIdx2 = ManifestEditorNew.getInt(rawChunkData,
                    36 + ii * 20 + 16);
            if (isTypeRefToStrTable(type)) {
                attrValIdx2 = changeIndex(attrValIdx2, 36 + ii * 20 + 16);
            }

            // To modify it
            String name = getAttributeName(attrNameIdx);
            if ("versionCode".equals(name)) {
                if (attrValIdx2 != -1) { // This value is the version code
                    ManifestEditorNew.setInt(rawChunkData, 36 + ii * 20 + 16,
                            newInfo.versionCode);
                } else if (attrValIdx != -1) {
                    ManifestEditorNew.setInt(rawChunkData, 36 + ii * 20 + 8,
                            newInfo.versionCode);
                }
            } else if ("versionName".equals(name)) {
                // Version name is changed in string chunk
            } else if ("package".equals(name)) {
                // Package name is changed in string chunk
            } else if ("installLocation".equals(name)) {
                if (attrValIdx2 != -1) { // This value is the location
                    ManifestEditorNew.setInt(rawChunkData, 36 + ii * 20 + 16,
                            newInfo.installLocation);
                } else if (attrValIdx != -1) {
                    ManifestEditorNew.setInt(rawChunkData, 36 + ii * 20 + 8,
                            newInfo.installLocation);
                }
            }

            ManifestEditorNew
                    .log("\t%s(%d)=%s(%d), type = 0X%x, attrValIdx=%d, attrValIdx2=%d",
                            stringChunk.getStringByIndex(attrNameIdx),
                            attrNameIdx,
                            stringChunk.getStringByIndex(attrValIdx),
                            attrValIdx, type, attrValIdx, attrValIdx2);
        }

        // Need to add install location
        if (oldInfo.installLocation == -1 && newInfo.installLocation != -1) {
            addAnAttribute(attrNum, 0, -1, 0x10000008, newInfo.installLocation);
            // int chunkSize = rawChunkData.length;
            // byte[] newRawBuf = new byte[rawChunkData.length + 20];
            // System.arraycopy(rawChunkData, 0, newRawBuf, 0, 36);
            // System.arraycopy(rawChunkData, 36, newRawBuf, 36 + 20,
            // chunkSize - 36);
            //
            // // New attr (5 bytes)
            // int newAttrOffset = 36;
            // ManifestEditorNew.setInt(newRawBuf, newAttrOffset, androidIndex);
            // ManifestEditorNew.setInt(newRawBuf, newAttrOffset + 4, 0); //
            // index(installLocation)
            // // = 0
            // ManifestEditorNew.setInt(newRawBuf, newAttrOffset + 8, -1);
            // ManifestEditorNew.setInt(newRawBuf, newAttrOffset + 12, 0x10000008);
            // ManifestEditorNew.setInt(newRawBuf, newAttrOffset + 16,
            // newInfo.installLocation);
            //
            // // Chunk size & attr count
            // chunkSize += 20;
            // ManifestEditorNew.setInt(newRawBuf, 4, chunkSize); // Chunk
            // // Size
            // attrNum += 1;
            // ManifestEditorNew.setInt(newRawBuf, 7 * 4, attrNum); // Attr
            // // count
            //
            // rawChunkData = newRawBuf;
        }
    }

    // Check the attribute value type whether refer to the string table
    private boolean isTypeRefToStrTable(int type) {
        return (type == TypedValue.TYPE_STRING);
    }

    private int changeIndex(int strIndex, int offset) {
        int skip = 0;
        for (int i = addedStrPositions.size() - 1; i >= 0; i--) {
            if (strIndex >= addedStrPositions.get(i)) {
                skip += 1;
            }
        }
        if (skip > 0) {
            ManifestEditorNew.setInt(rawChunkData, offset, strIndex + skip);
        }
        return strIndex + skip;
        // if (strIndex >= addedAttrPosition && strIndex < stringCount - 1) {
        // AddSharedUserId.setInt(rawChunkData, offset, strIndex + 1);
        // return strIndex + 1;
        // }
        // return strIndex;
    }

    // strIndex is the index before modification
    private int getRevisedIndex(int strIndex) {
        int skip = 0;
        for (int i = addedStrPositions.size() - 1; i >= 0; i--) {
            if (strIndex >= addedStrPositions.get(i)) {
                skip += 1;
            }
        }
        return strIndex + skip;
    }

}
