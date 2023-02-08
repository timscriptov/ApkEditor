package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;

import com.gmail.heagoo.apkeditor.smali.ResSmaliIdProvider;
import com.gmail.heagoo.apklib.ManifestInfoCollector;
import com.gmail.heagoo.common.FileUtil;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.RefInvoke;
import com.gmail.heagoo.common.SDCard;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.decoder.ARSCDecoder;
import brut.androlib.res.util.ExtFile;
import brut.directory.Directory;

public class ApkParseThread extends Thread {

    // ??
    public static boolean sKeepBroken = false;
    public static boolean isProAndNoModification = true;

    private WeakReference<Activity> activityRef;
    private WeakReference<ApkParseConsumer> consumerRef;
    private String apkPath;
    private String decodeRootPath;

    // Record resource information
    private ResPackage apkPackage;
    private ResTable resTable;

    private String errMessage;

    private ApkDecoderMine decoder;

    private ResSmaliIdProvider idProvider;

    // Protected or not (not contain "res" means protected)
    private boolean mApkProtected;

    // Full decoding means to decode all the files include images, assets, libs, and unknown files
    private boolean isFullDecoding;

    public ApkParseThread(Activity ctx, ApkParseConsumer consumer,
                          String apkPath, String decodeRootPath,
                          boolean isFullDecoding) {
        this.activityRef = new WeakReference<>(ctx);
        this.consumerRef = new WeakReference<>(consumer);
        this.apkPath = apkPath;
        this.decodeRootPath = decodeRootPath;
        this.isFullDecoding = isFullDecoding;
    }

    @Override
    public void run() {
        // Play tricks to extract files: borrow ApkComposeThread to extract files
        ApkComposeThread tmp = new ApkComposeThread(activityRef.get(), null, null, null);
        boolean ret = tmp.prepare();
        if (!ret) {
            consumerRef.get().decodeFailed(tmp.getErrMessage());
            return;
        }

        ret = parse();
        if (!ret) {
            if (consumerRef.get() != null)
                consumerRef.get().decodeFailed(errMessage);
        }
    }

    private void readAssetFile(Context ctx, String filename, byte[] data)
            throws IOException {
        AssetManager am = ctx.getAssets();
        InputStream input = am.open(filename);
        int readLen = input.read(data);
        while (readLen < data.length) {
            int curRead = input.read(data, readLen, data.length - readLen);
            if (curRead == -1) {
                break;
            }
            readLen += curRead;
        }
        input.close();
    }

    private boolean parse() {
        try {
            Activity activity = activityRef.get();
            byte[] dummyPng = null;
            byte[] dummyJpg = null;
            byte[] dummy9Png = null;
            if (!isFullDecoding) {
                dummyPng = new byte[68];
                readAssetFile(activity, "dummy_png.png", dummyPng);
                dummyJpg = new byte[667];
                readAssetFile(activity, "dummy_jpg.jpg", dummyJpg);
                dummy9Png = new byte[97];
                readAssetFile(activity, "dummy.9.png", dummy9Png);
            }

            ExtFile apkFile = new ExtFile(new File(apkPath));

            // Possible protected by AndResGuard
            Directory apkDir = apkFile.getDirectory();
            this.mApkProtected = !apkDir.containsDir("res");
            if (mApkProtected && (apkDir.containsDir("r") || apkDir.containsDir("R"))) {
                try {
                    InputStream in = apkDir.getFileInput("AndroidManifest.xml");
                    ManifestInfoCollector collector = new ManifestInfoCollector();
                    collector.parse(in, "manifest", "package");
                    String pkgName = collector.getValue();
                    in.close();

                    in = apkDir.getFileInput("classes.dex");
                    String workDir = SDCard.makeWorkingDir(activity);
                    String dexFilePath = workDir + "classes.dex";
                    OutputStream out = new FileOutputStream(dexFilePath);
                    IOUtils.copy(in, out);
                    in.close();
                    out.close();

                    RefInvoke.invokeStaticMethod(
                            "com.gmail.heagoo.apkeditor.pro.ResourceDecoder",
                            "decodeResources",
                            new Class<?>[]{String.class, String.class},
                            new Object[]{dexFilePath, workDir});
                    this.idProvider = new ResSmaliIdProvider(workDir, pkgName);
                } catch (Exception e) {
                }
            }

            // After decoding resource table, show string list
            this.resTable = getResTable(apkFile, true);
            if (consumerRef.get() != null) {
                consumerRef.get().resTableDecoded(true);
            }

            this.decoder = new ApkDecoderMine(resTable, dummyPng, dummyJpg, dummy9Png);

            // CommandRunner cr = new CommandRunner();
            // String[] command = { "sh", "-c", "rm -rf " + decodeRootPath };
            // cr.runCommand(command, null, 10000);
            FileUtil.deleteAll(new File(decodeRootPath));
            // LOGGER.info(decodeRootPath + " deleted!", true);

            // To protected me, detect whether installed from google play
            isProAndNoModification = ApkComposeThread
                    .isProAndNoModification(activityRef.get());

            File outDir = new File(decodeRootPath);
            if (!outDir.exists()) {
                boolean ret = outDir.mkdirs();
                //Log.d("DEBUG", "mkdir return " + ret);
            }

            // File outDir = new File("/storage/emulated/0/decoded/");
            decoder.decode(apkFile, outDir);
            if (consumerRef.get() != null) {
                consumerRef.get()
                        .resourceDecoded(decoder.getFileEntry2ZipEntry());
            }

            return true;
        } catch (Exception e) {
            errMessage = e.getMessage();
            e.printStackTrace();
        }

        return false;
    }

