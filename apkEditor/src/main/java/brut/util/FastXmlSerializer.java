package brut.util;

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * This is a quick and dirty implementation of XmlSerializer that isn't horribly
 * painfully slow like the normal one. It only does what is needed for the
 * specific XML files being written with it.
 */
public class FastXmlSerializer implements XmlSerializer {
    private static final String ESCAPE_TABLE[] = new String[]{null, null,
            null, null, null, null, null, null, // 0-7
            null, null, null, null, null, null, null, null, // 8-15
            null, null, null, null, null, null, null, null, // 16-23
            null, null, null, null, null, null, null, null, // 24-31
            null, null, "&quot;", null, null, null, "&amp;", null, // 32-39
            null, null, null, null, null, null, null, null, // 40-47
            null, null, null, null, null, null, null, null, // 48-55
            null, null, null, null, "&lt;", null, "&gt;", null, // 56-63
    };

    private static final int BUFFER_LEN = 8192;

    private final char[] mText = new char[BUFFER_LEN];
    private int mPos;

    private Writer mWriter;

    private OutputStream mOutputStream;
    private CharsetEncoder mCharset;
    private ByteBuffer mBytes = ByteBuffer.allocate(BUFFER_LEN);

    private boolean mInTag;

    private int mDepth = 0;

    // Record namespace information
    //Map<String, String> uri2Namespace = new HashMap<String, String>();
    private NamespaceManager nsMgr;
    private boolean seenBracket;
    private boolean seenBracketBracket;

    private void append(char c) throws IOException {
        int pos = mPos;
        if (pos >= (BUFFER_LEN - 1)) {
            flush();
            pos = mPos;
        }
        mText[pos] = c;
        mPos = pos + 1;
    }

    private void append(String str, int i, final int length) throws IOException {
        if (length > BUFFER_LEN) {
            final int end = i + length;
            while (i < end) {
                int next = i + BUFFER_LEN;
                append(str, i, next < end ? BUFFER_LEN : (end - i));
                i = next;
            }
            return;
        }
        int pos = mPos;
        if ((pos + length) > BUFFER_LEN) {
            flush();
            pos = mPos;
        }
        str.getChars(i, i + length, mText, pos);
        mPos = pos + length;
    }

    private void append(char[] buf, int i, final int length) throws IOException {
        if (length > BUFFER_LEN) {
            final int end = i + length;
            while (i < end) {
                int next = i + BUFFER_LEN;
                append(buf, i, next < end ? BUFFER_LEN : (end - i));
                i = next;
            }
            return;
        }
        int pos = mPos;
        if ((pos + length) > BUFFER_LEN) {
            flush();
            pos = mPos;
        }
        System.arraycopy(buf, i, mText, pos, length);
        mPos = pos + length;
    }

    private void append(String str) throws IOException {
        append(str, 0, str.length());
    }

    private void escapeAndAppendString(final String string) throws IOException {
        final int N = string.length();
        final char NE = (char) ESCAPE_TABLE.length;
        final String[] escapes = ESCAPE_TABLE;
        int lastPos = 0;
        int pos;
        for (pos = 0; pos < N; pos++) {
            char c = string.charAt(pos);
            if (c >= NE)
                continue;
            String escape = escapes[c];
            if (escape == null)
                continue;
            if (lastPos < pos)
                append(string, lastPos, pos - lastPos);
            lastPos = pos + 1;
            append(escape);
        }
        if (lastPos < pos)
            append(string, lastPos, pos - lastPos);
    }

    private void escapeAndAppendString(char[] buf, int start, int len)
            throws IOException {
        final char NE = (char) ESCAPE_TABLE.length;
        final String[] escapes = ESCAPE_TABLE;
        int end = start + len;
        int lastPos = start;
        int pos;
        for (pos = start; pos < end; pos++) {
            char c = buf[pos];
            if (c >= NE)
                continue;
            String escape = escapes[c];
            if (escape == null)
                continue;
            if (lastPos < pos)
                append(buf, lastPos, pos - lastPos);
            lastPos = pos + 1;
            append(escape);
        }
        if (lastPos < pos)
            append(buf, lastPos, pos - lastPos);
    }

