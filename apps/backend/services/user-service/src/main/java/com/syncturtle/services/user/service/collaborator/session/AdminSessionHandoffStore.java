package com.syncturtle.services.user.service.collaborator.session;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.core.security.token.TokenHasher;
import com.syncturtle.services.user.configuration.property.AdminSessionHandoffProperties;
import com.syncturtle.services.user.exception.AdminSessionHandoffException;
import com.syncturtle.services.user.service.param.AdminSessionHandoffCreateParam;
import com.syncturtle.services.user.type.AdminSessionHandoffState;

public final class AdminSessionHandoffStore {

    private static final int SECRET_BYTES = 32;
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
    private static final String OP_CLAIM = "CLAIM";
    private static final String OP_RELEASE = "RELEASE";
    private static final String OP_CONSUME = "CONSUME";

    private final StringRedisTemplate redis;
    private final AdminSessionHandoffRedisKeys keys;
    private final SecureTokenGenerator tokenGenerator;
    private final TokenHasher tokenHasher;
    private final AdminSessionHandoffSerializer serializer;
    private final AdminSessionHandoffScriptExecutor scriptExecutor;
    private final AdminSessionHandoffProperties properties;
    private final Clock clock;
    private final Supplier<String> idGenerator;

    public AdminSessionHandoffStore(
            StringRedisTemplate redis,
            AdminSessionHandoffRedisKeys keys,
            SecureTokenGenerator tokenGenerator,
            TokenHasher tokenHasher,
            AdminSessionHandoffSerializer serializer,
            AdminSessionHandoffScriptExecutor scriptExecutor,
            AdminSessionHandoffProperties properties,
            Clock clock) {
        this(redis, keys, tokenGenerator, tokenHasher, serializer, scriptExecutor,
                properties, clock, () -> UUID.randomUUID().toString());
    }

    AdminSessionHandoffStore(
            StringRedisTemplate redis,
            AdminSessionHandoffRedisKeys keys,
            SecureTokenGenerator tokenGenerator,
            TokenHasher tokenHasher,
            AdminSessionHandoffSerializer serializer,
            AdminSessionHandoffScriptExecutor scriptExecutor,
            AdminSessionHandoffProperties properties,
            Clock clock,
            Supplier<String> idGenerator) {
        Assert.notNull(redis, "redis is required");
        Assert.notNull(keys, "keys are required");
        Assert.notNull(tokenGenerator, "tokenGenerator is required");
        Assert.notNull(tokenHasher, "tokenHasher is required");
        Assert.notNull(serializer, "serializer is required");
        Assert.notNull(scriptExecutor, "scriptExecutor is required");
        Assert.notNull(properties, "properties are required");
        Assert.notNull(clock, "clock is required");
        Assert.notNull(idGenerator, "idGenerator is required");

        this.redis = redis;
        this.keys = keys;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.serializer = serializer;
        this.scriptExecutor = scriptExecutor;
        this.properties = properties;
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    public AdminSessionHandoffReceipt create(AdminSessionHandoffCreateParam param) {
        Assert.notNull(param, "admin session handoff create param is required");

        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.getTtl());
        String receiptId = nextId("generated receiptId");
        String secret = tokenGenerator.generateBase64Url(SECRET_BYTES);
        Assert.hasText(secret, "generated handoff secret is required");

        String completionCode = receiptId + "." + secret;
        AdminSessionHandoffRecord record = AdminSessionHandoffRecord.builder()
                .recordVersion(AdminSessionHandoffRecord.CURRENT_RECORD_VERSION)
                .codeHash(tokenHasher.hash(secret))
                .userId(param.getUserId())
                .instanceId(param.getInstanceId())
                .userAuthVersion(param.getUserAuthVersion())
                .adminSessionVersion(param.getAdminSessionVersion())
                .preAuthBindingHash(param.getPreAuthBindingHash())
                .clientBindingHash(param.getClientBindingHash())
                .issuedAtEpochMilli(issuedAt.toEpochMilli())
                .expiresAtEpochMilli(expiresAt.toEpochMilli())
                .state(AdminSessionHandoffState.PENDING)
                .claimId(null)
                .build();

        try {
            Boolean created = redis.opsForValue().setIfAbsent(
                    keys.handoff(receiptId),
                    serializer.encode(record),
                    properties.getTtl());
            if (!Boolean.TRUE.equals(created)) {
                throw new AdminSessionHandoffException(
                        AdminSessionHandoffException.Reason.INVARIANT_VIOLATION,
                        "generated admin session handoff receipt already exists");
            }
        } catch (AdminSessionHandoffException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        }

        return new AdminSessionHandoffReceipt(completionCode, issuedAt, expiresAt);
    }

