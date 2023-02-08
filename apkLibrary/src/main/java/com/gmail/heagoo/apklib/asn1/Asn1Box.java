// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

package com.gmail.heagoo.apklib.asn1;

import java.io.IOException;
import java.io.OutputStream;

// Referenced classes of package com.gmail.heagoo.pmaster.asn1:
//            Asn1Data

public class Asn1Box extends Asn1Data {

    protected byte mByteData[];
    protected Asn1Data mData;

    public Asn1Box(Asn1Data asn1data) {
        mData = asn1data;
        mByteData = null;
    }

    public Asn1Box(byte abyte0[]) {
        mData = null;
        mByteData = abyte0;
    }

    public int getBodyLength() {
        if (mData == null)
            return mByteData.length;
        else
            return mData.getTotalLength();
    }

    public void write(OutputStream outputstream)
            throws IOException {
        outputstream.write(160);
        writeLength(outputstream, getBodyLength());
        if (mData == null) {
            outputstream.write(mByteData);
            return;
        } else {
            mData.write(outputstream);
            return;
        }
    }
}
