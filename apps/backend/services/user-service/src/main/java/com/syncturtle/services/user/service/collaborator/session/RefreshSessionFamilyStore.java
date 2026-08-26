package com.syncturtle.services.user.service.collaborator.session;

import static com.syncturtle.services.user.exception.RefreshSessionLifecycleException.Reason.ENVELOPE_FAILURE;
import static com.syncturtle.services.user.exception.RefreshSessionLifecycleException.Reason.EXPIRED_FAMILY;
import static com.syncturtle.services.user.exception.RefreshSessionLifecycleException.Reason.INVARIANT_VIOLATION;
import static com.syncturtle.services.user.exception.RefreshSessionLifecycleException.Reason.MALFORMED_STATE;
import static com.syncturtle.services.user.exception.RefreshSessionLifecycleException.Reason.MISSING_FAMILY;
import static com.syncturtle.services.user.exception.RefreshSessionLifecycleException.Reason.REDIS_FAILURE;
import static com.syncturtle.services.user.exception.RefreshSessionLifecycleException.Reason.REPLAY_REVOKED;
import static com.syncturtle.services.user.exception.RefreshSessionLifecycleException.Reason.UNSUPPORTED_RECORD_VERSION;
import static com.syncturtle.services.user.exception.RefreshSessionLifecycleException.Reason.WRONG_REDIS_TYPE;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;
import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.core.security.token.TokenHasher;
import com.syncturtle.services.user.configuration.property.RefreshSessionLifecycleProperties;
import com.syncturtle.services.user.exception.RefreshSessionFamilySerializerException;
import com.syncturtle.services.user.exception.RefreshSessionLifecycleException;
import com.syncturtle.services.user.exception.RefreshSessionSuccessorEnvelopeException;
import com.syncturtle.services.user.service.param.RefreshSessionFamilyCreateParam;
import com.syncturtle.services.user.service.param.RefreshSessionFamilyRotateParam;
import com.syncturtle.services.user.type.RefreshSessionLifecycleOutcome;
import com.syncturtle.services.user.type.RefreshSessionLifetimeStatus;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

public final class RefreshSessionFamilyStore {

    private static final int REFRESH_SECRET_BYTES = 48;
    private static final Duration INDEX_LIFETIME = Duration.ofDays(30);
    private static final String INSTANCE_ADMIN = "INSTANCE_ADMIN";
    private static final String OP_CREATE = "CREATE";
    private static final String OP_ROTATE = "ROTATE";
    private static final String OP_REVOKE_PRESENTED = "REVOKE_PRESENTED";
    private static final String OP_REVOKE_ONE = "REVOKE_ONE";
    private static final String OP_REVOKE_OTHERS = "REVOKE_OTHERS";
    private static final String OP_REVOKE_ALL = "REVOKE_ALL";

    private final StringRedisTemplate redis;
    private final RefreshSessionRedisKeys keys;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHasher tokenHasher;
    private final RefreshSessionFamilySerializer familySerializer;
    private final RefreshSessionLifetimeDecider lifetimeDecider;
    private final RefreshSessionSuccessorEnvelopeCipher envelopeCipher;
    private final RefreshSessionLifecycleScriptExecutor scriptExecutor;
    private final RefreshSessionLifecycleProperties properties;
    private final JsonMapper jsonMapper;
    private final Supplier<String> sessionIdGenerator;

    public RefreshSessionFamilyStore(
            StringRedisTemplate redis,
            RefreshSessionRedisKeys keys,
            SecureTokenGenerator tokenGenerator,
            TokenHasher tokenHasher,
            RefreshSessionFamilySerializer familySerializer,
            RefreshSessionLifetimeDecider lifetimeDecider,
            RefreshSessionSuccessorEnvelopeCipher envelopeCipher,
            RefreshSessionLifecycleScriptExecutor scriptExecutor,
            RefreshSessionLifecycleProperties properties,
            JsonMapper jsonMapper) {
        this(
                redis,
                keys,
                tokenGenerator,
                tokenHasher,
                familySerializer,
                lifetimeDecider,
                envelopeCipher,
                scriptExecutor,
                properties,
                jsonMapper,
                () -> UUID.randomUUID().toString());
    }