    public AdminSessionHandoffClaim claim(
            String completionCode,
            String preAuthBindingHash,
            String clientBindingHash) {
        CodeParts code = parseCode(completionCode);
        requireHash(preAuthBindingHash, "preAuthBindingHash");
        requireHash(clientBindingHash, "clientBindingHash");

        String claimId = nextId("generated claimId");
        List<String> result = execute(
                List.of(keys.handoff(code.getReceiptId())),
                List.of(
                        OP_CLAIM,
                        tokenHasher.hash(code.getSecret()),
                        preAuthBindingHash,
                        clientBindingHash,
                        Long.toString(clock.millis()),
                        claimId));
        requireStatus(result, "CLAIMED");
        if (result.size() != 2 || !StringUtils.hasText(result.get(1))) {
            throw new AdminSessionHandoffException(
                    AdminSessionHandoffException.Reason.INVARIANT_VIOLATION,
                    "admin session handoff claim result is incomplete");
        }

        AdminSessionHandoffRecord record = serializer.decode(result.get(1));
        return new AdminSessionHandoffClaim(code.getReceiptId(), claimId, record);
    }

    public void release(AdminSessionHandoffClaim claim) {
        Assert.notNull(claim, "claim is required");

        List<String> result = execute(
                List.of(keys.handoff(claim.getReceiptId())),
                List.of(OP_RELEASE, claim.getClaimId()));
        requireStatus(result, "RELEASED");
    }

    public void consume(AdminSessionHandoffClaim claim) {
        Assert.notNull(claim, "claim is required");

        List<String> result = execute(
                List.of(keys.handoff(claim.getReceiptId())),
                List.of(OP_CONSUME, claim.getClaimId()));
        requireStatus(result, "CONSUMED");
    }

    private List<String> execute(List<String> redisKeys, List<String> arguments) {
        try {
            return scriptExecutor.execute(redisKeys, arguments);
        } catch (AdminSessionHandoffException exception) {
            throw exception;
        } catch (DataAccessException | IllegalStateException exception) {
            throw unavailable(exception);
        }
    }

    private String nextId(String name) {
        return canonicalUuid(idGenerator.get(), name);
    }

    private static void requireStatus(List<String> result, String expected) {
        String actual = result.get(0);
        if (!expected.equals(actual)) {
            throw failure(actual);
        }
    }

    private static AdminSessionHandoffException failure(String status) {
        AdminSessionHandoffException.Reason reason = switch (status) {
            case "EXPIRED" -> AdminSessionHandoffException.Reason.EXPIRED;
            case "ALREADY_CLAIMED", "CLAIM_MISMATCH" -> AdminSessionHandoffException.Reason.REPLAYED;
            case "BINDING_MISMATCH" -> AdminSessionHandoffException.Reason.BINDING_MISMATCH;
            case "MALFORMED" -> AdminSessionHandoffException.Reason.MALFORMED_RECORD;
            case "UNSUPPORTED_VERSION" -> AdminSessionHandoffException.Reason.UNSUPPORTED_RECORD_VERSION;
            case "WRONG_TYPE" -> AdminSessionHandoffException.Reason.WRONG_REDIS_TYPE;
            case "INVALID", "MISSING" -> AdminSessionHandoffException.Reason.INVALID;
            default -> AdminSessionHandoffException.Reason.INVARIANT_VIOLATION;
        };
        return new AdminSessionHandoffException(reason, "admin session handoff rejected");
    }

    private static AdminSessionHandoffException unavailable(Throwable cause) {
        return new AdminSessionHandoffException(
                AdminSessionHandoffException.Reason.REDIS_UNAVAILABLE,
                "admin session handoff state is unavailable",
                cause);
    }

    private static CodeParts parseCode(String value) {
        Assert.hasText(value, "completionCode is required");

        String normalized = value.trim();
        int separator = normalized.indexOf('.');
        if (separator <= 0 || separator == normalized.length() - 1
                || normalized.indexOf('.', separator + 1) >= 0) {
            throw failure("INVALID");
        }

        String receiptId;
        try {
            receiptId = canonicalUuid(normalized.substring(0, separator), "completionCode receiptId");
        } catch (IllegalArgumentException exception) {
            throw failure("INVALID");
        }

        String secret = normalized.substring(separator + 1);
        if (!StringUtils.hasText(secret)) {
            throw failure("INVALID");
        }
        return new CodeParts(receiptId, secret);
    }

    private static void requireHash(String value, String name) {
        Assert.isTrue(value != null && HASH.matcher(value).matches(),
                name + " must be a lowercase SHA-256 value");
    }

    private static String canonicalUuid(String value, String name) {
        Assert.hasText(value, name + " is required");

        String normalized = value.trim();
        String canonical = UUID.fromString(normalized).toString();
        Assert.isTrue(canonical.equals(normalized), name + " must be a canonical UUID");
        return canonical;
    }

    private static final class CodeParts {

        private final String receiptId;
        private final String secret;

        private CodeParts(String receiptId, String secret) {
            this.receiptId = receiptId;
            this.secret = secret;
        }

        private String getReceiptId() {
            return receiptId;
        }

        private String getSecret() {
            return secret;
        }

    }

}
