/*
 * Copyright 2008 Android4ME
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gmail.heagoo.apklib;

import android.util.TypedValue;

import com.gmail.heagoo.apklib.AXMLModifier.Section;
import com.gmail.heagoo.apklib.android.content.res.AXmlResourceParser;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * @author Dmitry Skiba
 * <p>
 * This is example usage of AXMLParser class.
 * <p>
 * Prints xml document from Android's binary xml file.
 */
public class AXMLParser {

    private static final float RADIX_MULTS[] = {0.00390625F, 3.051758E-005F,
            1.192093E-007F, 4.656613E-010F};
    private static final String DIMENSION_UNITS[] = {"px", "dip", "sp", "pt",
            "in", "mm", "", ""};
    private static final String FRACTION_UNITS[] = {"%", "%p", "", "", "", "",
            "", ""};
    private static Map<String, List<AttrReplacement>> replaces = new HashMap<String, List<AttrReplacement>>();

    static {
        List<AttrReplacement> list = new ArrayList<AttrReplacement>();
        list.add(new AttrReplacement(-1, "fill_parent"));
        list.add(new AttrReplacement(-2, "wrap_content"));
        replaces.put("layout_width", list);
        replaces.put("layout_height", list);

        list = new ArrayList<AttrReplacement>();
        list.add(new AttrReplacement(0, "horizontal"));
        list.add(new AttrReplacement(1, "vertical"));
        replaces.put("orientation", list);

        list = new ArrayList<AttrReplacement>();
        list.add(new AttrReplacement(0, "visible"));
        list.add(new AttrReplacement(1, "focus_backward"));
        list.add(new AttrReplacement(2, "gone"));
        list.add(new AttrReplacement(4, "invisible"));
        list.add(new AttrReplacement(8, "gone"));
        list.add(new AttrReplacement(17, "focus_left"));
        list.add(new AttrReplacement(33, "focus_up"));
        list.add(new AttrReplacement(66, "focus_right"));
        list.add(new AttrReplacement(130, "focus_down"));
        replaces.put("visibility", list);


        list = new ArrayList<AttrReplacement>();
        list.add(new AttrReplacement(0, "no_gravity"));
        list.add(new AttrReplacement(48, "top"));
        list.add(new AttrReplacement(80, "bottom"));
        list.add(new AttrReplacement(3, "left"));
        list.add(new AttrReplacement(5, "right"));
        list.add(new AttrReplacement(16, "center_vertical"));
        list.add(new AttrReplacement(112, "fill_vertical"));
        list.add(new AttrReplacement(1, "center_horizontal"));
        list.add(new AttrReplacement(7, "fill_horizontal"));
        list.add(new AttrReplacement(17, "center"));
        list.add(new AttrReplacement(119, "fill"));
        list.add(new AttrReplacement(128, "clip_vertical"));
        list.add(new AttrReplacement(8, "clip_horizontal"));
        replaces.put("gravity", list);
        replaces.put("layout_gravity", list);

        list = new ArrayList<AttrReplacement>();
        list.add(new AttrReplacement(0, "start"));
        list.add(new AttrReplacement(1, "middle"));
        list.add(new AttrReplacement(2, "end"));
        list.add(new AttrReplacement(3, "marquee"));
        replaces.put("ellipsize", list);

        list = new ArrayList<AttrReplacement>();
        list.add(new AttrReplacement(0, "normal"));
        list.add(new AttrReplacement(1, "bold"));
        list.add(new AttrReplacement(2, "italic"));
        list.add(new AttrReplacement(3, "bold_italic"));
        replaces.put("textStyle", list);

        list = new ArrayList<AttrReplacement>();
        list.add(new AttrReplacement(0, "auto"));
        replaces.put("installLocation", list);
    }

    private int lastType = 0;
    private String unfinishedLine;
    // Resource reference decoder
    private IReferenceDecode refDecoder;
    // Current line index
    private int curLineIndex = 0;
    // Last tag offset (offset in the file) (include start and end tag)
    private int lastTagOffset = -1;
    private Stack<Section> stack = new Stack<Section>();
    private List<Section> allSections = new ArrayList<Section>();

