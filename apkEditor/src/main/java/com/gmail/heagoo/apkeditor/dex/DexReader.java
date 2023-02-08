package com.gmail.heagoo.apkeditor.dex;

public class DexReader {

    private byte[] buf;
    private int offset;

    public DexReader(byte[] buf, int offset) {
        this.buf = buf;
        this.offset = offset;
    }

    public int readSmallUleb128() throws MyException {
        return readUleb128(false);
    }

    private int readUleb128(boolean allowLarge) throws MyException {
        int end = offset;
        int currentByteValue;
        int result;

        result = buf[end++] & 0xff;
        if (result > 0x7f) {
            currentByteValue = buf[end++] & 0xff;
            result = (result & 0x7f) | ((currentByteValue & 0x7f) << 7);
            if (currentByteValue > 0x7f) {
                currentByteValue = buf[end++] & 0xff;
                result |= (currentByteValue & 0x7f) << 14;
                if (currentByteValue > 0x7f) {
                    currentByteValue = buf[end++] & 0xff;
                    result |= (currentByteValue & 0x7f) << 21;
                    if (currentByteValue > 0x7f) {
                        currentByteValue = buf[end++];

                        // MSB shouldn't be set on last byte
                        if (currentByteValue < 0) {
                            throw new MyException(
                                    "Invalid uleb128 at offset 0x%x", offset);
                        } else if ((currentByteValue & 0xf) > 0x07) {
                            if (!allowLarge) {
                                // for non-large uleb128s, we assume most
                                // significant bit of the result will not be
                                // set, so that it can fit into a signed integer
                                // without wrapping
                                throw new MyException(
                                        "uleb128 is out of range at offset 0x%x",
                                        offset);
                            }
                        }
                        result |= currentByteValue << 28;
                    }
                }
            }
        }

        offset = end;
        return result;
    }

    public String readString(int utf16Length) {
        int[] ret = new int[1];
        String value = Utf8Utils.utf8BytesWithUtf16LengthToString(buf,
                offset, utf16Length, ret);
        offset += ret[0];
        return value;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
