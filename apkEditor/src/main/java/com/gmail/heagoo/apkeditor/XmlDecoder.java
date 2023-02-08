package com.gmail.heagoo.apkeditor;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.decoder.AXmlResourceParser;
import brut.androlib.res.decoder.ResAttrDecoder;
import brut.androlib.res.decoder.ResStreamDecoder;
import brut.androlib.res.xml.ResXmlEncoders;
import brut.util.FastXmlSerializer;

/**
 * XML Parser
 *
 * @author phe3
 */
public class XmlDecoder implements ResStreamDecoder {

    private static final float RADIX_MULTS[] = {0.00390625F, 3.051758E-005F,
            1.192093E-007F, 4.656613E-010F};
    private static final String DIMENSION_UNITS[] = {"px", "dip", "sp", "pt",
            "in", "mm", "", ""};
    private static final String FRACTION_UNITS[] = {"%", "%p", "", "", "", "",
            "", ""};
    IReferenceDecoder refDecoder;

    // private ResAttrDecoder attrDecoder;
    private FastXmlSerializer ser;
    //private KXmlSerializer ser;
    private AXmlResourceParser par;
    // Is the apk protected or not
    private boolean apkProtected = false;

    public XmlDecoder(IReferenceDecoder refDecoder, ResPackage resPackage) {
        this.refDecoder = refDecoder;
        // Original is FastXmlSerializer
        ser = new FastXmlSerializer();

        ResAttrDecoder attrDecoder = new ResAttrDecoder();
        attrDecoder.setCurrentPackage(resPackage);

        par = new AXmlResourceParser();
        par.setAttrDecoder(attrDecoder);

        // this.attrDecoder = new ResAttrDecoder();
        // this.attrDecoder.setCurrentPackage(new ResPackage(resTable, 0,
        // null));
        // par.setAttrDecoder(attrDecoder);
    }

    // ////////////////////////////////////////////////////////////////////////
    private static String getPackage(int id) {
        if (id >>> 24 == 1) {
            return "android:";
        }
        return "";
    }

    public static float complexToFloat(int complex) {
        return (float) (complex & 0xFFFFFF00) * RADIX_MULTS[(complex >> 4) & 3];
    }

    public void decode(InputStream input, OutputStream output)
            throws AndrolibException {
        try {
            ser.setOutput(output, "utf-8");
            par.setInput(input, "utf-8");
        } catch (Exception e) {
            throw new AndrolibException(e.getMessage());
        }

        try {
            while (par.nextToken() != XmlPullParser.END_DOCUMENT) {
                int eventType = par.getEventType();
                this.event(par, eventType);
            }
            ser.endDocument();
        } catch (Exception e) {
            throw new AndrolibException(e.getMessage());
        }
    }

    // The difference with decode:
    // The attributes are all sorted
    // Return the package name
    public String decodeManifest(InputStream input, OutputStream output)
            throws AndrolibException {
        String pkgName = null;

        try {
            ser.setOutput(output, "utf-8");
            par.setInput(input, "utf-8");
        } catch (Exception e) {
            throw new AndrolibException(e.getMessage());
        }

        try {
            while (par.nextToken() != XmlPullParser.END_DOCUMENT) {
                int eventType = par.getEventType();

                // Special deal with manifest
                if (eventType == XmlPullParser.START_TAG) {
                    String curName = par.getName();
                    if ("manifest".equals(curName)) {
                        startTagEvent("package");
                    } else {
                        startTagEvent();
                    }
                } else {
                    this.event(par, eventType);
                }
            }
            ser.flush();
        } catch (Exception e) {
            throw new AndrolibException(e.getMessage());
        }

        return pkgName;
    }
    private boolean parseManifest(XmlPullParser pp) throws AndrolibException {
        String attr_name;

        // read <manifest> for package:
        for (int i = 0; i < pp.getAttributeCount(); i++) {
            attr_name = pp.getAttributeName(i);

            // if (attr_name.equalsIgnoreCase(("package"))) {
            // resTable.setPackageRenamed(pp.getAttributeValue(i));
            // } else if (attr_name.equalsIgnoreCase("versionCode")) {
            // resTable.addVersionInfo("versionCode", pp.getAttributeValue(i));
            // } else if (attr_name.equalsIgnoreCase("versionName")) {
            // resTable.addVersionInfo("versionName", pp.getAttributeValue(i));
            // }
        }
        return true;
    }

