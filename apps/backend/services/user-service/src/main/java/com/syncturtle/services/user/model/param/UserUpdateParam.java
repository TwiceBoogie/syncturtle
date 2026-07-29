package com.syncturtle.services.user.model.param;

import java.util.UUID;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class UserUpdateParam {

    private static final int MAX_FIRST_NAME_LENGTH = 80;
    private static final int MAX_LAST_NAME_LENGTH = 80;

    private final String firstName;
    private final String lastName;
    private final UUID avatarAssetId;

    @Builder
    private UserUpdateParam(
            String firstName,
            String lastName,
            UUID avatarAssetId) {
        this.firstName = normalizeNullable(firstName, "firstName", MAX_FIRST_NAME_LENGTH);
        this.lastName = normalizeNullable(lastName, "lastName", MAX_LAST_NAME_LENGTH);
        this.avatarAssetId = avatarAssetId;
    }

    private static String normalizeNullable(String value, String fieldName, int maxLength) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }

        Assert.isTrue(normalized.length() <= maxLength, fieldName + " must be " + maxLength + " characters or fewer");

        return normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }

}
