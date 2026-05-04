package com.syncturtle.services.instance.services.configuration;

import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.syncturtle.services.instance.models.InstanceConfiguration;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class InstanceConfigurationCrypto {

    private final StringEncryptor encryptor;

    public String decryptIfNeeded(InstanceConfiguration row) {
        String v = row.getValue();
        if (!row.isEncrypted()) {
            return v == null ? "" : v;
        }
        return StringUtils.hasText(v) ? encryptor.decrypt(v) : "";
    }

    public String encryptIfNeeded(boolean encrypted, String raw) {
        String v = raw == null ? "" : raw;
        if (!encrypted) {
            return v;
        }
        return StringUtils.hasText(v) ? encryptor.encrypt(v) : "";
    }
}
