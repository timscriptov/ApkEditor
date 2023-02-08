package com.gmail.heagoo.apkeditor.util;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.FileUtil;
import com.gmail.heagoo.common.RootCommand;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.common.TextFileReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.util.List;

// Directly patch the DEX cache
public class OdexPatcher {
    public String errMessage;
    public String targetOdex; // Where is cached odex

    private String apkName; // like base.apk
    private int checksumOffset; // crc32 offset;
    private byte[] crc32;   // checksum in the cached odex

    private String packageName;

    public OdexPatcher(String packageName) {
        this.packageName = packageName;
    }

//    // Fake method
//    public void applyPatch(Activity activity, String targetApkPath) {
//    }

    private static boolean isAlphabet(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    public void applyPatch(Activity ctx, String apkPath) {
        targetOdex = getTargetOdex(ctx);
        if (targetOdex == null) {
            errMessage = ctx.getString(R.string.patch_err_odex_not_found1);
            return;
        }

        // Correct target odex path
        RootCommand rc = new RootCommand();
        rc.runCommand("ls " + targetOdex, null, 3000);
        String strError = rc.getStdError();
        if (strError != null && !strError.equals("")) {
            String newTryPath;
            if (targetOdex.contains("/arm64/")) {
                newTryPath = targetOdex.replace("/arm64/", "/arm/");
            } else {
                newTryPath = targetOdex.replace("/arm/", "/arm64/");
            }
            rc.runCommand("ls " + newTryPath, null, 3000);
            strError = rc.getStdError();
            if (strError != null && !strError.equals("")) {
                errMessage = String.format(
                        ctx.getString(R.string.patch_err_odex_not_found2),
                        targetOdex, newTryPath);
                return;
            }
            targetOdex = newTryPath;
        }

        parseARTOdex(ctx, targetOdex, true);
        if (crc32 == null) {
            errMessage = "Cannot get the original checksum.";
            return;
        }

        dex2oat(ctx, apkPath);

        killApp();
    }

    private String getTargetOdex(Activity ctx) {
        TextFileReader reader = null;
        try {
            reader = new TextFileReader("/proc/self/maps");
        } catch (Exception ignored) {
            return null;
        }

        // Get myself odex file
        List<String> lines = reader.getLines();
        String odexPath = getOdexPath(ctx, lines);
        if (odexPath == null) {
            return null;
        }

        // Get target odex file
        int position = odexPath.indexOf("/" + ctx.getPackageName());
        String folder = odexPath.substring(0, position);
        RootCommand rc = new RootCommand();
        rc.runCommand("ls " + folder, null, 3000, true);
        String output = rc.getStdOut();
        if (output == null || output.equals("")) {
            return null;
        }
        String targetPackagePath = getTargetFolder(output);
        if (targetPackagePath == null) {
            return null;
        }
        int position2 = odexPath.indexOf('/', position + 1);
        odexPath = folder + "/" + targetPackagePath + odexPath.substring(position2);
        return odexPath;
    }

    // Get target folder from "ls" command
    private String getTargetFolder(String output) {
        String name = null;
        BufferedReader br = new BufferedReader(new StringReader(output));
        try {
            String line = br.readLine();
            while (line != null) {
                if (line.equals(this.packageName)) {
                    name = line;
                    break;
                }
                if (line.startsWith(this.packageName)) {
                    char c = line.charAt(packageName.length());
                    if (c != '.' && !isAlphabet(c)) {
                        name = line;
                        break;
                    }
                }
                line = br.readLine();
            }
        } catch (Exception ignored) {
        }
        return name;
    }

    private String getOdexPath(Activity activity, List<String> lines) {
        String path = null;
        String myPkgName = activity.getPackageName();

        for (int i = 0; i < lines.size(); ++i) {
            String line = lines.get(i);
            if (line.endsWith(".odex") && line.contains("/" + myPkgName)) {
                String[] words = line.split("\\s+");
                path = words[words.length - 1];
                break;
            }
        }

        return path;
    }

    private void killApp() {
        RootCommand rc = new RootCommand();
        rc.runRootCommand("am force-stop " + packageName, null, 5000);
    }

    // Parse apk name and checksum in odex file
    // result is saved to checksumOffset and crc32
    private boolean parseARTOdex(Activity activity, String odexFile, boolean bSaveChecksum) {
        try {
            ApplicationInfo ai = activity.getPackageManager().getApplicationInfo(packageName, 0);
            String apkPath = ai.sourceDir;
            int position = apkPath.lastIndexOf('/');
            this.apkName = apkPath.substring(position + 1);

            // Read from odex file
            FileInputStream fis = new FileInputStream(odexFile);
            byte[] buf = new byte[64 * 1024]; // 64k should be enough
            int index = 0;
            int readlen;
            while (index < buf.length &&
                    (readlen = fis.read(buf, index, buf.length - index)) != -1) {
                index += readlen;
            }
            fis.close();

            // Search the checksum (search apk name or path)
            boolean checksumFound = searchChecksum(buf, apkName, bSaveChecksum);
            if (!checksumFound) {
                checksumFound = searchChecksum(buf, apkPath, bSaveChecksum);
            }

            return checksumFound;
        } catch (Exception e) {
            Log.d("DEBUG", "Exception: " + e.getMessage() + ": " + e);
            e.printStackTrace();
        }

        return false;
    }

    // Find the pattern of len and apk name
    private boolean searchChecksum(byte[] buf, String apkName, boolean bSaveChecksum) {
        int len = apkName.length();
        byte[] lenData = new byte[]{(byte) (len & 0xff), (byte) ((len >> 8) & 0xff),
                (byte) ((len >> 16) & 0xff), (byte) ((len >> 24) & 0xff),};
        for (int i = 0; i < buf.length - len - 16; ++i) {
            if (buf[i] == lenData[0] && buf[i + 1] == lenData[1]
                    && buf[i + 2] == lenData[2] && buf[i + 3] == lenData[3]) {
                byte[] nameData = apkName.getBytes();
                if (memcmp(buf, i + 4, nameData, 0, nameData.length) == 0) {
                    this.checksumOffset = i + 4 + nameData.length;
                    if (bSaveChecksum) {
                        this.crc32 = new byte[]{buf[i + 4 + nameData.length],
                                buf[i + 5 + nameData.length],
                                buf[i + 6 + nameData.length],
                                buf[i + 7 + nameData.length]};
//                            Log.d("DEBUG", String.format("checksum: %d %d %d %d",
//                                    crc32[0], crc32[1], crc32[2], crc32[3]));
                    }
                    //Log.d("DEBUG", "checksum offset = " + checksumOffset);
                    return true;
                }
            }
        }
        return false;
    }

    private int memcmp(byte[] buf1, int offset1, byte[] buf2, int offset2, int length) {
        for (int i = 0; i < length; ++i) {
            if (buf1[offset1 + i] == buf2[offset2 + i]) {
                continue;
            }
            if (buf1[offset1 + i] < buf2[offset2 + i]) {
                return -1;
            }
            if (buf1[offset1 + i] > buf2[offset2 + i]) {
                return 1;
            }
        }
        return 0;
    }

    private void dex2oat(Activity activity, String apkPath) {
        try {
            // Prepare the source apk
            String tmpDir = SDCard.makeWorkingDir(activity);
            String tmpApkPath = tmpDir + apkName;
            FileUtil.copyFile(apkPath, tmpApkPath);
            String tmpOdexPath = tmpDir + "odex";

            // run dex2oat
            String strArch = "";
            if (targetOdex.contains("/arm64/")) {
                strArch = " --instruction-set=arm64";
            }
            String command = "dex2oat --dex-file=" + apkName
                    + " --oat-file=" + tmpOdexPath + strArch;
            RootCommand rc = new RootCommand();
            rc.runRootCommand(command, null, 10 * 1000, tmpDir, false);

            // Remove the tmp apk
            new File(tmpApkPath).delete();

            // Patch to fix the checksum
            if (parseARTOdex(activity, tmpOdexPath, false)) {
                patchChecksum(tmpOdexPath);
            } else {
                errMessage = "Cannot fix the checksum.";
                return;
            }

            // Copy
            command = "cp " + tmpOdexPath + " " + targetOdex;
            rc.runRootCommand(command, null, 5000);
            //Log.d("DEBUG", command + ": " + rc.getStdError());

        } catch (Exception e) {
            e.printStackTrace();
            //Log.d("DEBUG", "Error: " + e.getMessage());
        }
    }

    private void patchChecksum(String odexPath) {
        try {
            RandomAccessFile raf = new RandomAccessFile(odexPath, "rw");
            raf.seek(checksumOffset);
            raf.write(crc32);
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}