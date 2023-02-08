package com.gmail.heagoo.neweditor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ParserRule {
    public static final int ACTION_HINTS = 65280;
    public static final int AT_LINE_START = 2;
    public static final int AT_WHITESPACE_END = 4;
    public static final int AT_WORD_START = 8;
    public static final int END_REGEXP = 16384;
    public static final int EOL_SPAN = 16;
    public static final int IS_ESCAPE = 2048;
    public static final int MAJOR_ACTIONS = 255;
    public static final int MARK_FOLLOWING = 8;
    public static final int MARK_PREVIOUS = 4;
    public static final byte MATCH_TYPE_CONTEXT = (byte) -1;
    public static final byte MATCH_TYPE_RULE = (byte) -2;
    public static final int NO_LINE_BREAK = 512;
    public static final int NO_WORD_BREAK = 1024;
    public static final int REGEXP = 8192;
    public static final int SEQ = 0;
    public static final int SPAN = 2;
    public final int action;
    public final char[] end;
    public final int endPosMatch;
    public final Pattern endRegexp;
    public final ParserRule escapeRule;
    public final byte matchType;
    public final char[] start;
    public final int startPosMatch;
    public final Pattern startRegexp;
    public final byte token;
    public final char[] upHashChar;
    public final char[] upHashChars;
    public ParserRuleSet delegate;

    private ParserRule(int action, String hashChar, int startPosMatch,
                       char[] start, Pattern startRegexp, int endPosMatch, char[] end,
                       Pattern endRegexp, ParserRuleSet delegate, byte token,
                       byte matchType, String escape) {
        ParserRule parserRule = null;
        this.action = action;
        this.upHashChar = hashChar == null ? null : hashChar.toUpperCase()
                .toCharArray();
        this.upHashChars = null;
        this.startPosMatch = startPosMatch;
        this.start = start;
        this.startRegexp = startRegexp;
        this.endPosMatch = endPosMatch;
        this.end = end;
        this.endRegexp = endRegexp;
        this.delegate = delegate;
        this.token = token;
        this.matchType = matchType;
        if (escape != null && escape.length() > 0) {
            parserRule = createEscapeRule(escape);
        }
        this.escapeRule = parserRule;
        if (this.delegate == null && (action & MAJOR_ACTIONS) != 0) {
            this.delegate = ParserRuleSet.getStandardRuleSet(token);
        }
    }

    private ParserRule(char[] hashChars, int action, int startPosMatch,
                       char[] start, Pattern startRegexp, int endPosMatch, char[] end,
                       Pattern endRegexp, ParserRuleSet delegate, byte token,
                       byte matchType, String escape) {
        this.action = action;
        this.upHashChar = null;
        Set<Character> hashCharsSet = new HashSet();
        int length = hashChars.length;
        for (int i = SEQ; i < length; i++) {
            hashCharsSet.add(Character.valueOf(Character
                    .toUpperCase(hashChars[i])));
        }
        this.upHashChars = new char[hashCharsSet.size()];
        int i2 = SEQ;
        for (Character c : hashCharsSet) {
            int i3 = i2 + 1;
            this.upHashChars[i2] = c.charValue();
            i2 = i3;
        }
        Arrays.sort(this.upHashChars);
        this.startPosMatch = startPosMatch;
        this.start = start;
        this.startRegexp = startRegexp;
        this.endPosMatch = endPosMatch;
        this.end = end;
        this.endRegexp = endRegexp;
        this.delegate = delegate;
        this.token = token;
        this.matchType = matchType;
        ParserRule createEscapeRule = (escape == null || escape.length() <= 0) ? null
                : createEscapeRule(escape);
        this.escapeRule = createEscapeRule;
        if (this.delegate == null && (action & MAJOR_ACTIONS) != 0) {
            this.delegate = ParserRuleSet.getStandardRuleSet(token);
        }
    }

    public static ParserRule createSequenceRule(int posMatch, String seq,
                                                ParserRuleSet delegate, byte id) {
        return new ParserRule((int) SEQ, seq.substring(SEQ, 1), posMatch,
                seq.toCharArray(), null, (int) SEQ, null, null, delegate, id,
                (byte) MATCH_TYPE_CONTEXT, null);
    }

    public static ParserRule createRegexpSequenceRule(String hashChar,
                                                      int posMatch, String seq, ParserRuleSet delegate, byte id,
                                                      boolean ignoreCase) throws PatternSyntaxException {
        return new ParserRule((int) REGEXP, hashChar, posMatch, null,
                Pattern.compile(seq, ignoreCase ? SPAN : SEQ), (int) SEQ, null,
                null, delegate, id, (byte) MATCH_TYPE_CONTEXT, null);
    }

    public static ParserRule createRegexpSequenceRule(int posMatch,
                                                      char[] hashChars, String seq, ParserRuleSet delegate, byte id,
                                                      boolean ignoreCase) throws PatternSyntaxException {
        return new ParserRule(hashChars, (int) REGEXP, posMatch, null,
                Pattern.compile(seq, ignoreCase ? SPAN : SEQ), (int) SEQ, null,
                null, delegate, id, (byte) MATCH_TYPE_CONTEXT, null);
    }

    public static ParserRule createSpanRule(int startPosMatch, String start,
                                            int endPosMatch, String end, ParserRuleSet delegate, byte id,
                                            byte matchType, boolean noLineBreak, boolean noWordBreak,
                                            String escape) {
        return new ParserRule(((noLineBreak ? NO_LINE_BREAK : SEQ) | SPAN)
                | (noWordBreak ? NO_WORD_BREAK : SEQ), start.substring(SEQ, 1),
                startPosMatch, start.toCharArray(), null, endPosMatch,
                end.toCharArray(), null, delegate, id, matchType, escape);
    }

    public static ParserRule createRegexpSpanRule(String hashChar,
                                                  int startPosMatch, String start, int endPosMatch, String end,
                                                  ParserRuleSet delegate, byte id, byte matchType,
                                                  boolean noLineBreak, boolean noWordBreak, boolean ignoreCase,
                                                  String escape, boolean endRegexp) throws PatternSyntaxException {
        Pattern endRegexpPattern;
        char[] endArray;
        // 8194 = FragmentTransaction.TRANSIT_FRAGMENT_CLOSE
        int ruleAction = ((noLineBreak ? NO_LINE_BREAK : SEQ) | 8194)
                | (noWordBreak ? NO_WORD_BREAK : SEQ);
        if (endRegexp) {
            ruleAction |= END_REGEXP;
            endRegexpPattern = Pattern.compile(end, ignoreCase ? SPAN : SEQ);
            endArray = null;
        } else {
            endRegexpPattern = null;
            endArray = end.toCharArray();
        }
        return new ParserRule(ruleAction, hashChar, startPosMatch, null,
                Pattern.compile(start, ignoreCase ? SPAN : SEQ), endPosMatch,
                endArray, endRegexpPattern, delegate, id, matchType, escape);
    }

    public static ParserRule createRegexpSpanRule(int startPosMatch,
                                                  char[] hashChars, String start, int endPosMatch, String end,
                                                  ParserRuleSet delegate, byte id, byte matchType,
                                                  boolean noLineBreak, boolean noWordBreak, boolean ignoreCase,
                                                  String escape, boolean endRegexp) throws PatternSyntaxException {
        Pattern endRegexpPattern;
        char[] endArray;
        // 8194 = FragmentTransaction.TRANSIT_FRAGMENT_CLOSE
        int ruleAction = ((noLineBreak ? NO_LINE_BREAK : SEQ) | 8194)
                | (noWordBreak ? NO_WORD_BREAK : SEQ);
        if (endRegexp) {
            ruleAction |= END_REGEXP;
            endRegexpPattern = Pattern.compile(end, ignoreCase ? SPAN : SEQ);
            endArray = null;
        } else {
            endRegexpPattern = null;
            endArray = end.toCharArray();
        }
        return new ParserRule(hashChars, ruleAction, startPosMatch, null,
                Pattern.compile(start, ignoreCase ? SPAN : SEQ), endPosMatch,
                endArray, endRegexpPattern, delegate, id, matchType, escape);
    }

    public static ParserRule createEOLSpanRule(int posMatch, String seq,
                                               ParserRuleSet delegate, byte id, byte matchType) {
        return new ParserRule(528, seq.substring(SEQ, 1), posMatch,
                seq.toCharArray(), null, (int) SEQ, null, null, delegate, id,
                matchType, null);
    }

    public static ParserRule createRegexpEOLSpanRule(String hashChar,
                                                     int posMatch, String seq, ParserRuleSet delegate, byte id,
                                                     byte matchType, boolean ignoreCase) throws PatternSyntaxException {
        return new ParserRule(8720, hashChar, posMatch, null, Pattern.compile(
                seq, ignoreCase ? SPAN : SEQ), (int) SEQ, null, null, delegate,
                id, matchType, null);
    }

    public static ParserRule createRegexpEOLSpanRule(int posMatch,
                                                     char[] hashChars, String seq, ParserRuleSet delegate, byte id,
                                                     byte matchType, boolean ignoreCase) throws PatternSyntaxException {
        return new ParserRule(hashChars, 8720, posMatch, null, Pattern.compile(
                seq, ignoreCase ? SPAN : SEQ), (int) SEQ, null, null, delegate,
                id, matchType, null);
    }

    public static ParserRule createMarkFollowingRule(int posMatch, String seq,
                                                     byte id, byte matchType) {
        return new ParserRule((int) MARK_FOLLOWING, seq.substring(SEQ, 1),
                posMatch, seq.toCharArray(), null, (int) SEQ, null, null, null,
                id, matchType, null);
    }

    public static ParserRule createMarkPreviousRule(int posMatch, String seq,
                                                    byte id, byte matchType) {
        return new ParserRule((int) MARK_PREVIOUS, seq.substring(SEQ, 1),
                posMatch, seq.toCharArray(), null, (int) SEQ, null, null, null,
                id, matchType, null);
    }

    public static ParserRule createEscapeRule(String seq) {
        return new ParserRule((int) IS_ESCAPE, seq.substring(SEQ, 1),
                (int) SEQ, seq.toCharArray(), null, SEQ, null, null, null,
                (byte) 0, (byte) MATCH_TYPE_CONTEXT, null);
    }

    public String toString() {
        boolean z;
        String str = null;
        boolean z2 = true;
        StringBuilder result = new StringBuilder();
        result.append(getClass().getName()).append("[action=");
        switch (this.action & MAJOR_ACTIONS) {
            case SEQ /* 0 */:
                result.append("SEQ");
                break;
            case SPAN /* 2 */:
                result.append("SPAN");
                break;
            case MARK_PREVIOUS /* 4 */:
                result.append("MARK_PREVIOUS");
                break;
            case MARK_FOLLOWING /* 8 */:
                result.append("MARK_FOLLOWING");
                break;
            case EOL_SPAN /* 16 */:
                result.append("EOL_SPAN");
                break;
            default:
                result.append("UNKNOWN");
                break;
        }
        int actionHints = this.action & ACTION_HINTS;
        StringBuilder append = result.append("[matchType=");
        String tokenToString = this.matchType == MATCH_TYPE_CONTEXT ? "MATCH_TYPE_CONTEXT"
                : this.matchType == MATCH_TYPE_RULE ? "MATCH_TYPE_RULE" : Token
                .tokenToString(this.matchType);
        append.append(tokenToString);
        append = result.append(",NO_LINE_BREAK=");
        if ((actionHints & NO_LINE_BREAK) != 0) {
            z = true;
        } else {
            z = false;
        }
        append.append(z);
        append = result.append(",NO_WORD_BREAK=");
        if ((actionHints & NO_WORD_BREAK) != 0) {
            z = true;
        } else {
            z = false;
        }
        append.append(z);
        append = result.append(",IS_ESCAPE=");
        if ((actionHints & IS_ESCAPE) != 0) {
            z = true;
        } else {
            z = false;
        }
        append.append(z);
        append = result.append(",REGEXP=");
        if ((actionHints & REGEXP) != 0) {
            z = true;
        } else {
            z = false;
        }
        append.append(z);
        result.append("],upHashChar=").append(new String(this.upHashChar));
        result.append(",upHashChars=")
                .append(Arrays.toString(this.upHashChars));
        result.append(",startPosMatch=");
        append = result.append("[AT_LINE_START=");
        if ((this.startPosMatch & SPAN) != 0) {
            z = true;
        } else {
            z = false;
        }
        append.append(z);
        append = result.append(",AT_WHITESPACE_END=");
        if ((this.startPosMatch & MARK_PREVIOUS) != 0) {
            z = true;
        } else {
            z = false;
        }
        append.append(z);
        append = result.append(",AT_WORD_START=");
        if ((this.startPosMatch & MARK_FOLLOWING) != 0) {
            z = true;
        } else {
            z = false;
        }
        append.append(z);
        result.append("],start=").append(
                this.start == null ? null : String.valueOf(this.start));
        result.append(",startRegexp=").append(this.startRegexp);
        result.append(",endPosMatch=");
        append = result.append("[AT_LINE_START=");
        if ((this.endPosMatch & SPAN) != 0) {
            z = true;
        } else {
            z = false;
        }
        append.append(z);
        append = result.append(",AT_WHITESPACE_END=");
        if ((this.endPosMatch & MARK_PREVIOUS) != 0) {
            z = true;
        } else {
            z = false;
        }
        append.append(z);
        StringBuilder append2 = result.append(",AT_WORD_START=");
        if ((this.endPosMatch & MARK_FOLLOWING) == 0) {
            z2 = false;
        }
        append2.append(z2);
        append2 = result.append("],end=");
        if (this.end != null) {
            str = String.valueOf(this.end);
        }
        append2.append(str);
        result.append(",delegate=").append(this.delegate);
        result.append(",escapeRule=").append(this.escapeRule);
        result.append(",token=").append(Token.tokenToString(this.token))
                .append(']');
        return result.toString();
    }
}