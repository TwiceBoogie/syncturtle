package com.syncturtle.platform.gateway.security.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

import com.syncturtle.common.cache.property.RedisKeyProperties;
import com.syncturtle.common.cache.template.RedisKeyBuilder;
import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;
import com.syncturtle.platform.gateway.exception.PassportAuthenticationException;
import com.syncturtle.platform.gateway.security.PassportClaims;
import com.syncturtle.platform.gateway.type.PassportAuthenticationFailureReason;
import com.syncturtle.testing.annotation.UseRedis;

import tools.jackson.databind.json.JsonMapper;

@UseRedis
@DataRedisTest(properties = {
        "spring.config.name=passport-session-it",
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false"
})
class PassportSessionValidatorIT {

    private static final String SESSION_ID = "3a428175-bef1-4c13-ae53-997b6fbfc508";
    private static final String USER_ID = "c12c7988-b9fe-4299-a72f-e430d004d37b";
    private static final String INSTANCE_ID = "91ff9854-e8ac-4bf5-91d8-3117dcd8d05c";
    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Autowired
    private ReactiveStringRedisTemplate redis;

    private PassportRedisKeys keys;
    private JsonMapper jsonMapper;
    private PassportSessionValidator validator;

    @BeforeEach
    void setUp() {
        keys = new PassportRedisKeys(new RedisKeyBuilder(new RedisKeyProperties("st:it:gateway-revocation:")));
        jsonMapper = JsonMapper.builder().findAndAddModules().build();
        validator = new PassportSessionValidator(redis, keys, new PassportSessionRecordParser(jsonMapper, CLOCK));
        deleteTestKeys();
    }

    @AfterEach
    void tearDown() {
        deleteTestKeys();
    }

    @Nested
    class Validate {

        @Test
        void rejectsSameUnexpiredOneHourJwtAfterSessionDeletion() throws Exception {
            Jwt jwt = jwt(List.of("USER"), null);
            PassportClaims claims = PassportClaims.from(jwt, CLOCK);
            storeSession(List.of("USER"), null);

            ParsedPassportSession accepted = validator.validate(claims).block();
            redis.delete(keys.sessionKey(SESSION_ID)).block();
            PassportAuthenticationException rejected = catchThrowableOfType(PassportAuthenticationException.class,
                    () -> validator.validate(claims).block());

            assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
            assertThat(accepted).isNotNull();
            assertThat(rejected.getReason()).isEqualTo(PassportAuthenticationFailureReason.SESSION_MISSING);
        }

        @Test
        void rejectsSameUnexpiredJwtAfterUserVersionIncrement() throws Exception {
            PassportClaims claims = PassportClaims.from(jwt(List.of("USER"), null), CLOCK);
            storeSession(List.of("USER"), null);

            ParsedPassportSession accepted = validator.validate(claims).block();
            redis.opsForValue().increment(keys.userAuthVersionKey(USER_ID)).block();
            PassportAuthenticationException rejected = catchThrowableOfType(PassportAuthenticationException.class,
                    () -> validator.validate(claims).block());

            assertThat(accepted).isNotNull();
            assertThat(rejected.getReason()).isEqualTo(PassportAuthenticationFailureReason.USER_VERSION_MISMATCH);
        }

        @Test
        void rejectsSameUnexpiredAdminJwtAfterAdminVersionIncrement() throws Exception {
            List<String> roles = List.of("INSTANCE_ADMIN", "USER");
            PassportClaims claims = PassportClaims.from(jwt(roles, 8L), CLOCK);
            storeSession(roles, 8L);

            ParsedPassportSession accepted = validator.validate(claims).block();
            redis.opsForValue().increment(keys.adminSessionVersionKey(INSTANCE_ID, USER_ID)).block();
            PassportAuthenticationException rejected = catchThrowableOfType(PassportAuthenticationException.class,
                    () -> validator.validate(claims).block());

            assertThat(accepted).isNotNull();
            assertThat(rejected.getReason()).isEqualTo(PassportAuthenticationFailureReason.ADMIN_VERSION_MISMATCH);
        }
    }

    private Jwt jwt(List<String> roles, Long adminVersion) {
        Jwt.Builder builder = Jwt.withTokenValue("decoded-test-jwt")
                .header("alg", "RS256")
                .issuer("https://api.syncturtle.test/auth")
                .subject(USER_ID)
                .issuedAt(NOW)
                .expiresAt(NOW.plus(Duration.ofHours(1)))
                .claim("sid", SESSION_ID)
                .claim("instance_id", INSTANCE_ID)
                .claim("roles", roles)
                .claim("auth_ver", 4L)
                .claim("token_use", "access");
        if (adminVersion != null) {
            builder.claim("admin_session_ver", adminVersion);
        }
        return builder.build();
    }

    private void storeSession(List<String> roles, Long adminVersion) throws Exception {
        RefreshSessionFamilyRecord record = RefreshSessionFamilyRecord.builder()
                .recordVersion(RefreshSessionFamilyRecord.CURRENT_RECORD_VERSION)
                .userId(USER_ID)
                .instanceId(INSTANCE_ID)
                .roles(roles)
                .authVersion(4L)
                .adminSessionVersion(adminVersion)
                .currentRefreshTokenHash("a".repeat(64))
                .rotationCounter(0L)
                .createdAt(NOW)
                .lastUsedAt(NOW)
                .idleExpiresAt(NOW.plus(RefreshSessionFamilyRecord.IDLE_LIFETIME))
                .absoluteExpiresAt(NOW.plus(RefreshSessionFamilyRecord.ABSOLUTE_LIFETIME))
                .deviceLabel("Test browser")
                .clientBindingHash("b".repeat(64))
                .build();
        redis.opsForValue().set(keys.sessionKey(SESSION_ID), jsonMapper.writeValueAsString(record)).block();
        redis.opsForValue().set(keys.userAuthVersionKey(USER_ID), "4").block();
        if (adminVersion != null) {
            redis.opsForValue().set(keys.adminSessionVersionKey(INSTANCE_ID, USER_ID), adminVersion.toString()).block();
        }
    }

    private void deleteTestKeys() {
        redis.delete(keys.sessionKey(SESSION_ID), keys.userAuthVersionKey(USER_ID),
                keys.adminSessionVersionKey(INSTANCE_ID, USER_ID)).block();
    }
}
