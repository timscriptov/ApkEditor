package com.gmail.heagoo.apkeditor.patch;

import com.gmail.heagoo.apkeditor.ApkInfoActivity;
import com.gmail.heagoo.apkeditor.base.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

class PatchRule_MatchGoto extends PatchRule {

    private static final String strEnd = "[/MATCH_GOTO]";
    private static final String TARGET = "TARGET:";
    private static final String MATCH = "MATCH:";
    private static final String REGEX = "REGEX:";
    private static final String GOTO = "GOTO:";
    private static final String DOTALL = "DOTALL:";

    // private String targetFile;
    private PathFinder pathFinder;
    private List<String> matches;
    private String gotoRule;
    private boolean bRegex = false;
    private boolean bDotall = false;

    private List<String> keywords;

    PatchRule_MatchGoto() {
        matches = new ArrayList<>();
        keywords = new ArrayList<>();
        keywords.add(strEnd);
        keywords.add(TARGET);
        keywords.add(MATCH);
        keywords.add(REGEX);
        keywords.add(GOTO);
        keywords.add(DOTALL);
    }

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
                String next = br.readLine();
                String pathStr = next.trim();
                this.pathFinder = new PathFinder(logger, pathStr, br.getCurrentLine());
            } else if (REGEX.equals(line)) {
                String next = br.readLine();
                this.bRegex = Boolean.valueOf(next.trim());
            } else if (DOTALL.equals(line)) {
                String next = br.readLine();
                this.bDotall = Boolean.valueOf(next.trim());
            } else if (MATCH.equals(line)) {
                line = readMultiLines(br, matches, true, keywords);
                continue;
            } else if (GOTO.equals(line)) {
                this.gotoRule = br.readLine().trim();
                line = br.readLine();
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
            // If matches, then goto target rule
            if (entryMatches(activity, logger, nextPath)) {
                return gotoRule;
            }
            nextPath = pathFinder.getNextPath();
        }
        return null;
    }

    private boolean entryMatches(ApkInfoActivity activity,
                                 IPatchContext patchCtx, String targetFile) {
        String filepath = activity.getDecodeRootPath() + "/" + targetFile;

        if (this.bRegex) {
            // Load all the content
            String content;
            try {
                content = readFileContent(filepath);
            } catch (IOException e) {
                patchCtx.error(R.string.patch_error_read_from, targetFile);
                return false;
            }

            // To record all matched positions
            List<Section> sections = new ArrayList<>();

            // Only use the first line
            String regStr = matches.get(0);
            Pattern pattern = (bDotall ? Pattern.compile(regStr.trim(), Pattern.DOTALL)
                    : Pattern.compile(regStr.trim()));
            Matcher m = pattern.matcher(content);
            int position = 0;
            while (m.find(position)) {
                List<String> groupStrs = null;
                int groupCount = m.groupCount();
                if (groupCount > 0) {
                    groupStrs = new ArrayList<>(groupCount);
                    for (int i = 0; i < groupCount; ++i) {
                        groupStrs.add(m.group(i + 1));
                    }
                }
                sections.add(new Section(m.start(), m.end(), groupStrs));
                position = m.end();
            }

            return !sections.isEmpty();
        }

        // NOT regex
        else {
            List<String> lines;
            try {
                lines = super.readFileLines(filepath);
            } catch (IOException e) {
                patchCtx.error(R.string.patch_error_read_from, targetFile);
                return false;
            }

            // Try to find all the matches
            boolean matches = false;
            for (int i = 0; i < lines.size() - this.matches.size() + 1; ++i) {
                matches = checkMatch(lines, i);
                if (matches) {
                    break;
                }
            }

            return matches;
        }
    }

    // Check if the content started at idx matches or not
    private boolean checkMatch(List<String> lines, int idx) {
        // Try matches line by line (trimmed)
        int i;
        for (i = 0; i < this.matches.size(); ++i) {
            String content = lines.get(idx + i).trim();
            String matchStr = this.matches.get(i);
            if (!content.equals(matchStr)) {
                break;
            }
        }

        // All lines matched, then return true
        return (i == this.matches.size());
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
        if (this.gotoRule == null) {
            logger.error(R.string.patch_error_no_goto_target);
            return false;
        }

        List<String> allRuleName = logger.getPatchNames();
        if (allRuleName == null || !allRuleName.contains(gotoRule)) {
            logger.error(R.string.patch_error_goto_target_notfound, gotoRule);
            return false;
        }

        return true;
    }

    @Override
    public boolean isSmaliNeeded() {
        return pathFinder.isSmaliNeeded();
    }

    private static class Section {
        public int start;
        public int end;
        List<String> groupStrs;
        public Section(int _start, int _end, List<String> _groupStrs) {
            this.start = _start;
            this.end = _end;
            this.groupStrs = _groupStrs;
        }
    }

}
