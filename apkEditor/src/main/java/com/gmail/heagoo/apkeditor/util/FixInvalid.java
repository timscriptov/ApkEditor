package com.gmail.heagoo.apkeditor.util;

import android.content.Context;

import com.gmail.heagoo.common.TextFileReader;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class FixInvalid {
    protected String decodeRootPath;
    protected String errMessage;
    HashSet<String> modifiedFileSet = new HashSet<>();
    int renamedFileNum = 0;

    FixInvalid(String decodeRootPath, String message) {
        this.decodeRootPath = decodeRootPath;
        if (!decodeRootPath.endsWith("/")) {
            this.decodeRootPath += "/";
        }
        this.errMessage = message;
    }

    // Do replace for the reference
    // Note: when oldStr is "@string/do", avoid to replace "@string/doit"
    // Return the content after replace, if not replaced, return null
    private static String doReferenceReplace(String content, Map<String, Map<String, String>> renameRecords) {
        // Cannot find any occurrence
        int atPos = content.indexOf('@', 0);
        if (atPos == -1) {
            return null;
        }

        // Record all replaces, and then do it in the end
        List<ReplaceRecord> allReplaces = new ArrayList<>();

        while (atPos != -1) {
            int next = atPos + 1;
            boolean slashFound = false;

            while (next < content.length()) {
                char c = content.charAt(next);
                if (c == '/') {
                    slashFound = true;
                    break;
                }
                if (c == '\"' || c == '<' || c == '\n') {
                    break;
                }
                next += 1;
            }

            if (slashFound) { // Found "/" before other special char
                int slashPos = next;
                String strType = content.substring(atPos + 1, slashPos);
                int nameEndPos = slashPos + 1;
                while (nameEndPos < content.length()) {
                    char c = content.charAt(nameEndPos);
                    if (c == '\"' || c == '<' || c == '\n') {
                        break;
                    }
                    nameEndPos += 1;
                }
                Map<String, String> old2NewName = renameRecords.get(strType);
                if (old2NewName != null) {
                    String strName = content.substring(slashPos + 1, nameEndPos);
                    String strNewName = old2NewName.get(strName);
                    if (strNewName != null) {
                        allReplaces.add(new ReplaceRecord(slashPos + 1, nameEndPos, strNewName));
                    }
                }
                atPos = content.indexOf('@', nameEndPos);
            } else {
                atPos = content.indexOf('@', next);
            }
        }

        if (!allReplaces.isEmpty()) {
            return doReplace(content, allReplaces);
        } else {
            return null;
        }
    }

    private static String doReplace(String content, List<ReplaceRecord> allReplaces) {
        StringBuilder sb = new StringBuilder();
        int startPos = 0;
        for (ReplaceRecord replace : allReplaces) {
            sb.append(content.substring(startPos, replace.start));
            sb.append(replace.replaceWith);
            startPos = replace.end;
        }
        sb.append(content.substring(startPos));
        return sb.toString();
    }

    public abstract boolean isErrorFixable();

    public abstract void fixErrors();

    // Fix succeeded or not
    public abstract boolean succeeded();

    // Used for toast showing
    public abstract String getMofifyMessage(Context ctx);

    // Filename -> string replaces
    // Only return modifications that will be reflected in AXML string block
    public abstract Map<String, Map<String, String>> getAxmlModifications();

    protected int modifyReferences(List<ResourceRename> _renameRecords) {
        File resDir = new File(decodeRootPath + "res");
        File[] subDirs = resDir.listFiles();
        if (subDirs == null) {
            return 0;
        }

        // Re-arrange to type -> rename(old name -> new name)
        Map<String, Map<String, String>> renameRecords = new HashMap<>();
        for (ResourceRename rr : _renameRecords) {
            Map<String, String> old2NewName = renameRecords.get(rr.resourceType);
            if (old2NewName == null) {
                old2NewName = new HashMap<>();
                renameRecords.put(rr.resourceType, old2NewName);
            }
            old2NewName.put(rr.resourceName, rr.newResourceName);
        }

        int modifiedFiles = 0;
        for (File dir : subDirs) {
            // Skip the raw resource
            String dirName = dir.getName();
            if (dirName.startsWith("raw")) {
                continue;
            }

            boolean isValueDir = "values".equals(dirName);

            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    // Not a file
                    if (!f.isFile()) {
                        continue;
                    }

                    // Not xml file
                    String name = f.getName();
                    if (!name.endsWith(".xml")) {
                        continue;
                    }

                    // For simple value file, there is no reference
                    if (isValueDir && isSimpleValueXml(name)) {
                        continue;
                    }

                    if (replaceReference(f, renameRecords)) {
                        modifiedFiles += 1;
                    }
                }
            }
        }

        // AndroidManifest.xml
        File f = new File(decodeRootPath + "AndroidManifest.xml");
        if (f.exists() && replaceReference(f, renameRecords)) {
            modifiedFiles += 1;
        }

        return modifiedFiles;
    }

    private boolean isSimpleValueXml(String curName) {
        final String[] names = {"bools.xml", "colors.xml", "dimens.xml",
                "ids.xml", "integers.xml", "public.xml", "strings.xml"};
        for (String n : names) {
            if (n.equals(curName)) {
                return true;
            }
        }
        return false;
    }

    // Rename all the resource names inside a file
    private boolean replaceReference(File f, Map<String, Map<String, String>> renameRecords) {
        try {
            boolean modified = false;

            TextFileReader reader = new TextFileReader(f.getPath());
            String content = reader.getContents();

            content = doReferenceReplace(content, renameRecords);

            // To check modified or not
            if (content != null) {
                modified = true;
            }

            // Write back
            if (modified) {
                this.writeBack(f.getPath(), content);
            }

            return modified;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    protected void modifyDeclaration(String type, List<ResourceRename> renameList) {
        // Modify the name declaration
        modifyNameInValues(type, renameList);

        // For attr type, modify in styles.xml? Correct?
        if ("attr".equals(type)) {
            modifyAttrInStyle(renameList);
            return;
        }

        // Modify the file name for types like "drawable"
        File resDir = new File(decodeRootPath + "res");
        File[] directories = resDir.listFiles();
        if (directories == null) {
            return;
        }
        for (File valueDir : directories) {
            if (valueDir.isFile()) {
                continue;
            }
            String fileName = valueDir.getName();
            if (fileName.startsWith(type) || fileName.startsWith(type + "-")) {
                File[] valueFiles = valueDir.listFiles();
                if (valueFiles != null) for (File f :
                        valueFiles) {
                    String newName = getNewFileName(f, renameList);
                    if (newName != null) {
                        f.renameTo(new File(valueDir, newName));
                        renamedFileNum += 1;
                    }
                }
            }
        }
    }

    // Rename for specified type in related value files
    // For example, type="attr", then do the rename in attrs.xml
    private void modifyNameInValues(String type, List<ResourceRename> renameList) {
        // Arrange to map record
        Map<String, String> nameReplaces = new HashMap<>();
        for (ResourceRename rr : renameList) {
            nameReplaces.put(rr.resourceName, rr.newResourceName);
        }

        String declareFileName;
        if ("plurals".equals(type)) {
            declareFileName = "plurals.xml";
        } else {
            declareFileName = type + "s.xml";
        }

        File resDir = new File(decodeRootPath + "res");
        File[] valueDirs = resDir.listFiles();
        if (valueDirs != null)
            for (File valueDir : valueDirs) {
                File file = null;
                if (valueDir.isDirectory() && valueDir.getName().startsWith("values")) {
                    file = new File(valueDir, declareFileName);
                    if (!file.exists()) {
                        file = null;
                    }
                }
                if (file == null) {
                    continue;
                }

                // Modify the declared name
                try {
                    TextFileReader reader = new TextFileReader(file.getPath());
                    String content = reader.getContents();

                    if ("id".equals(type)) {
                        content = replaceAllGroup1(content, "name=\"(.*?)\"", nameReplaces);
                    } else if ("drawable".equals(type)
                            || "dimen".equals(type)
                            || "color".equals(type)) {
                        String out = replaceAllGroup1(content, "@" + type + "/(.*?)<", nameReplaces);
                        if (out == null) {
                            out = content;
                        }
                        content = replaceAllGroup1(out, "name=\"(.*?)\"", nameReplaces);
                    } else {
                        content = replaceAllGroup1(content, type + " name=\"(.*?)\"", nameReplaces);
                    }

                    if (content != null) {
                        writeBack(file.getPath(), content);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
    }

    // Replace the content of group 1 when find the match
    private String replaceAllGroup1(String str, String format, Map<String, String> replaces) {
        List<ReplaceRecord> allReplaces = new ArrayList<>();

        Pattern p = Pattern.compile(format);
        Matcher matcher = p.matcher(str);

        int startIdx = 0;
        while (matcher.find(startIdx)) {
            String group1 = matcher.group(1);
            String newVal = replaces.get(group1);
            if (newVal != null) {
                int start = matcher.start(1);
                int end = matcher.end(1);
                allReplaces.add(new ReplaceRecord(start, end, newVal));
            }
            startIdx = matcher.end();
        }

        if (!allReplaces.isEmpty()) {
            return doReplace(str, allReplaces);
        } else {
            return null;
        }
    }

    // Rename attr inside styles.xml
    private void modifyAttrInStyle(List<ResourceRename> renameList) {
        File resDir = new File(decodeRootPath + "res");
        File[] valueDirs = resDir.listFiles();
        if (valueDirs != null)
            for (File valueDir : valueDirs) {
                File styleFile = null;
                if (valueDir.isDirectory() && valueDir.getName().startsWith("values")) {
                    styleFile = new File(valueDir, "styles.xml");
                    if (!styleFile.exists()) {
                        styleFile = null;
                    }
                }
                if (styleFile == null) {
                    continue;
                }

                // Get content of styles.xml
                boolean modified = false;
                String filePath = styleFile.getPath();
                TextFileReader reader;
                try {
                    reader = new TextFileReader(filePath);
                } catch (IOException e) {
                    continue;
                }
                String content = reader.getContents();

                // Do replace
                for (ResourceRename rename : renameList) {
                    int lenBefore = content.length();
                    content = content.replace("item name=\"" + rename.resourceName + "\"",
                            "item name=\"" + rename.newResourceName + "\"");
                    if (content.length() != lenBefore) {
                        modified = true;
                    }
                }

                if (modified) {
                    writeBack(filePath, content);
                }
            }
    }

    // Return the new file name in case we need to rename it
    private String getNewFileName(File f, List<ResourceRename> renameList) {
        String name = f.getName();
        for (ResourceRename rename : renameList) {
            if (name.startsWith(rename.resourceName)) {
                String postfix = name.substring(rename.resourceName.length());
                if (postfix.equals("")) {
                    return rename.newResourceName + postfix;
                }
                // ".png" etc
                if (postfix.charAt(0) == '.' && postfix.indexOf('.', 1) == -1) {
                    return rename.newResourceName + postfix;
                }
                if (postfix.equals(".9.png")) {
                    return rename.newResourceName + postfix;
                }
            }
        }
        return null;
    }

    private void writeBack(String filePath, String content) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filePath);
            fos.write(content.getBytes());
            modifiedFileSet.add(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(fos);
        }
    }

    protected void closeQuietly(Closeable c) {
        if (c != null) try {
            c.close();
        } catch (IOException ignored) {
        }
    }

    static class ReplaceRecord {
        int start;
        int end;
        String replaceWith;

        public ReplaceRecord(int _start, int _end, String str) {
            this.start = _start;
            this.end = _end;
            this.replaceWith = str;
        }
    }
}
