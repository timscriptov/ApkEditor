/**
 * Copyright 2014 Ryszard Wiśniewski <brut.alll@gmail.com>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package brut.androlib.res.data.value;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.xml.ResValuesXmlSerializable;
import brut.androlib.res.xml.ResXmlEncodable;
import brut.androlib.res.xml.ResXmlEncoders;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public abstract class ResScalarValue extends ResIntBasedValue implements
        ResXmlEncodable, ResValuesXmlSerializable {
    protected final String mType;
    protected String mRawValue;
    // Added by Pujiang
    protected String mValueRawRaw; // value in string block
    protected boolean mStyled = false;

    protected ResScalarValue(String type, int rawIntValue, String rawValue) {
        super(rawIntValue);
        mType = type;
        mRawValue = rawValue;
    }

    protected ResScalarValue(String type, int rawIntValue, String rawValue,
                             String rawStr, boolean styled) {
        super(rawIntValue);
        mType = type;
        mRawValue = rawValue;
        mValueRawRaw = rawStr;
        mStyled = styled;
    }

    @Override
    public String encodeAsResXmlAttr() throws AndrolibException {
        if (mRawValue != null) {
            return mRawValue;
        }
        return encodeAsResXml();
    }

    public String encodeAsResXmlItemValue() throws AndrolibException {
        return encodeAsResXmlValue();
    }

    @Override
    public String encodeAsResXmlValue() throws AndrolibException {
        if (mRawValue != null) {
            return mRawValue;
        }
        return encodeAsResXml();
    }

    public String encodeAsResXmlNonEscapedItemValue() throws AndrolibException {
        return encodeAsResXmlValue().replace("&amp;", "&").replace("&lt;", "<");
    }

    public boolean hasMultipleNonPositionalSubstitutions() throws AndrolibException {
        return ResXmlEncoders.hasMultipleNonPositionalSubstitutions(mRawValue);
    }

    @Override
    public void serializeToResValuesXml(XmlSerializer serializer,
                                        ResResource res) throws IOException, AndrolibException {
        String type = res.getResSpec().getType().getName();
        boolean item = !"reference".equals(mType) && !type.equals(mType);

        String body = encodeAsResXmlValue();

        // check for resource reference
        if (!type.equalsIgnoreCase("color")) {
            if (body.contains("@")) {
                if (!res.getFilePath().contains("string")) {
                    item = true;
                }
            }
        }

        // Android does not allow values (false) for ids.xml anymore
        // https://issuetracker.google.com/issues/80475496
        // But it decodes as a ResBoolean, which makes no sense. So force it to empty
        if (type.equalsIgnoreCase("id") && !body.isEmpty()) {
            body = "";
        }

        // check for using attrib as node or item
        String tagName = item ? "item" : type;

        serializer.startTag(null, tagName);
        if (item) {
            serializer.attribute(null, "type", type);
        }
        serializer.attribute(null, "name", res.getResSpec().getName());

        serializeExtraXmlAttrs(serializer, res);

        //if ("description".equals(res.getResSpec().getName())) {
        //    Log.d("DEBUG", "description detected");
        //}
        if (!body.isEmpty()) {
            serializer.ignorableWhitespace(body);
        }

        serializer.endTag(null, tagName);
    }

    public String getType() {
        return mType;
    }

    protected void serializeExtraXmlAttrs(XmlSerializer serializer,
                                          ResResource res) throws IOException {
    }

    protected abstract String encodeAsResXml() throws AndrolibException;

    // Added by pujiang
    public void setValue(String v) {
        this.mRawValue = v;
    }

    public String getHtmlValue() {
        return this.mRawValue;
    }

    public String getRawValue() {
        return this.mValueRawRaw;
    }

    public boolean isStyled() {
        return mStyled;
    }

    public String toString() {
        try {
            return encodeAsResXmlValue();
        } catch (AndrolibException e) {
        }
        return null;
    }
}
