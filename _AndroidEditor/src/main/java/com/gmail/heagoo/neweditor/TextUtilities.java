package com.gmail.heagoo.neweditor;

import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

public class TextUtilities {
    public static final int BRACKET_MATCH_LIMIT = 10000;
    public static final int LOWER_CASE = 1;
    public static final int MIXED = 0;
    public static final int SYMBOL = 2;
    public static final int TITLE_CASE = 3;
    public static final int UPPER_CASE = 2;
    public static final int WHITESPACE = 0;
    public static final int WORD_CHAR = 1;

    public static Token getTokenAtOffset(Token tokens, int offset) {
        if (!(offset == 0 && tokens.id == Token.END)) {
            while (tokens.id != Token.END) {
                if (tokens.offset + tokens.length <= offset) {
                    tokens = tokens.next;
                }
            }
            throw new ArrayIndexOutOfBoundsException("offset > line length");
        }
        return tokens;
    }

    public static char getComplementaryBracket(char ch, boolean[] direction) {
        switch (ch) {
            case 40 /* 40 */:
                if (direction != null) {
                    direction[WHITESPACE] = true;
                }
                return ')';
            case ')':
                if (direction != null) {
                    direction[WHITESPACE] = false;
                }
                return '(';
            case 60 /* 60 */:
                if (direction != null) {
                    direction[WHITESPACE] = true;
                }
                return '>';
            case '>':
                if (direction != null) {
                    direction[WHITESPACE] = false;
                }
                return '<';
            case '[':
                if (direction != null) {
                    direction[WHITESPACE] = true;
                }
                return ']';
            case ']':
                if (direction != null) {
                    direction[WHITESPACE] = false;
                }
                return '[';
            case 123 /* 123 */:
                if (direction != null) {
                    direction[WHITESPACE] = true;
                }
                return '}';
            case 125 /* 125 */:
                if (direction != null) {
                    direction[WHITESPACE] = false;
                }
                return '{';
            default:
                return '\u0000';
        }
    }

    public static String join(Collection<?> c, String delim) {
        StringBuilder retval = new StringBuilder();
        Iterator<?> itr = c.iterator();
        if (!itr.hasNext()) {
            return "";
        }
        retval.append(itr.next());
        while (itr.hasNext()) {
            retval.append(delim);
            retval.append(itr.next());
        }
        return retval.toString();
    }

    public static int findWordStart(String line, int pos, String noWordSep) {
        return findWordStart(line, pos, noWordSep, true, false);
    }

    public static int findWordStart(CharSequence line, int pos, String noWordSep) {
        return findWordStart(line, pos, noWordSep, true, false, false);
    }

    public static int findWordStart(String line, int pos, String noWordSep,
                                    boolean joinNonWordChars) {
        return findWordStart(line, pos, noWordSep, joinNonWordChars, false);
    }

    public static int findWordStart(String line, int pos, String noWordSep,
                                    boolean joinNonWordChars, boolean eatWhitespace) {
        return findWordStart(line, pos, noWordSep, joinNonWordChars, false,
                eatWhitespace);
    }

    public static int findWordStart(String line, int pos, String noWordSep,
                                    boolean joinNonWordChars, boolean camelCasedWords,
                                    boolean eatWhitespace) {
        return findWordStart((CharSequence) line, pos, noWordSep,
                joinNonWordChars, camelCasedWords, eatWhitespace);
    }

    public static int findWordStart(CharSequence line, int pos,
                                    String noWordSep, boolean joinNonWordChars,
                                    boolean camelCasedWords, boolean eatWhitespace) {
        return findWordStart(line, pos, noWordSep, joinNonWordChars,
                camelCasedWords, eatWhitespace, false);
    }

    public static int findWordStart(CharSequence line, int pos,
                                    String noWordSep, boolean joinNonWordChars,
                                    boolean camelCasedWords, boolean eatWhitespace,
                                    boolean eatOnlyAfterWord) {
        char ch = line.charAt(pos);
        if (noWordSep == null) {
            noWordSep = "";
        }
        int type = getCharType(ch, noWordSep);
        int i = pos;
        while (i >= 0) {
            char lastCh = ch;
            ch = line.charAt(i);
            switch (type) {
                case WHITESPACE /* 0 */:
                    if (!Character.isWhitespace(ch)) {
                        if (eatOnlyAfterWord) {
                            if (!Character.isLetterOrDigit(ch)
                                    && noWordSep.indexOf(ch) == -1) {
                                type = UPPER_CASE;
                                break;
                            }
                            type = WORD_CHAR;
                            break;
                        }
                        return i + WORD_CHAR;
                    }
                    continue;
                    // break;
                case WORD_CHAR /* 1 */:
                    if (camelCasedWords && Character.isUpperCase(ch)
                            && !Character.isUpperCase(lastCh)
                            && Character.isLetterOrDigit(lastCh)) {
                        return i;
                    }
                    if (!camelCasedWords || Character.isUpperCase(ch)
                            || !Character.isUpperCase(lastCh)) {
                        if (!Character.isLetterOrDigit(ch)
                                && noWordSep.indexOf(ch) == -1) {
                            if (Character.isWhitespace(ch) && eatWhitespace
                                    && !eatOnlyAfterWord) {
                                type = WHITESPACE;
                                break;
                            }
                            return i + WORD_CHAR;
                        }
                    }
                    return i + WORD_CHAR;
                // break;
                case UPPER_CASE /* 2 */:
                    if (joinNonWordChars || pos == i) {
                        if (!Character.isWhitespace(ch)) {
                            if (!Character.isLetterOrDigit(ch)
                                    && noWordSep.indexOf(ch) == -1) {
                                break;
                            }
                            return i + WORD_CHAR;
                        } else if (eatWhitespace && !eatOnlyAfterWord) {
                            type = WHITESPACE;
                            break;
                        } else {
                            return i + WORD_CHAR;
                        }
                    }
                    return i + WORD_CHAR;
                // break;
                default:
                    break;
            }
            i--;
        }
        return WHITESPACE;
    }

