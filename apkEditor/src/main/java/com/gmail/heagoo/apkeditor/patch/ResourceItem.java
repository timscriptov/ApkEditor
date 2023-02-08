package com.gmail.heagoo.apkeditor.patch;


public class ResourceItem {
    public String type;
    public String name;
    public int id;

    public ResourceItem(String t, String n, int _id) {
        this.type = t;
        this.name = n;
        this.id = _id;
    }

    public static ResourceItem parseFrom(String line) {
        String type = null;
        String name = null;
        int id = -1;
        int startPos = -1, endPos = -1;
        do {
            // type
            startPos = line.indexOf("type=\"");
            if (startPos == -1)
                break;
            startPos += 6;
            endPos = line.indexOf("\" ", startPos);
            if (endPos == -1)
                break;
            type = line.substring(startPos, endPos);

            // name
            startPos = line.indexOf("name=\"");
            if (startPos == -1)
                break;
            startPos += 6;
            endPos = line.indexOf("\" ", startPos);
            if (endPos == -1)
                break;
            name = line.substring(startPos, endPos);

            // id
            startPos = line.indexOf("id=\"");
            if (startPos == -1)
                break;
            startPos += 4;
            endPos = line.indexOf("\" ", startPos);
            if (endPos == -1)
                break;
            String strId = line.substring(startPos, endPos);
            id = string2Id(strId);
        } while (false);

        if (type != null && name != null && id != -1) {
            return new ResourceItem(type, name, id);
        }

        return null;
    }

    // convert string like 0x7f020007 to int
    public static int string2Id(String str) {
        int value = 0;
        if (str.length() == 10) {
            for (int i = 2; i < 10; i++) {
                value = (value << 4) | getVal(str.charAt(i));
            }
        }
        return value;
    }

    public static String id2String(int id) {
        return "0x" + Integer.toHexString(id);
    }

    private static int getVal(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        } else if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return 0;
    }

    @Override
    public String toString() {
        return String.format("<public type=\"%s\" name=\"%s\" id=\"0x%s\" />",
                type, name, Integer.toHexString(id));
    }
}