package com.gmail.heagoo.neweditor;

import java.util.ArrayList;
import java.util.List;

public class KeywordMap {
    private boolean ignoreCase;
    private Keyword[] map;
    private int mapLength;
    private StringBuilder noWordSep;

    public KeywordMap(boolean ignoreCase) {
        this(ignoreCase, 52);
        this.ignoreCase = ignoreCase;
        this.noWordSep = new StringBuilder();
    }

    public KeywordMap(boolean ignoreCase, int mapLength) {
        this.mapLength = mapLength;
        this.ignoreCase = ignoreCase;
        this.map = new Keyword[mapLength];
    }

    public byte lookup(Segment text, int offset, int length) {
        if (length == 0) {
            return (byte) 0;
        }
        Keyword k = this.map[getSegmentMapKey(text, offset, length)];
        while (k != null) {
            if (length != k.keyword.length) {
                k = k.next;
            } else if (SyntaxUtilities.regionMatches(this.ignoreCase, text,
                    offset, k.keyword)) {
                return k.id;
            } else {
                k = k.next;
            }
        }
        return (byte) 0;
    }

    public void add(String keyword, byte id) {
        add(keyword.toCharArray(), id);
    }

    public void add(char[] keyword, byte id) {
        int key = getStringMapKey(keyword);
        for (char ch : keyword) {
            if (!Character.isLetterOrDigit(ch)) {
                for (int j = 0; j < this.noWordSep.length(); j++) {
                    if (this.noWordSep.charAt(j) == ch) {
                        break;
                    }
                }
                this.noWordSep.append(ch);
            }
        }
        this.map[key] = new Keyword(keyword, id, this.map[key]);
    }

    public String getNonAlphaNumericChars() {
        return this.noWordSep.toString();
    }

    public String[] getKeywords() {
        List<String> vector = new ArrayList<String>(100);
        for (Keyword keyword2 = this.map[0]; keyword2 != null; keyword2 = keyword2.next) {
            vector.add(new String(keyword2.keyword));
        }
        String[] retVal = new String[vector.size()];
        vector.toArray(retVal);
        return retVal;
    }

    public boolean getIgnoreCase() {
        return this.ignoreCase;
    }

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    public void add(KeywordMap map) {
        for (Keyword k2 = map.map[0]; k2 != null; k2 = k2.next) {
            add(k2.keyword, k2.id);
        }
    }

    private int getStringMapKey(char[] s) {
        return (Character.toUpperCase(s[0]) + Character
                .toUpperCase(s[s.length - 1])) % this.mapLength;
    }

    protected int getSegmentMapKey(Segment s, int off, int len) {
        return (Character.toUpperCase(s.array[off]) + Character
                .toUpperCase(s.array[(off + len) - 1])) % this.mapLength;
    }

    private static class Keyword {
        public byte id;
        public char[] keyword;
        public Keyword next;

        Keyword(char[] str, byte id, Keyword next) {
            this.keyword = str;
            this.id = id;
            this.next = next;
        }
    }
}
