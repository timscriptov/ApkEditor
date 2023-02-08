package com.gmail.heagoo.apkeditor.ce.e;

import static com.gmail.heagoo.apkeditor.ce.ManifestParser.getNameFromAttr;

import android.util.TypedValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Tag {

    public static final int END_DOC_TAG = 0x00100101;
    public static final int START_TAG = 0x00100102;
    public static final int END_TAG = 0x00100103;
    public static final int NAMESPACE_TAG = 0x00100100;
    public static final int CDATA_TAG = 0x00100104;

    //    // Contains no data.
    //    TYPE_NULL=0x00,
    //    // The 'data' holds a ResTable_ref, a reference to another resource
    //    // table entry.
    //    TYPE_REFERENCE=0x01,
    //    // The 'data' holds an attribute resource identifier.
    //    TYPE_ATTRIBUTE=0x02,
    //    // The 'data' holds an index into the containing resource table's
    //    // global value string pool.
    //    TYPE_STRING=0x03,
    //    // The 'data' holds a single-precision floating point number.
    //    TYPE_FLOAT=0x04,
    //    // The 'data' holds a complex number encoding a dimension value,
    //    // such as "100in".
    //    TYPE_DIMENSION=0x05,
    //    // The 'data' holds a complex number encoding a fraction of a
    //    // container.
    //    TYPE_FRACTION=0x06,
    //
    //    // Beginning of integer flavors...
    //    TYPE_FIRST_INT=0x10,
    //
    //    // The 'data' is a raw integer value of the form n..n.
    //    TYPE_INT_DEC=0x10,
    //    // The 'data' is a raw integer value of the form 0xn..n.
    //    TYPE_INT_HEX=0x11,
    //    // The 'data' is either 0 or 1, for input "false" or "true" respectively.
    //    TYPE_INT_BOOLEAN=0x12,
    //
    //    // Beginning of color integer flavors...
    //    TYPE_FIRST_COLOR_INT=0x1c,
    //
    //    // The 'data' is a raw integer value of the form #aarrggbb.
    //    TYPE_INT_COLOR_ARGB8=0x1c,
    //    // The 'data' is a raw integer value of the form #rrggbb.
    //    TYPE_INT_COLOR_RGB8=0x1d,
    //    // The 'data' is a raw integer value of the form #argb.
    //    TYPE_INT_COLOR_ARGB4=0x1e,
    //    // The 'data' is a raw integer value of the form #rgb.
    //    TYPE_INT_COLOR_RGB4=0x1f,
    //
    //    // ...end of integer flavors.
    //    TYPE_LAST_COLOR_INT=0x1f,
    //
    //    // ...end of integer flavors.
    //    TYPE_LAST_INT=0x1f

    // Raw data of this tag
    private int tagType;
    private byte[] rawData;
    private int indent;
    private String tagName;
    private List<TagAttr> attrList;
    private Tag parentTag; // Parent tag, only available for start tag
    private Tag startTag;  // Corresponded start tag, only for end tag

    private ResStringChunk stringChunk;
    private ResAttrIdChunk attrIdChunk;

    Tag(int tagType, byte[] rawData,
        ResStringChunk strChunk, ResAttrIdChunk attrIdChunk, int indent) {
        this.tagType = tagType;
        this.rawData = rawData;
        this.stringChunk = strChunk;
        this.attrIdChunk = attrIdChunk;
        this.indent = indent;

        switch (tagType) {
            case START_TAG:
                this.attrList = parseStartTag();
                break;
            case END_TAG:
                parseEndTag();
                break;
        }
    }

    private static void reviseIndexByOffset(int[] mapping, byte[] rawData, int offset) {
        int index = ManifestEditorNew.getInt(rawData, offset);
        if (index >= 0 && index < mapping.length) {
            ManifestEditorNew.setInt(rawData, offset, mapping[index]);
        }
    }

    protected void setParentTag(Tag parentTag) {
        this.parentTag = parentTag;
    }

    protected void setStartTag(Tag startTag) {
        this.startTag = startTag;
    }

    public int getTagType() {
        return tagType;
    }

    public String getTagName() {
        return tagName;
    }

    public String getTagPath() {
        List<String> names = new ArrayList<>();
        Tag curTag = this;
        while (curTag != null) {
            names.add(curTag.tagName);
            curTag = curTag.parentTag;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = names.size() - 1; i >= 0; --i) {
            sb.append(names.get(i));
            sb.append("/");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public void dump(MyFileOutput out) throws IOException {
        out.writeBytes(rawData);
    }

    // mapping[oldIndex] = newIndex
    protected void reviseIndex(int[] mapping) {
        if (tagType == START_TAG) {
            // Revise namespace string index & name index
            reviseIndexByOffset(mapping, rawData, 4 * 4);
            reviseIndexByOffset(mapping, rawData, 5 * 4);

            if (this.attrList != null) {
                for (TagAttr attr : attrList) {
                    attr.reviseIndex(mapping);
                }
            }
        } else if (tagType == END_TAG) { // XML END TAG
            reviseIndexByOffset(mapping, rawData, 4 * 4);
            reviseIndexByOffset(mapping, rawData, 5 * 4);
        } else if (tagType == END_DOC_TAG) { // END OF XML DOC TAG
            // prefix and uri
            reviseIndexByOffset(mapping, rawData, 4 * 4);
            reviseIndexByOffset(mapping, rawData, 5 * 4);
        } else if (tagType == NAMESPACE_TAG) {
            // prefix and uri
            reviseIndexByOffset(mapping, rawData, 4 * 4);
            reviseIndexByOffset(mapping, rawData, 5 * 4);
        }
    }

    public List<TagAttr> getAttrList() {
        return attrList;
    }

    private List<TagAttr> parseStartTag() {
        int chunkTag = ManifestEditorNew.getInt(rawData, 0);
        int chunkSize = ManifestEditorNew.getInt(rawData, 4);

        // Double check the chunk type and chunk size
        if (chunkTag != START_TAG) {
            return null;
        }
        if (chunkSize != rawData.length) {
            return null;
        }

        // int lineNo = AddAttribute.getInt(rawChunkData, 2 * 4);
        // int tag3 = AddAttribute.getInt(rawChunkData, 3 * 4);
        //int nameNsSi = ManifestEditorNew.getInt(rawData, 4 * 4);
        int nameSi = ManifestEditorNew.getInt(rawData, 5 * 4);
        this.tagName = stringChunk.getStringByIndex(nameSi);
        // Expected to be 14001400 (attribute start & attribute size)
        // int tag6 = AddAttribute.getInt(rawChunkData, 6 * 4);
        // Attr number (16bit)
        int attrNum = ManifestEditorNew.getShort(rawData, 7 * 4);
        // Index (1-based) of the "id" attribute (16bit)
        // Index (1-based) of the "class" attribute (16bit)
        // Index (1-based) of the "style" attribute (16bit)

        List<TagAttr> attrList = new ArrayList<>();
        for (int ii = 0; ii < attrNum; ii++) {
            TagAttr attr = new TagAttr(this, rawData, 36 + ii * 20);
            attrList.add(attr);
        }

        return attrList;
    }

    // Example:
    //            00000294 03 01       // type [XML_END_ELEMENT]
    //            00000296 10 00       // header size
    //            00000298 18 00 00 00 // chunk size
    //            --------------------
    //
    //            0000029c 0c 00 00 00 // lineNumber
    //            000002a0 ff ff ff ff // comment
    //            ++++++++++++++++++++
    //
    //            000002a4 ff ff ff ff // ns
    //            000002a8 07 00 00 00 // name [LinearLayout]
    //            ==================== [End of XML_END_ELEMENT]
    private void parseEndTag() {
        if (rawData.length >= 24) {
            int nameIdx = ManifestEditorNew.getInt(rawData, 20);
            tagName = stringChunk.getStringByIndex(nameIdx);
        }
    }

    private String getIndentString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; ++i) {
            sb.append("    ");
        }
        return sb.toString();
    }

    public String toString() {
        switch (tagType) {
            case START_TAG:
                StringBuilder sb = new StringBuilder();
                sb.append(getIndentString());
                sb.append("<");
                sb.append(tagName);
                if (attrList != null) {
                    for (int i = 0; i < attrList.size(); ++i) {
                        sb.append("fuck");
                        sb.append(attrList.get(i).toString());
                    }
                }
                sb.append(">");
                return sb.toString();
            case END_TAG:
                return getIndentString() + "</" + tagName + ">";
            default:
                return getIndentString() + "<unsupported tag>: " + tagType;
        }
    }

    // addAnAttribute(0, -1, 0x10000008, newInfo.installLocation);
    public void addAttribute(int nameIdx, int rawValue, int typeAndSize, int typedValue) {
        int chunkSize = rawData.length;
        byte[] newRawBuf = new byte[rawData.length + 20];
        System.arraycopy(rawData, 0, newRawBuf, 0, 36);
        System.arraycopy(rawData, 36, newRawBuf, 36 + 20, chunkSize - 36);

        // New attr (5 bytes)
        int newAttrOffset = 36;
        int androidIndex = stringChunk.getStringIndex("android");
        ManifestEditorNew.setInt(newRawBuf, newAttrOffset, androidIndex);
        ManifestEditorNew.setInt(newRawBuf, newAttrOffset + 4, nameIdx); // index(installLocation)=0
        ManifestEditorNew.setInt(newRawBuf, newAttrOffset + 8, rawValue);
        ManifestEditorNew.setInt(newRawBuf, newAttrOffset + 12, typeAndSize);
        ManifestEditorNew.setInt(newRawBuf, newAttrOffset + 16, typedValue);

        // Chunk size & attr count
        chunkSize += 20;
        ManifestEditorNew.setInt(newRawBuf, 4, chunkSize); // Chunk size
        int attrNum = (attrList != null ? attrList.size() : 0) + 1;
        ManifestEditorNew.setInt(newRawBuf, 7 * 4, attrNum); // Attribute count

        rawData = newRawBuf;

        // Update attrList
        this.attrList = new ArrayList<>();
        for (int ii = 0; ii < attrNum; ii++) {
            TagAttr attr = new TagAttr(this, rawData, 36 + ii * 20);
            attrList.add(attr);
        }
    }

    public static class TagAttr {
        public int offset; // offset inside rawData
        public String name;
        public String value;
        private byte[] rawData;
        private int valueRaw;
        private int valueType;
        private int valueData; // int value in typedData

        // Example:
        //        000001e0 05 00 00 00 // attribute[0] ns
        //        000001e4 00 00 00 00 // attribute[0] name [orientation]
        //        000001e8 ff ff ff ff // attribute[0] rawValue
        //        000001ec 08 00       // size
        //        000001ee 00          // 0
        //        000001ef 10          // dataType
        //        000001f0 01 00 00 00 // data
        TagAttr(Tag containingTag, byte[] rawData, int offset) {
            this.rawData = rawData;
            this.offset = offset;

            //int attrNsIdx = ManifestEditorNew.getInt(rawData, offset);
            int attrNameIdx = ManifestEditorNew.getInt(rawData, offset + 4);
            // Raw value index
            this.valueRaw = ManifestEditorNew.getInt(rawData, offset + 8);
            // Typed value
            int type = ManifestEditorNew.getInt(rawData, offset + 12) >> 16;
            this.valueType = ((type & 0xff00) >> 8) | ((type & 0x00ff) << 8);
            this.valueData = ManifestEditorNew.getInt(rawData, offset + 16);

            // Parse to string value
            name = containingTag.stringChunk.getStringByIndex(attrNameIdx);
            if (name == null || name.equals("")) {
                if (attrNameIdx < containingTag.attrIdChunk.getCount()) {
                    name = getNameFromAttr(containingTag.attrIdChunk.getAttrId(attrNameIdx));
                }
            }

            if (valueType == TypedValue.TYPE_STRING) {
                int idx = (valueData >= 0 ? valueData : valueRaw);
                value = containingTag.stringChunk.getStringByIndex(idx);
            }
        }

        public String toString() {
            if (value != null) {
                return name + "=\"" + value + "\"";
            } else {
                return name + "=valueType:" + valueType + ",valueData:" + valueData;
            }
        }

        // Revise the attribute value index
        private void reviseIndex(int[] mapping) {
            Tag.reviseIndexByOffset(mapping, rawData, offset);
            Tag.reviseIndexByOffset(mapping, rawData, offset + 4);
            Tag.reviseIndexByOffset(mapping, rawData, offset + 8);
            if (valueType == TypedValue.TYPE_STRING) {
                Tag.reviseIndexByOffset(mapping, rawData, offset + 16);
            }
        }

        public String getName() {
            return name;
        }

        public byte[] getRawData() {
            return rawData;
        }

        public int getOffset() {
            return offset;
        }

        public int getValueRaw() {
            return valueRaw;
        }

        public int getValueType() {
            return valueType;
        }

        public int getValueData() {
            return valueData;
        }
    }
}
