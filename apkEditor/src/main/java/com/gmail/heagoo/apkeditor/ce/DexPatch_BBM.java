package com.gmail.heagoo.apkeditor.ce;

import android.content.Context;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DexPatch_BBM implements IApkMaking, Serializable {

    private static final long serialVersionUID = -23835190734466173L;

    private Map<String, String> extraStrReplaces;

    public DexPatch_BBM(String newPackageName,
                        Map<String, String> extraStrReplaces) {
        this.extraStrReplaces = extraStrReplaces;
    }

    @Override
    public void prepareReplaces(Context ctx, String apkFilePath,
                                Map<String, String> allReplaces, IDescriptionUpdate updater)
            throws Exception {

        if (updater != null) {
            updater.updateDescription("Patch Smali Files");
        }

        File fileDir = ctx.getFilesDir();
        String rootDirectory = fileDir.getAbsolutePath();
        String smaliRootPath = rootDirectory + "/decoded/smali/";

        // Patch "content://com.bbm/" with new name
        patchProviderName(smaliRootPath + "com/bbm/providers");

        // Comment the killProcess in com/bbm/util/*.smali
        patchUtilFiles(smaliRootPath + "com/bbm/util");

        if (updater != null) {
            updater.updateDescription("");
        }
    }

    private void patchUtilFiles(String utilFolder) throws Exception {
        File dir = new File(utilFolder);
        File files[] = dir.listFiles();
        if (files != null)
            for (File f : files) {
                if (!f.isFile()) {
                    continue;
                }

                List<String> readLines = new ArrayList<String>();
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        new FileInputStream(f)));

                boolean modified = false;
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.endsWith("->killProcess(I)V")) {
                        line = "#" + line;
                        modified = true;
                    }
                    readLines.add(line);
                }

                // Need to modify this file
                if (modified) {
                    String filePath = f.getAbsolutePath();
                    String modifiedFile = filePath + ".new";
                    BufferedOutputStream fos = new BufferedOutputStream(
                            new FileOutputStream(modifiedFile));
                    for (String _line : readLines) {
                        fos.write(_line.getBytes());
                        fos.write('\n');
                    }
                    fos.close();

                    renameFile(filePath + ".new", filePath);
                }

                br.close();
            }
    }

    // Patch all files inside folderPath
    private void patchProviderName(String folderPath) throws Exception {
        String newProviderName = extraStrReplaces.get("com.bbm");

        File dir = new File(folderPath);
        File files[] = dir.listFiles();
        if (files != null)
            for (File f : files) {
                if (!f.isFile()) {
                    continue;
                }

                List<String> readLines = new ArrayList<String>();
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        new FileInputStream(f)));

                boolean providerStrDetected = false;
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("content://com.bbm/")) {
                        line = line.replace("content://com.bbm/", "content://"
                                + newProviderName + "/");
                        readLines.add(line);
                        providerStrDetected = true;
                        break;
                    }
                    readLines.add(line);
                }

                // Need to modify the provider authority name
                if (providerStrDetected) {
                    patchOneFile(f.getAbsolutePath(), br, readLines);
                }

                br.close();
            }
    }

    // Patch one file which contains "content://com.bbm/"
    // br: Buffered reader of the original smali file
    // readLines: previous read lines
    private void patchOneFile(String filePath, BufferedReader br,
                              List<String> readLines) throws Exception {
        String newProviderName = extraStrReplaces.get("com.bbm");

        String modifiedFile = filePath + ".new";
        BufferedOutputStream fos = new BufferedOutputStream(
                new FileOutputStream(modifiedFile));

        // Write lines already read
        for (String line : readLines) {
            fos.write(line.getBytes());
            fos.write('\n');
        }

        // Revise remaining lines
        String line;
        while ((line = br.readLine()) != null) {
            if (line.contains("content://com.bbm/")) {
                line = line.replace("content://com.bbm/", "content://"
                        + newProviderName + "/");
            }
            fos.write(line.getBytes());
            fos.write('\n');
        }

        fos.close();

        // At last, remove the original files
        renameFile(filePath + ".new", filePath);
    }

    //
    private void renameFile(String fromPath, String toPath) {
        File f = new File(fromPath);
        File f2 = new File(toPath);
        if (!f.renameTo(f2)) {
            // Log.d("DEBUG", "rename failed!");
        } else {
            // Log.d("DEBUG", "rename succeed!");
        }
    }

    // @Override
    // public String getDescription() {
    // return "Patch Smali Files";
    // }

}
