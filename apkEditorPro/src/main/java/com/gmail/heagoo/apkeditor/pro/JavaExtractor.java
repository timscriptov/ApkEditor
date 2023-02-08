package com.gmail.heagoo.apkeditor.pro;

import com.gmail.heagoo.apkeditor.inf.IJavaExtractor;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.immutable.ImmutableDexFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jadx.api.JadxDecompiler;

public class JavaExtractor implements IJavaExtractor {

    private final String apkPath;
    private final String dexName;
    private final String className;
    private final String workingDirectory;
    private String interestedName;
    private String errorMessage = null;

    public JavaExtractor(String apkPath, String dexName, String className, String workingDirectory) {
        this.apkPath = apkPath;
        this.dexName = dexName;
        this.className = className;
        this.workingDirectory = workingDirectory;

        this.interestedName = className;
        int position = className.lastIndexOf('$');
        if (position != -1) {
            interestedName = className.substring(0, position);
        }
    }

    @Override
    public boolean extract() {
        if (extractDex()) {
            String dexPath = workingDirectory + "/extracted.dex";
            return decompile(dexPath, workingDirectory);
        }
        return false;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    private boolean extractDex() {
        // Load dex file
        DexFile dexFile = null;
        try {
            dexFile = (DexFile) DexFileFactory.loadDexEntry(new File(apkPath), dexName, true, Opcodes.forApi(15));
        } catch (Exception e) {
            errorMessage = "The dex file cannot be decompiled.";
            return false;
        }

        // Filter classes
        List<ClassDef> classes = new ArrayList<>();
        for (ClassDef classDef : dexFile.getClasses()) {
            final String currentClass = classDef.getType();
            if (currentClass.startsWith(interestedName)) {
                classes.add(classDef);
            }
        }

        // Check directory
        File dir = new File(workingDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        dexFile = new ImmutableDexFile(Opcodes.forApi(15), classes);

        try {
            DexFileFactory.writeDexFile(workingDirectory + "/extracted.dex", dexFile);
        } catch (Exception e) {
            errorMessage = "Cannot extract " + className
                    + " as dex extract failed: " + e.getMessage();
            return false;
        }

        return true;
    }

    private boolean decompile(String dexPath, String outputDir) {
        try {
            File dexInputFile = new File(dexPath);
            File javaOutputDir = new File(outputDir);

            JadxDecompiler jadx = new JadxDecompiler();
            jadx.setOutputDir(javaOutputDir);
            jadx.loadFile(dexInputFile);
            jadx.saveSources();

            return true;
        } catch (Exception | StackOverflowError e) {
            errorMessage = "Cannot decompile java code: " + e.getMessage();
        }
        return false;
    }
}
