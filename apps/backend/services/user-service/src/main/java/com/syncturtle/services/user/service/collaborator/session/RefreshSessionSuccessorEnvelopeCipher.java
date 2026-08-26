package com.syncturtle.services.user.service.collaborator.session;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.util.Assert;

import com.syncturtle.services.user.configuration.property.RefreshSessionSuccessorEnvelopeProperties;
import com.syncturtle.services.user.exception.RefreshSessionSuccessorEnvelopeException;

public final class RefreshSessionSuccessorEnvelopeCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String ENVELOPE_VERSION = "v1";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec encryptionKey;
    private final SecureRandom secureRandom;

    public RefreshSessionSuccessorEnvelopeCipher(RefreshSessionSuccessorEnvelopeProperties properties,
            SecureRandom secureRandom) {
        Assert.notNull(properties, "successor envelope properties is required");
        Assert.notNull(secureRandom, "secureRandom is required");

        this.encryptionKey = new SecretKeySpec(properties.getEncryptionKey(), KEY_ALGORITHM);
        this.secureRandom = secureRandom;
    }

    public String encrypt(String refreshToken, String sessionId, long rotationCounter, Instant expiresAt) {
        Assert.hasText(refreshToken, "refreshToken is required");

        byte[] nonce = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonce);

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(associatedData(sessionId, rotationCounter, expiresAt));
            byte[] cipherText = cipher.doFinal(refreshToken.getBytes(StandardCharsets.UTF_8));

            return ENVELOPE_VERSION + "." + encode(nonce) + "." + encode(cipherText);
        } catch (GeneralSecurityException exception) {
            throw new RefreshSessionSuccessorEnvelopeException("failed to encrypt refresh successor", exception);
        }
    }

    public String decrypt(String envelope, String sessionId, long rotationCounter, Instant expiresAt) {
        Assert.hasText(envelope, "successor envelope is required");

        String[] parts = envelope.split("\\.", -1);
        if (parts.length != 3 || !ENVELOPE_VERSION.equals(parts[0])) {
            throw new RefreshSessionSuccessorEnvelopeException("unsupported refresh successor envelope");
        }

        try {
            byte[] nonce = Base64.getUrlDecoder().decode(parts[1]);
            byte[] cipherText = Base64.getUrlDecoder().decode(parts[2]);

            if (nonce.length != NONCE_BYTES || cipherText.length <= TAG_BITS / Byte.SIZE) {
                throw new RefreshSessionSuccessorEnvelopeException("malformed refresh successor envelope");
            }

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(associatedData(sessionId, rotationCounter, expiresAt));
            byte[] plaintext = cipher.doFinal(cipherText);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (RefreshSessionSuccessorEnvelopeException exception) {
            throw exception;
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new RefreshSessionSuccessorEnvelopeException("failed to decrypt refresh successor", exception);
        }
    }

    private static byte[] associatedData(String sessionId, long rotationCounter, Instant expiresAt) {
        Assert.hasText(sessionId, "sessionId is required");
        Assert.isTrue(rotationCounter > 0, "rotationCounter must be positive");
        Assert.notNull(expiresAt, "expiresAt is required");

        String value = sessionId + ":" + rotationCounter + ":" + expiresAt.toEpochMilli();
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

}
