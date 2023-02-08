package com.gmail.heagoo.apkeditor.util;

import android.content.Context;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.RandomUtil;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FixInvalidSymbol extends FixInvalid {
    private final static String err_InvalidSymbol = "^(.+):([0-9]+): error: invalid symbol: '(.+)'";
    private List<InvalidFile> invalidFiles = new ArrayList<>();

    FixInvalidSymbol(String decodeRootPath, String message) {
        super(decodeRootPath, message);
        try {
            parseErrorMessage();
        } catch (IOException ignored) {
        }
    }

    private void parseErrorMessage() throws IOException {
        BufferedReader br = null;
        try {
            Pattern pattern = Pattern.compile(err_InvalidSymbol);

            br = new BufferedReader(new StringReader(this.errMessage));
            String line = br.readLine();
            while (line != null) {
                Matcher m = pattern.matcher(line);
                if (m.find()) {
                    String file = line.substring(m.start(1), m.end(1));
                    String strLine = line.substring(m.start(2), m.end(2));
                    String symbol = line.substring(m.start(3), m.end(3));

                    int lineNO;
                    try {
                        lineNO = Integer.valueOf(strLine);
                    } catch (Exception e) {
                        continue;
                    }

                    FixInvalidSymbol.InvalidFile rec = getInvalidFile(file);
                    if (rec == null) {
                        rec = new FixInvalidSymbol.InvalidFile(decodeRootPath, file,
                                lineNO, symbol);
                        invalidFiles.add(rec);
                    } else {
                        rec.addInvalidSymbol(lineNO, symbol);
                    }
                }
                line = br.readLine();
            }
        } finally {
            closeQuietly(br);
        }
    }

    private InvalidFile getInvalidFile(String file) {
        if (!file.startsWith("/")) {
            file = decodeRootPath + file;
        }
        for (InvalidFile rec : this.invalidFiles) {
            if (file.equals(rec.filePath)) {
                return rec;
            }
        }
        return null;
    }

    @Override
    public boolean isErrorFixable() {
        return invalidFiles.size() > 0;
    }

    @Override
    public void fixErrors() {
        fixInvalidSymbols();
    }

    private void fixInvalidSymbols() {
        // public.xml
        String path = decodeRootPath + "res/values/public.xml";
        InvalidFile invalidFile = getInvalidFile(path);
        if (invalidFile != null) {
            modifyPublicXml(invalidFile);
        }

        // Modify declaration or file name
        if (invalidFile != null) { // Do it according to public.xml
            List<InvalidFile.InvalidSymbol> symbols = invalidFile.symbolList;
            Map<String, List<ResourceRename>> type2Symbols = arrangeSymbols(symbols);
            for (Map.Entry<String, List<ResourceRename>> entry : type2Symbols.entrySet()) {
                String type = entry.getKey();
                modifyDeclaration(type, entry.getValue());
            }
        }

        // Update reference
        List<ResourceRename> allRenames = collectAllRenames();
        if (allRenames != null && !allRenames.isEmpty()) {
            modifyReferences(allRenames);
        }
    }

    private List<ResourceRename> collectAllRenames() {
        List<ResourceRename> result = new ArrayList<>();
        for (InvalidFile invalidFile : this.invalidFiles) {
            // Enumerate all invalid symbols
            for (InvalidFile.InvalidSymbol is : invalidFile.symbolList) {
                if (is.isNewNameCreated() && !containsRecord(result, is.type, is.name)) {
                    ResourceRename rr = new ResourceRename(is.type, is.name, is.getNewName());
                    result.add(rr);
                }
            }
        }
        return result;
    }

    // Check the list whether already contains the rename record
    private boolean containsRecord(List<ResourceRename> renameList, String type, String name) {
        for (ResourceRename rr : renameList) {
            if (type.equals(rr.resourceType) && name.equals(rr.resourceName)) {
                return true;
            }
        }
        return false;
    }

    // Arrange the invalid symbols according to resource type
    private Map<String, List<ResourceRename>> arrangeSymbols(
            List<InvalidFile.InvalidSymbol> symbols) {
        Map<String, List<ResourceRename>> type2Symbols = new HashMap<>();
        for (InvalidFile.InvalidSymbol symbol : symbols) {
            List<ResourceRename> valueList = type2Symbols.get(symbol.type);
            if (valueList == null) {
                valueList = new ArrayList<>();
                type2Symbols.put(symbol.type, valueList);
            }
            valueList.add(new ResourceRename(symbol.type, symbol.name, symbol.getNewName()));
        }
        return type2Symbols;
    }

    private void modifyPublicXml(InvalidFile invalidFile) {
        try {
            TextFileReader reader = new TextFileReader(invalidFile.filePath);
            List<String> lines = reader.getLines();

            // Revise the invalid symbol
            for (InvalidFile.InvalidSymbol rec : invalidFile.symbolList) {
                if (rec.lineNumber > 0) {
                    int idx = rec.lineNumber - 1;
                    String line = lines.get(idx);
                    int pos = line.indexOf("type=\"");
                    if (pos != -1) {
                        int start = pos + 6;
                        int end = line.indexOf('\"', start);
                        if (end != -1) {
                            rec.type = line.substring(start, end);
                        }
                    }
                    String newLine = line.replace("name=\"" + rec.name + "\"",
                            "name=\"" + rec.getNewName() + "\"");
                    lines.set(idx, newLine);
                }
            }

            // Write back
            BufferedWriter bw = new BufferedWriter(new FileWriter(invalidFile.filePath));
            for (String line : lines) {
                bw.write(line);
                bw.write("\n");
            }
            bw.close();

            modifiedFileSet.add(invalidFile.filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean succeeded() {
        return renamedFileNum > 0 || !modifiedFileSet.isEmpty();
    }

    @Override
    public String getMofifyMessage(Context ctx) {
        // Show toast and then rebuild
        StringBuilder sb = new StringBuilder();
        if (renamedFileNum > 0) {
            sb.append(
                    String.format(ctx.getString(R.string.str_num_renamed_file),
                            renamedFileNum));
            sb.append("\n");
        }
        if (!modifiedFileSet.isEmpty()) {
            sb.append(
                    String.format(ctx.getString(R.string.str_num_modified_file),
                            modifiedFileSet.size()));
            sb.append("\n");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    // Suppose no modification, as we do not need to revise AXML file
    @Override
    public Map<String, Map<String, String>> getAxmlModifications() {
        return null;
    }

    private static class InvalidFile {
        // Absolute file path
        String filePath;
        List<InvalidSymbol> symbolList;

        private InvalidFile(String decodeRootPath, String path, int lineNumber, String symbol) {
            this.filePath = path;
            if (!this.filePath.startsWith("/")) {
                this.filePath = decodeRootPath + filePath;
            }
            this.symbolList = new ArrayList<>();
            InvalidSymbol rec = new InvalidSymbol(lineNumber, symbol);
            this.symbolList.add(rec);
        }

        private void addInvalidSymbol(int lineNumber, String symbol) {
            InvalidSymbol rec = new InvalidSymbol(lineNumber, symbol);
            this.symbolList.add(rec);
        }

        class InvalidSymbol {
            int lineNumber;
            String type;
            String name;
            private String _newName;

            private InvalidSymbol(int lineNumber, String name) {
                this.lineNumber = lineNumber;
                this.name = name;
            }

            public String getNewName() {
                if (_newName == null) {
                    _newName = createNewName();
                }
                return _newName;
            }

            private boolean isNewNameCreated() {
                return _newName != null;
            }

            // Create a new valid name
            private String createNewName() {
                return name + "_" + RandomUtil.getRandomString(4);
            }
        }
    }

}
