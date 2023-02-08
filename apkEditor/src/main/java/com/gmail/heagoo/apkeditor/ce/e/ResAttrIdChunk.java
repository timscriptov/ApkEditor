package com.gmail.heagoo.apkeditor.ce.e;

import java.io.IOException;

public class ResAttrIdChunk {
    public int[] attrIdArray;
    int chunkTag;
    int chunkSize;

    public void parse(MyInputStream is) throws IOException {
        chunkTag = is.readInt();
        chunkSize = is.readInt();
        int attrCount = (chunkSize - 8) / 4;
        attrIdArray = new int[attrCount];
        ManifestEditorNew.log("Attr Count: " + attrCount);
        for (int i = 0; i < attrCount; i++) {
            attrIdArray[i] = is.readInt();
            ManifestEditorNew.log("\t" + attrIdArray[i]);
        }
    }

    public int getCount() {
        if (attrIdArray != null) {
            return attrIdArray.length;
        } else {
            return 0;
        }
    }

    public int getAttrId(int index) {
        return attrIdArray[index];
    }

    public void addAttributeId(int value, int position) {
        chunkSize += 4;
        int[] newAttrIds = new int[attrIdArray.length + 1];
        for (int i = 0; i < position; i++) {
            newAttrIds[i] = attrIdArray[i];
        }
        newAttrIds[position] = value;
        for (int i = position + 1; i < newAttrIds.length; i++) {
            newAttrIds[i] = attrIdArray[i - 1];
        }
        attrIdArray = newAttrIds;
    }

    public void dump(MyFileOutput out) throws IOException {
        out.writeInt(chunkTag);
        out.writeInt(chunkSize);
        out.writeIntArray(attrIdArray);
    }
}
