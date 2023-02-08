package com.gmail.heagoo.apkeditor.ce.e;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;

public class ResStringChunk {
    private static final int UTF8_FLAG = 0x00000100;
    private static final CharsetDecoder UTF16LE_DECODER = Charset.forName(
            "UTF-16LE").newDecoder();
    private static final CharsetDecoder UTF8_DECODER = Charset.forName("UTF-8")
            .newDecoder();
    public int chunkSize;
    byte[] resStrTableHdr = new byte[28];
    int chunkTag;
    int stringCount;
    int styleCount;
    int unknown1;
    int stringOffset;
    int styleOffset;
    int flags;
    // int[] stringOffsetArray;
    Object[] strBuffers;
    String[] stringValues;
    // For styles
    int[] styleOffsetArray = null;
    int[] styleDataArray = null;
    private boolean isUTF8;
    private List<Integer> addedStrPositions = new ArrayList<>();

    public static byte[] getLEB128(int val) {
        // b11111111111111
        if (val > 0x3fff) {
            byte b1 = (byte) ((val & 0x7f) | 0x80);
            byte b2 = (byte) ((val >> 7) & 0x7f | 0x80);
            byte b3 = (byte) (val >> 14);
            return new byte[]{b1, b2, b3};
        } else if (val >= 0x80) {
            byte b1 = (byte) ((val & 0x7f) | 0x80);
            byte b2 = (byte) (val >> 7);
            return new byte[]{b1, b2};
        } else {
            return new byte[]{(byte) val};
        }
    }

    public static final int[] getUtf8(byte[] array, int offset) {
        int val = array[offset];
        int length;

        if ((val & 0x80) != 0) {
            offset += 2;
        } else {
            offset += 1;
        }
        val = array[offset];
        if ((val & 0x80) != 0) {
            offset += 2;
        } else {
            offset += 1;
        }
        length = 0;
        while (array[offset + length] != 0) {
            length++;
        }
        return new int[]{offset, length};
    }

    public static final int[] getUtf16(byte[] array, int offset) {
        int val = ((array[offset + 1] & 0xFF) << 8 | array[offset] & 0xFF);

        if (val == 0x8000) {
            int high = (array[offset + 3] & 0xFF) << 8;
            int low = (array[offset + 2] & 0xFF);
            return new int[]{4, (high + low) * 2};
        }
        return new int[]{2, val * 2};
    }

    public List<Integer> getAddedStrPositions() {
        return addedStrPositions;
    }

    public int[] getStringIndexMapping() {
        int added = addedStrPositions.size();
        int afterCount = getStringCount();
        int beforeCount = afterCount - added;

        // mapping[oldIndex] = newIndex
        int[] mapping = new int[beforeCount];
        int delta = 0;
        for (int i = 0; i < beforeCount; ++i) {
            if (addedStrPositions.contains(i)) {
                delta += 1;
            }
            mapping[i] = i + delta;
        }

        return mapping;
    }

    public void parse(MyInputStream is) throws IOException {

        // Resource String Table
        // 4 bytes (Chunk Type: 01 00 1c 00)
        // 4 bytes (Chunk Size, total size of Resource String Table)
        // 4 bytes (# of String)
        // 4 bytes (Style offset count?)
        // 4 bytes (unknown)
        // 4 bytes (String offset)
        // 4 bytes (Style offset)
        is.readFully(resStrTableHdr);
        chunkTag = ManifestEditorNew.getInt(resStrTableHdr, 0);
        chunkSize = ManifestEditorNew.getInt(resStrTableHdr, 4);
        stringCount = ManifestEditorNew.getInt(resStrTableHdr, 8);
        styleCount = ManifestEditorNew.getInt(resStrTableHdr, 12);
        flags = ManifestEditorNew.getInt(resStrTableHdr, 16);
        stringOffset = ManifestEditorNew.getInt(resStrTableHdr, 20);
        styleOffset = ManifestEditorNew.getInt(resStrTableHdr, 24);

        this.isUTF8 = (flags & UTF8_FLAG) != 0;

        // String index array and string values
        int[] stringOffsetArray = new int[stringCount];
        stringValues = new String[stringCount];
        is.readIntArray(stringOffsetArray);

        // Read style offsets
        if (styleCount != 0) {
            this.styleOffsetArray = new int[styleCount];
            is.readIntArray(styleOffsetArray);
        }

        // how many padding before string value, generally should be 0
        int strValuePaddings = stringOffset
                - (28 + 4 * stringCount + 4 * styleCount);
        ManifestEditorNew.log("strValuePaddings=%d", strValuePaddings);
        if (strValuePaddings > 0) {
            is.skip(strValuePaddings);
        }

        // Read strings
        // int strBufSize = chunkSize - 28 - 4 * stringCount - strValuePaddings;
        int strBufSize = ((styleOffset == 0) ? chunkSize : styleOffset)
                - stringOffset;
        byte[] allStrBuf = new byte[strBufSize];
        is.readFully(allStrBuf);

        // Read style data
        if (styleOffset > 0) {
            int size = (chunkSize - styleOffset);
            this.styleDataArray = new int[size / 4];
            is.readIntArray(styleDataArray);

            // read remaining bytes
            int remaining = size % 4;
            if (remaining >= 1) {
                while (remaining-- > 0) {
                    is.readByte();
                }
            }
        }

        // Parse the string
        strBuffers = new Object[stringOffsetArray.length];
        parseString(allStrBuf, stringOffsetArray, strBuffers, stringValues);
    }