    public static int findWordEnd(String line, int pos, String noWordSep) {
        return findWordEnd(line, pos, noWordSep, true);
    }

    public static int findWordEnd(CharSequence line, int pos, String noWordSep) {
        return findWordEnd(line, pos, noWordSep, true, false, false);
    }

    public static int findWordEnd(String line, int pos, String noWordSep,
                                  boolean joinNonWordChars) {
        return findWordEnd(line, pos, noWordSep, joinNonWordChars, false);
    }

    public static int findWordEnd(String line, int pos, String noWordSep,
                                  boolean joinNonWordChars, boolean eatWhitespace) {
        return findWordEnd(line, pos, noWordSep, joinNonWordChars, false,
                eatWhitespace);
    }

    public static int findWordEnd(String line, int pos, String noWordSep,
                                  boolean joinNonWordChars, boolean camelCasedWords,
                                  boolean eatWhitespace) {
        return findWordEnd((CharSequence) line, pos, noWordSep,
                joinNonWordChars, camelCasedWords, eatWhitespace);
    }

    public static int findWordEnd(CharSequence line, int pos, String noWordSep,
                                  boolean joinNonWordChars, boolean camelCasedWords,
                                  boolean eatWhitespace) {
        if (pos != 0) {
            pos--;
        }
        char ch = line.charAt(pos);
        if (noWordSep == null) {
            noWordSep = "";
        }
        int type = getCharType(ch, noWordSep);
        int i = pos;
        while (i < line.length()) {
            char lastCh = ch;
            ch = line.charAt(i);
            switch (type) {
                case WHITESPACE /* 0 */:
                    if (Character.isWhitespace(ch)) {
                        break;
                    }
                    return i;
                case WORD_CHAR /* 1 */:
                    if (camelCasedWords && i > pos + WORD_CHAR
                            && !Character.isUpperCase(ch)
                            && Character.isLetterOrDigit(ch)
                            && Character.isUpperCase(lastCh)) {
                        return i - 1;
                    }
                    if (!camelCasedWords || !Character.isUpperCase(ch)
                            || Character.isUpperCase(lastCh)) {
                        if (!Character.isLetterOrDigit(ch)
                                && noWordSep.indexOf(ch) == -1) {
                            if (Character.isWhitespace(ch) && eatWhitespace) {
                                type = WHITESPACE;
                                break;
                            }
                            return i;
                        }
                    }
                    return i;
                //break;
                case UPPER_CASE /* 2 */:
                    if (!joinNonWordChars && i != pos) {
                        return i;
                    }
                    if (Character.isWhitespace(ch)) {
                        if (eatWhitespace) {
                            type = WHITESPACE;
                            break;
                        }
                        return i;
                    } else if (!Character.isLetterOrDigit(ch)) {
                        if (noWordSep.indexOf(ch) == -1) {
                            break;
                        }
                        return i;
                    } else {
                        return i;
                    }
                default:
                    break;
            }
            i += WORD_CHAR;
        }
        return line.length();
    }

    public static int getCharType(char ch, String noWordSep) {
        if (Character.isWhitespace(ch)) {
            return WHITESPACE;
        }
        if (Character.isLetterOrDigit(ch) || noWordSep.indexOf(ch) != -1) {
            return WORD_CHAR;
        }
        return UPPER_CASE;
    }

    public static String tabsToSpaces(String in, int tabSize) {
        StringBuilder buf = new StringBuilder();
        int width = WHITESPACE;
        for (int i = WHITESPACE; i < in.length(); i += WORD_CHAR) {
            switch (in.charAt(i)) {
                case 9 /* 9 */:
                    int count = tabSize - (width % tabSize);
                    width += count;
                    while (true) {
                        count--;
                        if (count < 0) {
                            break;
                        }
                        buf.append(' ');
                    }
                case 10 /* 10 */:
                    width = WHITESPACE;
                    buf.append(in.charAt(i));
                    break;
                default:
                    width += WORD_CHAR;
                    buf.append(in.charAt(i));
                    break;
            }
        }
        return buf.toString();
    }

