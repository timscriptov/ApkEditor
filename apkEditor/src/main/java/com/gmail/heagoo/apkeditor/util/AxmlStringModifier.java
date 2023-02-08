package com.gmail.heagoo.apkeditor.util;

import android.content.Context;

import com.gmail.heagoo.apkeditor.ce.IApkMaking;
import com.gmail.heagoo.apkeditor.ce.IDescriptionUpdate;
import com.gmail.heagoo.apkeditor.ce.e.MyFileOutput;
import com.gmail.heagoo.apkeditor.ce.e.MyInputStream;
import com.gmail.heagoo.apkeditor.ce.e.ResStringChunk;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AxmlStringModifier implements IApkMaking {

    // Always ends with "/"
    private String decodedRootpath;
    private Map<String, Map<String, String>> fileReplaces;

    public AxmlStringModifier(String decodedRootpath,
                              Map<String, Map<String, String>> modifications) {
        if (!decodedRootpath.endsWith("/")) {
            decodedRootpath += "/";
        }
        this.decodedRootpath = decodedRootpath;
        this.fileReplaces = modifications;
    }

    public boolean modify(InputStream input, String outFilePath,
                          Map<String, String> replaces) throws IOException {
        // Create reverse replaces
        Map<String, String> rReplaces = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : replaces.entrySet()) {
            rReplaces.put(entry.getValue(), entry.getKey());
        }

        MyInputStream is = new MyInputStream(input);
        RandomAccessFile outFile = null;
        MyFileOutput out = null;

        try {
            outFile = new RandomAccessFile(outFilePath, "rw");
            outFile.setLength(0);
            out = new MyFileOutput(outFile);

            // Header
            int headTag = is.readInt();
            int fileSize = is.readInt();
            out.writeInt(headTag);
            out.writeInt(fileSize);

            // String table
            ResStringChunk strChunk = new ResStringChunk();
            strChunk.parse(is);

            int count = strChunk.getStringCount();
            for (int i = 0; i < count; i++) {
                String str = strChunk.getStringByIndex(i);
                String replacing = rReplaces.get(str);
                if (replacing != null) {
                    //Log.d("DEBUG", "Replace " + str + " with " + replacing);
                    strChunk.modifyString(i, replacing);
                }
            }

            int oldSize = strChunk.chunkSize;
            strChunk.dump(out);
            fileSize += strChunk.chunkSize - oldSize;

            // Copy remain content
            byte[] buf = new byte[4096];
            int ret = -1;
            while ((ret = is.readBytes(buf, 0, buf.length)) != -1) {
                out.writeBytes(buf, 0, ret);
            }

            // Revise file size
            out.writeInt(4, out.getWriteLength());
        } finally {
            closeQuietly(out);
        }

        return false;
    }

    private void closeQuietly(MyFileOutput out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
            }
        }
    }

    private void closeQuietly(ZipFile f) {
        if (f != null) {
            try {
                f.close();
            } catch (IOException e) {
            }
        }
    }

    @Override
    public void prepareReplaces(Context ctx, String apkFilePath,
                                Map<String, String> allReplaces, IDescriptionUpdate updater)
            throws Exception {
        ZipFile zipFile = null;

        try {
            zipFile = new ZipFile(apkFilePath);
            for (Map.Entry<String, Map<String, String>> entry : fileReplaces
                    .entrySet()) {

                String filePath = entry.getKey();
                Map<String, String> replaces = entry.getValue();

                // Something is wrong is not in the decoded path
                if (!filePath.startsWith(decodedRootpath)) {
                    continue;
                }

                String entryName = filePath.substring(decodedRootpath.length());
                ZipEntry zipEntry = zipFile.getEntry(entryName);
                InputStream in = zipFile.getInputStream(zipEntry);

                String newPath = filePath + ".bin";
                modify(in, newPath, replaces);
                allReplaces.put(entryName, newPath);

                in.close();
            }

            zipFile.close();
        } finally {
            closeQuietly(zipFile);
        }
    }
}
