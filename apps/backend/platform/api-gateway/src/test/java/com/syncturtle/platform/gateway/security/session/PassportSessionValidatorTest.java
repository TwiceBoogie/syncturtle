package com.syncturtle.platform.gateway.security.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;

import com.syncturtle.common.cache.property.RedisKeyProperties;
import com.syncturtle.common.cache.template.RedisKeyBuilder;
import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;
import com.syncturtle.platform.gateway.exception.PassportAuthenticationException;
import com.syncturtle.platform.gateway.type.PassportAuthenticationFailureReason;
import com.syncturtle.platform.gateway.security.PassportClaims;

import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("PassportSessionValidator")
class PassportSessionValidatorTest {

    private static final String SESSION_ID = "3a428175-bef1-4c13-ae53-997b6fbfc508";
    private static final String USER_ID = "c12c7988-b9fe-4299-a72f-e430d004d37b";
    private static final String OTHER_USER_ID = "077a399a-066b-4a54-b40d-302bde77af48";
    private static final String INSTANCE_ID = "91ff9854-e8ac-4bf5-91d8-3117dcd8d05c";
    private static final Instant NOW = Instant.parse("2026-08-10T12:00:00Z");

    private ReactiveValueOperations<String, String> values;
    private JsonMapper jsonMapper;
    private Clock clock;
    private PassportSessionValidator validator;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        values = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        jsonMapper = JsonMapper.builder().findAndAddModules().build();
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        PassportSessionRecordParser parser = new PassportSessionRecordParser(jsonMapper, clock);
        PassportRedisKeys keys = new PassportRedisKeys(
                new RedisKeyBuilder(new RedisKeyProperties("st:test:")));
        validator = new PassportSessionValidator(redis, keys, parser);
    }

    @Nested
    @DisplayName("validate(PassportClaims)")
    class Validate {

        @Test
        @DisplayName("accepts jwt and matching session snapshot")
        void acceptsJwtAndMatchingSessionSnapshot() throws Exception {
            PassportClaims claims = userClaims(USER_ID, INSTANCE_ID, List.of("USER"), 4L, null);
            String sessionJson = jsonMapper
                    .writeValueAsString(userSession(USER_ID, INSTANCE_ID, List.of("USER"), 4L, null));
            when(values.multiGet(anyCollection())).thenReturn(Mono.just(List.of(sessionJson, "4")));

            ParsedPassportSession result = validator.validate(claims).block();

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getSessionId()).isEqualTo(SESSION_ID);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<String>> keys = ArgumentCaptor.forClass(Collection.class);
            verify(values).multiGet(keys.capture());
            assertThat(keys.getValue().iterator().next())
                    .isEqualTo("st:test:auth:user-service:refresh-session:sid:" + SESSION_ID);
        }

        @Test
        @DisplayName("rejects jwt when session belongs to different user")
        void rejectsJwtWhenSessionBelongsToDifferentUser() throws Exception {
            PassportClaims claims = userClaims(USER_ID, INSTANCE_ID, List.of("USER"), 4L, null);
            String sessionJson = jsonMapper
                    .writeValueAsString(userSession(OTHER_USER_ID, INSTANCE_ID, List.of("USER"), 4L, null));
            when(values.multiGet(anyCollection())).thenReturn(Mono.just(List.of(sessionJson, "4")));

            PassportAuthenticationException failure = catchThrowableOfType(PassportAuthenticationException.class,
                    () -> validator.validate(claims).block());

            assertThat(failure.getReason())
                    .isEqualTo(PassportAuthenticationFailureReason.SUBJECT_MISMATCH);
        }

        @Test
        @DisplayName("rejects jwt when instance does not match session")
        void rejectsJwtWhenInstanceDoesNotMatchSession() throws Exception {
            PassportClaims claims = userClaims(USER_ID, INSTANCE_ID, List.of("USER"), 4L, null);
            String otherInstance = "4b7e3a73-3986-4d9a-9cda-fde4c2166017";
            String sessionJson = jsonMapper
                    .writeValueAsString(userSession(USER_ID, otherInstance, List.of("USER"), 4L, null));
            when(values.multiGet(anyCollection())).thenReturn(Mono.just(List.of(sessionJson, "4")));

            PassportAuthenticationException failure = catchThrowableOfType(PassportAuthenticationException.class,
                    () -> validator.validate(claims).block());

            assertThat(failure.getReason())
                    .isEqualTo(PassportAuthenticationFailureReason.INSTANCE_MISMATCH);
        }

        @Test
        @DisplayName("rejects jwt when role snapshot does not match session")
        void rejectsJwtWhenRoleSnapshotDoesNotMatchSession() throws Exception {
            PassportClaims claims = userClaims(USER_ID, INSTANCE_ID, List.of("USER"), 4L, null);
            String sessionJson = jsonMapper.writeValueAsString(userSession(
                    USER_ID,
                    INSTANCE_ID,
                    List.of("INSTANCE_ADMIN", "USER"),
                    4L,
                    8L));
            when(values.multiGet(anyCollection())).thenReturn(Mono.just(List.of(sessionJson, "4")));

            PassportAuthenticationException failure = catchThrowableOfType(PassportAuthenticationException.class,
                    () -> validator.validate(claims).block());

            assertThat(failure.getReason())
                    .isEqualTo(PassportAuthenticationFailureReason.ROLE_MISMATCH);
        }

        @Test
        @DisplayName("rejects stale user version")
        void rejectsStaleUserVersion() throws Exception {
            PassportClaims claims = userClaims(USER_ID, INSTANCE_ID, List.of("USER"), 4L, null);
            String sessionJson = jsonMapper
                    .writeValueAsString(userSession(USER_ID, INSTANCE_ID, List.of("USER"), 4L, null));
            when(values.multiGet(anyCollection())).thenReturn(Mono.just(List.of(sessionJson, "5")));

            PassportAuthenticationException failure = catchThrowableOfType(PassportAuthenticationException.class,
                    () -> validator.validate(claims).block());

            assertThat(failure.getReason())
                    .isEqualTo(PassportAuthenticationFailureReason.USER_VERSION_MISMATCH);
        }

        @Test
        @DisplayName("accepts matching admin snapshot and version")
        void acceptsMatchingAdminSnapshotAndVersion() throws Exception {
            List<String> roles = List.of("INSTANCE_ADMIN", "USER");
            PassportClaims claims = userClaims(USER_ID, INSTANCE_ID, roles, 4L, 8L);
            String sessionJson = jsonMapper.writeValueAsString(userSession(USER_ID, INSTANCE_ID, roles, 4L, 8L));
            when(values.multiGet(anyCollection())).thenReturn(Mono.just(List.of(sessionJson, "4", "8")));

            ParsedPassportSession result = validator.validate(claims).block();

            assertThat(result).isNotNull();
            assertThat(result.getAdminSessionVersion()).isEqualTo(8L);
        }

        @Test
        @DisplayName("rejects stale admin version")
        void rejectsStaleAdminVersion() throws Exception {
            List<String> roles = List.of("INSTANCE_ADMIN", "USER");
            PassportClaims claims = userClaims(USER_ID, INSTANCE_ID, roles, 4L, 8L);
            String sessionJson = jsonMapper.writeValueAsString(userSession(USER_ID, INSTANCE_ID, roles, 4L, 8L));
            when(values.multiGet(anyCollection())).thenReturn(Mono.just(List.of(sessionJson, "4", "9")));

            PassportAuthenticationException failure = catchThrowableOfType(PassportAuthenticationException.class,
                    () -> validator.validate(claims).block());

            assertThat(failure.getReason())
                    .isEqualTo(PassportAuthenticationFailureReason.ADMIN_VERSION_MISMATCH);
        }

        @Test
        @DisplayName("rejects missing session record")
        void rejectsMissingSessionRecord() {
            PassportClaims claims = userClaims(USER_ID, INSTANCE_ID, List.of("USER"), 4L, null);
            when(values.multiGet(anyCollection())).thenReturn(Mono.just(java.util.Arrays.asList(null, "4")));

            PassportAuthenticationException failure = catchThrowableOfType(PassportAuthenticationException.class,
                    () -> validator.validate(claims).block());

            assertThat(failure.getReason())
                    .isEqualTo(PassportAuthenticationFailureReason.SESSION_MISSING);
        }

        @Test
        @DisplayName("rejects malformed version value")
        void rejectsMalformedVersionValue() throws Exception {
            PassportClaims claims = userClaims(USER_ID, INSTANCE_ID, List.of("USER"), 4L, null);
            String sessionJson = jsonMapper
                    .writeValueAsString(userSession(USER_ID, INSTANCE_ID, List.of("USER"), 4L, null));
            when(values.multiGet(anyCollection())).thenReturn(Mono.just(List.of(sessionJson, "not-a-version")));

            PassportAuthenticationException failure = catchThrowableOfType(PassportAuthenticationException.class,
                    () -> validator.validate(claims).block());

            assertThat(failure.getReason())
                    .isEqualTo(PassportAuthenticationFailureReason.USER_VERSION_MALFORMED);
        }

        @Test
        @DisplayName("fails closed when redis read fails")
        void failsClosedWhenRedisReadFails() {
            PassportClaims claims = userClaims(USER_ID, INSTANCE_ID, List.of("USER"), 4L, null);
            when(values.multiGet(anyCollection()))
                    .thenReturn(Mono.error(new IllegalStateException("redis unavailable")));

            PassportAuthenticationException failure = catchThrowableOfType(PassportAuthenticationException.class,
                    () -> validator.validate(claims).block());

            assertThat(failure.getReason())
                    .isEqualTo(PassportAuthenticationFailureReason.REDIS_UNAVAILABLE);
        }
    }

    private PassportClaims userClaims(
            String userId,
            String instanceId,
            List<String> roles,
            long userVersion,
            Long adminVersion) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuer("https://api.syncturtle.test/auth")
                .subject(userId)
                .issuedAt(NOW.minusSeconds(60))
                .expiresAt(NOW.plusSeconds(900))
                .claim("sid", SESSION_ID)
                .claim("instance_id", instanceId)
                .claim("roles", roles)
                .claim("auth_ver", userVersion)
                .claim("token_use", "access");
        if (adminVersion != null) {
            builder.claim("admin_session_ver", adminVersion);
        }
        return PassportClaims.from(builder.build(), clock);
    }

    private RefreshSessionFamilyRecord userSession(
            String userId,
            String instanceId,
            List<String> roles,
            long userVersion,
            Long adminVersion) {
        Instant createdAt = NOW.minus(Duration.ofDays(1));
        Instant lastUsedAt = NOW.minus(Duration.ofHours(1));
        return RefreshSessionFamilyRecord.builder()
                .recordVersion(RefreshSessionFamilyRecord.CURRENT_RECORD_VERSION)
                .userId(userId)
                .instanceId(instanceId)
                .roles(roles)
                .authVersion(userVersion)
                .adminSessionVersion(adminVersion)
                .currentRefreshTokenHash("a".repeat(64))
                .rotationCounter(2L)
                .createdAt(createdAt)
                .lastUsedAt(lastUsedAt)
                .idleExpiresAt(lastUsedAt.plus(RefreshSessionFamilyRecord.IDLE_LIFETIME))
                .absoluteExpiresAt(createdAt.plus(RefreshSessionFamilyRecord.ABSOLUTE_LIFETIME))
                .deviceLabel("Test browser")
                .clientBindingHash("b".repeat(64))
                .build();
    }
}
