package com.gmail.heagoo.apkeditor.ce.e;

import com.gmail.heagoo.apkeditor.ce.e.ResourceEditor.ResChunkHeader;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import brut.androlib.res.decoder.StringBlock;

// Edit the string pool in resources.arsc
public class StringBlockEditor {

    private static final CharsetDecoder UTF16LE_DECODER = Charset.forName(
            "UTF-16LE").newDecoder();
    private static final CharsetDecoder UTF8_DECODER = Charset.forName("UTF-8")
            .newDecoder();

    private ResChunkHeader strPoolHeader;
    private byte[] strPoolData;

    private int chunkSize;
    private int stringCount;
    private int styleCount;
    private int flags;
    private int stringsOffset;
    private int stylesOffset;

    private boolean isUTF8;
    private int[] stringOffsets;
    private int[] styleOffsets;

    // When string is modified, delta size for this block
    private int totalDeltaSize = 0;

    public StringBlockEditor(ResChunkHeader strPoolHeader, byte[] strPoolData) {
        this.strPoolHeader = strPoolHeader;
        this.strPoolData = strPoolData;

        int dataOffset = 0;
        this.chunkSize = strPoolHeader.chunkSize;
        this.stringCount = getInt(strPoolData, dataOffset);
        dataOffset += 4;
        this.styleCount = getInt(strPoolData, dataOffset);
        dataOffset += 4;
        this.flags = getInt(strPoolData, dataOffset);
        dataOffset += 4;
        this.stringsOffset = getInt(strPoolData, dataOffset);
        dataOffset += 4;
        this.stylesOffset = getInt(strPoolData, dataOffset);
        dataOffset += 4;

        // Read string offsets array and style offsets array
        this.isUTF8 = (flags & 256) != 0;
//        Log.d("DEBUG", "isUTF8=" + this.isUTF8);
        this.stringOffsets = new int[stringCount];
        for (int i = 0; i < stringCount; i++) {
            this.stringOffsets[i] = getInt(strPoolData, dataOffset);
            dataOffset += 4;
        }
        if (styleCount != 0) {
            this.styleOffsets = new int[styleCount];
            for (int i = 0; i < styleCount; i++) {
                this.styleOffsets[i] = getInt(strPoolData, dataOffset);
                dataOffset += 4;
            }
        }
    }

    public final short getShort(byte[] work, int offset) {
        return (short) ((work[offset + 1] & 0xff) << 8 | (work[offset] & 0xff));
    }

    public final int getInt(byte[] work, int offset) {
        return (work[offset + 3]) << 24 | (work[offset + 2] & 0xff) << 16
                | (work[offset + 1] & 0xff) << 8 | (work[offset] & 0xff);
    }

    public final void setInt(byte[] buffer, int offset, int v) {
        buffer[offset] = (byte) (v & 0xff);
        buffer[offset + 1] = (byte) ((v >> 8) & 0xff);
        buffer[offset + 2] = (byte) ((v >> 16) & 0xff);
        buffer[offset + 3] = (byte) ((v >> 24) & 0xff);
    }

    public final void setShort(byte[] buffer, int offset, int v) {
        buffer[offset] = (byte) (v & 0xff);
        buffer[offset + 1] = (byte) ((v >> 8) & 0xff);
    }

    private String decodeString(int offset, int length) {
        try {
            return (isUTF8 ? UTF8_DECODER : UTF16LE_DECODER).decode(
                    ByteBuffer.wrap(strPoolData, offset, length)).toString();
        } catch (Exception ex) {
            // LOGGER.log(Level.WARNING, null, ex);
            return null;
        }
    }

    /*
     * For each string item, if it is UTF-8, it contains: 1) the length of the
     * string in characters 2) the length of the UTF-8 encoding of the string in
     * bytes 3) the UTF-8 encoded string 4) a trailing 8-bit zero
     *
     * If it is UTF-16, it contains: 1) the length of the string in characters
     * 2) the 16-bit characters 3) a trailing 16-bit zero
     */
    private boolean replaceStringIfEqualOrStarts(int index, String oldStr, String newStr,
                                                 String oldStarts, String newStarts) {
        if (index < 0) {
            return false;
        }

        int lenLength; // how many bytes for the length fields
        int offset = stringOffsets[index];
        int length; // length of the encoded string content

        if (isUTF8) {
            // strPoolData = chunk_start_addr + 8
            // actual addr = chunk_start_addr + (stringsOffset + offset)
            int startAt = stringsOffset + offset - 8;
            int[] val = StringBlock.getUtf8(strPoolData, startAt);
            offset = val[0];
            lenLength = offset - startAt;
            length = val[1];
        } else {
            int[] val = StringBlock.getUtf16(strPoolData, stringsOffset
                    + offset - 8);
            lenLength = val[0];
            offset = stringsOffset + offset - 8 + lenLength;
            length = val[1];
        }

        String value = decodeString(offset, length);
        if (value != null) {
            String newValue = null;
            if (value.equals(oldStr) && newStr != null) {
                newValue = newStr;
            } else if (value.startsWith(oldStarts) && newStarts != null) {
                newValue = newStarts + value.substring(oldStarts.length());
            }

            if (newValue != null) {
                //Log.e("DEBUG", "StringBlockEditor: " + value + " -> " + newValue);
                int oldTotalLen = lenLength + length + (isUTF8 ? 1 : 2);
                byte[] newString = this.isUTF8 ? encodeString8(newValue)
                        : encodeString16(newValue);
                reviseBuffer(index, offset - lenLength, oldTotalLen, newString);
                return true;
            }
        }

        return false;
    }

