package com.gmail.heagoo.httpserver;

import java.io.File;
import java.util.Comparator;


public class FileComparator implements Comparator {

    private static int myCompare(String str1, String str2) {
        if (str1.isEmpty()) {
            if (str2.isEmpty()) {
                return 0;
            } else {
                return -1;
            }
        } else if (str2.isEmpty()) {
            return 1;
        } else {
            char c1 = str1.charAt(0);
            char c2 = str2.charAt(0);
            if (c1 == c2) {
                return myCompare(str1.substring(1), str2.substring(1));
            } else if (Character.isLetter(c1) && Character.isLetter(c2)) {
                char lower1 = Character.toLowerCase(c1);
                char lower2 = Character.toLowerCase(c2);
                if (lower1 == lower2) {
                    return c1 < c2 ? -1 : 1;
                } else {
                    return lower1 < lower2 ? -1 : 1;
                }
            } else {
                if (c1 == c2) {
                    return myCompare(str1.substring(1), str2.substring(1));
                } else if (c1 < c2) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }
    }

    @Override
    public int compare(Object obj1, Object obj2) {
        File rec1 = (File) obj1;
        File rec2 = (File) obj2;
        if (rec1.isDirectory()) {
            if (rec2.isDirectory()) {
                //return rec1.fileName.compareToIgnoreCase(rec2.fileName);
                return myCompare(rec1.getName(), rec2.getName());
            } else {
                return -1;
            }
        } else {
            if (rec2.isDirectory()) {
                return 1;
            } else {
                //return rec1.fileName.compareToIgnoreCase(rec2.fileName);
                return myCompare(rec1.getName(), rec2.getName());
            }
        }
    }
}