    RefreshSessionFamilyStore(
            StringRedisTemplate redis,
            RefreshSessionRedisKeys keys,
            SecureTokenGenerator tokenGenerator,
            TokenHasher tokenHasher,
            RefreshSessionFamilySerializer familySerializer,
            RefreshSessionLifetimeDecider lifetimeDecider,
            RefreshSessionSuccessorEnvelopeCipher envelopeCipher,
            RefreshSessionLifecycleScriptExecutor scriptExecutor,
            RefreshSessionLifecycleProperties properties,
            JsonMapper jsonMapper,
            Supplier<String> sessionIdGenerator) {
        Assert.notNull(redis, "redis is required");
        Assert.notNull(keys, "keys are required");
        Assert.notNull(tokenGenerator, "tokenGenerator is required");
        Assert.notNull(tokenHasher, "tokenHasher is required");
        Assert.notNull(familySerializer, "familySerializer is required");
        Assert.notNull(lifetimeDecider, "lifetimeDecider is required");
        Assert.notNull(envelopeCipher, "envelopeCipher is required");
        Assert.notNull(scriptExecutor, "scriptExecutor is required");
        Assert.notNull(properties, "lifecycle properties are required");
        Assert.notNull(jsonMapper, "jsonMapper is required");
        Assert.notNull(sessionIdGenerator, "sessionIdGenerator is required");

        this.redis = redis;
        this.keys = keys;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.familySerializer = familySerializer;
        this.lifetimeDecider = lifetimeDecider;
        this.envelopeCipher = envelopeCipher;
        this.scriptExecutor = scriptExecutor;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.sessionIdGenerator = sessionIdGenerator;
    }

    public RefreshSessionLifecycleResult create(RefreshSessionFamilyCreateParam param) {
        Assert.notNull(param, "refresh session family create param is required");

        String sessionId = canonicalUuid(sessionIdGenerator.get(), "generated sessionId");
        String refreshToken = newRefreshToken(sessionId);
        RefreshSessionLifetimeDecision lifetime = lifetimeDecider.decideForCreation();

        RefreshSessionFamilyRecord family = RefreshSessionFamilyRecord.builder()
                .recordVersion(RefreshSessionFamilyRecord.CURRENT_RECORD_VERSION)
                .userId(param.getUserId())
                .instanceId(param.getInstanceId())
                .roles(param.getRoles())
                .authVersion(param.getAuthVersion())
                .adminSessionVersion(param.getAdminSessionVersion())
                .currentRefreshTokenHash(tokenHasher.hash(refreshToken))
                .rotationCounter(0L)
                .createdAt(lifetime.getCreatedAt())
                .lastUsedAt(lifetime.getLastUsedAt())
                .idleExpiresAt(lifetime.getIdleExpiresAt())
                .absoluteExpiresAt(lifetime.getAbsoluteExpiresAt())
                .deviceLabel(param.getDeviceLabel())
                .clientBindingHash(param.getClientBindingHash())
                .build();

        List<String> result = execute(
                OP_CREATE,
                sessionId,
                family,
                "",
                family,
                lifetime.getEffectiveNow(),
                "",
                null,
                "",
                "",
                family.getRotationCounter(),
                family.getRotationCounter(),
                "");
        requireStatus(result, "CREATED");

        return new RefreshSessionLifecycleResult(
                RefreshSessionLifecycleOutcome.CREATED,
                sessionId,
                refreshToken,
                decodeReturnedFamily(result));
    }

