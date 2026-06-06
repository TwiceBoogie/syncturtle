package com.syncturtle.services.user.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.syncturtle.common.contracts.auth.provider.AuthProvider;
import com.syncturtle.common.data.jpa.entity.TimeAuditEntity;

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
import lombok.Getter;

@Getter
@Entity
@Table(name = "accounts")
public class Account extends TimeAuditEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "provider_account_id", nullable = false)
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

}
