package com.gmail.heagoo.apkeditor.patch;

import com.gmail.heagoo.apkeditor.ApkInfoActivity;
import com.gmail.heagoo.apkeditor.IGeneralCallback;
import com.gmail.heagoo.apkeditor.base.R;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PatchExecutor implements IGeneralCallback {

    private WeakReference<ApkInfoActivity> activityRef;
    private String patchPath;
    private IPatchContext patchContext;

    // patch and source zip file
    private Patch patch;
    private ZipFile sourceZip;

    public PatchExecutor(ApkInfoActivity activity, String patchPath,
                         IPatchContext logger) {
        this.activityRef = new WeakReference<>(activity);
        this.patchPath = patchPath;
        this.patchContext = logger;
    }

    public void applyPatch() {
        // Parse the patch
        try {
            this.sourceZip = new ZipFile(patchPath);
            ZipEntry entry = sourceZip.getEntry("patch.txt");
            if (entry == null) {
                sourceZip.close();
                sourceZip = null;
                patchContext.error(R.string.patch_error_no_entry, "patch.txt");
            }

            InputStream input = sourceZip.getInputStream(entry);
            this.patch = PatchParser.parse(input, patchContext);
            input.close();
        } catch (Exception e) {
            patchContext.error(R.string.general_error, e.getMessage());
            e.printStackTrace();
            return;
        }

        boolean needToDecode = false;

        // Check if need to decode DEX file
        if (!activityRef.get().isDexDecoded()) {
            for (PatchRule rule : patch.rules) {
                needToDecode = rule.isSmaliNeeded();
                if (needToDecode) {
                    break;
                }
            }

            if (needToDecode) {
                patchContext.info(R.string.decode_dex_file, true);
                activityRef.get().decodeDex(this);
            }
        }

        // When do not need to decode DEX, directly apply patch
        if (!needToDecode) {
            applyRules(patch.rules, sourceZip);
        }
    }

    private void applyRules(final List<PatchRule> rules, final ZipFile sourceZip) {
        new Thread() {
            @Override
            public void run() {
                // Apply all the rules
                int index = 0;
                while (index < rules.size()) {
                    PatchRule rule = rules.get(index);
                    patchContext.info(R.string.patch_start_apply, true, rule.startLine);

                    String nextRule = null;
                    if (rule.isValid(patchContext)) {
                        nextRule = rule.executeRule(activityRef.get(), sourceZip, patchContext);
                    }
                    // Goto the target rule
                    if (nextRule != null) {
                        index = findTargetRule(rules, nextRule);
                        continue;
                    }

                    index += 1;
                }
                patchContext.info(R.string.all_rules_applied, true);
                patchContext.patchFinished();
            }
        }.start();
    }

    // Get the index of the target rule
    private int findTargetRule(List<PatchRule> rules, String name) {
        for (int i = 0; i < rules.size(); ++i) {
            if (name.equals(rules.get(i).getRuleName())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void callbackFunc() {
        if (patch != null && patch.rules != null && sourceZip != null) {
            applyRules(patch.rules, sourceZip);
        }
    }

    public List<String> getRuleNames() {
        List<String> names = new ArrayList<>();
        if (patch != null && patch.rules != null) {
            for (PatchRule rule : patch.rules) {
                names.add(rule.getRuleName());
            }
        }
        return names;
    }
}
