package com.gmail.heagoo.appdm.util;

import android.util.TypedValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

class MyInputStream {
    int curOffset = 0;
    InputStream is;

    public MyInputStream(InputStream is) {
        this.is = is;
    }

    public void readBytes(byte[] buf) throws IOException {
        int ret = is.read(buf);
        if (ret > 0) {
            curOffset += ret;
        }
    }

    public void readBytes(byte[] buf, int off, int len) throws IOException {
        int ret = is.read(buf, off, len);
        if (ret > 0) {
            curOffset += ret;
        }
    }

    public int readShort() throws IOException {
        int low = is.read();
        int high = is.read();
        curOffset += 2;
        return (low & 0xff) | ((high & 0xff) << 8);
    }

    public int readInt() throws IOException {
        byte[] arr = new byte[4];
        is.read(arr);
        curOffset += 4;
        return AddSharedUserId.getInt(arr, 0);
    }

    public void readIntArray(int[] arr) throws IOException {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = readInt();
        }
    }

    public void skip(int n) throws IOException {
        if (n <= 0) {
            return;
        }

        is.skip(n);
        curOffset += n;
        AddSharedUserId
                .log("########## skip detected (should not happen) ##########");
    }

    public int getPosition() {
        return curOffset;
    }
}

// Output wrapper
class MyFileOutput {

    private RandomAccessFile outFile;
    private int writeLength = 0;

    public MyFileOutput(RandomAccessFile outFile) {
        this.outFile = outFile;
    }

    public int getWriteLength() {
        return writeLength;
    }

    public void writeBytes(byte[] buf) throws IOException {
        outFile.write(buf);
        writeLength += buf.length;
    }

    public void writeBytes(byte[] buf, int off, int len) throws IOException {
        outFile.write(buf, off, len);
        writeLength += len;
    }

    public void writeInt(int val) throws IOException {
        byte[] buf = new byte[4];
        AddSharedUserId.setInt(buf, 0, val);
        outFile.write(buf);
        writeLength += 4;
    }

    public void writeShort(int val) throws IOException {
        byte[] buf = new byte[2];
        AddSharedUserId.setShort(buf, 0, val);
        outFile.write(buf);
        writeLength += 2;
    }

    public void writeIntArray(int[] values) throws IOException {
        byte[] buf = new byte[4 * values.length];
        for (int i = 0; i < values.length; i++) {
            AddSharedUserId.setInt(buf, 4 * i, values[i]);
        }
        writeBytes(buf);
    }

    // Write to the position
    public void writeInt(int position, int val) throws IOException {
        long oldPosition = outFile.getFilePointer();
        outFile.seek(position);
        writeInt(val);
        outFile.seek(oldPosition);
    }
}

class ResStringChunk {
    byte[] resStrTableHdr = new byte[28];
    int chunkTag;
    int chunkSize;
    int stringCount;
    int styleCount;
    int unknown1;
    int stringOffset;
    int styleOffset;

    int[] stringOffsetArray;
    byte[] strBuffer;
    String[] stringValues;

    private static void parseString(byte[] buffer, int[] stringIdxOffset,
                                    String[] stringValues) throws IOException {
        for (int i = 0; i < stringIdxOffset.length; i++) {
            int curOffset = stringIdxOffset[i];
            int strLen = AddSharedUserId.getShort(buffer, curOffset);
            // log ("string length = %d", strLen);
            if (strLen < 32768) {
                byte[] chars = new byte[strLen];
                for (int ii = 0; ii < strLen; ii++) {
                    chars[ii] = buffer[curOffset + 2 + ii * 2];
                }
                stringValues[i] = new String(chars); // Hack, just use 8
                // byte chars
            }

            AddSharedUserId.log("%d:\t%s", i, stringValues[i]);
        }
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
        is.readBytes(resStrTableHdr);
        chunkTag = AddSharedUserId.getInt(resStrTableHdr, 0);
        chunkSize = AddSharedUserId.getInt(resStrTableHdr, 4);
        stringCount = AddSharedUserId.getInt(resStrTableHdr, 8);
        styleCount = AddSharedUserId.getInt(resStrTableHdr, 12);
        stringOffset = AddSharedUserId.getInt(resStrTableHdr, 20);
        styleOffset = AddSharedUserId.getInt(resStrTableHdr, 24);

        // String index array and string values
        stringOffsetArray = new int[stringCount];
        stringValues = new String[stringCount];
        is.readIntArray(stringOffsetArray);

        // how many padding before string value, generally should be 0
        int strValuePaddings = 8 + stringOffset - (8 + 28 + 4 * stringCount);
        AddSharedUserId.log("strValuePaddings=%d", strValuePaddings);
        is.skip(strValuePaddings);
        int strBufSize = chunkSize - 28 - 4 * stringCount - strValuePaddings;
        strBuffer = new byte[strBufSize];
        is.readBytes(strBuffer);

        parseString(strBuffer, stringOffsetArray, stringValues);
    }