    // Decode the string, as well as save the raw bytes to strBuffers
    public String getString(byte[] buffer, int offset, Object[] strBuffers, int idx) {
        int length;

        if (isUTF8) {
            int[] val = getUtf8(buffer, offset);
            int lenLength = val[0] - offset;
            offset = val[0];
            length = val[1];
            // The raw buffer contains the length, and the tail zero
            strBuffers[idx] = new byte[length + lenLength + 1];
            System.arraycopy(buffer, offset - lenLength, strBuffers[idx], 0, length + lenLength);
        } else {
            int[] val = getUtf16(buffer, offset);
            offset += val[0];
            length = val[1];
            // The raw buffer does not contain length
            strBuffers[idx] = new byte[length];
            System.arraycopy(buffer, offset, strBuffers[idx], 0, length);
        }

        return decodeString(buffer, offset, length);
    }

    private String decodeString(byte[] buffer, int offset, int length) {
        try {
            return (isUTF8 ? UTF8_DECODER : UTF16LE_DECODER).decode(
                    ByteBuffer.wrap(buffer, offset, length)).toString();
        } catch (Exception ex) {
            // LOGGER.log(Level.WARNING, null, ex);
            return null;
        }
    }

    private void parseString(byte[] buffer, int[] stringIdxOffset,
                             Object[] strBuffers, String[] stringValues) throws IOException {
        for (int i = 0; i < stringIdxOffset.length; i++) {
            int curOffset = stringIdxOffset[i];
            stringValues[i] = getString(buffer, curOffset, strBuffers, i);
            ManifestEditorNew.log("%d:\t%s", i, stringValues[i]);
        }
    }

    // Add a string to the string table
    // position is the index of added string
    // NOTE: long string (> 32767) is not supported
    // When call this function, please first add the larger position
    public void addString(String addedStr, int position) {
        ManifestEditorNew
                .log("StringChunk.addString: " + addedStr + ", " + position);
        int addedSize = 4 + (addedStr.length() + 2) * 2;

        this.chunkSize += addedSize;
        this.stringCount += 1;
        this.stringOffset += 4;
        ManifestEditorNew.setInt(resStrTableHdr, 4, chunkSize);
        ManifestEditorNew.setInt(resStrTableHdr, 8, stringCount);
        ManifestEditorNew.setInt(resStrTableHdr, 20, stringOffset);

        // Update string values
        String[] newStringValues = new String[stringCount];
        Object[] newStringBufs = new Object[stringCount];
        int index = 0;
        for (int i = 0; i < stringCount; i++) {
            if (i != position) {
                newStringValues[i] = this.stringValues[index];
                newStringBufs[i] = this.strBuffers[index];
                index++;
            } else {
                newStringValues[i] = addedStr;
                newStringBufs[i] = getStringBytes(addedStr);
            }
        }
        this.stringValues = newStringValues;
        this.strBuffers = newStringBufs;

        // Record the position
        this.addedStrPositions.add(0, position);
    }

