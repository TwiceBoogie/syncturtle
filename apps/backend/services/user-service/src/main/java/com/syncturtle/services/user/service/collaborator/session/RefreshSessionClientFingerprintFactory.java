package com.syncturtle.services.user.service.collaborator.session;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.services.user.configuration.property.RefreshSessionClientBindingProperties;

public final class RefreshSessionClientFingerprintFactory {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MAX_DEVICE_LABEL_LENGTH = 120;
    private static final int MAX_BINDING_COMPONENT_LENGTH = 512;

    private final SecretKeySpec hmacKey;

    public RefreshSessionClientFingerprintFactory(RefreshSessionClientBindingProperties properties) {
        Assert.notNull(properties, "client binding properties is required");

        this.hmacKey = new SecretKeySpec(properties.getHmacKey(), HMAC_ALGORITHM);
    }

    public RefreshSessionClientFingerprint create(String clientIp, String userAgent) {
        String canonicalIp = normalizeBindingComponent(clientIp);
        String canonicalUserAgent = normalizeBindingComponent(userAgent);
        String bindingInput = lengthPrefixed(canonicalIp) + lengthPrefixed(canonicalUserAgent);
        String bindingHash = hmacSha256(bindingInput);
        String deviceLabel = normalizeDeviceLabel(userAgent);

        return new RefreshSessionClientFingerprint(deviceLabel, bindingHash);
    }

    private String hmacSha256(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacKey);
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("refresh session client binding could not be computed", exception);
        }
    }

    private static String lengthPrefixed(String value) {
        return value.length() + ":" + value;
    }

    private static String normalizeBindingComponent(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String normalized = normalizeWhitespace(value).toLowerCase(Locale.ROOT);
        if (normalized.length() > MAX_BINDING_COMPONENT_LENGTH) {
            return normalized.substring(0, MAX_BINDING_COMPONENT_LENGTH);
        }
        return normalized;
    }

    private static String normalizeDeviceLabel(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return null;
        }

        String normalized = normalizeWhitespace(userAgent);
        if (normalized.length() > MAX_DEVICE_LABEL_LENGTH) {
            return normalized.substring(0, MAX_DEVICE_LABEL_LENGTH);
        }
        return normalized;
    }

    private static String normalizeWhitespace(String value) {
        StringBuilder normalized = new StringBuilder(value.length());
        boolean previousWhitespace = false;

        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isISOControl(current) || Character.isWhitespace(current)) {
                if (!previousWhitespace && normalized.length() > 0) {
                    normalized.append(' ');
                }
                previousWhitespace = true;
                continue;
            }

            normalized.append(current);
            previousWhitespace = false;
        }

        return normalized.toString().trim();
    }

}