    // Add a string to the string table
    // position is the index of added string
    // NOTE: long string (> 32767) is not supported
    public void addString(String addedStr, int position) {
        int addedSize = 4 + (addedStr.length() + 2) * 2;

        this.chunkSize += addedSize;
        this.stringCount += 1;
        this.stringOffset += 4;
        AddSharedUserId.setInt(resStrTableHdr, 4, chunkSize);
        AddSharedUserId.setInt(resStrTableHdr, 8, stringCount);
        AddSharedUserId.setInt(resStrTableHdr, 20, stringOffset);

        // Update string values
        String[] newStringValues = new String[stringCount];
        for (int i = 0; i < stringCount; i++) {
            if (i < position) {
                newStringValues[i] = stringValues[i];
            } else if (i > position) {
                newStringValues[i] = stringValues[i - 1];
            } else {
                newStringValues[i] = addedStr;
            }
        }
        this.stringValues = newStringValues;

        // Update string offsets & string buffers
        int oldBufferOffset = 0;
        int newBufferOffset = 0;
        int[] newStrOffsetArray = new int[stringCount];
        byte[] newStrBuffer = new byte[strBuffer.length
                + (addedStr.length() + 2) * 2];

        for (int i = 0; i < stringCount; i++) {
            newStrOffsetArray[i] = newBufferOffset;
            if (i != position) {
                int oneStrLen = getStrTotalLength(strBuffer, oldBufferOffset);
                AddSharedUserId.log("String Len: %d", oneStrLen);
                System.arraycopy(strBuffer, oldBufferOffset, newStrBuffer,
                        newBufferOffset, oneStrLen);
                oldBufferOffset += oneStrLen;
                newBufferOffset += oneStrLen;
            } else {
                AddSharedUserId.setShort(newStrBuffer, newBufferOffset,
                        addedStr.length());
                newBufferOffset += 2;
                for (int ii = 0; ii < addedStr.length(); ii++) {
                    newStrBuffer[newBufferOffset + ii * 2] = (byte) addedStr
                            .charAt(ii);
                    newStrBuffer[newBufferOffset + ii * 2 + 1] = 0;
                }
                newBufferOffset += addedStr.length() * 2;
                AddSharedUserId.setShort(newStrBuffer, newBufferOffset, 0);
                newBufferOffset += 2;
            }
        }

        stringOffsetArray = newStrOffsetArray;
        strBuffer = newStrBuffer;
    }

    // At stringsStart are all of the UTF-16 strings concatenated together; each
    // starts with a uint16_t of the string's length and each ends with a 0x0000
    // terminator. If a string is > 32767 characters, the high bit of the length
    // is set meaning to take those 15 bits as a high word and it will be
    // followed by another uint16_t containing the low word.
    private int getStrTotalLength(byte[] buf, int offset) {
        int sectionLen = 2;
        int len = AddSharedUserId.getShort(buf, offset);
        if ((len & 0x8000) != 0) {
            int low = AddSharedUserId.getShort(buf, offset + 2);
            len = (((len & 0x7fff) << 16) | low);
            sectionLen += 2;
        }
        sectionLen += len * 2 + 2; // end with 0x0000
        return sectionLen;
    }

