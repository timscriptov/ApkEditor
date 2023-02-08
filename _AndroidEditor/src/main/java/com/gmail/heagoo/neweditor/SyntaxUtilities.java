package com.gmail.heagoo.neweditor;


public class SyntaxUtilities {
    public static boolean regionMatches(boolean ignoreCase, Segment text,
                                        int offset, char[] match) {
        int length = offset + match.length;
        if (length > text.offset + text.count) {
            return false;
        }
        char[] textArray = text.array;
        int i = offset;
        int j = 0;
        while (i < length) {
            char c1 = textArray[i];
            char c2 = match[j];
            if (ignoreCase) {
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
            }
            if (c1 != c2) {
                return false;
            }
            i++;
            j++;
        }
        return true;
    }
}