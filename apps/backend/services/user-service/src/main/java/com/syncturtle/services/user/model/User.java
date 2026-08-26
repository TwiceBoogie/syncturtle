package com.syncturtle.services.user.model;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.util.Assert;

import com.syncturtle.common.core.actor.PrincipalType;
import com.syncturtle.common.data.jpa.entity.AuditedEntity;
import com.syncturtle.services.user.model.param.UserCreateParam;
import com.syncturtle.services.user.model.param.UserUpdateParam;
import com.syncturtle.services.user.model.support.ValidTimeZone;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Access(AccessType.FIELD)
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AuditedEntity {

    private static final int MAX_IP_LENGTH = 64;
    private static final int MAX_LOGIN_MEDIUM_LENGTH = 40;
    private static final int MAX_USER_AGENT_LENGTH = 2048;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(name = "mobile_number", length = 32)
    private String mobileNumber;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "first_name", length = 80)
    private String firstName;

    @Column(name = "last_name", length = 80)
    private String lastName;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "avatar_asset_id")
    private UUID avatarAssetId;

    @Column(name = "cover_image_asset_id")
    private UUID coverImageAssetId;

    @Column(name = "is_managed", nullable = false)
    private boolean managed;

    @Column(name = "is_password_expired", nullable = false)
    private boolean passwordExpired;

    @Column(name = "is_active", nullable = false)
    private boolean activated;

    @Column(name = "is_email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "is_password_autoset", nullable = false)
    private boolean passwordAutoset;

    @ValidTimeZone
    @Column(name = "user_timezone", nullable = false, length = 255)
    private String userTimezone;

    @Enumerated(EnumType.STRING)
    @Column(name = "principal_type", nullable = false)
    private PrincipalType principalType;

    @Column(name = "last_active")
    private Instant lastActive;

    @Column(name = "last_login_time")
    private Instant lastLoginTime;

    @Column(name = "last_login_ip", length = 64)
    private String lastLoginIp;

    @Column(name = "last_login_medium", length = 40)
    private String lastLoginMedium;

    @Column(name = "last_login_uagent", columnDefinition = "text")
    private String lastLoginUagent;

    @Column(name = "is_email_valid", nullable = false)
    private boolean emailValid;

    @Column(name = "masked_at")
    private Instant maskedAt;

    @Column(name = "auth_version", nullable = false)
    private Long authVersion;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static User create(UserCreateParam param, Clock clock) {
        Assert.notNull(param, "user create param is required");
        Assert.notNull(clock, "clock is required");

        User user = new User();
        user.initializeForCreate(param, clock);
        return user;
    }

    public void update(UserUpdateParam param) {
        this.firstName = param.getFirstName();
        this.lastName = param.getLastName();
        this.displayName = param.getDisplayName();

        if (param.getAvatarAssetId() != null) {
            assignAvatar(param.getAvatarAssetId());
        } else {
            clearAvatar();
        }
    }

    public void markEmailVerified() {
        requireMutableUser();

        if (emailValid && emailVerified) {
            return;
        }

        emailValid = true;
        emailVerified = true;
    }

    public void markEmailInvalid() {
        requireMutableUser();

        if (!emailValid && !emailVerified) {
            return;
        }

        emailValid = false;
        emailVerified = false;
        bumpAuthVersion();
    }

    public void expirePassword() {
        requireMutableUser();

        if (passwordExpired) {
            return;
        }

        passwordExpired = true;
        bumpAuthVersion();
    }

    public void markPasswordChanged(String passwordHash, boolean passwordAutoset) {
        requireMutableUser();
        Assert.hasText(passwordHash, "passwordHash is required");

        this.passwordHash = passwordHash.trim();
        this.passwordAutoset = passwordAutoset;
        this.passwordExpired = false;

        bumpAuthVersion();
    }

    public void assignAvatar(UUID avatarAssetId) {
        requireMutableUser();
        Assert.notNull(avatarAssetId, "avatarAssetId");

        if (avatarAssetId.equals(this.avatarAssetId)) {
            return;
        }

        this.avatarAssetId = avatarAssetId;
    }

    public void clearAvatar() {
        requireMutableUser();

        if (avatarAssetId == null) {
            return;
        }

        avatarAssetId = null;
    }

    public void assignCoverImage(UUID coverImageAssetId) {
        requireMutableUser();
        Assert.notNull(coverImageAssetId, "coverImageAssetId is required");

        if (coverImageAssetId.equals(this.coverImageAssetId)) {
            return;
        }

        this.coverImageAssetId = coverImageAssetId;
    }

    public void clearCoverImage() {
        requireMutableUser();

        if (coverImageAssetId == null) {
            return;
        }

        coverImageAssetId = null;
    }

    public void markActiveNow(Clock clock) {
        requireEnabledUser();
        Assert.notNull(clock, "clock is required");

        lastActive = Instant.now(clock);
    }

    public void deactivateAccount() {
        requireMutableUser();

        if (!activated) {
            return;
        }

        activated = false;
        bumpAuthVersion();
    }

    public void reactivateAccount() {
        requireActive("User");

        if (activated) {
            return;
        }

        Assert.state(maskedAt == null, "User is masked");

        activated = true;
        bumpAuthVersion();
    }

    public void markManaged() {
        requireMutableUser();

        managed = true;
    }

    public void markUnmanaged() {
        requireMutableUser();

        managed = false;
    }

    public void markMasked(Clock clock) {
        requireMutableUser();
        Assert.notNull(clock, "clock is required");

        maskedAt = Instant.now(clock);
        activated = false;

        bumpAuthVersion();
    }

    public void delete(Clock clock) {
        requireMutableUser();
        Assert.notNull(clock, "clock is required");

        activated = false;
        bumpAuthVersion();

        softDelete(clock);
    }

    public void restoreUser() {
        restore();

        if (!activated) {
            activated = true;
            bumpAuthVersion();
        }
    }

    public void bumpAuthVersion() {
        authVersion = authVersion == null ? 1L : authVersion + 1L;
    }

    public boolean isLoginAllowed() {
        return isActive()
                && activated
                && !passwordExpired
                && maskedAt == null;
    }

    public void recordSuccessfullLogin(
            String loginMedium,
            String ipAddress,
            String userAgent,
            Clock clock) {
        requireEnabledUser();

        Assert.hasText(loginMedium, "loginMedium is required");
        Assert.notNull(clock, "clock is required");

        Instant now = Instant.now(clock);

        lastLoginMedium = normalizeRequired(loginMedium, "loginMedium", MAX_LOGIN_MEDIUM_LENGTH);
        lastLoginIp = normalizeNullable(ipAddress, "ipAddress", MAX_IP_LENGTH);
        lastLoginUagent = normalizeNullable(userAgent, "userAgent", MAX_USER_AGENT_LENGTH);
        lastLoginTime = now;
        lastActive = now;
    }

    private void initializeForCreate(UserCreateParam param, Clock clock) {
        Instant now = Instant.now(clock);

        username = param.getUsername();
        mobileNumber = param.getMobileNumber();
        email = param.getEmail();
        displayName = param.getDisplayName();
        firstName = param.getFirstName();
        lastName = param.getLastName();
        passwordHash = param.getPasswordHash();
        passwordAutoset = param.isPasswordAutoset();
        userTimezone = param.getUserTimezone();
        principalType = param.getPrincipalType();

        managed = false;
        passwordExpired = false;
        activated = true;
        emailVerified = false;
        emailValid = true;
        maskedAt = null;
        authVersion = 1L;

        lastActive = now;

        if (param.hasInitialLoginAudit()) {
            lastLoginTime = now;
            lastLoginIp = param.getInitialLoginIp();
            lastLoginMedium = param.getInitialLoginMedium();
            lastLoginUagent = param.getInitialLoginUserAgent();
        }
    }

    private void requireMutableUser() {
        requireActive("User");

        Assert.state(maskedAt == null, "User is masked");
    }

    private void requireEnabledUser() {
        requireMutableUser();
        Assert.state(activated, "User account is not activated");
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
        if (normalized.isEmpty()) {
            return null;
        }

        Assert.isTrue(normalized.length() <= maxLength, fieldName + " must be " + maxLength + " characters or fewer");

        return normalized;
    }

}
