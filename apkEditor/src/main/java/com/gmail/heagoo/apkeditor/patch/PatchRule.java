package com.gmail.heagoo.apkeditor.patch;

import com.gmail.heagoo.apkeditor.ApkInfoActivity;
import com.gmail.heagoo.apkeditor.base.R;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public abstract class PatchRule {

    private static final String NAME = "NAME:";
    protected String ruleName;
    protected int startLine;

    // Assign values inside rawStr, values are got from patch context
    // For example: name="${STR_NAME}" --> name="app_name"
    public static String assignValues(IPatchContext ctx, String rawStr) {
        List<REPLACE_REC> replaces = new ArrayList<>();

        int position = rawStr.indexOf("${", 0);
        while (position != -1) {
            int startPos = position + 2;
            int endPos = rawStr.indexOf("}", startPos);
            if (endPos != -1) {
                String varName = rawStr.substring(startPos, endPos);
                String realVal = ctx.getVariableValue(varName);
                if (realVal != null) {
                    replaces.add(new REPLACE_REC(startPos - 2, endPos + 1, realVal));
                }
            } else {
                break;
            }
            position = rawStr.indexOf("${", endPos);
        }

        // Do replaces
        if (!replaces.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int startPos = 0;
            for (REPLACE_REC rec : replaces) {
                int curPos = rec.startPos;
                if (curPos > startPos) {
                    sb.append(rawStr.substring(startPos, curPos));
                }
                sb.append(rec.replacing);
                startPos = rec.endPos;
            }
            // remaining
            if (startPos < rawStr.length()) {
                sb.append(rawStr.substring(startPos));
            }
            return sb.toString();
        }

        return null;
    }

    public abstract void parseFrom(LinedReader br, IPatchContext logger)
            throws IOException;

    // Return the next rule name (match_gotogoto), null for common rules
    public abstract String executeRule(ApkInfoActivity activity,
                                       ZipFile patchZip, IPatchContext logger);

    // Is the rule valid or not
    public abstract boolean isValid(IPatchContext logger);

    // Need to decode DEX or not
    public abstract boolean isSmaliNeeded();

    public String getRuleName() {
        return ruleName;
    }

    // Parse the common keyword like "NAME:"
    boolean parseAsKeyword(String line, LinedReader br) throws IOException {
        if (NAME.equals(line)) {
            ruleName = br.readLine();
            if (ruleName != null) {
                ruleName = ruleName.trim();
            }
            return true;
        }
        return false;
    }

    // Read as text file
    String readFileContent(String filepath) throws IOException {
        File f = new File(filepath);
        long size = f.length();
        StringBuilder sb = new StringBuilder((int) size + 32);
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(f)));
        try {
            String line = br.readLine();
            if (line != null) {
                sb.append(line);
            }
            while ((line = br.readLine()) != null) {
                sb.append("\n");
                sb.append(line);
            }
            return sb.toString();
        } finally {
            closeQuietly(br);
        }
    }

    List<String> readFileLines(String filepath) throws IOException {

        List<String> lines = new ArrayList<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(filepath)));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                if (!"".equals(line.trim())) { // skip blank lines
                    lines.add(line);
                }
            }
        } finally {
            closeQuietly(br);
        }

        return lines;
    }

    // To read multiple lines until encounter one keyword
    String readMultiLines(BufferedReader br, List<String> lines,
                          boolean bTrim, List<String> endKeywords) throws IOException {
        String line = br.readLine();
        while (line != null) {
            if (bTrim) {
                line = line.trim();
            }
            if (endKeywords.contains(line)) {
                break;
            }
            lines.add(line);
            line = br.readLine();
        }
        return line;
    }

    protected void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }

    // ////////////////////////////////////////////////////////////////////

    protected void closeQuietly(ZipFile f) {
        if (f != null) {
            try {
                f.close();
            } catch (IOException ignored) {
            }
        }
    }

    private String getParentFolder(String path) {
        int pos = path.lastIndexOf("/");
        if (pos > 0) {
            return path.substring(0, pos);
        } else {
            return null;
        }
    }

    // targetDir is the absolute directory path
    private boolean addFileEntry(ApkInfoActivity activity, ZipFile zfile,
                                 ZipEntry entry, String targetDir, IPatchContext logger) {

        String name = entry.getName();
        String path = targetDir + "/" + name;

        // Create the folder if not exist
        String parent = getParentFolder(path);
        while (!activity.getResListAdapter().isFolderExist(parent)) {
            //Log.d("DEBUG", "folder " + parent + " not exist");
            parent = getParentFolder(parent);
        }
        String[] paths = path.substring(parent.length() + 1).split("/");
        if (paths.length > 1) {
            for (int i = 0; i < paths.length - 1; ++i) {
                try {
                    activity.getResListAdapter().addFolderReportError(parent,
                            paths[i], false);
                } catch (Exception e) {
                    logger.error(R.string.failed_create_dir, e.getMessage());
                    return false;
                }
                parent += "/" + paths[i];
            }
        }

        InputStream input = null;
        try {
            input = zfile.getInputStream(entry);
            return activity.getResListAdapter().addFile(path, input) != null;
        } catch (Exception e) {
            logger.error(R.string.general_error, e.getMessage());
        } finally {
            closeQuietly(input);
        }

        return false;
    }

    void addFilesInZip(ApkInfoActivity activity, String zipFile,
                       IBeforeAddFile hook, IPatchContext logger) throws Exception {
        ZipFile zfile = null;
        String targetDir = activity.getDecodeRootPath();

        try {
            zfile = new ZipFile(zipFile);
            Enumeration<?> zList = zfile.entries();
            ZipEntry ze;
            while (zList.hasMoreElements()) {
                ze = (ZipEntry) zList.nextElement();

                // Currently, we omit the directory, maybe not correct
                if (ze.isDirectory()) {
                    continue;
                }

                boolean consumed = false;
                if (hook != null) {
                    consumed = hook.consumeAddedFile(activity, zfile, ze);
                }
                if (!consumed) {
                    addFileEntry(activity, zfile, ze, targetDir, logger);
                }
            }
            zfile.close();
            zfile = null;
        } finally {
            try {
                if (null != zfile) {
                    zfile.close();
                }
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    // Check the file/directory is in smali folder or not
    boolean isInSmaliFolder(String targetFile) {
        if (targetFile != null) {
            int pos = targetFile.lastIndexOf('/');
            if (pos != -1) {
                String firstDir = targetFile.substring(0, pos);
                if ("smali".equals(firstDir) || firstDir.startsWith("smali_")) {
                    return true;
                }
            }
        }
        return false;
    }

    // Pre-process the values in string list
    protected void preProcessing(IPatchContext ctx, List<String> values) {
        for (int i = 0; i < values.size(); ++i) {
            String assignedVal = assignValues(ctx, values.get(i));
            if (assignedVal != null) {
                values.set(i, assignedVal);
            }
        }
    }

    private static class REPLACE_REC {
        int startPos;
        int endPos;
        String replacing;

        public REPLACE_REC(int _s, int _e, String _r) {
            this.startPos = _s;
            this.endPos = _e;
            this.replacing = _r;
        }
    }
}
