package com.gmail.heagoo.apkeditor.ce.e;

import android.content.Context;

import com.gmail.heagoo.apkeditor.ce.IApkMaking;
import com.gmail.heagoo.apkeditor.ce.IDescriptionUpdate;
import com.gmail.heagoo.common.SDCard;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.zip.Adler32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// Edit classes.dex and launcher icon inside the plugin wrapper
public class PluginWrapperExtra implements IApkMaking, Serializable {
    private static final long serialVersionUID = -4503319102745406211L;

    // buf size (a little larger than the size of dex file inside wrapper)
    private final int dexBufSize = 559000;

    // String offset for "com.morgoo.droidplugin_stub", but point to "stub"
    private final int stringOffset = 0x6201e;

    private String authorityStr;

    public PluginWrapperExtra(String authorityStr) {
        this.authorityStr = authorityStr;
    }

    public static void calcSignature(byte bytes[], int totalLen) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        md.update(bytes, 32, totalLen - 32);
        try {
            md.digest(bytes, 12, 20);
        } catch (DigestException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void calcChecksum(byte bytes[], int totalLen) {
        Adler32 a32 = new Adler32();
        a32.update(bytes, 12, totalLen - 12);
        int sum = (int) a32.getValue();
        bytes[8] = (byte) (sum & 0xff);
        bytes[9] = (byte) ((sum >> 8) & 0xff);
        bytes[10] = (byte) ((sum >> 16) & 0xff);
        bytes[11] = (byte) ((sum >> 24) & 0xff);
    }

    // apkFilePath: the path of the plugin wrapper
    @Override
    public void prepareReplaces(Context ctx, String apkFilePath,
                                Map<String, String> allReplaces, IDescriptionUpdate updater)
            throws Exception {
        String dexPath = SDCard.makeWorkingDir(ctx) + "_dex";

        RandomAccessFile outFile = new RandomAccessFile(dexPath, "rw");
        outFile.setLength(0);

        // Copy classes.dex
        ZipFile zipFile = new ZipFile(apkFilePath);
        ZipEntry entry = zipFile.getEntry("classes.dex");
        InputStream in = zipFile.getInputStream(entry);

        // Read all data from classes.dex
        int totalLen = 0;
        byte[] allData = new byte[dexBufSize];
        int curRead = 0;
        while ((curRead = in.read(allData, totalLen,
                allData.length - totalLen)) > 0) {
            totalLen += curRead;
        }

        // Change authorities name
        System.arraycopy(authorityStr.getBytes(), 0, allData, stringOffset,
                authorityStr.length());

        // Revise signature and checksum
        calcSignature(allData, totalLen);
        calcChecksum(allData, totalLen);

        // Write to file
        outFile.write(allData, 0, totalLen);

        in.close();
        zipFile.close();
        outFile.close();

        allReplaces.put("classes.dex", dexPath);
    }

    @SuppressWarnings("unused")
    private void getExactBytes(InputStream in, byte[] data) throws IOException {
        int totalRead = 0;
        int readBytes = 0;
        while (totalRead < data.length && (readBytes = in.read(data, totalRead,
                data.length - totalRead)) > 0) {
            totalRead += readBytes;
        }
    }
}
