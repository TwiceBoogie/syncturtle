package com.syncturtle.common.security.provenance;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.EdECKey;
import java.security.spec.NamedParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

public final class RequestProvenanceKeyLoader {

    private static final int MAX_KEY_BYTES = 16 * 1024;
    private static final byte[] PAIR_CHECK_MESSAGE = "syncturtle-request-provenance-key-check"
            .getBytes(StandardCharsets.US_ASCII);
    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();

    public RequestProvenanceSigningKey loadSigningKey(Resource privateKeyResource, Resource publicKeyResource) {
        PrivateKey privateKey = loadPrivateKey(privateKeyResource);
        PublicKey publicKey = loadPublicKey(publicKeyResource);
        verifyPair(privateKey, publicKey);

        return new RequestProvenanceSigningKey(keyId(publicKey), privateKey, publicKey);
    }

    public PublicKey loadPublicKey(Resource resource) {
        byte[] encoded = readEncodedKey(resource, "PUBLIC KEY");
        try {
            PublicKey key = KeyFactory.getInstance(RequestProvenanceProtocol.ALGORITHM)
                    .generatePublic(new X509EncodedKeySpec(encoded));
            requireEd25519(key, "public");

            return key;
        } catch (GeneralSecurityException exception) {
            throw configuration("request provenance public key is malformed", exception);
        }
    }

    public PrivateKey loadPrivateKey(Resource resource) {
        byte[] encoded = readEncodedKey(resource, "PRIVATE KEY");
        try {
            PrivateKey key = KeyFactory.getInstance(RequestProvenanceProtocol.ALGORITHM)
                    .generatePrivate(new PKCS8EncodedKeySpec(encoded));
            requireEd25519(key, "private");

            return key;
        } catch (GeneralSecurityException exception) {
            throw configuration("request provenance private key is malformed", exception);
        }
    }

    public String keyId(PublicKey publicKey) {
        Assert.notNull(publicKey, "publicKey is required");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(publicKey.getEncoded());
            return B64URL.encodeToString(Arrays.copyOf(digest, 16));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void verifyPair(PrivateKey privateKey, PublicKey publicKey) {
        try {
            Signature signer = Signature.getInstance(RequestProvenanceProtocol.ALGORITHM);
            signer.initSign(privateKey);
            signer.update(PAIR_CHECK_MESSAGE);
            byte[] signature = signer.sign();

            Signature verifier = Signature.getInstance(RequestProvenanceProtocol.ALGORITHM);
            verifier.initVerify(publicKey);
            verifier.update(PAIR_CHECK_MESSAGE);

            if (!verifier.verify(signature)) {
                throw configuration("request provenance private and public keys do not match");
            }
        } catch (GeneralSecurityException exception) {
            throw configuration("request provenance key pair validation failed", exception);
        }
    }

    private static void requireEd25519(Key key, String keyType) {
        if (!(key instanceof EdECKey edECKey)
                || !NamedParameterSpec.ED25519.getName().equals(edECKey.getParams().getName())) {
            throw configuration("request provenance " + keyType + " key algorithm is invalid");
        }
    }

    private static byte[] readEncodedKey(Resource resource, String pemLabel) {
        if (resource == null || !resource.exists() || !resource.isReadable()) {
            throw configuration("request provenance key resource is missing or unreadable");
        }

        byte[] bytes;
        try (InputStream input = resource.getInputStream()) {
            bytes = input.readNBytes(MAX_KEY_BYTES + 1);
        } catch (IOException exception) {
            throw configuration("request provenance key resource cannot be read", exception);
        }

        if (bytes.length == 0 || bytes.length > MAX_KEY_BYTES) {
            throw configuration("request provenance key resource has invalid bounds");
        }

        String text = new String(bytes, StandardCharsets.US_ASCII).trim();
        if (!text.startsWith("-----BEGIN ")) {
            return bytes;
        }

        String begin = "-----BEGIN " + pemLabel + "-----";
        String end = "-----END " + pemLabel + "-----";
        if (!text.startsWith(begin) || !text.endsWith(end)) {
            throw configuration("request provenance PEM label is invalid");
        }

        String body = text.substring(begin.length(), text.length() - end.length()).replaceAll("\\s", "");
        try {
            return Base64.getDecoder().decode(body);
        } catch (IllegalArgumentException exception) {
            throw configuration("request provenance PEM body is malformed", exception);
        }
    }

    private static IllegalStateException configuration(String message) {
        return new IllegalStateException(message);
    }

    private static IllegalStateException configuration(String message, Throwable cause) {
        return new IllegalStateException(message, cause);
    }

}