    public XmlSerializer attribute(String namespace, String name, String value)
            throws IOException, IllegalArgumentException, IllegalStateException {
        // Modified by Pujiang, omit the null value
        if (value == null) {
            return this;
        }

        append(' ');
        if (namespace != null && !namespace.equals("")) {
            // Modified by Pujiang, NOT use the uri as namespace
            //if (namespace.startsWith("http")) {
            //String real = uri2Namespace.get(namespace);
            String real = nsMgr.getNsName(mDepth, namespace);

            if (real != null) {
                // Modified at 20150725
                append(real);
                append(':');
            }
            // 20171107: Otherwise add the namespace
            else {
                int pos = namespace.lastIndexOf('/');
                if (pos != -1 && pos != namespace.length() - 1) {
                    real = namespace.substring(pos + 1);
                } else {
                    real = "ns" + new Random(System.currentTimeMillis()).nextInt();
                }
                nsMgr.putNamespace(mDepth, real, namespace);
                append("xmlns:");
                append(real);
                append("=\"");
                append(namespace);
                append("\" ");
                append(real);
                append(':');
            }
        }
        append(name);
        append("=\"");

        // Modified at 20170303: already escaped
        //escapeAndAppendString(value);
        append(value);
        append('"');
        return this;
    }

    // The attributes will be sorted
    public XmlSerializer attributesToSort(String[] namespaces, String[] names, String[] values)
            throws IOException, IllegalArgumentException, IllegalStateException {

        Map<String, String> keyValues = new HashMap<String, String>();
        List<String> keyList = new ArrayList<String>();

        for (int i = 0; i < namespaces.length; i++) {
            String namespace = namespaces[i];
            String name = names[i];
            String value = values[i];

            String key = name;
            if (namespace != null && !namespace.equals("")) {
                //String real = uri2Namespace.get(namespace);
                String real = nsMgr.getNsName(mDepth, namespace);
                if (real != null) {
                    // Modified at 20150725
                    key = real + ":" + name;
                }
            }

            keyList.add(key);
            keyValues.put(key, value);
        }

        Collections.sort(keyList);

        for (int i = 0; i < keyList.size(); i++) {
            String key = keyList.get(i);
            String value = keyValues.get(key);
            append(' ');
            append(key);
            append("=\"");

            escapeAndAppendString(value);
            append('"');
        }

        return this;
    }

    public void cdsect(String text) throws IOException,
            IllegalArgumentException, IllegalStateException {
        throw new UnsupportedOperationException();
    }

    public void comment(String text) throws IOException,
            IllegalArgumentException, IllegalStateException {
        throw new UnsupportedOperationException();
    }

    public void docdecl(String text) throws IOException,
            IllegalArgumentException, IllegalStateException {
        throw new UnsupportedOperationException();
    }

    public void endDocument() throws IOException, IllegalArgumentException,
            IllegalStateException {
        flush();
        this.mOutputStream = null;
    }

    public XmlSerializer endTag(String namespace, String name)
            throws IOException, IllegalArgumentException, IllegalStateException {
        nsMgr.depthEnded(mDepth);
        mDepth--;
        if (mInTag) {
            append(" />\n");
        } else {
            for (int i = 0; i < mDepth; i++) {
                append('\t');
            }
            append("</");
            if (namespace != null) {
                String real = nsMgr.getNsName(mDepth, namespace);
                // Modified at 20150725
                if (real != null) {
                    append(real);
                    append(':');
                }
            }
            append(name);
            append(">\n");
        }
        mInTag = false;
        return this;
    }

    public void entityRef(String text) throws IOException,
            IllegalArgumentException, IllegalStateException {
        throw new UnsupportedOperationException();
    }

    private void flushBytes() throws IOException {
        int position;
        if ((position = mBytes.position()) > 0) {
            mBytes.flip();
            mOutputStream.write(mBytes.array(), 0, position);
            mBytes.clear();
        }
    }

    public void flush() throws IOException {
        // Log.i("PackageManager", "flush mPos=" + mPos);
        if (mPos > 0) {
            if (mOutputStream != null) {
                CharBuffer charBuffer = CharBuffer.wrap(mText, 0, mPos);
                CoderResult result = mCharset.encode(charBuffer, mBytes, true);
                while (true) {
                    if (result.isError()) {
                        throw new IOException(result.toString());
                    } else if (result.isOverflow()) {
                        flushBytes();
                        result = mCharset.encode(charBuffer, mBytes, true);
                        continue;
                    }
                    break;
                }
                flushBytes();
                mOutputStream.flush();
            } else {
                mWriter.write(mText, 0, mPos);
                mWriter.flush();
            }
            mPos = 0;
        }
    }

    public int getDepth() {
        throw new UnsupportedOperationException();
    }

    public boolean getFeature(String name) {
        throw new UnsupportedOperationException();
    }

    public String getName() {
        throw new UnsupportedOperationException();
    }

    public String getNamespace() {
        throw new UnsupportedOperationException();
    }

    public String getPrefix(String namespace, boolean generatePrefix)
            throws IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    public Object getProperty(String name) {
        throw new UnsupportedOperationException();
    }

    public void ignorableWhitespace(String text) throws IOException,
            IllegalArgumentException, IllegalStateException {
        throw new UnsupportedOperationException();
    }

    public void processingInstruction(String text) throws IOException,
            IllegalArgumentException, IllegalStateException {
        throw new UnsupportedOperationException();
    }

