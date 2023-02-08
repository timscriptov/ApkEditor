package com.gmail.heagoo.apkeditor.pro;

import org.jf.baksmali.Baksmali;
import org.jf.baksmali.BaksmaliOptions;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;

import java.io.File;


public class DexDecoder {

    private String dexFilePath;

    private String strWarning;

    public DexDecoder(String dexFilePath) {
        this.dexFilePath = dexFilePath;
    }

    protected DexBackedDexFile loadDexFile(String input) throws Exception {
        File file = new File(input);

        if (file == null || !file.exists() || file.isDirectory()) {
            throw new Exception("Can't find file: " + input);
        }

        return DexFileFactory.loadDexFile(file, Opcodes.forApi(15));
    }


    public void dex2smali(String outputDirectory) throws Exception {
        DexBackedDexFile dexFile = loadDexFile(dexFilePath);

        if (dexFile.supportsOptimizedOpcodes()) {
            this.strWarning = "You are disassembling an odex file without deodexing it. You won't be able to re-assemble the results unless you deodex it";
        }

        File outputDirectoryFile = new File(outputDirectory);
        if (!outputDirectoryFile.exists()) {
            if (!outputDirectoryFile.mkdirs()) {
                throw new Exception("Can't create the output directory " + outputDirectory);
            }
        }

        int jobs = Runtime.getRuntime().availableProcessors();

        if (!Baksmali.disassembleDexFile(dexFile, outputDirectoryFile, jobs, getOptions(), null)) {
            throw new Exception("Baksmali.disassembleDexFile failed.");
        }
    }

    private BaksmaliOptions getOptions() {
        final BaksmaliOptions options = new BaksmaliOptions();

        options.parameterRegisters = true;
        options.localsDirective = false;
        options.sequentialLabels = false;
        options.debugInfo = true;
        options.codeOffsets = false;
        options.accessorComments = true;
        options.implicitReferences = false;
        options.normalizeVirtualMethods = false;

        options.registerInfo = 0;

        return options;
    }

    public String getWarning() {
        return strWarning;
    }
}
