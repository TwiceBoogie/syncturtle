package com.syncturtle.services.user.service.authentication.workflow;

import org.springframework.util.Assert;

import com.syncturtle.services.user.type.CredentialProviderType;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class CredentialCompletionSpec {

    private final CredentialProviderType provider;
    private final CredentialUserDataSpec userData;
    private final String ipAddress;
    private final String userAgent;

    @Builder
    private CredentialCompletionSpec(
            CredentialProviderType provider,
            CredentialUserDataSpec userData,
            String ipAddress,
            String userAgent) {
        Assert.notNull(provider, "provider is required");
        Assert.notNull(userData, "userData is required");

        this.provider = provider;
        this.userData = userData;
        this.ipAddress = normalizeNullable(ipAddress);
        this.userAgent = normalizeNullable(userAgent);
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

}
