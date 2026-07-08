package com.syncturtle.services.user.model;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.syncturtle.common.contracts.auth.provider.AuthProvider;
import com.syncturtle.common.data.jpa.entity.TimeAuditEntity;
import com.syncturtle.services.user.model.param.AccountConnectionParam;
import com.syncturtle.services.user.model.param.AccountCreateParam;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Access(AccessType.FIELD)
@Table(name = "accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends TimeAuditEntity {

    private static final int MAX_PROVIDER_ACCOUNT_ID_LENGTH = 255;

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "provider_account_id", nullable = false, length = MAX_PROVIDER_ACCOUNT_ID_LENGTH)
    private String providerAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private AuthProvider provider;

    @Column(name = "access_token", nullable = false, columnDefinition = "text")
    private String accessToken;

    @Column(name = "access_token_expired_at")
    private Instant accessTokenExpiredAt;

    @Column(name = "refresh_token", columnDefinition = "text")
    private String refreshToken;

    @Column(name = "refresh_token_expired_at")
    private Instant refreshTokenExpiredAt;

    @Column(name = "last_connected_at", nullable = false)
    private Instant lastConnectedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false)
    private JsonNode metadata;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "id_token", nullable = false, columnDefinition = "text")
    private String idToken;

    public static Account create(AccountCreateParam param, Clock clock) {
        Assert.notNull(param, "account create param is required");
        Assert.notNull(clock, "clock is required");

        Account account = new Account();
        account.initializeForCreate(param, clock);
        return account;
    }

    public void reconnect(AccountConnectionParam param, Clock clock) {
        Assert.notNull(param, "account connection param is required");
        Assert.notNull(clock, "clock is required");

        requireSameProvider(provider);
        requireSameProviderAccountId(providerAccountId);

        applyConnection(param, clock);
    }

    public boolean belongsTo(User user) {
        Assert.notNull(user, "user is required");

        return this.user != null && this.user.getId().equals(user.getId());
    }

    private void initializeForCreate(AccountCreateParam param, Clock clock) {
        user = param.getUser();
        provider = param.getProvider();
        providerAccountId = param.getProviderAccountId();

        applyConnection(param.getConnection(), clock);
    }

    private void applyConnection(AccountConnectionParam param, Clock clock) {
        accessToken = param.getAccessToken();
        accessTokenExpiredAt = param.getAccessTokenExpiresAt();
        refreshToken = param.getRefreshToken();
        refreshTokenExpiredAt = param.getRefreshTokenExpiresAt();
        idToken = param.getIdToken();
        metadata = param.getMetadata();
        lastConnectedAt = Instant.now(clock);
    }

    private void requireSameProvider(AuthProvider provider) {
        Assert.notNull(provider, "provider is required");
        Assert.state(this.provider == provider, "Account provider cannot be changed");
    }

    private void requireSameProviderAccountId(String providerAccountId) {
        Assert.hasText(providerAccountId, "providerAccountId is required");
        Assert.state(this.providerAccountId.equals(providerAccountId.trim()),
                "Account providerAccountId cannot be changed");
    }

}
