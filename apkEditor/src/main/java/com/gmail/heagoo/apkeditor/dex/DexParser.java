package com.gmail.heagoo.apkeditor.dex;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DexParser {

    public static final int FILESIZE_OFFSET = 0x20;
    public static final int MAP_OFFSET = 0x34;

    public static final int STRING_COUNT_OFFSET = 56;
    public static final int STRING_START_OFFSET = 60;

    public static final int TYPE_COUNT_OFFSET = 64;
    public static final int TYPE_START_OFFSET = 68;

    public static final int PROTO_COUNT_OFFSET = 72;
    public static final int PROTO_START_OFFSET = 76;

    public static final int FIELD_COUNT_OFFSET = 80;
    public static final int FIELD_START_OFFSET = 84;

    public static final int METHOD_COUNT_OFFSET = 88;
    public static final int METHOD_START_OFFSET = 92;

    // Class start offset point to kDexTypeClassDefItem
    public static final int CLASS_COUNT_OFFSET = 96;
    public static final int CLASS_START_OFFSET = 100;

    public static final int DATA_COUNT_OFFSET = 104;
    public static final int DATA_START_OFFSET = 108;
    private final int mapOffset;
    private final int stringCount;
    private final int stringStartOffset;
    private final int typeCount;
    private final int typeStartOffset;
    private final int protoCount;
    private final int protoStartOffset;
    private final int fieldCount;
    private final int fieldStartOffset;
    private final int methodCount;
    private final int methodStartOffset;
    private final int classCount;
    private final int classStartOffset;
    private final int dataCount;
    private final int dataStartOffset;
    private byte[] buf;

    public DexParser(byte[] buf) throws MyException {
        this.buf = buf;

        mapOffset = readSmallUint(MAP_OFFSET);
        stringCount = readSmallUint(STRING_COUNT_OFFSET);
        stringStartOffset = readSmallUint(STRING_START_OFFSET);
        typeCount = readSmallUint(TYPE_COUNT_OFFSET);
        typeStartOffset = readSmallUint(TYPE_START_OFFSET);
        protoCount = readSmallUint(PROTO_COUNT_OFFSET);
        protoStartOffset = readSmallUint(PROTO_START_OFFSET);
        fieldCount = readSmallUint(FIELD_COUNT_OFFSET);
        fieldStartOffset = readSmallUint(FIELD_START_OFFSET);
        methodCount = readSmallUint(METHOD_COUNT_OFFSET);
        methodStartOffset = readSmallUint(METHOD_START_OFFSET);
        classCount = readSmallUint(CLASS_COUNT_OFFSET);
        classStartOffset = readSmallUint(CLASS_START_OFFSET);
        dataCount = readSmallUint(DATA_COUNT_OFFSET);
        dataStartOffset = readSmallUint(DATA_START_OFFSET);
    }

    public static int readInt(byte[] buf, int offset) {
        return (buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8)
                | ((buf[offset + 2] & 0xff) << 16) | (buf[offset + 3] << 24);
    }

    public static void writeInt(int val, byte[] buf, int offset) {
        buf[offset] = (byte) (val & 0xff);
        buf[offset + 1] = (byte) ((val >> 8) & 0xff);
        buf[offset + 2] = (byte) ((val >> 16) & 0xff);
        buf[offset + 3] = (byte) ((val >> 24) & 0xff);
    }

    public static short readShort(byte[] buf, int offset) {
        return (short) ((buf[offset] & 0xff) | (buf[offset + 1] << 8));
    }

    public static void writeShort(short val, byte[] buf, int offset) {
        buf[offset] = (byte) (val & 0xff);
        buf[offset + 1] = (byte) ((val >> 8) & 0xff);
    }

    public int readSmallUint(int offset) throws MyException {
        byte[] buf = this.buf;
        int result = (buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8)
                | ((buf[offset + 2] & 0xff) << 16) | ((buf[offset + 3]) << 24);
        if (result < 0) {
            throw new MyException("out of range when read int at offset 0x%x",
                    offset);
        }
        return result;
    }

    public int readUshort(int offset) {
        byte[] buf = this.buf;
        return (buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8);
    }

    public int readUbyte(int offset) {
        return buf[offset] & 0xff;
    }

    public long readLong(int offset) {
        byte[] buf = this.buf;
        return (buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8)
                | ((buf[offset + 2] & 0xff) << 16)
                | ((buf[offset + 3] & 0xffL) << 24)
                | ((buf[offset + 4] & 0xffL) << 32)
                | ((buf[offset + 5] & 0xffL) << 40)
                | ((buf[offset + 6] & 0xffL) << 48)
                | (((long) buf[offset + 7]) << 56);
    }

    public int readInt(int offset) {
        return readInt(this.buf, offset);
    }

    public int readShort(int offset) {
        return readShort(this.buf, offset);
    }

    public int readByte(int offset) {
        return buf[offset];
    }

    public int readOptionalUint(int offset) throws MyException {
        byte[] buf = this.buf;
        int result = (buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8)
                | ((buf[offset + 2] & 0xff) << 16) | ((buf[offset + 3]) << 24);
        if (result < -1) {
            throw new MyException(
                    "optional uint is out of range at offset 0x%x", offset);
        }
        return result;
    }

    public int getStringIdItemOffset(int stringIndex) throws MyException {
        if (stringIndex < 0 || stringIndex >= stringCount) {
            throw new MyException("String index out of bounds: %d", stringIndex);
        }
        // return stringStartOffset + stringIndex * StringIdItem.ITEM_SIZE;
        return stringStartOffset + stringIndex * 4;
    }

    public int getTypeIdItemOffset(int typeIndex) throws MyException {
        if (typeIndex < 0 || typeIndex >= typeCount) {
            throw new MyException("Type index out of bounds: %d", typeIndex);
        }
        return typeStartOffset + typeIndex * 4;
    }

    public int getFieldIdItemOffset(int fieldIndex) throws InvalidItemIndex {
        if (fieldIndex < 0 || fieldIndex >= fieldCount) {
            throw new InvalidItemIndex(fieldIndex,
                    "Field index out of bounds: %d", fieldIndex);
        }
        return fieldStartOffset + fieldIndex * 8;
        //return fieldStartOffset + fieldIndex * FieldIdItem.ITEM_SIZE;
    }

    public int getMethodIdItemOffset(int methodIndex) throws InvalidItemIndex {
        if (methodIndex < 0 || methodIndex >= methodCount) {
            throw new InvalidItemIndex(methodIndex,
                    "Method index out of bounds: %d", methodIndex);
        }
        return methodStartOffset + methodIndex * 8;
        //return methodStartOffset + methodIndex * MethodIdItem.ITEM_SIZE;
    }

    public int getProtoIdItemOffset(int protoIndex) throws InvalidItemIndex {
        if (protoIndex < 0 || protoIndex >= protoCount) {
            throw new InvalidItemIndex(protoIndex,
                    "Proto index out of bounds: %d", protoIndex);
        }
        return protoStartOffset + protoIndex * 12;
        //return protoStartOffset + protoIndex * ProtoIdItem.ITEM_SIZE;
    }

    public int getClassDefItemOffset(int classIndex) throws InvalidItemIndex {
        if (classIndex < 0 || classIndex >= classCount) {
            throw new InvalidItemIndex(classIndex,
                    "Class index out of bounds: %d", classIndex);
        }
        return classStartOffset + classIndex * 32;
        //return classStartOffset + classIndex * ClassDefItem.ITEM_SIZE;
    }

    public DexReader readerAt(int offset) {
        return new DexReader(this.buf, offset);
    }

    public int getClassCount() {
        return classCount;
    }

    public int getStringCount() {
        return stringCount;
    }

    public int getTypeCount() {
        return typeCount;
    }

    public int getProtoCount() {
        return protoCount;
    }

    public int getProtoStartOffset() {
        return protoStartOffset;
    }

    public int getClassStartOffset() {
        return classStartOffset;
    }

    public int getFieldCount() {
        return fieldCount;
    }

    public int getMethodCount() {
        return methodCount;
    }

    public String getString(int stringIndex) throws MyException {
        int stringOffset = getStringIdItemOffset(stringIndex);
        int stringDataOffset = readSmallUint(stringOffset);
        DexReader reader = readerAt(stringDataOffset);
        int utf16Length = reader.readSmallUleb128();
        return reader.readString(utf16Length);
    }

    public String getType(int typeIndex) throws MyException {
        int typeOffset = getTypeIdItemOffset(typeIndex);
        int stringIndex = readSmallUint(typeOffset);
        return getString(stringIndex);
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Get map list
    public List<DexMapItem> getMapItems() throws MyException {
        int items = readSmallUint(mapOffset);
        List<DexMapItem> result = new ArrayList<>(items);
        for (int i = 0; i < items; ++i) {
            result.add(DexMapItem.from(buf, mapOffset + 4 + i * 12));
        }
        Collections.sort(result, new Comparator<DexMapItem>() {
            @Override
            public int compare(DexMapItem o1, DexMapItem o2) {
                return o1.offset - o2.offset;
            }
        });
        return result;
    }

    public int getMapOffset() {
        return mapOffset;
    }

    // Revise the header offset
    // affectedOffset: addresses impacted if the offset >= startOffset
    public void reviseHeader(int affectedOffset, int delta) {
        if (delta != 0) {
            //Log.d("DEBUG", "delta=" + delta);

            // Revise the file size
            int fileSize = readInt(FILESIZE_OFFSET);
            writeInt(fileSize + delta, buf, FILESIZE_OFFSET);

            reviseSingleOffset(mapOffset, affectedOffset, delta, MAP_OFFSET);
            reviseSingleOffset(stringStartOffset, affectedOffset, delta, STRING_START_OFFSET);
            reviseSingleOffset(typeStartOffset, affectedOffset, delta, TYPE_START_OFFSET);
            reviseSingleOffset(protoStartOffset, affectedOffset, delta, PROTO_START_OFFSET);
            reviseSingleOffset(fieldStartOffset, affectedOffset, delta, FIELD_START_OFFSET);
            reviseSingleOffset(methodStartOffset, affectedOffset, delta, METHOD_START_OFFSET);
            reviseSingleOffset(classStartOffset, affectedOffset, delta, CLASS_START_OFFSET);

            // Either revise data start offset or data count
            if (!reviseSingleOffset(dataStartOffset, affectedOffset, delta, DATA_START_OFFSET)) {
                writeInt(dataCount + delta, buf, DATA_COUNT_OFFSET);
            }
        }
    }

    private boolean reviseSingleOffset(int originalVal, int affectedOffset, int delta, int offInBuf) {
        if (originalVal >= affectedOffset) {
            writeInt(originalVal + delta, buf, offInBuf);
            //Log.d("DEBUG", "old value: " + originalVal + ", new value: " + readInt(offInBuf));
            return true;
        }
        return false;
    }
}
