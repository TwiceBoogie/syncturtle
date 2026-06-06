package com.syncturtle.services.user.service.authentication.workflow;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class CredentialUserDataSpec {

    private final String email;
    private final String firstName;
    private final String lastName;
    private final String displayName;
    private final String avatarUrl;
    private final boolean passwordAutoset;
    private final String rawPassword;

    @Builder
    private CredentialUserDataSpec(
            String email,
            String firstName,
            String lastName,
            String displayName,
            String avatarUrl,
            boolean passwordAutoset,
            String rawPassword) {
        Assert.hasText(email, "email is required");

        if (!passwordAutoset) {
            Assert.hasText(rawPassword, "rawPassword is required when password is not autoset");
        }

        this.email = email.trim().toLowerCase();
        this.firstName = normalizeNullable(firstName);
        this.lastName = normalizeNullable(lastName);
        this.displayName = normalizeNullable(displayName);
        this.avatarUrl = normalizeNullable(avatarUrl);
        this.passwordAutoset = passwordAutoset;
        this.rawPassword = rawPassword;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

}
