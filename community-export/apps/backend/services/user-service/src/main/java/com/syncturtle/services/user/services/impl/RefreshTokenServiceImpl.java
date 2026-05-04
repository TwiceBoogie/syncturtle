package com.syncturtle.services.user.services.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.contracts.auth.session.RefreshSessionRecord;
import com.syncturtle.services.user.configurations.properties.AuthProperties;
import com.syncturtle.services.user.dto.command.CreateRefreshSessionCommand;
import com.syncturtle.services.user.dto.command.RotateRefreshSessionCommand;
import com.syncturtle.services.user.payload.IssuedRefreshToken;
import com.syncturtle.services.user.services.RefreshTokenService;
import com.syncturtle.services.user.services.TokenGenerator;
import com.syncturtle.services.user.services.TokenHashingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final int REFRESH_SECRET_BYTES = 48;

    private final StringRedisTemplate redis;
    private final TokenGenerator tokenGenerator;
    private final TokenHashingService tokenHashingService;
    private final ObjectMapper objectMapper;
    private final AuthProperties authProperties;

    @Override
    public IssuedRefreshToken createSessionRefreshToken(CreateRefreshSessionCommand command) {
        validateCreate(command);

        String sessionId = UUID.randomUUID().toString();

        return storeRefreshSession(
                sessionId,
                command.getUserId(),
                command.getInstanceId(),
                command.getEmail(),
                command.getRoles(),
                command.getAuthVersion(),
                command.getAdminSessionVersion(),
                command.getIpAddress(),
                command.getUserAgent());
    }

    @Override
    public Optional<RefreshSessionRecord> findSession(String sessionId) {
        Assert.hasText(sessionId, "sessionId is required");

        String json = redis.opsForValue().get(sessionKey(sessionId));

        if (!StringUtils.hasText(json)) {
            return Optional.empty();
        }

        return Optional.of(fromJson(json));
    }

    @Override
    public IssuedRefreshToken rotateRefreshToken(RotateRefreshSessionCommand command) {
        validateRotate(command);

        return storeRefreshSession(
                command.getSessionId(),
                command.getUserId(),
                command.getInstanceId(),
                command.getEmail(),
                command.getRoles(),
                command.getAuthVersion(),
                command.getAdminSessionVersion(),
                command.getIpAddress(),
                command.getUserAgent());
    }

    @Override
    public void revokeSession(String sessionId) {
        Assert.hasText(sessionId, "sessionId is required");

        redis.delete(sessionKey(sessionId));
    }

    @Override
    public String extractSessionIdFromRefreshToken(String refreshToken) {
        Assert.hasText(refreshToken, "refreshToken is required");

        int dot = refreshToken.indexOf('.');

        if (dot <= 0) {
            throw new IllegalArgumentException("Invalid refresh token format");
        }

        return refreshToken.substring(0, dot);
    }

    @Override
    public boolean matchesStoredRefreshToken(String presentedRefreshToken, RefreshSessionRecord record) {
        Assert.hasText(presentedRefreshToken, "presentedRefreshToken is required");
        Assert.notNull(record, "record is required");
        Assert.hasText(record.getRefreshTokenHash(), "record.refreshToken is required");

        return tokenHashingService.hash(presentedRefreshToken).equals(record.getRefreshTokenHash());
    }

    @Override
    public String sessionKey(String sessionId) {
        Assert.hasText(sessionId, "sessionId is required");

        return "auth:session:" + sessionId;
    }

    @Override
    public String currentUserAuthVersionKey(String userId) {
        Assert.hasText(userId, "userId is required");

        return "auth:user-version:" + userId;
    }

    @Override
    public String currentAdminSessionVersionKey(String instanceId, String userId) {
        Assert.hasText(instanceId, "instanceId is required");
        Assert.hasText(userId, "userId is required");

        return "auth:admin-session-version:" + instanceId + ":" + userId;
    }

    private IssuedRefreshToken storeRefreshSession(
            String sessionId,
            String userId,
            String instanceId,
            String email,
            List<String> roles,
            Long authVersion,
            Long adminSessionVersion,
            String ipAddress,
            String userAgent) {
        String secret = tokenGenerator.generate(REFRESH_SECRET_BYTES);
        String refreshToken = sessionId + "." + secret;

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(authProperties.getRefreshTokenTtl());

        RefreshSessionRecord sessionRecord = RefreshSessionRecord.builder()
                .userId(userId)
                .instanceId(instanceId)
                .email(email)
                .roles(roles == null ? List.of() : List.copyOf(roles))
                .authVersion(authVersion)
                .adminSessionVersion(adminSessionVersion)
                .active(true)
                .refreshTokenHash(tokenHashingService.hash(refreshToken))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        redis.opsForValue().set(
                sessionKey(sessionId),
                toJson(sessionRecord),
                authProperties.getRefreshTokenTtl());

        return IssuedRefreshToken.builder()
                .sessionId(sessionId)
                .token(refreshToken)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }

    private void validateCreate(CreateRefreshSessionCommand command) {
        Assert.notNull(command, "command is required");
        Assert.hasText(command.getUserId(), "userId is required");
        Assert.hasText(command.getInstanceId(), "instanceId is required");
        Assert.hasText(command.getEmail(), "email is required");

        if (command.getRoles() == null || command.getRoles().isEmpty()) {
            throw new IllegalArgumentException("At least one role is required");
        }
    }

    private void validateRotate(RotateRefreshSessionCommand command) {
        Assert.notNull(command, "command is required");
        Assert.hasText(command.getSessionId(), "sessionid is required");
        Assert.hasText(command.getUserId(), "userId is required");
        Assert.hasText(command.getInstanceId(), "instanceId is required");
        Assert.hasText(command.getEmail(), "email is required");

        if (command.getRoles() == null || command.getRoles().isEmpty()) {
            throw new IllegalArgumentException("At least one role is required");
        }
    }

    private String toJson(RefreshSessionRecord record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize refresh session record", exception);
        }
    }

    private RefreshSessionRecord fromJson(String value) {
        try {
            return objectMapper.readValue(value, RefreshSessionRecord.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize refresh session record", exception);
        }
    }

}