    public AXMLParser(IReferenceDecode refDecoder) {
        this.refDecoder = refDecoder;
    }

    private static String getNamespacePrefix(String prefix) {
        if (prefix == null || prefix.length() == 0) {
            return "";
        }
        return prefix + ":";
    }

    private static String getPackage(int id) {
        if (id >>> 24 == 1) {
            return "android:";
        }
        return "";
    }

    public static float complexToFloat(int complex) {
        return (float) (complex & 0xFFFFFF00) * RADIX_MULTS[(complex >> 4) & 3];
    }

    public List<Section> getAllSections() {
        return allSections;
    }

    // in is the binary input stream
    public void parse(InputStream in, OutputStream out) {
        try {
            AXmlResourceParser parser = new AXmlResourceParser();
            parser.open(in);
            StringBuilder indent = new StringBuilder(10);
            final String indentStep = "	";
            while (true) {
                int type = parser.next();
                if (type == XmlPullParser.END_DOCUMENT) {
                    break;
                }
                switch (type) {
                    case XmlPullParser.START_DOCUMENT: {
                        log(out, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
                        break;
                    }
                    case XmlPullParser.START_TAG: {
                        switch (lastType) {
                            case XmlPullParser.START_TAG:
                                log(out, ">\n");
                                break;
                            case XmlPullParser.END_TAG:
                            case XmlPullParser.TEXT:
                                break;
                        }

                        // Initialize section record
                        Section section = new Section(lastTagOffset, -1);
                        section.startLineIndex = this.curLineIndex;
                        section.endLineIndex = -1;
                        String sectionTag = parser.getName();
                        section.sectionType = sectionTag;

                        log(out, "%s<%s%s", indent,
                                getNamespacePrefix(parser.getPrefix()),
                                sectionTag);
                        indent.append(indentStep);

                        int namespaceCountBefore = parser.getNamespaceCount(parser
                                .getDepth() - 1);
                        int namespaceCount = parser.getNamespaceCount(parser
                                .getDepth());
                        for (int i = namespaceCountBefore; i != namespaceCount; ++i) {
                            log(out, " xmlns:%s=\"%s\"",
                                    parser.getNamespacePrefix(i),
                                    parser.getNamespaceUri(i));
                        }

                        int attrCount = parser.getAttributeCount();
                        for (int i = 0; i != attrCount; ++i) {
                            String attributeName = parser.getAttributeName(i);
                            String attrValue = getAttributeValue(parser, attributeName, i);
                            if ("name".equals(attributeName)) {
                                section.sectionValue = attrValue;
                            }
                            // Ugly fix for special case
                            else if ("".equals(attributeName) && attrCount == 1) {
                                attributeName = "android:name";
                                section.sectionValue = attrValue;
                            }
                            log(out,
                                    " %s%s=\"%s\"",
                                    getNamespacePrefix(parser.getAttributePrefix(i)),
                                    attributeName, attrValue
                            );
                        }

                        // Push section record to stack
                        this.stack.push(section);

                        lastTagOffset = parser.getPosition();

                        break;
                    }
                    case XmlPullParser.END_TAG: {
                        int curOffset = parser.getPosition();

                        // Update the section
                        Section section = stack.pop();
                        section.endLineIndex = curLineIndex;
                        section.endOffset = curOffset;
                        allSections.add(section);

                        indent.setLength(indent.length() - indentStep.length());
                        switch (lastType) {
                            case XmlPullParser.START_TAG:
                                log(out, " />\n");
                                break;
                            case XmlPullParser.END_TAG:
                                log(out, "%s</%s%s>\n", indent,
                                        getNamespacePrefix(parser.getPrefix()),
                                        parser.getName());
                                break;
                            case XmlPullParser.TEXT:
                                log(out, "</%s%s>\n",
                                        getNamespacePrefix(parser.getPrefix()),
                                        parser.getName());
                                break;
                        }

                        // Renew the value
                        this.lastTagOffset = curOffset;

                        break;
                    }
                    case XmlPullParser.TEXT: {
                        switch (lastType) {
                            case XmlPullParser.START_TAG:
                                log(out, ">");
                                break;
                            case XmlPullParser.END_TAG:
                            case XmlPullParser.TEXT:
                                break;
                        }
                        log(out, "%s", parser.getText());
                        break;
                    }
                }

                // Update lastType
                lastType = type;
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // private static void log(String format,Object...arguments) {
    // System.out.printf(format,arguments);
    // System.out.println();
    // }

    private String getAttributeValue(AXmlResourceParser parser,
                                     String attrName, int index) {
        int type = parser.getAttributeValueType(index);
        int data = parser.getAttributeValueData(index);
        //Log.d("APKEDITOR", "type=" + type + ", data=" + data);
        if (type == TypedValue.TYPE_STRING) {
            return parser.getAttributeValue(index);
        }
        if (type == TypedValue.TYPE_ATTRIBUTE) {
            return String.format("?%s%08X", getPackage(data), data);
        }
        if (type == TypedValue.TYPE_REFERENCE) {
            return refDecoder.getResReference(data);
            // return String.format("@%s%08X", getPackage(data), data);
        }
        if (type == TypedValue.TYPE_FLOAT) {
            return String.valueOf(Float.intBitsToFloat(data));
        }
        if (type == TypedValue.TYPE_INT_HEX) {
            String str = getReplacedString(attrName, data);
            return str == null ? String.format("0x%08X", data) : str;
        }
        if (type == TypedValue.TYPE_INT_BOOLEAN) {
            return data != 0 ? "true" : "false";
        }
        if (type == TypedValue.TYPE_DIMENSION) {
            return Float.toString(complexToFloat(data))
                    + DIMENSION_UNITS[data & TypedValue.COMPLEX_UNIT_MASK];
        }
        if (type == TypedValue.TYPE_FRACTION) {
            return Float.toString(complexToFloat(data))
                    + FRACTION_UNITS[data & TypedValue.COMPLEX_UNIT_MASK];
        }
        if (type >= TypedValue.TYPE_FIRST_COLOR_INT
                && type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return String.format("#%08X", data);
        }
        if (type >= TypedValue.TYPE_FIRST_INT
                && type <= TypedValue.TYPE_LAST_INT) {
            String str = getReplacedString(attrName, data);
            return str == null ? String.valueOf(data) : str;
        }
        return String.format("<0x%X, type 0x%02X>", data, type);
    }

    // ///////////////////////////////// ILLEGAL STUFF, DONT LOOK :)

    private String getReplacedString(String attrName, int data) {
        List<AttrReplacement> replaceList = replaces.get(attrName);
        if (replaceList != null) {
            for (AttrReplacement r : replaceList) {
                if (data == r.intVal) {
                    return r.strVal;
                }
            }
        }
        return null;
    }

    private void log(OutputStream out, String format, Object... arguments) {
        try {
            String str = String.format(format, arguments);
            out.write(str.getBytes());

            // Detect line complete
            if (str.endsWith("\n")) {
                String line = str.substring(0, str.length() - 1);
                if (unfinishedLine != null) {
                    line = unfinishedLine + line;
                    unfinishedLine = null;
                }
                curLineIndex++;
                //Log.d("APKEDITOR", line);
            }
            // The line is not complete
            else {
                if (unfinishedLine != null) {
                    unfinishedLine += str;
                } else {
                    unfinishedLine = str;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static interface IReferenceDecode {
        public String getResReference(int data);
    }

    private static class AttrReplacement {
        public int intVal;
        public String strVal;

        public AttrReplacement(int intVal, String strVal) {
            this.intVal = intVal;
            this.strVal = strVal;
        }
    }
}