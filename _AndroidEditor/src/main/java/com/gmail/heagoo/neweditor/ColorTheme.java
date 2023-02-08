package com.gmail.heagoo.neweditor;

import android.content.Context;

public class ColorTheme {
    private int backgroundColor;
    private int foregroundColors[] = new int[16];

    //
    // theme.1.background = #002b36
    //0 theme.1.normal = #fdf6e3
    //1 theme.1.reserved = #859900
    //2 theme.1.type = #b58900
    //3 theme.1.literal = #2aa198
    //4 theme.1.identifier = #93a1a1
    //5 theme.1.preprocessor = #cb4b16
    //6 theme.1.comment = #586e75
    //7 theme.1.error = #dc322f
    //8 theme.1.function = #268bd2
    //9 theme.1.operator = #93a1a1
    //
    public ColorTheme(Context ctx) {
        this.backgroundColor = 0xff002b36;

        foregroundColors[0] = 0xfffdf6e3;
        foregroundColors[1] = 0xff859900;
        foregroundColors[2] = 0xffb58900;
        foregroundColors[3] = 0xff2aa198;
        foregroundColors[4] = 0xff93a1a1;
        foregroundColors[5] = 0xffcb4b16;
        foregroundColors[6] = 0xff586e75;
        foregroundColors[7] = 0xffdc322f;
        foregroundColors[8] = 0xff268bd2;
        foregroundColors[9] = 0xff93a1a1;
    }

    public int getBackgroundColor() {
        return this.backgroundColor;
    }

    public int getForegroundColor(Token t) {
        switch (t.id) {
            case Token.COMMENT1:
            case Token.COMMENT2:
            case Token.COMMENT3:
            case Token.COMMENT4:
                return foregroundColors[6];
            case Token.DIGIT:
                return foregroundColors[2];
            case Token.FUNCTION:
                return foregroundColors[8];
            case Token.INVALID:
                return foregroundColors[7];
            case Token.KEYWORD1:
            case Token.KEYWORD2:
            case Token.KEYWORD3:
            case Token.KEYWORD4:
                return foregroundColors[1];
            case Token.LABEL:
                return foregroundColors[4];
            case Token.LITERAL1:
            case Token.LITERAL2:
            case Token.LITERAL3:
            case Token.LITERAL4:
                return foregroundColors[3];
            case Token.MARKUP:
                return foregroundColors[5];
            case Token.OPERATOR:
                return foregroundColors[9];
        }

        return 0xff000000;
    }

    public int getForeground() {
        return foregroundColors[0];
    }
}
