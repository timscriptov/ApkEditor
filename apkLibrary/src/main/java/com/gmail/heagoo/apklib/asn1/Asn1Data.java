// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

package com.gmail.heagoo.apklib.asn1;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

public abstract class Asn1Data {

    protected int mLength;
    protected int mTag;

    public Asn1Data() {
    }

    public static int getLengthLength(int i) {
        if (i > 127) {
            byte abyte0[] = BigInteger.valueOf(i).toByteArray();
            if (abyte0[0] == 0)
                return abyte0.length;
            else
                return 1 + abyte0.length;
        } else {
            return 1;
        }
    }

    public static void writeLength(OutputStream outputstream, int i)
            throws IOException {
        if (i > 127) {
            byte abyte0[] = BigInteger.valueOf(i).toByteArray();
            if (abyte0[0] == 0) {
                outputstream.write(0x80 | -1 + abyte0.length);
                outputstream.write(abyte0, 1, -1 + abyte0.length);
                return;
            } else {
                outputstream.write(0x80 | abyte0.length);
                outputstream.write(abyte0);
                return;
            }
        } else {
            outputstream.write(i);
            return;
        }
    }

    public abstract int getBodyLength();

    public int getTotalLength() {
        return 1 + getLengthLength(getBodyLength()) + getBodyLength();
    }

    public abstract void write(OutputStream outputstream)
            throws IOException;
}