    public void setFeature(String name, boolean state)
            throws IllegalArgumentException, IllegalStateException {
        if (name.equals("http://xmlpull.org/v1/doc/features.html#indent-output")) {
            return;
        }
        throw new UnsupportedOperationException();
    }

    public void setOutput(OutputStream os, String encoding) throws IOException,
            IllegalArgumentException, IllegalStateException {
        if (os == null)
            throw new IllegalArgumentException();
        if (true) {
            try {
                mCharset = Charset.forName(encoding).newEncoder();
            } catch (IllegalCharsetNameException e) {
                throw (UnsupportedEncodingException) (new UnsupportedEncodingException(
                        encoding).initCause(e));
            } catch (UnsupportedCharsetException e) {
                throw (UnsupportedEncodingException) (new UnsupportedEncodingException(
                        encoding).initCause(e));
            }
            mOutputStream = os;
        } else {
            setOutput(encoding == null ? new OutputStreamWriter(os)
                    : new OutputStreamWriter(os, encoding));
        }
    }

    public void setOutput(Writer writer) throws IOException,
            IllegalArgumentException, IllegalStateException {
        mWriter = writer;
    }

    public void setPrefix(String prefix, String namespace) throws IOException,
            IllegalArgumentException, IllegalStateException {
        throw new UnsupportedOperationException();
    }

    public void setProperty(String name, Object value)
            throws IllegalArgumentException, IllegalStateException {
        throw new UnsupportedOperationException();
    }

    public void startDocument(String encoding, Boolean standalone)
            throws IOException, IllegalArgumentException, IllegalStateException {
        reset();
        if (standalone != null) {
            append("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\""
                    + (standalone ? "yes" : "no") + "\" ?>\n");
        } else {
            append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
        }
    }

    private void reset() {
        seenBracket = seenBracketBracket = false;
        nsMgr = new NamespaceManager();

        mDepth = 0;
        mPos = 0;
        mInTag = false;
    }

    public void writeStartTag(XmlPullParser pp) throws XmlPullParserException, IOException {
//        if (!pp.getFeature (XmlPullParser.FEATURE_REPORT_NAMESPACE_ATTRIBUTES)) {
//            int nsStart = pp.getNamespaceCount(pp.getDepth()-1);
//            int nsEnd = pp.getNamespaceCount(pp.getDepth());
//            for (int i = nsStart; i < nsEnd; i++) {
//                String prefix = pp.getNamespacePrefix(i);
//                String ns = pp.getNamespaceUri(i);
//                setPrefix(prefix, ns);
//            }
//        }
        startTag(pp.getNamespace(), pp.getName());

        int namespaceCountBefore = pp.getNamespaceCount(pp
                .getDepth() - 1);
        int namespaceCount = pp.getNamespaceCount(pp
                .getDepth());
        for (int i = namespaceCountBefore; i != namespaceCount; ++i) {
            String namespace = pp.getNamespacePrefix(i);
            String uri = pp.getNamespaceUri(i);
            if (!"".equals(namespace)) { // skip the empty namespace
                // uri2Namespace.put(uri, namespace);
                nsMgr.putNamespace(mDepth, namespace, uri);
                this.append(String.format(" xmlns:%s=\"%s\"", namespace, uri));
            }
            //Log.d("DEBUG", namespace + ": " + uri + " added");
        }

        //ser.closeStartTag();
    }

    public XmlSerializer startTag(String namespace, String name)
            throws IOException, IllegalArgumentException, IllegalStateException {
        if (mInTag) {
            append(">\n");
        }
        for (int i = 0; i < mDepth; i++) {
            append('\t');
        }
        append('<');
        if (namespace != null) {
            //String real = uri2Namespace.get(namespace);
            String real = nsMgr.getNsName(mDepth, namespace);
            if (real != null) {
                // modified at 20150725
                append(real);
                append(':');
            }
        }
        append(name);
        mInTag = true;
        mDepth++;
        return this;
    }

    public XmlSerializer text(char[] buf, int start, int len)
            throws IOException, IllegalArgumentException, IllegalStateException {
        if (mInTag) {
            append(">");
            mInTag = false;
        }
        //escapeAndAppendString(buf, start, len);
        seenBracket = seenBracketBracket = false;
        writeElementContent(buf, start, len);
        return this;
    }

    public XmlSerializer text(String text) throws IOException,
            IllegalArgumentException, IllegalStateException {
        if (mInTag) {
            append(">");
            mInTag = false;
        }
        //escapeAndAppendString(text);
        seenBracket = seenBracketBracket = false;
        writeElementContent(text);
        return this;
    }

