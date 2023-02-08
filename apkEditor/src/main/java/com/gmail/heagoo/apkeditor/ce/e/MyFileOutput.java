package com.gmail.heagoo.apkeditor.ce.e;

import java.io.IOException;
import java.io.RandomAccessFile;

public // Output wrapper
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
        ManifestEditorNew.setInt(buf, 0, val);
        outFile.write(buf);
        writeLength += 4;
    }

    public void writeShort(int val) throws IOException {
        byte[] buf = new byte[2];
        ManifestEditorNew.setShort(buf, 0, val);
        outFile.write(buf);
        writeLength += 2;
    }

    public void writeIntArray(int[] values) throws IOException {
        byte[] buf = new byte[4 * values.length];
        for (int i = 0; i < values.length; i++) {
            ManifestEditorNew.setInt(buf, 4 * i, values[i]);
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

    public void close() throws IOException {
        outFile.close();
    }
}
