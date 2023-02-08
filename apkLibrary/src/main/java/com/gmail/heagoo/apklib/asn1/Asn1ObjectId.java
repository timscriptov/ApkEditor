// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

package com.gmail.heagoo.apklib.asn1;


import java.io.IOException;
import java.io.OutputStream;

// Referenced classes of package com.gmail.heagoo.pmaster.asn1:
//            Asn1Data

public class Asn1ObjectId extends Asn1Data {

    private int mId[];

    public Asn1ObjectId(String s) {
        String as[] = s.split("\\.");
        mId = new int[as.length];
        int i = 0;
        do {
            if (i >= as.length)
                return;
            mId[i] = Integer.parseInt(as[i]);
            i++;
        } while (true);
    }

    public int getBodyLength() {
        int i, size;

        if ((mId.length < 2) || (mId[0] < 0) || (mId[0] > 2) || (mId[1] < 0)
                || (mId[1] > 39))
            throw new IllegalArgumentException("Object identifier out of range");

        size = 1;

        for (i = 2; i < mId.length; i++) {
            if (mId[i] > 16384) {
                size += 3;
            } else if (mId[i] > 128) {
                size += 2;
            } else {
                size++;
            }
        }

        return size;
    }

    public void write(OutputStream outputstream) throws IOException {
        int i, j, size, val;

        // Write the tag 0x06
        outputstream.write(6);

        // Write length
        size = getBodyLength();
        writeLength(outputstream, getBodyLength());

        // Get the Body
        byte[] value = new byte[size];

        value[0] = (byte) (40 * mId[0] + mId[1]);

        j = 1;
        for (i = 2; i < mId.length; i++) {
            val = mId[i];

            if (val >= 0x4000) {
                value[j++] = (byte) ((val >> 14) | 0x80);
                val &= 0x3FFF;
            }
            if (val >= 0x80) {
                value[j++] = (byte) ((val >> 7) | 0x80);
                val &= 0x7F;
            }
            value[j++] = (byte) (val);
        }

        outputstream.write(value);
    }
}