    protected void writeElementContent(String text)
            throws IOException {
        // esccape '<', '&', ']]>', <32 if necessary
        int pos = 0;
        for (int i = 0; i < text.length(); i++) {
            // TODO: check if doing char[] text.getChars() would be faster than
            // getCharAt(i) ...
            char ch = text.charAt(i);
            if (ch == ']') {
                if (seenBracket) {
                    seenBracketBracket = true;
                } else {
                    seenBracket = true;
                }
            } else {
                if (ch == '&') {
                    if (!(i < text.length() - 3 && text.charAt(i + 1) == 'l'
                            && text.charAt(i + 2) == 't' && text.charAt(i + 3) == ';')) {
                        if (i > pos)
                            append(text.substring(pos, i));
                        append("&amp;");
                        pos = i + 1;
                    }
                } else if (ch == '<') {
                    if (i > pos)
                        append(text.substring(pos, i));
                    append("&lt;");
                    pos = i + 1;
                } else if (seenBracketBracket && ch == '>') {
                    if (i > pos)
                        append(text.substring(pos, i));
                    append("&gt;");
                    pos = i + 1;
                } else if (ch < 32) {
                    // in XML 1.0 only legal character are #x9 | #xA | #xD
                    if (ch == 9 || ch == 10 || ch == 13) {
                        // pass through

                        // } else if(ch == 13) { //escape
                        // if(i > pos) append(text.substring(pos, i));
                        // append("&#");
                        // append(Integer.toString(ch));
                        // append(';');
                        // pos = i + 1;
                    } else {
//						if (TRACE_ESCAPING)
//							System.err.println(getClass().getName()
//									+ " DEBUG TEXT value.len=" + text.length()
//									+ " " + printable(text));
//						throw new IllegalStateException("character "
//								+ Integer.toString(ch)
//								+ " is not allowed in output" + getLocation()
//								+ " (text value=" + printable(text) + ")");
                        // in XML 1.1 legal are [#x1-#xD7FF]
                        // if(ch > 0) {
                        // if(i > pos) append(text.substring(pos, i));
                        // append("&#");
                        // append(Integer.toString(ch));
                        // append(';');
                        // pos = i + 1;
                        // } else {
                        // throw new IllegalStateException(
                        // "character zero is not allowed in XML 1.1 output"+getLocation());
                        // }
                    }
                }
                if (seenBracket) {
                    seenBracketBracket = seenBracket = false;
                }

            }
        }
        if (pos > 0) {
            append(text.substring(pos));
        } else {
            append(text); // this is shortcut to the most common case
        }

    }

    protected void writeElementContent(char[] buf, int off, int len)
            throws IOException {
        // esccape '<', '&', ']]>'
        final int end = off + len;
        int pos = off;
        for (int i = off; i < end; i++) {
            final char ch = buf[i];
            if (ch == ']') {
                if (seenBracket) {
                    seenBracketBracket = true;
                } else {
                    seenBracket = true;
                }
            } else {
                if (ch == '&') {
                    if (i > pos) {
                        append(buf, pos, i - pos);
                    }
                    append("&amp;");
                    pos = i + 1;
                } else if (ch == '<') {
                    if (i > pos) {
                        append(buf, pos, i - pos);
                    }
                    append("&lt;");
                    pos = i + 1;

                } else if (seenBracketBracket && ch == '>') {
                    if (i > pos) {
                        append(buf, pos, i - pos);
                    }
                    append("&gt;");
                    pos = i + 1;
                } else if (ch < 32) {
                    // in XML 1.0 only legal character are #x9 | #xA | #xD
                    if (ch == 9 || ch == 10 || ch == 13) {
                        // pass through

                        // } else if(ch == 13 ) { //if(ch == '\r') {
                        // if(i > pos) {
                        // append(buf, pos, i - pos);
                        // }
                        // append("&#");
                        // append(Integer.toString(ch));
                        // append(';');
                        // pos = i + 1;
                    } else {
//						if (TRACE_ESCAPING)
//							System.err.println(getClass().getName()
//									+ " DEBUG TEXT value.len=" + len + " "
//									+ printable(new String(buf, off, len)));
//						throw new IllegalStateException("character "
//								+ printable(ch) + " (" + Integer.toString(ch)
//								+ ") is not allowed in output" + getLocation());
                        // in XML 1.1 legal are [#x1-#xD7FF]
                        // if(ch > 0) {
                        // if(i > pos) append(text.substring(pos, i));
                        // append("&#");
                        // append(Integer.toString(ch));
                        // append(';');
                        // pos = i + 1;
                        // } else {
                        // throw new IllegalStateException(
                        // "character zero is not allowed in XML 1.1 output"+getLocation());
                        // }
                    }
                }
                if (seenBracket) {
                    seenBracketBracket = seenBracket = false;
                }
                // assert seenBracketBracket == seenBracket == false;
            }
        }
        if (end > pos) {
            append(buf, pos, end - pos);
        }
    }


}