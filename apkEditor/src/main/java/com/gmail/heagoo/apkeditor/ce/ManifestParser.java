package com.gmail.heagoo.apkeditor.ce;

import android.util.TypedValue;

import com.gmail.heagoo.apkeditor.ce.e.MyFileOutput;
import com.gmail.heagoo.apkeditor.ce.e.MyInputStream;
import com.gmail.heagoo.apkeditor.ce.e.ResAttrIdChunk;
import com.gmail.heagoo.apkeditor.ce.e.ResStringChunk;

import java.io.IOException;
import java.io.InputStream;

interface IAttributeCallback {
    public void onAttribute(String tag, int attrNsIdx, int attrNameIdx,
                            int attrValIdx, int type, int attrValIdx2);
}

class AxmlBodyChunk {
    public static int endDocTag = 0x00100101;
    public static int startTag = 0x00100102;
    public static int endTag = 0x00100103;
    public static int namespaceTag = 0x00100100;
    public static int cdataTag = 0x00100104;
    // index in the string table ("android" & "manifest")
    ResStringChunk stringChunk;
    private byte[] rawChunkData;
    private int stringCount; // stringCount is already added by 1
    private int androidIndex;

    // addedAttrPosition = the position of added attribute name in string table
    public AxmlBodyChunk(ResStringChunk strChunk) {
        this.stringCount = strChunk.getStringCount();
        this.stringChunk = strChunk;

        for (int i = 0; i < stringCount; i++) {
            if ("android".equals(strChunk.getStringByIndex(i))) {
                androidIndex = i;
            }
        }
    }

    // Check the attribute value type whether refer to the string table
    public static boolean isTypeRefToStrTable(int type) {
        if (type == TypedValue.TYPE_STRING) {
            return true;
        }
        if (type == TypedValue.TYPE_ATTRIBUTE) {
            return false;
        }
        if (type == TypedValue.TYPE_REFERENCE) {
            return false;
        }
        if (type == TypedValue.TYPE_FLOAT) {
            return false;
        }
        if (type == TypedValue.TYPE_INT_HEX) {
            return false;
        }
        if (type == TypedValue.TYPE_INT_BOOLEAN) {
            return false;
        }
        if (type == TypedValue.TYPE_DIMENSION) {
            return false;
        }
        if (type == TypedValue.TYPE_FRACTION) {
            return false;
        }
        if (type >= TypedValue.TYPE_FIRST_COLOR_INT
                && type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return false;
        }
        if (type >= TypedValue.TYPE_FIRST_INT
                && type <= TypedValue.TYPE_LAST_INT) {
            return false;
        }
        return false;
    }

    private boolean isInterestedTag(int tagIdx) {
        String name = stringChunk.getStringByIndex(tagIdx);
        if ("uses-permission".equals(name) || "manifest".equals(name)
                || "application".equals(name) || "activity".equals(name)
                || "service".equals(name) || "receiver".equals(name)
                || "provider".equals(name) || "activity-alias".equals(name)
                || "category".equals(name) || "permission".equals(name)
                || "uses-sdk".equals(name)) {
            return true;
        }
        return false;
    }

    public byte[] getRawChunkData() {
        return rawChunkData;
    }

