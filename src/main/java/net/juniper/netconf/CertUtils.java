package net.juniper.netconf;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import net.juniper.netconf.exception.NetconfCertificateException;

/**
 * Utility class to deal with parsing of keys.
 * <br>Reading of a private key based on https://stackoverflow.com/a/55339208/359394
 */
public final class CertUtils {

    private static final String PKCS_1_PEM_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String PKCS_1_PEM_FOOTER = "-----END RSA PRIVATE KEY-----";
    private static final String PKCS_8_PEM_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PKCS_8_PEM_FOOTER = "-----END PRIVATE KEY-----";

    /**
     * Generates a KeyPair from a PKCS#1 or PKCS#8 encoded private key.
     * <br>Generation of public key from the private key based on https://stackoverflow.com/a/12813708/359394
     *
     * @param pkcsEncodedPrivateKey the Base 64 encoded private key.
     * @return A KeyPair from the encoded private key.
     * @throws NetconfCertificateException if the supplied key could not be decoded.
     */
    public static KeyPair getGetPair(final String pkcsEncodedPrivateKey)
        throws NetconfCertificateException {
        try {
            final PrivateKey privateKey = decodePrivateKey(pkcsEncodedPrivateKey);

            final RSAPrivateCrtKey privateCertKey = (RSAPrivateCrtKey) privateKey;
            final RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
                privateCertKey.getModulus(),
                privateCertKey.getPublicExponent()
            );

            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            final PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
            return new KeyPair(publicKey, privateKey);
        } catch (final GeneralSecurityException e) {
            throw new NetconfCertificateException("Unable to decode supplied certificate", e);
        }
    }

    /**
     * Generates a private key from a PKCS#1 or PKCS#8 encoded private key.
     *
     * @param pkcsEncodedPrivateKey the Base 64 encoded private key.
     * @return a decoded PrivateKey from the encoded key.
     * @throws GeneralSecurityException if the certificate could not be decoded.
     */
    private static PrivateKey decodePrivateKey(final String pkcsEncodedPrivateKey)
        throws GeneralSecurityException {

        if (pkcsEncodedPrivateKey.contains(PKCS_1_PEM_HEADER)) {
            // OpenSSL / PKCS#1 Base64 PEM encoded file
            final String rawKey = pkcsEncodedPrivateKey
                .replace(PKCS_1_PEM_HEADER, "")
                .replace(PKCS_1_PEM_FOOTER, "")
                .replaceAll("\\s", "");
            return readPkcs1PrivateKey(Base64.getDecoder().decode(rawKey));
        }

        if (pkcsEncodedPrivateKey.contains(PKCS_8_PEM_HEADER)) {
            // PKCS#8 Base64 PEM encoded file
            final String rawKey = pkcsEncodedPrivateKey
                .replace(PKCS_8_PEM_HEADER, "")
                .replace(PKCS_8_PEM_FOOTER, "")
                .replaceAll("\\s", "");
            return readPkcs8PrivateKey(Base64.getDecoder().decode(rawKey));
        }

        // We assume it's a PKCS#8 DER encoded binary file
        return readPkcs8PrivateKey(pkcsEncodedPrivateKey.getBytes(StandardCharsets.UTF_8));
    }

    private static PrivateKey readPkcs8PrivateKey(final byte[] pkcs8Bytes)
        throws GeneralSecurityException {
        final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
        return keyFactory.generatePrivate(keySpec);
    }

    private static PrivateKey readPkcs1PrivateKey(final byte[] pkcs1Bytes)
        throws GeneralSecurityException {
        // We can't use Java internal APIs to parse ASN.1 structures,
        // so build a PKCS#8 key Java can understand
        final int pkcs1Length = pkcs1Bytes.length;
        final int totalLength = pkcs1Length + 22;
        final byte[] pkcs8Header = new byte[] {
            0x30, (byte) 0x82, (byte) ((totalLength >> 8) & 0xff), (byte) (totalLength & 0xff),
            // Sequence + total length
            0x2, 0x1, 0x0, // Integer (0)
            0x30, 0xD, 0x6, 0x9, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0xD, 0x1, 0x1,
            0x1, 0x5, 0x0, // Sequence: 1.2.840.113549.1.1.1, NULL
            0x4, (byte) 0x82, (byte) ((pkcs1Length >> 8) & 0xff), (byte) (pkcs1Length & 0xff)
            // Octet string + length
        };
        final byte[] pkcs8bytes = join(pkcs8Header, pkcs1Bytes);
        return readPkcs8PrivateKey(pkcs8bytes);
    }

    private static byte[] join(final byte[] byteArray1, final byte[] byteArray2) {
        final byte[] bytes = new byte[byteArray1.length + byteArray2.length];
        System.arraycopy(byteArray1, 0, bytes, 0, byteArray1.length);
        System.arraycopy(byteArray2, 0, bytes, byteArray1.length, byteArray2.length);
        return bytes;
    }

}
