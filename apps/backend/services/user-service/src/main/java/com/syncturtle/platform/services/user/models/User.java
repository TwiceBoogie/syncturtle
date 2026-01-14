package com.syncturtle.platform.services.user.models;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.common.core.utils.StringHelper;
import com.syncturtle.common.data.jpa.model.AuditedEntity;
import com.syncturtle.platform.services.user.models.support.ValidTimeZone;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "users")
public class User extends AuditedEntity {

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "avatar", nullable = false, columnDefinition = "text")
    private String avatar;

    @Column(name = "cover_image")
    private String coverImage;

    @Column(name = "avatar_asset_id")
    private UUID avatarAssetId;

    @Column(name = "cover_image_asset_id")
    private UUID coverImageAssetId;

    @Column(name = "last_location", nullable = false)
    private String lastLocation;

    @Column(name = "created_location", nullable = false)
    private String createdLocation;

    @Column(name = "is_managed", nullable = false)
    private boolean managed;

    @Column(name = "is_password_expired", nullable = false)
    private boolean passwordExpired;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "is_email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "is_password_autoset", nullable = false)
    private boolean passwordAutoset;

    @Column(name = "token", nullable = false)
    private String token;

    @ValidTimeZone
    @Column(name = "user_timezone", nullable = false)
    private String userTimezone;

    @Column(name = "last_active")
    private Instant lastActive;

    @Column(name = "last_login_time")
    private Instant lastLoginTime;

    @Column(name = "last_logout_time")
    private Instant lastLogoutTime;

    @Column(name = "last_login_ip", nullable = false)
    private String lastLoginIp;

    @Column(name = "last_logout_ip", nullable = false)
    private String lastLogoutIp;

    @Column(name = "last_login_medium", nullable = false)
    private String lastLoginMedium;

    @Column(name = "last_login_uagent", nullable = false, columnDefinition = "text")
    private String lastLoginUagent;

    @Column(name = "token_updated_at")
    private Instant tokenUpdatedAt;

    @Column(name = "is_bot", nullable = false)
    private boolean bot;

    @Column(name = "bot_type")
    private String botType;

    @Column(name = "is_email_valid", nullable = false)
    private boolean emailValid;

    @Column(name = "masked_at")
    private Instant maskedAt;

    @PrePersist
    void prePersist() {
        normalizeEmail();
        applyDefaults();
    }

    private void normalizeEmail() {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }

    private void applyDefaults() {
        if (displayName == null || displayName.isBlank()) {
            displayName = (email != null && email.contains("@")) ? email.substring(0, email.indexOf('@'))
                    : StringHelper.randomAsciLetters("user");
        }
        if (tokenUpdatedAt != null) {
            token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
            tokenUpdatedAt = Instant.now();
        }
    }
}
