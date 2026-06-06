package com.syncturtle.services.instance.model.param;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.core.actor.PrincipalType;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class UserReplicaParam {

    private static final long INITIAL_VERSION = 0L;

    private final UUID id;
    private final Long sourceVersion;

    private final String username;
    private final String email;
    private final String displayName;
    private final String firstName;
    private final String lastName;
    private final String lastLoginMedium;

    private final UUID avatarAssetId;
    private final UUID coverImageAssetId;

    private final boolean active;
    private final boolean emailVerified;
    private final boolean passwordAutoset;
    private final String userTimezone;
    private final PrincipalType principalType;
    private final Long authVersion;

    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;
    private final boolean deleteEvent;

    @Builder
    private UserReplicaParam(
            UUID id,
            Long sourceVersion,
            String username,
            String email,
            String displayName,
            String firstName,
            String lastName,
            String lastLoginMedium,
            UUID avatarAssetId,
            UUID coverImageAssetId,
            Boolean active,
            Boolean emailVerified,
            Boolean passwordAutoset,
            String userTimezone,
            PrincipalType principalType,
            Long authVersion,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            Boolean deleteEvent) {
        Assert.notNull(id, "id is required");
        Assert.notNull(sourceVersion, "sourceVersion is required");
        Assert.isTrue(sourceVersion >= INITIAL_VERSION, "sourceVersion must be greater than or equal to 0");

        Assert.hasText(username, "username is required");
        Assert.hasText(displayName, "displayName is required");
        Assert.notNull(active, "active is required");
        Assert.notNull(emailVerified, "emailVerified is required");
        Assert.notNull(passwordAutoset, "passwordAutoset is required");
        Assert.hasText(userTimezone, "userTimezone is required");
        Assert.notNull(principalType, "principalType is required");
        Assert.notNull(authVersion, "authVersion is required");
        Assert.isTrue(authVersion >= INITIAL_VERSION, "authVersion must be greater than or equal to 0");
        Assert.notNull(createdAt, "createdAt is required");
        Assert.notNull(updatedAt, "updatedAt is required");
        Assert.notNull(deleteEvent, "deleteEvent is required");

        this.id = id;
        this.sourceVersion = sourceVersion;

        this.username = username.trim();
        this.email = normalizeEmail(email);
        this.displayName = displayName.trim();
        this.firstName = StringUtils.hasText(firstName) ? firstName.trim() : null;
        this.lastName = StringUtils.hasText(lastName) ? lastName.trim() : null;
        this.lastLoginMedium = StringUtils.hasText(lastLoginMedium) ? lastLoginMedium.trim() : null;

        this.avatarAssetId = avatarAssetId;
        this.coverImageAssetId = coverImageAssetId;

        this.active = active;
        this.emailVerified = emailVerified;
        this.passwordAutoset = passwordAutoset;
        this.userTimezone = userTimezone;
        this.principalType = principalType;
        this.authVersion = authVersion;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.deleteEvent = deleteEvent;

        requireDeleteShape();
    }

    public boolean isStaleComparedTo(Long currentSourceVersion) {
        if (currentSourceVersion == null) {
            return false;
        }

        return sourceVersion < currentSourceVersion;
    }

    public boolean isSameVersionAs(Long currentSourceVersion) {
        return currentSourceVersion != null && sourceVersion.equals(currentSourceVersion);
    }

    private void requireDeleteShape() {
        if (!deleteEvent) {
            return;
        }

        Assert.isTrue(!active, "delete user replica param must have active=false");
    }

    private static String normalizeEmail(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

}