    // Dump to the output
    public void dump(MyFileOutput out) throws IOException {
        // Recompute chunk size
        int lastStrPos = this.stringOffsetArray[this.stringCount - 1];
        int lastStrLen = getStrTotalLength(strBuffer, lastStrPos);
        int strValueLen = lastStrPos + lastStrLen;
        this.chunkSize = 28 + this.stringCount * 4 + strValueLen;
        int paddingSize = chunkSize % 4;
        this.chunkSize += paddingSize;
        AddSharedUserId.setInt(resStrTableHdr, 4, chunkSize);

        out.writeBytes(resStrTableHdr);
        out.writeIntArray(stringOffsetArray);
        out.writeBytes(strBuffer, 0, strValueLen);
        if (paddingSize > 0) {
            out.writeShort(0);
        }
    }

    public String getStringByIndex(int idx) {
        if (idx >= 0 && idx < stringValues.length) {
            return stringValues[idx];
        }
        return null;
    }
}

class ResAttrIdChunk {
    int chunkTag;
    int chunkSize;
    int[] attrIdArray;

    public void parse(MyInputStream is) throws IOException {
        chunkTag = is.readInt();
        chunkSize = is.readInt();
        int attrCount = (chunkSize - 8) / 4;
        attrIdArray = new int[attrCount];
        AddSharedUserId.log("Attr Count: " + attrCount);
        for (int i = 0; i < attrCount; i++) {
            attrIdArray[i] = is.readInt();
            AddSharedUserId.log("\t" + attrIdArray[i]);
        }
    }

    public int getCount() {
        if (attrIdArray != null) {
            return attrIdArray.length;
        } else {
            return 0;
        }
    }

    public void addAttributeId(int value, int position) {
        chunkSize += 4;
        int[] newAttrIds = new int[attrIdArray.length + 1];
        for (int i = 0; i < position; i++) {
            newAttrIds[i] = attrIdArray[i];
        }
        newAttrIds[position] = value;
        for (int i = position + 1; i < newAttrIds.length; i++) {
            newAttrIds[i] = attrIdArray[i - 1];
        }
        attrIdArray = newAttrIds;
    }

    public void dump(MyFileOutput out) throws IOException {
        out.writeInt(chunkTag);
        out.writeInt(chunkSize);
        out.writeIntArray(attrIdArray);
    }
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
    private int addedAttrPosition;
    private int addedValPosition;
    private int stringCount; // stringCount after modification
    private int androidIndex;
    private int manifestIndex;

    // addedAttrPosition = the position of added attribute name in string table
    public AxmlBodyChunk(int addedAttrPos, int addedValPos, ResStringChunk strChunk) {
        this.addedAttrPosition = addedAttrPos;
        this.addedValPosition = addedValPos;
        this.stringCount = strChunk.stringCount;
        this.stringChunk = strChunk;

        for (int i = 0; i < stringCount; i++) {
            if ("android".equals(strChunk.stringValues[i])) {
                androidIndex = i;
            } else if ("manifest".equals(strChunk.stringValues[i])) {
                manifestIndex = i;
            }
        }
    }

    public byte[] getRawChunkData() {
        return rawChunkData;
    }

