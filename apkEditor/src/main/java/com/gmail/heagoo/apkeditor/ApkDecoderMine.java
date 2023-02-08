package com.gmail.heagoo.apkeditor;

import com.gmail.heagoo.apkeditor.XmlDecoder.IReferenceDecoder;
import com.gmail.heagoo.apkeditor.util.TimeDumper;
import com.gmail.heagoo.apklib.AXMLParser.IReferenceDecode;
import com.gmail.heagoo.common.FileUtil;

import org.xmlpull.v1.XmlSerializer;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import brut.androlib.AndrolibException;
import brut.androlib.err.CantFind9PatchChunk;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResSpec;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.data.ResValuesFile;
import brut.androlib.res.data.value.ResBoolValue;
import brut.androlib.res.data.value.ResFileValue;
import brut.androlib.res.data.value.ResValue;
import brut.androlib.res.decoder.Res9patchStreamDecoder;
import brut.androlib.res.decoder.ResRawStreamDecoder;
import brut.androlib.res.decoder.ResStreamDecoderContainer;
import brut.androlib.res.util.ExtFile;
import brut.androlib.res.xml.ResValuesXmlSerializable;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.FileDirectory;
import brut.util.KXmlSerializer;

public class ApkDecoderMine implements IReferenceDecoder, IReferenceDecode {

    private final static String[] APK_STANDARD_FILES = new String[]{
            "classes.dex", "AndroidManifest.xml", "resources.arsc",
    };
    //    private final static String[] APK_STANDARD_DIRS = new String[]{
//            "res", "r", "R",
//            "lib", "libs", "assets", "META-INF", "kotlin",
//    };
    private final static String[] APK_STANDARD_DIRS = new String[]{
            "res", "r", "R",
            "lib", "libs", "assets", "kotlin",
    };
    private ResTable resTable;
    private XmlDecoder xmlDecoder;
    private ResStreamDecoderContainer mDecoders = new ResStreamDecoderContainer();
    // Resource may contained in "r" directory
    private String resDirName = "";
    // Flag to stop running
    private boolean stopRunning = false;
    private byte[] dummyPng;
    private byte[] dummyJpg;
    private byte[] dummy9Png;
    // Record all the file entry to zip entry
    // like res/drawable-hdpi-v4/a.png -> res/drawable-hdpi/a.png
    private Map<String, String> fileEntry2ZipEntry = new HashMap<>();
    private String packageName;
    // Is full decoding or not, full decoding means decode all the files
    private boolean isFullDecoding;

    public ApkDecoderMine(ResTable resTable, byte[] dummyPng, byte[] dummyJpg, byte[] dummy9Png) {
        this.resTable = resTable;
        this.dummyPng = dummyPng;
        this.dummyJpg = dummyJpg;
        this.dummy9Png = dummy9Png;

        // When no dummy images, then means to decode all the files
        this.isFullDecoding = (dummyJpg == null && dummyPng == null);

        this.xmlDecoder = new XmlDecoder(this, getResPackage());
        mDecoders.setDecoder("xml", xmlDecoder);
        Res9patchStreamDecoder _9pathDecoder = new Res9patchStreamDecoder();
        mDecoders.setDecoder("9path", _9pathDecoder);
        ResRawStreamDecoder rawDecoder = new ResRawStreamDecoder();
        mDecoders.setDecoder("raw", rawDecoder);
    }

    private static String getPackageName(int id) {
        if (id >>> 24 == 1) {
            return "android:";
        }
        return "";
    }

    public Map<String, String> getFileEntry2ZipEntry() {
        return fileEntry2ZipEntry;
    }

