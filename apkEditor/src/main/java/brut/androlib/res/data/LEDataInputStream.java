/*
 * @(#)LEDataInputStream.java
 *
 * Summary: Little-Endian version of DataInputStream.
 *
 * Copyright: (c) 1998-2010 Roedy Green, Canadian Mind Products, http://mindprod.com
 *
 * Licence: This software may be copied and used freely for any purpose but military.
 *          http://mindprod.com/contact/nonmil.html
 *
 * Requires: JDK 1.1+
 *
 * Created with: IntelliJ IDEA IDE.
 *
 * Version History:
 *  1.8 2007-05-24
 */
package brut.androlib.res.data;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

///**
// * Little-Endian version of DataInputStream.
// * <p/>
// * Very similar to DataInputStream except it reads little-endian instead of
// * big-endian binary data. We can't extend DataInputStream directly since it has
// * only final methods, though DataInputStream itself is not final. This forces
// * us implement LEDataInputStream with a DataInputStream object, and use wrapper
// * methods.
// * 
// * @author Roedy Green, Canadian Mind Products
// * @version 1.8 2007-05-24
// * @since 1998
// */
//public final class LEDataInputStream implements DataInput {
//	// ------------------------------ CONSTANTS ------------------------------
//
//	/**
//	 * undisplayed copyright notice.
//	 * 
//	 * @noinspection UnusedDeclaration
//	 */
//
//	// ------------------------------ FIELDS ------------------------------
//
//	/**
//	 * to get at the big-Endian methods of a basic DataInputStream
//	 * 
//	 * @noinspection WeakerAccess
//	 */
//	// protected final DataInputStream dis;
//
//	/**
//	 * to get at the a basic readBytes method.
//	 * 
//	 * @noinspection WeakerAccess
//	 */
//	protected final InputStream is;
//
//	/**
//	 * work array for buffering input.
//	 * 
//	 * @noinspection WeakerAccess
//	 */
//	protected byte[] work;
//
//	// Work as cache
//	private byte[] cacheBuffer;
//	private int startPosition = 0;
//	private int endPosition = 0;
//
//	// -------------------------- PUBLIC STATIC METHODS
//	// --------------------------
//
//	/**
//	 * Note. This is a STATIC method!
//	 * 
//	 * @param in
//	 *            stream to read UTF chars from (endian irrelevant)
//	 * 
//	 * @return string from stream
//	 * @throws IOException
//	 *             if read fails.
//	 */
//	// public static String readUTF(DataInput in) throws IOException {
//	// return DataInputStream.readUTF(in);
//	// }
//
//	// -------------------------- PUBLIC INSTANCE METHODS
//	// --------------------------
//
//	/**
//	 * constructor.
//	 * 
//	 * @param in
//	 *            binary inputstream of little-endian data.
//	 */
//	public LEDataInputStream(InputStream in) {
//		this.is = in;
//		// this.dis = new DataInputStream(in);
//		work = new byte[1024];
//
//		cacheBuffer = new byte[8192];
//	}
//
//	/**
//	 * close.
//	 * 
//	 * @throws IOException
//	 *             if close fails.
//	 */
//	public final void close() throws IOException {
//		is.close();
//	}
//
//	/**
//	 * Read bytes. Watch out, read may return fewer bytes than requested.
//	 * 
//	 * @param ba
//	 *            where the bytes go.
//	 * @param off
//	 *            offset in buffer, not offset in file.
//	 * @param len
//	 *            count of bytes to read.
//	 * 
//	 * @return how many bytes read.
//	 * @throws IOException
//	 *             if read fails.
//	 */
//	public final int read(byte ba[], int off, int len) throws IOException {
//		// return is.read(ba, off, len);
//
//		int readLen = getFromCache(ba, off, len);
//
//		while (readLen < len) {
//			int curRead = is.read(ba, off + readLen, len - readLen);
//			if (curRead > 0) {
//				readLen += curRead;
//			} else {
//				break;
//			}
//		}
//
//		return readLen;
//	}
//
//	private int read() throws IOException {
//		if (read(work, 0, 1) > 0) {
//			return (work[0] & 0xff);
//		} else {
//			return -1;
//		}
//	}
//
//	// Just try to get some data from cache
//	// May not return all the data, even there are still some data to read
//	private int getFromCache(byte[] ba, int off, int len) throws IOException {
//		if (startPosition < endPosition) {
//			int available = endPosition - startPosition;
//			int copyLen = (len < available ? len : available);
//			System.arraycopy(cacheBuffer, startPosition, ba, off, copyLen);
//			startPosition += copyLen;
//			return copyLen;
//		}
//		// To load data from input stream
//		else {
//			startPosition = 0;
//			endPosition = is.read(cacheBuffer);
//			if (endPosition > 0) {
//				return getFromCache(ba, off, len);
//			} else {
//				endPosition = 0;
//				return 0;
//			}
//		}
//	}
//
//	/**
//	 * read only a one-byte boolean.
//	 * 
//	 * @return true or false.
//	 * @throws IOException
//	 *             if read fails.
//	 * @see java.io.DataInput#readBoolean()
//	 */
//	@Override
//	public final boolean readBoolean() throws IOException {
//		// return dis.readBoolean();
//		getFromCache(work, 0, 1);
//		return (work[0] != 0);
//	}
//
//	/**
//	 * read byte.
//	 * 
//	 * @return the byte read.
//	 * @throws IOException
//	 *             if read fails.
//	 * @see java.io.DataInput#readByte()
//	 */
//	@Override
//	public final byte readByte() throws IOException {
//		// return dis.readByte();
//		getFromCache(work, 0, 1);
//		return work[0];
//	}
//
//	/**
//	 * Read on char. like DataInputStream.readChar except little endian.
//	 * 
//	 * @return little endian 16-bit unicode char from the stream.
//	 * @throws IOException
//	 *             if read fails.
//	 */
//	@Override
//	public final char readChar() throws IOException {
//		// dis.readFully(work, 0, 2);
//		read(work, 0, 2);
//		return (char) ((work[1] & 0xff) << 8 | (work[0] & 0xff));
//	}
//
//	/**
//	 * Read a double. like DataInputStream.readDouble except little endian.
//	 * 
//	 * @return little endian IEEE double from the datastream.
//	 * @throws IOException
//	 */
//	@Override
//	public final double readDouble() throws IOException {
//		return Double.longBitsToDouble(readLong());
//	}
//
//	/**
//	 * Read one float. Like DataInputStream.readFloat except little endian.
//	 * 
//	 * @return little endian IEEE float from the datastream.
//	 * @throws IOException
//	 *             if read fails.
//	 */
//	@Override
//	public final float readFloat() throws IOException {
//		return Float.intBitsToFloat(readInt());
//	}
//
//	/**
//	 * Read bytes until the array is filled.
//	 * 
//	 * @see java.io.DataInput#readFully(byte[])
//	 */
//	@Override
//	public final void readFully(byte ba[]) throws IOException {
//		// dis.readFully(ba, 0, ba.length);
//		read(ba, 0, ba.length);
//	}
//
//	/**
//	 * Read bytes until the count is satisfied.
//	 * 
//	 * @throws IOException
//	 *             if read fails.
//	 * @see java.io.DataInput#readFully(byte[],int,int)
//	 */
//	@Override
//	public final void readFully(byte ba[], int off, int len) throws IOException {
//
//		// dis.readFully(ba, off, len);
//		// read(ba, off, len);
//
//		int readLen = getFromCache(ba, off, len);
//
//		while (readLen < len) {
//			int curRead = is.read(ba, off + readLen, len - readLen);
//			if (curRead < 0) {
//				throw new IOException("End of stream");
//			}
//			readLen += curRead;
//		}
//	}
//
//	/**
//	 * Read an int, 32-bits. Like DataInputStream.readInt except little endian.
//	 * 
//	 * @return little-endian binary int from the datastream
//	 * @throws IOException
//	 *             if read fails.
//	 */
//	@Override
//	public final int readInt() throws IOException {
//		// dis.readFully(work, 0, 4);
//		read(work, 0, 4);
//		return (work[3]) << 24 | (work[2] & 0xff) << 16 | (work[1] & 0xff) << 8
//				| (work[0] & 0xff);
//	}
//
//	public int[] readIntArray(int length) throws IOException {
//		int byteLen = length * 4;
//		if (work.length < byteLen) {
//			work = new byte[byteLen];
//		}
//
//		read(work, 0, byteLen);
//
//		int[] array = new int[length];
//		for (int i = 0; i < length; i++) {
//			int addr = (i << 2);
//			array[i] = (work[addr + 3]) << 24 | (work[addr + 2] & 0xff) << 16
//					| (work[addr + 1] & 0xff) << 8 | (work[addr + 0] & 0xff);
//		}
//		return array;
//	}
//
//	/**
//	 * Read a line.
//	 * 
//	 * @return a rough approximation of the 8-bit stream as a 16-bit unicode
//	 *         string
//	 * @throws IOException
//	 * @noinspection deprecation
//	 * @deprecated This method does not properly convert bytes to characters.
//	 *             Use a Reader instead with a little-endian encoding.
//	 */
//	@Deprecated
//	@Override
//	public final String readLine() throws IOException {
//		// return dis.readLine();
//
//		// buf and lineBuffer is used to store all the line data
//		char buf[] = new char[128];
//		char lineBuffer[] = buf;
//
//		int room = buf.length;
//		int offset = 0;
//		int c;
//
//		loop: while (true) {
//			switch (c = read()) {
//			case -1:
//			case '\n':
//				break loop;
//
//			case '\r':
//				int c2 = read();
//				if ((c2 != '\n') && (c2 != -1)) {
//					if (startPosition > 0) {
//						startPosition -= 1;
//					} else {
//						throw new IOException("Encountered unsupported code!");
//					}
//				}
//				break loop;
//
//			default:
//				if (--room < 0) {
//					buf = new char[offset + 128];
//					room = buf.length - offset - 1;
//					System.arraycopy(lineBuffer, 0, buf, 0, offset);
//					lineBuffer = buf;
//				}
//				buf[offset++] = (char) c;
//				break;
//			}
//		}
//		if ((c == -1) && (offset == 0)) {
//			return null;
//		}
//		String ret = String.copyValueOf(buf, 0, offset);
//		LOGGER.info("readLine called, ret=" + ret);
//		return ret;
//	}
//
//	/**
//	 * read a long, 64-bits. Like DataInputStream.readLong except little endian.
//	 * 
//	 * @return little-endian binary long from the datastream.
//	 * @throws IOException
//	 */
//	@Override
//	public final long readLong() throws IOException {
//		// dis.readFully(work, 0, 8);
//		read(work, 0, 8);
//		return (long) (work[7]) << 56 |
//		/* long cast needed or shift done modulo 32 */
//		(long) (work[6] & 0xff) << 48 | (long) (work[5] & 0xff) << 40
//				| (long) (work[4] & 0xff) << 32 | (long) (work[3] & 0xff) << 24
//				| (long) (work[2] & 0xff) << 16 | (long) (work[1] & 0xff) << 8
//				| work[0] & 0xff;
//	}
//
//	/**
//	 * Read short, 16-bits. Like DataInputStream.readShort except little endian.
//	 * 
//	 * @return little endian binary short from stream.
//	 * @throws IOException
//	 *             if read fails.
//	 */
//	@Override
//	public final short readShort() throws IOException {
//		// dis.readFully(work, 0, 2);
//		read(work, 0, 2);
//		return (short) ((work[1] & 0xff) << 8 | (work[0] & 0xff));
//	}
//
//	/**
//	 * Read UTF counted string.
//	 * 
//	 * @return String read.
//	 */
//	@Override
//	public final String readUTF() throws IOException {
//		// return dis.readUTF();
//		int utflen = readUnsignedShort();
//		byte[] bytearr = null;
//		char[] chararr = null;
//		{
//			bytearr = new byte[utflen];
//			chararr = new char[utflen];
//		}
//
//		int c, char2, char3;
//		int count = 0;
//		int chararr_count = 0;
//
//		readFully(bytearr, 0, utflen);
//
//		while (count < utflen) {
//			c = (int) bytearr[count] & 0xff;
//			if (c > 127)
//				break;
//			count++;
//			chararr[chararr_count++] = (char) c;
//		}
//
//		while (count < utflen) {
//			c = (int) bytearr[count] & 0xff;
//			switch (c >> 4) {
//			case 0:
//			case 1:
//			case 2:
//			case 3:
//			case 4:
//			case 5:
//			case 6:
//			case 7:
//				/* 0xxxxxxx */
//				count++;
//				chararr[chararr_count++] = (char) c;
//				break;
//			case 12:
//			case 13:
//				/* 110x xxxx 10xx xxxx */
//				count += 2;
//				if (count > utflen)
//					throw new IOException(
//							"malformed input: partial character at end");
//				char2 = (int) bytearr[count - 1];
//				if ((char2 & 0xC0) != 0x80)
//					throw new IOException("malformed input around byte "
//							+ count);
//				chararr[chararr_count++] = (char) (((c & 0x1F) << 6) | (char2 & 0x3F));
//				break;
//			case 14:
//				/* 1110 xxxx 10xx xxxx 10xx xxxx */
//				count += 3;
//				if (count > utflen)
//					throw new IOException(
//							"malformed input: partial character at end");
//				char2 = (int) bytearr[count - 2];
//				char3 = (int) bytearr[count - 1];
//				if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
//					throw new IOException("malformed input around byte "
//							+ (count - 1));
//				chararr[chararr_count++] = (char) (((c & 0x0F) << 12)
//						| ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0));
//				break;
//			default:
//				/* 10xx xxxx, 1111 xxxx */
//				throw new IOException("malformed input around byte " + count);
//			}
//		}
//		// The number of chars produced may be less than utflen
//		String ret = new String(chararr, 0, chararr_count);
//		LOGGER.info("readUTF called, ret=" + ret);
//		return ret;
//	}
//
//	/**
//	 * Read an unsigned byte. Note: returns an int, even though says Byte
//	 * (non-Javadoc)
//	 * 
//	 * @throws IOException
//	 *             if read fails.
//	 * @see java.io.DataInput#readUnsignedByte()
//	 */
//	@Override
//	public final int readUnsignedByte() throws IOException {
//		// return dis.readUnsignedByte();
//		read(work, 0, 1);
//		return work[0] & 0xff;
//	}
//
//	/**
//	 * Read an unsigned short, 16 bits. Like DataInputStream.readUnsignedShort
//	 * except little endian. Note, returns int even though it reads a short.
//	 * 
//	 * @return little-endian int from the stream.
//	 * @throws IOException
//	 *             if read fails.
//	 */
//	@Override
//	public final int readUnsignedShort() throws IOException {
//		// dis.readFully(work, 0, 2);
//		read(work, 0, 2);
//		return ((work[1] & 0xff) << 8 | (work[0] & 0xff));
//	}
//
//	/**
//	 * Skip over bytes in the stream. See the general contract of the
//	 * <code>skipBytes</code> method of <code>DataInput</code>.
//	 * <p/>
//	 * Bytes for this operation are read from the contained input stream.
//	 * 
//	 * @param n
//	 *            the number of bytes to be skipped.
//	 * 
//	 * @return the actual number of bytes skipped.
//	 * @throws IOException
//	 *             if an I/O error occurs.
//	 */
//	@Override
//	public final int skipBytes(int n) throws IOException {
//		// return dis.skipBytes(n);
//		int remain = endPosition - startPosition;
////		if (n == 3632) {
////			LOGGER.debug("remain=" + remain);
////		}
//		if (remain >= n) {
//			startPosition += n;
//		} else {
//			startPosition = endPosition;
//			byte[] tmp = new byte[n - remain];
//			readFully(tmp);
//		}
//		return n;
//	}
//}

