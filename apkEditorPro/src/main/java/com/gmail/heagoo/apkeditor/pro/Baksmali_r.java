package com.gmail.heagoo.apkeditor.pro;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import org.jf.baksmali.Adaptors.ClassDefinition;
import org.jf.baksmali.BaksmaliOptions;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.util.ClassFileNameHandler;
import org.jf.util.IndentingWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

public class Baksmali_r {
    public static boolean disassembleDexResource(DexFile dexFile, File outputDir, int jobs, BaksmaliOptions options) {
        return disassembleDexResource(dexFile, outputDir, jobs, options, null);
    }

    public static boolean disassembleDexResource(DexFile dexFile, File outputDir, int jobs, final BaksmaliOptions options, @Nullable List<String> classes) {
        List<ClassDef> targets = new ArrayList<>();
        Set<? extends ClassDef> clsDefs = dexFile.getClasses();
        for (ClassDef cls : clsDefs) {
            String classDescriptor = cls.getType();
            if (classDescriptor.contains("/R$")) {
                targets.add(cls);
            }
        }
        if (targets.isEmpty()) {
            return false;
        }
        List<? extends ClassDef> classDefs = Ordering.natural().sortedCopy(targets);
        final ClassFileNameHandler fileNameHandler = new ClassFileNameHandler(outputDir, ".smali");
        ExecutorService executor = Executors.newFixedThreadPool(jobs);
        List<Future<Boolean>> tasks = Lists.newArrayList();
        Set<String> classSet = null;
        if (classes != null) {
            classSet = new HashSet<>(classes);
        }
        for (final ClassDef classDef : classDefs) {
            if (classSet == null || classSet.contains(classDef.getType())) {
                tasks.add(executor.submit(() -> Baksmali_r.disassembleClass(classDef, fileNameHandler, options)));
            }
        }
        boolean errorOccurred = false;
        try {
            for (Future<Boolean> task : tasks) {
                if (!task.get()) {
                    errorOccurred = true;
                }
            }
            executor.shutdown();
            return !errorOccurred;
        } catch (Throwable th) {
            executor.shutdown();
        }
        return errorOccurred;
    }

    public static boolean disassembleClass(ClassDef classDef, ClassFileNameHandler fileNameHandler, BaksmaliOptions options) {
        String classDescriptor = classDef.getType();
        if (classDescriptor.charAt(0) != 'L' || classDescriptor.charAt(classDescriptor.length() - 1) != ';') {
            System.err.println("Unrecognized class descriptor - " + classDescriptor + " - skipping class");
            return false;
        }
        File smaliFile = fileNameHandler.getUniqueFilenameForClass(classDescriptor);
        ClassDefinition classDefinition = new ClassDefinition(options, classDef);
        Writer writer = null;
        try {
            try {
                File smaliParent = smaliFile.getParentFile();
                if (!smaliParent.exists() && !smaliParent.mkdirs() && !smaliParent.exists()) {
                    System.err.println("Unable to create directory " + smaliParent.toString() + " - skipping class");
                    if (0 != 0) {
                        try {
                            writer.close();
                        } catch (Throwable ex) {
                            System.err.println("\n\nError occurred while closing file " + smaliFile.toString());
                            ex.printStackTrace();
                        }
                    }
                    return false;
                } else if (!smaliFile.exists() && !smaliFile.createNewFile()) {
                    System.err.println("Unable to create file " + smaliFile.toString() + " - skipping class");
                    if (0 != 0) {
                        try {
                            writer.close();
                        } catch (Throwable ex2) {
                            System.err.println("\n\nError occurred while closing file " + smaliFile.toString());
                            ex2.printStackTrace();
                        }
                    }
                    return false;
                } else {
                    BufferedWriter bufWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(smaliFile), "UTF8"));
                    IndentingWriter indentingWriter = new IndentingWriter(bufWriter);
                    classDefinition.writeTo(indentingWriter);
                    try {
                        indentingWriter.close();
                    } catch (Throwable ex3) {
                        System.err.println("\n\nError occurred while closing file " + smaliFile.toString());
                        ex3.printStackTrace();
                    }
                    return true;
                }
            } catch (Exception ex4) {
                System.err.println("\n\nError occurred while disassembling class " + classDescriptor.replace('/', '.') + " - skipping class");
                ex4.printStackTrace();
                smaliFile.delete();
                if (0 != 0) {
                    try {
                        writer.close();
                    } catch (Throwable ex5) {
                        System.err.println("\n\nError occurred while closing file " + smaliFile.toString());
                        ex5.printStackTrace();
                    }
                }
                return false;
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    writer.close();
                } catch (Throwable ex6) {
                    System.err.println("\n\nError occurred while closing file " + smaliFile.toString());
                    ex6.printStackTrace();
                }
            }
            throw th;
        }
    }
}