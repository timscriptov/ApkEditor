package com.gmail.heagoo.apkeditor.util;

import android.content.Context;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.TextFileReader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FixInvalidToken extends FixInvalid {
    private final static String err_InvalidToken = ": error: Error parsing XML: not well-formed (invalid token)";

    private int modifiedFiles = 0;

    // Record modification for each files
    private Map<String, Map<String, String>> fileModifications = new HashMap<String, Map<String, String>>();
    private List<InvalidTokenRecord> invalidRecords = new ArrayList<InvalidTokenRecord>();

    public FixInvalidToken(String decodeRootPath, String message) {
        super(decodeRootPath, message);
        try {
            parseErrorMessage();
        } catch (IOException e) {
        }
    }

    // Parse invalid tokens
    private void parseErrorMessage() throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new StringReader(this.errMessage));
            String line = br.readLine();
            while (line != null) {
                int pos = line.indexOf(err_InvalidToken);
                if (pos > 0) {
                    String pathAndLine = line.substring(0, pos);
                    int commaPos = pathAndLine.lastIndexOf(':');
                    if (commaPos == -1) {
                        continue;
                    }
                    String path = pathAndLine.substring(0, commaPos);
                    String strLine = pathAndLine.substring(commaPos + 1);
                    int lineNO = -1;
                    try {
                        lineNO = Integer.valueOf(strLine);
                    } catch (Exception e) {
                        continue;
                    }

                    InvalidTokenRecord rec = getInvalidRecord(path);
                    if (rec == null) {
                        rec = new InvalidTokenRecord(path, lineNO);
                        invalidRecords.add(rec);
                    }
                }
                line = br.readLine();
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    // Get record by file path
    private InvalidTokenRecord getInvalidRecord(String path) {
        for (int i = 0; i < invalidRecords.size(); i++) {
            InvalidTokenRecord rec = invalidRecords.get(i);
            if (path.equals(rec.filePath)) {
                return rec;
            }
        }
        return null;
    }

    @Override
    public boolean isErrorFixable() {
        return invalidRecords.size() > 0;
    }

    @Override
    public void fixErrors() {
        fixInvalidTokens();
    }

    private void fixInvalidTokens() {
        // Record replaced tags/tokens
        Map<String, String> allReplaces = new HashMap<String, String>();

        for (int i = 0; i < invalidRecords.size(); i++) {
            InvalidTokenRecord rec = invalidRecords.get(i);
            reviseTokens(rec.filePath, rec.lineIndex, allReplaces);
        }
    }

    // Revise tokens for a file
    private void reviseTokens(String filePath, int startLine,
                              Map<String, String> allReplaces) {
        BufferedWriter bw = null;

        try {
            boolean modified = false;
            Map<String, String> replaces = new HashMap<String, String>();

            TextFileReader reader = new TextFileReader(filePath);
            List<String> lines = reader.getLines();
            // Search from 1, not startLine, as the reported line may lag
            // (if the previous invalid token already reported)
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);

                int pos = line.indexOf('<');
                if (pos == -1) {
                    continue;
                }

                if (!line.endsWith(">")) {
                    continue;
                }

                int tagStart = -1, tagEnd = -1;
                String token = null;

                // Get tag name from the line
                if (line.charAt(pos + 1) == '/') {
                    tagStart = pos + 2;
                    tagEnd = line.length() - 1;
                } else {
                    tagEnd = line.indexOf(' ', pos);
                    tagStart = pos + 1;
                }
                if (tagEnd <= tagStart) {
                    continue;
                }

                // a normal tag or not
                token = line.substring(tagStart, tagEnd);
                if (token.matches("^[a-zA-Z0-9._]+$")) {
                    continue;
                }

                String strBefore = line.substring(0, tagStart);
                String strAfter = line.substring(tagEnd);
                String newLine = null;

                // Check already replaced or not
                String newToken = allReplaces.get(token);
                if (newToken == null) {
                    newToken = ErrorFixManager.makeValidToken(token);
                    allReplaces.put(token, newToken);
                }

                replaces.put(token, newToken);
                newLine = strBefore + newToken + strAfter;
//                Log.d("DEBUG", line + " -> " + newLine);
                lines.set(i, newLine);
                modified = true;
            }

            // Write to file
            if (modified) {
                bw = new BufferedWriter(new FileWriter(filePath));
                for (String line : lines) {
                    bw.write(line);
                    bw.write("\n");
                }

                fileModifications.put(filePath, replaces);
                this.modifiedFiles += 1;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ErrorFixManager.closeQuietly(bw);
        }
    }

    @Override
    public boolean succeeded() {
        return modifiedFiles > 0;
    }

    @Override
    public String getMofifyMessage(Context ctx) {
        String msg = String.format(
                ctx.getString(R.string.str_num_modified_file), modifiedFiles);
        return msg;
    }

    @Override
    public Map<String, Map<String, String>> getAxmlModifications() {
        return this.fileModifications;
    }

    static class InvalidTokenRecord {
        String filePath;

        // This is just the start line which contains invalid token
        int lineIndex;

        public InvalidTokenRecord(String filePath, int lineNO) {
            this.filePath = filePath;
            this.lineIndex = (lineNO > 0 ? (lineNO - 1) : 0);
        }
    }
}
