package com.syncturtle.services.user.model.param;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

import com.syncturtle.common.core.actor.PrincipalType;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class UserCreateParam {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 64;
    private static final int MAX_EMAIL_LENGTH = 320;
    private static final int MAX_DISPLAY_NAME_LENGTH = 120;
    private static final int MAX_NAME_LENGTH = 80;
    private static final int MAX_MOBILE_NUMBER_LENGTH = 32;
    private static final int MAX_PASSWORD_HASH_LENGTH = 255;
    private static final int MAX_TIMEZONE_LENGTH = 255;
    private static final int MAX_IP_LENGTH = 64;
    private static final int MAX_LOGIN_MEDIUM_LENGTH = 40;
    private static final int MAX_USER_AGENT_LENGTH = 2048;

    private static final String DEFAULT_TIMEZONE = "America/Chicago";
    private static final String DEFAULT_INTIAL_LOGIN_MEDIUM = "password";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern USERNAME_ALLOWED_CHARS = Pattern.compile("^[a-z0-9._-]+$");

    private final String username;
    private final String mobileNumber;
    private final String email;
    private final String displayName;
    private final String firstName;
    private final String lastName;
    private final String passwordHash;
    private final boolean passwordAutoset;
    private final String userTimezone;
    private final PrincipalType principalType;

    private final String initialLoginIp;
    private final String initialLoginMedium;
    private final String initialLoginUserAgent;

    @Builder
    private UserCreateParam(
            String username,
            String mobileNumber,
            String email,
            String displayName,
            String firstName,
            String lastName,
            String passwordHash,
            boolean passwordAutoset,
            String userTimezone,
            PrincipalType principalType,
            String initialLoginIp,
            String initialLoginMedium,
            String initialLoginUserAgent) {
        this.email = normalizeEmail(email);
        this.username = normalizeUsername(username);
        this.firstName = normalizeNullable(firstName, "firstName", MAX_NAME_LENGTH);
        this.lastName = normalizeNullable(lastName, "lastName", MAX_NAME_LENGTH);
        this.mobileNumber = normalizeNullable(mobileNumber, "mobileNumber", MAX_MOBILE_NUMBER_LENGTH);
        this.displayName = resolveDisplayName(displayName, this.firstName, this.lastName, this.email);
        this.passwordHash = normalizePasswordHash(passwordHash);
        this.passwordAutoset = passwordAutoset;
        this.userTimezone = normalizeTimezoneOrDefault(userTimezone);
        Assert.notNull(principalType, "principalType is required");
        this.principalType = principalType;

        InitialLoginAudit audit = normalizeInitialLoginAudit(initialLoginIp, initialLoginMedium, initialLoginUserAgent);

        this.initialLoginIp = audit.ipAddress;
        this.initialLoginMedium = audit.medium;
        this.initialLoginUserAgent = audit.userAgent;
    }

    public boolean hasInitialLoginAudit() {
        return initialLoginIp != null
                && initialLoginMedium != null
                && initialLoginUserAgent != null;
    }

    private static String normalizeEmail(String value) {
        String normalized = normalizeRequired(value, "email", MAX_EMAIL_LENGTH).toLowerCase(Locale.ROOT);

        Assert.isTrue(EMAIL_PATTERN.matcher(normalized).matches(), "email must be valid");

        return normalized;
    }

    private static String normalizeUsername(String value) {
        String normalized = normalizeRequired(value, "username", MAX_USERNAME_LENGTH);

        Assert.isTrue(normalized.length() >= MIN_USERNAME_LENGTH,
                "username must be at least " + MIN_USERNAME_LENGTH + " characters");

        Assert.isTrue(USERNAME_ALLOWED_CHARS.matcher(normalized).matches(),
                "username may only contain lowercase letters, numbers, dots, underscores, or hyphens");

        Assert.isTrue(Character.isLetterOrDigit(normalized.charAt(0)), "username must start with a letter or number");

        Assert.isTrue(Character.isLetterOrDigit(normalized.charAt(normalized.length() - 1)),
                "username must end with a letter or number");

        return normalized;
    }

    private static String normalizePasswordHash(String value) {
        String normalized = normalizeRequired(value, "passwordHash", MAX_PASSWORD_HASH_LENGTH);

        Assert.isTrue(normalized.length() >= 20, "passwordHash appears too short to be an encoded password");

        return normalized;
    }

    private static String resolveDisplayName(String displayName, String firstName, String lastName, String email) {
        String normalizedDisplayName = normalizeNullable(displayName, "displayName", MAX_DISPLAY_NAME_LENGTH);
        if (normalizedDisplayName != null) {
            return normalizedDisplayName;
        }

        String fullName = joinName(firstName, lastName);
        if (fullName != null) {
            return normalizeRequired(fullName, "displayName", MAX_DISPLAY_NAME_LENGTH);
        }

        int atIndex = email.indexOf('@');
        String fallback = atIndex > 0 ? email.substring(0, atIndex) : email;

        return normalizeRequired(fallback, "displayName", MAX_DISPLAY_NAME_LENGTH);
    }

    private static String joinName(String firstName, String lastName) {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }

        if (firstName != null) {
            return firstName;
        }

        if (lastName != null) {
            return lastName;
        }

        return null;
    }

    private static String normalizeTimezoneOrDefault(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            normalized = DEFAULT_TIMEZONE;
        }

        Assert.isTrue(normalized.length() <= MAX_TIMEZONE_LENGTH,
                "userTimezone must be " + MAX_TIMEZONE_LENGTH + " characters or fewer");

        try {
            ZoneId.of(normalized);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("userTimezone must be a valid IANA time zone", exception);
        }

        return normalized;
    }

    private static InitialLoginAudit normalizeInitialLoginAudit(String ipAddress, String medium, String userAgent) {
        boolean hasIpAddress = hasText(ipAddress);
        boolean hasMedium = hasText(medium);
        boolean hasUserAgent = hasText(userAgent);

        if (!hasIpAddress && !hasMedium && !hasUserAgent) {
            return InitialLoginAudit.empty();
        }

        /**
         * If a caller supplies one piece of ligin audit data, force the command
         * to be complete
         */
        Assert.isTrue(hasIpAddress, "initialLoginIp is required when initial login audit is provided");
        Assert.isTrue(hasUserAgent, "initialLoginUserAgent is required when initial login audit is provided");

        String normalizedMedium = hasMedium
                ? normalizeRequired(medium, "initialLoginMedium", MAX_LOGIN_MEDIUM_LENGTH)
                : DEFAULT_INTIAL_LOGIN_MEDIUM;

        return new InitialLoginAudit(
                normalizeRequired(ipAddress, "initialLoginIp", MAX_IP_LENGTH),
                normalizedMedium,
                normalizeRequired(initialUserAgentOrFallback(userAgent), "initialLoginUserAgent",
                        MAX_USER_AGENT_LENGTH));
    }

    private static String initialUserAgentOrFallback(String userAgent) {
        return userAgent;
    }

    private static String normalizeRequired(String value, String fieldName, int maxLength) {
        Assert.hasText(value, fieldName + " is required");

        String normalized = value.trim();

        Assert.isTrue(normalized.length() <= maxLength, fieldName + " must be " + maxLength + " characters or fewer");

        return normalized;
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

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static final class InitialLoginAudit {
        private final String ipAddress;
        private final String medium;
        private final String userAgent;

        private InitialLoginAudit(String ipAddress, String medium, String userAgent) {
            this.ipAddress = ipAddress;
            this.medium = medium;
            this.userAgent = userAgent;
        }

        private static InitialLoginAudit empty() {
            return new InitialLoginAudit(null, null, null);
        }
    }

}
