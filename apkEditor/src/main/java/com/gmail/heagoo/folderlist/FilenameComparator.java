package com.gmail.heagoo.folderlist;

import java.util.Comparator;

@SuppressWarnings("rawtypes")
public class FilenameComparator implements Comparator {

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
            } else if (isLetter(c1) && isLetter(c2)) {
                char lower1 = Character.toLowerCase(c1);
                char lower2 = Character.toLowerCase(c2);
                if (lower1 == lower2) {
                    return c1 < c2 ? -1 : 1;
                } else {
                    return lower1 < lower2 ? -1 : 1;
                }
            } else {
                if (c1 < c2) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }
    }

    private static boolean isLetter(char c) {
        if (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z')) {
            return true;
        }
        return false;
    }

    @Override
    public int compare(Object obj1, Object obj2) {
        FileRecord rec1 = (FileRecord) obj1;
        FileRecord rec2 = (FileRecord) obj2;
        if (rec1.isDir) {
            if (rec2.isDir) {
                //return rec1.fileName.compareToIgnoreCase(rec2.fileName);
                return myCompare(rec1.fileName, rec2.fileName);
            } else {
                return -1;
            }
        } else {
            if (rec2.isDir) {
                return 1;
            } else {
                //return rec1.fileName.compareToIgnoreCase(rec2.fileName);
                return myCompare(rec1.fileName, rec2.fileName);
            }
        }
    }
}
