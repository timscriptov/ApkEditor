package com.gmail.heagoo.apkeditor.ce.e;

import com.google.common.io.LittleEndianDataInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

//import brut.androlib.res.data.LEDataInputStream;

public class ResourceEditor {

    private LittleEndianDataInputStream input;
    private FileOutputStream output;

    // Parsed from arsc file
    private ResChunkHeader tableHeader;
    private int packageCount;
    private ResChunkHeader strPoolHeader;
    private byte[] strPoolData;
    private ResChunkHeader resPkgHeader;
    private int packageId;

    // Store the package name
    private byte[] packageNameBuf;

    private StringBlockEditor strPoolEditor;

    public ResourceEditor(InputStream _input, String outputFile)
            throws IOException {
        this.input = new LittleEndianDataInputStream(_input);
        File f = new File(outputFile);
        if (f.exists()) {
            f.delete();
        }
        this.output = new FileOutputStream(f);

        // 8 bytes: ResChunk_header
        this.tableHeader = new ResChunkHeader();
        tableHeader.readFrom(input);

        // Package Count
        this.packageCount = input.readInt();
        //Log.d("DEBUG", "Package Count = " + packageCount);

        // ResStringPool_header
        this.strPoolHeader = new ResChunkHeader();
        strPoolHeader.readFrom(input);
        this.strPoolData = new byte[strPoolHeader.chunkSize - 8];
        input.readFully(strPoolData);

        // ResTable_package
        this.resPkgHeader = new ResChunkHeader();
        resPkgHeader.readFrom(input);
        this.packageId = input.readInt();
    }

    // Little endian
    private void writeInt(int v) throws IOException {
        byte[] buffer = {(byte) (v & 0xff), (byte) ((v >> 8) & 0xff),
                (byte) ((v >> 16) & 0xff), (byte) ((v >> 24) & 0xff)};
        this.output.write(buffer);
    }

    // Ugly interface, but works
    // Change app name by modifying string in the pool
    // Modify all the strings starts with oldStarts, and replace it with newStarts
    public boolean modifyString(String oldStr, String newStr,
                                String oldStarts, String newStarts) {
        this.strPoolEditor = new StringBlockEditor(strPoolHeader, strPoolData);
        return strPoolEditor.replaceString(oldStr, newStr, oldStarts, newStarts);
    }

    public boolean modifyPackageName(String name) {
        if (name.length() > 127) {
            return false;
        }

        // The id of the Package defined by an application is always 0x7F
        // https://justanapplication.wordpress.com/2011/09/16/android-internals-resources-part-five-the-package-chunk/
        if (packageId == 0x7f) {
            this.packageNameBuf = new byte[256];
            for (int i = 0; i < name.length(); i++) {
                packageNameBuf[2 * i] = (byte) name.charAt(i);
            }
            return true;
        }

        return false;
    }

    public void save() throws IOException {

        // Write the head block
        tableHeader.setChunkSize(tableHeader.chunkSize
                + this.strPoolEditor.getDeltaSize());
        tableHeader.writeTo(output);
        writeInt(packageCount);

        // Write the string pool block
        if (strPoolEditor != null) {
            this.strPoolEditor.writeTo(output);
        } else {
            strPoolHeader.writeTo(output);
            output.write(strPoolData);
        }

        // Write package header and id
        resPkgHeader.writeTo(output);
        writeInt(packageId);

        // Write package name
        if (this.packageNameBuf != null) {
            output.write(packageNameBuf);
            input.skipBytes(256);
        }

        // Copy remaining
        int length = 4096;
        byte[] buffer = new byte[length];
        int readLen = input.read(buffer, 0, length);
        while (readLen > 0) {
            output.write(buffer, 0, readLen);
            readLen = input.read(buffer, 0, length);
        }

        output.close();
    }

    static class ResChunkHeader {
        public short type;
        public short headerSize;
        public int chunkSize;
        private byte[] buffer;

        public void readFrom(LittleEndianDataInputStream is) throws IOException {
            this.buffer = new byte[8];
            is.readFully(buffer);

            this.type = getShort(buffer, 0);
            this.headerSize = getShort(buffer, 2);
            this.chunkSize = getInt(buffer, 4);
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

        // Revise the chunk size
        public void setChunkSize(int size) {
            this.chunkSize = size;
            setInt(buffer, 4, size);
        }

        public void writeTo(OutputStream output) throws IOException {
            output.write(buffer);
        }
    }
}