    public int parseNext(MyInputStream is, IAttributeCallback callback)
            throws IOException {
        int chunkTag = is.readInt();
        int chunkSize = is.readInt();

        rawChunkData = new byte[chunkSize];
        ManifestParser.setInt(rawChunkData, 0, chunkTag);
        ManifestParser.setInt(rawChunkData, 4, chunkSize);
        if (chunkSize > 8) {
            is.readFully(rawChunkData, 8, chunkSize - 8);
        }

        if (chunkTag == startTag) { // XML START TAG
            // int lineNo = AddAttribute.getInt(rawChunkData, 2 * 4);
            // int tag3 = AddAttribute.getInt(rawChunkData, 3 * 4);
            int nameNsSi = ManifestParser.getInt(rawChunkData, 4 * 4);
            int nameSi = ManifestParser.getInt(rawChunkData, 5 * 4);
            // Expected to be 14001400
            // int tag6 = AddAttribute.getInt(rawChunkData, 6 * 4);
            // Number of Attributes to follow
            int attrNum = ManifestParser.getInt(rawChunkData, 7 * 4);
            // Expected to be 00000000
            // int tag8 = AddAttribute.getInt(rawChunkData, 8 * 4);

            // Look for the Attributes
            // 0th word: StringIndex of Attribute Name's Namespace, or FFFFFFFF
            // 1st word: StringIndex of Attribute Name
            // 2nd word: StringIndex of Attribute Value, or FFFFFFF if
            // ResourceId
            // used
            // 3rd word: Flags? (This is type)
            // 4th word: str ind of attr value again, or ResourceId of value
            if (isInterestedTag(nameSi)) {
                for (int ii = 0; ii < attrNum; ii++) {
                    int attrNsIdx = ManifestParser.getInt(rawChunkData,
                            36 + ii * 20);
                    int attrNameIdx = ManifestParser.getInt(rawChunkData,
                            36 + ii * 20 + 4);
                    int attrValIdx = ManifestParser.getInt(rawChunkData,
                            36 + ii * 20 + 8);
                    int type = ManifestParser.getInt(rawChunkData,
                            36 + ii * 20 + 12) >> 16;
                    type = ((type & 0xff00) >> 8) | ((type & 0x00ff) << 8);
                    int attrValIdx2 = ManifestParser.getInt(rawChunkData,
                            36 + ii * 20 + 16);
                    ManifestParser
                            .log("%s=%s, type = 0X%x, attrValIdx=%d, attrValIdx2=%d",
                                    stringChunk.getStringByIndex(attrNameIdx),
                                    stringChunk.getStringByIndex(attrValIdx),
                                    type, attrValIdx, attrValIdx2);
                    callback.onAttribute(stringChunk.getStringByIndex(nameSi),
                            attrNsIdx, attrNameIdx, attrValIdx, type,
                            attrValIdx2);
                }
            }

        } else if (chunkTag == endTag) { // XML END TAG
            int nameNsSi = ManifestParser.getInt(rawChunkData, 4 * 4);
            int nameSi = ManifestParser.getInt(rawChunkData, 5 * 4);
        } else if (chunkTag == endDocTag) { // END OF XML DOC TAG
            int prefix = ManifestParser.getInt(rawChunkData, 4 * 4);
            int uri = ManifestParser.getInt(rawChunkData, 5 * 4);
        } else if (chunkTag == namespaceTag) {
            int prefix = ManifestParser.getInt(rawChunkData, 4 * 4);
            int uri = ManifestParser.getInt(rawChunkData, 5 * 4);
        } else if (chunkTag == cdataTag) {

        }

        return chunkTag;
    }
}

// Designed to add installLocation Attribute to AndroidManifest binary file
public class ManifestParser implements IAttributeCallback {

    private MyInputStream is;
    private MyFileOutput out;

    private int headTag;
    private int fileSize;
    private ResStringChunk strChunk;
    private ResAttrIdChunk attrIdChunk;

    // Value we concerned
    private ManifestInfo result;

    // Temp variable to record the string index of last activity[-alias]
    private int lastActivityNameIdx = -1;

    // Record provider info during manifest parsing
    private ManifestInfo.ProviderInfo unfinishedProvider;

    public ManifestParser(InputStream is) {
        this.is = new MyInputStream(is);
        this.result = new ManifestInfo();
    }

    public static void main(String[] args) throws Exception {
        // if (args.length < 1) {
        // System.out
        // .println("Usage: AddInstallLocationAttr.jar inputFile outputFile");
        // return;
        // }

        // String inputFile = args[0];
        // String outputFile = args[1];
        // String inputFile = "D:\\Android\\apk\\AndroidManifest.xml";
        // String outputFile = "D:\\Android\\apk\\out.xml";
        //
        // ManifestParser ama = new ManifestParser(new
        // FileInputStream(inputFile));
        //
        // ama.parse();
    }

