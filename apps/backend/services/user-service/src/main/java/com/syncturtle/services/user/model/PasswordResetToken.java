package com.syncturtle.services.user.model;

import java.time.Instant;
import java.util.Objects;

import org.springframework.util.Assert;

import com.syncturtle.common.data.jpa.entity.AuditedEntity;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Access(AccessType.FIELD)
@Table(name = "password_reset_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 128)
    private String tokenHash;

    @Column(name = "issued_for_auth_version", nullable = false, updatable = false)
    private Long issuedForAuthVersion;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static PasswordResetToken issue(User user, String tokenHash, Instant expiresAt) {
        Assert.notNull(user, "user is required");
        Assert.notNull(user.getId(), "user must be persisted");
        Assert.notNull(user.getAuthVersion(), "user.authVersion is required");
        Assert.hasText(tokenHash, "tokenHash is required");
        Assert.notNull(expiresAt, "expiresAt is required");

        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.user = user;
        passwordResetToken.tokenHash = tokenHash.trim();
        passwordResetToken.issuedForAuthVersion = user.getAuthVersion();
        passwordResetToken.expiresAt = expiresAt;

        return passwordResetToken;
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean isInvalidated() {
        return invalidatedAt != null;
    }

    public boolean isExpired(Instant now) {
        Assert.notNull(now, "now is required");

        return !now.isBefore(expiresAt);
    }

    public boolean matchesCurrentAuthVersion() {
        return Objects.equals(issuedForAuthVersion, user.getAuthVersion());
    }

    public boolean isUsable(Instant now) {
        Assert.notNull(now, "now is required");

        return !isConsumed()
                && !isInvalidated()
                && !isExpired(now)
                && matchesCurrentAuthVersion();
    }

    public void consume(Instant consumedAt) {
        Assert.notNull(consumedAt, "consumedAt is required");

        requireActive("PasswordResetToken");

        Assert.state(!isConsumed(), "PasswordResetToken is already consumed");
        Assert.state(!isInvalidated(), "PasswordResetToken is invalidated");
        Assert.state(!isExpired(consumedAt), "PasswordResetToken is expired");
        Assert.state(matchesCurrentAuthVersion(), "PasswordResetToken auth version is stale");

        this.consumedAt = consumedAt;
    }

    public void invalidate(Instant invalidatedAt) {
        Assert.notNull(invalidatedAt, "invalidatedAt is required");

        requireActive("PasswordResetToken");

        if (isConsumed() || isInvalidated()) {
            return;
        }

        this.invalidatedAt = invalidatedAt;
    }

}
