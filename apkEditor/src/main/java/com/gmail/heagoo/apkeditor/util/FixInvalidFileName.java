package com.gmail.heagoo.apkeditor.util;

import android.content.Context;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.TextFileReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FixInvalidFileName extends FixInvalid {
    private final static String err_InvalidFile = ": Invalid file name: must contain only ";
    //private final static String err_InvalidFile2 = ": Invalid file name: must contain only [a-z0-9_.]";

    private int renamedFiles = 0;
    private int modifiedFiles = 0;
    private List<InvalidFileRecord> invalidFiles = new ArrayList<>();

    public FixInvalidFileName(String decodeRootPath, String message) {
        super(decodeRootPath, message);
        try {
            parseErrorMessage();
        } catch (IOException e) {
        }
    }

    private void parseErrorMessage() throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new StringReader(this.errMessage));
            String line = br.readLine();
            while (line != null) {
                int pos = line.indexOf(err_InvalidFile);
                if (pos > 0) {
                    String relativePath = line.substring(0, pos);
                    // Exclude the repeated error
                    if (!isInvalidPathExist(relativePath)) {
                        InvalidFileRecord rec = new InvalidFileRecord(relativePath);
                        invalidFiles.add(rec);
                    }
                }
                line = br.readLine();
            }
        } finally {
            closeQuietly(br);
        }
    }

    private void closeQuietly(BufferedReader br) {
        if (br != null) {
            try {
                br.close();
            } catch (IOException e) {
            }
        }
    }

    // Is there already the error of "Invalid file name" for same file
    private boolean isInvalidPathExist(String relativePath) {
        String[] folders = relativePath.split("/");
        int num = folders.length;
        String fileName = folders[num - 1];
        String parentFolder = folders[num - 2];

        String resourceType = parentFolder;
        int idx = parentFolder.indexOf('-');
        if (idx != -1) {
            resourceType = parentFolder.substring(0, idx);
        }

        for (int i = 0; i < invalidFiles.size(); i++) {
            InvalidFileRecord rec = invalidFiles.get(i);
            if (fileName.equals(rec.fileName)
                    && resourceType.equals(rec.resourceType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isErrorFixable() {
        return invalidFiles.size() > 0;
    }

    @Override
    public void fixErrors() {
        fixInvalidFileNames();
    }

    private void fixInvalidFileNames() {
        File rootDir = new File(decodeRootPath + "res");
        File[] dirs = rootDir.listFiles();

        for (int i = 0; i < invalidFiles.size(); i++) {
            InvalidFileRecord rec = invalidFiles.get(i);

            // Generate new names
            rec.createNewName(i);

            // Rename target files
            // NOTE: the same name for the same resource type also need to be
            // renamed
            if (dirs != null)
                for (File dir : dirs) {
                    if (dir.isFile()) {
                        continue;
                    }
                    // Same resource type
                    if (dir.getName().startsWith(rec.resourceType)) {
                        File[] files = dir.listFiles();
                        if (files != null)
                            for (File f : files) {
                                if (f.isDirectory()) {
                                    continue;
                                }
                                if (f.getName().equals(rec.fileName)) {
                                    File newF = new File(dir, rec.newFileName);
                                    f.renameTo(newF);
//                            Log.d("DEBUG",
//                                    f.getPath() + " --> " + newF.getPath());
                                    renamedFiles += 1;
                                }
                            }
                    }
                }
        }

        // public.xml
        modifyPublicXml();

        // other xml files
        modifyXmlFiles();
    }

    private void modifyXmlFiles() {
        List<ResourceRename> renameRecords = new ArrayList<>();
        for (InvalidFileRecord rec : this.invalidFiles) {
            renameRecords.add(new ResourceRename(rec.resourceType, rec.resourceName, rec.newResourceName));
        }

        modifyReferences(renameRecords);
    }

    private boolean isSpecialValueXml(String curName) {
        final String[] names = {"bools.xml", "colors.xml", "dimens.xml",
                "integers.xml", "public.xml", "strings.xml"};
        for (String n : names) {
            if (n.equals(curName)) {
                return true;
            }
        }
        return false;
    }

    private void modifyPublicXml() {
        String path = decodeRootPath + "res/values/public.xml";
        try {
            TextFileReader reader = new TextFileReader(path);
            String content = reader.getContents();

            for (int i = 0; i < invalidFiles.size(); i++) {
                InvalidFileRecord rec = invalidFiles.get(i);
                content = content.replace("\"" + rec.resourceName + "\"",
                        "\"" + rec.newResourceName + "\"");
//                Log.d("DEBUG", "Change " + rec.resourceName + " to "
//                        + rec.newResourceName);
            }

            // Write back
            FileOutputStream fos = new FileOutputStream(path);
            fos.write(content.getBytes());
            fos.close();

            modifiedFiles += 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean succeeded() {
        return renamedFiles > 0 || modifiedFiles > 0;
    }

    @Override
    public String getMofifyMessage(Context ctx) {
        // Show toast and then rebuild
        StringBuilder sb = new StringBuilder();
        if (renamedFiles > 0) {
            sb.append(
                    String.format(ctx.getString(R.string.str_num_renamed_file),
                            renamedFiles));
            sb.append("\n");
        }
        if (modifiedFiles > 0) {
            sb.append(
                    String.format(ctx.getString(R.string.str_num_modified_file),
                            modifiedFiles));
            sb.append("\n");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    // Suppose no modification, as we do not need to revise AXML file
    @Override
    public Map<String, Map<String, String>> getAxmlModifications() {
        return null;
    }

    private static class InvalidFileRecord {
        String relativePath;
        String parentFolder;
        String resourceType; // resourceType is like drawable
        String resourceName; // resourceName is like background
        String fileName; // fileName is like background.png

        String newFileName;
        String newResourceName;

        private InvalidFileRecord(String relativePath) {
            this.relativePath = relativePath;

            String[] folders = relativePath.split("/");
            int num = folders.length;
            this.fileName = folders[num - 1];
            this.parentFolder = folders[num - 2];
            int idx = parentFolder.indexOf('-');
            if (idx != -1) {
                this.resourceType = parentFolder.substring(0, idx);
            } else {
                this.resourceType = parentFolder;
            }

            int pos = fileName.lastIndexOf('.');
            if (pos != -1) {
                this.resourceName = fileName.substring(0, pos);
            } else {
                this.resourceName = fileName;
            }
        }

        // Create a new valid name
        private void createNewName(int idx) {
            StringBuilder sb = new StringBuilder();

            // suffix is like ".png"
            String suffix = "";
            int pos = fileName.lastIndexOf('.');
            if (pos != -1) {
                // Special case: .9.png
                if (fileName.endsWith(".9.png")) {
                    suffix = ".9.png";
                } else {
                    suffix = fileName.substring(pos);
                }
            }

            for (int i = 0; i < fileName.length() - suffix.length(); i++) {
                char c = fileName.charAt(i);
                if (Character.isLowerCase(c)) {
                    sb.append(c);
                } else if (Character.isUpperCase(c)) {
                    sb.append((char) (c - 'A' + 'a'));
                } else if (Character.isDigit(c)) {
                    sb.append(c);
                } else if (c == '_' || c == '.') {
                    sb.append(c);
                }
            }
            sb.append("_r" + idx);
            this.newResourceName = sb.toString();
            // Make sure the length is not equal
            // so that we can detect change or not after replace
            if (newResourceName.length() == resourceName.length()) {
                newResourceName += "_";
            }

            sb.append(suffix);
            this.newFileName = sb.toString();
        }
    }

}
