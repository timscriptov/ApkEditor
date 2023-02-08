package com.gmail.heagoo.apkeditor.patch;

import android.util.Log;
import android.util.SparseIntArray;

import com.gmail.heagoo.apkeditor.ApkInfoActivity;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.RandomUtil;
import com.gmail.heagoo.common.SDCard;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class PatchRule_Merge extends PatchRule {

    private static final String strEnd = "[/MERGE]";
    private static final String SOURCE = "SOURCE:";

    // Source file in the patched zip
    private String sourceFile;

    // Record all the replaced ids (old id -> new id)
    private SparseIntArray replacedIds;

    @Override
    public void parseFrom(LinedReader br, IPatchContext logger) throws IOException {
        super.startLine = br.getCurrentLine();

        String line = br.readLine();
        while (line != null) {
            line = line.trim();
            if (strEnd.equals(line)) {
                break;
            }
            if (super.parseAsKeyword(line, br)) {
                line = br.readLine();
                continue;
            } else if (SOURCE.equals(line)) {
                String next = br.readLine();
                this.sourceFile = next.trim();
            } else {
                logger.error(R.string.patch_error_cannot_parse,
                        br.getCurrentLine(), line);
            }
            line = br.readLine();
        }
    }

    @Override
    public String executeRule(ApkInfoActivity activity, ZipFile patchZip,
                              IPatchContext logger) {

        ZipEntry entry = patchZip.getEntry(sourceFile);
        if (entry == null) {
            logger.error(R.string.patch_error_no_entry, sourceFile);
            return null;
        }

        InputStream input = null;
        FileOutputStream fos = null;
        try {
            input = patchZip.getInputStream(entry);

            // Extract the zip file inside patch
            String tmpDir = SDCard.makeDir(activity, "tmp");
            String path = tmpDir + RandomUtil.getRandomString(6);
            fos = new FileOutputStream(path);
            IOUtils.copy(input, fos);
            fos.close();
            fos = null;

            // Refactor the new added Ids (public.xml)
            mergeIds(activity.getDecodeRootPath() + "/res/values/public.xml",
                    path, logger);

            // Extract files in zip to target folder
            addFilesInZip(activity, path,
                    new ResourceMerger(activity.getDecodeRootPath()), logger);

        } catch (Exception e) {
            logger.error(R.string.general_error, e.getMessage());
        } finally {
            closeQuietly(input);
            closeQuietly(fos);
        }

        return null;
    }

    // filepath specifies the zip file which contains res/values/public.xml
    private void mergeIds(String curPublicXml, String zipFilepath,
                          IPatchContext logger) throws Exception {
        ZipFile zfile = null;
        InputStream input = null;
        FileInputStream fis = null;

        try {
            zfile = new ZipFile(zipFilepath);
            ZipEntry entry = zfile.getEntry("res/values/public.xml");
            if (entry == null) {
                String message = logger
                        .getString(R.string.patch_error_publicxml_notfound);
                throw new Exception(message);
            }

            // Parse new added Ids
            input = zfile.getInputStream(entry);
            List<ResourceItem> addedItems = getResourceItems(input);

            // Parse original Ids
            fis = new FileInputStream(curPublicXml);
            List<ResourceItem> originItems = getResourceItems(fis);
            Map<String, Integer> type2maxId = getMaxIds(originItems);

            // Refactor new added Ids
            this.replacedIds = refactorAddedItems(addedItems, type2maxId);

            // Save to public.xml
            writeAddedItems(curPublicXml, addedItems);
        } finally {
            closeQuietly(fis);
            closeQuietly(input);
            closeQuietly(zfile);
        }
    }

    private void writeAddedItems(String curPublicXml,
                                 List<ResourceItem> addedItems) throws Exception {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < addedItems.size(); ++i) {
            ResourceItem item = addedItems.get(i);
            lines.add(item.toString());
        }

        this.appendResourceLines(curPublicXml, lines);
    }

    // Append some lines to target resource file
    // lines does not contain "</resources>"
    private void appendResourceLines(String resourceFile, List<String> lines)
            throws Exception {
        RandomAccessFile randomFile = null;
        try {
            randomFile = new RandomAccessFile(resourceFile, "rw");
            long fileLength = randomFile.length();
            if (fileLength < 16) {
                // In general, should not happen
                throw new Exception("File is too small!");
            }

            // Find the position of "</resources>"
            randomFile.seek(fileLength - 16);
            byte[] buffer = new byte[32];
            int readBytes = randomFile.read(buffer);
            int i = 0;
            for (; i < readBytes; ++i) {
                if (buffer[i] == '<' && buffer[i + 1] == '/') {
                    break;
                }
            }

            // Append added lines
            randomFile.seek(fileLength - 16 + i);
            StringBuilder sb = new StringBuilder();
            for (i = 0; i < lines.size(); ++i) {
                sb.append(lines.get(i));
                sb.append("\n");
            }
            sb.append("</resources>");
            randomFile.write(sb.toString().getBytes());

        } finally {
            closeQuietly(randomFile);
        }
    }

    // Refactor added resource items, according to current existing max ids
    private SparseIntArray refactorAddedItems(List<ResourceItem> addedItems,
                                              Map<String, Integer> type2maxId) {

        // old id -> new id
        SparseIntArray replaces = new SparseIntArray();

        for (int i = 0; i < addedItems.size(); ++i) {
            ResourceItem item = addedItems.get(i);
            Integer curMaxId = type2maxId.get(item.type);
            if (curMaxId != null) {
                int newId = curMaxId + 1;
                replaces.put(item.id, newId);
                item.id = newId;
                type2maxId.put(item.type, newId);
            }
            // The original apk does not contain such resources
            else {
                // Note: maxType does not need to multiply 2^16
                int maxType = getMaxType(type2maxId);
                int newId = 0x7f000000 + ((maxType + 1) << 16);
                replaces.put(item.id, newId);
                item.id = newId;
                type2maxId.put(item.type, newId);
            }
        }

        return replaces;
    }

    // Get max resource type in current editing apk
    private int getMaxType(Map<String, Integer> type2maxId) {
        int maxType = 0;
        for (Integer val : type2maxId.values()) {
            int curType = val & 0x00ff0000;
            if (curType > maxType) {
                maxType = curType;
            }
        }
        return (maxType >> 16);
    }

    private Map<String, Integer> getMaxIds(List<ResourceItem> items) {
        Map<String, Integer> maxIds = new HashMap<>();
        int drawableMaxId = 0;
        int layoutMaxId = 0;
        int stringMaxId = 0;
        for (ResourceItem item : items) {
            if ("drawable".equals(item.type)) {
                if (item.id > drawableMaxId) {
                    drawableMaxId = item.id;
                }
            } else if ("layout".equals(item.type)) {
                if (item.id > layoutMaxId) {
                    layoutMaxId = item.id;
                }
            } else if ("string".equals(item.type)) {
                if (item.id > stringMaxId) {
                    stringMaxId = item.id;
                }
            } else {
                Integer curMax = maxIds.get(item.type);
                if (curMax == null || item.id > curMax) {
                    maxIds.put(item.type, item.id);
                }
            }
        }
        maxIds.put("drawable", drawableMaxId);
        maxIds.put("layout", layoutMaxId);
        maxIds.put("string", stringMaxId);
        return maxIds;
    }

    private List<ResourceItem> getResourceItems(InputStream input)
            throws IOException {
        List<ResourceItem> result = new ArrayList<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(input));

        String line;
        while ((line = br.readLine()) != null) {
            ResourceItem item = ResourceItem.parseFrom(line);
            if (item != null) {
                result.add(item);
            }
        }

        return result;
    }

    @Override
    public boolean isValid(IPatchContext logger) {
        if (sourceFile == null) {
            logger.error(R.string.patch_error_no_source_file);
            return false;
        }
        return true;
    }

    @Override
    public boolean isSmaliNeeded() {
        return true;
    }

    // Merge resources under values/ values-xx/
    private class ResourceMerger implements IBeforeAddFile {
        private String rootPath;

        ResourceMerger(String rootPath) {
            this.rootPath = rootPath;
        }

        @Override
        public boolean consumeAddedFile(ApkInfoActivity activity,
                                        ZipFile zfile, ZipEntry entry) throws Exception {
            String name = entry.getName();
            String targetPath = this.rootPath + "/" + name;
            if ("res/values/public.xml".equals(name)) {
                return true;
            }

            // Deal with smali files
            if (replacedIds != null && name.endsWith(".smali")
                    && (name.startsWith("smali/") || name.startsWith("smali_"))) {
                int pos = name.indexOf('/');
                if (pos != -1) {
                    refactorAndSaveSmaliFiles(targetPath, zfile, entry);
                    // Make a trick to notify smali folder modified
                    String fakeSmali = name.substring(0, pos + 1) + "a.smali";
                    activity.getResListAdapter().fileModified(fakeSmali,
                            targetPath);
                    return true;
                }
            }

            // The same file not exist, directly add
            File f = new File(targetPath);
            if (!f.exists()) {
                // Make sure res file is copied
                if (name.startsWith("res/")) {
                    f.getParentFile().mkdirs();
                }
                return false;
            }

            // Merge resource file
            if (name.endsWith(".xml")) {
                String[] paths = name.split("/");
                if (paths.length == 3
                        && paths[0].equals("res")
                        && (paths[1].equals("values") || paths[1]
                        .startsWith("values-"))) {
                    mergeResourceFiles(targetPath, zfile, entry);
                    return true;
                }
            }

            return false;
        }

        // Refactor the id inside smali file, and then save to target path
        private void refactorAndSaveSmaliFiles(String targetPath,
                                               ZipFile zfile, ZipEntry entry) throws IOException {
            List<String> lines = readZipEntry(zfile, entry);
            BufferedWriter bw = null;
            try {
                // Must create parent folder if it does not exist
                File outFile = new File(targetPath);
                File parentFolder = outFile.getParentFile();
                if (!parentFolder.exists()) {
                    parentFolder.mkdirs();
                }
                bw = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(targetPath)));
                for (int i = 0; i < lines.size(); ++i) {
                    String line = lines.get(i);
                    line = refactorId(line);
                    bw.write(line);
                    bw.write('\n');
                }
            } finally {
                closeQuietly(bw);
            }
        }

        // Refactor one line of a smali file
        private String refactorId(String line) {
            boolean idModified = false;

            int pos = line.indexOf("0x7f");
            while (pos != -1 && (pos + 10) <= line.length()) {
                String originStr = line.substring(pos, pos + 10);
                int originId = ResourceItem.string2Id(originStr);
                int newId = replacedIds.get(originId);
                if (newId != 0) {
                    line = line.replace(originStr,
                            ResourceItem.id2String(newId));
                    idModified = true;
                } else {
                    Log.e("DEBUG", "Cannot find id " + originStr);
                }

                pos = line.indexOf("0x7f", pos + 10);
            }

            // special case: change "const/high16 v0, 0x7f030000"
            // to "const v0, 0x7f030000"
            if (idModified) {
                if (line.trim().startsWith("const/high16 v")) {
                    line = line.replace("const/high16 v", "const v");
                }
            }

            return line;
        }

        private List<String> readZipEntry(ZipFile zfile, ZipEntry entry)
                throws IOException {
            List<String> lines = new ArrayList<>();
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(
                        zfile.getInputStream(entry)));
                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
            } finally {
                closeQuietly(br);
            }
            return lines;
        }

        // Merge resources in zip entry to the original path
        private void mergeResourceFiles(String path, ZipFile zfile,
                                        ZipEntry entry) throws Exception {
            BufferedReader br = null;
            try {
                // Collect resource lines
                br = new BufferedReader(new InputStreamReader(
                        zfile.getInputStream(entry)));
                List<String> items = new ArrayList<>();
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("<?xml")
                            || line.startsWith("<resources>")
                            || line.startsWith("</resources>")) {
                        //continue;
                    } else {
                        items.add(line);
                    }
                }

                // Append resource lines to target file
                appendResourceLines(path, items);
            } finally {
                closeQuietly(br);
            }
        }
    }
}
