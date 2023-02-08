package com.gmail.heagoo.appdm.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SignatureInfoReader {
    public static String getSignature(String zipFilePath) {
        try {
            ZipFile zipFile = new ZipFile(zipFilePath);
            Enumeration<?> emu = zipFile.entries();
            while (emu.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) emu.nextElement();

                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName();
                if (name.endsWith(".RSA") || name.endsWith(".rsa") ||
                        name.endsWith(".DSA") || name.endsWith(".dsa")) {
                    //Log.d("DEBUG", "Entry: " + name);
                    InputStream input = zipFile.getInputStream(entry);
                    try {
                        X509Certificate cert = readSignatureBlock(input);
                        return getCertInfo(cert);
                    } catch (Exception e) {

                    } finally {
                        input.close();
                    }
                }
            }
            zipFile.close();
        } catch (Exception e) {
            e.printStackTrace();
            // Log.d("DEBUG", "Error: " + e.getMessage());
        }

        return null;
    }

    public static X509Certificate readSignatureBlock(InputStream in)
            throws IOException, GeneralSecurityException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory
                .generateCertificate(in);

        return cert;
    }

    public static String getCertInfo(X509Certificate cert)
            throws FileNotFoundException, IOException, GeneralSecurityException {
        // Log.d("DEBUG", "issuer: " + cert.getIssuerDN());
        Principal p = cert.getSubjectDN();

        // Log.d("DEBUG", "subject: " + p);
        return SignatureInfo.makeReadable(p.toString());
    }

    public static class SignatureInfo {
        public String country; // C
        public String organization; // (organizationName, O),
        public String orgUnit; // (organizationalUnitName, OU),
        // distinguished name qualifier (dnQualifier),
        public String state; // state or province name (stateOrProvinceName,
        // ST),
        public String commonName; // (commonName, CN)
        // serial number (serialNumber)

        public String locality; // (locality, L),
        // title (title),
        public String surname; // (surName, SN),
        public String givenName; // (givenName, GN),

        // initials (initials),
        // pseudonym (pseudonym) and
        // generation qualifier (generationQualifier).

        public SignatureInfo(String info) {
            String segments[] = info.split(",");
            for (String seg : segments) {
                seg = seg.trim();
                String words[] = seg.split("=");
                if (words.length == 2) {
                    String key = words[0];
                    String value = words[1];
                    setKeyValue(key, value);
                }
            }
        }

        public static String makeReadable(String str) {
            return str.replace("C=", "Country=")
                    .replaceAll("O=", "Organization=")
                    .replaceAll("OU=", "Organization Unit=")
                    .replaceAll("ST=", "State/Province=")
                    .replaceAll("CN=", "Common Name=")
                    .replaceAll("L=", "Locality=")
                    .replaceAll("SN=", "Surname=")
                    .replaceAll("GN=", "Given Name=");
        }

        private void setKeyValue(String key, String value) {
            if ("C".equals(key)) {
                this.country = value;
            } else if ("O".equals(key)) {
                this.organization = value;
            } else if ("OU".equals(key)) {
                this.orgUnit = value;
            } else if ("ST".equals(key)) {
                this.state = value;
            } else if ("CN".equals(key)) {
                this.commonName = value;
            } else if ("L".equals(key)) {
                this.locality = value;
            } else if ("SN".equals(key)) {
                this.surname = value;
            } else if ("GN".equals(key)) {
                this.givenName = value;
            }
        }
    }
}
