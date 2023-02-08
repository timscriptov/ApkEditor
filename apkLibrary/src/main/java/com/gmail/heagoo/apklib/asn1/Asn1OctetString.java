// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

package com.gmail.heagoo.apklib.asn1;

import java.io.IOException;
import java.io.OutputStream;

// Referenced classes of package com.gmail.heagoo.pmaster.asn1:
//            Asn1Data

public class Asn1OctetString extends Asn1Data {

    private byte mData[];

    public Asn1OctetString(byte abyte0[]) {
        mData = abyte0;
    }

    public int getBodyLength() {
        return mData.length;
    }

    public void write(OutputStream outputstream)
            throws IOException {
        outputstream.write(4);
        writeLength(outputstream, getBodyLength());
        outputstream.write(mData);
    }
}
