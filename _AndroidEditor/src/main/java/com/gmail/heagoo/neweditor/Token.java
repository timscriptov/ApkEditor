package com.gmail.heagoo.neweditor;

public class Token {
    public static final byte COMMENT1 = (byte) 1;
    public static final byte COMMENT2 = (byte) 2;
    public static final byte COMMENT3 = (byte) 3;
    public static final byte COMMENT4 = (byte) 4;
    public static final byte DIGIT = (byte) 5;
    public static final byte END = Byte.MAX_VALUE;
    public static final byte FUNCTION = (byte) 6;
    public static final byte ID_COUNT = (byte) 19;
    public static final byte INVALID = (byte) 7;
    public static final byte KEYWORD1 = (byte) 8;
    public static final byte KEYWORD2 = (byte) 9;
    public static final byte KEYWORD3 = (byte) 10;
    public static final byte KEYWORD4 = (byte) 11;
    public static final byte LABEL = (byte) 12;
    public static final byte LITERAL1 = (byte) 13;
    public static final byte LITERAL2 = (byte) 14;
    public static final byte LITERAL3 = (byte) 15;
    public static final byte LITERAL4 = (byte) 16;
    public static final byte MARKUP = (byte) 17;
    public static final byte NULL = (byte) 0;
    public static final byte OPERATOR = (byte) 18;
    public static final String[] TOKEN_TYPES = new String[]{"NULL",
            "COMMENT1", "COMMENT2", "COMMENT3", "COMMENT4", "DIGIT",
            "FUNCTION", "INVALID", "KEYWORD1", "KEYWORD2", "KEYWORD3",
            "KEYWORD4", "LABEL", "LITERAL1", "LITERAL2", "LITERAL3",
            "LITERAL4", "MARKUP", "OPERATOR"};
    public byte id;
    public int length;
    public Token next;
    public int offset;
    public ParserRuleSet rules;

    public Token(byte id, int offset, int length, ParserRuleSet rules) {
        this.id = id;
        this.offset = offset;
        this.length = length;
        this.rules = rules;
    }

    public static byte stringToToken(String value) {
        try {
            return Token.class.getField(value).getByte(null);
        } catch (Exception e) {
            return (byte) -1;
        }
    }

    public static String tokenToString(byte token) {
        return token == END ? "END" : TOKEN_TYPES[token];
    }

    public String toString() {
        return "[id=" + this.id + ",offset=" + this.offset + ",length="
                + this.length + "]";
    }
}