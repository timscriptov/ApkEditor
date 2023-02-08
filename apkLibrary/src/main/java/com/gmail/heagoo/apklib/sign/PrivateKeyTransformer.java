package com.gmail.heagoo.apklib.sign;

import java.io.IOException;
import java.io.InputStream;

public class PrivateKeyTransformer extends InputStream {

    private InputStream input;

    // Position in input stream
    private int position = 0;

    public PrivateKeyTransformer(InputStream is) {
        this.input = is;
    }

    @Override
    public int read() throws IOException {
        int ret = input.read();
        if (ret != -1) {
            ret = (int) transform((byte) ret, position) & 0xff;
            position += 1;
        }
        return ret;
    }

    @Override
    public int read(byte[] buf) throws IOException {
        return this.read(buf, 0, buf.length);
    }

    @Override
    public int read(byte[] buf, int offset, int len) throws IOException {
        int ret = input.read(buf, offset, len);
        if (ret != -1) {
            for (int i = 0; i < ret; i++) {
                buf[offset + i] = transform(buf[offset + i], position + i);
            }
            position += ret;
        }
        return ret;
    }

    private byte transform(byte b, int _position) {
        int val = (int) b & 0xff;
        if (_position % 16 == 0) {
            val += 1;
        }
        val = val ^ 0x55;
        return (byte) val;
    }
}
