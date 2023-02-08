package com.gmail.heagoo.apkeditor.ce.e;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class AxmlBodyChunk {

//    public static int endDocTag = 0x00100101;
//    public static int startTag = 0x00100102;
//    public static int endTag = 0x00100103;
//    public static int namespaceTag = 0x00100100;
//    public static int cdataTag = 0x00100104;

    private byte[] rawChunkData;

    private ResStringChunk strChunk;
    private ResAttrIdChunk attrIdChunk;
    private List<Tag> bodyTags = new ArrayList<>();

    // addedAttrPosition = the position of added attribute name in string table
    public AxmlBodyChunk(ResStringChunk strChunk, ResAttrIdChunk attrIdChunk) {
        this.strChunk = strChunk;
        this.attrIdChunk = attrIdChunk;
    }

    private byte[] getRawChunkData() {
        return rawChunkData;
    }

    public int parseNext(MyInputStream is) throws IOException {
        int chunkTag = is.readInt();
        int chunkSize = is.readInt();

        rawChunkData = new byte[chunkSize];
        ManifestEditorNew.setInt(rawChunkData, 0, chunkTag);
        ManifestEditorNew.setInt(rawChunkData, 4, chunkSize);
        if (chunkSize > 8) {
            is.readFully(rawChunkData, 8, chunkSize - 8);
        }

        return chunkTag;
    }

    public void parse(MyInputStream is) throws IOException {
        Stack<Tag> tagStack = new Stack<>();
        int tagType = 0;
        do {
            tagType = parseNext(is);
            byte[] data = getRawChunkData();

            Tag tag = null;
            if (tagType == Tag.START_TAG) {
                tag = new Tag(tagType, data, strChunk, attrIdChunk, tagStack.size());
                if (!tagStack.isEmpty()) {
                    tag.setParentTag(tagStack.peek());
                }
                tagStack.push(tag);
            } else if (tagType == Tag.END_TAG) {
                if (!tagStack.isEmpty()) {
                    Tag startTag = tagStack.pop();
                    tag = new Tag(tagType, data, strChunk, attrIdChunk, tagStack.size());
                    tag.setStartTag(startTag);
                }
            } else {
                tag = new Tag(tagType, data, strChunk, attrIdChunk, tagStack.size());
            }

            this.bodyTags.add(tag);
        } while (tagType != AxmlManipulateBodyChunk.endDocTag);
    }

    public void dump(MyFileOutput out) throws IOException {
        for (Tag tag : bodyTags) {
            tag.dump(out);
        }
    }

    // Update all string indices as some string is added
    public void updateStringIndex() {
        List<Integer> positions = strChunk.getAddedStrPositions();
        if (positions != null && !positions.isEmpty()) {
            int[] mapping = strChunk.getStringIndexMapping();
            // Revise all the indices
            for (Tag tag : bodyTags) {
                tag.reviseIndex(mapping);
            }
        }
    }

    // addAttribute("manifest", 0, -1, 0x10000008, newInfo.installLocation);
    public void addAttribute(String tagPath,
                             int nameIdx, int rawValue, int typeAndSize, int typedValue) {
        for (Tag tag : bodyTags) {
            if (tag.getTagType() != Tag.START_TAG) {
                continue;
            }
            if (tagPath.equals(tag.getTagPath())) {
                tag.addAttribute(nameIdx, rawValue, typeAndSize, typedValue);
            }
        }
    }

    // Find out the attribute and then do callback to modify it
    public void modifyAttribute(String tagPath, String attrName,
                                IModifyTagAttribute iModifyTagAttribute) {
        for (Tag tag : bodyTags) {
            if (tag.getTagType() != Tag.START_TAG) {
                continue;
            }
            if (tagPath.equals(tag.getTagPath())) {
                List<Tag.TagAttr> attrList = tag.getAttrList();
                if (attrList != null) {
                    for (Tag.TagAttr attr : attrList) {
                        String name = attr.getName();
                        if (attrName.equals(name)) {
                            iModifyTagAttribute.modify(attr);
                        }
                    }
                } // end if
            } // end if
        } // end for tag
    }
}
