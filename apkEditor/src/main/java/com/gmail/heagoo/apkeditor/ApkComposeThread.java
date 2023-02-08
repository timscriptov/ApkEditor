package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.ce.IApkMaking;
import com.gmail.heagoo.apkeditor.ce.IDescriptionUpdate;
import com.gmail.heagoo.apkeditor.smali.ISmaliAssembleCallback;
import com.gmail.heagoo.apkeditor.util.SignHelper;
import com.gmail.heagoo.common.CommandRunner;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.ITaskCallback;
import com.gmail.heagoo.common.ITaskCallback.TaskStepInfo;
import com.gmail.heagoo.common.SDCard;
import com.gmail.heagoo.common.TextFileReader;
import com.gmail.heagoo.common.ZipUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ApkComposeThread extends ComposeThread implements ISmaliAssembleCallback {

    public static String sysArch;
    private Context ctx;
    private String binRootPath;
    private String aaptPath;
    private String androidJarPath;
    private String decodedFilePath;
    private String srcApkPath;
    private String targetApkPath; // Target APK path
    // merge result
    private String tempApkPath; // if res modified, store the intermediate
    // Indicate succeed or not
    private boolean succeed;
    private String errMessage;
    // Record all the dex file replaces
    private Map<String, String> dexReplaces = new HashMap<String, String>();
    // String resource modified or not
    private boolean stringModified;
    // Manifest modified or not
    private boolean manifestModified;
    // resource file added/deleted
    private boolean resFileModified;
    // Samli file modified or not
    private List<String> modifiedSmaliFolders;
    // Add/Delete/Modify files
    private Map<String, String> addedFiles;
    private Map<String, String> replacedFiles;
    private Set<String> deletedFiles;
    private Map<String, String> fileEntry2ZipEntry;
    private ITaskCallback taskCallback;
    private TaskStepInfo stepInfo;
    // Last time updating the smali assemble info
    private long lastUpdateAssembleTime = 0;
    // Flag to control run or not
    private boolean stopFlag = false;
    private boolean bSignApk;
    private IApkMaking extraMaker;


    /**
     * @param ctx             Context
     * @param decodedFilePath path store all the decoded files
     * @param srcApkPath      where the source file is from
     * @param apkPath         target apk path
     */
    public ApkComposeThread(Context ctx, String decodedFilePath,
                            String srcApkPath, String apkPath) {
        this.ctx = ctx;

        File fileDir = ctx.getFilesDir();
        String rootDirectory = fileDir.getAbsolutePath();
        this.binRootPath = rootDirectory + "/bin";
        this.aaptPath = binRootPath + "/aapt";
        this.androidJarPath = binRootPath + "/android.jar";
        // this.androidJarPath = SDCard.getRootDirectory() + "/android.jar";
        this.decodedFilePath = decodedFilePath;
        this.srcApkPath = srcApkPath;
        this.targetApkPath = apkPath;
        this.tempApkPath = srcApkPath;

        LOGGER.info("aaptPath: " + this.aaptPath);
        LOGGER.info("androidJarPath: " + this.androidJarPath);
        LOGGER.info("decodedFilePath: " + this.decodedFilePath);

        this.stepInfo = new ITaskCallback.TaskStepInfo();
    }

    public static String getInstaller(Context ctx, String pkgName) {
        String installer = null;
        try {
            // String pkgName = ctx.getPackageName();
            String strPackage = "Package";
            Class<?> ctxCls = Context.class;

            // PackageManager pm = ctx.getPackageManager();
            StringBuffer sb2 = new StringBuffer();
            sb2.append('g');
            sb2.append('e');
            sb2.append('t');
            sb2.append(strPackage);
            sb2.append('M');
            sb2.append('a');
            sb2.append('n');
            sb2.append('a');
            sb2.append('g');
            sb2.append('e');
            sb2.append('r');
            Method method2 = ctxCls.getMethod(sb2.toString(), new Class[]{});
            PackageManager pm = (PackageManager) method2.invoke(ctx, new Object[]{});

            // pm.getInstallerPackageName(pkgName);
            Class<?> pmCls = PackageManager.class;
            StringBuffer sb = new StringBuffer();
            sb.append('g');
            sb.append('e');
            sb.append('t');
            sb.append("In");
            sb.append("stall");
            sb.append("er");
            sb.append(strPackage);
            sb.append('N');
            sb.append('a');
            sb.append('m');
            sb.append('e');
            Method method3 = pmCls.getMethod(sb.toString(),
                    new Class<?>[]{String.class});
            installer = (String) method3.invoke(pm, new Object[]{pkgName});
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (installer == null) {
            installer = "";
        }
        return installer;
    }

    public static boolean isProAndNoModification(Context ctx) {
        // Testing
        // for (android.content.pm.ApplicationInfo ai :
        // ctx.getPackageManager().getInstalledApplications(0)) {
        // String installer = getInstaller(ctx, ai.packageName);
        // Log.d("DEBUG", ai.packageName + " --> " + installer);
        // }

        boolean ret = false;
        try {
            // String apkPath = ctx.getApplicationInfo().sourceDir;
            // JarFile jarFile = new JarFile(apkPath);
            // ZipEntry entry = jarFile.getEntry("AndroidManifest.xml");
            // InputStream input = jarFile.getInputStream(entry);
            // AXMLSimpleParser parser = new AXMLSimpleParser();
            // parser.parse(input);
            // String packageName = parser.getPackageName();

            String packageName = ctx.getPackageName();
            String installer = getInstaller(ctx, packageName);
            StringBuffer sb = new StringBuffer();
            sb.append('i');
            sb.append('n');
            sb.append('g');
            if (installer.endsWith(sb.toString())
                    && packageName.endsWith("pro")) {
                ret = true;
            }
            // input.close();
            // jarFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    // Some aapt must pass "--no-version-vectors" option to get the correct result
    protected static boolean getNoVersionVectorOption(Context ctx, String aaptPath) {
        String configKey = "aapt-no-version-vectors";
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        int intVal = sp.getInt(configKey, -1);
        if (intVal == 1) {
            return true;
        } else if (intVal == 0) {
            return false;
        }

        String[] command = {aaptPath};
        CommandRunner cr = new CommandRunner();
        cr.runCommand(command, null, null, 5 * 1000, false);
        String strOut = cr.getStdOut();
        String strError = cr.getStdError();
        boolean option = ((strOut != null && strOut.contains("--no-version-vectors")) ||
                (strError != null && strError.contains("--no-version-vectors")));
        Editor editor = sp.edit();
        editor.putInt(configKey, option ? 1 : 0);
        editor.apply();
        return option;
    }

    // This method will extract the necessary files
    public static boolean prepare(Context ctx, String aaptPath, String binRootPath) throws Exception {
        String curVersion = null;
        try {
            PackageInfo pInfo = ctx.getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            curVersion = pInfo.versionName;
        } catch (NameNotFoundException e) {
        }

        // Prepare file
        SharedPreferences sp = ctx.getSharedPreferences("info", 0);
        boolean inited = sp.getBoolean("initialized", false);
        String lastVersion = sp.getString("version", "");
        if (!inited || !lastVersion.equals(curVersion)) {
            if (copyFiles(ctx, aaptPath, binRootPath)) {
                Editor editor = sp.edit();
                editor.putBoolean("initialized", true);
                editor.putString("version", curVersion);
                editor.commit();
                return true;
            }

            return false;
        }
        // Already inited before
        else {
            return true;
        }
    }

    // Copy aapt & android.jar
    private static boolean copyFiles(Context ctx, String aaptPath, String binRootPath) throws Exception {
        try {


            // Make parent directory if not exist
            File aaptFile = new File(aaptPath);
            File parentDir = aaptFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            AssetManager assets = ctx.getAssets();
            sysArch = detArchName();

            File aaptFile2 = new File(binRootPath);
            // copy_aapt(assets, aaptFile);
            copy_aapt2(assets, aaptFile2);

            // Copy aapt
//            InputStream input;
//            int isX86Val = MainActivity.isX86();
//            if (isX86Val != 0) {
//                if (Build.VERSION.SDK_INT >= 16) {
//                    input = ctx.getAssets().open("aapt-x86-pie");
//                } else {
//                    throw new Exception(Build.MODEL +
//                            "(SDK=" + Build.VERSION.SDK_INT + ") is not supported yet.");
//                }
//            } else {
//                if (Build.VERSION.SDK_INT >= 21) {
//                    input = ctx.getAssets().open("aapt7.1");
//                    //input = ctx.getAssets().open("aapt7-arm64");
//                } else if (Build.VERSION.SDK_INT >= 16) {
//                    input = ctx.getAssets().open("aapt6-arm32"); // PIE
//                } else {
//                    input = ctx.getAssets().open("aapt");
//                }
//            }
//            FileOutputStream output = new FileOutputStream(aaptFile);
//            IOUtils.copy(input, output);
//            input.close();
//            output.close();
//            aaptFile.setExecutable(true);

            // Copy android.jar
            copyAndroidjar(ctx, binRootPath);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Can not copy file: " + e.getMessage());
        }
    }

    private static void copy_aapt(AssetManager assets, File outDir) throws IOException {
        File aapt = new File(outDir, "aapt");
        InputStream aapt_in = assets.open(ApkComposeThread.sysArch + "/aapt");
        OutputStream aapu_out = new FileOutputStream(aapt);
        IOUtils.copy(aapt_in, aapu_out);
        aapt_in.close();
        aapu_out.close();
        aapt.setExecutable(true);
        //   Settings.aapt = aapt.getAbsolutePath();
    }

    private static void copy_aapt2(AssetManager assets, File outDir) throws IOException {
        File aapt2 = new File(outDir, "aapt");
        InputStream aapt2_in = assets.open(ApkComposeThread.sysArch + "/aapt");
        OutputStream aapt2_out = new FileOutputStream(aapt2);
        IOUtils.copy(aapt2_in, aapt2_out);
        aapt2_in.close();
        aapt2_out.close();
        aapt2.setExecutable(true);
        //   Settings.aapt2 = aapt2.getAbsolutePath();
    }

    //    private static String detArchName() {
//        if (Build.CPU_ABI.equals("arm64-v8a")) {
//
//            return "arm64-v8a";
//        }
//        if (Build.CPU_ABI.equals("armeabi-v7a")) {
//            return "armeabi-v7a";
//        }
//        if (Build.CPU_ABI.equals("x86_64")) {
//            return "x86_64";
//        }
//        if (Build.CPU_ABI.equals("x86")) {
//            return "x86";
//        }
//
//        return "armeabi-v7a";
//    }
    private static String detArchName() {
        String arch = Build.CPU_ABI;
        if (arch.equals("arm64-v8a")) {
            return "arm64-v8a";
        } else if (arch.equals("armeabi-v7a")) {
            return "armeabi-v7a";
        } else if (arch.equals("x86_64")) {
            return "x86_64";
        } else if (arch.equals("x86")) {
            return "x86";
        } else {
            return "armeabi-v7a";
        }
    }

    // Copy android.jar
    private static void copyAndroidjar(Context ctx, String binRootPath) throws IOException {
        // Copy to tmp.zip
        String assetName = "android.zip";
        InputStream input = ctx.getAssets().open(assetName);
        String zipFilePath = binRootPath + "/tmp.zip";
        FileOutputStream output = new FileOutputStream(
                binRootPath + "/tmp.zip");
        IOUtils.copy(input, output);
        input.close();
        output.close();

        // unzip
        ZipUtil.unzipNoThrow(zipFilePath, binRootPath);

        // Delete tmp.zip
        File f = new File(zipFilePath);
        f.delete();

        // unzip again
        ZipUtil.unzipNoThrow(binRootPath + "/android.jar", binRootPath);
    }

    // resReplaces contains non-xml replaces
    // resFileModified means res added/deleted, or xml changed
    @Override
    public void setModification(boolean strModified, boolean manifestModified,
                                boolean resFileModified, List<String> modifiedSmaliFolders,
                                Map<String, String> addedFiles, Map<String, String> replacedFiles,
                                Set<String> deletedFiles, Map<String, String> fileEntry2ZipEntry,
                                boolean bSignApk) {

        this.stringModified = strModified;
        this.manifestModified = manifestModified;
        this.resFileModified = resFileModified;
        this.modifiedSmaliFolders = modifiedSmaliFolders;
        this.addedFiles = addedFiles;
        this.deletedFiles = deletedFiles;
        this.replacedFiles = replacedFiles;
        this.fileEntry2ZipEntry = fileEntry2ZipEntry;
        this.bSignApk = bSignApk;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        boolean rebuildResNeeded = isResourceModified();
        // Sign & Cleanup
        if (BuildConfig.WITH_SIGN) {
            this.stepInfo.stepTotal = (bSignApk ? 2 : 1);
        } else {
            this.stepInfo.stepTotal = 1;
        }
        // Need to compile the resource
        if (rebuildResNeeded)
            stepInfo.stepTotal += 2;
        // For DEX assembling
        if (this.modifiedSmaliFolders != null
                && !this.modifiedSmaliFolders.isEmpty())
            stepInfo.stepTotal += this.modifiedSmaliFolders.size();

        do {


            // XML/String/Manifest modified, requires re-compiling
            if (rebuildResNeeded) {
                if (stopFlag) {
                    this.errMessage = "User request to stop";
                    break;
                }
                setNextStep(ctx.getString(R.string.compose));

                if (!prepare()) {
                    break;
                }

                // Compose resource and extract files
                if (stopFlag) {
                    this.errMessage = "User request to stop";
                    break;
                }
                if (!composeResource()) {
                    break;
                }
            }
            // Assemble DEX files
            if (this.modifiedSmaliFolders != null
                    && !this.modifiedSmaliFolders.isEmpty()) {
                boolean assembleError = false;
                for (String smaliFolder : modifiedSmaliFolders) {
                    String smaliPath = decodedFilePath + "/" + smaliFolder;
                    String dexName = getDexNameBySmaliFolder(smaliFolder);
                    setNextStep(ctx.getString(R.string.assemble_dex_file) + ": " + dexName);
                    try {
                        // Log.d("DEBUG", "Assemble " + smaliPath + " to " +
                        // dexName);
                        assembleSmali(smaliPath, dexName);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        this.errMessage = e.getMessage();
                        assembleError = true;
                        break;
                    }

                    if (stopFlag) {
                        this.errMessage = "User request to stop";
                        assembleError = true;
                        break;
                    }
                }

                if (assembleError)
                    break;
            }

            if (rebuildResNeeded) {
                if (stopFlag) {
                    this.errMessage = "User request to stop";
                    break;
                }
                setNextStep(ctx.getString(R.string.merge));

                try {
                    // if (isProAndNoModification()) {
                    long start = System.currentTimeMillis();
                    mergeApk();
                    //Log.e("DEBUG", "Merge Time: " + (System.currentTimeMillis() - start));
                    // } else {
                    // mergeApkFreeVersion();
                    // }
                    // Must applied, otherwise may replace it again when sign
                    removeResChanges();
                } catch (Exception e) {
                    e.printStackTrace();
                    this.errMessage = ctx.getString(R.string.merge) + ": " + e.getMessage();
                    break;
                }
            }
            // Do not need to rebuild resource, thus need to translate the
            // modification
            else {
                translate2OriginEntry();
            }

            // Sign or not
            if (BuildConfig.WITH_SIGN && bSignApk) {
                if (stopFlag) {
                    this.errMessage = "User request to stop";
                    break;
                }
                setNextStep(ctx.getString(R.string.sign));
                if (!signApk()) {
                    break;
                }
            } else {
                notSignApk();
            }

            // Clean up
            if (stopFlag) {
                this.errMessage = "User request to stop";
                break;
            }
            setNextStep(ctx.getString(R.string.cleanup));
            cleanup();

            // For free version, make sure it longer enough, so that ad could be loaded
            if (!BuildConfig.IS_PRO) {
                long curTime = System.currentTimeMillis();
                if (curTime - startTime < 7500) {
                    try {
                        Thread.sleep(7500 - (curTime - startTime));
                    } catch (InterruptedException igonred) {
                    }
                }
            }

            this.succeed = true;
        } while (false);

        if (!stopFlag) {
            if (succeed) {
                taskCallback.taskSucceed();
            } else {
                taskCallback.taskFailed(errMessage);
            }
        }
    }

    // When add/delete/modify some general images, rebuild is not need
    // But in sign step, we need to do the modification
    // Here we must translate it back to the original entry name
    private void translate2OriginEntry() {
        // Do not need to translate
        if (fileEntry2ZipEntry == null || fileEntry2ZipEntry.isEmpty()) {
            return;
        }

        // Translate add records
        if (!this.addedFiles.isEmpty()) {
            Map<String, String> newAdded = new HashMap<>();
            for (Map.Entry<String, String> entry : this.addedFiles.entrySet()) {
                String newKey = fileEntry2ZipEntry.get(entry.getKey());
                if (newKey != null) {
                    newAdded.put(newKey, entry.getValue());
                } else {
                    newAdded.put(entry.getKey(), entry.getValue());
                }
            }
            this.addedFiles = newAdded;
        }

        // Translate replace records
        if (!this.replacedFiles.isEmpty()) {
            Map<String, String> newReplace = new HashMap<>();
            for (Map.Entry<String, String> entry : this.replacedFiles
                    .entrySet()) {
                String newKey = fileEntry2ZipEntry.get(entry.getKey());
                if (newKey != null) {
                    newReplace.put(newKey, entry.getValue());
                } else {
                    newReplace.put(entry.getKey(), entry.getValue());
                }
            }
            this.replacedFiles = newReplace;
        }

        // Translate delete records
        if (!this.deletedFiles.isEmpty()) {
            Set<String> newDelete = new HashSet<String>();
            for (String entry : deletedFiles) {
                String newEntry = fileEntry2ZipEntry.get(entry);
                if (newEntry != null) {
                    newDelete.add(newEntry);
                } else {
                    newDelete.add(entry);
                }
            }
            this.deletedFiles = newDelete;
        }
    }

    // Delete resource changes, as the resource change is already applied
    private void removeResChanges() {
        removeResInMap(this.addedFiles);
        removeResInMap(this.replacedFiles);

        Iterator<String> it = this.deletedFiles.iterator();
        while (it.hasNext()) {
            if (it.next().startsWith("res/")) {
                it.remove();
            }
        }
    }

    /*
     * protected boolean mergeApk_Java() throws IOException { String resourceApk
     * = this.tempApkPath; JarFile resJar = new JarFile(new File(resourceApk),
     * false); JarFile originJar = new JarFile(new File(srcApkPath), false);
     *
     * String outputPath = this.tempApkPath + ".out"; OutputStream outputStream
     * = new FileOutputStream(outputPath); JarOutputStream outputJar = new
     * JarOutputStream(outputStream); outputJar.setLevel(1);
     *
     * JarEntry je;
     *
     * // Copy resource to intermediate file Enumeration<JarEntry> it =
     * resJar.entries(); while (it.hasMoreElements()) { JarEntry entry =
     * it.nextElement(); je = new JarEntry(entry);
     *
     * InputStream in = null; String name = entry.getName(); // Is the common
     * image if (name.endsWith(".jpg") || (name.endsWith(".png") &&
     * !name.endsWith(".9.png"))) { // Not in add and replace collections //
     * Then use the content in original apk/jar if
     * (!addedFiles.containsKey(name) && !replacedFiles.containsKey(name)) {
     * String originName = this.fileEntry2ZipEntry.get(name); if (originName ==
     * null) { originName = name; } ZipEntry ze =
     * originJar.getEntry(originName); if (ze != null) { // ze may be null like
     * QuickPic je.setMethod(ze.getMethod()); je.setSize(ze.getSize());
     * je.setCompressedSize(ze.getCompressedSize()); je.setCrc(ze.getCrc());
     * je.setExtra(ze.getExtra()); je.setComment(ze.getComment()); in =
     * originJar.getInputStream(ze); } else { continue; } } }
     *
     * if (in == null) { in = resJar.getInputStream(entry); }
     *
     * // in is still null, cannot do the copy if (in == null) { continue; }
     *
     * outputJar.putNextEntry(je);
     *
     * IOUtils.copy(in, outputJar); in.close(); }
     *
     * // Copy other files it = originJar.entries(); while
     * (it.hasMoreElements()) { JarEntry entry = it.nextElement();
     *
     * // Skip res/resources.arsc/AndroidManifest.xml String name =
     * entry.getName(); if (name.startsWith("res/") ||
     * name.equals("resources.arsc") || name.equals("AndroidManifest.xml")) {
     * continue; }
     *
     * InputStream in = originJar.getInputStream(entry);
     *
     * je = new JarEntry(entry); outputJar.putNextEntry(je);
     *
     * IOUtils.copy(in, outputJar); in.close(); }
     *
     * outputJar.close(); outputStream.close(); originJar.close();
     * resJar.close();
     *
     * new File(this.tempApkPath).delete(); new File(outputPath).renameTo(new
     * File(this.tempApkPath));
     *
     * return false; }
     */

    // Copy the compiled file from target Apk path after compose
    // Merge 2 jar files
    // Seems not used any more
    // private void extractCompiledFile() throws IOException {
    // JarFile inputJar = new JarFile(new File(targetApkPath), false);
    //
    // // When xmlGotoResource=true, it means the xml file does not
    // // independently exist in the apk
    // boolean xmlGotoResource = false;
    // for (String entryName : xmlReplaces.keySet()) {
    // String filePath = xmlReplaces.get(entryName);
    // JarEntry inEntry = inputJar.getJarEntry(entryName);
    // if (inEntry != null) {
    // compiledResourceFiles.put(entryName, filePath);
    // InputStream in = inputJar.getInputStream(inEntry);
    // FileOutputStream out = new FileOutputStream(filePath);
    // IOUtils.copy(in, out);
    // in.close();
    // out.close();
    // } else {
    // xmlGotoResource = true;
    // }
    // }
    //
    // if (this.stringModified || xmlGotoResource) {
    // compiledResourceFiles.put("resources.arsc", this.decodedFilePath
    // + "/resources.arsc");
    // JarEntry inEntry = inputJar.getJarEntry("resources.arsc");
    // InputStream in = inputJar.getInputStream(inEntry);
    // FileOutputStream out = new FileOutputStream(this.decodedFilePath
    // + "/resources.arsc");
    // IOUtils.copy(in, out);
    // in.close();
    // out.close();
    // }
    //
    // if (this.manifestModified) {
    // JarEntry inEntry = inputJar.getJarEntry("AndroidManifest.xml");
    // InputStream in = inputJar.getInputStream(inEntry);
    // FileOutputStream out = new FileOutputStream(this.decodedFilePath
    // + "/AndroidManifest.xml");
    // IOUtils.copy(in, out);
    // out.close();
    // }
    //
    // inputJar.close();
    // }

    private void removeResInMap(Map<String, String> data) {
        Iterator<Map.Entry<String, String>> it = data.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            if (entry.getKey().startsWith("res/")) {
                it.remove();
            }
        }
    }

    private String getDexNameBySmaliFolder(String smaliFolder) {
        if ("smali".equals(smaliFolder)) {
            return "classes.dex";
        }
        if (smaliFolder.startsWith("smali_")) {
            return smaliFolder.substring("smali_".length()) + ".dex";
        }
        return smaliFolder + ".dex";
    }

    private void setNextStep(String description) {
        stepInfo.stepIndex += 1;
        stepInfo.stepDescription = description;
        taskCallback.setTaskStepInfo(stepInfo);
    }

    // Assemble smali to DEX
    private void assembleSmali(String smaliFilePath, String dexFileName)
            throws Throwable {
        String dexFilePath = getPathInSameDirectory(this.targetApkPath,
                dexFileName);

        // Log.d("DEBUG", "assemble " + smaliFilePath + " to " + dexFilePath +
        // ", dexFileName=" + dexFileName);

        // Invoke DexEncoder.smali2Dex
        try {
            long start = System.currentTimeMillis();
            Class<?> obj_class = Class
                    .forName("com.gmail.heagoo.apkeditor.pro.DexEncoder");
            Method method = obj_class.getMethod("smali2Dex", String.class, String.class, ISmaliAssembleCallback.class);
            method.invoke(null, smaliFilePath, dexFilePath, this);
            Log.i("DEBUG", "Encode time=" + (System.currentTimeMillis() - start));

            // Record the replace
            this.dexReplaces.put(dexFileName, dexFilePath);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private String getPathInSameDirectory(String path, String name) {
        int position = path.lastIndexOf('/');
        return path.substring(0, position + 1) + name;
    }

    private void cleanup() {
        // Delete the intermediate file
        if (!tempApkPath.equals(srcApkPath)) {
            File f = new File(tempApkPath);
            f.delete();
        }

        // Delete all the decoded files
        // Do not delete the res directory any more, as the project must keep it
        CommandRunner cr = new CommandRunner();
//        cr.runCommand("rm -rf " + decodedFilePath + "/res", null, 10000);

        // Clean /sdcard/ApkEditor/tmp
        try {
            String tmpDir = SDCard.getRootDirectory() + "/ApkEditor/tmp";
            cr.runCommand("rm -rf " + tmpDir, null, 10000);
        } catch (Exception e) {
        }

        // Log.d("DEBUG", "decodedFilePath" + decodedFilePath);
    }

    protected void mergeApk() {
        // Before merge, call the extra maker
        Map<String, String> extraReplaces = new HashMap<>();
        if (this.extraMaker != null) {
            try {
                extraMaker.prepareReplaces(ctx, tempApkPath, extraReplaces,
                        // Note: currently not support description update
                        new IDescriptionUpdate() {
                            @Override
                            public void updateDescription(String strDesc) {
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        int mappingLen = 0;
        StringBuilder sb1 = new StringBuilder();
        for (Map.Entry<String, String> entry : fileEntry2ZipEntry.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb1.append(key);
            sb1.append('\n');
            sb1.append(value);
            sb1.append('\n');
            mappingLen += key.getBytes().length + value.getBytes().length + 2;
        }

        int replaceLen = 0;
        StringBuilder sb2 = new StringBuilder();
        for (Map.Entry<String, String> entry : addedFiles.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            // For xml files, not replace it, as already compiled it into AXML
            if (key.startsWith("res/") && key.endsWith(".xml")) {
                continue;
            }
            sb2.append(key);
            sb2.append('\n');
            sb2.append(value);
            sb2.append('\n');
            replaceLen += key.getBytes().length + value.getBytes().length + 2;
        }
        for (Map.Entry<String, String> entry : replacedFiles.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.startsWith("res/")) {
                // For xml files, not replace it, as already compiled it into AXML
                if (key.endsWith(".xml")) {
                    continue;
                }
                // For 9.png files, not replace it, as already it is already compiled
                if (key.endsWith(".9.png")) {
                    continue;
                }
            }

            sb2.append(key);
            sb2.append('\n');
            sb2.append(value);
            sb2.append('\n');
            replaceLen += key.getBytes().length + value.getBytes().length + 2;
        }
        for (Map.Entry<String, String> entry : extraReplaces.entrySet()) {
            // The extra one must be replaced
//            String key = entry.getKey();
//            if (key.startsWith("res/") && key.endsWith(".xml")) {
//                continue;
//            }
            String key = entry.getKey();
            String value = entry.getValue();
            sb2.append(entry.getKey());
            sb2.append('\n');
            sb2.append(entry.getValue());
            sb2.append('\n');
            replaceLen += key.getBytes().length + value.getBytes().length + 2;
        }

        MainActivity.mg(tempApkPath, srcApkPath,
                sb2.toString(), replaceLen,
                sb1.toString(), mappingLen
        );
    }

    private boolean isResourceModified() {
        return (this.stringModified || this.manifestModified
                || this.resFileModified);
    }

    private boolean signApk() {
        // When smali code is edited, also replace classes.dex
        replacedFiles.putAll(this.dexReplaces);

        try {
            SignHelper.sign(ctx, tempApkPath, targetApkPath, replacedFiles,
                    addedFiles, deletedFiles);
            // AssetManager am = ctx.getAssets();
            // int level = SettingActivity.getCompressionLevel(ctx);
            // // String keyFile = "testkey";
            // String keyName = SettingActivity.getSignKeyName(ctx);
            // String keyFile = CheckUtil.getTargetString(ctx, keyName);
            // InputStream publicKeyInput = am.open(keyFile + ".x509.pem");
            // InputStream privateKeyInput = am.open(keyFile + ".pk8");
            // SignApk.signAPK(publicKeyInput, privateKeyInput, intermediateApk,
            // targetApkPath, allRepalces, level);
            return true;
        } catch (Exception e) {
            String strHeader = ctx.getResources()
                    .getString(R.string.sign_error);
            this.errMessage = strHeader + e.getMessage();
            //           Log.d("sawsem", e.getMessage());
        }

        return false;
    }

    private boolean notSignApk() {
        replacedFiles.putAll(this.dexReplaces);

        StringBuilder sb1 = new StringBuilder();
        int addLen = 0;
        for (Map.Entry<String, String> entry : addedFiles.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb1.append(key);
            sb1.append('\n');
            sb1.append(value);
            sb1.append('\n');
            addLen += key.getBytes().length + value.getBytes().length + 2;
        }

        StringBuilder sb2 = new StringBuilder();
        int deleteLen = 0;
        for (String entry : deletedFiles) {
            sb2.append(entry);
            sb2.append('\n');
            deleteLen += entry.getBytes().length + 1;
        }

        StringBuilder sb3 = new StringBuilder();
        int replaceLen = 0;
        for (Map.Entry<String, String> entry : replacedFiles.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb3.append(key);
            sb3.append('\n');
            sb3.append(value);
            sb3.append('\n');
            replaceLen += key.getBytes().length + value.getBytes().length + 2;
        }

        MainActivity.md(targetApkPath, tempApkPath, sb1.toString(), addLen,
                sb2.toString(), deleteLen, sb3.toString(), replaceLen);
        return true;
    }

    private boolean composeResource() {
        this.tempApkPath = targetApkPath + ".in";
        boolean noVersionVectorOption = getNoVersionVectorOption(ctx, aaptPath);
        String[] command1 = {aaptPath, "package", "-f", "-I", androidJarPath,
                "-S", decodedFilePath + "/res", "-M",
                decodedFilePath + "/AndroidManifest.xml", "-F", tempApkPath};
        String[] command2 = {aaptPath, "package", "-f", "-I", androidJarPath,
                "-S", decodedFilePath + "/res", "-M",
                decodedFilePath + "/AndroidManifest.xml", "-F", tempApkPath,
                "--no-version-vectors"};

        long startTime = System.currentTimeMillis();
        CommandRunner cr = new CommandRunner();
        boolean ret = cr.runCommand(noVersionVectorOption ? command2 : command1,
                null, null, 300 * 1000, true);
        //Log.e("DEBUG", "aapt Time: " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");

        LOGGER.info("stdout: " + cr.getStdOut() + ", ret=" + ret);
        if (!ret) {
            LOGGER.info("stderr: " + cr.getStdError());
            this.errMessage = cr.getStdError();
            return false;
        }

        return true;
    }

    public boolean prepare() {
        try {
            return prepare(ctx, aaptPath, binRootPath);
        } catch (Exception e) {
            this.errMessage = e.getMessage();
            return false;
        }
    }

    private boolean isArm64() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }
        try {
            TextFileReader reader = new TextFileReader("/proc/cpuinfo");
            String content = reader.getContents();
            return content.contains("AArch64") || content.contains("aarch64");
        } catch (IOException ignored) {
        }
        return false;
    }

    @Override
    public void setTaskCallback(ITaskCallback taskCallback) {
        this.taskCallback = taskCallback;
    }

    @Override
    public void updateAssembledFiles(int assembledFiles, int totalFiles) {
        long curTime = System.currentTimeMillis();
        if (curTime > this.lastUpdateAssembleTime + 500) {
            String fmt = ctx.getString(R.string.assemble_dex_detail);
            stepInfo.stepDescription = String.format(fmt, assembledFiles,
                    totalFiles);
            taskCallback.setTaskStepInfo(stepInfo);
        }
    }

    @Override
    public void stopRunning() {
        this.stopFlag = true;
        this.interrupt();
    }

    @Override
    public void setExtraMaker(IApkMaking extraMaker) {
        this.extraMaker = extraMaker;
    }

    public String getErrMessage() {
        return errMessage;
    }
}