    public static String format(String text, int maxLineLength, int tabSize) {
        StringBuilder buf = new StringBuilder();
        int index = WHITESPACE;
        while (true) {
            int newIndex = text.indexOf("\n\n", index);
            if (newIndex == -1) {
                break;
            }
            formatParagraph(text.substring(index, newIndex), maxLineLength,
                    tabSize, buf);
            buf.append("\n\n");
            index = newIndex + UPPER_CASE;
        }
        if (index != text.length()) {
            formatParagraph(text.substring(index), maxLineLength, tabSize, buf);
        }
        return buf.toString();
    }

    public static int indexIgnoringWhitespace(String str, int index) {
        int j = WHITESPACE;
        for (int i = WHITESPACE; i < index; i += WORD_CHAR) {
            if (!Character.isWhitespace(str.charAt(i))) {
                j += WORD_CHAR;
            }
        }
        return j;
    }

    public static int ignoringWhitespaceIndex(String str, int index) {
        int j = WHITESPACE;
        int i = WHITESPACE;
        while (true) {
            if (!Character.isWhitespace(str.charAt(i))) {
                j += WORD_CHAR;
            }
            if (j > index) {
                return i;
            }
            if (i == str.length() - 1) {
                return i + WORD_CHAR;
            }
            i += WORD_CHAR;
        }
    }

    public static int getStringCase(CharSequence str) {
        if (str.length() == 0) {
            return WHITESPACE;
        }
        int state = -1;
        char ch = str.charAt(WHITESPACE);
        if (Character.isLetter(ch)) {
            if (Character.isUpperCase(ch)) {
                state = UPPER_CASE;
            } else {
                state = WORD_CHAR;
            }
        }
        for (int i = WORD_CHAR; i < str.length(); i += WORD_CHAR) {
            ch = str.charAt(i);
            if (Character.isLetter(ch)) {
                switch (state) {
                    case WORD_CHAR /* 1 */:
                    case TITLE_CASE /* 3 */:
                        if (!Character.isUpperCase(ch)) {
                            break;
                        }
                        return WHITESPACE;
                    case UPPER_CASE /* 2 */:
                        if (!Character.isLowerCase(ch)) {
                            continue;
                        } else if (i == WORD_CHAR) {
                            state = TITLE_CASE;
                            break;
                        } else {
                            return WHITESPACE;
                        }
                    default:
                        break;
                }
            }
        }
        return state;
    }

    public static int getStringCase(String str) {
        return getStringCase((CharSequence) str);
    }

    public static String toTitleCase(String str) {
        return str.length() == 0 ? str : new StringBuilder(
                String.valueOf(Character.toUpperCase(str.charAt(WHITESPACE))))
                .append(str.substring(WORD_CHAR).toLowerCase()).toString();
    }

    public static String escapeText(String text) {
        return "\\Q" + text.replace("\\E", "\\\\E") + "\\E";
    }

    private static void formatParagraph(String text, int maxLineLength,
                                        int tabSize, StringBuilder buf) {
        String leadingWhitespace = text.substring(WHITESPACE,
                StandardUtilities.getLeadingWhiteSpace(text));
        int leadingWhitespaceWidth = StandardUtilities
                .getLeadingWhiteSpaceWidth(text, tabSize);
        buf.append(leadingWhitespace);
        int lineLength = leadingWhitespaceWidth;
        StringTokenizer st = new StringTokenizer(text);
        while (st.hasMoreTokens()) {
            String word = st.nextToken();
            if (lineLength != leadingWhitespaceWidth) {
                if ((word.length() + lineLength) + WORD_CHAR > maxLineLength) {
                    buf.append('\n');
                    buf.append(leadingWhitespace);
                    lineLength = leadingWhitespaceWidth;
                } else {
                    buf.append(' ');
                    lineLength += WORD_CHAR;
                }
            }
            buf.append(word);
            lineLength += word.length();
        }
    }

    public static void indexIgnoringWhitespace(String text, int maxLineLength,
                                               int tabSize, StringBuffer buf) {
        String leadingWhitespace = text.substring(WHITESPACE,
                StandardUtilities.getLeadingWhiteSpace(text));
        int leadingWhitespaceWidth = StandardUtilities
                .getLeadingWhiteSpaceWidth(text, tabSize);
        buf.append(leadingWhitespace);
        int lineLength = leadingWhitespaceWidth;
        StringTokenizer st = new StringTokenizer(text);
        while (st.hasMoreTokens()) {
            String word = st.nextToken();
            if (lineLength != leadingWhitespaceWidth) {
                if ((word.length() + lineLength) + WORD_CHAR > maxLineLength) {
                    buf.append('\n');
                    buf.append(leadingWhitespace);
                    lineLength = leadingWhitespaceWidth;
                } else {
                    buf.append(' ');
                    lineLength += WORD_CHAR;
                }
            }
            buf.append(word);
            lineLength += word.length();
        }
    }
}