    // Revise the buffer: stringOffsets and styleOffsets
    // The bytes between [offset, offset+length) will be replaced by newString
    // All offsets after index will be revised
    private void reviseBuffer(int index, int offset, int length,
                              byte[] newString) {
//        Log.d("DEBUG",
//                String.format(
//                        "index=%d, startsStart=%d, stringCount=%d, offset=%d, length=%d, stylesOffset=%d, styleCount=%d, newString.length=%d",
//                        index, stringsOffset, stringCount, offset, length,
//                        stylesOffset, styleCount, newString.length));
        this.chunkSize += newString.length - length;
        int paddingSize = 0;
        if ((this.chunkSize % 4) != 0) {
            paddingSize = 4 - chunkSize % 4;
            this.chunkSize += paddingSize;
        }
//        Log.d("DEBUG", "padding size=" + paddingSize);

        int deltaSize = newString.length - length;

        // Revise the chunk size
        this.strPoolHeader.setChunkSize(this.chunkSize);

        // Revise the style start offset
        if (this.stylesOffset > 0 && this.styleCount > 0) {
            this.stylesOffset += deltaSize + paddingSize;
            setInt(strPoolData, 16, this.stylesOffset);
        }

        // Revise the offsets value
        int dataOffset = 20 + (index + 1) * 4;
        for (int i = index + 1; i < this.stringCount; i++) {
            this.stringOffsets[i] += deltaSize;
            setInt(strPoolData, dataOffset, this.stringOffsets[i]);
            dataOffset += 4;
        }

        // Revise the string content
        byte[] newData = new byte[strPoolData.length + deltaSize + paddingSize];
        System.arraycopy(strPoolData, 0, newData, 0, offset); // data before
        // offset
        System.arraycopy(newString, 0, newData, offset, newString.length); // replace
        // data
        // [offset,
        // offset+length)
        // Note: the zero padding is added after all the strings, before the
        // style data
        if (this.stylesOffset > 0) {
            // Copy remain strings (note: stylesOffset is already changed)
            int oldStylesOffset = stylesOffset - deltaSize - paddingSize;
            int stringEndPos = oldStylesOffset - 8;
//            Log.d("DEBUG", "stringEndPos=" + stringEndPos);
//            Log.d("DEBUG", String.format("Copy from %d, len %d", offset + length, stringEndPos - offset - length));
            System.arraycopy(strPoolData, offset + length, newData, offset
                    + newString.length, stringEndPos - offset - length);
            // Copy style data
            int dstOffset = stringEndPos + deltaSize + paddingSize;
//            Log.d("DEBUG", String.format("Copy from %d, len %d", stringEndPos, strPoolData.length - stringEndPos));
            System.arraycopy(strPoolData, stringEndPos, newData, dstOffset,
                    strPoolData.length - stringEndPos);
        } else {
            System.arraycopy(strPoolData, offset + length, newData, offset
                    + newString.length, strPoolData.length - offset - length);
        }

        // Replace the data
        this.strPoolData = newData;

        this.totalDeltaSize += deltaSize + paddingSize;
    }

    /*
     * At stringsStart are all of the UTF-16 strings concatenated together; each
     * starts with a uint16_t of the string's length and each ends with a 0x0000
     * terminator. If a string is > 32767 characters, the high bit of the length
     * is set meaning to take those 15 bits as a high word and it will be
     * followed by another uint16_t containing the low word.
     */
    private byte[] encodeString16(String newStr) {
        try {
            String codeName = "UTF-16LE";
            byte[] data = newStr.getBytes(codeName);
            while (data.length > 32767) {
                newStr = newStr.substring(0, newStr.length() - 1);
                data = newStr.getBytes(codeName);
            }

            int characters = newStr.length();
            byte[] result = new byte[4 + data.length];
            setShort(result, 0, characters);
            System.arraycopy(data, 0, result, 2, data.length);
            result[2 + data.length] = 0;
            result[3 + data.length] = 0;

            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private byte[] encodeString8(String newStr) {
        try {
            String codeName = "UTF-8";
            byte[] data = newStr.getBytes(codeName);
            while (data.length >= 128) {
                newStr = newStr.substring(0, newStr.length() - 1);
                data = newStr.getBytes(codeName);
            }

            int characters = newStr.length();
            int utf8Length = data.length;
            byte[] result = new byte[3 + utf8Length];
            result[0] = (byte) characters;
            result[1] = (byte) utf8Length;
            System.arraycopy(data, 0, result, 2, utf8Length);
            result[2 + utf8Length] = 0;

            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean replaceString(String oldStr, String newStr,
                                 String oldStarts, String newStarts) {
        boolean replaced = false;
        for (int i = 0; i < this.stringCount; i++) {
            if (replaceStringIfEqualOrStarts(i, oldStr, newStr, oldStarts, newStarts)) {
                replaced = true;
            }
        }

        return replaced;
    }

    public int getDeltaSize() {
        return this.totalDeltaSize;
    }

    // Save all the contents of the string pool
    public void writeTo(OutputStream output) throws IOException {
        strPoolHeader.writeTo(output);
        output.write(strPoolData);
    }
}
