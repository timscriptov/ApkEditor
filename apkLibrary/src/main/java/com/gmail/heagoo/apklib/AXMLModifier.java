package com.gmail.heagoo.apklib;

import android.annotation.SuppressLint;
import android.util.TypedValue;

import com.gmail.heagoo.apklib.android.content.res.AXmlResourceParser;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class AXMLModifier {

    private static final float RADIX_MULTS[] = {0.00390625F, 3.051758E-005F,
            1.192093E-007F, 4.656613E-010F};
    private static final String DIMENSION_UNITS[] = {"px", "dip", "sp", "pt",
            "in", "mm", "", ""};
    private static final String FRACTION_UNITS[] = {"%", "%p", "", "", "", "",
            "", ""};
    private InputStream input;
    private OutputStream output;
    private String inputFile;

    public AXMLModifier(String inputFile, String outputFile) throws IOException {
        this.inputFile = inputFile;
        this.input = new FileInputStream(inputFile);
        this.output = new FileOutputStream(outputFile);
    }

    public static InputStream getInputStream(String inputApkPath)
            throws IOException {
        JarFile inputJar = new JarFile(new File(inputApkPath), false); // Don't
        // verify.

        ZipEntry ze = inputJar.getEntry("AndroidManifest.xml");
        return inputJar.getInputStream(ze);
    }

    public static void main(String args[]) throws Exception {
        AXMLModifier modifier = new AXMLModifier(
                "D:\\Android\\apk\\AndroidManifest.xml.bak",
                "D:\\Android\\apk\\AndroidManifest.xml");

        List<String> permissionList = new ArrayList<String>();
        permissionList.add("android.permission.INTERNET");
        modifier.deletePermission(permissionList);
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

    private void checkInput(List<Section> sectionList) {
        // Sort first
        Collections.sort(sectionList, new Comparator() {
            @Override
            public int compare(Object lhs, Object rhs) {
                Section section1 = (Section) lhs;
                Section section2 = (Section) rhs;
                if (section1.startOffset < section2.startOffset) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });

        int previousEnd = -1;
        for (Section section : sectionList) {
            if (previousEnd > section.startOffset) {
                section.startOffset = previousEnd;
            }
            previousEnd = section.endOffset;
        }
    }

    public void deleteSections(List<Section> sectionList) throws IOException {
        checkInput(sectionList);

        int cutLength = 0;
        for (Section sec : sectionList) {
            int len = sec.endOffset - sec.startOffset;
            cutLength += len;
        }

        // System.out.println(cutLength);

        // Scan again
        FileInputStream fis = new FileInputStream(inputFile);

        // First 4 magic bytes
        byte[] head = new byte[4];
        fis.read(head);
        output.write(head);

        // File size
        fis.read(head);
        int fileSize = ((int) head[0] & 0xff) | (((int) head[1] & 0xff) << 8)
                | (((int) head[2] & 0xff) << 16)
                | (((int) head[3] & 0xff) << 24);
        //Log.d("AXML", "fileSize=" + fileSize);
        fileSize -= cutLength;

        head[0] = (byte) (fileSize & 0xff);
        head[1] = (byte) ((fileSize >> 8) & 0xff);
        head[2] = (byte) ((fileSize >> 16) & 0xff);
        head[3] = (byte) ((fileSize >> 24) & 0xff);
//		Log.d("AXML", "New size data: " + head[0] + " " + head[1] + " "
//				+ head[2] + " " + head[3]);
        output.write(head);

        int lastOffset = 8;
        int curBufferSize = 1024;
        byte[] buf = new byte[1024];
        for (int i = 0; i < sectionList.size(); i++) {
            Section sec = sectionList.get(i);
            int copySize = sec.startOffset - lastOffset;
            // Reallocate the buffer
            if (copySize > curBufferSize) {
                buf = new byte[copySize];
                curBufferSize = copySize;
            }
            int readLen = fis.read(buf, 0, copySize);
            output.write(buf, 0, readLen);

            fis.skip(sec.endOffset - sec.startOffset);
            lastOffset = sec.endOffset;
        }

        // Copy the remaining tail
        int readLen = fis.read(buf);
        while (readLen > 0) {
            output.write(buf, 0, readLen);
            readLen = fis.read(buf);
        }

        fis.close();

    }

    // ///////////////////////////////// ILLEGAL STUFF, DONT LOOK :)

    public void deletePermission(List<String> permissionList)
            throws IOException {
        List<Section> sectionList = getPermissionSections(permissionList);
        deleteSections(sectionList);
    }

    public List<Section> getSections(List<String> sectionTypeList) {

        List<Section> sections = new ArrayList<Section>();
        try {
            AXmlResourceParser parser = new AXmlResourceParser();
            parser.open(input);

            int startOffset = -1;
            int lastEndOffset = -1;
            boolean isTargetSection = false;

            String sectionType = null;
            String sectionValue = null;
            while (true) {
                int type = parser.next();
                if (type == XmlPullParser.END_DOCUMENT) {
                    break;
                }
                switch (type) {
                    case XmlPullParser.START_DOCUMENT: {
                        break;
                    }
                    case XmlPullParser.START_TAG: {
                        // Check if is the interested section
                        if (sectionTypeList.contains(parser.getName())) {
                            sectionType = parser.getName();
                            sectionValue = "";
                            for (int i = 0; i != parser.getAttributeCount(); ++i) {
                                if ("name".equals(parser.getAttributeName(i))) {
                                    sectionValue = getAttributeValue(parser, i);
                                    break;
                                }
                            }
                            startOffset = lastEndOffset;
                            isTargetSection = true;
                            //Log.d("Permission", "start tag detected!");
                        }
                        break;
                    }
                    case XmlPullParser.END_TAG: {
                        int curOffset = parser.getPosition();
                        //Log.d("Permission", "end tag detected! sectionType = " + sectionType + ", name = " + parser.getName());
                        if (isTargetSection && sectionType.equals(parser.getName())) {
                            Section section = new Section(startOffset, curOffset);
                            section.sectionType = sectionType;
                            section.sectionValue = sectionValue;
                            sections.add(section);
                            //Log.d("Permission", String.format("%s: %d - %d", sectionValue, startOffset, curOffset));
                            isTargetSection = false;
                        }

                        // Renew the value
                        lastEndOffset = curOffset;
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

        return sections;

    }

    // permissionList = null means to get all the permission sections
    public List<Section> getPermissionSections(List<String> permissionList) {
        List<Section> sections = new ArrayList<Section>();
        try {
            AXmlResourceParser parser = new AXmlResourceParser();
            parser.open(input);

            int lastEndOffset = -1;
            boolean isTargetSection = false;

            String sectionValue = null;
            while (true) {
                int type = parser.next();
                if (type == XmlPullParser.END_DOCUMENT) {
                    break;
                }
                switch (type) {
                    case XmlPullParser.START_DOCUMENT: {
                        break;
                    }
                    case XmlPullParser.START_TAG: {
                        // Find "uses-permission"
                        if ("uses-permission".equals(parser.getName())) {
                            sectionValue = "";
                            for (int i = 0; i != parser.getAttributeCount(); ++i) {
                                if ("name".equals(parser.getAttributeName(i))) {
                                    sectionValue = getAttributeValue(parser, i);
                                    break;
                                }
                            }
                            if (permissionList == null
                                    || permissionList.contains(sectionValue)) {
                                isTargetSection = true;
                            }
                            ;
                        }
                        break;
                    }
                    case XmlPullParser.END_TAG: {
                        int curOffset = parser.getPosition();
                        if (isTargetSection) {
                            Section section = new Section(lastEndOffset, curOffset);
                            section.sectionValue = sectionValue;
                            //Log.d("Permission", String.format("%s: %d - %d", sectionValue, lastEndOffset, curOffset));
                            sections.add(section);
                        }

                        // Renew the value
                        isTargetSection = false;
                        lastEndOffset = curOffset;
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

        return sections;
    }

    @SuppressLint("DefaultLocale")
    public static class Section {
        public int startOffset;
        public int endOffset;
        public String sectionType;
        public String sectionValue;

        public int startLineIndex;
        public int endLineIndex;

        public Section(int start, int end) {
            this.startOffset = start;
            this.endOffset = end;
        }

        public static Section fromString(String str) {
            String[] words = str.split(",");
            int startOffset = Integer.valueOf(words[0]);
            int endOffset = Integer.valueOf(words[1]);
            return new Section(startOffset, endOffset);
        }

        public String toString() {
            return String.format("%d,%d", startOffset, endOffset);
        }
    }
}