    public int parseNext(MyInputStream is) throws IOException {
        int chunkTag = is.readInt();
        int chunkSize = is.readInt();

        rawChunkData = new byte[chunkSize];
        AddSharedUserId.setInt(rawChunkData, 0, chunkTag);
        AddSharedUserId.setInt(rawChunkData, 4, chunkSize);
        if (chunkSize > 8) {
            is.readBytes(rawChunkData, 8, chunkSize - 8);
        }

        if (chunkTag == startTag) { // XML START TAG
            // int lineNo = AddAttribute.getInt(rawChunkData, 2 * 4);
            // int tag3 = AddAttribute.getInt(rawChunkData, 3 * 4);
            int nameNsSi = AddSharedUserId.getInt(rawChunkData, 4 * 4);
            nameNsSi = changeIndex(nameNsSi, 4 * 4);
            int nameSi = AddSharedUserId.getInt(rawChunkData, 5 * 4);
            nameSi = changeIndex(nameSi, 5 * 4);
            // Expected to be 14001400
            // int tag6 = AddAttribute.getInt(rawChunkData, 6 * 4);
            // Number of Attributes to follow
            int attrNum = AddSharedUserId.getInt(rawChunkData, 7 * 4);
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
            for (int ii = 0; ii < attrNum; ii++) {
                int attrNsIdx = AddSharedUserId.getInt(rawChunkData,
                        36 + ii * 20);
                changeIndex(attrNsIdx, 36 + ii * 20);
                int attrNameIdx = AddSharedUserId.getInt(rawChunkData,
                        36 + ii * 20 + 4);
                attrNameIdx = changeIndex(attrNameIdx, 36 + ii * 20 + 4);
                int attrValIdx = AddSharedUserId.getInt(rawChunkData,
                        36 + ii * 20 + 8);
                attrValIdx = changeIndex(attrValIdx, 36 + ii * 20 + 8);
                int type = AddSharedUserId.getInt(rawChunkData,
                        36 + ii * 20 + 12) >> 16;
                type = ((type & 0xff00) >> 8) | ((type & 0x00ff) << 8);
                int attrValIdx2 = AddSharedUserId.getInt(rawChunkData,
                        36 + ii * 20 + 16);
                if (isTypeRefToStrTable(type)) {
                    attrValIdx2 = changeIndex(attrValIdx2, 36 + ii * 20 + 16);
                }
                AddSharedUserId.log(
                        "%s(%d)=%s(%d), type = 0X%x, attrValIdx=%d, attrValIdx2=%d",
                        stringChunk.getStringByIndex(attrNameIdx), attrNameIdx,
                        stringChunk.getStringByIndex(attrValIdx), attrValIdx, type,
                        attrValIdx, attrValIdx2);
            }

            // Add attribute to manifest tag
            // android:sharedUserId="xyz.xyz.x"
            // 0d00 0000 0000 0000 ffff ffff 0800 0003 1400 0000
            if (nameSi == manifestIndex) {
                AddSharedUserId.log("manifest start tag offset: " + (is.getPosition() - chunkSize));

                byte[] newRawBuf = new byte[rawChunkData.length + 20];
                System.arraycopy(rawChunkData, 0, newRawBuf, 0, 36);
                System.arraycopy(rawChunkData, 36, newRawBuf, 36 + 20, chunkSize - 36);

                // New attr (5 bytes)
                int newAttrOffset = 36;
                AddSharedUserId.setInt(newRawBuf, newAttrOffset, androidIndex);
                AddSharedUserId.setInt(newRawBuf, newAttrOffset + 4,
                        addedAttrPosition);
                AddSharedUserId.setInt(newRawBuf, newAttrOffset + 8,
                        addedValPosition + 1);
                AddSharedUserId.setInt(newRawBuf, newAttrOffset + 12,
                        0x03000008);
                // AddManifestAttribute.setInt(newRawBuf, chunkSize + 16, 2);
                AddSharedUserId.setInt(newRawBuf, newAttrOffset + 16, addedValPosition + 1);

                // Chunk size & attr count
                chunkSize += 20;
                AddSharedUserId.setInt(newRawBuf, 4, chunkSize); // Chunk
                // Size
                attrNum += 1;
                AddSharedUserId.setInt(newRawBuf, 7 * 4, attrNum); // Attr
                // count

                rawChunkData = newRawBuf;
            }

        } else if (chunkTag == endTag) { // XML END TAG
            int nameNsSi = AddSharedUserId.getInt(rawChunkData, 4 * 4);
            changeIndex(nameNsSi, 4 * 4);
            int nameSi = AddSharedUserId.getInt(rawChunkData, 5 * 4);
            changeIndex(nameSi, 5 * 4);

        } else if (chunkTag == endDocTag) { // END OF XML DOC TAG
            int prefix = AddSharedUserId.getInt(rawChunkData, 4 * 4);
            changeIndex(prefix, 4 * 4);
            int uri = AddSharedUserId.getInt(rawChunkData, 5 * 4);
            changeIndex(uri, 5 * 4);

        } else if (chunkTag == namespaceTag) {
            int prefix = AddSharedUserId.getInt(rawChunkData, 4 * 4);
            changeIndex(prefix, 4 * 4);
            int uri = AddSharedUserId.getInt(rawChunkData, 5 * 4);
            changeIndex(uri, 5 * 4);

        } else if (chunkTag == cdataTag) {

        }

        return chunkTag;
    }

