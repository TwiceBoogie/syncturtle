package com.syncturtle.services.workspace.model.param;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class WorkspaceCreateParam {

    private static final int MAX_NAME_LENGTH = 80;
    private static final int MAX_SLUG_LENGTH = 48;
    private static final int MAX_ORGANIZATION_SIZE_LENGTH = 20;
    private static final int MAX_TIMEZONE_LENGTH = 255;

    private static final String DEFAULT_TIMEZONE = "America/Chicago";

    private final String name;
    private final String slug;
    private final String organizationSize;
    private final UUID ownerId;
    private final String timezone;

    @Builder
    private WorkspaceCreateParam(
            String name,
            String slug,
            String organizationSize,
            UUID ownerId,
            String timezone) {
        Assert.notNull(ownerId, "ownerId is required");

        this.name = normalizeRequired(name, "name", MAX_NAME_LENGTH);
        this.slug = normalizeRequired(slug, "slug", MAX_SLUG_LENGTH);
        this.organizationSize = normalizeRequired(organizationSize, "organizationSize", MAX_ORGANIZATION_SIZE_LENGTH);
        this.ownerId = ownerId;
        this.timezone = normalizeTimezoneOrDefault(timezone);
    }

    private static String normalizeRequired(String value, String fieldName, int maxLength) {
        Assert.hasText(value, fieldName + " is required");

        String normalized = value.trim();

        Assert.isTrue(normalized.length() <= maxLength, fieldName + " must be " + maxLength + " characters or fewer");

        return normalized;
    }

    private static String normalizeTimezoneOrDefault(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            normalized = DEFAULT_TIMEZONE;
        }

        Assert.isTrue(normalized.length() <= MAX_TIMEZONE_LENGTH,
                "timezone must be " + MAX_TIMEZONE_LENGTH + " characters or fewer");

        try {
            ZoneId.of(normalized);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("timezone must be a valid IANA time zone", exception);
        }

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