    // Root interface of the decode
    public void decode(ExtFile apkFile, File outDir) throws Exception {
        Directory inApk = apkFile.getDirectory();
        if (!inApk.containsDir("res")) {
            this.xmlDecoder.setApkProtected(true);
        }

        TimeDumper timer = new TimeDumper(false);
        if (!stopRunning) {
            this.packageName = decodeManifest(apkFile, outDir);
        }
        timer.lastTime("Manifest Decode Time");

        // Delete res directory
        // String command = "rm -rf " + outDir.getPath() + "/res";
        // CommandRunner cr = new CommandRunner();
        // cr.runCommand(new String[] {"sh", "-c", command}, null, 10 * 1000);
        try {
            FileUtil.deleteAll(new File(outDir.getPath() + "/res"));
        } catch (Exception e) {
        }
        LOGGER.info(outDir.getPath() + "/res" + " deleted!");

        File resDir = new File(outDir, "res");
        if (!resDir.exists())
            resDir.mkdirs();

        timer.lastTime("Dir Prepare Time");

        // Firstly to decode the file resources, so that missed raw files can be
        // saved to xml values
        if (!stopRunning) {
            FileDirectory out = new FileDirectory(resDir);
            decodeFileResources(apkFile, out);
        }

        LOGGER.info("file-resources decoded!");
        timer.lastTime("Resource Decode Time");
        // startTime = stopTime;

        if (!stopRunning)
            decodeValues(resDir);
        if (!stopRunning)
            generatePublicXml(resDir);

        timer.lastTime("Value Decode Time");

        // Decode other files
        if (isFullDecoding) {
            if (!stopRunning)
                decodeRawFiles(apkFile, outDir);
            if (!stopRunning)
                decodeUnknownFiles(apkFile, outDir, resTable);
        }
    }

