package com.gmail.heagoo.apklib.sign;

import android.os.Build;
import android.util.Log;

import com.gmail.heagoo.apklib.asn1.Asn1Box;
import com.gmail.heagoo.apklib.asn1.Asn1IA5String;
import com.gmail.heagoo.apklib.asn1.Asn1Integer;
import com.gmail.heagoo.apklib.asn1.Asn1Null;
import com.gmail.heagoo.apklib.asn1.Asn1ObjectId;
import com.gmail.heagoo.apklib.asn1.Asn1OctetString;
import com.gmail.heagoo.apklib.asn1.Asn1PrintableString;
import com.gmail.heagoo.apklib.asn1.Asn1Sequence;
import com.gmail.heagoo.apklib.asn1.Asn1Set;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.security.DigestOutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Command line tool to sign JAR files (including APKs and OTA updates) in a way
 * compatible with the mincrypt verifier, using SHA1 and RSA keys.
 */
public class SignApk {
    private static final String CERT_SF_NAME = "META-INF/CERT.SF";
    private static final String CERT_RSA_NAME = "META-INF/CERT.RSA";

    private static final String OTACERT_NAME = "META-INF/com/android/otacert";

    private static final int LINE_LENGTH_LIMIT = 72;
    private static final byte[] LINE_SEPARATOR = new byte[]{13, 10};
    private static final byte[] VALUE_SEPARATOR = new byte[]{58, 32};
    private static final Attributes.Name NAME_ATTRIBUTE = new Attributes.Name(
            "Name");
    static String noCompressExt[] = {".jpg", ".jpeg", ".png", ".gif", ".wav",
            ".mp2", ".mp3", ".ogg", ".aac", ".mpg", ".mpeg", ".mid", ".midi",
            ".smf", ".jet", ".rtttl", ".imy", ".xmf", ".mp4", ".m4a", ".m4v",
            ".3gp", ".3gpp", ".3g2", ".3gpp2", ".amr", ".awb", ".wma", ".wmv"};
    static String noCompressExt2[] = {"res/xml"};
    private static int ALIGNMENT = 4;
    // Files matching this pattern are not copied to the output.
    private static Pattern stripPattern = Pattern
            .compile("^META-INF/(.*)[.](SF|RSA|DSA)$");