    public RefreshSessionLifecycleResult rotate(RefreshSessionFamilyRotateParam param) {
        Assert.notNull(param, "refresh session family rotate param is required");

        String presentedRefreshToken = param.getPresentedRefreshToken();
        String sessionId = extractSessionId(presentedRefreshToken);
        String expectedJson = readFamilyJson(sessionId);

        if (!StringUtils.hasText(expectedJson)) {
            throw new RefreshSessionLifecycleException(MISSING_FAMILY, "refresh session family is unavailable");
        }

        RefreshSessionFamilyRecord current = decodeFamily(expectedJson);
        RefreshSessionLifetimeDecision lifetime = lifetimeDecider.decideForRotation(current);
        String presentedHash = tokenHasher.hash(presentedRefreshToken);

        RefreshSessionFamilyRecord successor = null;
        String successorToken = "";
        String graceJson = "";
        Instant graceExpiresAt = null;

        if (lifetime.getStatus() == RefreshSessionLifetimeStatus.ROTATABLE) {
            successorToken = newRefreshToken(sessionId);
            long nextCounter = current.nextRotationCounter();
            successor = RefreshSessionFamilyRecord.builder()
                    .recordVersion(RefreshSessionFamilyRecord.CURRENT_RECORD_VERSION)
                    .userId(current.getUserId())
                    .instanceId(current.getInstanceId())
                    .roles(param.getRoles())
                    .authVersion(param.getAuthVersion())
                    .adminSessionVersion(param.getAdminSessionVersion())
                    .currentRefreshTokenHash(tokenHasher.hash(successorToken))
                    .rotationCounter(nextCounter)
                    .createdAt(lifetime.getCreatedAt())
                    .lastUsedAt(lifetime.getLastUsedAt())
                    .idleExpiresAt(lifetime.getIdleExpiresAt())
                    .absoluteExpiresAt(lifetime.getAbsoluteExpiresAt())
                    .deviceLabel(param.getDeviceLabel())
                    .clientBindingHash(param.getClientBindingHash())
                    .build();

            graceExpiresAt = lifetime.getEffectiveNow().plus(properties.getGraceWindow());
            String envelope = encryptSuccessor(successorToken, sessionId, nextCounter, graceExpiresAt);
            RefreshSessionGraceRecord grace = RefreshSessionGraceRecord.builder()
                    .graceVersion(RefreshSessionGraceRecord.CURRENT_GRACE_VERSION)
                    .previousRefreshTokenHash(current.getCurrentRefreshTokenHash())
                    .successorRefreshTokenHash(successor.getCurrentRefreshTokenHash())
                    .successorRotationCounter(nextCounter)
                    .successorEnvelope(envelope)
                    .clientBindingHash(param.getClientBindingHash())
                    .expiresAt(graceExpiresAt)
                    .expiresAtEpochMilli(graceExpiresAt.toEpochMilli())
                    .consumed(false)
                    .build();
            graceJson = encodeGrace(grace);
        }

        List<String> result = execute(
                OP_ROTATE,
                sessionId,
                current,
                expectedJson,
                successor,
                lifetime.getEffectiveNow(),
                presentedHash,
                graceExpiresAt,
                graceJson,
                param.getClientBindingHash(),
                current.getRotationCounter(),
                successor == null ? current.getRotationCounter() : successor.getRotationCounter(),
                "");

        String status = first(result);
        if ("ROTATED".equals(status)) {
            return new RefreshSessionLifecycleResult(
                    RefreshSessionLifecycleOutcome.ROTATED,
                    sessionId,
                    successorToken,
                    decodeReturnedFamily(result));
        }

        if ("GRACE_RECOVERED".equals(status)) {
            return recoverGraceSuccessor(result, sessionId);
        }

        throw failure(status);
    }

    public RefreshSessionFamilyRecord requireFamily(String sessionId) {
        String canonicalSessionId = canonicalUuid(sessionId, "sessionId");
        String json = readFamilyJson(canonicalSessionId);

        if (!StringUtils.hasText(json)) {
            throw new RefreshSessionLifecycleException(MISSING_FAMILY, "refresh session family is unavailable");
        }
        return decodeFamily(json);
    }

