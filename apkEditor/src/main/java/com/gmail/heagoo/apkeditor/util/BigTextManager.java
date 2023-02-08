package com.gmail.heagoo.apkeditor.util;

import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by phe3 on 7/11/2017.
 */

public class BigTextManager {
    private String text;

    // Line index inside the EditText when wrap
    // lineLines[0] = 0
    // lineLines[1] = 3, means the first line (line index is 0) occupy 3 lines
    private int[] lineLines = new int[16384];
    private List<String> strLines = new ArrayList<>();

    public BigTextManager(String text, float widthPx, Paint paint) {
        this.text = text;
        parseLines(widthPx, paint);
    }

    public static boolean isEmpty(String str) {
        return str == null || "".equals(str);
    }

    public static String join(String strConcat, List<String> elements) {
        StringBuilder sb = new StringBuilder();
        if (!elements.isEmpty()) {
            sb.append(elements.get(0));
        }
        for (int i = 1; i < elements.size(); ++i) {
            sb.append(strConcat);
            sb.append(elements.get(i));
        }
        return sb.toString();
    }

    private void parseLines(float widthPx, Paint paint) {
        int lineIdx = 0;
        int lineInEt = 0; // line index in EditText

        int position = 0;
        int endPos = text.indexOf('\n', position);
        while (endPos != -1) {
            String line = text.substring(position, endPos);
            List<String> ret = splitWordsIntoStringsThatFit(line, widthPx, paint);
            strLines.add(line);
            setLineStartAt(lineIdx, lineInEt);

            lineIdx += 1;
            lineInEt += ret.size();
            position = endPos + 1;
            endPos = text.indexOf('\n', position);
        }

        // Last line
        String lastLine = text.substring(position);
        strLines.add(lastLine);
        setLineStartAt(lineIdx, lineInEt);

        // Guard
        List<String> ret = splitWordsIntoStringsThatFit(lastLine, widthPx, paint);
        setLineStartAt(lineIdx + 1, lineInEt + ret.size());
    }

    private void setLineStartAt(int lineIdx, int lineInEt) {
        if (lineIdx >= lineLines.length) {
            final int increaseSize = 8192;
            int[] arr = new int[lineLines.length + increaseSize];
            System.arraycopy(lineLines, 0, arr, 0, lineLines.length);
            lineLines = arr;
        }
        lineLines[lineIdx] = lineInEt;
    }

    // Return the padded partial text
    // startLine: line index in EditText
    public String getPartialText(int startLineInEt, int linesInEt) {
        if (startLineInEt < 0) {
            startLineInEt = 0;
        }

        // Find the real start line inside the text
        int lineNum = strLines.size();
        int startLineInText = 0;
        int endLineInText = lineNum;
        int i;
        for (i = 1; i < lineNum; ++i) {
            if (lineLines[i] > startLineInEt) {
                startLineInText = i - 1;
                break;
            }
        }
        for (; i < lineNum; ++i) {
            if (lineLines[i] - startLineInEt >= linesInEt) {
                endLineInText = i + 1;
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        appendPadding(sb, lineLines[startLineInText]);
        for (int t = startLineInText; t < endLineInText; ++t) {
            sb.append(strLines.get(t));
            sb.append("\n");
        }
        if (endLineInText < lineNum) {
            appendPadding(sb, lineLines[lineNum] - lineLines[endLineInText]);
        }

        return sb.toString();
    }

    private void appendPadding(StringBuilder sb, int lines) {
        String sixteen = "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n";
        int i;
        for (i = 0; i < lines - 15; i += 16) {
            sb.append(sixteen);
        }
        for (; i < lines; ++i) {
            sb.append("\n");
        }
    }

    public List<String> splitWordsIntoStringsThatFit(String source, float maxWidthPx, Paint paint) {
        ArrayList<String> result = new ArrayList<>();

        ArrayList<String> currentLine = new ArrayList<>();

        String[] sources = source.split("\\s");
        for (String chunk : sources) {
            if (paint.measureText(chunk) < maxWidthPx) {
                processFitChunk(maxWidthPx, paint, result, currentLine, chunk);
            } else {
                //the chunk is too big, split it.
                List<String> splitChunk = splitIntoStringsThatFit(chunk, maxWidthPx, paint);
                for (String chunkChunk : splitChunk) {
                    processFitChunk(maxWidthPx, paint, result, currentLine, chunkChunk);
                }
            }
        }

        if (!currentLine.isEmpty()) {
            result.add(BigTextManager.join(" ", currentLine));
        }
        return result;
    }

    /**
     * Splits a string to multiple strings each of which does not exceed the width
     * of maxWidthPx.
     */
    private List<String> splitIntoStringsThatFit(String source, float maxWidthPx, Paint paint) {
        if (BigTextManager.isEmpty(source) || paint.measureText(source) <= maxWidthPx) {
            return Arrays.asList(source);
        }

        ArrayList<String> result = new ArrayList<>();
        int start = 0;
        for (int i = 1; i <= source.length(); i++) {
            String substr = source.substring(start, i);
            if (paint.measureText(substr) >= maxWidthPx) {
                //this one doesn't fit, take the previous one which fits
                String fits = source.substring(start, i - 1);
                result.add(fits);
                start = i - 1;
            }
            if (i == source.length()) {
                String fits = source.substring(start, i);
                result.add(fits);
            }
        }

        return result;
    }

    /**
     * Processes the chunk which does not exceed maxWidth.
     */
    private void processFitChunk(float maxWidth, Paint paint, ArrayList<String> result, ArrayList<String> currentLine, String chunk) {
        currentLine.add(chunk);
        String currentLineStr = BigTextManager.join(" ", currentLine);
        if (paint.measureText(currentLineStr) >= maxWidth) {
            //remove chunk
            currentLine.remove(currentLine.size() - 1);
            result.add(BigTextManager.join(" ", currentLine));
            currentLine.clear();
            //ok because chunk fits
            currentLine.add(chunk);
        }
    }
}
