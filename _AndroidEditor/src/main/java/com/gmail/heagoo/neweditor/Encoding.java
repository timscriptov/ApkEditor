package com.gmail.heagoo.neweditor;

import android.content.Context;
import android.preference.PreferenceManager;

import java.io.Serializable;

public class Encoding implements CharSequence, Serializable {
    public static final String DEFAULT_CHARSET_NAME = "UTF-8";
    public static final String UTF_16 = "UTF-16";
    private static final long serialVersionUID = -1382788588721415465L;
    public static Encoding[] encodings = new Encoding[]{
            new Encoding("UTF8", DEFAULT_CHARSET_NAME),
            new Encoding("UTF8", DEFAULT_CHARSET_NAME, BOM.UTF_8),
            new Encoding("ISO8859_1", "Western (ISO-8859-1)"),
            new Encoding("Cp1252", "Western (Windows-1252)"),
            new Encoding("ISO8859_2", "Eastern (ISO-8859-2)"),
            new Encoding("Cp1250", "Eastern (Windows-1250)"),
            new Encoding("ISO8859_5", "Cyrillic (ISO-8859-5)"),
            new Encoding("Cp1251", "Cyrillic (Windows-1251)"),
            new Encoding("ISO8859_7", "Greek (ISO-8859-7)"),
            new Encoding("Cp1253", "Greek (Windows-1253)"),
            new Encoding("Big5", "Traditional Chinese (Big5)"),
            new Encoding("EUC-KR", "Korean (EUC-KR)"),
            new Encoding("MS874", "Thai (Windows-874)"),
            new Encoding("Shift_JIS", "Japanese (Shift JIS)"),
            new Encoding("EUC-JP", "Japanese (EUC-JP)"),
            new Encoding("Unicode", UTF_16),
            new Encoding("ASCII", "ASCII")};
    private BOM bom;
    private String humanName;
    private String javaName;

    public Encoding(String javaName, String humanName) {
        this.javaName = javaName;
        this.humanName = humanName;
        this.bom = BOM.NONE;
    }

    public Encoding(String javaName, String humanName, BOM bom) {
        this.javaName = javaName;
        this.humanName = humanName;
        this.bom = bom;
    }

    public Encoding(String javaName, String humanName, String bom) {
        this.javaName = javaName;
        this.humanName = humanName;
        if (bom == null) {
            this.bom = BOM.NONE;
        } else if (bom.equals("UTF_16_BE")) {
            this.bom = BOM.UTF_16_BE;
        } else if (bom.equals("UTF_16_LE")) {
            this.bom = BOM.UTF_16_LE;
        } else if (bom.equals("UTF_32_LE")) {
            this.bom = BOM.UTF_32_BE;
        } else if (bom.equals("UTF_32_LE")) {
            this.bom = BOM.UTF_32_LE;
        } else if (bom.equals("UTF_8")) {
            this.bom = BOM.UTF_8;
        } else {
            this.bom = BOM.NONE;
        }
    }

    public static Encoding getDefaultEncoding(Context context) {
        String defaultEncoding = PreferenceManager.getDefaultSharedPreferences(
                context).getString("defaultEncoding", DEFAULT_CHARSET_NAME);
        for (Encoding encoding : encodings) {
            if (encoding.getHumanName().equals(defaultEncoding)) {
                return encoding;
            }
        }
        return encodings[0];
    }

    public static CharSequence[] getEncodingHumanNames() {
        String[] names = new String[encodings.length];
        Encoding[] encodingArr = encodings;
        int length = encodingArr.length;
        int i = 0;
        int i2 = 0;
        while (i < length) {
            int i3 = i2 + 1;
            names[i2] = encodingArr[i].getHumanName();
            i++;
            i2 = i3;
        }
        return names;
    }

    public String getJavaName() {
        return this.javaName;
    }

    public void setJavaName(String javaName) {
        this.javaName = javaName;
    }

    public String getHumanName() {
        if (this.bom != BOM.NONE) {
            return this.humanName + " (BOM)";
        }
        return this.humanName;
    }

    public void setHumanName(String humanName) {
        this.humanName = humanName;
    }

    public String toString() {
        return getHumanName();
    }

    public char charAt(int arg0) {
        return this.humanName.charAt(arg0);
    }

    public int length() {
        return this.humanName.length();
    }

    public CharSequence subSequence(int arg0, int arg1) {
        return this.humanName.subSequence(arg0, arg1);
    }

    public void changeTo(int id) {
        this.humanName = encodings[id].humanName;
        this.javaName = encodings[id].javaName;
        this.bom = encodings[id].bom;
    }

    public Encoding clone() {
        return new Encoding(this.javaName, this.humanName, this.bom);
    }

    public BOM getBom() {
        return this.bom;
    }

    public void setBom(BOM bom) {
        this.bom = bom;
    }

    public enum BOM {
        NONE, UTF_32_BE, UTF_32_LE, UTF_16_BE, UTF_16_LE, UTF_8
    }
}