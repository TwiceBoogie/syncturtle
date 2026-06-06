package com.syncturtle.services.instance.service.configuration;

import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.services.instance.model.InstanceConfiguration;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class InstanceConfigurationCrypto {

    private final StringEncryptor encryptor;

    public String decryptIfNeeded(InstanceConfiguration row) {
        Assert.notNull(row, "configuration row is required");

        String storedValue = normalize(row.getValue());
        if (!row.isEncrypted()) {
            return storedValue;
        }

        if (!StringUtils.hasText(storedValue)) {
            return "";
        }

        return encryptor.decrypt(storedValue);
    }

    public String encryptIfNeeded(boolean encrypted, String rawValue) {
        String plainValue = normalize(rawValue);

        if (!encrypted) {
            return plainValue;
        }

        if (!StringUtils.hasText(plainValue)) {
            return "";
        }

        return encryptor.encrypt(plainValue);
    }

    public boolean wouldStoredValueChange(InstanceConfiguration row, String nextPlainValue) {
        Assert.notNull(row, "configuration row is required");

        String currentPlainValue = decryptIfNeeded(row);
        String normalizedNextPlainValue = normalize(nextPlainValue);

        return !currentPlainValue.equals(normalizedNextPlainValue);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