    @SuppressWarnings("unused")
    private static X509Certificate readPublicKey(File file) throws IOException,
            GeneralSecurityException {
        FileInputStream input = new FileInputStream(file);
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(input);
        } finally {
            input.close();
        }
    }

    private static X509Certificate readPublicKey(InputStream input)
            throws IOException, GeneralSecurityException {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(input);
        } finally {
            input.close();
        }
    }

    /**
     * Reads the password from stdin and returns it as a string.
     *
     * @param keyFile The file containing the private key. Used to prompt the user.
     */
    private static String readPassword(InputStream keyInput) {
        // TODO: use Console.readPassword() when it's available.
        System.out.print("Enter password for private key"
                + " (password will not be hidden): ");
        System.out.flush();
        BufferedReader stdin = new BufferedReader(new InputStreamReader(
                System.in));
        try {
            return stdin.readLine();
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Decrypt an encrypted PKCS 8 format private key.
     * <p>
     * Based on ghstark's post on Aug 6, 2006 at
     * http://forums.sun.com/thread.jspa?threadID=758133&messageID=4330949
     *
     * @param encryptedPrivateKey The raw data of the private key
     * @param keyFile             The file containing the private key
     */
    private static KeySpec decryptPrivateKey(byte[] encryptedPrivateKey,
                                             InputStream keyInput) throws GeneralSecurityException {
        EncryptedPrivateKeyInfo epkInfo;
        try {
            epkInfo = new EncryptedPrivateKeyInfo(encryptedPrivateKey);
        } catch (IOException ex) {
            // Probably not an encrypted key.
            return null;
        }

        char[] password = readPassword(keyInput).toCharArray();

        SecretKeyFactory skFactory = SecretKeyFactory.getInstance(epkInfo
                .getAlgName());
        Key key = skFactory.generateSecret(new PBEKeySpec(password));

        Cipher cipher = Cipher.getInstance(epkInfo.getAlgName());
        cipher.init(Cipher.DECRYPT_MODE, key, epkInfo.getAlgParameters());

        try {
            return epkInfo.getKeySpec(cipher);
        } catch (InvalidKeySpecException ex) {
            System.err.println("Password for private key may be bad.");
            throw ex;
        }
    }

    /**
     * Read a PKCS 8 format private key.
     */
    private static PrivateKey readPrivateKey(InputStream input)
            throws IOException, GeneralSecurityException {

        try {
            byte[] bytes = new byte[8 * 1024];
            int read = input.read(bytes);
            byte[] data = new byte[read];
            System.arraycopy(bytes, 0, data, 0, read);
            bytes = data;

            KeySpec spec = decryptPrivateKey(bytes, input);
            if (spec == null) {
                spec = new PKCS8EncodedKeySpec(bytes);
            }

            try {
                return KeyFactory.getInstance("RSA").generatePrivate(spec);
            } catch (InvalidKeySpecException ex) {
                return KeyFactory.getInstance("DSA").generatePrivate(spec);
            }
        } finally {
            input.close();
        }
    }

    /**
     * Add the SHA1 of every file to the manifest, creating it if necessary.
     */
    private static Manifest addDigestsToManifest(JarFile jar,
                                                 Map<String, String> repalces, Map<String, String> addedAssetFiles,
                                                 Set<String> deletedFiles) throws IOException,
            GeneralSecurityException {
        Manifest input = jar.getManifest();
        Manifest output = new Manifest();
        Attributes main = output.getMainAttributes();
        if (input != null) {
            main.putAll(input.getMainAttributes());
        } else {
            main.putValue("Manifest-Version", "1.0");
            main.putValue("Created-By", "1.0 (Android SignApk)");
        }

        MessageDigest md = MessageDigest.getInstance("SHA1");
        byte[] buffer = new byte[4096];
        int num;

        // We sort the input entries by name, and add them to the
        // output manifest in sorted order. We expect that the output
        // map will be deterministic.

        TreeMap<String, JarEntry> byName = new TreeMap<String, JarEntry>();

        for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
            JarEntry entry = e.nextElement();
            byName.put(entry.getName(), entry);
        }

        for (JarEntry entry : byName.values()) {
            String name = entry.getName();
            // Added 20160410: skip the removed entry
            if (deletedFiles != null && deletedFiles.contains(name)) {
                continue;
            }

            if (!entry.isDirectory()
                    && !name.equals(JarFile.MANIFEST_NAME)
                    && !name.equals(CERT_SF_NAME)
                    && !name.equals(CERT_RSA_NAME)
                    && !name.equals(OTACERT_NAME)
                    && (stripPattern == null || !stripPattern.matcher(name)
                    .matches())) {

                // Modified by Pujiang
                if (repalces.keySet().contains(name)) {
                    FileInputStream fis = new FileInputStream(
                            repalces.get(name));
                    while ((num = fis.read(buffer)) > 0) {
                        md.update(buffer, 0, num);
                    }
                    fis.close();
                } else {
                    InputStream data = jar.getInputStream(entry);
                    while ((num = data.read(buffer)) > 0) {
                        md.update(buffer, 0, num);
                    }
                }

                // It has bugs if the original digest is not SHA1-Digest
                // Attributes attr = null;
                // if (input != null)
                // attr = input.getAttributes(name);
                // attr = attr != null ? new Attributes(attr) : new
                // Attributes();
                // attr.putValue("SHA1-Digest", MyBase64.encode(md.digest()));

                Attributes attr = new Attributes();
                String digest = MyBase64.encode(md.digest());
                attr.putValue("SHA1-Digest", digest);
                output.getEntries().put(name, attr);
            }
        }

        // For new added files
        if (addedAssetFiles != null) {
            for (String name : addedAssetFiles.keySet()) {
                String filePath = addedAssetFiles.get(name);
                FileInputStream fis = new FileInputStream(filePath);
                while ((num = fis.read(buffer)) > 0) {
                    md.update(buffer, 0, num);
                }
                fis.close();

                Attributes attr = new Attributes();
                attr.putValue("SHA1-Digest", MyBase64.encode(md.digest()));
                output.getEntries().put(name, attr);
            }
        }

        return output;
    }

    /**
     * Add a copy of the public key to the archive; this should exactly match
     * one of the files in /system/etc/security/otacerts.zip on the device. (The
     * same cert can be extracted from the CERT.RSA file but this is much easier
     * to get at.)
     */
    private static void addOtacert(JarOutputStream outputJar,
                                   InputStream publicKeyInput, long timestamp, Manifest manifest)
            throws IOException, GeneralSecurityException {
        MessageDigest md = MessageDigest.getInstance("SHA1");

        JarEntry je = new JarEntry(OTACERT_NAME);
        je.setTime(timestamp);
        outputJar.putNextEntry(je);

        byte[] b = new byte[4096];
        int read;
        while ((read = publicKeyInput.read(b)) != -1) {
            outputJar.write(b, 0, read);
            md.update(b, 0, read);
        }

        Attributes attr = new Attributes();
        attr.putValue("SHA1-Digest", MyBase64.encode(md.digest()));
        manifest.getEntries().put(OTACERT_NAME, attr);
    }

    private static void writeEntry(OutputStream paramOutputStream,
                                   Attributes.Name paramName, String paramString,
                                   CharsetEncoder paramCharsetEncoder, ByteBuffer paramByteBuffer)
            throws IOException {
        String str = paramName.toString();
        //paramOutputStream.write(str.getBytes("US_ASCII"));
        paramOutputStream.write(str.getBytes());
        paramOutputStream.write(VALUE_SEPARATOR);
        paramCharsetEncoder.reset();
        paramByteBuffer.clear().limit(-2 + (LINE_LENGTH_LIMIT - str.length()));
        CharBuffer localCharBuffer = CharBuffer.wrap(paramString);
        while (true) {
            CoderResult localCoderResult = paramCharsetEncoder.encode(
                    localCharBuffer, paramByteBuffer, true);
            if (CoderResult.UNDERFLOW == localCoderResult)
                localCoderResult = paramCharsetEncoder.flush(paramByteBuffer);
            paramOutputStream.write(paramByteBuffer.array(),
                    paramByteBuffer.arrayOffset(), paramByteBuffer.position());
            paramOutputStream.write(LINE_SEPARATOR);
            if (CoderResult.UNDERFLOW == localCoderResult)
                return;
            paramOutputStream.write(32);
            paramByteBuffer.clear().limit(-1 + LINE_LENGTH_LIMIT);
        }
    }

    static void write(Manifest manifest, OutputStream outputstream)
            throws IOException {
        if (Build.VERSION.SDK_INT >= 18) {
            manifest.write(outputstream);
            return;
        }
        CharsetEncoder charsetencoder = Charset.forName("UTF-8").newEncoder();
        ByteBuffer bytebuffer = ByteBuffer.allocate(LINE_LENGTH_LIMIT);
        Attributes attributes = manifest.getMainAttributes();
        Attributes.Name versionName = Attributes.Name.MANIFEST_VERSION;
        String versionValue = attributes.getValue(versionName);
        if (versionValue == null) {
            versionName = Attributes.Name.SIGNATURE_VERSION;
            versionValue = attributes.getValue(versionName);
        }
        if (versionValue != null) {
            writeEntry(outputstream, versionName, versionValue, charsetencoder,
                    bytebuffer);
        }

        // Attributes ?
        Iterator<?> iterator2 = attributes.keySet().iterator();
        while (iterator2.hasNext()) {
            java.util.jar.Attributes.Name name2 = (java.util.jar.Attributes.Name) iterator2
                    .next();
            if (!name2.equals(Attributes.Name.SIGNATURE_VERSION))
                writeEntry(outputstream, name2, attributes.getValue(name2),
                        charsetencoder, bytebuffer);
        }
        outputstream.write("\r\n".getBytes());

        Iterator<Entry<String, Attributes>> it = manifest.getEntries()
                .entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Attributes> e = it.next();
            StringBuffer buffer = new StringBuffer("Name: ");
            String value = e.getKey();
            // Comment at 20161127
            //if (value != null) {
            //byte[] vb = value.getBytes("UTF8");
            //byte[] vb = value.getBytes();
            //value = new String(vb, 0, 0, vb.length);
            //}
            buffer.append(value);
            buffer.append("\r\n");
            make72Safe(buffer);
            outputstream.write(buffer.toString().getBytes());

            attributes = ((Attributes) e.getValue());
            iterator2 = attributes.keySet().iterator();
            while (iterator2.hasNext()) {
                java.util.jar.Attributes.Name name2 = (java.util.jar.Attributes.Name) iterator2
                        .next();
                if (!name2
                        .equals(java.util.jar.Attributes.Name.MANIFEST_VERSION))
                    writeEntry(outputstream, name2, attributes.getValue(name2),
                            charsetencoder, bytebuffer);
            }
            outputstream.write("\r\n".getBytes());
        }
    }

    static void make72Safe(StringBuffer line) {
        int length = line.length();
        if (length > 72) {
            int index = 70;
            while (index < length - 2) {
                line.insert(index, "\r\n ");
                index += 72;
                length += 3;
            }
        }
        return;
    }

    /**
     * Write a .SF file with a digest of the specified manifest.
     */
    private static void writeSignatureFile(Manifest manifest,
                                           SignatureOutputStream out) throws IOException,
            GeneralSecurityException {
        Manifest sf = new Manifest();
        Attributes main = sf.getMainAttributes();
        main.putValue("Signature-Version", "1.0");
        main.putValue("Created-By", "1.0 (Android SignApk)");

        MessageDigest md = MessageDigest.getInstance("SHA1");
        PrintStream print = new PrintStream(new DigestOutputStream(
                new ByteArrayOutputStream(), md), true, "UTF-8");

        // Digest of the entire manifest
        //manifest.write(new FileOutputStream("/sdcard/test.MF"));
        manifest.write(print);
        print.flush();
        String digest = MyBase64.encode(md.digest());
        main.putValue("SHA1-Digest-Manifest", digest);

        Map<String, Attributes> entries = manifest.getEntries();
        for (Map.Entry<String, Attributes> entry : entries.entrySet()) {
            // Digest of the manifest stanza for this entry.
            print.print("Name: " + entry.getKey() + "\r\n");
            for (Map.Entry<Object, Object> att : entry.getValue().entrySet()) {
                print.print(att.getKey() + ": " + att.getValue() + "\r\n");
            }
            print.print("\r\n");
            print.flush();

            Attributes sfAttr = new Attributes();
            sfAttr.putValue("SHA1-Digest", MyBase64.encode(md.digest()));
            sf.getEntries().put(entry.getKey(), sfAttr);
        }

        // out.write("Signature-Version: 1.0\r\n".getBytes());
        // out.write("Created-By: 1.0 (Android SignApk)\r\n".getBytes());
        // String digestStr = "SHA1-Digest-Manifest: " + digest + "\r\n";
        // out.write(digestStr.getBytes());

        // sf.write(out);
        write(sf, out);

        // A bug in the java.util.jar implementation of Android platforms
        // up to version 1.6 will cause a spurious IOException to be thrown
        // if the length of the signature file is a multiple of 1024 bytes.
        // As a workaround, add an extra CRLF in this case.
        // if ((out.size() % 1024) == 0) {
        // out.write('\r');
        // out.write('\n');
        // }
    }

    /**
     * Write a .RSA file with a digital signature.
     */
    // private static void writeSignatureBlock(Signature signature,
    // X509Certificate publicKey, OutputStream out) throws IOException,
    // GeneralSecurityException {
    // SignerInfo signerInfo = new SignerInfo(new X500Name(publicKey
    // .getIssuerX500Principal().getName()), publicKey
    // .getSerialNumber(), AlgorithmId.get("SHA1"), AlgorithmId
    // .get("RSA"), signature.sign());
    //
    // PKCS7 pkcs7 = new PKCS7(new AlgorithmId[] { AlgorithmId.get("SHA1") },
    // new ContentInfo(ContentInfo.DATA_OID, null),
    // new X509Certificate[] { publicKey },
    // new SignerInfo[] { signerInfo });
    //
    // pkcs7.encodeSignedData(out);
    // }
    private static void writeSignatureBlockM(Signature signature,
                                             X509Certificate publicKey, OutputStream out)
            throws SignatureException, IOException,
            CertificateEncodingException {
        // BigInteger serialNumber = new BigInteger(
        // "10623618503190643167");
        BigInteger serialNumber = publicKey.getSerialNumber();

        // Parse:
        // OID.1.2.840.113549.1.9.1=#1613616E64726F696440616E64726F69642E636F6D,
        // CN=Android, OU=Android, O=Android, L=Mountain View, ST=California,
        // C=US
        Map<String, String> issueInfo = new HashMap<String, String>();
        String issuer = publicKey.getIssuerDN().toString();
        String values[] = issuer.split(",");
        for (String v : values) {
            v = v.trim();
            String[] keyvalue = v.split("=");
            if (keyvalue.length == 2) {
                issueInfo.put(keyvalue[0], keyvalue[1]);
            }
        }

        String strC = issueInfo.get("C");
        String strST = issueInfo.get("ST");
        String strL = issueInfo.get("L");
        String strO = issueInfo.get("O");
        String strOU = issueInfo.get("OU");
        String strCN = issueInfo.get("CN");
        String strMail = issueInfo.get("OID.1.2.840.113549.1.9.1");
        if (strMail != null && strMail.startsWith("#")) {
            strMail = fromHex(strMail.substring(5));
        } else {
            strMail = issueInfo.get("EMAILADDRESS");
        }

        // Make sequence
        Asn1Sequence signerSequence = new Asn1Sequence();
        if (strC != null)
            signerSequence.add(new Asn1Set().add(new Asn1Sequence().add(
                    new Asn1ObjectId("2.5.4.6")).add(
                    new Asn1PrintableString(strC))));
        if (strST != null)
            signerSequence.add(new Asn1Set().add(new Asn1Sequence().add(
                    new Asn1ObjectId("2.5.4.8")).add(
                    new Asn1PrintableString(strST))));
        if (strL != null)
            signerSequence.add(new Asn1Set().add(new Asn1Sequence().add(
                    new Asn1ObjectId("2.5.4.7")).add(
                    new Asn1PrintableString(strL))));
        if (strO != null)
            signerSequence.add(new Asn1Set().add(new Asn1Sequence().add(
                    new Asn1ObjectId("2.5.4.10")).add(
                    new Asn1PrintableString(strO))));
        if (strOU != null)
            signerSequence.add(new Asn1Set().add(new Asn1Sequence().add(
                    new Asn1ObjectId("2.5.4.11")).add(
                    new Asn1PrintableString(strOU))));
        if (strCN != null)
            signerSequence.add(new Asn1Set().add(new Asn1Sequence().add(
                    new Asn1ObjectId("2.5.4.3")).add(
                    new Asn1PrintableString(strCN))));
        if (strMail != null)
            signerSequence.add(new Asn1Set().add(new Asn1Sequence().add(
                    new Asn1ObjectId("1.2.840.113549.1.9.1")).add(
                    new Asn1IA5String(strMail))));

        byte[] arrayOfByte = signature.sign();
        Asn1Set localAsn1Set = new Asn1Set();
        localAsn1Set.add(new Asn1Sequence()
                .add(new Asn1Integer(1))
                .add(new Asn1Sequence().add(signerSequence).add(
                        new Asn1Integer(serialNumber)))
                .add(new Asn1Sequence().add(new Asn1ObjectId("1.3.14.3.2.26"))
                        .add(new Asn1Null()))
                .add(new Asn1Sequence().add(
                        new Asn1ObjectId("1.2.840.113549.1.1.1")).add(
                        new Asn1Null())).add(new Asn1OctetString(arrayOfByte)));
        Asn1Sequence localAsn1Sequence1 = new Asn1Sequence();
        localAsn1Sequence1.add(new Asn1Integer(1));
        localAsn1Sequence1.add(new Asn1Set().add(new Asn1Sequence().add(
                new Asn1ObjectId("1.3.14.3.2.26")).add(new Asn1Null())));
        localAsn1Sequence1.add(new Asn1Sequence().add(new Asn1ObjectId(
                "1.2.840.113549.1.7.1")));
        localAsn1Sequence1.add(new Asn1Box(publicKey.getEncoded()));
        localAsn1Sequence1.add(localAsn1Set);
        Asn1Sequence localAsn1Sequence2 = new Asn1Sequence();
        localAsn1Sequence2.add(new Asn1ObjectId("1.2.840.113549.1.7.2"));
        localAsn1Sequence2.add(new Asn1Box(localAsn1Sequence1));
        localAsn1Sequence2.write(out);
    }

    private static String fromHex(String hexStr) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < hexStr.length() - 1; i += 2) {
            int high = hex2Int(hexStr.charAt(i));
            int low = hex2Int(hexStr.charAt(i + 1));
            int charVal = (high << 4) | low;
            sb.append((char) charVal);
        }

        return sb.toString();
    }

    private static int hex2Int(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        } else if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return 0;
    }

    private static void signWholeOutputFile(byte[] zipData,
                                            OutputStream outputStream, X509Certificate publicKey,
                                            PrivateKey privateKey) throws IOException, GeneralSecurityException {

        // For a zip with no archive comment, the
        // end-of-central-directory record will be 22 bytes long, so
        // we expect to find the EOCD marker 22 bytes from the end.
        if (zipData[zipData.length - 22] != 0x50
                || zipData[zipData.length - 21] != 0x4b
                || zipData[zipData.length - 20] != 0x05
                || zipData[zipData.length - 19] != 0x06) {
            throw new IllegalArgumentException(
                    "zip data already has an archive comment");
        }

        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initSign(privateKey);
        signature.update(zipData, 0, zipData.length - 2);

        ByteArrayOutputStream temp = new ByteArrayOutputStream();

        // put a readable message and a null char at the start of the
        // archive comment, so that tools that display the comment
        // (hopefully) show something sensible.
        // TODO: anything more useful we can put in this message?
        byte[] message = "signed by SignApk".getBytes("UTF-8");
        temp.write(message);
        temp.write(0);
        writeSignatureBlockM(signature, publicKey, temp);
        int total_size = temp.size() + 6;
        if (total_size > 0xffff) {
            throw new IllegalArgumentException(
                    "signature is too big for ZIP file comment");
        }
        // signature starts this many bytes from the end of the file
        int signature_start = total_size - message.length - 1;
        temp.write(signature_start & 0xff);
        temp.write((signature_start >> 8) & 0xff);
        // Why the 0xff bytes? In a zip file with no archive comment,
        // bytes [-6:-2] of the file are the little-endian offset from
        // the start of the file to the central directory. So for the
        // two high bytes to be 0xff 0xff, the archive would have to
        // be nearly 4GB in side. So it's unlikely that a real
        // commentless archive would have 0xffs here, and lets us tell
        // an old signed archive from a new one.
        temp.write(0xff);
        temp.write(0xff);
        temp.write(total_size & 0xff);
        temp.write((total_size >> 8) & 0xff);
        temp.flush();

        // Signature verification checks that the EOCD header is the
        // last such sequence in the file (to avoid minzip finding a
        // fake EOCD appended after the signature in its scan). The
        // odds of producing this sequence by chance are very low, but
        // let's catch it here if it does.
        byte[] b = temp.toByteArray();
        for (int i = 0; i < b.length - 3; ++i) {
            if (b[i] == 0x50 && b[i + 1] == 0x4b && b[i + 2] == 0x05
                    && b[i + 3] == 0x06) {
                throw new IllegalArgumentException(
                        "found spurious EOCD header at " + i);
            }
        }

        outputStream.write(zipData, 0, zipData.length - 2);
        outputStream.write(total_size & 0xff);
        outputStream.write((total_size >> 8) & 0xff);
        temp.writeTo(outputStream);
    }

    static boolean isNoCompressFileType(String entryName) {

        for (int i = 0; i < noCompressExt.length; i++) {
            if (entryName.endsWith(noCompressExt[i])) {
                Log.d("DEBUG", entryName + " not compress.");
                return true;
            }
        }
        return false;
    }

    /**
     * Copy all the files in a manifest from input to output. We set the
     * modification times in the output to a fixed time, so as to reduce
     * variation in the output file and make incremental OTAs more efficient.
     */
    // NOTE: this method will modify jarPath2FilePath
    private static void copyFiles(Manifest manifest, JarFile in,
                                  JarOutputStream out, CountingOutputStream outCount, long timestamp,
                                  Map<String, String> jarPath2FilePath,
                                  Map<String, String> addedAssetFiles, Set<String> deletedFiles)
            throws IOException {
        byte[] buffer = new byte[4096];
        int num;

        if (addedAssetFiles != null) {
            jarPath2FilePath.putAll(addedAssetFiles);
        }

        Map<String, Attributes> entries = manifest.getEntries();
        List<String> names = new ArrayList<String>(entries.keySet());
        Collections.sort(names);

        boolean firstEntry = true;
        for (String name : names) {
            long offset = outCount.getCount();

            if (firstEntry) {
                // The first entry in a jar file has an extra field of
                // four bytes that you can't get rid of; any extra
                // data you specify in the JarEntry is appended to
                // these forced four bytes. This is JAR_MAGIC in
                // JarOutputStream; the bytes are 0xfeca0000.
                firstEntry = false;
                offset += 4;
            }

            // The file will be replaced or added
            if (jarPath2FilePath.containsKey(name)) {
                String filePath = jarPath2FilePath.get(name);
                JarEntry outEntry = new JarEntry(name);
                outEntry.setTime(timestamp);
                if (isNoCompressFileType(name)) {
                    storeTheEntry(outEntry, filePath);

                    // This is equivalent to zipalign.
                    // offset += JarFile.LOCHDR + name.length();
                    offset += 30 + name.length();
                    // Log.d("DEBUG", "name: " + name + ", offset: " + offset);
                    int needed = (ALIGNMENT - (int) (offset % ALIGNMENT))
                            % ALIGNMENT;
                    if (needed != 0) {
                        outEntry.setExtra(new byte[needed]);
                    }
                }

                out.putNextEntry(outEntry);

                FileInputStream fileinput = new FileInputStream(filePath);
                while ((num = fileinput.read(buffer)) > 0) {
                    out.write(buffer, 0, num);
                }
                fileinput.close();
                out.closeEntry();
                out.flush();
                continue;
            }

            JarEntry inEntry = in.getJarEntry(name);
            JarEntry outEntry = null;
            // Here we make resources.arsc as compressed resources
            // if (inEntry.getMethod() == JarEntry.STORED &&
            // !name.equals("resources.arsc")) {
            if (inEntry.getMethod() == JarEntry.STORED) {
                // Preserve the STORED method of the input entry.
                outEntry = new JarEntry(inEntry);

                // This is equivalent to zipalign.
                // offset += JarFile.LOCHDR + name.length();
                offset += 30 + name.length();
                int needed = (ALIGNMENT - (int) (offset % ALIGNMENT))
                        % ALIGNMENT;
                if (needed != 0) {
                    outEntry.setExtra(new byte[needed]);
                } else {
                    outEntry.setExtra(null);
                }
            } else {
                // Create a new entry so that the compressed len is recomputed.
                outEntry = new JarEntry(name);
            }
            outEntry.setTime(timestamp);
            out.putNextEntry(outEntry);

            InputStream data = in.getInputStream(inEntry);
            while ((num = data.read(buffer)) > 0) {
                out.write(buffer, 0, num);
            }
            data.close();
            out.closeEntry();
            out.flush();
        }
    }

    // Resources in raw maybe cannot compressed, make it as STORE
    private static void storeTheEntry(JarEntry outEntry, String filePath)
            throws IOException {
        outEntry.setMethod(ZipEntry.STORED);
        File f = new File(filePath);
        outEntry.setSize((long) f.length());

        FileInputStream fis = null;
        try {
            CRC32 crc32 = new CRC32();
            crc32.reset();
            byte[] b = new byte[8192];
            int num = 0;

            fis = new FileInputStream(f);
            while ((num = fis.read(b)) > 0) {
                crc32.update(b, 0, num);
            }
            outEntry.setCrc(crc32.getValue());
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.err.println("Usage: signapk "
                    + "publickey.x509[.pem] privatekey.pk8 "
                    + "input.jar output.jar");
            System.exit(2);
        }

        FileInputStream publicKeyInput = new FileInputStream(args[0]);
        FileInputStream privateKeyInput = new FileInputStream(args[1]);
        signAPK(publicKeyInput, privateKeyInput, args[2], args[3], null, null,
                9);
    }

    // The simple version: no asset adding
    public static void signAPK(InputStream publicKeyInput,
                               InputStream privateKeyInput, String inputApkPath,
                               String outputApkPath, Map<String, String> jarPath2FilePath,
                               int level) {
        signAPK(publicKeyInput, privateKeyInput, inputApkPath, outputApkPath,
                jarPath2FilePath, null, level);
    }

    public static void signAPK(InputStream publicKeyInput,
                               InputStream privateKeyInput, String inputApkPath,
                               String outputApkPath, Map<String, String> jarPath2FilePath,
                               int level, boolean encrypted) {
        // Play tricks
        if (encrypted) {
            InputStream privateInput = new PrivateKeyTransformer(
                    privateKeyInput);
            signAPK(publicKeyInput, privateInput, inputApkPath, outputApkPath,
                    jarPath2FilePath, null, level);
        }
    }

    public static void signAPK(InputStream publicKeyInput,
                               InputStream privateKeyInput, String inputApkPath,
                               String outputApkPath, Map<String, String> jarPath2FilePath,
                               Map<String, String> addedAssetFiles, int level) {

        JarFile inputJar = null;
        JarOutputStream outputJar = null;
        FileOutputStream outputFile = null;

        try {
            X509Certificate publicKey = readPublicKey(publicKeyInput);

            // Assume the certificate is valid for at least an hour.
            long timestamp = publicKey.getNotBefore().getTime() + 3600L * 1000;

            PrivateKey privateKey = readPrivateKey(privateKeyInput);
            inputJar = new JarFile(new File(inputApkPath), false); // Don't
            // verify.

            OutputStream outputStream = null;
            outputStream = outputFile = new FileOutputStream(outputApkPath);
            CountingOutputStream outCount = new CountingOutputStream(outputFile);
            outputJar = new JarOutputStream(outCount);
            outputJar.setLevel(level);

            JarEntry je;

            Manifest manifest = addDigestsToManifest(inputJar,
                    jarPath2FilePath, addedAssetFiles, null);

            // Everything else
            copyFiles(manifest, inputJar, outputJar, outCount, timestamp,
                    jarPath2FilePath, addedAssetFiles, null);

            // MANIFEST.MF
            je = new JarEntry(JarFile.MANIFEST_NAME);
            je.setTime(timestamp);
            outputJar.putNextEntry(je);
            manifest.write(outputJar);

            // CERT.SF
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(privateKey);
            je = new JarEntry(CERT_SF_NAME);
            je.setTime(timestamp);
            outputJar.putNextEntry(je);
            writeSignatureFile(manifest, new SignatureOutputStream(outputJar,
                    signature));

            // CERT.RSA
            je = new JarEntry(CERT_RSA_NAME);
            je.setTime(timestamp);
            outputJar.putNextEntry(je);
            writeSignatureBlockM(signature, publicKey, outputJar);

            outputJar.close();
            outputJar = null;
            outputStream.flush();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputJar != null)
                    inputJar.close();
                if (outputFile != null)
                    outputFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // This function aims to remove the merge process
    // resourceApkPath: the resource need to be merged
    public static void signAPK(InputStream publicKeyInput,
                               InputStream privateKeyInput, String inputApkPath,
                               String outputApkPath, Map<String, String> addedFiles,
                               Set<String> deletedFiles, Map<String, String> replacedFiles,
                               int level) {

        JarFile inputJar = null;
        JarOutputStream outputJar = null;
        FileOutputStream outputFile = null;

        try {
            X509Certificate publicKey = readPublicKey(publicKeyInput);

            // Assume the certificate is valid for at least an hour.
            long timestamp = publicKey.getNotBefore().getTime() + 3600L * 1000;

            PrivateKey privateKey = readPrivateKey(privateKeyInput);
            inputJar = new JarFile(new File(inputApkPath), false); // Don't
            // verify.

            OutputStream outputStream = null;
            outputStream = outputFile = new FileOutputStream(outputApkPath);
            CountingOutputStream outCount = new CountingOutputStream(outputFile);
            outputJar = new JarOutputStream(outCount);
            outputJar.setLevel(level);

            JarEntry je;

            Manifest manifest = addDigestsToManifest(inputJar, replacedFiles,
                    addedFiles, deletedFiles);

            // Everything else
            copyFiles(manifest, inputJar, outputJar, outCount, timestamp,
                    replacedFiles, addedFiles, deletedFiles);

            // MANIFEST.MF
            je = new JarEntry(JarFile.MANIFEST_NAME);
            je.setTime(timestamp);
            outputJar.putNextEntry(je);
            manifest.write(outputJar);

            // CERT.SF
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(privateKey);
            je = new JarEntry(CERT_SF_NAME);
            je.setTime(timestamp);
            outputJar.putNextEntry(je);
            writeSignatureFile(manifest, new SignatureOutputStream(outputJar,
                    signature));

            // CERT.RSA
            je = new JarEntry(CERT_RSA_NAME);
            je.setTime(timestamp);
            outputJar.putNextEntry(je);
            writeSignatureBlockM(signature, publicKey, outputJar);

            outputJar.close();
            outputJar = null;
            outputStream.flush();

        } catch (Exception e) {
            e.printStackTrace();
            //     Log.d("sawsem", e.toString());
        } finally {
            try {
                if (inputJar != null)
                    inputJar.close();
                if (outputFile != null)
                    outputFile.close();
            } catch (IOException e) {
                e.printStackTrace();
                //              Log.d("sawsem", e.toString());
            }
        }
    }

    protected void test() {
        // java.security.KeyStore keyStoreFile = java.security.KeyStore
        // .getInstance("PKCS12");
        // keyStoreFile.load(new FileInputStream("keyStore.pfx"),
        // "password".toCharArray());
        // PrivateKey privateKey = (PrivateKey) keyStoreFile.getKey(
        // "alais", "password".toCharArray());
    }

    /**
     * Write to another stream and also feed it to the Signature object.
     */
    private static class SignatureOutputStream extends FilterOutputStream {
        private Signature mSignature;
        private int mCount;

        public SignatureOutputStream(OutputStream out, Signature sig) {
            super(out);
            mSignature = sig;
            mCount = 0;
        }

        @Override
        public void write(int b) throws IOException {
            try {
                mSignature.update((byte) b);
            } catch (SignatureException e) {
                throw new IOException("SignatureException: " + e);
            }
            super.write(b);
            mCount++;
        }
    }

    private static class CountingOutputStream extends OutputStream {
        private long mCount = 0;
        private OutputStream mOut;

        public CountingOutputStream(OutputStream out) {
            this.mOut = out;
        }

        /**
         * Returns the number of bytes written.
         */
        public long getCount() {
            return mCount;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            mOut.write(b, off, len);
            mCount += len;
        }

        @Override
        public void write(int b) throws IOException {
            mOut.write(b);
            mCount++;
        }

        @Override
        public void close() throws IOException {
            mOut.close();
        }

        @Override
        public void flush() throws IOException {
            mOut.flush();
        }
    }
}
