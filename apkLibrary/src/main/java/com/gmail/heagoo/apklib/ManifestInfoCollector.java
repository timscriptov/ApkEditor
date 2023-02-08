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

import com.gmail.heagoo.apklib.android.content.res.AXmlResourceParser;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;

public class ManifestInfoCollector {

    private String value = null;

    private static String getAttributeValue(AXmlResourceParser parser,
                                            int index) {
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
            return "";
        }
        if (type == TypedValue.TYPE_FRACTION) {
            return "";
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

    public void parse(InputStream input, String tagName, String attrName) {
        AXmlResourceParser parser = null;
        try {
            parser = new AXmlResourceParser();
            parser.open(input);
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
                        if (tagName.equals(parser.getName())) {
                            for (int i = 0; i != parser.getAttributeCount(); ++i) {
                                String name = parser.getAttributeName(i);
                                String value = getAttributeValue(parser, i);
                                if (attrName.equals(name)) {
                                    this.value = value;
                                    return;
                                }
                            }
                        }

                        break;
                    }
                    case XmlPullParser.END_TAG: {
                        break;
                    }
                    case XmlPullParser.TEXT: {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    public String getValue() {
        return value;
    }
}