    public boolean revokePresented(String presentedRefreshToken) {
        Assert.hasText(presentedRefreshToken, "presentedRefreshToken is required");

        String sessionId = extractSessionId(presentedRefreshToken);
        String currentJson = readFamilyJson(sessionId);

        if (!StringUtils.hasText(currentJson)) {
            return false;
        }

        RefreshSessionFamilyRecord current = decodeFamily(currentJson);
        List<String> result = execute(
                OP_REVOKE_PRESENTED,
                sessionId,
                current,
                currentJson,
                null,
                current.getLastUsedAt(),
                tokenHasher.hash(presentedRefreshToken),
                null,
                "",
                "",
                current.getRotationCounter(),
                current.getRotationCounter(),
                "");

        String status = first(result);
        if ("REVOKED".equals(status)) {
            return true;
        }
        if ("NOT_REVOKED".equals(status)) {
            return false;
        }
        throw failure(status);
    }

    public void revokeOne(String userId, String sessionId) {
        Assert.hasText(userId, "userId is required");

        String canonicalSessionId = canonicalUuid(sessionId, "sessionId");
        List<String> result = executeRevoke(OP_REVOKE_ONE, userId, canonicalSessionId, "");
        requireStatus(result, "REVOKED");
    }

    public void revokeOthers(String userId, String retainedSessionId) {
        Assert.hasText(userId, "userId is required");

        String canonicalSessionId = canonicalUuid(retainedSessionId, "retainedSessionId");
        List<String> result = executeRevoke(OP_REVOKE_OTHERS, userId, canonicalSessionId, canonicalSessionId);
        requireStatus(result, "REVOKED");
    }

    public void revokeAll(String userId) {
        Assert.hasText(userId, "userId is required");

        List<String> result = executeRevoke(OP_REVOKE_ALL, userId, "all", "");
        requireStatus(result, "REVOKED");
    }

    public String extractSessionId(String refreshToken) {
        Assert.hasText(refreshToken, "refreshToken is required");

        int separator = refreshToken.indexOf('.');
        if (separator <= 0 || separator == refreshToken.length() - 1) {
            throw new IllegalArgumentException("invalid refresh token format");
        }

        return canonicalUuid(refreshToken.substring(0, separator), "refresh token sessionId");
    }

    private RefreshSessionLifecycleResult recoverGraceSuccessor(List<String> result, String sessionId) {
        if (result.size() < 5) {
            revokeAfterEnvelopeFailure(result, sessionId);
            throw new RefreshSessionLifecycleException(INVARIANT_VIOLATION, "grace recovery result is incomplete");
        }

        String envelope = result.get(1);
        long counter;
        long expiresAtEpochMilli;

        try {
            counter = Long.parseLong(result.get(3));
            expiresAtEpochMilli = Long.parseLong(result.get(4));
        } catch (NumberFormatException exception) {
            revokeAfterEnvelopeFailure(result, sessionId);
            throw new RefreshSessionLifecycleException(INVARIANT_VIOLATION,
                    "grace recovery result contains invalid lineage", exception);
        }

        RefreshSessionFamilyRecord family = decodeFamily(result.get(2));
        String recoveredToken;

        try {
            recoveredToken = envelopeCipher.decrypt(envelope, sessionId, counter,
                    Instant.ofEpochMilli(expiresAtEpochMilli));
        } catch (RefreshSessionSuccessorEnvelopeException exception) {
            revokeOne(family.getUserId(), sessionId);
            throw new RefreshSessionLifecycleException(ENVELOPE_FAILURE, "refresh successor recovery failed",
                    exception);
        }

        if (!tokenHasher.hash(recoveredToken).equals(family.getCurrentRefreshTokenHash())) {
            revokeOne(family.getUserId(), sessionId);
            throw new RefreshSessionLifecycleException(ENVELOPE_FAILURE,
                    "recovered refresh successor does not match the family");
        }

        return new RefreshSessionLifecycleResult(
                RefreshSessionLifecycleOutcome.GRACE_RECOVERED,
                sessionId,
                recoveredToken,
                family);
    }

