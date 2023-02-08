package com.gmail.heagoo.apkeditor.dex;

import java.util.List;

public class DexMapItem {
    public static final int kDexTypeHeaderItem = 0x0000;
    public static final int kDexTypeStringIdItem = 0x0001;
    public static final int kDexTypeTypeIdItem = 0x0002;
    public static final int kDexTypeProtoIdItem = 0x0003;
    public static final int kDexTypeFieldIdItem = 0x0004;
    public static final int kDexTypeMethodIdItem = 0x0005;
    public static final int kDexTypeClassDefItem = 0x0006;
    public static final int kDexTypeMapList = 0x1000;
    public static final int kDexTypeTypeList = 0x1001;
    public static final int kDexTypeAnnotationSetRefList = 0x1002;
    public static final int kDexTypeAnnotationSetItem = 0x1003;
    public static final int kDexTypeClassDataItem = 0x2000;
    public static final int kDexTypeCodeItem = 0x2001;
    public static final int kDexTypeStringDataItem = 0x2002;
    public static final int kDexTypeDebugInfoItem = 0x2003;
    public static final int kDexTypeAnnotationItem = 0x2004;
    public static final int kDexTypeEncodedArrayItem = 0x2005;
    public static final int kDexTypeAnnotationsDirectoryItem = 0x2006;

    short type;
    short unused;
    int size;
    int offset;

    public static DexMapItem from(byte[] buf, int offset) {
        DexMapItem item = new DexMapItem();
        item.type = DexParser.readShort(buf, offset);
        item.size = DexParser.readInt(buf, offset + 4);
        item.offset = DexParser.readInt(buf, offset + 8);
        return item;
    }

    public static DexMapItem findItem(List<DexMapItem> items, int targetType) {
        for (int i = 0; i < items.size(); ++i) {
            DexMapItem item = items.get(i);
            if (item.type == targetType) {
                return item;
            }
        }
        return null;
    }

    public void to(byte[] buf, int offset) {
        DexParser.writeShort(type, buf, offset);
        DexParser.writeInt(size, buf, offset + 4);
        DexParser.writeInt(this.offset, buf, offset + 8);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("type: ");
        String name = "";
        switch (type) {
            case kDexTypeHeaderItem:
                name = "kDexTypeHeaderItem";
                break;
            case kDexTypeStringIdItem:
                name = "kDexTypeStringIdItem";
                break;
            case kDexTypeTypeIdItem:
                name = "kDexTypeTypeIdItem";
                break;
            case kDexTypeProtoIdItem:
                name = "kDexTypeProtoIdItem";
                break;
            case kDexTypeFieldIdItem:
                name = "kDexTypeFieldIdItem";
                break;
            case kDexTypeMethodIdItem:
                name = "kDexTypeMethodIdItem";
                break;
            case kDexTypeClassDefItem:
                name = "kDexTypeClassDefItem";
                break;
            case 0x0007:
                name = "kDexTypeCallSiteIdItem";
                break;
            case 0x0008:
                name = "kDexTypeMethodHandleItem";
                break;
            case kDexTypeMapList:
                name = "kDexTypeMapList";
                break;
            case kDexTypeTypeList:
                name = "kDexTypeTypeList";
                break;
            case kDexTypeAnnotationSetRefList:
                name = "kDexTypeAnnotationSetRefList";
                break;
            case kDexTypeAnnotationSetItem:
                name = "kDexTypeAnnotationSetItem";
                break;
            case kDexTypeClassDataItem:
                name = "kDexTypeClassDataItem";
                break;
            case kDexTypeCodeItem:
                name = "kDexTypeCodeItem";
                break;
            case kDexTypeStringDataItem:
                name = "kDexTypeStringDataItem";
                break;
            case kDexTypeDebugInfoItem:
                name = "kDexTypeDebugInfoItem";
                break;
            case kDexTypeAnnotationItem:
                name = "kDexTypeAnnotationItem";
                break;
            case kDexTypeEncodedArrayItem:
                name = "kDexTypeEncodedArrayItem";
                break;
            case kDexTypeAnnotationsDirectoryItem:
                name = "kDexTypeAnnotationsDirectoryItem";
                break;
        }
        sb.append(name);
        sb.append("; ");
        sb.append("size: " + size + "; ");
        sb.append("offset: " + offset);
        return sb.toString();
    }
}