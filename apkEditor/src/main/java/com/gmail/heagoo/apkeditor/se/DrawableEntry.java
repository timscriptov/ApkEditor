package com.gmail.heagoo.apkeditor.se;

import java.util.ArrayList;
import java.util.List;

public class DrawableEntry {
    public String name;
    // Decoration is like "drawable-hdpi" "drawable-nodpi" "mipmap-xhdpi", etc
    public List<String> qualifierList = new ArrayList<>();
    // In most cases, it is NULL
    public String replaceFile;
    // Which one is the biggest picture
    public String bestQualifier;

    public DrawableEntry(String name, String qualifier) {
        this.name = name;
        this.qualifierList.add(qualifier);
        bestQualifier = qualifier;
    }

    public void addQualifier(String qualifier) {
        this.qualifierList.add(qualifier);
        if (getValue(qualifier) > getValue(bestQualifier)) {
            this.bestQualifier = qualifier;
        }
    }

    // ldpi Resources for low-density (ldpi) screens (~120dpi).
    // mdpi Resources for medium-density (mdpi) screens (~160dpi). (This is
    // the baseline density.)
    // tvdpi Resources for screens somewhere between mdpi and hdpi;
    // approximately 213dpi. This is not considered a "primary" density
    // group. It is mostly intended f
    // hdpi Resources for high-density (hdpi) screens (~240dpi).
    // xhdpi Resources for extra-high-density (xhdpi) screens (~320dpi).
    // xxhdpi Resources for extra-extra-high-density (xxhdpi) screens
    // (~480dpi).
    // xxxhdpi Resources for extra-extra-extra-high-density (xxxhdpi) uses
    // (~640dpi). Use this for the launcher icon only, see note above.
    // nodpi Resources for all densities. These are density-independent
    // resources. The system does not scale resources tagged with this
    // qualifier, regardless of the current screen's density.
    //
    private int getValue(String qualifier) {
        if (qualifier.endsWith("-hdpi")) {
            return 4;
        }
        if (qualifier.endsWith("-xhdpi")) {
            return 5;
        }
        if (qualifier.endsWith("-xxhdpi")) {
            return 6;
        }
        if (qualifier.endsWith("-mdpi")) {
            return 2;
        }
        if (qualifier.endsWith("-ldpi")) {
            return 1;
        }
        if (qualifier.endsWith("-xxxhdpi")) {
            return 7;
        }
        if (qualifier.endsWith("-tvdpi")) {
            return 3;
        } else { // default as medium
            return 2;
        }
    }

    // Generate all the entry path, separated by ";"
    public String getAllPaths() {
        StringBuffer sb = new StringBuffer();
        for (String q : qualifierList) {
            sb.append(q);
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
