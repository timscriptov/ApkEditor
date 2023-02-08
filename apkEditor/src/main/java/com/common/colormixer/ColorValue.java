package com.common.colormixer;

import android.annotation.SuppressLint;
import android.content.Context;

import com.gmail.heagoo.common.RefInvoke;

import java.util.List;

public class ColorValue {

    public String name;
    public String strColorValue;
    public int intColorValue;

    // intValue successfully parsed from strValue, or not
    public boolean parsed;

    public ColorValue(String _name, String _val) {
        this.name = _name;
        this.strColorValue = _val;

        parseColorFrom(_val);
    }

    // str = "#ffffffff"
    // str = "@color/xyz"
    // str = "@android:color/xyz"
    private void parseColorFrom(String str) {
        if (str.startsWith("#")) {
            try {
                this.intColorValue = (int) Long.parseLong(str.substring(1), 16);
                this.parsed = true;
            } catch (Exception e) {
            }
        }
    }

    // Parse the reference color
    @SuppressLint("NewApi")
    public void parseRefColor(Context ctx, List<ColorValue> values) {
        if (!parsed) {
            if (strColorValue.startsWith("@color/")) {
                String refStr = strColorValue.substring(7);
                for (int i = 0; i < values.size(); ++i) {
                    ColorValue v = values.get(i);
                    if (v.parsed && refStr.equals(v.name)) {
                        this.intColorValue = v.intColorValue;
                        this.parsed = true;
                        break;
                    }
                }
            } else if (strColorValue.startsWith("@android:color/")) {
                try {
                    String refStr = strColorValue.substring(15);
                    Object obj = RefInvoke
                            .getStaticFieldOjbect("android.R$color", refStr);
                    if (obj != null) {
                        this.intColorValue = ctx.getColor((Integer) obj);
                        this.parsed = true;
                    }
                } catch (Throwable e) {
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("    <color name=\"");
        sb.append(name);
        sb.append("\">");
        sb.append(strColorValue);
        sb.append("</color>");
        return sb.toString();
    }
}