    private byte[] getStringBytes_utf8(String _str) {
        byte[] strBuffer = null;

        try {
            byte[] coded = _str.getBytes("UTF-8");
            int len1 = _str.length();
            int len2 = coded.length;
            byte[] data1 = getLEB128(len1);
            byte[] data2 = getLEB128(len2);
            strBuffer = new byte[data1.length + data2.length + coded.length + 1];
            System.arraycopy(data1, 0, strBuffer, 0, data1.length);
            System.arraycopy(data2, 0, strBuffer, data1.length, data2.length);
            System.arraycopy(coded, 0, strBuffer, data1.length + data2.length, coded.length);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return strBuffer;
    }

    // private void updateOffsetAndBuffer(String addedStr, int position) {
    // int oldBufferOffset = 0;
    // int newBufferOffset = 0;
    // int[] newStrOffsetArray = new int[stringCount];
    // // make it a bit larger
    // byte[] newStrBuffer = new byte[strBuffer.length
    // + (addedStr.length() + 2) * 4];
    //
    // for (int i = 0; i < stringCount; i++) {
    // newStrOffsetArray[i] = newBufferOffset;
    // if (i != position) {
    // int oneStrLen = getStrTotalLength(strBuffer, oldBufferOffset);
    // // ManifestEditorNew.log("String Len: %d", oneStrLen);
    // System.arraycopy(strBuffer, oldBufferOffset, newStrBuffer,
    // newBufferOffset, oneStrLen);
    // oldBufferOffset += oneStrLen;
    // newBufferOffset += oneStrLen;
    // } else {
    // try {
    // byte[] data = addedStr.getBytes("utf-16");
    // int offset = 0;
    // int length = data.length;
    // if (data.length >= 2 && ((data[0] & 0xff) == 255)
    // && ((data[1] & 0xff) == 254)) {
    // offset = 2;
    // length -= 2;
    // }
    // ManifestEditorNew.setShort(newStrBuffer, newBufferOffset,
    // length / 2);
    // newBufferOffset += 2;
    //
    // // ManifestEditorNew.log("%s, string length: %d", addedStr,
    // // data.length);
    // System.arraycopy(data, offset, newStrBuffer, newBufferOffset,
    // length);
    // newBufferOffset += length;
    // } catch (UnsupportedEncodingException e) {
    // e.printStackTrace();
    // }
    // ManifestEditorNew.setShort(newStrBuffer, newBufferOffset, 0);
    // newBufferOffset += 2;
    //
    // if (newBufferOffset % 2 != 0) {
    // ManifestEditorNew.setByte(newStrBuffer, newBufferOffset, 0);
    // newBufferOffset += 1;
    // }
    // }
    // }
    //
    // stringOffsetArray = newStrOffsetArray;
    // strBuffer = newStrBuffer;
    // }

    private byte[] getStringBytes_utf16(String _str) {
        byte[] strBuffer = null;
        try {
            byte[] data = _str.getBytes("utf-16");
            int offset = 0;
            int length = data.length;
            if (data.length >= 2 && ((data[0] & 0xff) == 255)
                    && ((data[1] & 0xff) == 254)) {
                offset = 2;
                length -= 2;
            }

            strBuffer = new byte[length];
            System.arraycopy(data, offset, strBuffer, 0, length);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return strBuffer;
    }

    // public void modifyString(int strIndex, String newVal) {
    // ManifestEditorNew
    // .log("StringChunk.modifyString(%s, %d)", newVal, strIndex);
    //
    // int oldBufferOffset = 0;
    // int newBufferOffset = 0;
    // int[] newStrOffsetArray = new int[stringCount];
    // // The buffer is larger than needed
    // byte[] newStrBuffer = new byte[strBuffer.length + (newVal.length() + 2)
    // * 4];
    //
    // ManifestEditorNew.log("old buffer length: %d", strBuffer.length);
    //
    // for (int i = 0; i < stringCount; i++) {
    // newStrOffsetArray[i] = newBufferOffset;
    //
    // int oneStrLen = getStrTotalLength(strBuffer, oldBufferOffset);
    // //
    // ManifestEditorNew.log("%d, String Len: %d, old offset: %d, new offset: %d",
    // // i, oneStrLen, oldBufferOffset, newBufferOffset);
    //
    // if (i != strIndex) {
    // System.arraycopy(strBuffer, oldBufferOffset, newStrBuffer,
    // newBufferOffset, oneStrLen);
    // newBufferOffset += oneStrLen;
    // } else {
    // try {
    // byte[] data = newVal.getBytes("utf-16");
    // int offset = 0;
    // int length = data.length;
    // if (data.length >= 2 && ((data[0] & 0xff) == 255)
    // && ((data[1] & 0xff) == 254)) {
    // offset = 2;
    // length -= 2;
    // }
    // ManifestEditorNew.setShort(newStrBuffer, newBufferOffset,
    // length / 2);
    // newBufferOffset += 2;
    //
    // System.arraycopy(data, offset, newStrBuffer,
    // newBufferOffset, length);
    // newBufferOffset += length;
    // } catch (UnsupportedEncodingException e) {
    // e.printStackTrace();
    // }
    // // for (int ii = 0; ii < newVal.length(); ii++) {
    // // newStrBuffer[newBufferOffset + ii * 2] = (byte) newVal
    // // .charAt(ii);
    // // newStrBuffer[newBufferOffset + ii * 2 + 1] = 0;
    // // }
    //
    // ManifestEditorNew.setShort(newStrBuffer, newBufferOffset, 0);
    // newBufferOffset += 2;
    //
    // if (newBufferOffset % 2 != 0) {
    // ManifestEditorNew.setByte(newStrBuffer, newBufferOffset, 0);
    // newBufferOffset += 1;
    // }
    // }
    //
    // // Always updated original buffer
    // oldBufferOffset += oneStrLen;
    // }
    //
    // stringOffsetArray = newStrOffsetArray;
    // strBuffer = newStrBuffer;
    // }

    private byte[] getStringBytes(String _str) {
        if (isUTF8) {
            return getStringBytes_utf8(_str);
        } else {
            return getStringBytes_utf16(_str);
        }
    }

    public void modifyString(int strIndex, String newVal) {
        byte[] strBuffer = getStringBytes(newVal);
        this.stringValues[strIndex] = newVal;
        this.strBuffers[strIndex] = strBuffer;
    }

    // At stringsStart are all of the UTF-16 strings concatenated together; each
    // starts with a uint16_t of the string's length and each ends with a 0x0000
    // terminator. If a string is > 32767 characters, the high bit of the length
    // is set meaning to take those 15 bits as a high word and it will be
    // followed by another uint16_t containing the low word.
    private int getStrTotalLength(byte[] buf, int offset) {
        int sectionLen = 2;
        int len = ManifestEditorNew.getShort(buf, offset);
        if ((len & 0x8000) != 0) {
            int low = ManifestEditorNew.getShort(buf, offset + 2);
            len = (((len & 0x7fff) << 16) | low);
            sectionLen += 2;
        }
        sectionLen += len * 2 + 2; // end with 0x0000
        return sectionLen;
    }

    private byte[] getLengthEncoding(int val) {
        byte[] result = null;
        if (val > 32767) {
            result = new byte[4];
            int low = val & 0x7fff;
            int high = (val >> 15) & 0x7fff;
            ManifestEditorNew.setShort(result, 0, high);
            ManifestEditorNew.setShort(result, 2, low);
        } else {
            result = new byte[2];
            ManifestEditorNew.setShort(result, 0, val);
        }

        return result;
    }

    // Get encoding length for string size
    private int getEncodeLength(int val) {
        if (val <= 32767) {
            return 2;
        }

        // TODO: just a hack, 4 should be enough
        return 4;
    }

    // Dump to the output
    public void dump(MyFileOutput out) throws IOException {
        // Recompute chunk size
        int[] stringOffsetArray = new int[this.stringCount];
        int strValueLen = 0;
        for (int i = 0; i < this.stringCount; i++) {
            stringOffsetArray[i] = strValueLen;
            int len = ((byte[]) (this.strBuffers[i])).length;
            if (isUTF8) {
                strValueLen += len;
            } else {
                strValueLen += getEncodeLength(len / 2) + len + 2;
            }
        }

        //
        this.chunkSize = 28 + this.stringCount * 4
                + (this.styleCount > 0 ? styleCount * 4 : 0) + strValueLen
                + (this.styleDataArray != null ? styleDataArray.length : 0);
        int paddingSize = (4 - (chunkSize % 4)) % 4;
        this.chunkSize += paddingSize;
        ManifestEditorNew.setInt(resStrTableHdr, 4, chunkSize);

        if (this.styleOffset > 0) {
            this.styleOffset = chunkSize - styleDataArray.length;
            ManifestEditorNew.setInt(resStrTableHdr, 24, styleOffset);
        }

        out.writeBytes(resStrTableHdr);
        out.writeIntArray(stringOffsetArray);
        if (this.styleCount > 0) {
            out.writeIntArray(styleOffsetArray);
        }
        for (int i = 0; i < this.stringCount; i++) {
            byte[] data = (byte[]) (this.strBuffers[i]);
            if (isUTF8) {
                out.writeBytes(data, 0, data.length);
            } else {
                byte[] hdr = getLengthEncoding(data.length / 2);
                out.writeBytes(hdr);
                out.writeBytes(data, 0, data.length);
                out.writeShort(0);
            }
        }

        // Write padding data
        if (paddingSize > 0) {
            switch (paddingSize) {
                case 3:
                    out.writeBytes(new byte[]{0, 0, 0});
                    break;
                case 2:
                    out.writeShort(0);
                    break;
                case 1:
                    out.writeBytes(new byte[]{0});
                    break;
            }
        }

        if (styleDataArray != null) {
            out.writeIntArray(styleDataArray);
        }
    }

    public String getStringByIndex(int idx) {
        if (idx >= 0 && idx < stringValues.length) {
            return stringValues[idx];
        }
        return null;
    }

    public int getStringCount() {
        return stringValues.length;
    }

    public int getStringIndex(String value) {
        for (int i = 0; i < stringValues.length; ++i) {
            if (stringValues[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }

}
