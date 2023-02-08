package com.gmail.heagoo.apkeditor.patch;

import com.gmail.heagoo.apkeditor.ApkInfoActivity;
import com.gmail.heagoo.apkeditor.base.R;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipFile;

class PatchRule_MatchReplace extends PatchRule {

    private static final String strEnd = "[/MATCH_REPLACE]";
    private static final String TARGET = "TARGET:";
    private static final String MATCH = "MATCH:";
    private static final String REGEX = "REGEX:";
    private static final String REPLACE = "REPLACE:";
    private static final String DOTALL = "DOTALL:";

    // private String targetFile;
    private PathFinder pathFinder;
    private List<String> matches;
    private List<String> replaces;
    private String replacingStr = null; // concat all lines in replaces
    private boolean bRegex = false;
    private boolean bDotall = false;

    private List<String> keywords;

    // The target file name is specified by wildchar or not
    private boolean isWildMatch;

    PatchRule_MatchReplace() {
        matches = new ArrayList<>();
        replaces = new ArrayList<>();
        keywords = new ArrayList<>();
        keywords.add(strEnd);
        keywords.add(TARGET);
        keywords.add(MATCH);
        keywords.add(REGEX);
        keywords.add(REPLACE);
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
            } else if (REPLACE.equals(line)) {
                line = readMultiLines(br, replaces, false, keywords);
                continue;
            } else {
                logger.error(R.string.patch_error_cannot_parse,
                        br.getCurrentLine(), line);
            }
            line = br.readLine();
        }

        if (pathFinder != null) {
            this.isWildMatch = pathFinder.isWildMatch();
        }
    }

    @Override
    public String executeRule(ApkInfoActivity activity, ZipFile patchZip,
                              IPatchContext logger) {
        preProcessing(logger, matches);
        preProcessing(logger, replaces);

        // Try to compile the regular expression
        Pattern pattern = null;
        if (this.bRegex) {
            // Only use the first line
            String regStr = matches.get(0);
            try {
                pattern = (bDotall ? Pattern.compile(regStr.trim(), Pattern.DOTALL)
                        : Pattern.compile(regStr.trim()));
            } catch (PatternSyntaxException e) {
                logger.error(R.string.patch_error_regex_syntax, e.getMessage());
                return null;
            }
        }

        String nextPath = pathFinder.getNextPath();
        while (nextPath != null) {
            executeOnEntry(activity, patchZip, logger, nextPath, pattern);
            nextPath = pathFinder.getNextPath();
        }
        return null;
    }

    private void executeOnEntry(ApkInfoActivity activity, ZipFile patchZip,
                                IPatchContext patchCtx, String targetFile, Pattern pattern) {
        boolean modified = false;
        String filepath = activity.getDecodeRootPath() + "/" + targetFile;

        if (pattern != null) {
            // Load all the content
            String content;
            try {
                content = readFileContent(filepath);
            } catch (IOException e) {
                patchCtx.error(R.string.patch_error_read_from, targetFile);
                return;
            }

            // To record all matched positions
            List<Section> sections = new ArrayList<>();


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

            if (sections.isEmpty()) {
                if (!isWildMatch) { // For wild match, not log it, as too many
                    patchCtx.error(R.string.patch_error_no_match, targetFile);
                }
            } else {
                try {
                    writeReplaces(filepath, content, sections);
                    modified = true;
                    String message = patchCtx.getString(R.string.patch_info_num_replaced);
                    message = targetFile + ": " + String.format(message, sections.size());
                    patchCtx.info(message, false);
                } catch (IOException e) {
                    patchCtx.error(R.string.patch_error_write_to, targetFile);
                }
            }
        }

        // NOT regex
        else {
            List<String> lines;
            try {
                lines = super.readFileLines(filepath);
            } catch (IOException e) {
                patchCtx.error(R.string.patch_error_read_from, targetFile);
                return;
            }

            // Try to find all the matches
            List<Integer> matchedIndexes = new ArrayList<>();
            for (int i = 0; i < lines.size() - this.matches.size() + 1; ++i) {
                boolean ret = checkMatch(lines, i);
                if (ret) {
                    matchedIndexes.add(i);
                    i += this.matches.size() - 1;
                }
            }

            // Save to file
            if (matchedIndexes.isEmpty()) {
                patchCtx.error(R.string.patch_error_no_match, targetFile);
            } else {
                try {
                    writeReplaces(filepath, lines, matchedIndexes);
                    modified = true;
                    patchCtx.info(R.string.patch_info_num_replaced, false,
                            matchedIndexes.size());
                } catch (IOException e) {
                    patchCtx.error(R.string.patch_error_write_to, targetFile);
                }
            }
        }

        // The file is indeed modified
        if (modified) {
            if ("AndroidManifest.xml".equals(targetFile)) {
                activity.setManifestModified(false);
            } else {
                activity.getResListAdapter().fileModified(targetFile, filepath);
            }
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

        // All lines matched
        return (i == this.matches.size());
    }

    // content: original file content
    // sections: replacement position
    private void writeReplaces(String filepath, String content,
                               List<Section> sections) throws IOException {
        String replaceStr = getReplaceString();

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filepath);

            int startPos = 0;
            for (int i = 0; i < sections.size(); ++i) {
                Section sec = sections.get(i);
                int endPos = sec.start;
                fos.write(content.substring(startPos, endPos).getBytes());
                startPos = sec.end;

                String curReplace;
                if (sec.groupStrs != null && !sec.groupStrs.isEmpty()) {
                    curReplace = getRealReplace(replaceStr, sec);
                } else {
                    curReplace = replaceStr;
                }
                fos.write(curReplace.getBytes());
            }

            // Write the remain string
            fos.write(content.substring(startPos).getBytes());
        } finally {
            closeQuietly(fos);
        }
    }

    private String getRealReplace(String replaceStr, Section sec) {
        String result = replaceStr;
        List<String> groups = sec.groupStrs;
        for (int i = 0; i < groups.size(); ++i) {
            result = result.replace("${GROUP" + (i + 1) + "}", groups.get(i));
        }
        return result;
    }

    private void writeReplaces(String filepath, List<String> lines,
                               List<Integer> matchedIndexes) throws IOException {
        String replaceStr = getReplaceString();

        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(filepath));

            int startIdx = 0;
            for (int i = 0; i < matchedIndexes.size(); ++i) {
                int curIdx = matchedIndexes.get(i);
                writeLines(out, lines, startIdx, curIdx);
                out.write(replaceStr.getBytes());
                out.write("\n".getBytes());
                startIdx = curIdx + this.matches.size();
            }

            // Write the remain string
            writeLines(out, lines, startIdx, lines.size());
        } finally {
            closeQuietly(out);
        }
    }

    private void writeLines(BufferedOutputStream out, List<String> lines,
                            int startIdx, int endIdx) throws IOException {
        for (int i = startIdx; i < endIdx; ++i) {
            out.write(lines.get(i).getBytes());
            out.write("\n".getBytes());
        }
    }

    // Get the replacement string
    private String getReplaceString() {
        if (this.replacingStr == null) {
            if (this.replaces.isEmpty()) {
                this.replacingStr = "";
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(replaces.get(0));
                for (int i = 1; i < replaces.size(); ++i) {
                    sb.append("\n");
                    sb.append(replaces.get(i));
                }
                this.replacingStr = sb.toString();
            }
        }
        return this.replacingStr;
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
