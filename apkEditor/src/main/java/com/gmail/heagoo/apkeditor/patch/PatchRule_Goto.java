package com.gmail.heagoo.apkeditor.patch;

import com.gmail.heagoo.apkeditor.ApkInfoActivity;
import com.gmail.heagoo.apkeditor.base.R;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipFile;


class PatchRule_Goto extends PatchRule {

    private static final String strEnd = "[/GOTO]";
    private static final String GOTO = "GOTO:";

    private String targetRule;

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
            } else if (GOTO.equals(line)) {
                String next = br.readLine();
                this.targetRule = next.trim();
            } else {
                logger.error(R.string.patch_error_cannot_parse,
                        br.getCurrentLine(), line);
            }
            line = br.readLine();
        }
    }

    @Override
    public String executeRule(ApkInfoActivity activity, ZipFile patchZip, IPatchContext logger) {
        return targetRule;
    }

    @Override
    public boolean isValid(IPatchContext logger) {
        if (this.targetRule == null) {
            logger.error(R.string.patch_error_no_goto_target);
            return false;
        }

        List<String> allRuleName = logger.getPatchNames();
        if (allRuleName == null || !allRuleName.contains(targetRule)) {
            logger.error(R.string.patch_error_goto_target_notfound, targetRule);
            return false;
        }

        return true;
    }

    @Override
    public boolean isSmaliNeeded() {
        return false;
    }
}
