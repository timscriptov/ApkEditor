package com.gmail.heagoo.apkeditor.pro;

import org.jf.baksmali.BaksmaliOptions;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;

import java.io.File;

public class ResourceDecoder {
    protected static DexBackedDexFile loadDexFile(String input) throws Exception {
        File file = new File(input);

        if (file == null || !file.exists() || file.isDirectory()) {
            throw new Exception("Can't find file: " + input);
        }

        return DexFileFactory.loadDexFile(file, Opcodes.forApi(15));
    }

    public static void decodeResources(String dexFilePath, String outputDirectory) throws Exception {
        DexBackedDexFile dexFile = loadDexFile(dexFilePath);

        File outputDirectoryFile = new File(outputDirectory);
        if (!outputDirectoryFile.exists()) {
            if (!outputDirectoryFile.mkdirs()) {
                throw new Exception("Can't create the output directory " + outputDirectory);
            }
        }

        int jobs = Runtime.getRuntime().availableProcessors();

        if (!Baksmali_r.disassembleDexResource(dexFile, outputDirectoryFile, jobs, getOptions(), null)) {
            throw new Exception("Baksmali.disassembleDexFile failed.");
        }
    }

    private static BaksmaliOptions getOptions() {
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
}
