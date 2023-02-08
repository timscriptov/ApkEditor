package com.gmail.heagoo.common;

import java.util.Random;

public class RandomUtil {

    private static final char[] letters = new char[]{'a', 'b', 'c', 'd', 'e',
            'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
            's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    private static Random r;

    public static String getRandomString(int bits) {
        if (r == null) {
            r = new Random(System.currentTimeMillis());
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bits; i++) {
            int index = r.nextInt(letters.length);
            sb.append(letters[index]);
        }
        return sb.toString();
    }
}
