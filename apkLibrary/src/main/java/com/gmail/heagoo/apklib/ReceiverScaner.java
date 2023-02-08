package com.gmail.heagoo.apklib;

import android.util.TypedValue;

import com.gmail.heagoo.apklib.android.content.res.AXmlResourceParser;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class ReceiverScaner {

    private static final float RADIX_MULTS[] = {0.00390625F, 3.051758E-005F,
            1.192093E-007F, 4.656613E-010F};
    private static final String DIMENSION_UNITS[] = {"px", "dip", "sp", "pt",
            "in", "mm", "", ""};
    private static final String FRACTION_UNITS[] = {"%", "%p", "", "", "", "",
            "", ""};
    private InputStream input;
    private LinkedHashMap<String, List<ActionSection>> receiver2Actions = new LinkedHashMap<String, List<ActionSection>>();

    public ReceiverScaner(InputStream input) {
        this.input = input;
    }

    public static InputStream getInputStream(String inputApkPath)
            throws IOException {
        JarFile inputJar = new JarFile(new File(inputApkPath), false); // Don't
        // verify.

        ZipEntry ze = inputJar.getEntry("AndroidManifest.xml");
        return inputJar.getInputStream(ze);
    }

    public static void main(String args[]) throws IOException {
        ReceiverScaner rs = new ReceiverScaner(
                getInputStream("D:\\Android\\apk\\com.tencent.mm_355.apk"));
        rs.scanReceiver();
    }

    private static String getAttributeValue(AXmlResourceParser parser, int index) {
        int type = parser.getAttributeValueType(index);
        int data = parser.getAttributeValueData(index);
        if (type == TypedValue.TYPE_STRING) {
            return parser.getAttributeValue(index);
        }
        if (type == TypedValue.TYPE_ATTRIBUTE) {
            return String.format("?%s%08X", getPackage(data), data);
        }
        if (type == TypedValue.TYPE_REFERENCE) {
            return String.format("@%s%08X", getPackage(data), data);
        }
        if (type == TypedValue.TYPE_FLOAT) {
            return String.valueOf(Float.intBitsToFloat(data));
        }
        if (type == TypedValue.TYPE_INT_HEX) {
            return String.format("0x%08X", data);
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
            return String.valueOf(data);
        }
        return String.format("<0x%X, type 0x%02X>", data, type);
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

    public void scanReceiver() {

        try {
            AXmlResourceParser parser = new AXmlResourceParser();
            parser.open(input);

            boolean inReceiverSection = false;
            String receiverName = null;
            String actionName = null;
            int lastOffset = -1;
            int actionStartOffset = -1;

            while (true) {
                lastOffset = parser.getPosition();
                int type = parser.next();
                if (type == XmlPullParser.END_DOCUMENT) {
                    break;
                }
                switch (type) {
                    case XmlPullParser.START_DOCUMENT: {
                        break;
                    }
                    case XmlPullParser.START_TAG: {
                        // Find "receiver"
                        if ("receiver".equals(parser.getName())) {
                            inReceiverSection = true;
                            for (int i = 0; i != parser.getAttributeCount(); ++i) {
                                if ("name".equals(parser.getAttributeName(i))) {
                                    receiverName = getAttributeValue(parser, i);
                                    break;
                                }
                            }
                        } else if (inReceiverSection
                                && "action".equals(parser.getName())) {
                            for (int i = 0; i != parser.getAttributeCount(); ++i) {
                                if ("name".equals(parser.getAttributeName(i))) {
                                    actionStartOffset = lastOffset;
                                    actionName = getAttributeValue(parser, i);
                                    break;
                                }
                            }
                        }
                        break;
                    }
                    case XmlPullParser.END_TAG: {
                        if ("receiver".equals(parser.getName())) {
                            inReceiverSection = false;
                            receiverName = null;
                        } else if (inReceiverSection
                                && "action".equals(parser.getName())) {
                            int actionEndOffset = parser.getPosition();
                            ActionSection as = new ActionSection(actionName,
                                    actionStartOffset, actionEndOffset);
                            if (receiverName != null) {
                                List<ActionSection> asList = receiver2Actions
                                        .get(receiverName);
                                if (asList == null) {
                                    asList = new ArrayList<ActionSection>();
                                    receiver2Actions.put(receiverName, asList);
                                }
                                asList.add(as);
                            }
                        }
                        break;
                    }
                    case XmlPullParser.TEXT: {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //dumpResult();
    }

    public LinkedHashMap<String, List<ActionSection>> getReceivers() {
        return receiver2Actions;
    }

    @SuppressWarnings("unused")
    private void dumpResult() {
        for (Entry<String, List<ActionSection>> entry : receiver2Actions
                .entrySet()) {
            System.out.println(entry.getKey());
            for (ActionSection as : entry.getValue()) {
                System.out.println("\t" + as.actionName);
            }
        }
    }

    public static class ActionSection {
        public int startOffset;
        public int endOffset;
        public String actionName;

        public ActionSection(String name, int start, int end) {
            this.actionName = name;
            this.startOffset = start;
            this.endOffset = end;
        }
    }
}