///////////////////////////////////////////////////////////////////////////////


public class LEDataInputStream implements DataInput {

    private DataInputStream d; // to get at high level readFully methods of
    // DataInputStream
    private InputStream in; // to get at the low-level read methods of
    // InputStream
    private byte w[]; // work array for buffering input

    public LEDataInputStream(InputStream in) {
        this.in = in;
        this.d = new DataInputStream(in);
        w = new byte[8];
    }

    public int available() throws IOException {
        return d.available();
    }

    public final short readShort() throws IOException {
        d.readFully(w, 0, 2);
        return (short) (
                (w[1] & 0xff) << 8 |
                        (w[0] & 0xff));
    }

    /**
     * Note, returns int even though it reads a short.
     */
    public final int readUnsignedShort() throws IOException {
        d.readFully(w, 0, 2);
        return (
                (w[1] & 0xff) << 8 |
                        (w[0] & 0xff));
    }

    /**
     * like DataInputStream.readChar except little endian.
     */
    public final char readChar() throws IOException {
        d.readFully(w, 0, 2);
        return (char) (
                (w[1] & 0xff) << 8 |
                        (w[0] & 0xff));
    }

    /**
     * like DataInputStream.readInt except little endian.
     */
    public final int readInt() throws IOException {
        d.readFully(w, 0, 4);
        return
                (w[3]) << 24 |
                        (w[2] & 0xff) << 16 |
                        (w[1] & 0xff) << 8 |
                        (w[0] & 0xff);
    }

