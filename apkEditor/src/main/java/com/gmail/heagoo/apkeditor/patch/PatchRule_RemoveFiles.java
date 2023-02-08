package com.gmail.heagoo.apkeditor.patch;

import com.gmail.heagoo.apkeditor.ApkInfoActivity;
import com.gmail.heagoo.apkeditor.ResListAdapter;
import com.gmail.heagoo.apkeditor.base.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

class PatchRule_RemoveFiles extends PatchRule {

    private static final String strEnd = "[/REMOVE_FILES]";
    private static final String TARGET = "TARGET:";

    private List<String> targetList = new ArrayList<>();

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
            } else if (TARGET.equals(line)) {
                String next;
                while ((next = br.readLine()) != null) {
                    next = next.trim();
                    if (next.startsWith("[")) {
                        break;
                    }
                    if (!"".equals(next)) {
                        this.targetList.add(next);
                    }
                }
                line = next;
                continue;
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
        String rootPath = activity.getDecodeRootPath();

        ResListAdapter resAdapter = activity.getResListAdapter();
        for (int i = 0; i < targetList.size(); ++i) {
            String targetPath = targetList.get(i);
            String filePath = rootPath + "/" + targetPath;
            int pos = filePath.lastIndexOf('/');
            String dirPath = filePath.substring(0, pos);
            String fileName = filePath.substring(pos + 1);

            File f = new File(filePath);
            if (f.exists()) {
                resAdapter.deleteFile(dirPath, fileName, false);
            } else {
                resAdapter.deleteFile(dirPath, fileName, true);
            }
        }

        return null;
    }

    @Override
    public boolean isValid(IPatchContext logger) {
        if (targetList.isEmpty()) {
            logger.error(R.string.patch_error_no_target_file);
            return false;
        }
        return true;
    }

    @Override
    public boolean isSmaliNeeded() {
        for (String file : targetList) {
            if (super.isInSmaliFolder(file)) {
                return true;
            }
        }
        return false;
    }

}
