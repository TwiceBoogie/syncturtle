package com.syncturtle.services.user.model.param;

import java.time.Instant;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.provider.AuthProvider;

import lombok.Builder;
import lombok.Getter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

@Getter
public final class AccountConnectionParam {

    private static final int MAX_PROVIDER_ACCOUNT_ID_LENGTH = 255;
    private static final int MAX_TOKEN_LENGTH = 16_384;

    private final AuthProvider provider;
    private final String providerAccountId;
    private final String accessToken;
    private final Instant accessTokenExpiresAt;
    private final String refreshToken;
    private final Instant refreshTokenExpiresAt;
    private final String idToken;
    private final JsonNode metadata;

    @Builder
    private AccountConnectionParam(
            AuthProvider provider,
            String providerAccountId,
            String accessToken,
            Instant accessTokenExpiresAt,
            String refreshToken,
            Instant refreshTokenExpiresAt,
            String idToken,
            JsonNode metadata) {
        Assert.notNull(provider, "provider is required");

        this.provider = provider;
        this.providerAccountId = normalizeProviderAccountId(providerAccountId);
        this.accessToken = normalizeRequiredToken(accessToken, "accessToken");
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshToken = normalizeNullableToken(refreshToken, "refreshToken");
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        this.idToken = normalizedIdToken(idToken);
        this.metadata = normalizeMetadata(metadata);

        requireRefreshTokenExpirationMatchesRefreshToken(this.refreshToken, this.refreshTokenExpiresAt);
    }

    public boolean hasRefreshToken() {
        return refreshToken != null;
    }

    public boolean hasAccessTokenExpiration() {
        return accessTokenExpiresAt != null;
    }

    public boolean hasRefreshTokenExpiration() {
        return refreshTokenExpiresAt != null;
    }

    private static void requireRefreshTokenExpirationMatchesRefreshToken(String refreshToken,
            Instant refreshTokenExpiresAt) {
        if (refreshToken == null) {
            Assert.isTrue(refreshTokenExpiresAt == null,
                    "refreshTokenExpiresAt cannot be provided without refreshToken");
        }
    }

    private static String normalizeProviderAccountId(String value) {
        return normalizeRequired(value, "providerAccountId", MAX_PROVIDER_ACCOUNT_ID_LENGTH);
    }

    private static String normalizeRequiredToken(String value, String fieldName) {
        return normalizeRequired(value, fieldName, MAX_TOKEN_LENGTH);
    }

    private static String normalizeNullableToken(String value, String fieldName) {
        return normalizeNullable(value, fieldName, MAX_TOKEN_LENGTH);
    }

    private static String normalizedIdToken(String value) {
        String normalized = normalizeNullable(value, "idToken", MAX_TOKEN_LENGTH);

        if (normalized == null) {
            return "";
        }

        return normalized;
    }

    private static JsonNode normalizeMetadata(JsonNode value) {
        if (value == null || value.isNull()) {
            return JsonNodeFactory.instance.objectNode();
        }

        Assert.isTrue(value.isObject(), "metadata must be a JSON object");

        return value;
    }

    private static String normalizeRequired(String value, String fieldName, int maxLength) {
        Assert.hasText(value, fieldName + " is required");

        String normalized = value.trim();

        Assert.isTrue(normalized.length() <= maxLength, fieldName + " must be " + maxLength + " characters or fewer");

        return normalized;
    }

    private static String normalizeNullable(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        Assert.isTrue(normalized.length() <= maxLength, fieldName + " must be " + maxLength + " characters or fewer");
        ;

        return normalized;
    }

}
