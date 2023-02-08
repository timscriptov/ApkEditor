package com.gmail.heagoo.apkeditor.patch;

import com.gmail.heagoo.apkeditor.ApkInfoActivity;
import com.gmail.heagoo.apkeditor.base.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

class PatchRule_MatchAssign extends PatchRule {

    private static final String strEnd = "[/MATCH_ASSIGN]";
    private static final String TARGET = "TARGET:";
    private static final String MATCH = "MATCH:";
    private static final String REGEX = "REGEX:";
    private static final String ASSIGN = "ASSIGN:";
    private static final String DOTALL = "DOTALL:";

    // private String targetFile;
    private PathFinder pathFinder;
    private List<String> matches;
    private List<String> assigns;
    private boolean bRegex = false;
    private boolean bDotall = false;

    private List<String> keywords;

    PatchRule_MatchAssign() {
        matches = new ArrayList<>();
        assigns = new ArrayList<>();
        keywords = new ArrayList<>();
        keywords.add(strEnd);
        keywords.add(TARGET);
        keywords.add(MATCH);
        keywords.add(REGEX);
        keywords.add(ASSIGN);
        keywords.add(DOTALL);
    }

    @Override
    public void parseFrom(LinedReader br, IPatchContext logger)
            throws IOException {
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
                String next = br.readLine();
                String pathStr = next.trim();
                this.pathFinder = new PathFinder(logger, pathStr,
                        br.getCurrentLine());
            } else if (REGEX.equals(line)) {
                String next = br.readLine();
                this.bRegex = Boolean.valueOf(next.trim());
            } else if (DOTALL.equals(line)) {
                String next = br.readLine();
                this.bDotall = Boolean.valueOf(next.trim());
            } else if (MATCH.equals(line)) {
                line = readMultiLines(br, matches, true, keywords);
                continue;
            } else if (ASSIGN.equals(line)) {
                line = readMultiLines(br, assigns, false, keywords);
                continue;
            } else {
                logger.error(R.string.patch_error_cannot_parse, br.getCurrentLine(), line);
            }
            line = br.readLine();
        }
    }

    @Override
    public String executeRule(ApkInfoActivity activity, ZipFile patchZip, IPatchContext logger) {
        preProcessing(logger, matches);

        String nextPath = pathFinder.getNextPath();
        while (nextPath != null) {
            if (executeOnEntry(activity, patchZip, logger, nextPath)) {
                break;
            }
            nextPath = pathFinder.getNextPath();
        }
        return null;
    }

    private boolean executeOnEntry(ApkInfoActivity activity, ZipFile patchZip,
                                   IPatchContext patchCtx, String targetFile) {
        String filepath = activity.getDecodeRootPath() + "/" + targetFile;

        // Load all the content
        String content;
        try {
            content = readFileContent(filepath);
        } catch (IOException e) {
            patchCtx.error(R.string.patch_error_read_from, targetFile);
            return false;
        }

        // Only use the first line
        String regStr = matches.get(0);
        Pattern pattern = (bDotall ? Pattern.compile(regStr.trim(), Pattern.DOTALL)
                : Pattern.compile(regStr.trim()));
        Matcher m = pattern.matcher(content);
        if (m.find(0)) {
            List<String> groupStrs = new ArrayList<>();
            int groupCount = m.groupCount();
            if (groupCount > 0) {
                for (int i = 0; i < groupCount; ++i) {
                    groupStrs.add(m.group(i + 1));
                }
            }

            // Do the assign
            for (String strAssign : assigns) {
                strAssign = strAssign.trim();
                int position = strAssign.indexOf('=');
                if (position != -1) {
                    String name = strAssign.substring(0, position);
                    String valueBeforeAssign = strAssign.substring(position + 1);
                    String assignedVal = getRealValue(valueBeforeAssign, groupStrs);
                    patchCtx.setVariableValue(name, assignedVal);
                    patchCtx.info("%s=\"%s\"", false, name, assignedVal);
                }
            }

            return true;
        }

        return false;
    }


    private String getRealValue(String valueBefore, List<String> groupStrs) {
        String result = valueBefore;
        for (int i = 0; i < groupStrs.size(); ++i) {
            result = result.replace("${GROUP" + (i + 1) + "}", groupStrs.get(i));
        }
        return result;
    }

    @Override
    public boolean isValid(IPatchContext logger) {
        if (pathFinder == null) {
            return false;
        }
        if (!pathFinder.isValid()) {
            return false;
        }
        if (this.matches.isEmpty()) {
            logger.error(R.string.patch_error_no_match_content);
            return false;
        }
        if (!this.bRegex) {
            logger.error(R.string.patch_error_regex_not_true);
            return false;
        }
        return true;
    }

    @Override
    public boolean isSmaliNeeded() {
        return pathFinder.isSmaliNeeded();
    }

}