    private List<String> executeRevoke(String operation, String userId, String sessionId, String retainedSessionId) {
        RefreshSessionFamilyRecord placeholder = RefreshSessionFamilyRecord.builder()
                .recordVersion(RefreshSessionFamilyRecord.CURRENT_RECORD_VERSION)
                .userId(userId)
                .instanceId("00000000-0000-0000-0000-000000000000")
                .roles(List.of("USER"))
                .authVersion(0L)
                .currentRefreshTokenHash("0".repeat(64))
                .rotationCounter(0L)
                .createdAt(Instant.EPOCH)
                .lastUsedAt(Instant.EPOCH)
                .idleExpiresAt(Instant.EPOCH.plus(Duration.ofDays(7)))
                .absoluteExpiresAt(Instant.EPOCH.plus(Duration.ofDays(30)))
                .clientBindingHash("0".repeat(64))
                .build();

        return execute(
                operation,
                sessionId,
                placeholder,
                "",
                null,
                Instant.EPOCH,
                "",
                null,
                "",
                "",
                0L,
                0L,
                retainedSessionId);
    }

    private List<String> execute(
            String operation,
            String sessionId,
            RefreshSessionFamilyRecord current,
            String expectedJson,
            RefreshSessionFamilyRecord next,
            Instant effectiveNow,
            String presentedHash,
            Instant graceExpiresAt,
            String graceJson,
            String clientBindingHash,
            long currentCounter,
            long nextCounter,
            String retainedSessionId) {
        List<String> redisKeys = List.of(
                keys.session(sessionId),
                keys.grace(sessionId),
                keys.baseIndex(current.getUserId()),
                keys.elevatedIndex(current.getUserId()));
        String nextJson = next == null ? "" : familySerializer.encode(next);
        String nextExpiry = next == null ? "" : Long.toString(next.getIdleExpiresAt().toEpochMilli());
        String graceExpiry = graceExpiresAt == null ? "" : Long.toString(graceExpiresAt.toEpochMilli());
        String adminVersion = next == null || next.getAdminSessionVersion() == null
                ? ""
                : Long.toString(next.getAdminSessionVersion());
        boolean elevated = next != null && isElevated(next);
        boolean writesVersions = OP_CREATE.equals(operation) || OP_ROTATE.equals(operation);

        List<String> arguments = new ArrayList<>(25);
        arguments.add(operation);
        arguments.add(sessionId);
        arguments.add(current.getUserId());
        arguments.add(current.getInstanceId());
        arguments.add(Long.toString(effectiveNow.toEpochMilli()));
        arguments.add(expectedJson);
        arguments.add(nextJson);
        arguments.add(nextExpiry);
        arguments.add(graceJson);
        arguments.add(graceExpiry);
        arguments.add(presentedHash);
        arguments.add(clientBindingHash);
        arguments.add(elevated ? "1" : "0");
        arguments.add(next == null ? "" : Long.toString(next.getAuthVersion()));
        arguments.add(adminVersion);
        arguments.add(keys.sessionPrefix());
        arguments.add(keys.gracePrefix());
        arguments.add(Long.toString(effectiveNow.plus(INDEX_LIFETIME).toEpochMilli()));
        arguments.add(Long.toString(current.getIdleExpiresAt().toEpochMilli()));
        arguments.add(Long.toString(current.getAbsoluteExpiresAt().toEpochMilli()));
        arguments.add(Long.toString(currentCounter));
        arguments.add(Long.toString(nextCounter));
        arguments.add(retainedSessionId);
        arguments.add(writesVersions ? keys.userAuthVersion(current.getUserId()) : "");
        arguments.add(writesVersions ? keys.adminSessionVersion(current.getInstanceId(), current.getUserId()) : "");

        try {
            return scriptExecutor.execute(redisKeys, arguments);
        } catch (RefreshSessionLifecycleException exception) {
            throw exception;
        } catch (DataAccessException | IllegalStateException exception) {
            throw new RefreshSessionLifecycleException(REDIS_FAILURE,
                    "refresh session lifecycle Redis transition failed", exception);
        }
    }

    private String readFamilyJson(String sessionId) {
        try {
            return redis.opsForValue().get(keys.session(sessionId));
        } catch (DataAccessException exception) {
            throw new RefreshSessionLifecycleException(REDIS_FAILURE,
                    "refresh session family could not be read", exception);
        }
    }

