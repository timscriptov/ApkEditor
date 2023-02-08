// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

package com.gmail.heagoo.apklib.asn1;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

// Referenced classes of package com.gmail.heagoo.pmaster.asn1:
//            Asn1Data

public class Asn1Sequence extends Asn1Data {

    protected ArrayList mList;

    public Asn1Sequence() {
        mList = new ArrayList();
    }

    public Asn1Sequence add(Asn1Data asn1data) {
        mList.add(asn1data);
        return this;
    }

    public int getBodyLength() {
        int i = 0;
        Iterator iterator = mList.iterator();
        do {
            if (!iterator.hasNext())
                return i;
            i += ((Asn1Data) iterator.next()).getTotalLength();
        } while (true);
    }

    public void write(OutputStream outputstream)
            throws IOException {
        outputstream.write(48);
        writeLength(outputstream, getBodyLength());
        Iterator iterator = mList.iterator();
        do {
            if (!iterator.hasNext())
                return;
            ((Asn1Data) iterator.next()).write(outputstream);
        } while (true);
    }
}