    /**
     * like DataInputStream.readLong except little endian.
     */
    public final long readLong() throws IOException {
        d.readFully(w, 0, 8);
        return
                (long) (w[7]) << 56 |
                        (long) (w[6] & 0xff) << 48 |
                        (long) (w[5] & 0xff) << 40 |
                        (long) (w[4] & 0xff) << 32 |
                        (long) (w[3] & 0xff) << 24 |
                        (long) (w[2] & 0xff) << 16 |
                        (long) (w[1] & 0xff) << 8 |
                        (long) (w[0] & 0xff);
    }

    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public final int read(byte b[], int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    public final void readFully(byte b[]) throws IOException {
        d.readFully(b, 0, b.length);
    }

    public final void readFully(byte b[], int off, int len) throws IOException {
        d.readFully(b, off, len);
    }

    public final int skipBytes(int n) throws IOException {
        return d.skipBytes(n);
    }

    public final boolean readBoolean() throws IOException {
        return d.readBoolean();
    }

    public final byte readByte() throws IOException {
        return d.readByte();
    }

    public int read() throws IOException {
        return in.read();
    }

    public final int readUnsignedByte() throws IOException {
        return d.readUnsignedByte();
    }

    @Deprecated
    public final String readLine() throws IOException {
        return d.readLine();
    }

    public final String readUTF() throws IOException {
        return d.readUTF();
    }

    public final void close() throws IOException {
        d.close();
    }
}