    private RefreshSessionFamilyRecord decodeFamily(String json) {
        try {
            return familySerializer.decode(json);
        } catch (RefreshSessionFamilySerializerException exception) {
            RefreshSessionLifecycleException.Reason reason = MALFORMED_STATE;
            if (exception.getReason() == RefreshSessionFamilySerializerException.Reason.UNSUPPORTED_RECORD_VERSION) {
                reason = UNSUPPORTED_RECORD_VERSION;
            }
            throw new RefreshSessionLifecycleException(reason, "refresh session family is invalid", exception);
        }
    }

    private String encodeGrace(RefreshSessionGraceRecord grace) {
        try {
            return jsonMapper.writeValueAsString(grace);
        } catch (JacksonException exception) {
            throw new RefreshSessionLifecycleException(INVARIANT_VIOLATION,
                    "refresh session grace could not be serialized", exception);
        }
    }

    private String encryptSuccessor(String token, String sessionId, long counter, Instant graceExpiresAt) {
        try {
            return envelopeCipher.encrypt(token, sessionId, counter, graceExpiresAt);
        } catch (RefreshSessionSuccessorEnvelopeException exception) {
            throw new RefreshSessionLifecycleException(ENVELOPE_FAILURE,
                    "refresh successor could not be encrypted", exception);
        }
    }

    private RefreshSessionFamilyRecord decodeReturnedFamily(List<String> result) {
        if (result.size() < 2 || !StringUtils.hasText(result.get(1))) {
            throw new RefreshSessionLifecycleException(INVARIANT_VIOLATION,
                    "lifecycle script did not return a family");
        }
        return decodeFamily(result.get(1));
    }

    private void revokeAfterEnvelopeFailure(List<String> result, String sessionId) {
        if (result.size() < 3 || !StringUtils.hasText(result.get(2))) {
            return;
        }
        try {
            RefreshSessionFamilyRecord family = decodeFamily(result.get(2));
            revokeOne(family.getUserId(), sessionId);
        } catch (RuntimeException ignored) {
            // The caller still fails closed. Operational reconciliation handles corrupt
            // state.
        }
    }

    private static String first(List<String> result) {
        return result.get(0);
    }

    private static void requireStatus(List<String> result, String requiredStatus) {
        String actualStatus = first(result);
        if (!requiredStatus.equals(actualStatus)) {
            throw failure(actualStatus);
        }
    }

    private static RefreshSessionLifecycleException failure(String status) {
        return switch (status) {
            case "MISSING" -> new RefreshSessionLifecycleException(MISSING_FAMILY,
                    "refresh session family is unavailable");
            case "EXPIRED" -> new RefreshSessionLifecycleException(EXPIRED_FAMILY,
                    "refresh session family is expired");
            case "REPLAY_REVOKED" -> new RefreshSessionLifecycleException(REPLAY_REVOKED,
                    "refresh session family was revoked after replay");
            case "MALFORMED_STATE" -> new RefreshSessionLifecycleException(MALFORMED_STATE,
                    "refresh session Redis state is malformed");
            case "WRONG_TYPE" -> new RefreshSessionLifecycleException(WRONG_REDIS_TYPE,
                    "refresh session Redis state has the wrong type");
            case "UNSUPPORTED_VERSION" -> new RefreshSessionLifecycleException(UNSUPPORTED_RECORD_VERSION,
                    "refresh session Redis state has an unsupported version");
            default -> new RefreshSessionLifecycleException(INVARIANT_VIOLATION,
                    "unexpected refresh session lifecycle result");
        };
    }

    private String newRefreshToken(String sessionId) {
        return sessionId + "." + tokenGenerator.generateBase64Url(REFRESH_SECRET_BYTES);
    }

    private static String canonicalUuid(String value, String name) {
        Assert.hasText(value, name + " is required");

        String canonical = UUID.fromString(value.trim()).toString();
        if (!canonical.equals(value.trim().toLowerCase())) {
            throw new IllegalArgumentException(name + " must be a canonical UUID");
        }
        return canonical;
    }

    private static boolean isElevated(RefreshSessionFamilyRecord family) {
        return family.getAdminSessionVersion() != null && family.getRoles().contains(INSTANCE_ADMIN);
    }

}
