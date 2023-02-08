// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

package com.gmail.heagoo.apklib.asn1;

import java.io.IOException;
import java.io.OutputStream;

// Referenced classes of package com.gmail.heagoo.pmaster.asn1:
//            Asn1Data

public class Asn1Null extends Asn1Data {

    public Asn1Null() {
    }

    public int getBodyLength() {
        return 0;
    }

    public void write(OutputStream outputstream)
            throws IOException {
        outputstream.write(5);
        outputstream.write(0);
    }
}
