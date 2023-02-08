package com.gmail.heagoo.common;

import android.text.InputFilter;
import android.text.Spanned;

/**
 * Created by phe3 on 3/30/2017.
 */

public class InputUtil {
    public static InputFilter getFileNameFilter() {
        return new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    char c = source.charAt(i);
                    switch (c) {
                        case '/':
                        case '\\':
                        case '\"':
                        case ':':
                        case '*':
                        case '?':
                        case '<':
                        case '>':
                        case '|':
                            return "";
                        default:
                            break;
                    }
                }
                return null;
            }
        };
    }
}
