package com.syncturtle.platform.gateway.security.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.syncturtle.platform.gateway.exception.PassportAuthenticationException;
import com.syncturtle.platform.gateway.security.PassportClaims;
import com.syncturtle.platform.gateway.type.PassportAuthenticationFailureReason;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public final class PassportSessionValidator {

    private final ReactiveStringRedisTemplate redis;
    private final PassportRedisKeys redisKeys;
    private final PassportSessionRecordParser recordParser;

    public Mono<ParsedPassportSession> validate(PassportClaims claims) {
        List<String> keys = validationKeys(claims);

        return redis.opsForValue()
                .multiGet(keys)
                .switchIfEmpty(Mono.error(failure(PassportAuthenticationFailureReason.REDIS_UNAVAILABLE)))
                .onErrorMap(this::mapRedisFailure)
                .map(values -> validateSnapshot(claims, values));
    }

    private ParsedPassportSession validateSnapshot(PassportClaims claims, List<String> values) {
        int expectedSize = claims.getAdminSessionVersion() == null ? 2 : 3;
        if (values == null || values.size() != expectedSize) {
            throw failure(PassportAuthenticationFailureReason.REDIS_UNAVAILABLE);
        }

        ParsedPassportSession session = recordParser.parse(claims.getSessionId(), values.get(0));

        if (!Objects.equals(claims.getUserId(), session.getUserId())) {
            throw failure(PassportAuthenticationFailureReason.SUBJECT_MISMATCH);
        }
        if (!Objects.equals(claims.getSessionId(), session.getSessionId())) {
            throw failure(PassportAuthenticationFailureReason.JWT_CLAIMS_INVALID);
        }
        if (!Objects.equals(claims.getInstanceId(), session.getInstanceId())) {
            throw failure(PassportAuthenticationFailureReason.INSTANCE_MISMATCH);
        }
        if (!Objects.equals(claims.getRoles(), session.getRoles())) {
            throw failure(PassportAuthenticationFailureReason.ROLE_MISMATCH);
        }
        if (claims.getUserAuthVersion() != session.getUserAuthVersion()) {
            throw failure(PassportAuthenticationFailureReason.USER_VERSION_MISMATCH);
        }
        if (!Objects.equals(claims.getAdminSessionVersion(), session.getAdminSessionVersion())) {
            throw failure(PassportAuthenticationFailureReason.ADMIN_VERSION_MISMATCH);
        }

        long currentUserVersion = parseVersion(
                values.get(1),
                PassportAuthenticationFailureReason.USER_VERSION_MISSING,
                PassportAuthenticationFailureReason.USER_VERSION_MALFORMED);
        if (currentUserVersion != claims.getUserAuthVersion()) {
            throw failure(PassportAuthenticationFailureReason.USER_VERSION_MISMATCH);
        }

        if (claims.getAdminSessionVersion() != null) {
            long currentAdminVersion = parseVersion(
                    values.get(2),
                    PassportAuthenticationFailureReason.ADMIN_VERSION_MISSING,
                    PassportAuthenticationFailureReason.ADMIN_VERSION_MALFORMED);
            if (currentAdminVersion != claims.getAdminSessionVersion()) {
                throw failure(PassportAuthenticationFailureReason.ADMIN_VERSION_MISMATCH);
            }
        }

        return session;
    }

    private List<String> validationKeys(PassportClaims claims) {
        List<String> keys = new ArrayList<>(3);
        keys.add(redisKeys.sessionKey(claims.getSessionId()));
        keys.add(redisKeys.userAuthVersionKey(claims.getUserId()));

        if (claims.getAdminSessionVersion() != null) {
            keys.add(redisKeys.adminSessionVersionKey(claims.getInstanceId(), claims.getUserId()));
        }

        return List.copyOf(keys);
    }

    private long parseVersion(
            String value,
            PassportAuthenticationFailureReason missingReason,
            PassportAuthenticationFailureReason malformedReason) {
        if (!StringUtils.hasText(value)) {
            throw failure(missingReason);
        }

        try {
            long version = Long.parseLong(value.trim());
            if (version < 0) {
                throw failure(malformedReason);
            }
            return version;
        } catch (NumberFormatException exception) {
            throw new PassportAuthenticationException(malformedReason, exception);
        }
    }

    private Throwable mapRedisFailure(Throwable exception) {
        if (exception instanceof PassportAuthenticationException) {
            return exception;
        }
        return new PassportAuthenticationException(PassportAuthenticationFailureReason.REDIS_UNAVAILABLE, exception);
    }

    private static PassportAuthenticationException failure(PassportAuthenticationFailureReason reason) {
        return new PassportAuthenticationException(reason);
    }

}
