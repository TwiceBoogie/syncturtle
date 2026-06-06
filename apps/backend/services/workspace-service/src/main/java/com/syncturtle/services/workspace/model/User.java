package com.syncturtle.services.workspace.model;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.util.Assert;

import com.syncturtle.common.core.actor.PrincipalType;
import com.syncturtle.common.data.jpa.support.ValidTimeZone;
import com.syncturtle.services.workspace.model.param.UserReplicaParam;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Access(AccessType.FIELD)
@Table(name = "users_lite")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * Copied from user-service
     * 
     * Important:
     * This is NOT a JPA @Version filed for instance-service
     */
    @Column(name = "source_version", nullable = false)
    private Long sourceVersion;

    @Column(name = "auth_version", nullable = false)
    private Long authVersion;

    @Column(name = "username", nullable = false, length = 128)
    private String username;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "avatar_asset_id")
    private UUID avatarAssetId;

    @Column(name = "cover_image_asset_id")
    private UUID coverImageAssetId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "is_email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "is_password_autoset", nullable = false)
    private boolean passwordAutoset;

    @ValidTimeZone
    @Column(name = "user_timezone", nullable = false)
    private String userTimezone;

    @Enumerated(EnumType.STRING)
    @Column(name = "principal_type", nullable = false)
    private PrincipalType principalType;

    @Column(name = "last_login_medium", length = 20)
    private String lastLoginMedium;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "projected_at", nullable = false)
    private Instant projectedAt;

    public static User fromReplicaParam(UserReplicaParam param, Clock clock) {
        Assert.notNull(param, "user replica param is required");
        Assert.notNull(clock, "clock is required");

        User user = new User();
        user.applyReplicaParam(param, clock);
        return user;
    }

    public boolean applyReplicaParam(UserReplicaParam param, Clock clock) {
        Assert.notNull(param, "user replica param is required");
        Assert.notNull(clock, "clock is required");

        requireSameUserOrUninitialized(param);

        if (param.isStaleComparedTo(sourceVersion)) {
            return false;
        }

        boolean changed = false;

        changed |= setIdIfMissing(param.getId());
        changed |= setSourceVersion(param.getSourceVersion());
        changed |= setAuthVersion(param.getAuthVersion());

        changed |= setUsername(param.getUsername());
        changed |= setEmail(param.getEmail());
        changed |= setDisplayName(param.getDisplayName());
        changed |= setFirstName(param.getFirstName());
        changed |= setLastName(param.getLastName());

        changed |= setAvatarAssetId(param.getAvatarAssetId());
        changed |= setCoverImageAssetId(param.getCoverImageAssetId());

        changed |= setActive(param.isActive());
        changed |= setEmailVerified(param.isEmailVerified());
        changed |= setPasswordAutoset(param.isPasswordAutoset());
        changed |= setUserTimezone(param.getUserTimezone());
        changed |= setPrincipalType(param.getPrincipalType());

        changed |= setCreatedAt(param.getCreatedAt());
        changed |= setUpdatedAt(param.getUpdatedAt());

        projectedAt = Instant.now(clock);

        return changed;
    }

    public boolean isDeletedReplica() {
        return !active;
    }

    private void requireSameUserOrUninitialized(UserReplicaParam param) {
        if (id == null) {
            return;
        }

        Assert.isTrue(id.equals(param.getId()), "cannot apply a user replica param for a different user");
    }

    private boolean setIdIfMissing(UUID id) {
        if (this.id != null) {
            return false;
        }

        this.id = id;
        return true;
    }

    private boolean setSourceVersion(Long sourceVersion) {
        if (Objects.equals(this.sourceVersion, sourceVersion)) {
            return false;
        }

        this.sourceVersion = sourceVersion;
        return true;
    }

    private boolean setAuthVersion(Long authVersion) {
        if (Objects.equals(this.authVersion, authVersion)) {
            return false;
        }

        this.authVersion = authVersion;
        return true;
    }

    private boolean setUsername(String username) {
        if (Objects.equals(this.username, username)) {
            return false;
        }

        this.username = username;
        return true;
    }

    private boolean setEmail(String email) {
        if (Objects.equals(this.email, email)) {
            return false;
        }

        this.email = email;
        return true;
    }

    private boolean setDisplayName(String displayName) {
        if (Objects.equals(this.displayName, displayName)) {
            return false;
        }

        this.displayName = displayName;
        return true;
    }

    private boolean setFirstName(String firstName) {
        if (Objects.equals(this.firstName, firstName)) {
            return false;
        }

        this.firstName = firstName;
        return true;
    }

    private boolean setLastName(String lastName) {
        if (Objects.equals(this.lastName, lastName)) {
            return false;
        }

        this.lastName = lastName;
        return true;
    }

    private boolean setAvatarAssetId(UUID avatarAssetId) {
        if (Objects.equals(this.avatarAssetId, avatarAssetId)) {
            return false;
        }

        this.avatarAssetId = avatarAssetId;
        return true;
    }

    private boolean setCoverImageAssetId(UUID coverImageAssetId) {
        if (Objects.equals(this.coverImageAssetId, coverImageAssetId)) {
            return false;
        }

        this.coverImageAssetId = coverImageAssetId;
        return true;
    }

    private boolean setCreatedAt(Instant createdAt) {
        if (Objects.equals(this.createdAt, createdAt)) {
            return false;
        }

        this.createdAt = createdAt;
        return true;
    }

    private boolean setActive(boolean active) {
        if (this.active == active) {
            return false;
        }

        this.active = active;
        return true;
    }

    private boolean setEmailVerified(boolean emailVerified) {
        if (this.emailVerified == emailVerified) {
            return false;
        }

        this.emailVerified = emailVerified;
        return true;
    }

    private boolean setPasswordAutoset(boolean passwordAutoset) {
        if (this.passwordAutoset == passwordAutoset) {
            return false;
        }

        this.passwordAutoset = passwordAutoset;
        return true;
    }

    private boolean setUserTimezone(String userTimezone) {
        if (Objects.equals(this.userTimezone, userTimezone)) {
            return false;
        }

        this.userTimezone = userTimezone;
        return true;
    }

    private boolean setPrincipalType(PrincipalType principalType) {
        if (this.principalType == principalType) {
            return false;
        }

        this.principalType = principalType;
        return true;
    }

    private boolean setUpdatedAt(Instant updatedAt) {
        if (Objects.equals(this.updatedAt, updatedAt)) {
            return false;
        }

        this.updatedAt = updatedAt;
        return true;
    }

}
