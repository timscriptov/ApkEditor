package com.gmail.heagoo.apkeditor.patch;

import com.gmail.heagoo.apkeditor.base.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class PatchParser {

    public static final String MIN_ENGINE_VER = "[MIN_ENGINE_VER]";
    public static final String AUTHOR = "[AUTHOR]";
    public static final String PACKAGE = "[PACKAGE]";

    public static final String ADD_FILES = "[ADD_FILES]";
    public static final String REMOVE_FILES = "[REMOVE_FILES]";
    public static final String MERGE = "[MERGE]";
    public static final String MATCH_REPLACE = "[MATCH_REPLACE]";
    public static final String MATCH_GOTO = "[MATCH_GOTO]";
    public static final String MATCH_ASSIGN = "[MATCH_ASSIGN]";
    public static final String GOTO = "[GOTO]";
    public static final String DUMMY = "[DUMMY]";
    public static final String FUNCTION_REPLACE = "[FUNCTION_REPLACE]";
    public static final String SIGNATURE_REVISE = "[SIGNATURE_REVISE]";
    public static final String EXECUTE_DEX = "[EXECUTE_DEX]";

    public static Patch parse(InputStream input, IPatchContext logger)
            throws Exception {
        logger.info(R.string.patch_start_parse, true);
        Patch result = new Patch();

        LinedReader br = new LinedReader(new InputStreamReader(input));
        String line = br.readLine();
        while (line != null) {
            line = line.trim();

            // Start a tag
            if (line.startsWith("[")) {
                if (MIN_ENGINE_VER.equals(line)) {
                    String next = br.readLine();
                    result.requiredEngine = Integer.valueOf(next);
                } else if (AUTHOR.equals(line)) {
                    String next = br.readLine();
                    result.author = next;
                } else if (PACKAGE.equals(line)) {
                    String next = br.readLine();
                    result.packagename = next;
                } else {
                    PatchRule rule = parseRule(br, line, logger);
                    if (rule != null) {
                        result.rules.add(rule);
                    }
                }
            } else if (line.startsWith("#") || "".equals(line)) {
                // comment or blank line
            } else {
                logger.error(R.string.patch_error_unknown_rule,
                        br.getCurrentLine(), line);
            }

            line = br.readLine();
        }

        return result;
    }

    private static PatchRule parseRule(LinedReader br, String startLine,
                                       IPatchContext logger) throws IOException {
        PatchRule rule = null;
        if (ADD_FILES.equals(startLine)) {
            rule = new PatchRule_AddFiles();
        } else if (REMOVE_FILES.equals(startLine)) {
            rule = new PatchRule_RemoveFiles();
        } else if (MERGE.equals(startLine)) {
            rule = new PatchRule_Merge();
        } else if (MATCH_REPLACE.equals(startLine)) {
            rule = new PatchRule_MatchReplace();
        } else if (MATCH_GOTO.equals(startLine)) {
            rule = new PatchRule_MatchGoto();
        } else if (MATCH_ASSIGN.equals(startLine)) {
            rule = new PatchRule_MatchAssign();
        } else if (FUNCTION_REPLACE.equals(startLine)) {
            rule = new PatchRule_FuncReplace();
        } else if (SIGNATURE_REVISE.equals(startLine)) {
            rule = new PatchRule_ReviseSig();
        } else if (GOTO.equals(startLine)) {
            rule = new PatchRule_Goto();
        } else if (DUMMY.equals(startLine)) {
            rule = new PatchRule_Dummy();
        } else if (EXECUTE_DEX.equals(startLine)) {
            rule = new PatchRule_ExecDex();
        } else {
            logger.error(R.string.patch_error_unknown_rule,
                    br.getCurrentLine(), startLine);
        }
        if (rule != null) {
            rule.parseFrom(br, logger);
        }
        return rule;
    }
}