    protected static int getInt(byte[] buf, int offset) {
        return ((int) buf[offset + 0] & 0xff)
                | (((int) buf[offset + 1] & 0xff) << 8)
                | (((int) buf[offset + 2] & 0xff) << 16)
                | (((int) buf[offset + 3] & 0xff) << 24);
    }

    protected static int getShort(byte[] buf, int offset) {
        return ((int) buf[offset + 0] & 0xff)
                | (((int) buf[offset + 1] & 0xff) << 8);
    }

    protected static void setInt(byte[] buf, int offset, int value) {
        buf[offset + 0] = (byte) (value & 0xff);
        buf[offset + 1] = (byte) ((value >> 8) & 0xff);
        buf[offset + 2] = (byte) ((value >> 16) & 0xff);
        buf[offset + 3] = (byte) ((value >> 24) & 0xff);
    }

    protected static void setShort(byte[] buf, int offset, int value) {
        buf[offset + 0] = (byte) (value & 0xff);
        buf[offset + 1] = (byte) ((value >> 8) & 0xff);
    }

    protected static void log(String format, Object... arguments) {
        // String msg = String.format(format, arguments);
        // System.out.println(msg);
    }

    // If the string inside string pool is NULL/empty, get it from attr table
    public static String getNameFromAttr(int attr) {
        switch (attr) {
            case 0x01010000:
                return "theme";
            case 0x01010001:
                return "label";
            case 0x01010002:
                return "icon";
            case 0x01010003:
                return "name";
            case 0x01010018:
                return "authorities";
            case 0x0101021c:
                return "versionName";
            case 0x0101021b:
                return "versionCode";
        }
        return null;
    }

    public ManifestInfo getManifestInfo() {
        return result;
    }

    public void parse() throws Exception {
        // Header
        this.headTag = is.readInt();
        this.fileSize = is.readInt();

        // String table
        this.strChunk = new ResStringChunk();
        strChunk.parse(is);
        log("ChunkSize of Resource String: %d", strChunk.chunkSize);
        log("String Count: %d", strChunk.getStringCount());
        //log("String Offset: 0x%x", strChunk.stringOffset);
        // if (strChunk.styleCount != 0 || strChunk.styleOffset != 0) {
        // throw new Exception(
        // "Detected style information, not supported yet!");
        // }
        for (int i = 0; i < strChunk.getStringCount(); ++i) {
            result.strings.add(strChunk.getStringByIndex(i));
        }

        // Resource Attribute ID table
        this.attrIdChunk = new ResAttrIdChunk();
        attrIdChunk.parse(is);

        // Body
        AxmlBodyChunk body = new AxmlBodyChunk(strChunk);
        int tag = 0;
        do {
            tag = body.parseNext(is, this);
        } while (tag != AxmlBodyChunk.endDocTag);
        fileSize += 20;
    }

