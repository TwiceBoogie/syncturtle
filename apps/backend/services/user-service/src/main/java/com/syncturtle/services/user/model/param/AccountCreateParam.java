package com.syncturtle.services.user.model.param;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.provider.AuthProvider;
import com.syncturtle.services.user.model.User;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class AccountCreateParam {

    private static final int MAX_PROVIDER_ACCOUNT_ID_LENGTH = 255;

    private final User user;
    private final AuthProvider provider;
    private final String providerAccountId;
    private final AccountConnectionParam connection;

    @Builder
    private AccountCreateParam(
            User user,
            AuthProvider provider,
            String providerAccountId,
            AccountConnectionParam connection) {
        Assert.notNull(user, "user is required");
        Assert.notNull(provider, "provider is required");
        Assert.notNull(connection, "account connection param is required");

        this.user = user;
        this.provider = provider;
        this.providerAccountId = normalizeProviderAccountId(providerAccountId);

        requireConnectionMatchesCreateIdentity(provider, this.providerAccountId, connection);
        this.connection = connection;
    }

    private static void requireConnectionMatchesCreateIdentity(AuthProvider provider, String providerAccountId,
            AccountConnectionParam connection) {
        Assert.isTrue(connection.getProvider() == provider, "connection provider must match account provider");

        Assert.isTrue(connection.getProviderAccountId().equals(providerAccountId),
                "connection providerAccountId must match account providerAccountId");
    }

    private static String normalizeProviderAccountId(String value) {
        return normalizeRequired(value, "providerAccountId", MAX_PROVIDER_ACCOUNT_ID_LENGTH);
    }

    private static String normalizeRequired(String value, String fieldName, int maxLength) {
        Assert.hasText(value, fieldName + " is required");

        String normalized = value.trim();

        Assert.isTrue(normalized.length() <= maxLength, fieldName + " must be " + maxLength + " characters or fewer");

        return normalized;
    }

}