    // Check the attribute value type whether refer to the string table
    private boolean isTypeRefToStrTable(int type) {
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

    private int changeIndex(int strIndex, int offset) {
        int skip = 0;
        if (strIndex >= addedAttrPosition) {
            skip += 1;
            if (strIndex >= addedValPosition) {
                skip += 1;
            }
        }
        if (skip > 0) {
            AddSharedUserId.setInt(rawChunkData, offset, strIndex + skip);
        }
        return strIndex + skip;
//		if (strIndex >= addedAttrPosition && strIndex < stringCount - 1) {
//			AddSharedUserId.setInt(rawChunkData, offset, strIndex + 1);
//			return strIndex + 1;
//		}
//		return strIndex;
    }
}

// Designed to add installLocation Attribute to AndroidManifest binary file
public class AddSharedUserId {

    private MyInputStream is;
    private MyFileOutput out;

    public AddSharedUserId(String inputFile, String outputFile)
            throws IOException {
        FileInputStream fis = new FileInputStream(inputFile);
        is = new MyInputStream(fis);
        File f = new File(outputFile);
        if (f.exists()) {
            f.delete();
        }
        RandomAccessFile outFile = new RandomAccessFile(outputFile, "rw");
        outFile.setLength(0);
        out = new MyFileOutput(outFile);
    }

    public static void main(String[] args) throws Exception {
//		if (args.length < 1) {
//			System.out
//					.println("Usage: AddInstallLocationAttr.jar inputFile outputFile");
//			return;
//		}
//
//		String inputFile = args[0];
//		String outputFile = args[1];

        String inputFile = "D:\\Android\\apk\\HackAppDataFree\\dist\\AndroidManifest.old.xml";
        String outputFile = "D:\\Android\\apk\\HackAppDataFree\\dist\\AndroidManifest.xml";

        AddSharedUserId ama = new AddSharedUserId(inputFile,
                outputFile);

        ama.addSharedUserId();
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
        String msg = String.format(format, arguments);
        //Log.d("DEBUG", msg);
        System.out.println(msg);
    }

    public void addSharedUserId() throws Exception {
        addAttribute();
    }

    private void addAttribute() throws Exception {
        // Header
        int headTag = is.readInt();
        int fileSize = is.readInt();
        out.writeInt(headTag);
        out.writeInt(fileSize);

        // String table
        ResStringChunk strChunk = new ResStringChunk();
        strChunk.parse(is);
        log("ChunkSize of Resource String: %d", strChunk.chunkSize);
        log("String Count: %d", strChunk.stringCount);
        log("String Offset: 0x%x", strChunk.stringOffset);
        if (strChunk.styleCount != 0 || strChunk.styleOffset != 0) {
            throw new Exception(
                    "Detected style information, not supported yet!");
        }

        // Resource Attribute ID table
        ResAttrIdChunk attrIdChunk = new ResAttrIdChunk();
        attrIdChunk.parse(is);
        attrIdChunk.addAttributeId(0x0101000b, 0);
        fileSize += 4;

        int oldSize = strChunk.chunkSize;
        // Add the tail first (make sure the index not changed)
        int valPos = attrIdChunk.getCount();
        strChunk.addString("com.gmail.heagoo.hackappdata", valPos);
        strChunk.addString("sharedUserId", 0);
        strChunk.dump(out);
        fileSize += strChunk.chunkSize - oldSize;
        attrIdChunk.dump(out);

        // Body
        AxmlBodyChunk body = new AxmlBodyChunk(0, valPos, strChunk);
        int tag = 0;
        do {
            tag = body.parseNext(is);
            log("tag=0x%x", tag);
            out.writeBytes(body.getRawChunkData());
            log("Wrote: 0x%x", out.getWriteLength());
        } while (tag != AxmlBodyChunk.endDocTag);

        // Because added 1 attr, 20 bytes
        fileSize += 20;

        // Revise the file size
        out.writeInt(4, fileSize);
    }

}
