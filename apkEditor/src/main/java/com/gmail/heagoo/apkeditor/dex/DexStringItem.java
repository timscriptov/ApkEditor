package com.gmail.heagoo.apkeditor.dex;


import com.gmail.heagoo.apkeditor.ce.e.ResStringChunk;

public class DexStringItem {
    // Original index in DEX file
    int oldIndex;

    // length is included in the buffer
    byte[] codedBuf;
    int offset;
    int len;

    String value;

    public DexStringItem(int index, String value) {
        this.oldIndex = index;
        this.value = value;
    }

    public DexStringItem(int index, String value, byte[] buffer, int offset, int len) {
        this.oldIndex = index;
        this.value = value;
        this.codedBuf = buffer;
        this.offset = offset;
        this.len = len;
    }

    public int getEncodedLength() {
        if (codedBuf != null && len > 0) {
            return len;
        } else {
            encodeStringValue();
            return len;
        }
    }

    public int writeToBuffer(byte[] buffer, int _offset) {
        System.arraycopy(this.codedBuf, this.offset, buffer, _offset, this.len);
        return this.len;
    }

    // byte -> String
    public void decodeValue() {
        int[] ret = new int[1];
        DexReader reader = new DexReader(codedBuf, offset);
        int utf16Length = 0;
        try {
            utf16Length = reader.readSmallUleb128();
        } catch (MyException e) {
            e.printStackTrace();
        }
        value = Utf8Utils.utf8BytesWithUtf16LengthToString(
                codedBuf, reader.getOffset(), utf16Length, ret);
    }

    public int compare(DexStringItem item) {
        if (value == null) {
            decodeValue();
        }
        if (item.value == null) {
            item.decodeValue();
        }
        return value.compareTo(item.value);
    }

    public void setValue(String value) {
        this.value = value;
        encodeStringValue();
    }

    private void encodeStringValue() {
        byte[] header = ResStringChunk.getLEB128(value.length());
        byte[] buf = Utf8Utils.stringToUtf8Bytes(value);
        this.codedBuf = new byte[header.length + buf.length + 1];
        System.arraycopy(header, 0, codedBuf, 0, header.length);
        System.arraycopy(buf, 0, codedBuf, header.length, buf.length);
        this.offset = 0;
        this.len = header.length + buf.length + 1;
    }
}
