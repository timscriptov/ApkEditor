/**
 * Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package brut.util;

import java.io.DataInput;
import java.io.IOException;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
//abstract public class DataInputDelegateOld implements DataInput {
//	protected final DataInput mDelegate;
//
//	public DataInputDelegateOld(DataInput delegate) {
//		this.mDelegate = delegate;
//	}
//
//	public int skipBytes(int n) throws IOException {
//		return mDelegate.skipBytes(n);
//	}
//
//	public int readUnsignedShort() throws IOException {
//		return mDelegate.readUnsignedShort();
//	}
//
//	public int readUnsignedByte() throws IOException {
//		return mDelegate.readUnsignedByte();
//	}
//
//	public String readUTF() throws IOException {
//		return mDelegate.readUTF();
//	}
//
//	public short readShort() throws IOException {
//		return mDelegate.readShort();
//	}
//
//	public long readLong() throws IOException {
//		return mDelegate.readLong();
//	}
//
//	public String readLine() throws IOException {
//		return mDelegate.readLine();
//	}
//
//	public int readInt() throws IOException {
//		return mDelegate.readInt();
//	}
//
//	public int[] readIntArray(int length) throws IOException {
//		int[] array = new int[length];
//		for (int i = 0; i < length; i++) {
//			array[i] = readInt();
//		}
//		return array;
//	}
//
//	public void readFully(byte[] b, int off, int len) throws IOException {
//		mDelegate.readFully(b, off, len);
//	}
//
//	public void readFully(byte[] b) throws IOException {
//		mDelegate.readFully(b);
//	}
//
//	public float readFloat() throws IOException {
//		return mDelegate.readFloat();
//	}
//
//	public double readDouble() throws IOException {
//		return mDelegate.readDouble();
//	}
//
//	public char readChar() throws IOException {
//		return mDelegate.readChar();
//	}
//
//	public byte readByte() throws IOException {
//		return mDelegate.readByte();
//	}
//
//	public boolean readBoolean() throws IOException {
//		return mDelegate.readBoolean();
//	}
//}
abstract public class DataInputDelegateOld implements DataInput {
    protected final DataInput mDelegate;
    //private int offset = 0;

    public DataInputDelegateOld(DataInput delegate) {
        this.mDelegate = delegate;
    }

    public int skipBytes(int n) throws IOException {
        return mDelegate.skipBytes(n);
    }

    public int readUnsignedShort() throws IOException {
        return mDelegate.readUnsignedShort();
    }

    public int readUnsignedByte() throws IOException {
        return mDelegate.readUnsignedByte();
    }

    public String readUTF() throws IOException {
        return mDelegate.readUTF();
    }

    public short readShort() throws IOException {
        return mDelegate.readShort();
    }

    public long readLong() throws IOException {
        return mDelegate.readLong();
    }

    public String readLine() throws IOException {
        return mDelegate.readLine();
    }

    public int readInt() throws IOException {
        return mDelegate.readInt();
    }

    public int[] readIntArray(int length) throws IOException {
        //LOGGER.debug(String.format("readIntArray(%d)", length));
        int[] array = new int[length];
        for (int i = 0; i < length; i++) {
            array[i] = readInt();
        }

        return array;
    }

    // LE order
    public int[] leReadIntArray(int length) throws IOException {
        byte[] buf = new byte[4 * length];
        int[] array = new int[length];
        mDelegate.readFully(buf);

        int index = 0;
        for (int i = 0; i < length; i++) {
            array[i] = (buf[index + 3]) << 24 | (buf[index + 2] & 0xff) << 16
                    | (buf[index + 1] & 0xff) << 8 | (buf[index + 0] & 0xff);
            index += 4;
        }

        return array;
    }

    public void readFully(byte[] b, int off, int len) throws IOException {
        //LOGGER.debug(String.format("readFully(b, %d, %d)", off, len));
        mDelegate.readFully(b, off, len);
        //offset += len;
    }

    public void readFully(byte[] b) throws IOException {
        mDelegate.readFully(b);
    }

    public float readFloat() throws IOException {
        return mDelegate.readFloat();
    }

    public double readDouble() throws IOException {
        return mDelegate.readDouble();
    }

    public char readChar() throws IOException {
        return mDelegate.readChar();
    }

    public byte readByte() throws IOException {
        return mDelegate.readByte();
    }

    public boolean readBoolean() throws IOException {
        return mDelegate.readBoolean();
    }
}
