// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

package com.gmail.heagoo.apklib.asn1;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

// Referenced classes of package com.gmail.heagoo.pmaster.asn1:
//            Asn1Data

public class Asn1Integer extends Asn1Data {

    private byte mByte[];
    private BigInteger mInt;

    public Asn1Integer(int i) {
        mInt = BigInteger.valueOf(i);
        mByte = mInt.toByteArray();
    }

    public Asn1Integer(BigInteger biginteger) {
        mInt = biginteger;
        mByte = mInt.toByteArray();
    }

    private boolean needLeadingZero() {
        return mInt.signum() > 0 && (0x80 & mByte[0]) != 0;
    }

    public int getBodyLength() {
        if (!needLeadingZero())
            return mByte.length;
        else
            return 1 + mByte.length;
    }

    public void write(OutputStream outputstream)
            throws IOException {
        outputstream.write(2);
        writeLength(outputstream, getBodyLength());
        if (needLeadingZero())
            outputstream.write(0);
        outputstream.write(mByte);
    }
}
