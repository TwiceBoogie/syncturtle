package com.syncturtle.services.user.service.collaborator.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.auth.session.RefreshSessionRecord;
import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.core.security.token.TokenHasher;
import com.syncturtle.services.user.exception.RefreshSessionException;
import com.syncturtle.services.user.exception.RefreshSessionException.Reason;
import com.syncturtle.services.user.service.param.AuthorizedSessionIssueParam;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class RefreshSessionStore {

    private static final int REFRESH_SECRET_BYTES = 48;
    private static final Pattern REFRESH_SECRET = Pattern.compile("[A-Za-z0-9_-]{64}");
    private static final Set<String> FIELDS = Set.of(
            "recordVersion",
            "userId",
            "instanceId",
            "roles",
            "authVersion",
            "adminSessionVersion",
            "currentRefreshTokenHash",
            "createdAt",
            "idleExpiresAt",
            "absoluteExpiresAt");

    private final StringRedisTemplate redis;
    private final RefreshSessionRedisKeys keys;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHasher tokenHasher;
    private final UserAuthVersionStore versionStore;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public RefreshSessionStore(
            StringRedisTemplate redis,
            RefreshSessionRedisKeys keys,
            SecureTokenGenerator tokenGenerator,
            TokenHasher tokenHasher,
            UserAuthVersionStore versionStore,
            JsonMapper jsonMapper,
            Clock clock) {
        Assert.notNull(redis, "redis is required");
        Assert.notNull(keys, "keys are required");
        Assert.notNull(tokenGenerator, "tokenGenerator is required");
        Assert.notNull(tokenHasher, "tokenHasher is required");
        Assert.notNull(versionStore, "versionStore is required");
        Assert.notNull(jsonMapper, "jsonMapper is required");
        Assert.notNull(clock, "clock is required");

        this.redis = redis;
        this.keys = keys;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.versionStore = versionStore;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    public RefreshSessionReceipt create(AuthorizedSessionIssueParam param) {
        Assert.notNull(param, "authorized session issue param is required");

        versionStore.ensureUserCurrent(param.getUserId(), param.getAuthVersion());
        versionStore.ensureAdminCurrent(param.getInstanceId(), param.getUserId(), param.getAdminSessionVersion());

        Instant now = clock.instant();
        Instant absolute = now.plus(RefreshSessionRecord.ABSOLUTE_LIFETIME);
        Instant idle = earlier(now.plus(RefreshSessionRecord.IDLE_LIFETIME), absolute);
        String sessionId = UUID.randomUUID().toString();
        String refreshToken = newRefreshToken(sessionId);
        RefreshSessionRecord record = RefreshSessionRecord.builder()
                .recordVersion(RefreshSessionRecord.CURRENT_RECORD_VERSION)
                .userId(param.getUserId())
                .instanceId(param.getInstanceId())
                .roles(param.getRoles())
                .authVersion(param.getAuthVersion())
                .adminSessionVersion(param.getAdminSessionVersion())
                .currentRefreshTokenHash(tokenHasher.hash(refreshToken))
                .createdAt(now)
                .idleExpiresAt(idle)
                .absoluteExpiresAt(absolute)
                .build();
        String json = encode(record);

        try {
            Boolean created = redis.opsForValue().set(
                    keys.session(sessionId),
                    json,
                    set -> set.ifAbsent().expiration(at(idle)));
            if (!Boolean.TRUE.equals(created)) {
                throw new RefreshSessionException(Reason.UNAVAILABLE, "session ID collision");
            }
        } catch (DataAccessException exception) {
            throw new RefreshSessionException(Reason.UNAVAILABLE, "session store is unavailable", exception);
        }

        return new RefreshSessionReceipt(sessionId, refreshToken, record, now);
    }

    public RefreshSessionReceipt rotate(String presentedRefreshToken, RefreshSessionSnapshot snapshot) {
        Assert.notNull(snapshot, "refresh session snapshot is required");

        if (!extractSessionId(presentedRefreshToken).equals(snapshot.getSessionId())) {
            throw new RefreshSessionException(Reason.INVALID, "refresh session ID does not match");
        }

        String presentedHash = tokenHasher.hash(presentedRefreshToken);
        if (!MessageDigest.isEqual(presentedHash.getBytes(StandardCharsets.US_ASCII),
                snapshot.getRecord().getCurrentRefreshTokenHash().getBytes(StandardCharsets.US_ASCII))) {
            throw new RefreshSessionException(Reason.INVALID, "refresh credential is stale");
        }

        versionStore.requireCurrent(snapshot);
        Instant now = clock.instant();
        RefreshSessionRecord current = snapshot.getRecord();

        if (!current.getIdleExpiresAt().isAfter(now) || !current.getAbsoluteExpiresAt().isAfter(now)) {
            throw new RefreshSessionException(Reason.INVALID, "session has expired");
        }

        String newToken = newRefreshToken(snapshot.getSessionId());
        Instant idle = earlier(now.plus(RefreshSessionRecord.IDLE_LIFETIME), current.getAbsoluteExpiresAt());
        RefreshSessionRecord next = RefreshSessionRecord.builder()
                .recordVersion(RefreshSessionRecord.CURRENT_RECORD_VERSION)
                .userId(current.getUserId())
                .instanceId(current.getInstanceId())
                .roles(current.getRoles())
                .authVersion(current.getAuthVersion())
                .adminSessionVersion(current.getAdminSessionVersion())
                .currentRefreshTokenHash(tokenHasher.hash(newToken))
                .createdAt(current.getCreatedAt())
                .idleExpiresAt(idle)
                .absoluteExpiresAt(current.getAbsoluteExpiresAt())
                .build();

        try {
            Boolean replaced = redis.opsForValue().set(
                    keys.session(snapshot.getSessionId()),
                    encode(next),
                    set -> set.ifEquals().value(snapshot.getExactJson()).expiration(at(idle)));
            if (!Boolean.TRUE.equals(replaced)) {
                throw new RefreshSessionException(Reason.STALE, "refresh session changed or was revoked");
            }
        } catch (DataAccessException exception) {
            throw new RefreshSessionException(Reason.UNAVAILABLE, "rotation outcome is unavailable", exception);
        }

        return new RefreshSessionReceipt(snapshot.getSessionId(), newToken, next, now);
    }

    public RefreshSessionSnapshot requirePresented(String presentedRefreshToken) {
        String sessionId = extractSessionId(presentedRefreshToken);
        String json;

        try {
            json = redis.opsForValue().get(keys.session(sessionId));
        } catch (DataAccessException exception) {
            throw new RefreshSessionException(Reason.UNAVAILABLE, "session store is unavailable", exception);
        }

        if (!StringUtils.hasText(json)) {
            throw new RefreshSessionException(Reason.INVALID, "session is unavailable");
        }

        RefreshSessionRecord record = decode(json);
        if (!record.getIdleExpiresAt().isAfter(clock.instant())
                || !record.getAbsoluteExpiresAt().isAfter(clock.instant())) {
            throw new RefreshSessionException(Reason.INVALID, "session has expired");
        }

        String presentedHash = tokenHasher.hash(presentedRefreshToken);
        if (!MessageDigest.isEqual(presentedHash.getBytes(StandardCharsets.US_ASCII),
                record.getCurrentRefreshTokenHash().getBytes(StandardCharsets.US_ASCII))) {
            throw new RefreshSessionException(Reason.INVALID, "refresh credential is stale");
        }

        return new RefreshSessionSnapshot(sessionId, json, record);
    }

    public boolean revokePresented(String presentedRefreshToken) {
        RefreshSessionSnapshot snapshot;

        try {
            snapshot = requirePresented(presentedRefreshToken);
        } catch (RefreshSessionException exception) {
            if (exception.getReason() == Reason.INVALID || exception.getReason() == Reason.STALE) {
                return false;
            }
            throw exception;
        }

        try {
            return Boolean.TRUE.equals(redis.compareAndDelete(
                    keys.session(snapshot.getSessionId()),
                    snapshot.getExactJson()));
        } catch (DataAccessException exception) {
            throw new RefreshSessionException(Reason.UNAVAILABLE, "logout outcome is unavailable", exception);
        }
    }

    public void revokeSession(String sessionId) {
        Assert.hasText(sessionId, "sessionId is required");

        try {
            redis.delete(keys.session(sessionId));
        } catch (DataAccessException exception) {
            throw new RefreshSessionException(Reason.UNAVAILABLE, "session revocation is unavailable", exception);
        }
    }

    public String extractSessionId(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new RefreshSessionException(Reason.INVALID, "refresh token is missing");
        }

        int separator = refreshToken.indexOf('.');
        if (separator <= 0 || separator == refreshToken.length() - 1 || refreshToken.indexOf('.', separator + 1) >= 0) {
            throw new RefreshSessionException(Reason.INVALID, "refresh token format is invalid");
        }

        String candidate = refreshToken.substring(0, separator);
        if (!REFRESH_SECRET.matcher(refreshToken.substring(separator + 1)).matches()) {
            throw new RefreshSessionException(Reason.INVALID, "refresh secret format is invalid");
        }

        try {
            String canonical = UUID.fromString(candidate).toString();
            if (!canonical.equals(candidate)) {
                throw new IllegalArgumentException("noncanonical session ID");
            }

            return canonical;
        } catch (IllegalArgumentException exception) {
            throw new RefreshSessionException(Reason.INVALID, "refresh session ID is invalid", exception);
        }
    }

    private RefreshSessionRecord decode(String json) {
        try {
            JsonNode root = jsonMapper.readTree(json);
            if (root == null || !root.isObject() || root.size() != FIELDS.size()) {
                throw new IllegalArgumentException("unsupported session record fields");
            }

            for (Entry<String, JsonNode> field : root.properties()) {
                if (!FIELDS.contains(field.getKey())) {
                    throw new IllegalArgumentException("unsupported session record field");
                }
            }

            return jsonMapper.treeToValue(root, RefreshSessionRecord.class);
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new RefreshSessionException(Reason.INVALID, "session record is malformed", exception);
        }
    }

    private String encode(RefreshSessionRecord record) {
        try {
            return jsonMapper.writeValueAsString(record);
        } catch (JacksonException exception) {
            throw new RefreshSessionException(Reason.UNAVAILABLE, "session serialization failed", exception);
        }
    }

    private String newRefreshToken(String sessionId) {
        return sessionId + "." + tokenGenerator.generateBase64Url(REFRESH_SECRET_BYTES);
    }

    private static Expiration at(Instant deadline) {
        return Expiration.unixTimestamp(deadline.toEpochMilli(), TimeUnit.MILLISECONDS);
    }

    private static Instant earlier(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }

}