    private ResTable getResTable(ExtFile apkFile, boolean loadMainPkg)
            throws AndrolibException {
        Activity activity = activityRef.get();
        ResTable resTable = new ResTable(activity.getApplicationContext(), mApkProtected);
        if (loadMainPkg) {
            loadMainPkg(resTable, apkFile);
        }
        return resTable;
    }

    private ResPackage loadMainPkg(ResTable resTable, ExtFile apkFile)
            throws AndrolibException {
        LOGGER.info("Loading resource table of apk file...");
        ResPackage[] pkgs = getResPackagesFromApk(apkFile, resTable,
                sKeepBroken);

        if (pkgs == null) {
            return null;
        }

        switch (pkgs.length) {
            case 1:
                this.apkPackage = pkgs[0];
                break;
            case 2:
                if (pkgs[0].getName().equals("android")) {
                    LOGGER.warning("Skipping \"android\" package group");
                    this.apkPackage = pkgs[1];
                } else if (pkgs[0].getName().equals("com.htc")) {
                    LOGGER.warning("Skipping \"htc\" package group");
                    this.apkPackage = pkgs[1];
                }
                break;
        }

        if (this.apkPackage == null) {
            throw new AndrolibException(
                    "Arsc files with zero or multiple packages");
        }

        resTable.addPackage(this.apkPackage, true);
        LOGGER.info("Loaded.");
        return this.apkPackage;
    }

    private ResPackage[] getResPackagesFromApk(ExtFile apkFile,
                                               ResTable resTable, boolean keepBroken) throws AndrolibException {
        // try {
        // InputStream fis =
        // apkFile.getDirectory().getFileInput("resources.arsc");
        //
        // return ARSCDecoder.decode(
        // fis, false, keepBroken, resTable).getPackages();
        // } catch (DirectoryException ex) {
        // throw new AndrolibException(
        // "Could not load resources.arsc from file: " + apkFile, ex);
        // }
        ZipFile zipFile = null;
        ByteArrayInputStream ais = null;
        try {
            zipFile = new ZipFile(apkFile);
            ZipEntry entry = zipFile.getEntry("resources.arsc");
            if (entry != null) {
                int size = (int) entry.getSize();
                byte[] data = new byte[size];
                IOUtils.readFully(zipFile.getInputStream(entry), data);

                ais = new ByteArrayInputStream(data);
                return ARSCDecoder
                        .decode(ais, false, keepBroken, resTable, idProvider, mApkProtected)
                        .getPackages();
            }
        } catch (IOException e) {
            throw new AndrolibException(
                    "Could not read resources.arsc from file: " + apkFile, e);
        } finally {
            try {
                zipFile.close();
            } catch (IOException e) {
            }
            if (ais != null)
                try {
                    ais.close();
                } catch (IOException e) {
                }
        }
        return null;
    }

    public ResTable getResTable() {
        return resTable;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public String getResourceRootPath() {
        return decodeRootPath + "/res";
    }

    public ResPackage getApkPackage() {
        return apkPackage;
    }

    public void stopParse() {
        if (this.decoder != null) {
            decoder.stopDecode();
        }
    }
}
