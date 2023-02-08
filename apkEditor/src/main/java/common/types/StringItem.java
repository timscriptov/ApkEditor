package common.types;

import java.io.Serializable;

import brut.androlib.res.xml.ResXmlEncoders;


public class StringItem implements Serializable {
    private static final long serialVersionUID = -3234844926022744481L;

    public String name;

    // Value in string block?
    public String value;

    // When it is null, means not styled
    // otherwise contains format like <b> <font color=red>, etc
    public String styledValue;

    public StringItem(String name, String value) {
        this(name, value, null);
    }


    public StringItem(String name, String value, String styledValue) {
        this.name = name;
        this.value = value;
        this.styledValue = styledValue;
    }

    public static String toString(String name, String value, String styledValue) {
        // Should add formatted="false" or not
        boolean bAddFormat = false;
        if (ResXmlEncoders.hasMultipleNonPositionalSubstitutions(value)) {
            bAddFormat = true;
        }

        String txt;
        // Special case: reference, no encode needed
        if (value.startsWith("@string/")
                || value.startsWith("@android:string/")) {
            txt = value;
        } else {
            if (styledValue == null) {
                String escaped = ResXmlEncoders.escapeXmlChars(value);
                txt = ResXmlEncoders.encodeAsXmlValue(escaped);
            } else {
                txt = ResXmlEncoders.encodeAsXmlValue(styledValue);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<string name=\"");
        sb.append(name);
        if (bAddFormat) {
            sb.append(" formatted=\"false\"");
        }
        sb.append("\">");
        sb.append(txt);
        sb.append("</string>");

        return sb.toString();
    }

    @Override
    public String toString() {
        return toString(name, value, styledValue);
    }
}