    public void decodeUnknownFiles(ExtFile apkFile, File outDir, ResTable resTable)
            throws AndrolibException {
        File unknownOut = new File(outDir, "unknown");
        try {
            Directory unk = apkFile.getDirectory();

            // For protected apk, the entry name is strange
            Collection<String> zipEntries = fileEntry2ZipEntry.values();

            // loop all items in container recursively, ignoring any that are pre-defined by aapt
            Set<String> files = unk.getFiles(true);
            for (String file : files) {
                if (!isDecodedFileNames(file) && !zipEntries.contains(file)) {
                    // copy file out of archive into special "unknown" folder
                    unk.copyToDir(unknownOut, file);
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private boolean isDecodedFileNames(String file) {
        for (String apkFile : APK_STANDARD_FILES) {
            if (apkFile.equals(file)) {
                return true;
            }
        }
        for (String apkFile : APK_STANDARD_DIRS) {
            if (file.startsWith(apkFile + "/")) {
                return true;
            }
        }
        // Like classes2.dex
        if (file.endsWith(".dex") && !file.contains("/")) {
            return true;
        }
        return false;
    }

    public void decodeRawFiles(ExtFile apkFile, File outDir) throws AndrolibException {
        try {
            Directory in = apkFile.getDirectory();

            if (in.containsDir("assets")) {
                in.copyToDir(outDir, "assets");
            }
            if (in.containsDir("lib")) {
                in.copyToDir(outDir, "lib");
            }
            if (in.containsDir("libs")) {
                in.copyToDir(outDir, "libs");
            }
            if (in.containsDir("kotlin")) {
                in.copyToDir(outDir, "kotlin");
            }
            // Copy dex files
            Set<String> files = in.getFiles(false);
            for (String file : files) {
                if (file.endsWith(".dex")) {
                    in.copyToDir(outDir, file);
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void generatePublicXml(File outDir) throws AndrolibException {
        for (ResPackage pkg : resTable.listMainPackages()) {
            if (stopRunning) {
                break;
            }
            try {
                File file = new File(outDir, "values/public.xml");
                if (!file.getParentFile().exists()) { // create values dir if
                    // needed
                    file.getParentFile().mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(file);
                OutputStreamWriter writer = new OutputStreamWriter(fos);
                writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
                writer.write("<resources>\n");

                for (ResResSpec spec : pkg.listResSpecs()) {
                    String str = String.format(
                            "<public type=\"%s\" name=\"%s\" id=\"0x%08x\" />\n",
                            spec.getType().getName(), spec.getName(),
                            spec.getId().id);
                    writer.write(str);
                }

                writer.write("</resources>");
                writer.close();
                fos.close();
            } catch (IOException ex) {
                throw new AndrolibException(
                        "Could not generate public.xml file", ex);
            }
        }

    }

    private void decode(ResResource res, Directory inDir, Directory outDir)
            throws AndrolibException {
        ResFileValue fileValue = (ResFileValue) res.getValue();
        String inFileName = fileValue.getStrippedPath();
        String outResName = res.getFilePath();
        String typeName = res.getResSpec().getType().getName();

        String ext = null;
        String outFileName;
        String filename = inFileName.substring(inFileName.lastIndexOf("/") + 1);
        int extPos = filename.lastIndexOf(".");
        if (extPos == -1) {
            outFileName = outResName;
        } else {
            ext = filename.substring(extPos);
            outFileName = outResName + ext;
        }

        LOGGER.info("decoding " + outFileName);

        try {
            if (typeName.equals("raw")) {
                decode(inDir, inFileName, outDir, outFileName, "raw");
                return;
            }
            if (typeName.equals("font") && !".xml".equals(ext)) {
                decode(inDir, inFileName, outDir, outFileName, "raw");
                return;
            }
            if (typeName.equals("drawable") || typeName.equals("mipmap")) {
                // Special case: no ext, maybe protected
                if (ext == null) {
                    InputStream input = null;
                    try {
                        byte[] buffer = new byte[12];
                        input = inDir.getFileInput(inFileName);
                        input.read(buffer);
                        // Binary xml
                        if (buffer[0] == 0x03 && buffer[1] == 0x00
                                && buffer[2] == 0x08 && buffer[3] == 0x00) {
                            decode(inDir, inFileName, outDir, outFileName + ".xml", "xml");
                        }
                        // PNG
                        else if ((buffer[0] & 0xff) == 0x89 && buffer[1] == 0x50
                                && buffer[2] == 0x4e && buffer[3] == 0x47) {
                            decode(inDir, inFileName, outDir,
                                    outFileName + ".png", "raw");
                        }
                        // JPG
                        else if ((buffer[0] & 0xff) == 0xff
                                && (buffer[1] & 0xff) == 0xd8
                                && (buffer[2] & 0xff) == 0xff
                                && (buffer[3] & 0xff) == 0xe0) {
                            decode(inDir, inFileName, outDir,
                                    outFileName + ".jpg", "raw");
                        } else {
                            decode(inDir, inFileName, outDir, outFileName,
                                    "raw");
                        }
                    } catch (Exception e) {
                    } finally {
                        this.closeQuietly(input);
                    }
                    return;
                }
                if (inFileName.toLowerCase().endsWith(".9.png")) {
                    outFileName = outResName + ".9" + ext;

                    // check for htc .r.9.png
                    if (inFileName.toLowerCase().endsWith(".r.9.png")) {
                        outFileName = outResName + ".r.9" + ext;
                    }

                    try {
                        decode(inDir, inFileName, outDir, outFileName, "9path");
                        return;
                    } catch (CantFind9PatchChunk ex) {
                        LOGGER.warning(String.format(
                                "Cant find 9path chunk in file: \"%s\". Renaming it to *.png.",
                                inFileName));
                        outDir.removeFile(outFileName);
                        outFileName = outResName + ext;
                    }
                }
                if (!".xml".equals(ext)) {
                    decode(inDir, inFileName, outDir, outFileName, "raw");
                    return;
                }
            }

            decode(inDir, inFileName, outDir, outFileName, "xml");
        } catch (AndrolibException e) {
            res.replace(new ResBoolValue(false, 0, null));
            // Added by Pujiang: remove in-correct decoded file
            outDir.removeFile(outFileName);
        }
    }

    public void decode(Directory inDir, String inFileName, Directory outDir,
                       String outFileName, String decoder) throws AndrolibException {
        InputStream in = null;
        OutputStream out = null;

        // Add xml postfix
        if ("xml".equals(decoder) && !outFileName.endsWith(".xml")) {
            outFileName += ".xml";
        }

        try {
            boolean dummyDecoded = false;
            // 20171223: added !"".equals(resDirName)
            // 20180728: added !isFullDecoding
            if (!isFullDecoding && "raw".equals(decoder) && !"".equals(resDirName)) {
                if (outFileName.endsWith(".jpg")) {
                    out = outDir.getFileOutput(outFileName);
                    out.write(dummyJpg);
                    dummyDecoded = true;
                } else if (outFileName.endsWith(".png")
                        && !outFileName.endsWith(".9.png")) {
                    out = outDir.getFileOutput(outFileName);
                    out.write(dummyPng);
                    dummyDecoded = true;
                }
            }
            if (!dummyDecoded) {
                // Original code
                in = inDir.getFileInput(inFileName);
                out = outDir.getFileOutput(outFileName);
                mDecoders.decode(in, out, decoder);
            }

            // Record the relationship between file entry and zip entry
            String _fileEntry = "res/" + outFileName;
            String _zipEntry = resDirName + inFileName;
            if (!_fileEntry.equals(_zipEntry)) {
                fileEntry2ZipEntry.put(_fileEntry, _zipEntry);
            }
        } catch (Exception ex) {
            throw new AndrolibException(ex);
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    private void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }

    private void decodeFileResources(ExtFile apkFile, Directory outDir)
            throws AndrolibException {
        Directory inDir = null;
        try {
            Directory inApk = apkFile.getDirectory();
            if (inApk.containsDir("res")) {
                inDir = inApk.getDir("res");
                this.resDirName = "res/";
            }

            if (inDir == null && inApk.containsDir("r")) {
                inDir = inApk.getDir("r");
                this.resDirName = "r/";
            }

            if (inDir == null && inApk.containsDir("R")) {
                inDir = inApk.getDir("R");
                this.resDirName = "R/";
            }

            if (inDir == null) {
                inDir = inApk;
            }

        } catch (Exception e1) {
            throw new AndrolibException(e1);
        }

        for (ResPackage pkg : resTable.listMainPackages()) {
            if (stopRunning) {
                break;
            }

            LOGGER.info("Decoding file-resources...");
            for (ResResource res : pkg.listFiles()) {
                if (stopRunning) {
                    break;
                }

                decode(res, inDir, outDir);
            }
        }
    }

    protected void decodeValues(File outDir) throws Exception {

        Set<ResPackage> packages = resTable.listMainPackages();
        for (ResPackage pkg : packages) {
            if (stopRunning) {
                break;
            }

            Collection<ResValuesFile> files = pkg.listValuesFiles();

            for (ResValuesFile valueFile : files) {
                if (stopRunning) {
                    break;
                }
                // Create directory
                String path = valueFile.getPath();
                int pos = path.lastIndexOf('/');
                if (pos != -1) {
                    File dir = new File(outDir, path.substring(0, pos));
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                }

                XmlSerializer xmlSerializer = new KXmlSerializer();
                File file = new File(outDir, path);

                FileOutputStream fos = new FileOutputStream(file);
                OutputStreamWriter writer = new OutputStreamWriter(fos);
                xmlSerializer.setOutput(writer);
                // xmlSerializer.startDocument("utf-8", false);
                writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
                writer.write("<resources>\n");

                // LOGGER.info(path);
                Set<ResResource> resources = valueFile.listResources();
                for (ResResource res : resources) {
                    if (valueFile.isSynthesized(res)) {
                        continue;
                    }
                    ResValue v = res.getValue();
                    // // Pujiang: omit android values
                    // if (res.getResSpec().getId().package_ == 1) {
                    // continue;
                    // }
                    if (v instanceof ResValuesXmlSerializable) {
                        try { // Omit the error
                            ((ResValuesXmlSerializable) v)
                                    .serializeToResValuesXml(xmlSerializer, res);
                        } catch (Throwable e) {
                            //Log.e("DEBUG", e.getMessage() == null ? e.getClass().toString() : e.getMessage());
                            //Log.e("DEBUG", "v=" + v + ", res=" + res);
                            e.printStackTrace();
                        }
                    }
                    xmlSerializer.flush();
                    writer.write("\n");
                    // LOGGER.info(" " + res.getValue());
                }

                writer.write("</resources>\n");
                writer.close();
                fos.close();
            }
        }
    }

    private ResPackage getResPackage() {
        Set<ResPackage> packages = resTable.listMainPackages();
        if (packages != null && !packages.isEmpty()) {
            return packages.iterator().next();
        }
        return null;
    }

    protected String decodeManifest(ExtFile apkFile, File outDir)
            throws AndrolibException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = apkFile.getDirectory().getFileInput("AndroidManifest.xml");
            out = new FileOutputStream(new File(outDir, "AndroidManifest.xml"));

            // new AXMLParser(this).parse(in, out);
            return xmlDecoder.decodeManifest(in, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (Exception e) {
            }
            try {
                out.close();
            } catch (Exception e) {
            }
        }

        return null;
    }

    @Override
    public String getResReference(int id) {
        try {
            // This is android
            // if ((data >>> 24) == 1) {
            // return "@android:xx/xx";
            // }
            ResResSpec spec = resTable.getResSpec(id);
            return String.format("@%s%s/%s", getPackageName(id), spec.getType(),
                    spec.getName());
        } catch (AndrolibException e) {
            e.printStackTrace();
        }
        return String.format("@%s%08X", getPackageName(id), id);
    }

    public ResTable getResTable() {
        return resTable;
    }

    @Override
    public String getAttributeById(int id) {
        try {
            ResResSpec spec = resTable.getResSpec(id);
            return String.format("@%s%s/%s", getPackageName(id), spec.getType(),
                    spec.getName());
        } catch (AndrolibException e) {
            e.printStackTrace();
        }
        return String.format("?%s%08X", getPackageName(id), id);
    }

    public void stopDecode() {
        this.stopRunning = true;
    }

}
