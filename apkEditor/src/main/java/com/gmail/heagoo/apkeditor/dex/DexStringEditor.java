package com.gmail.heagoo.apkeditor.dex;

import android.util.Log;

import com.gmail.heagoo.apkeditor.ce.e.ResStringChunk;
import com.gmail.heagoo.common.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.Adler32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DexStringEditor {

    // For proto
    public static final int SHORTY_OFFSET = 0;
    public static final int RETURN_TYPE_OFFSET = 4;
    public static final int PARAMETERS_OFFSET = 8;
    // For type list
    public static final int SIZE_OFFSET = 0;
    public static final int LIST_OFFSET = 4;
    // For class def
    public static final int ACCESS_FLAGS_OFFSET = 4;
    public static final int SUPERCLASS_OFFSET = 8;
    public static final int INTERFACES_OFFSET = 12;
    public static final int SOURCE_FILE_OFFSET = 16;
    public static final int ANNOTATIONS_OFFSET = 20;
    public static final int CLASS_DATA_OFFSET = 24;
    public static final int STATIC_VALUES_OFFSET = 28;
    // For field
    private static final int CLASS_OFFSET = 0;
    private static final int TYPE_OFFSET = 2;
    private static final int NAME_OFFSET = 4;
    // For method: class, proto, name
    private static final int PROTO_OFFSET = 2;
    // Dex buffer
    private byte[] dexBuf;
    // Parser
    private DexParser parser;

    // Buffer for the class data section
    private List<PatchBuffer> patchBuffers = new ArrayList<>();

    public DexStringEditor(String zipFilePath) {
        this(zipFilePath, "classes.dex");
    }

    public DexStringEditor(String zipFilePath, String targetDex) {
        ZipFile zfile = null;
        BufferedInputStream is = null;
        try {
            zfile = new ZipFile(zipFilePath);
            ZipEntry ze = zfile.getEntry(targetDex);

            int dexFileSize = (int) ze.getSize();
            this.dexBuf = new byte[dexFileSize];

            is = new BufferedInputStream(zfile.getInputStream(ze));
            int totalLen = 0;
            while (totalLen < dexFileSize) {
                int readLen = is.read(dexBuf, totalLen, dexFileSize - totalLen);
                totalLen += readLen;
            }

            // Create parser
            parser = new DexParser(dexBuf);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(zfile);
        }
    }

    public static String fieldAsString(DexParser parser, int fieldIndex)
            throws MyException {
        int fieldOffset = parser.getFieldIdItemOffset(fieldIndex);
        int classIndex = parser.readUshort(fieldOffset + CLASS_OFFSET);
        String classType = parser.getType(classIndex);

        int typeIndex = parser.readUshort(fieldOffset + TYPE_OFFSET);
        String fieldType = parser.getType(typeIndex);

        int nameIndex = parser.readSmallUint(fieldOffset + NAME_OFFSET);
        String fieldName = parser.getString(nameIndex);

        return String.format("%s->%s:%s", classType, fieldName, fieldType);
    }

    public static String methodAsString(DexParser parser, int methodIndex)
            throws MyException {
        int methodOffset = parser.getMethodIdItemOffset(methodIndex);
        int classIndex = parser.readUshort(methodOffset + CLASS_OFFSET);
        String classType = parser.getType(classIndex);

        int protoIndex = parser.readUshort(methodOffset + PROTO_OFFSET);
        String protoString = protoAsString(parser, protoIndex);

        int nameIndex = parser.readSmallUint(methodOffset + NAME_OFFSET);
        String methodName = parser.getString(nameIndex);

        return String.format("%s->%s%s", classType, methodName, protoString);
    }

    public static String protoAsString(DexParser parser, int protoIndex)
            throws MyException {
        int offset = parser.getProtoIdItemOffset(protoIndex);

        StringBuilder sb = new StringBuilder();
        sb.append("(");

        int parametersOffset = parser.readSmallUint(offset + PARAMETERS_OFFSET);
        sb.append(typeListAsString(parser, parametersOffset));
        sb.append(")");

        int returnTypeIndex = parser.readSmallUint(offset + RETURN_TYPE_OFFSET);
        String returnType = parser.getType(returnTypeIndex);
        sb.append(returnType);

        return sb.toString();
    }

    public static String typeListAsString(DexParser parser, int typeListOffset)
            throws MyException {
        if (typeListOffset == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        int size = parser.readSmallUint(typeListOffset);
        for (int i = 0; i < size; i++) {
            int typeIndex = parser.readUshort(typeListOffset + 4 + i * 2);
            String type = parser.getType(typeIndex);
            sb.append(type);
        }
        return sb.toString();
    }

    public static String classDefAsString(DexParser parser, int classIndex)
            throws MyException {
        int offset = parser.getClassDefItemOffset(classIndex);
        int typeIndex = parser.readSmallUint(offset + CLASS_OFFSET);

        int accessFlags = parser.readInt(offset + ACCESS_FLAGS_OFFSET);
        System.out.printf("access_flags = 0x%x: %s, ", accessFlags, AccessFlags
                .getAccessFlagsForClass(accessFlags).toString());

        int superclassIndex = parser.readOptionalUint(offset
                + SUPERCLASS_OFFSET);
        if (superclassIndex != -1) {
            System.out.printf("superclass_idx[%d] = %s ", superclassIndex,
                    parser.getType(superclassIndex));
        } else {
            System.out.print("no super class, ");
        }

        // int interfacesOffset = dexFile.readSmallUint(out.getCursor());
        // out.annotate(4, "interfaces_off = %s",
        // TypeListItem.getReferenceAnnotation(dexFile, interfacesOffset));
        //
        int sourceFileIdx = parser
                .readOptionalUint(offset + SOURCE_FILE_OFFSET);
        if (sourceFileIdx != -1) {
            System.out.printf("source_file_idx[%d] = %s, ", sourceFileIdx,
                    parser.getString(sourceFileIdx));
        } else {
            System.out.print("source_file_idx = -1");
        }
        //
        // int annotationsOffset = dexFile.readSmallUint(out.getCursor());
        // if (annotationsOffset == 0) {
        // out.annotate(4,
        // "annotations_off = annotations_directory_item[NO_OFFSET]");
        // } else {
        // out.annotate(4, "annotations_off = annotations_directory_item[0x%x]",
        // annotationsOffset);
        // }
        //
        // int classDataOffset = dexFile.readSmallUint(out.getCursor());
        // if (classDataOffset == 0) {
        // out.annotate(4, "class_data_off = class_data_item[NO_OFFSET]");
        // } else {
        // out.annotate(4, "class_data_off = class_data_item[0x%x]",
        // classDataOffset);
        // addClassDataIdentity(classDataOffset, dexFile.getType(classIndex));
        // }
        //
        // int staticValuesOffset = dexFile.readSmallUint(out.getCursor());
        // if (staticValuesOffset == 0) {
        // out.annotate(4, "static_values_off = encoded_array_item[NO_OFFSET]");
        // } else {
        // out.annotate(4, "static_values_off = encoded_array_item[0x%x]",
        // staticValuesOffset);
        // }

        return parser.getType(typeIndex);
    }

    private static String getHexString(byte[] buf) {
        // Create Hex String
        StringBuffer hexString = new StringBuffer();
        // Convert to hex
        for (int i = 0; i < buf.length; i++) {
            String shaHex = Integer.toHexString(buf[i] & 0xFF);
            if (shaHex.length() < 2) {
                hexString.append(0);
            }
            hexString.append(shaHex);
        }
        return hexString.toString();
    }

    private static void reviseHash(byte[] buf) {
        // SHA-1
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1");
            digest.update(buf, 32, buf.length - 32);
            byte messageDigest[] = digest.digest();
            // Copy to the buffer
            System.arraycopy(messageDigest, 0, buf, 12, messageDigest.length);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private static void reviseHash(List<MyBuffer> buffers) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1");
            // Remove the header of the first buffer
            MyBuffer head = buffers.get(0);
            digest.update(head.buf, 32, head.len - 32);
            for (int i = 1; i < buffers.size(); ++i) {
                MyBuffer buffer = buffers.get(i);
                digest.update(buffer.buf, buffer.offset, buffer.len);
            }
            byte messageDigest[] = digest.digest();
            // Copy to the buffer
            System.arraycopy(messageDigest, 0, head.buf, 12, messageDigest.length);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private static void reviseCrc32(byte[] buf) {
        // crc32
        byte[] crc32 = new byte[4];
        Adler32 checksum = new Adler32();
        checksum.update(buf, 12, buf.length - 12);
        long val = checksum.getValue();
        crc32[0] = (byte) (val & 0xff);
        crc32[1] = (byte) ((val >> 8) & 0xff);
        crc32[2] = (byte) ((val >> 16) & 0xff);
        crc32[3] = (byte) ((val >> 24) & 0xff);
        // Copy to buffer
        System.arraycopy(crc32, 0, buf, 8, crc32.length);
    }

    private static void reviseCrc32(List<MyBuffer> buffers) {
        // crc32
        byte[] crc32 = new byte[4];
        Adler32 checksum = new Adler32();
        MyBuffer head = buffers.get(0);
        checksum.update(head.buf, 12, head.len - 12);
        for (int i = 1; i < buffers.size(); ++i) {
            MyBuffer buffer = buffers.get(i);
            checksum.update(buffer.buf, buffer.offset, buffer.len);
        }
        long val = checksum.getValue();
        crc32[0] = (byte) (val & 0xff);
        crc32[1] = (byte) ((val >> 8) & 0xff);
        crc32[2] = (byte) ((val >> 16) & 0xff);
        crc32[3] = (byte) ((val >> 24) & 0xff);
        // Copy to buffer
        System.arraycopy(crc32, 0, head.buf, 8, crc32.length);
    }

    private static void saveToFile(byte[] buf, String filePath) throws Exception {
        FileOutputStream fos = new FileOutputStream(filePath);
        fos.write(buf);
        fos.close();
    }

    private static void saveToFile(List<MyBuffer> buffers, String filePath) throws Exception {
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filePath));
        for (MyBuffer buffer : buffers) {
            out.write(buffer.buf, buffer.offset, buffer.len);
        }
        out.close();
    }

    // TODO: we currently assume the string is coded in single byte
    private static void doReplace(byte[] buf, int contentOffset, String str) {
        //Log.d("DEBUG", "In doReplace, offset=" + contentOffset);
        System.arraycopy(str.getBytes(), 0, buf, contentOffset, str.length());
    }

    // Rename old package name to new package name, and re-order the string
    public boolean refactorPackageName(String oldName, String newName, String savePath)
            throws Exception {
        // Get all the string offset
        int stringCount = parser.getStringCount();
        int offsetArray[] = new int[stringCount];
        List<DexStringItem> dexStringList = new ArrayList<>(stringCount);
        for (int index = 0; index < stringCount; index++) {
            int itemOffset = parser.getStringIdItemOffset(index);
            int strOffset = (dexBuf[itemOffset] & 0xff)
                    | ((dexBuf[itemOffset + 1] & 0xff) << 8)
                    | ((dexBuf[itemOffset + 2] & 0xff) << 16)
                    | ((dexBuf[itemOffset + 3]) << 24);
            offsetArray[index] = strOffset;
        }

        // Decode all the string
        char firstChar = oldName.charAt(0);
        DexReader reader = new DexReader(dexBuf, offsetArray[0]);
        for (int index = 0; index < stringCount; index++) {
            DexStringItem strItem;

            reader.setOffset(offsetArray[index]);
            int utf16Length = reader.readSmallUleb128();
            String strVal = null;

            // Only decode the string starts with 'L', '[', 'c', or the first char of package name
            // 'c' is for "content://"
            int curOff = reader.getOffset();
            if (dexBuf[curOff] == 'L'
                    || dexBuf[curOff] == '['
                    || dexBuf[curOff] == 'c'
                    || dexBuf[curOff] == firstChar) {
                strVal = reader.readString(utf16Length);
            }

            if ((index + 1 < stringCount) && (offsetArray[index + 1] > offsetArray[index])) {
                strItem = new DexStringItem(index, strVal, dexBuf,
                        offsetArray[index], offsetArray[index + 1] - offsetArray[index]);
            } else {
                if (strVal == null) {
                    strVal = reader.readString(utf16Length);
                }
                strItem = new DexStringItem(index, strVal, dexBuf, offsetArray[index],
                        new DexStringItem(index, strVal).getEncodedLength());
            }

            dexStringList.add(strItem);
        }

        return doRefactor(savePath, dexStringList, oldName, newName);
    }

    // Refactor strings
    private boolean doRefactor(String savePath, List<DexStringItem> dexStringList,
                               String oldName, String newName) {
        String LoldName = getLFormat(oldName);
        String LarrayOld = "[" + LoldName;
        String LnewName = getLFormat(newName);
        String LarrayNew = "[" + LnewName;
        String oldContent = "content://" + oldName;

        int clsMatchStart = -1, clsMatchEnd = -1;
        int pkgMatchStart = -1, pkgMatchEnd = -1;
        int arrayClsStart = -1, arrayClsEnd = -1;
        int contentStart = -1, contentEnd = -1; // "content://[PACKAGE NAME]"
        for (int index = 0; index < dexStringList.size(); ++index) {
            DexStringItem strItem = dexStringList.get(index);
            if (strItem.value == null) {
                continue;
            }

            // Classes that need refactor
            if (strItem.value.startsWith(LoldName)) {
                strItem.setValue(LnewName + strItem.value.substring(LoldName.length()));
                if (clsMatchStart == -1) {
                    clsMatchStart = index;
                }
                clsMatchEnd = index + 1;
            }
            // String starts with package name
            else if (strItem.value.startsWith(oldName)) {
                strItem.setValue(newName + strItem.value.substring(oldName.length()));
                if (pkgMatchStart == -1) {
                    pkgMatchStart = index;
                }
                pkgMatchEnd = index + 1;
            }
            // Array Classes that need refactor
            else if (strItem.value.startsWith(LarrayOld)) {
                strItem.setValue(LarrayNew + strItem.value.substring(LarrayOld.length()));
                if (clsMatchStart == -1) {
                    arrayClsStart = index;
                }
                arrayClsEnd = index + 1;
            }
            // provider name that need refactor
            else if (strItem.value.startsWith(oldContent)) {
                strItem.setValue("content://" + newName +
                        strItem.value.substring(oldContent.length()));
                if (contentStart == -1) {
                    contentStart = index;
                }
                contentEnd = index + 1;
            }
        }

        // Cannot find any string starts with the old package name
        if (clsMatchStart == -1) {
            return false;
        }

        if (stringOrderChanged(dexStringList, clsMatchStart, clsMatchEnd)) {
            Log.e("DEBUG", "The string order is changed! (as the class name change)");
        }
        if (pkgMatchStart != -1) {
            if (stringOrderChanged(dexStringList, pkgMatchStart, pkgMatchEnd)) {
                Log.e("DEBUG", "The string order is changed! (as the pkg name change)");
            }
        }
        if (arrayClsStart != -1) {
            if (stringOrderChanged(dexStringList, arrayClsStart, arrayClsEnd)) {
                Log.e("DEBUG", "The string order is changed! (as the array class name change)");
            }
        }

        return simpleRefactor(savePath, dexStringList);
    }

    // Check if we break the order as changed the string between [modifyStart, modifyEnd)
    private boolean stringOrderChanged(List<DexStringItem> dexStringList, int modifiyStart, int modifyEnd) {
        int stringCount = dexStringList.size();
        DexStringItem beforeItem = null;
        DexStringItem afterItem = null;
        if (modifiyStart > 0) {
            beforeItem = dexStringList.get(modifiyStart - 1);
            //Log.d("DEBUG", "beforeItem=" + beforeItem.value);
        }
        if (modifyEnd < stringCount) {
            afterItem = dexStringList.get(modifyEnd);
            //Log.d("DEBUG", "afterItem=" + afterItem.value);
        }

//        for (int i = clsMatchStart; i < clsMatchEnd; ++i) {
//            Log.d("DEBUG", "" + i + ": " + dexStringList.get(i).value);
//        }

        // The order is changed
        return ((beforeItem != null && dexStringList.get(modifiyStart).compare(beforeItem) < 0)
                || (afterItem != null && dexStringList.get(modifyEnd - 1).compare(afterItem) > 0));
    }

    private boolean simpleRefactor(String savePath, List<DexStringItem> dexStringList) {
        try {
            // Find the original string data offset and length
            int originStringStart = 0;
            int originStringSize = 0;
            List<DexMapItem> items = parser.getMapItems();
            for (DexMapItem item : items) { // DEBUG
                //Log.d("DEBUG", item.toString());
            }
            // Get the size of string data section
            for (int i = 0; i < items.size(); ++i) {
                DexMapItem item = items.get(i);
                if (item.type == DexMapItem.kDexTypeStringDataItem) {
                    originStringStart = item.offset;
                    if (i + 1 < items.size()) {
                        originStringSize = items.get(i + 1).offset - item.offset;
                    } else {
                        originStringSize = dexBuf.length - item.offset;
                    }
                    break;
                }
            }
            //Log.d("DEBUG", String.format("%d, %d", originStringStart, originStringSize));

            // Cannot get information in map section, then get it from dex string list
            if (originStringSize == 0) {
                originStringStart = dexStringList.get(0).offset;
                DexStringItem last = dexStringList.get(dexStringList.size() - 1);
                originStringSize = last.offset + last.len - dexStringList.get(0).offset;
                //Log.d("DEBUG", String.format("%d, %d", originStringStart, originStringSize));
            }

            // Compute the length needed, and write encoded bytes to new string buffer
            int newStringDataSize = 0;
            for (DexStringItem strItem : dexStringList) {
                newStringDataSize += strItem.getEncodedLength();
            }
            do { // Make sure next section is aligned
                if ((newStringDataSize - originStringSize) % 4 == 0) {
                    break;
                }
                if ((newStringDataSize - originStringSize + 1) % 4 == 0) {
                    newStringDataSize += 1;
                    break;
                }
                if ((newStringDataSize - originStringSize + 2) % 4 == 0) {
                    newStringDataSize += 2;
                    break;
                }
                if ((newStringDataSize - originStringSize + 3) % 4 == 0) {
                    newStringDataSize += 3;
                    break;
                }
            } while (false);

            int offset = 0;
            byte[] newStringBuf = new byte[newStringDataSize];
            for (int i = 0; i < dexStringList.size(); ++i) {
                // Update the string id item
                DexParser.writeInt(originStringStart + offset, dexBuf,
                        parser.getStringIdItemOffset(i));
                DexStringItem strItem = dexStringList.get(i);
                offset += strItem.writeToBuffer(newStringBuf, offset);
            }

            // Prepare
            int delta = newStringDataSize - originStringSize;
            int affectedOffset = originStringStart + originStringSize;

            // kDexTypeProtoIdItem
            reviseProtoIdItems(affectedOffset, delta);

            // kDexTypeClassDefItem
            // Will also revise kDexTypeCodeItem and code in kDexTypeCodeItem
            reviseClassDefItems(affectedOffset, delta);

            // Revise annotation related
            reviseAnnocationDirectoryItems(items, affectedOffset, delta);
            reviseAnnotationSetItems(items, affectedOffset, delta);
            reviseAnnotationSetRefList(items, affectedOffset, delta);

            // Revise the map item
            reviseMapItem(items, affectedOffset, delta);

            // Revise the header
            parser.reviseHeader(affectedOffset, delta);

            // 3 or 5 buffers together compose the new buffer
            List<MyBuffer> buffers = new ArrayList<>();
            buffers.add(new MyBuffer(dexBuf, 0, originStringStart));
            buffers.add(new MyBuffer(newStringBuf, 0, newStringDataSize));
            if (originStringStart + originStringSize < dexBuf.length) {
                buffers.add(new MyBuffer(dexBuf, originStringStart + originStringSize,
                        dexBuf.length - originStringStart - originStringSize));
            }

            // Update hash/checksum and save to file
            reviseHash(buffers);
            reviseCrc32(buffers);
            saveToFile(buffers, savePath);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //    struct DexAnnotationsDirectoryItem {
//        u4  classAnnotationsOff;  /* 指向 DexAnnotationSetItem 的文件偏移，若无，值为0*/
//        u4  fieldsSize;           /* DexFieldAnnotationsItem 的个数*/
//        u4  methodsSize;          /* DexMethodAnnotationsItem 的个数*/
//        u4  parametersSize;       /* DexParameterAnnotationsItem 的个数*/
//    /* followed by DexFieldAnnotationsItem[fieldsSize] */
//    /* followed by DexMethodAnnotationsItem[methodsSize] */
//    /* followed by DexParameterAnnotationsItem[parametersSize] */
//    };
//    struct DexFieldAnnotationsItem {
//        u4  fieldIdx;
//        u4  annotationsOff;             /* 指向 DexAnnotationSetItem 的文件偏移*/
//    };
//    struct DexMethodAnnotationsItem {
//        u4  methodIdx;
//        u4  annotationsOff;             /* 指向 DexAnnotationSetItem 的文件偏移 */
//    };
//    struct DexParameterAnnotationsItem {
//        u4  methodIdx;
//        u4  annotationsOff;             /* 指向 DexAnnotationSetRefList 的文件偏移*/
//    };
//    struct DexAnnotationSetRefList {
//        u4  size;
//        DexAnnotationSetRefItem list[1];
//    };
//    struct DexAnnotationSetRefItem {
//        u4  annotationsOff;             /* offset to DexAnnotationSetItem */
//    };
    private void reviseAnnocationDirectoryItems(List<DexMapItem> items, int affectedOffset, int delta) {
        DexMapItem annotationDir = DexMapItem.findItem(
                items, DexMapItem.kDexTypeAnnotationsDirectoryItem);
        if (annotationDir == null) {
            return;
        }
        int offset = annotationDir.offset;
        for (int index = 0; index < annotationDir.size; ++index) {
            reviseIntValue(offset, affectedOffset, delta);
            offset += 4;
            int fieldsSize = DexParser.readInt(dexBuf, offset);
            offset += 4;
            int methodsSize = DexParser.readInt(dexBuf, offset);
            offset += 4;
            int parametersSize = DexParser.readInt(dexBuf, offset);
            offset += 4;
            for (int i = 0; i < fieldsSize; ++i) {
                offset += 4;
                reviseIntValue(offset, affectedOffset, delta);
                offset += 4;
            }
            for (int i = 0; i < methodsSize; ++i) {
                offset += 4;
                reviseIntValue(offset, affectedOffset, delta);
                offset += 4;
            }
            for (int i = 0; i < parametersSize; ++i) {
                offset += 4;
                reviseIntValue(offset, affectedOffset, delta);
                offset += 4;
            }
        }
    }

    //    struct DexAnnotationSetItem {
//        u4  size;               /* DexAnnotationItem 的个数*/
//        u4  entries[1];         /* DexAnnotationItem 的内容*/
//    };
    private void reviseAnnotationSetItems(List<DexMapItem> items, int affectedOffset, int delta) {
        DexMapItem item = DexMapItem.findItem(items, DexMapItem.kDexTypeAnnotationSetItem);
        if (item == null) {
            return;
        }

        int offset = item.offset;
        for (int index = 0; index < item.size; ++index) {
            int size = DexParser.readInt(dexBuf, offset);
            offset += 4;

            for (int i = 0; i < size; ++i) {
                reviseIntValue(offset, affectedOffset, delta);
                offset += 4;
            }
        }
    }

    private void reviseAnnotationSetRefList(List<DexMapItem> items, int affectedOffset, int delta) {
        DexMapItem item = DexMapItem.findItem(items, DexMapItem.kDexTypeAnnotationSetRefList);
        if (item == null) {
            return;
        }

        int offset = item.offset;
        for (int index = 0; index < item.size; ++index) {
            int size = DexParser.readInt(dexBuf, offset);
            offset += 4;

            for (int i = 0; i < size; ++i) {
                reviseIntValue(offset, affectedOffset, delta);
                offset += 4;
            }
        }
    }

    // Revise the map section
    private void reviseMapItem(List<DexMapItem> items, int affectedOffset, int delta) {
        for (int i = 0; i < items.size(); ++i) {
            DexMapItem item = items.get(i);
            if (item.offset >= affectedOffset) {
                item.offset += delta;
                int _offset = parser.getMapOffset() + 4 + i * 12;
                item.to(dexBuf, _offset);
            }
        }
    }

    // Change the value at offset, if the old value is greater than (equal to) the threshold
    private void reviseIntValue(int offset, int threshold, int delta) {
        int oldVal = DexParser.readInt(dexBuf, offset);
        if (oldVal >= threshold) {
            DexParser.writeInt(oldVal + delta, dexBuf, offset);
        }
    }

    //    struct DexProtoId {
//        u4  shortyIdx;          /* index in DexStringId */
//        u4  returnTypeIdx;      /* index DexTypeId */
//        u4  parametersOff;      /* offset to DexTypeList */
//    };
    private void reviseProtoIdItems(int affectedOffset, int delta) {
        int count = parser.getProtoCount();
        int start = parser.getProtoStartOffset();
        int offset = start + 8;
        for (int i = 0; i < count; ++i) {
            reviseIntValue(offset, affectedOffset, delta);
            offset += 12;
        }
    }

    //    struct DexClassDef {
//        u4  classIdx;           /* 类的类型，指向DexTypeId列表索引 */
//        u4  accessFlags;        /* 访问标志 */
//        u4  superclassIdx;      /* 父类的类型，指向DexTypeId列表的索引 */
//        u4  interfacesOff;      /* 实现了哪些接口，指向DexTypeList结构的偏移 */
//        u4  sourceFileIdx;      /* 源文件名，指向DexStringId列表的索引 */
//        u4  annotationsOff;     /* 注解，指向DexAnnotationsDirectoryItem结构的偏移 */
//        u4  classDataOff;       /* 指向DexClassData结构的偏移 */
//        u4  staticValuesOff;    /* 指向DexEncodedArray结构的偏移 */
//    };
    private void reviseClassDefItems(int affectedOffset, int delta) {
        int offset = parser.getClassStartOffset();
        int count = parser.getClassCount();

        // ERROR when i = 8
        for (int i = 0; i < count; ++i) {
            int addr = offset + i * 32;
            reviseIntValue(addr + 12, affectedOffset, delta);
            reviseIntValue(addr + 20, affectedOffset, delta);
            reviseIntValue(addr + 28, affectedOffset, delta);

            // classDataOff
            int clsDataOff = DexParser.readInt(dexBuf, addr + 24);
            if (clsDataOff >= affectedOffset) {
                DexParser.writeInt(clsDataOff + delta, dexBuf, addr + 24);
            }

            if (clsDataOff > 0) {
                reviseClassDataItem(clsDataOff, affectedOffset, delta);
            }
        }
    }

    //    DexClassData {
//        DexClassDataHeader header; //指向DexClassDataHeader，字段和方法个数
//        DexField*          staticFields; //静态字段
//        DexField*          instanceFields; //实例字段
//        DexMethod*         directMethods; //直接方法
//        DexMethod*         virtualMethods;//虚方法
//    };
//    struct DexClassDataHeader {
//        u4 staticFieldsSize; //静态字段个数
//        u4 instanceFieldsSize;//实例字段个数
//        u4 directMethodsSize;//直接方法个数
//        u4 virtualMethodsSize;//虚方法个数
//    };
//    struct DexField {
//        u4 fieldIdx;    /* 指向DexFieldId列表的索引 */
//        u4 accessFlags; //访问标志
//    };
//    struct DexMethod {
//        u4 methodIdx;    /* 指向DexMethodId列表的索引 */
//        u4 accessFlags;
//        u4 codeOff;      /* 指向DexCode结构的偏移 */ -----
//    };                                                   │
    private void reviseClassDataItem(int clsDataOff, int affectedOffset, int delta) {
        int nums[] = new int[4];
        int headerSize = parseLEB(clsDataOff, nums);
        int fieldsSize = nums[0] + nums[1];
        int methodsSize = nums[2] + nums[3];

        int skipSize = 0;
        if (fieldsSize > 0) {
            skipSize = skipLEB(clsDataOff + headerSize, fieldsSize * 2);
        }

        int codeOff[] = new int[1];
        int startOff = clsDataOff + headerSize + skipSize;
        for (int i = 0; i < methodsSize; ++i) {
            int methodHeaderSize = skipLEB(startOff, 2);
            startOff += methodHeaderSize;
            int codeOffSize = parseLEB(startOff, codeOff);
            startOff += codeOffSize;

            // Revise codeOff
            if (codeOff[0] >= affectedOffset) {
                byte[] newContent = ResStringChunk.getLEB128(codeOff[0] + delta);
                if (newContent.length == codeOffSize) {
                    for (int k = 0; k < codeOffSize; ++k) {
                        dexBuf[startOff - codeOffSize + k] = newContent[k];
                    }
                }
                // Record the changes to patch
                else {
                    //Log.d("DEBUG", "******** PatchBuffer is created! ********");
                    patchBuffers.add(new PatchBuffer(
                            startOff - codeOffSize, codeOffSize, newContent));
                }
            }

            // Revise debug info offset inside code
            reviseDexCode(codeOff[0], affectedOffset, delta);
        }
    }

    //    struct DexCode {                                 <----
//        u2  registersSize;//使用寄存器个数
//        u2  insSize;//参数个数
//        u2  outsSize;//调用其他方法时使用的寄存器个数
//        u2  triesSize;//try/catch个数
//        u4  debugInfoOff;//指向调试信息的偏移
//        u4  insnsSize;//指令集个数，以2字节为单位
//        u2  insns[1];//指令集
//    /* followed by optional u2 padding */
//    /* followed by try_item[triesSize] */
//    /* followed by uleb handlersSize */
//    /* followed by catch_handler_item[handlersSize] */
//    };
    private void reviseDexCode(int codeOff, int affectedOffset, int delta) {
        reviseIntValue(codeOff + 8, affectedOffset, delta);
    }

    private int parseLEB(int startOff, int[] values) {
        int curOff = startOff;
        for (int i = 0; i < values.length; ++i) {
            int v = 0, shift = 0;
            while ((dexBuf[curOff] & 0x80) != 0) {
                v |= ((dexBuf[curOff] & 0x7f) << shift);
                shift += 7;
                curOff += 1;
            }
            v |= ((int) dexBuf[curOff] << shift);
            curOff += 1;
            values[i] = v;
        }
        return curOff - startOff;
    }

    // bypass N LEB numbers
    private int skipLEB(int startOff, int N) {
        int curOff = startOff;
        for (int i = 0; i < N; ++i) {
            while ((dexBuf[curOff] & 0x80) != 0) {
                curOff += 1;
            }
            curOff += 1;
        }
        return curOff - startOff;
    }

    // Return the L format, com.gmail.apkeditor --> Lcom/gmail/apkeditor
    private String getLFormat(String oldName) {
        //return "L" + oldName.replace('.', '/') + "/";
        return "L" + oldName.replace('.', '/');
    }

    // Replace strings, and save modified dex to targetFilePath
    public void replaceDexString(Map<String, String> replaces,
                                 String targetFilePath) throws Exception {

        // bitmap[x]=1 means one string to be replaced has the length of x
        int replacedStrMaxLen = 0;
        byte[] bitmap = new byte[256];
        for (int i = 0; i < bitmap.length; i++) {
            bitmap[i] = 0;
        }
        for (Entry<String, String> entry : replaces.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.length() != value.length()) {
                throw new Exception("Different class name length not supported.");
            }
            if (key.length() >= bitmap.length) {
                byte[] newBitmap = new byte[key.length() + 1];
                System.arraycopy(bitmap, 0, newBitmap, 0, bitmap.length);
                bitmap = newBitmap;
            }
            // Set the bitmap
            bitmap[key.length()] = 1;
            // Refresh the max length
            if (key.length() > replacedStrMaxLen) {
                replacedStrMaxLen = key.length();
            }
        }

        // To find string start with 'L'
        DexParser parser = new DexParser(dexBuf);
        int index = 0;
        for (; index < parser.getStringCount(); index++) {
            int itemOffset = parser.getStringIdItemOffset(index);
            int strOffset = (dexBuf[itemOffset] & 0xff)
                    | ((dexBuf[itemOffset + 1] & 0xff) << 8)
                    | ((dexBuf[itemOffset + 2] & 0xff) << 16)
                    | ((dexBuf[itemOffset + 3]) << 24);
            DexReader reader = new DexReader(dexBuf, strOffset);
            reader.readSmallUleb128();
            // Find the class name string starting with 'L'
            if (dexBuf[reader.getOffset()] == 'L') {
                break;
            }
        }

        // Iterate the string starting with 'L'
        String previousStr = "";
        for (; index < parser.getStringCount(); index++) {
            int itemOffset = parser.getStringIdItemOffset(index);
            int strOffset = (dexBuf[itemOffset] & 0xff)
                    | ((dexBuf[itemOffset + 1] & 0xff) << 8)
                    | ((dexBuf[itemOffset + 2] & 0xff) << 16)
                    | ((dexBuf[itemOffset + 3]) << 24);
            DexReader reader = new DexReader(dexBuf, strOffset);
            int strLen = reader.readSmallUleb128();
            // The string is not starting with 'L' any more
            if (dexBuf[reader.getOffset()] != 'L') {
                break;
            }

            int contentOffset = reader.getOffset();
            String strVal = reader.readString(strLen);
            String factoredName = getRefactorName(strVal, replaces);
            if (factoredName != null) {
                doReplace(dexBuf, contentOffset, factoredName);
            }

            // Check if break the string order
            String curStr = (factoredName != null ? factoredName : strVal);
            if (curStr.compareTo(previousStr) <= 0) {
                Log.e("ERROR", "Order break: " + curStr + ", " + previousStr);
            }

            // Update previous string
            previousStr = curStr;

//            if (strLen <= replacedStrMaxLen && bitmap[strLen] == 1) {
//
//                if (replaces.containsKey(strVal)) {
//                    Log.d("DEBUG", strVal + ", previous: " + getString(parser, index - 1));
//                    String replaceStr = replaces.get(strVal);
//                    doReplace(dexBuf, contentOffset, replaceStr);
//                    Log.d("DEBUG", strVal + ", next: " + getString(parser, index + 1));
//                }
//            }
        }

        // Revise the hash and checksum
        reviseHash(dexBuf);
        reviseCrc32(dexBuf);

        saveToFile(dexBuf, targetFilePath);
    }

    // Do we need to refactor the class name?
    // If so, return the refactored name; otherwise return null
    private String getRefactorName(String clsName, Map<String, String> replaces) {
        for (Map.Entry<String, String> entry : replaces.entrySet()) {
            String key = entry.getKey();
            if (clsName.startsWith(key)) {
                return entry.getValue() + clsName.substring(key.length());
            }
        }
        return null;
    }

    private String getString(DexParser parser, int index) throws MyException {
        if (index < parser.getStringCount()) {
            int itemOffset = parser.getStringIdItemOffset(index);
            int strOffset = (dexBuf[itemOffset] & 0xff)
                    | ((dexBuf[itemOffset + 1] & 0xff) << 8)
                    | ((dexBuf[itemOffset + 2] & 0xff) << 16)
                    | ((dexBuf[itemOffset + 3]) << 24);
            DexReader reader = new DexReader(dexBuf, strOffset);
            int strLen = reader.readSmallUleb128();
            String strVal = reader.readString(strLen);
            return strVal;
        }
        return null;
    }

    class PatchBuffer {
        int offset;
        int size;
        byte[] newContent;

        public PatchBuffer(int offset, int size, byte[] newContent) {
            this.offset = offset;
            this.size = size;
            this.newContent = newContent;
        }
    }

//    public static void main(String[] args) throws Exception {
//        DexStringEditor editor = new DexStringEditor("D:\\Temp\\BBM\\BBM1.apk");
//        Map<String, String> replaces = new HashMap<>();
//        replaces.put("Lcom/BBM1/ui/activities/FilePickerActivity;",
//                "Lcom/BBM1/ui/activities/FilePickerActivitz;");
//        editor.replaceDexString(replaces, "D:\\Temp\\BBM\\BBM1\\classes2.dex");
//    }
}