    @Override
    public void onAttribute(String tag, int attrNsIdx, int attrNameIdx,
                            int attrValIdx, int type, int attrValIdx2) {
        String attrName = strChunk.getStringByIndex(attrNameIdx);
        if (attrName == null || attrName.equals("")) {
            if (attrNameIdx < attrIdChunk.getCount()) {
                attrName = getNameFromAttr(attrIdChunk.attrIdArray[attrNameIdx]);
                //Log.d("DEBUG", "getNameFromAttr return " + attrName);
            }
        }
        int idx = (attrValIdx2 >= 0 ? attrValIdx2 : attrValIdx);

        if ("uses-permission".equals(tag)) {
            if ("name".equals(attrName)) {
                this.result.permissions.add(strChunk.getStringByIndex(idx));
            }
        } else if ("manifest".equals(tag)) {
            if ("versionCode".equals(attrName)) {
                this.result.versionCode = idx;
            } else if ("versionName".equals(attrName)
                    && AxmlBodyChunk.isTypeRefToStrTable(type)) {
                this.result.verNameIdx = idx;
                this.result.versionName = strChunk.getStringByIndex(idx);
            } else if ("installLocation".equals(attrName)) {
                this.result.installLocation = idx;
            } else if ("package".equals(attrName)
                    && AxmlBodyChunk.isTypeRefToStrTable(type)) {
                this.result.pkgNameIdx = idx;
                this.result.packageName = strChunk.getStringByIndex(idx);
            }
        } else if ("application".equals(tag)) {
            if ("label".equals(attrName)) {
                if (AxmlBodyChunk.isTypeRefToStrTable(type)) {
                    this.result.appNameIdx = idx;
                    this.result.appName = strChunk.getStringByIndex(idx);
                } else { // resource id
                    this.result.appNameId = idx;
                }
            } else if ("name".equals(attrName)) {
                this.result.compNameIdxs.add(idx);
                this.result.applicationCls = strChunk.getStringByIndex(idx);
            } else if ("icon".equals(attrName)) {
                this.result.launcherId = idx;
            }
        } else if ("activity".equals(tag) || "service".equals(tag)
                || "receiver".equals(tag) || "provider".equals(tag)) {
            if ("name".equals(attrName)
                    && AxmlBodyChunk.isTypeRefToStrTable(type)) {
                this.result.compNameIdxs.add(idx);
                if ("activity".equals(tag)) {
                    this.result.compNameIdx2Type.put(idx, 0);
                } else if ("service".equals(tag)) {
                    this.result.compNameIdx2Type.put(idx, 1);
                } else if ("receiver".equals(tag)) {
                    this.result.compNameIdx2Type.put(idx, 2);
                } else if ("provider".equals(tag)) {
                    this.result.compNameIdx2Type.put(idx, 3);
                }
            }

            if ("activity".equals(tag) && "name".equals(attrName)) {
                this.lastActivityNameIdx = idx;
            }
            if ("provider".equals(tag)
                    && AxmlBodyChunk.isTypeRefToStrTable(type)) {
                if (this.unfinishedProvider == null) {
                    unfinishedProvider = new ManifestInfo.ProviderInfo();
                }
                if ("authorities".equals(attrName)) {
                    unfinishedProvider.authorityIdx = idx;
                    unfinishedProvider.authority = strChunk
                            .getStringByIndex(idx);
                } else if ("name".equals(attrName)) {
                    unfinishedProvider.nameIdx = idx;
                    unfinishedProvider.name = strChunk.getStringByIndex(idx);
                }
                if (unfinishedProvider.authority != null
                        && unfinishedProvider.name != null) {
                    this.result.providerInfoList.add(unfinishedProvider);
                    this.unfinishedProvider = null;
                }
            }
        } else if ("activity-alias".equals(tag)) {
            if ("name".equals(attrName)) {
                this.lastActivityNameIdx = idx;
            } else if ("targetActivity".equals(attrName)) {
                this.result.targetActivityIdxs.put(lastActivityNameIdx, idx);
            }
        }
        // To detect the launcher activity
        else if ("category".equals(tag)) {
            // android:name=android.intent.category.LAUNCHER
            if ("name".equals(attrName)) {
                if ("android.intent.category.LAUNCHER".equals(strChunk
                        .getStringByIndex(idx))) {
                    if (lastActivityNameIdx != -1) {
                        this.result.launcherActIdxs.add(lastActivityNameIdx);
                    }
                }
            }
        }
        // Self defined permissions
        else if ("permission".equals(tag)) {
            if ("name".equals(attrName)) {
                this.result.permissionIdx2Name.put(idx,
                        strChunk.getStringByIndex(idx));
            }
        }
        // SDK version
        else if ("uses-sdk".equals(tag)) {
            if ("minSdkVersion".equals(attrName)) {
                this.result.minSdkVersion = idx;
            } else if ("targetSdkVersion".equals(attrName)) {
                this.result.targetSdkVersion = idx;
            } else if ("maxSdkVersion".equals(attrName)) {
                this.result.maxSdkVersion = idx;
            }
        }
    }
}