    // This method will sort the attributes for the tag
    private void startTagEvent() throws XmlPullParserException, IOException {
        ser.writeStartTag(par);
        int attrCount = par.getAttributeCount();
        if (attrCount >= 2) {
            String[] nss = new String[attrCount];
            String[] names = new String[attrCount];
            String[] values = new String[attrCount];
            for (int i = 0; i < attrCount; i++) {
                nss[i] = par.getAttributeNamespace(i);
                names[i] = par.getAttributeName(i);
                values[i] = par.getAttributeValue(i);
            }
            ser.attributesToSort(nss, names, values);
        }

        // 0 or 1 attribute
        else {
            for (int i = 0; i < attrCount; i++) {
                ser.attribute(par.getAttributeNamespace(i),
                        par.getAttributeName(i), par.getAttributeValue(i));
            }
        }
    }

    // Get the value of target attribute
    private String startTagEvent(String attrName)
            throws XmlPullParserException, IOException {
        String ret = null;

        ser.writeStartTag(par);
        int attrCount = par.getAttributeCount();
        if (attrCount >= 2) {
            String[] nss = new String[attrCount];
            String[] names = new String[attrCount];
            String[] values = new String[attrCount];
            for (int i = 0; i < attrCount; i++) {
                nss[i] = par.getAttributeNamespace(i);
                names[i] = par.getAttributeName(i);
                values[i] = par.getAttributeValue(i);
                if (attrName.equals(names[i])) {
                    ret = values[i];
                }
            }
            ser.attributesToSort(nss, names, values);
        }

        // 0 or 1 attribute
        else {
            for (int i = 0; i < attrCount; i++) {
                String name = par.getAttributeName(i);
                String value = par.getAttributeValue(i);
                if (attrName.equals(name)) {
                    ret = value;
                }
                ser.attribute(par.getAttributeNamespace(i), name, value);
            }
        }

        return ret;
    }

    public void event(AXmlResourceParser pp, int eventType)
            throws XmlPullParserException, IOException {

        switch (eventType) {
            case XmlPullParser.START_DOCUMENT:
                ser.startDocument(pp.getInputEncoding(), null);
                break;

            case XmlPullParser.END_DOCUMENT:
                ser.endDocument();
                break;

            case XmlPullParser.START_TAG:
                ser.writeStartTag(pp);

                for (int i = 0; i < pp.getAttributeCount(); i++) {
                    ser.attribute(pp.getAttributeNamespace(i),
                            pp.getAttributeName(i), pp.getAttributeValue(i));
                    // String attrName = pp.getAttributeName(i);
                    // String namespace = pp.getAttributeNamespace(i);
                    // String attrValue = getAttributeValue(pp, namespace, attrName,
                    // i);
                    // ser.attribute(namespace, attrName, attrValue);
                }
                break;

            case XmlPullParser.END_TAG:
                ser.endTag(pp.getNamespace(), pp.getName());
                break;

            case XmlPullParser.IGNORABLE_WHITESPACE:
                // comment it to remove ignorable whtespaces from XML infoset
                String s = pp.getText();
                ser.ignorableWhitespace(s);
                break;

            case XmlPullParser.TEXT:
                ser.text(ResXmlEncoders.encodeAsXmlValue(pp.getText()));
                // if (pp.getDepth() > 0) {
                // ser.text(pp.getText());
                // } else {
                // ser.ignorableWhitespace(pp.getText());
                // }
                break;

            case XmlPullParser.ENTITY_REF:
                ser.entityRef(pp.getName());
                break;

            case XmlPullParser.CDSECT:
                ser.cdsect(pp.getText());
                break;

            case XmlPullParser.PROCESSING_INSTRUCTION:
                ser.processingInstruction(pp.getText());
                break;

            case XmlPullParser.COMMENT:
                ser.comment(pp.getText());
                break;

            case XmlPullParser.DOCDECL:
                ser.docdecl(pp.getText());
                break;
        }
    }

    public void setApkProtected(boolean b) {
        this.apkProtected = b;
        par.setApkProtected(apkProtected);
    }


    public static interface IReferenceDecoder {
        public String getResReference(int id);

        public String getAttributeById(int id);
    }
}
