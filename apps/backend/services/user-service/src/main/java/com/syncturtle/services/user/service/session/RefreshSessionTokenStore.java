package com.syncturtle.services.user.service.session;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.cache.template.RedisKeyBuilder;
import com.syncturtle.common.contracts.auth.session.RefreshSessionRecord;
import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.core.security.token.TokenHasher;
import com.syncturtle.services.user.configuration.property.AuthProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshSessionTokenStore {

    private static final int REFRESH_SECRET_BYTES = 48;

    private static final String OWNER = "user-service";
    private static final String RESOURCE_REFRESH_SESSION = "refresh-session";
    private static final String RESOURCE_USER_REFRESH_SESSIONS = "refresh-sessions-by-user";
    private static final String RESOURCE_USER_AUTH_VERSION = "user-auth-version";
    private static final String RESOURCE_ADMIN_SESSION_VERSION = "admin-session-version";
    private static final String SCOPE_SESSION_ID = "sid";
    private static final String SCOPE_USER_ID = "u";
    private static final String SCOPE_INSTANCE_ID = "i";

    private final StringRedisTemplate redis;
    private final RedisKeyBuilder redisKeyBuilder;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHasher tokenHasher;
    private final ObjectMapper objectMapper;
    private final AuthProperties authProperties;

    public IssuedRefreshTokenReceipt createSessionRefreshToken(RefreshSessionCreateSpec spec) {
        validateCreate(spec);

        String sessionId = UUID.randomUUID().toString();

        return storeRefreshSession(
                sessionId,
                spec.getUserId(),
                spec.getInstanceId(),
                spec.getEmail(),
                spec.getRoles(),
                spec.getAuthVersion(),
                spec.getAdminSessionVersion(),
                spec.getIpAddress(),
                spec.getUserAgent());
    }

    public Optional<RefreshSessionRecord> findSession(String sessionId) {
        Assert.hasText(sessionId, "sessionId is required");

        String json = redis.opsForValue().get(sessionKey(sessionId));

        if (!StringUtils.hasText(json)) {
            return Optional.empty();
        }

        return Optional.of(fromJson(json));
    }

    public IssuedRefreshTokenReceipt rotateRefreshToken(RefreshSessionRotateSpec spec) {
        validateRotate(spec);

        return storeRefreshSession(
                spec.getSessionId(),
                spec.getUserId(),
                spec.getInstanceId(),
                spec.getEmail(),
                spec.getRoles(),
                spec.getAuthVersion(),
                spec.getAdminSessionVersion(),
                spec.getIpAddress(),
                spec.getUserAgent());
    }

    public void revokeSession(String sessionId) {
        Assert.hasText(sessionId, "sessionId is required");

        String key = sessionKey(sessionId);
        String json = redis.opsForValue().get(key);

        redis.delete(key);

        if (!StringUtils.hasText(json)) {
            return;
        }

        RefreshSessionRecord record = fromJson(json);
        if (StringUtils.hasText(record.getUserId())) {
            redis.opsForSet().remove(userSessionsKey(record.getUserId()), sessionId);
        }
    }

    public void revokeUserSessions(String userId) {
        Assert.hasText(userId, "userId is required");

        String userSessionsKey = userSessionsKey(userId);
        Set<String> sessionIds = redis.opsForSet().members(userSessionsKey);

        if (sessionIds == null || sessionIds.isEmpty()) {
            redis.delete(userSessionsKey);
            return;
        }

        List<String> sessionKeys = new ArrayList<>(sessionIds.size());
        for (String sessionId : sessionIds) {
            if (StringUtils.hasText(sessionId)) {
                sessionKeys.add(sessionKey(sessionId));
            }
        }

        if (!sessionKeys.isEmpty()) {
            redis.delete(sessionKeys);
        }

        redis.delete(userSessionsKey);
    }

    public void revokeUserSessionExcept(String userId, String retainedSessionId) {
        Assert.hasText(userId, "userId is required");
        Assert.hasText(retainedSessionId, "retainedSessionId is required");

        String userSessionsKey = userSessionsKey(userId);
        Set<String> sessionIds = redis.opsForSet().members(userSessionsKey);

        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }

        List<String> revokedSessionIds = new ArrayList<>();
        List<String> revokedSessionKeys = new ArrayList<>();

        for (String sessionId : sessionIds) {
            if (!StringUtils.hasText(sessionId) || retainedSessionId.equals(sessionId)) {
                continue;
            }

            revokedSessionIds.add(sessionId);
            revokedSessionKeys.add(sessionKey(sessionId));
        }

        if (!revokedSessionKeys.isEmpty()) {
            redis.delete(revokedSessionKeys);
        }

        if (!revokedSessionIds.isEmpty()) {
            redis.opsForSet().remove(userSessionsKey, revokedSessionIds.toArray());
        }

        redis.expire(userSessionsKey, authProperties.getRefreshTokenTtl());
    }

    public String extractSessionIdFromRefreshToken(String refreshToken) {
        Assert.hasText(refreshToken, "refreshToken is required");

        int dot = refreshToken.indexOf('.');

        if (dot <= 0) {
            throw new IllegalArgumentException("Invalid refresh token format");
        }

        return refreshToken.substring(0, dot);
    }

    public boolean matchesStoredRefreshToken(String presentedRefreshToken, RefreshSessionRecord record) {
        Assert.hasText(presentedRefreshToken, "presentedRefreshToken is required");
        Assert.notNull(record, "record is required");
        Assert.hasText(record.getRefreshTokenHash(), "record.refreshToken is required");

        return tokenHasher.hash(presentedRefreshToken).equals(record.getRefreshTokenHash());
    }

    public String sessionKey(String sessionId) {
        Assert.hasText(sessionId, "sessionId is required");

        return redisKeyBuilder.authKey(OWNER, RESOURCE_REFRESH_SESSION, SCOPE_SESSION_ID, sessionId);
    }

    public String userSessionsKey(String userId) {
        Assert.hasText(userId, "userId is required");

        return redisKeyBuilder.authKey(OWNER, RESOURCE_USER_REFRESH_SESSIONS, SCOPE_USER_ID, userId);
    }

    public String currentUserAuthVersionKey(String userId) {
        Assert.hasText(userId, "userId is required");

        return redisKeyBuilder.authKey(OWNER, RESOURCE_USER_AUTH_VERSION, SCOPE_USER_ID, userId);
    }

    public String currentAdminSessionVersionKey(String instanceId, String userId) {
        Assert.hasText(instanceId, "instanceId is required");
        Assert.hasText(userId, "userId is required");

        return redisKeyBuilder.authKey(OWNER, RESOURCE_ADMIN_SESSION_VERSION, SCOPE_INSTANCE_ID, instanceId,
                SCOPE_USER_ID, userId);
    }

    private IssuedRefreshTokenReceipt storeRefreshSession(
            String sessionId,
            String userId,
            String instanceId,
            String email,
            List<String> roles,
            Long authVersion,
            Long adminSessionVersion,
            String ipAddress,
            String userAgent) {
        String secret = tokenGenerator.generateBase64Url(REFRESH_SECRET_BYTES);
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
                .refreshTokenHash(tokenHasher.hash(refreshToken))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        redis.opsForValue().set(
                sessionKey(sessionId),
                toJson(sessionRecord),
                authProperties.getRefreshTokenTtl());

        indexSessionForUser(userId, sessionId);

        return IssuedRefreshTokenReceipt.builder()
                .sessionId(sessionId)
                .token(refreshToken)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }

    private void indexSessionForUser(String userId, String sessionId) {
        Assert.hasText(userId, "userId is required");
        Assert.hasText(sessionId, "sessionId is required");

        String key = userSessionsKey(userId);

        redis.opsForSet().add(key, sessionId);
        redis.expire(key, authProperties.getRefreshTokenTtl());
    }

    private void validateCreate(RefreshSessionCreateSpec spec) {
        Assert.notNull(spec, "spec is required");
        Assert.hasText(spec.getUserId(), "userId is required");
        Assert.hasText(spec.getInstanceId(), "instanceId is required");
        Assert.hasText(spec.getEmail(), "email is required");

        if (spec.getRoles() == null || spec.getRoles().isEmpty()) {
            throw new IllegalArgumentException("At least one role is required");
        }
    }

    private void validateRotate(RefreshSessionRotateSpec spec) {
        Assert.notNull(spec, "spec is required");
        Assert.hasText(spec.getSessionId(), "sessionid is required");
        Assert.hasText(spec.getUserId(), "userId is required");
        Assert.hasText(spec.getInstanceId(), "instanceId is required");
        Assert.hasText(spec.getEmail(), "email is required");

        if (spec.getRoles() == null || spec.getRoles().isEmpty()) {
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
