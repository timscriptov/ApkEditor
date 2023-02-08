package com.gmail.heagoo.apkeditor.ce.e;

import com.gmail.heagoo.common.RandomUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

// Designed to edit string inside AXML
public class AxmlStringEditor {

    private MyInputStream is;
    private String outputFolder;
    private String outputFile;
    private MyFileOutput out;

    // Header
    private int headTag;
    private int fileSize;

    // String pool
    private ResStringChunk strChunk;


    public AxmlStringEditor(InputStream input, String outputFolder) throws IOException {
        this.outputFolder = outputFolder;

        // Prepare input and output
        is = new MyInputStream(input);

        // Parse the manifest into internal presentation
        try {
            parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static int getInt(byte[] buf, int offset) {
        return ((int) buf[offset] & 0xff)
                | (((int) buf[offset + 1] & 0xff) << 8)
                | (((int) buf[offset + 2] & 0xff) << 16)
                | (((int) buf[offset + 3] & 0xff) << 24);
    }

    protected static void setInt(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value & 0xff);
        buf[offset + 1] = (byte) ((value >> 8) & 0xff);
        buf[offset + 2] = (byte) ((value >> 16) & 0xff);
        buf[offset + 3] = (byte) ((value >> 24) & 0xff);
    }

    protected static void log(String format, Object... arguments) {
        // String msg = String.format(format, arguments);
        // Log.d("DEBUG", msg);
        // System.out.println(msg);
    }

    private void prepareOutput() throws IOException {
        File dir = new File(outputFolder);
        File f = new File(dir, RandomUtil.getRandomString(6));
        if (f.exists()) {
            f.delete();
        }

        this.outputFile = f.getPath();
        RandomAccessFile outFile = new RandomAccessFile(f.getPath(), "rw");
        outFile.setLength(0);
        out = new MyFileOutput(outFile);
    }

    private void parse() throws Exception {
        // Header
        this.headTag = is.readInt();
        this.fileSize = is.readInt();

        // String table
        this.strChunk = new ResStringChunk();
        strChunk.parse(is);
    }

    // Return null if no string starts with oldStarts
    public String modifyStringStartWith(String oldStarts, String newStarts) throws IOException {
        boolean strModified = false;

        // Check if there are string starts with oldStarts
        int count = strChunk.getStringCount();
        for (int i = 0; i < count; ++i) {
            String str = strChunk.getStringByIndex(i);
            if (str.startsWith(oldStarts)) {
                String newStr = newStarts + str.substring(oldStarts.length());
                strChunk.modifyString(i, newStr);
                strModified = true;
            }
        }

        if (strModified) {
            // Prepare the output file
            prepareOutput();

            save();
        }

        return outputFile;
    }

    private void save() throws IOException {
        try {
            out.writeInt(headTag);
            out.writeInt(fileSize);

            // String
            int oldSize = strChunk.chunkSize;
            strChunk.dump(out);

            int readBytes;
            byte[] buf = new byte[4096];
            while ((readBytes = is.readBytes(buf, 0, buf.length)) > 0) {
                out.writeBytes(buf, 0, readBytes);
            }

            // Revise the file size
            log("Wrote: 0x%x", out.getWriteLength());
            out.writeInt(4, out.getWriteLength());
        } finally {
            closeQuietly(out);
        }
    }

    private void closeQuietly(MyFileOutput out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
            }
        }
    }
}
